/*
 * TCP client tcpClient
 * 0.0.1
 * Tyler MacNeil
 */

package p2p.tcp; 

import java.io.*;
import java.net.*;

public class tcpClient extends Thread{
    
    private int serverPort;
    String serverIP;
	String fname;
	String dest;
	String saveName;
    
    public tcpClient(String serverIP, int serverPort,String fname,String dest, String saveName){
        this.serverIP = serverIP;
        this.serverPort = serverPort;
		this.fname = fname;
		this.dest = dest;
		this.saveName = saveName;
    }
    
    //Start thread to connect and begin sending
    public void run(){
		
		boolean downloadFile = false;
		try{
		//Initialize Handshake socket
        System.out.println("Trying to Connect to socket...");
		Socket socket = new Socket(serverIP, 5009);
        //byte[] contents = new byte[10000];
		
		//Send request
		DataOutputStream outToServer = new DataOutputStream(socket.getOutputStream());
		BufferedReader inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		outToServer.writeBytes(fname + "\n");
		outToServer.writeBytes(serverPort + "\n");
		
		String reply, answer;
		
		reply = inFromServer.readLine();
		answer = inFromServer.readLine();
		if(answer.equals("true")){
			System.out.println("Successful ping for: " + reply);
			downloadFile = true;
		}
		else{
			System.out.println("Unsuccessful ping for: " + reply);
			
		}
		outToServer.close();
		inFromServer.close();
		socket.close();
		} catch (Exception e){
			e.printStackTrace();
		}
		if(downloadFile){
			try{
				Thread.sleep(3000);
				//Initialize Transfer socket
				System.out.println("Trying to get file...");
				Socket ssocket = new Socket(serverIP, serverPort);
				byte[] contents = new byte[10000];
				System.out.println("Got to here!");
				//Initialize the FileOutputStream to the output file's full path.
				FileOutputStream fos = new FileOutputStream(dest + "/" + saveName);
				BufferedOutputStream bos = new BufferedOutputStream(fos);
				InputStream is = ssocket.getInputStream();
        
				//No of bytes read in one read() call
				int bytesRead = 0; 
        
				while((bytesRead=is.read(contents))!=-1)
					bos.write(contents, 0, bytesRead);
                                
                                
				bos.flush(); 
                                fos.close();
				ssocket.close(); 
        
				System.out.println("File saved successfully!");
			} catch (Exception e){
				e.printStackTrace();
			}
		}
    }
}