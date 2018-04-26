package p2p;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

//Contains functionality shared between servers and senders
public abstract class Host extends Thread{
  public final static boolean DEBUG = true;
  public final static String CRLF = "\r\n";
  public final static int MTU = 128; //Maximum Tranmission Unit - Number of bytes that can be sent at once
  public final static int MSS = MTU - 8 - 20; //Maximum Segment Size - The number of bytes that the payload is allowed to be (accounts for UDP and IP headers, 8 bytes and 20 bytes respectively)
  private String IPAddress = null;
  private String hostName = null;
  
  public Host(){
    this("default");
  }
  
  public Host(String name){
    super(name);
    getIPAddress(); //Get IPAddress
    getHostName();  //Get Hostname
    if(DEBUG){
      System.out.println("\t>>Created Sender with IP " + getIPAddress());
      System.out.println("\t>>Created Sender with hostname " + getHostName()+"\n");
    }
    
  }
  
  //Get the IP address of the host
  public final String getIPAddress(){
    //If IP address is not currently set, set it, then return it
    if(IPAddress == null){
      try{
        IPAddress = InetAddress.getLocalHost().getHostAddress();
      }
      catch(Exception e){
        IPAddress = null;
        e.printStackTrace();
      }
    }
    return IPAddress;
  }
  
  //Get the hostname of the host
  public final String getHostName(){
    //If hostname is not currently set, set it, then return it
    if(hostName == null){ 
      hostName = "";
      String line;        //Holds each line
      ProcessBuilder pb;  //Used to build procress being ran
      Process pr;         //The actual process
      BufferedReader result; //Reader to read from process' input stream
      
      try{
        //Create process to execute 'hostname' process locally & run it
        pb = new ProcessBuilder("hostname");
        pb.redirectErrorStream(true); //Send standard output and error output to same stream
        pr = pb.start(); //Run process

        //Get input
        result = new BufferedReader(new InputStreamReader(pr.getInputStream()));
        while((line = result.readLine())!=null){ hostName += line;}
        pr.waitFor();   //Wait until process is complete
        result.close(); //Close reader
        }
      catch(Exception e){
        e.printStackTrace();
        hostName = null;
      }
    }
    return hostName;
  }
  
  //Encode a string so that requests conform to format
  //i.e. replace special characters with placeholders
  public final String encodeString(String str){
    return str.replaceAll(" ","%20");
  }
  
  //Decode string to original form
  public final String decodeString(String str){
    return str.replaceAll("%20"," ");
  }
  
  /* MESSAGING SENDING AND RECEIVING FUNCTIONALITY */
  
    public String performMessage(byte[] data, String[] headerData, 
            InetAddress targetIPAddr, int targetPort, DatagramSocket socket){
      //Message data
      ByteArrayInputStream byteStream = new ByteArrayInputStream(data); //Stream of full-message payload 

      //Packet specific data
      boolean SEQ = false; //SEQ flag of message, start with SEQ = 0
      String constantHeaders = createHeaders(headerData); //Create app-header data
      DatagramPacket currentPacket = null;                //Current packet being sent
      int packetNum = 0;  //Current packet number being sent

      while(byteStream.available()>0){ //While data still needs to be sent..
        //Create packet of next data
        currentPacket = createPacket(byteStream, constantHeaders, SEQ,
                targetIPAddr, targetPort); //Create packet with rdt app-headers and headers specific to required format
        if(currentPacket == null){ return null; } //If no packet created, packet size is an issue. Don't send null packet.

        //Print packet data
        byte[] packetData = currentPacket.getData();
        System.out.println(
                "\n\n| - - - - START PACKET (" + packetNum++ + ") - - - - - - - - |\n"
                + new String(packetData) 
                + "|- - - - END PACKET (total length: " + packetData.length + ") - - - - -| \n");

        //Send packet
        rdt_send(socket, currentPacket, SEQ);
        SEQ = !SEQ;
        try{ //Give time so that everything doesn't happen too quickly
          Thread.sleep(1200);
        }catch(Exception e){
          System.out.println("Unable to have thread wait");
          e.printStackTrace();
        }
      }
      System.out.println("Full message sent!");
      System.out.println("Waiting for server response..");
      return getResponse(); //Get response from destination machine after server processes
  }
  
  //Creates application headers specific to the class sending the packets
  abstract public String createHeaders(String[] params);
  
  //Creates packets
  private DatagramPacket createPacket(ByteArrayInputStream byteStream, 
          String appHeaders, boolean SEQ, InetAddress targetIPAddr,
          int targetPort){
    //Other application-header data
    boolean EOM = false; //End of Message flag. True when last packet of message
    
    //Amounts of data to send
    int headerChars = appHeaders.length() + "0 0\r\n".length() + CRLF.length();
    int availableBufSize = MSS - headerChars; //This is the only amount we can send. If less than 0, no data can be sent at all
    if(availableBufSize <= 0){
      System.out.println("No data can be sent using the current configuration -- Increase Buffer Size / MTU");
      return null;
    }
    
    //Data to send from byte stream
    byte[] appDataBuffer = new byte[availableBufSize];
    int bytesRead = 0;
    try{
      bytesRead = byteStream.read(appDataBuffer);
    }catch(Exception e){
      System.out.println("Could not read data from input stream to buffer");
      e.printStackTrace();
    }
    
    //If entire buffer not filled, only get array containing data
      if(bytesRead< availableBufSize){ 
        appDataBuffer = Arrays.copyOf(appDataBuffer, bytesRead);
      }
      
      //If there are no more bytes, set EOM flag (end of message flag)
      if(byteStream.available()==0) EOM = true;
    
      //Create packet's exact data
      //int packetSize = headerChars + bytesRead;
      String secondRow = "" + (SEQ?1:0) + " " + (EOM?1:0) + CRLF;
      byte[] packetData = (appHeaders + secondRow + new String(appDataBuffer) + CRLF).getBytes();
      return new DatagramPacket(packetData, packetData.length, targetIPAddr, targetPort);
  }
  
  private void rdt_send(DatagramSocket socket, DatagramPacket packet, 
          boolean SEQ){
    //Send
      try{
        socket.send(packet);
      }catch(Exception e){
        System.out.println("Could not send packet");
        e.printStackTrace();
        return;
      }
      
      //Wait for correct ACK
      ACKTimer timer = new ACKTimer(socket, packet, 1000); //Timer that will resend packet
      byte[] ACKdata = { (byte)(SEQ?0:1) }; //Where ACK data will be placed, intialize to incorrect ACK value (which would be the value not equal to SEQ since we need ACK corresponding to sent packet)
      DatagramPacket ack = new DatagramPacket(ACKdata, 1); //ACK packet
      timer.start(); //Start timer
      try{
          while(ACKdata[0] != (SEQ?1:0)){ //While incorrect ACK data, wait for correct ACK (this prevents delayed ACKS from affecting system)
            socket.receive(ack); //Constantly receive acks until we get the correct one
            System.out.println("Got ACK:");
            System.out.println("\tACK Data Expected:" + (SEQ?1:0) + "\tAck Data Got " + ACKdata[0]);
          }
          System.out.println("Correct ACK received, continue sending data.\n");
      }catch(Exception e){
        System.out.println("Could not receive ACK for server");
        e.printStackTrace();
      }
      timer.stop(); //End timer after correct ACK
  }
  
  //Get response from destination, and make sure response is received reliably (i.e. send ACKs)
  abstract public String getResponse();
  
}