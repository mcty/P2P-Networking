

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
    int targetPort;
    String data;
    
    try{
        UDPSender sender = new UDPSender(); //Create inital sender
        System.out.println("Hello host " + sender.getHostName());
        
        System.out.println("What is the target's port?"); //Get target port
        targetPort = scan.nextInt();
        
        System.out.println("What is the target's IP address?"); //Get target IP
        byte[] targetAddress = new byte[4];
        for(int i = 0; i < 4; i++) targetAddress[i] = (byte)(scan.nextInt());
        //byte[] targetAddress = {10,8,101,(byte)172};
        
        //Send data until 'stop'
        sender.startSender(targetAddress, targetPort);
        while( !(data = scan.nextLine()).equals("stop")){
          sender.sendData(data.getBytes());
        }
      }
      catch(Exception e){
        e.printStackTrace();
      }
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
