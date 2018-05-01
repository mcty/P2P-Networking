
package p2p;

public class PeerData {
   private String IP;
   private String hostname;
   private int port;
   private String currentRequest;

    public PeerData(String peerIP, String peerHostname, int peerPort){
        IP = peerIP;
        hostname = peerHostname;
        port = peerPort;
        currentRequest = "";
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
    
    public void addToRequest(String data){
        currentRequest+= data;
    }
    
    public void finishRequest(){
        currentRequest="";
    }
    
    public String getRequest(){
        return currentRequest;
    }
    
}
