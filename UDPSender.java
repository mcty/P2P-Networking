


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * The client to the server. Sends request to the server.
 * 
 * @author Austin
 */
public class UDPSender extends Host{
  private boolean DEBUG = true;
  
  //Target machine info
  private int targetPort = 0;
  private InetAddress targetIPAddr = null;
  
  //Host machine info
  private int hostport = 25565;
  
  //Data sending info
  private int bufSize = 5;  //MTU - Maximum Tranmission Unit - Number of bytes that can be sent at once
  private DatagramSocket socket = null; //The socket through which data is being sent to receiver
  
  public UDPSender(){
    super();
  }
  
  //Sets MTU of sender
  public void setBufferSize(int size){
    if(size > 0) bufSize = size;
  }
  
  //Prepares the sender to start sending
  public void startSender(byte[] targetAddress, int targetPort) throws SocketException, UnknownHostException{
    socket = new DatagramSocket(hostport);  //Create socket to send out of
    targetIPAddr = InetAddress.getByAddress(targetAddress); //Get IP of target
    this.targetPort = targetPort; 
  }
  
  //Closes the socket, stops the transmission of data
  public void stopSender(){
    if(socket!=null) socket.close();
  }
  
  //Send an amount of data to the target machine
  public void sendData(byte[] data) throws SocketException, IOException, InterruptedException{
    
    String method = "inform";
    ByteArrayInputStream byteStream = new ByteArrayInputStream(data);
    int packetNum = 0;
    
    
    while(byteStream.available()>0){
      byte[] packetData = new byte[bufSize];
      int bytesRead = byteStream.read(packetData);
      if(bytesRead<bufSize){
        packetData = Arrays.copyOf(packetData, bytesRead);
      }
        System.out.println("Sending packet (" + packetNum++ + "): '"
                + new String(packetData));
        DatagramPacket packet = new DatagramPacket(packetData, packetData.length, targetIPAddr, targetPort);
        socket.send(packet);
        Thread.sleep(1200);
    }
    
    System.out.println("Full message sent!");
  }
}
