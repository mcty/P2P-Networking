


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
  private int bufSize = 128;  //MTU - Maximum Tranmission Unit - Number of bytes that can be sent at once
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
}
