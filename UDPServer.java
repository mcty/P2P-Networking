


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
}
