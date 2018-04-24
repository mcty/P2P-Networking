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
    //send("Entry added".getBytes(), "200", "OK", hostIP);
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
    DatabaseManager.queryFiles(query);
    
    //Send response specifying results of query to sender
    //TODO
  }
  
  private void performExit(String hostname, String hostIP){
    //Complete exit request by removing entries from db
    System.out.println("Removing file records for peer " + hostname + " with "
            + "IP address " + hostIP);
    DatabaseManager.removePeer(hostname, hostIP);
    
    //Send response to sender
    //send("Entries removed".getBytes(), "200", "OK", hostIP);
  }
  
  public void close(){
    DatabaseManager.exitDatabase();
  }
  
  private void send(byte[] data, String statusCode, String statusPhrase, 
          String hostIP){
    //Socket to send from
    DatagramSocket socket = receivingSocket;
    if(socket == null){
      System.out.println("Error, there is no socket to send from!");
      return;
    }
    
    //Managing RDT
    ACKTimer timer; //Used to resend packets when their ACKs are not received (covers corruption and packet loss)
    
    //Application-Header String
    String CRLF = "\r\n";
    String firstRow = "0 0"+CRLF;
    String secondRow = statusCode + " " + statusPhrase + CRLF;
    
    //Amounts of data to send
    int availableBufSize = bufSize - firstRow.length() - secondRow.length() - CRLF.length(); //This is the only amount we can send. If less than 0, no data can be sent at all
    if(availableBufSize <= 0){
      System.out.println("No data can be sent using the current configuration -- Increase Buffer Size / MTU");
      return;
    }
    
    //Send data
    byte[] fullMessageData = (secondRow + new String(data)).getBytes();
    ByteArrayInputStream byteStream = new ByteArrayInputStream(fullMessageData);
    int packetNum = 0;
    boolean SEQ = false;
    boolean EOM = false;
    
    System.out.println("Starting message to peer " + hostIP);
    try{
      while(byteStream.available()>0){ //While data remains
        byte[] packetData = new byte[availableBufSize]; //Read data into buffer
        int bytesRead = byteStream.read(packetData);

        //If entire buffer not filled, only get array containing data
        if(bytesRead< availableBufSize){ 
          packetData = Arrays.copyOf(packetData, bytesRead);
        }

        //If there are no more bytes, set EOM flag (end of message flag)
      if(byteStream.available()==0) EOM = true;
        
        //Create packet's exact data
        packetData = (""+ (SEQ?1:0) + " " + (EOM?1:0) + CRLF + new String(packetData)).getBytes();
        int packetSize = bytesRead+firstRow.length();

        //Send packet
        System.out.println(
                "\n\n| - - - - START PACKET (" + packetNum++ + ") - - - - - - - - |\n"
                + new String(packetData) 
                + "|- - - - END PACKET (total length: " + packetSize + ") - - - - -| \n");
        DatagramPacket packet = new DatagramPacket(packetData, packetData.length, InetAddress.getByName(hostIP), 50001); //TODO hardcoded host port
        socket.send(packet);

        //Wait for correct ACK
        timer = new ACKTimer(socket, packet, 1000); //Timer that will resend packet
        byte[] ACKdata = { (byte)(SEQ?0:1) }; //Where ACK data will be placed, intialize to incorrect ACK value (which would be the value not equal to SEQ since we need ACK corresponding to sent packet)
        DatagramPacket ack = new DatagramPacket(ACKdata, 1); //ACK packet
        timer.start(); //Start timer
        while(ACKdata[0] != (SEQ?1:0)){ //While incorrect ACK data, wait for correct ACK (this prevents delayed ACKS from affecting system)
          socket.receive(ack); //Constantly receive acks until we get the correct one
          System.out.println("Got ACK:");
          System.out.println("\tACK Data Expected:" + (SEQ?1:0) + "\tAck Data Got " + ACKdata[0]);
        }
        timer.stop(); //End timer after correct ACK
        System.out.println("Correct ACK received, continue sending data.\n");

        //Update SEQ for new packet
        SEQ = !SEQ;
        Thread.sleep(1200);
      }
    }catch(Exception e){
      e.printStackTrace();
    }
    
    System.out.println("Full message sent!");
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
