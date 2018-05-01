package p2p.udp;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

//Contains functionality shared between servers and senders
public abstract class Host{
  public final static boolean DEBUG = true;
  public final static String CRLF = "\r\n";
  public final static int MTU = 128; //Maximum Tranmission Unit - Number of bytes that can be sent at once
  public final static int MSS = MTU - 8 - 20; //Maximum Segment Size - The number of bytes that the payload is allowed to be (accounts for UDP and IP headers, 8 bytes and 20 bytes respectively)
  protected String IPAddress = null;
  protected String hostname = null;
  
  public Host(){
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
    if(hostname == null){ 
      hostname = "";
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
        while((line = result.readLine())!=null){ hostname += line;}
        pr.waitFor();   //Wait until process is complete
        result.close(); //Close reader
        }
      catch(Exception e){
        e.printStackTrace();
        hostname = null;
      }
    }
    return hostname;
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
  
  //Creates packets
  protected DatagramPacket createPacket(ByteArrayInputStream byteStream, 
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
  
  //Get response from destination, and make sure response is received reliably (i.e. send ACKs)
  public String getResponse(){return null;}
  
}