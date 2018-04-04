

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Scanner;

/**
 *
 * @author Austin
 */
public class Main {
  public static final boolean DEBUG = true;
  
  public static void main(String[] args){
    Scanner scan = new Scanner(System.in);
    boolean isServer;
    
    //What is the current process?
    System.out.println("Is the current process a 'server' or a 'sender'?");
    switch(scan.next()){
      case "server": isServer = true; break;
      default: isServer = false;
    }
    System.out.println("Okay, current process is a " + (isServer?"server":"sender"));
    
    //Run process specific to sender/receiver
    if(isServer) serverRoutine(scan);
    else senderRoutine(scan);
  }
  
  private static void senderRoutine(Scanner scan){
    int port;
    String hostname;
    byte[] localIPAddress;
    
    try{
        //Get hostname (and IP)
        hostname = getHostName();
        System.out.println("Hello host " + hostname);
      
        //Get port
        System.out.println("What is the target's port?");
        port = scan.nextInt();
        
        //Create sender
		byte[] targetAddress = {127,0,0,1};
        //byte[] targetAddress = {10,8,101,(byte)172};
        UDPSender sender = new UDPSender();
        sender.startSender(targetAddress, port);
        
        //Send data until 'stop'
        String data;
        while( !(data = scan.nextLine()).equals("stop")){
          sender.sendData(data.getBytes());
        }
      }
      catch(Exception e){
        e.printStackTrace();
      }
  }
  
  private static String getHostName(){
    String hostname = "";
    String temp = null;
    ProcessBuilder pb;
    Process pr;
    
    try{
      //Create process to run 'hostname' locally
      pb = new ProcessBuilder("hostname");
      pb.redirectErrorStream(true); //Send standard output and error output to same stream
      pr = pb.start(); //Run process

      //Get input
      BufferedReader result = new BufferedReader(new InputStreamReader(pr.getInputStream()));
      while((temp = result.readLine())!=null){ hostname += temp;}
      pr.waitFor(); //Wait until process is complete (should be quick)
      result.close(); //Close reader
      }
    catch(Exception e){
      e.printStackTrace();
    }
    return hostname;
  }
  private static void serverRoutine(Scanner scan){
      String serverName;
      int port;
      
      System.out.println("What is the server's name?");
      serverName = scan.next();
      System.out.println("What is the server's port?");
      port = scan.nextInt();
      
      UDPServer server = new UDPServer(serverName, port);
      server.start();
      
      System.out.println("Okay, server has been set up!");
      while(!scan.next().equals("stop")); //Just wait until told to stop
      server.stopListening();
  }
}
