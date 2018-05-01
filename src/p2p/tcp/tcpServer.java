/*
 * TCP server tcpServer
 * 0.0.1
 * Tyler MacNeil
 */
package p2p.tcp;

import java.io.*;
import java.net.*;

public class tcpServer extends Thread{
    
    private int port;
	private String fname;
    
    public tcpServer(String name, int port, String fname){
        super(name);
        this.port = port;
		this.fname = fname;
    }
    
    //Start thread to begin listening
    @Override
    public void run(){
        try{
		//Initialize Sockets
        System.out.println("tcpServer Thread started for file: " + fname + " " + port);
        ServerSocket sssock = new ServerSocket(port);
        Socket socket = sssock.accept();
        
        //The InetAddress specification
        InetAddress IA = InetAddress.getLocalHost(); 
        
        //Specify the file
        File file = new File(fname);
        FileInputStream fis = new FileInputStream(file);
        BufferedInputStream bis = new BufferedInputStream(fis); 
          
        //Get socket's output stream
        OutputStream os = socket.getOutputStream();
                
        //Read File Contents into contents array 
        byte[] contents;
        long fileLength = file.length(); 
        long current = 0;
         
        
        while(current!=fileLength){ 
            int size = 10000;
            if(fileLength - current >= size)
                current += size;    
            else{ 
                size = (int)(fileLength - current); 
                current = fileLength;
            } 
            contents = new byte[size]; 
            bis.read(contents, 0, size); 
            os.write(contents);
            if(((current*100)/fileLength)%10 == 0){
				System.out.println("Sending file ... "+(current*100)/fileLength+"% complete!");
			}
        }   
        
        os.flush(); 
        //File transfer done. Close the socket connection!
        socket.close();
        sssock.close();
        Thread.currentThread().interrupt();
        System.out.println("File sent succesfully!");
        
        
		} catch(Exception e){
			e.printStackTrace();
		}
    }
}
