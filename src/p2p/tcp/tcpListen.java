/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package p2p.tcp;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

/**
 *
 * @author Tyler PC
 */
public class tcpListen extends Thread{
    
    private int port;
    //private string fname;
    private ArrayList<String> servedFiles;
    
    public tcpListen(String name, int port, ArrayList<String> servedFiles){
        super(name);
        this.port = port;
        this.servedFiles = servedFiles;
    }
    
    public void run(){
        ServerSocket ssock = null;
        try{
            String result;
            String tsPort;
            boolean isAval = false;
            while(true){
                ssock = new ServerSocket(50007);
                System.out.println("Start of ServerLoop.");
                Socket clientConnectionSocket = ssock.accept();
                InetAddress IA = InetAddress.getByName("localhost");
                BufferedReader inFromClient = new BufferedReader(new InputStreamReader(clientConnectionSocket.getInputStream()));
                DataOutputStream outToClient = new DataOutputStream(clientConnectionSocket.getOutputStream());
                result = inFromClient.readLine();
                tsPort = inFromClient.readLine();
                if(result != null){
                    outToClient.writeBytes("File: " + result + " Port: " + tsPort + "\n");
                    System.out.println("Got ping for file: " + result + " on port " + tsPort);     //Remove this after testing possibly
                    for(int i=0;i<servedFiles.size();i++){
                        if(result.equals(servedFiles.get(i))){
                            isAval = true;
                        }
                    }
                    if(isAval){
                        outToClient.writeBytes("true" + "\n");
                        tcpServer serverThread = null;
                        serverThread = new tcpServer("tcpServer", Integer.parseInt(tsPort), result);
                        serverThread.start();
                    }
                    else{
                        outToClient.writeBytes("false" + "\n");
                    }
                }
                System.out.println("Socket Closed.");
                ssock.close();
            }
            
        } catch(Exception e){
            e.printStackTrace();
        }
        System.out.println("Stopped tcpListener. No longer serving files");
    }
}
