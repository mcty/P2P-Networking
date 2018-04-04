


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
public class UDPSender {
  private int receiverPort = 0;
  private int bufSize = 5;
  private int hostport = 25565;
  private DatagramSocket socket = null;
  private InetAddress IPAddress = null;
  
  public UDPSender(){
    
  }
  
  public void setBufferSize(int size){
    if(size > 0) bufSize = size;
  }
  
  public void startSender(byte[] targetAddress, int receiverPort) throws SocketException, UnknownHostException{
    socket = new DatagramSocket(hostport);
    IPAddress = InetAddress.getByAddress(targetAddress);
    this.receiverPort = receiverPort;
  }
  
  public void stopSender(){
    if(socket!=null) socket.close();
  }
  
  public void sendData(byte[] data) throws SocketException, IOException, InterruptedException{
    
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
        DatagramPacket packet = new DatagramPacket(packetData, packetData.length, IPAddress, receiverPort);
        socket.send(packet);
        Thread.sleep(1200);
    }
    
    System.out.println("Full message sent!");
  }
}
