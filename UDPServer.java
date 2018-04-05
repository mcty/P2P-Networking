


import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;
import java.util.*;


/**
 * The server of the application. Will be expecting packets sent from peers.
 * 
 * @author Austin
 */
public class UDPServer extends Host {
  private int port;
  private int bufSize = 5; //128 bytes
  private DatagramSocket receivingSocket = null;
  LinkedList<String> linkedlist = new LinkedList<String>();
  
  
  public UDPServer(String name, int port){
    super(name);
    this.port = port;
  }
  
  public void stopListening(){
    if(receivingSocket!=null) receivingSocket.close();
    System.out.println("Server socket was closed");
  }
  
  public void printDataReceived(String message){
	System.out.println("Received data packet with data: '" + message + "'");
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
		
		messageT = messageT + new String(packetData);
		
		//linkedlist.add(new String(packetData));
		
        printDataReceived(messageT);
        
      }
    }
    catch(Exception e){
      stopListening();
      e.printStackTrace();
    }
  }
}
