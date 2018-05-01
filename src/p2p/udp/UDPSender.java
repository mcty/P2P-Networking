package p2p.udp;

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
public class UDPSender extends Host {

    private boolean DEBUG = true;

    //Target machine info
    private int targetPort = 0;
    private InetAddress targetIPAddr = null;

    //Host machine info
    private int hostPort = 25565;

    //Data sending and receiving
    private int bufSize = Host.MSS;  //Set to comply with Maximum Segment Size specified in requirements
    private DatagramSocket socket = null; //The socket through which data is being sent to receiver
    private ACKTimer timer;
    
    public UDPSender() {
        super();
    }

    //Sets MTU of sender
    public void setBufferSize(int size) {
        if (size > 0) {
            bufSize = size;
        }
    }

    //Prepares the sender to start sending
    public void startSender(byte[] targetAddress, int targetPort) throws SocketException, UnknownHostException {
        socket = new DatagramSocket(hostPort);  //Create socket to send out of
        targetIPAddr = InetAddress.getByAddress(targetAddress); //Get IP of target
        this.targetPort = targetPort;
        timer = new ACKTimer(socket);
    }

    //Closes the socket, stops the transmission of data
    public void stopSender() {
        if (socket != null) {
            socket.close();
        }
    }

    //Using a list of filenames and filesizes, creates the payload for the inform and update message, then sends it
    public String sendInformAndUpdate(ArrayList<String> fileNames, ArrayList<Long> fileSizes)
            throws SocketException, IOException, InterruptedException {
        String payload = ""; //Payload of the request (meaning the headers)

        //Create payload
        //Follows the format: "Filename: filename filesize<CRLF>Filename: filename filesize<CRLF>" (any number of files
        for (int currentFile = 0; currentFile < fileNames.size(); currentFile++) {
            payload += "Filename: " + encodeString(fileNames.get(currentFile)) + " " + fileSizes.get(currentFile) + CRLF;
        }
        if(payload.length() == 0) payload = "\n"; //Allows for empty 'inform and updates' to remove files
        
        //sendData(payload.getBytes(), "inform"); //Send
        return performMessage(payload.getBytes(), new String[]{"inform"});
    }

    //Given a query string, creates the formatted payload for query message and sends it
    public String sendQuery(String query) throws SocketException,
            IOException, InterruptedException {
        String payload = "";

        //Create payload
        //Follows the format: "Search-term: query<CR><LF>". Currently, only supports one term
        payload = "Search-term: " + encodeString(query) + CRLF;

        //sendData(payload.getBytes(),"query");
        return performMessage(payload.getBytes(), new String[]{"query"});
    }

    //Sends exit message
    public String sendExit() throws SocketException, IOException,
            InterruptedException {
        //sendData("*".getBytes(),"exit");
        return performMessage("*".getBytes(), new String[]{"exit"});

    }
    
    //Other messages
    public String sendUnknown(String type) throws SocketException,
            IOException, InterruptedException{
        return performMessage("*".getBytes(), new String[]{type});
    }

