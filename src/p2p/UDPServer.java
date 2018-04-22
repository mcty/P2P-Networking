package p2p;




import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.io.IOException;
import java.util.Arrays;
import java.util.*;
import java.lang.*;

/**
 * The server of the application. Will be expecting packets sent from peers.
 * 
 * @authors: Tyler & Austin
 */
public class UDPServer extends Host {
  private int port;
  private int bufSize = Host.MSS; //Set to comply with Maximum Segment Size specified in requirements
  private DatagramSocket receivingSocket = null;
  
  public UDPServer(String name, int port){
    super(name);
    this.port = port;
  }
  
  public void stopListening(){
    if(receivingSocket!=null) receivingSocket.close();
    System.out.println("Server socket was closed");
  }
  
  public void printDataReceived(int SEQ, int EOM, String SenderIP, String message){
	System.out.println("|---*Received data packet info*---|");
	System.out.println("SEQ:" +SEQ);
	System.out.println("EOM:" + EOM);
	System.out.println("SenderIP:" + SenderIP);
	System.out.println("message:" + message + "\n");
  }
  
  //To give a specific buffer size
  public void setBufferSize(int size){
    if(size>0){
      bufSize = size;
    }
  }
  
  public void run(){
    String messageT = "";
	int SEQ_previous = 1;
	int PacketNum = 0;
	try{
      receivingSocket = new DatagramSocket(port);
      System.out.println("Host is listening for UDP data on port " + port
              + " with IP address " + getIPAddress());
      while(true){
        //Wait for request
        System.out.println("Waiting for data...");
        byte[] buf = new byte[bufSize];
        
        //Receive request
        DatagramPacket packet = new DatagramPacket(buf, bufSize);
        receivingSocket.receive(packet);
		
        byte[] packetData = Arrays.copyOf(packet.getData(), packet.getLength());
        
		System.out.println("TTS: Socket info: "+ packet.getSocketAddress() +"");
		
        //Handle request
		
		String rph = new String (packetData);	//Save the string from packet.
		String[] arr = rph.split(" |\r\n", 6);
		
		//Variables saved from packetData
		String message_Type = arr[0];
		String Sender = arr[1];
		String SenderIP = arr[2];
		int SEQ = Integer.parseInt(arr[3]);
		int EOM = Integer.parseInt(arr[4]);
		String userData = arr[5];
		userData = userData.substring(0, userData.length() - 2);
		
		if(SEQ != SEQ_previous){
			PacketNum = PacketNum + 1;
			messageT = messageT + userData;
			printPacketReceived(PacketNum, SEQ, EOM, SenderIP, packet.getLength());
			if(EOM == 1){
				printDataReceived(SEQ,EOM,SenderIP,messageT);
                                processMessage(message_Type, userData, Sender, SenderIP);
				PacketNum = 0;
				SEQ_previous = 1;
                                messageT = "";
			}
			else{
				SEQ_previous = SEQ;
			}
			sendACK(receivingSocket,SEQ,packet.getAddress(), packet.getPort());
		}
		else{
			sendACK(receivingSocket,SEQ_previous,packet.getAddress(), packet.getPort());
		}
      }
    }
    catch(Exception e){
      stopListening();
      e.printStackTrace();
    }
  }
  
  private void processMessage(String messageType, String payload,
          String hostname, String hostIP){
    switch(messageType){
      case "inform":
        performInformAndUpdate(payload, hostname, hostIP);
        break;
      case "query":
        performQuery(payload, hostname, hostIP);
        break;
      case "exit":
        performExit(hostname, hostIP);
        break;
      default:
        System.out.println("Unexpected message-type");
    }
  }
  
  private void performInformAndUpdate(String payload, String hostname, 
          String hostIP){
    String currentFileName;
    long currentFileSize;
    Scanner scan; 
    
    //Process inform and update request
    scan = new Scanner(payload);
    while(scan.hasNext()){ //While there's another file...
      //Get file data from payload
      while(scan.next().equals("")); //Ignore any newlines, and ignore 'Filename:'
      currentFileName = decodeString(scan.next());
      currentFileSize = scan.nextLong();
      
      //Insert file into db
      System.out.println("Adding file record: {File: '"+currentFileName
              + "', File size: '" + currentFileSize +"bytes'}, associated with host"
              + "{" + hostname + " at IP address " + hostIP);
    }
    
    //Send response to sender (if file already exists, send error, else, send OK
    send("Entry added".getBytes(), "200", "OK", hostIP);
  }
  
  private void performQuery(String payload, String hostname, String hostIP){
    Scanner scan;
    String query;
    
    //Process query request
    //Get query
    scan = new Scanner(payload);
    scan.next(); //Ignore 'Query'
    query = decodeString(scan.next());
    System.out.println("Searching files with keyword '" + query + "'.");
    
    //Perform query on db
    //TODO
    
    //Send response specifying results of query to sender
    //TODO
  }
  
  private void performExit(String hostname, String hostIP){
    //Complete exit request by removing entries from db
    System.out.println("Removing file records for peer " + hostname + " with "
            + "IP address " + hostIP);
    
    //Send response to sender
    send("Entries removed".getBytes(), "200", "OK", hostIP);
  }
  
  private void send(byte[] data, String statusCode, String statusPhrase, 
          String hostIP){
    
  }
  
  //Packet Recieved Print Statement
  public void printPacketReceived(int pnum, int SEQ, int EOM, String SenderIP, int length){
	  System.out.println("|----Packet " + pnum + " recieved----|");
	  System.out.println("Recieved: " + SenderIP + "\nSEQ: " + SEQ + "\tEOM: " + EOM);
	  System.out.println("|----Packet Length: " + length + " ----|\n");
  }
  
  //Send ACK
  public void sendACK(DatagramSocket socket, int SEQ, InetAddress SenderIP, int SenderPort)throws SocketException, IOException, InterruptedException{
	  byte[] packetData = {(byte)SEQ};
	  DatagramPacket packet = new DatagramPacket(packetData, packetData.length, SenderIP, SenderPort);
      socket.send(packet);
	  System.out.println("ACK Sent: " + SEQ);
  }
}
