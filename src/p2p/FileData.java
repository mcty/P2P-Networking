
package p2p;

public class FileData {
    private String path;
    private long size;
    private String hostPeerIP;
    private long hostPeerPort;
    
    public FileData(String path, long size, String hostPeerIP, long hostPeerPort){
        this.path = path;
        this.size = size;
        this.hostPeerIP = hostPeerIP;
        this.hostPeerPort = hostPeerPort;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getHostPeerIP() {
        return hostPeerIP;
    }

    public void setHostPeerIP(String hostPeerIP) {
        this.hostPeerIP = hostPeerIP;
    }

    public long getHostPeerPort() {
        return hostPeerPort;
    }

    public void setHostPeerPort(long hostPeerPort) {
        this.hostPeerPort = hostPeerPort;
    }
    
    
}