    private String performMessage(byte[] payload, String[] headerData) {
        
        return performMessage(payload, headerData, targetIPAddr, targetPort, socket);
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

      //While data still needs to be sent..
      while(byteStream.available()>0){ 
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
        rdt_send(socket, currentPacket, SEQ, timer);
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
  
  private void rdt_send(DatagramSocket socket, DatagramPacket packet, 
          boolean SEQ, ACKTimer timer){
      long startTime, endTime;
      //Send
      try{
        socket.send(packet);
      }catch(Exception e){
        System.out.println("Could not send packet");
        e.printStackTrace();
        return;
      }
      
      //Wait for correct ACK
      byte[] ACKdata = { (byte)(SEQ?0:1), 0, 0}; //Where ACK data will be placed, intialize to incorrect ACK value (which would be the value not equal to SEQ since we need ACK corresponding to sent packet)
      DatagramPacket ack = new DatagramPacket(ACKdata, 3); //ACK packet
      timer.setPacketToResend(packet);
      startTime = System.currentTimeMillis();
      timer.start(); //Start timer
      try{
          while(ACKdata[0] != (SEQ?1:0)){ //While incorrect ACK data, wait for correct ACK (this prevents delayed ACKS from affecting system)
            socket.receive(ack); //Constantly receive acks until we get the correct one
            System.out.println("Got ACK:");
            System.out.println("\tACK Data Expected:" + (SEQ?1:0) + "\tAck Data Got " + ACKdata[0]);
            
            //We may have received a response to the complete BEFORE the ACK for the last packet.
            //This occurs with when the last ACK is lost or delayed, while the response is sent propely and faster.
            String potentialStatusCode = new String(ACKdata);
            if(potentialStatusCode.equals("200") || potentialStatusCode.equals("400")){
                break; //If Status code in ACK packet, get out of function, since full message has been received properly.
            }
          }
          System.out.println("Correct ACK received, continue sending data.\n");
      }catch(Exception e){
        System.out.println("Could not receive ACK for server");
        e.printStackTrace();
      }
      endTime = System.currentTimeMillis();
      timer.stop(); //End timer after correct ACK
      timer.updateInterval((int)(endTime-startTime));
  }
    
    public String createHeaders(String[] params) {
        if (params == null) {
            params = new String[]{"exit"};
        }
        String hostName = getHostName();
        String hostIP = getIPAddress();
        String method = params[0];

        String headers = method + " " + hostName + " " + hostIP + CRLF;
        return headers;
    }

    //Function handles the response being sent from the server to the peer
    //Specifically, how that response is reliable
    public String getResponse() {
        int EOM, SEQ, prevSEQ = 1;
        byte[] buffer, packetData;
        String fullMessage = "", packetString;
        String[] packetStringSplit;
        DatagramPacket packet;
        int packetDataLength = 0;
        String statusCode, statusPhrase, userData;

        try {
            do {
                //Create packet to receive
                buffer = new byte[MSS];
                packet = new DatagramPacket(buffer, MSS);
                
                //If we ever get a delayed ACK from the server, we ignore it here
                do{
                socket.receive(packet); //Receive packet
                System.out.println("Packet length: " + packet.getLength());
                }while(packet.getLength() == 1);
                packetData = Arrays.copyOf(packet.getData(), packet.getLength());
                packetDataLength = packet.getLength();
                
                
                //Get packet data (knowing for sure it isn't an ACK)
                System.out.println("TTS: Socket info: " + packet.getSocketAddress() + "");
                packetString = new String(packet.getData());
                packetStringSplit = packetString.split(" |" + CRLF, 5);

                //Parse packet data
                statusCode = packetStringSplit[0];
                statusPhrase = packetStringSplit[1];
                SEQ = Integer.parseInt(packetStringSplit[2]);
                EOM = Integer.parseInt(packetStringSplit[3]);
                userData = packetStringSplit[4];
                userData = userData.substring(0, userData.length() - 2);

                if (SEQ != prevSEQ) {
                    fullMessage += userData;
                    prevSEQ = SEQ;
                }

                sendACK(socket, SEQ, packet.getAddress(), packet.getPort());
                packet = null;
            } while (EOM != 1);

            fullMessage = statusCode + " " + statusPhrase + CRLF + decodeString(fullMessage);
            return fullMessage;
        } catch (Exception e) {
            System.out.println("Could not receive packet");
            e.printStackTrace();
            return null;
        }
    }

    //Send ACK
    public void sendACK(DatagramSocket socket, int SEQ, InetAddress SenderIP, int SenderPort) throws SocketException, IOException, InterruptedException {
        String ACKData = "ACK " + hostname + " " + IPAddress + CRLF + SEQ + CRLF;    //ACK in format to identify origin
        byte[] packetData = ACKData.getBytes(); //Packet data
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
