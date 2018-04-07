
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Scanner;

/**
 *
 * @author Austin
 */
public class Main {
  public static final boolean DEBUG = true;
  
  public static void main(String[] args){
    Scanner scan = new Scanner(System.in);
    boolean isServer = false;
    boolean isSimpleSender = false;
    
    //What is the current process?
    System.out.println("Is the current process a 'server' or a 'sender' (use 'simple-sender' to send data without packets)?");
    switch(scan.next()){
      case "server": isServer = true; break;
      case "simple-sender": isSimpleSender = true; break;
    }
    System.out.println("Okay, current process is a " + (isServer?"server":"sender"));
    
    //For testing only
    if(isSimpleSender){
      simpleSenderRoutine(scan);
    }
    
    //Run process specific to sender/receiver
    if(isServer) serverRoutine(scan);
    else senderRoutine(scan);
  }
  
  private static void senderRoutine(Scanner scan){
    int targetPort, senderPort;
    String data;
    
    try{
        UDPSender sender = new UDPSender(); //Create inital sender
        System.out.println("Hello host " + sender.getHostName());
        
        System.out.println("What is the target's port (port data is read from on server side)?"); //Get target port
        targetPort = scan.nextInt();
        
        System.out.println("What is the host's port (port data is sent from and data received by client)?");
        senderPort = scan.nextInt();
        sender.setHostPort(senderPort);
        
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
      System.out.println("What is the server's port (the port being listened to)?");
      port = scan.nextInt();
      
      UDPServer server = new UDPServer(serverName, port);
      server.start();
      
      System.out.println("Okay, server has been set up!");
      while(!scan.next().equals("stop")); //Just wait until told to stop
      server.stopListening();
  }
  
  private static void simpleSenderRoutine(Scanner scan){
    int sourcePort, targetPort;
    
    //IP
    System.out.println("What is the target's IP address?"); //Get target IP
        byte[] targetAddress = new byte[4];
        for(int i = 0; i < 4; i++) targetAddress[i] = (byte)(scan.nextInt());
    
    //Source port
    System.out.println("What is the host's port (port data is sent from and data received by client)?");
    sourcePort = scan.nextInt();
    
    //Target port
    System.out.println("What is the target's port (port data is read from on server side)?"); //Get target port
    targetPort = scan.nextInt();
    
    //Socket
    DatagramSocket socket;
    try{
      socket = new DatagramSocket(sourcePort);
    }catch(Exception e){
      socket = null;
      e.printStackTrace();
    }
    
    //Send exact data until 'stop'
    int data;
    while((data = scan.nextInt())!=-1){
      try{
        byte[] a = {(byte)data};
        socket.send(new DatagramPacket(a,1, InetAddress.getByAddress(targetAddress), targetPort));
      }
      catch(Exception e){
        e.printStackTrace();
      }
    }
    
    socket.close();
  }
}
