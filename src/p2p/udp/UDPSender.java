package p2p.udp;

import p2p.*;
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
  public String sendInformAndUpdate(ArrayList<String> fileNames, ArrayList<Long> fileSizes) 
          throws SocketException, IOException, InterruptedException{
    String payload = ""; //Payload of the request (meaning the headers)
    String CRLF = "\r\n";
    
    //Create payload
    //Follows the format: "Filename: filename filesize<CRLF>Filename: filename filesize<CRLF>" (any number of files
    for(int currentFile = 0; currentFile < fileNames.size(); currentFile++){
      payload+= "Filename: " + encodeString(fileNames.get(currentFile))+ " " + fileSizes.get(currentFile) + CRLF;
    }
    
    //sendData(payload.getBytes(), "inform"); //Send
    return performMessage(payload.getBytes(), new String[]{"inform"});
  }
  
  //Given a query string, creates the formatted payload for query message and sends it
  public String sendQuery(String query) throws SocketException, 
          IOException, InterruptedException{
    String payload = "";
    String CRLF = "\r\n";
    
    //Create payload
    //Follows the format: "Search-term: query<CR><LF>". Currently, only supports one term
    payload = "Search-term: " + encodeString(query) + CRLF;
    
    //sendData(payload.getBytes(),"query");
    return performMessage(payload.getBytes(),new String[]{"query"});
  }
  
  //Sends exit message
  public String sendExit() throws SocketException, IOException, 
          InterruptedException{
    //sendData("*".getBytes(),"exit");
    return performMessage("*".getBytes(), new String[]{"exit"});

  }
  
  private String performMessage(byte[] payload, String[] headerData){
      return performMessage(payload, headerData, targetIPAddr, targetPort, socket);
  }
  
  public String createHeaders(String[] params){
    if(params == null){ params = new String[]{"exit"}; }
    String hostName = getHostName();
    String hostIP = getIPAddress();
    String method = params[0];
    
    String headers = method + " " + hostName + " " + hostIP + CRLF;
    return headers;
  }
  
  //Function handles the response being sent from the server to the peer
  //Specifically, how that response is reliable
  public String getResponse(){
      int EOM, SEQ, prevSEQ = 1;
      byte[] buffer, packetData;
      String fullMessage = "", packetString;
      String[] packetStringSplit;
      DatagramPacket packet;
      
      String statusCode, statusPhrase, userData; 
      
      try{
        do{
          //Create packet to receive
          buffer = new byte[MSS];
          packet = new DatagramPacket(buffer, MSS);
          socket.receive(packet);
          
          //Get packet data
          packetData = Arrays.copyOf(packet.getData(), packet.getLength());
          System.out.println("TTS: Socket info: "+ packet.getSocketAddress() +"");
          packetString = new String(packet.getData());
          packetStringSplit = packetString.split(" |"+CRLF, 5);
          
          //Parse packet data
          statusCode = packetStringSplit[0];
          statusPhrase = packetStringSplit[1];
          SEQ = Integer.parseInt(packetStringSplit[2]);
          EOM = Integer.parseInt(packetStringSplit[3]);
          userData = packetStringSplit[4];
          userData = userData.substring(0,userData.length()-2);
          
          if(SEQ != prevSEQ){
              fullMessage += userData;
              prevSEQ = SEQ;
          }
          
          sendACK(socket, SEQ, packet.getAddress(), packet.getPort());
        }while(EOM != 1);
        
        fullMessage = statusCode + " " + statusPhrase + CRLF + fullMessage;
        return fullMessage;
      }
      catch(Exception e){
          System.out.println("Could not receive packet");
          return null;
      }
  }
  
  //Send ACK
  public void sendACK(DatagramSocket socket, int SEQ, InetAddress SenderIP, int SenderPort)throws SocketException, IOException, InterruptedException{
	  byte[] packetData = {(byte)SEQ};
	  DatagramPacket packet = new DatagramPacket(packetData, packetData.length, SenderIP, SenderPort);
             socket.send(packet);
	  System.out.println("ACK Sent: " + SEQ);
  }
  
  /* GETTERS/SETTERS */
  public void setTargetPort(int targetPort) {
    this.targetPort = targetPort;
  }

  public void setHostPort(int hostPort) {
    this.hostPort = hostPort;
  }
}
