
package p2p;

import p2p.udp.ACKTimer;

public class PeerData {
   //Peer data
   private String IP;
   private String hostname;
   private int port;
   
   //Current request data
   private String currentRequest;
   private int expectedSEQ;
   private int packetCount;
   
   //Request related
   private ACKTimer timer;
   private int currentACKValue;
   
    public PeerData(String peerIP, String peerHostname, int peerPort){
        IP = peerIP;
        hostname = peerHostname;
        port = peerPort;
        currentRequest = "";
        expectedSEQ = 0;
        packetCount = 0;
        timer = null;
        currentACKValue = -1;
    }

    public String getIP() {
        return IP;
    }

    public void setIP(String IP) {
        this.IP = IP;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
    
    public int getExpectedSEQ(){
        return expectedSEQ;
    }
    
    public void updateExpectedSEQ(){
        if(expectedSEQ == 0) 
            expectedSEQ = 1;
        else
            expectedSEQ = 0;
    }
    
    public int getPacketCount(){
        return packetCount;
    }
    
    public void addToRequest(String data){
        currentRequest+= data;
        packetCount++;
    }
    
    public void finishRequest(){
        currentRequest="";
        expectedSEQ = 0;
        packetCount = 0;
    }
    
    public String getRequest(){
        return currentRequest;
    }
    
    public ACKTimer getTimer() {
        return timer;
    }

    public void setTimer(ACKTimer timer) {
        this.timer = timer;
    }
    
    public int getCurrentACKValue() {
        return currentACKValue;
    }

    public void setCurrentACKValue(int currentACKValue) {
        this.currentACKValue = currentACKValue;
    }
    
    public void print(){
        System.out.println("Peer: " + hostname
                + "\n IP: " + IP
                + "\n Port: " + port
        + "\nCurrentACK " + currentACKValue);
    }
}
