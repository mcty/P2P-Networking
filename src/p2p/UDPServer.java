package p2p;

import java.io.ByteArrayInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.io.IOException;
import java.util.Arrays;
import java.util.*;
import p2p.db.DatabaseManager;

/**
 * The server of the application. Will be expecting packets sent from peers.
 * 
 * @authors: Tyler & Austin
 */
public class UDPServer extends Host {
  private int port;
  private int bufSize = Host.MSS; //Set to comply with Maximum Segment Size specified in requirements
  private DatagramSocket receivingSocket = null;
  private boolean newConnection = true;
  public UDPServer(String name, int port){
    super(name);
    this.port = port;
    DatabaseManager.initialize(); //Initialize database
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
                                sendACK(receivingSocket,SEQ,packet.getAddress(), packet.getPort());
                                processMessage(message_Type, messageT, Sender, SenderIP);
				PacketNum = 0;
				SEQ_previous = 1;
                                messageT = "";
			}
			else{
				SEQ_previous = SEQ;
                                sendACK(receivingSocket,SEQ,packet.getAddress(), packet.getPort());
			}
			
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
    
    //If new user, add them to DB before trying to fulfill request.
    if(newConnection){
      DatabaseManager.addPeer(hostname, hostIP);
      newConnection = false;
    }
    
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
    
    DatabaseManager.printCurrentDBState();
  }
  
  private void performInformAndUpdate(String payload, String hostname, 
          String hostIP){
    String currentFileName;
    long currentFileSize;
    Scanner scan; 
    
    //Remove previously existing files from peer (inefficient, but working solution)
    DatabaseManager.removePeerFiles(hostname,hostIP);
    
    //Process inform and update request
    scan = new Scanner(payload);
    while(scan.hasNext()){ //While there's another file...
      //Get file data from payload
      while(!scan.next().equals("Filename:")); //Ignore any newlines, and ignore 'Filename:'
      currentFileName = decodeString(scan.next()); //System.out.println(currentFileName);
      currentFileSize = scan.nextLong();
      
      //Insert file into db
      System.out.println("Adding file record: {File: '"+currentFileName
              + "', File size: '" + currentFileSize +" bytes'}, associated with host "
              + hostname + " at IP address " + hostIP);
      DatabaseManager.addFile(currentFileName, currentFileSize, hostname, hostIP);
      
      //Remove old files no longer shared??
      //TODO
    }
    
    //Send response to sender (if file already exists, send error, else, send OK
    performMessage("Entries have been added".getBytes(), 
            new String[]{"200", "OK"}, hostIP, 50001, receivingSocket);
  }
  
  private void performQuery(String payload, String hostname, String hostIP){
    Scanner scan;
    String query;
    FileData[] results;
    String resultText = "";
    
    //Process query request
    //Get query
    scan = new Scanner(payload);
    scan.next(); //Ignore 'Query'
    query = decodeString(scan.next());
    System.out.println("Searching files with keyword '" + query + "'.");
    
    //Perform query on db, create response to peer
    results = DatabaseManager.queryFiles(query);
    for(FileData file: results){
        resultText+= encodeString(file.getPath()) + " " + + file.getSize() + " " 
                + file.getHostPeerIP() + CRLF;
    }
    
    //Send response specifying results of query to sender
    performMessage(resultText.getBytes(),
            new String[]{"200","OK"}, hostIP, 50001, receivingSocket);
  }
  
  private void performExit(String hostname, String hostIP){
    //Complete exit request by removing entries from db
    System.out.println("Removing file records for peer " + hostname + " with "
            + "IP address " + hostIP);
    DatabaseManager.removePeer(hostname, hostIP);
    
    //Send response to sender
    performMessage("Entries removed".getBytes(),
            new String[]{"200","OK"}, hostIP, 50001, receivingSocket);
  }
  
  private void performMessage(byte[] data, String[] headerData, String IP,
          int port, DatagramSocket socket){
      try{
          performMessage(data, headerData, InetAddress.getByName(IP), port,
                  socket);
      }catch(Exception e){
          System.out.println("Unable to send response to peer");
          e.printStackTrace();
      }
  }
  
  public void close(){
    DatabaseManager.exitDatabase();
  }
  
  public String createHeaders(String[] params){
      if(params == null){ return null;}
      
      String statusCode = params[0];
      String statusPhrase = params[1];
      
      String headerData = statusCode + " " + statusPhrase + CRLF;
      return headerData;
  }
 
  public String getResponse(){
      return null;
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
