


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
  private int bufSize = 128; //128 bytes
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
	System.out.println("**Received data packet info**");
	System.out.println("SEQ:" +SEQ);
	System.out.println("EOM:" + EOM);
	System.out.println("SenderIP:" + SenderIP);
	System.out.println("message:" + message);
  }
  
  //To give a specific buffer size
  public void setBufferSize(int size){
    if(size>0){
      bufSize = size;
    }
  }
  
  public void run(){
    String messageT = "";
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
		
		messageT = messageT + userData;
		if(EOM == 1){
			printDataReceived(SEQ,EOM,SenderIP,messageT);
			
		}
		
		//Send ACK to sender.
		
		sendACK(receivingSocket, SEQ, packet.getAddress(), packet.getPort());
      }
    }
    catch(Exception e){
      stopListening();
      e.printStackTrace();
    }
  }
  
  //Send ACK
  public void sendACK(DatagramSocket socket, int SEQ, InetAddress SenderIP, int SenderPort)throws SocketException, IOException, InterruptedException{
	  byte[] packetData = {(byte)SEQ};
	  DatagramPacket packet = new DatagramPacket(packetData, packetData.length, SenderIP, SenderPort);
      socket.send(packet);
  }
/* 
  //Send an amount of data to the target machine
  public void sendData(byte[] data) throws SocketException, IOException, InterruptedException{
   
    //Data to put in Application-Header
    String method = "inform"; //TODO: need to allow client program to specify method
    String hostName = getHostName();
    String hostIP = getIPAddress();
    boolean SEQ = false; //Sequence number (0/false or 1/true
    boolean EOM = false; //End of message flag (0-->more data soon, 1-->last packet)
    
    //Application-Header String
    String CRLF = "\r\n";
    String firstRow = method + " " + hostName + " " + hostIP + CRLF;
    
    //Amounts of data to send
    int headerChars = firstRow.length() + "0 0\r\n".length() + CRLF.length();
    int availableBufSize = bufSize - headerChars; //This is the only amount we can send. If less than 0, no data can be sent at all
    if(availableBufSize <= 0){
      System.out.println("No data can be sent using the current configuration -- Increase Buffer Size / MTU");
      return;
    }
    
    //Send data!
    ByteArrayInputStream byteStream = new ByteArrayInputStream(data);
    int packetNum = 0;
    
    System.out.println("Starting message!");
    while(byteStream.available()>0){ //While data remains
      byte[] appDataBuffer = new byte[availableBufSize]; //Read data into buffer
      int bytesRead = byteStream.read(appDataBuffer);
      
      //If entire buffer not filled, only get array containing data
      if(bytesRead< availableBufSize){ 
        appDataBuffer = Arrays.copyOf(appDataBuffer, bytesRead);
      }
      
      //If there are no more bytes, set EOM flag
      if(byteStream.available()==0) EOM = true;
      
      //Create packet's exact data
      int packetSize = headerChars + bytesRead;
      String secondRow = "" + (SEQ?1:0) + " " + (EOM?1:0) + CRLF;
      byte[] packetData = (firstRow + secondRow + new String(appDataBuffer) + CRLF).getBytes();
      
      //Send packet
      System.out.println(
              "\n\n| - - - - START PACKET (" + packetNum++ + ") - - - - - - - - |\n"
              + new String(packetData) 
              + "|- - - - END PACKET (total length: " + packetSize + ") - - - - -| \n");
      DatagramPacket packet = new DatagramPacket(packetData, packetData.length, targetIPAddr, targetPort);
      socket.send(packet);
      SEQ = !SEQ;
      
      Thread.sleep(1200);
    }
    
    System.out.println("Full message sent!");
  }
*/  
}
