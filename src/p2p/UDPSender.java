package p2p;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
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
  private int hostPort = 25565;
  
  //Data sending and receiving
  private int bufSize = Host.MSS;  //Set to comply with Maximum Segment Size specified in requirements
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
    socket = new DatagramSocket(hostPort);  //Create socket to send out of
    targetIPAddr = InetAddress.getByAddress(targetAddress); //Get IP of target
    this.targetPort = targetPort; 
  }
  
  //Closes the socket, stops the transmission of data
  public void stopSender(){
    if(socket!=null) socket.close();
  }
  
  //Using a list of filenames and filesizes, creates the payload for the inform and update message, then sends it
  public void sendInformAndUpdate(ArrayList<String> fileNames, ArrayList<Long> fileSizes) 
          throws SocketException, IOException, InterruptedException{
    String payload = ""; //Payload of the request (meaning the headers)
    String CRLF = "\r\n";
    
    //Create payload
    //Follows the format: "Filename: filename filesize<CRLF>Filename: filename filesize<CRLF>" (any number of files
    for(int currentFile = 0; currentFile < fileNames.size(); currentFile++){
      payload+= "Filename: " + encodeString(fileNames.get(currentFile))+ " " + fileSizes.get(currentFile) + CRLF;
    }
    
    sendData(payload.getBytes(), "inform"); //Send
  }
  
  //Given a query string, creates the formatted payload for query message and sends it
  public void sendQuery(String query) throws SocketException, 
          IOException, InterruptedException{
    String payload = "";
    String CRLF = "\r\n";
    
    //Create payload
    //Follows the format: "Search-term: query<CR><LF>". Currently, only supports one term
    payload = "Search-term: " + encodeString(query) + CRLF;
    
    sendData(payload.getBytes(),"exit");
  }
  
  //Sends exit message
  public void sendExit() throws SocketException, IOException, 
          InterruptedException{
    sendData("*".getBytes(),"exit");
  }
  
  //Send an amount of data to the target machine
  public void sendData(byte[] data, String method) 
          throws SocketException, IOException, InterruptedException{
    //Managing RDT
    ACKTimer timer; //Used to resend packets when their ACKs are not received (covers corruption and packet loss)
    
    //Data to put in Application-Header
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
      
      //If there are no more bytes, set EOM flag (end of message flag)
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
    
    System.out.println("Full message sent!");
  }
  
  /* GETTERS/SETTERS */
  public void setTargetPort(int targetPort) {
    this.targetPort = targetPort;
  }

  public void setHostPort(int hostPort) {
    this.hostPort = hostPort;
  }
}
