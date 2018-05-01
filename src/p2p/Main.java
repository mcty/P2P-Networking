package p2p;


import p2p.udp.UDPSender;
import p2p.udp.UDPServer;
import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Scanner;

/**
 *
 * @author Austin
 */
public class Main {

  public static final boolean DEBUG = true;

  public static void main(String[] args) {
    //Class.forName("");
    Scanner scan = new Scanner(System.in);
    boolean isServer = false;
    boolean isSimpleSender = false;

    //What is the current process?
    System.out.println("Is the current process a 'server' or a 'sender' (use 'simple-sender' to send data without packets)?");
    switch (scan.next()) {
      case "server":
        isServer = true;
        break;
      case "simple-sender":
        isSimpleSender = true;
        break;
    }
    System.out.println("Okay, current process is a " + (isServer ? "server" : "sender"));

    //For testing only
    if (isSimpleSender) {
      simpleSenderRoutine(scan);
    }

    //Run process specific to sender/receiver
    if (isServer) {
      serverRoutine(scan);
    } else {
      senderRoutine(scan);
    }
  }

    /* START SENDER FUNCTIONALITY */
  
  private static void senderRoutine(Scanner scan) {
    int targetPort, senderPort;
    String data; //User's input

    boolean isValid;

    try {
      UDPSender sender = new UDPSender(); //Create inital sender
      System.out.println("Hello host " + sender.getHostName());

      System.out.println("What is the target's port (the port that data is read from on server side)?"); //Get target port
      targetPort = scan.nextInt();

      System.out.println("What is the host's port (the port that data is sent from and data received by client)?"); //Get host port
      senderPort = scan.nextInt();
      sender.setHostPort(senderPort);

      System.out.println("What is the target's IP address?"); //Get target IP
      byte[] targetAddress = new byte[4];
      for (int i = 0; i < 4; i++) {
        targetAddress[i] = (byte) (scan.nextInt());
      }
      //byte[] targetAddress = {10,8,101,(byte)172};

      //Send data until user exits
      System.out.println("Okay, starting communication with server. Type 'exit' to end communication.\n");
      sender.startSender(targetAddress, targetPort); //Start sender
      while (true) {

        //Get message type, create and send message of that type
        data = scan.next().toLowerCase();
        scan.nextLine(); //Done to ignore newline
        switch (data) {
          case "inform":
            informAndUpdate(scan, sender);
            break;
          case "query":
            query(scan, sender);
            break;
          case "exit":
            exit(sender);
            break;
          default:
            System.out.println("Incorrect command. Use 'inform', 'query' or 'exit'");
        }
        
        if(data.equals("exit")){
          //exit code here...
          sender.stopSender();
          break; //Get out of while
        }
        else{
          System.out.println("What is your next message?");
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  //Perform an inform and update
  //a. Get list of file paths and file sizes from user that they are willing to share
  //b. Send this data to the UDPSender and allow it to handle sending the request
  private static void informAndUpdate(Scanner scan, UDPSender sender) 
          throws SocketException, IOException, InterruptedException{
    ArrayList<String> fileNames = new ArrayList<String>();
    ArrayList<Long>fileSizes = new ArrayList<Long>();
    File currentFile = null;
    String input;
    System.out.println("Insert the paths to the files that you are providing.\n"
            + "When you're done, hit enter without any text");
    
    //Get files paths and send when done
    while (!(input = scan.nextLine()).equals("")) {
      currentFile = new File(input); //Get file
      if(currentFile.exists()){ //If exists, add it and its size
        fileNames.add(input);
        fileSizes.add(currentFile.length()); 
      }
      else 
        System.out.println("File not found");
    }     
    String responseText = sender.sendInformAndUpdate(fileNames, fileSizes);
    System.out.println(responseText);
  }

  //Perform a query (single keyword)
  //a. Get user's query and send
  private static void query(Scanner scan, UDPSender sender) 
          throws SocketException, IOException, InterruptedException {
    String query;
    
    System.out.println("Insert your query");
    query = scan.nextLine();
    
    String responseText = sender.sendQuery(query);
    System.out.println(responseText);
    
  }

  //Perform exit
  //a. send exit message
  private static void exit(UDPSender sender) throws SocketException, 
          IOException, InterruptedException {
    String responseText = sender.sendExit();
    System.out.println(responseText);
  }
  /* END SENDER FUNCTIONALITY */

  
  /* START SERVER FUNCTIONALITY */
  
  private static void serverRoutine(Scanner scan) {
    String serverName;
    int port;

    System.out.println("What is the server's name?"); //Server's name
    serverName = scan.next();
    System.out.println("What is the server's port (the port being listened to)?"); //The port the server is listening to
    port = scan.nextInt();

    UDPServer server = new UDPServer(port); //Create and start the server itself
    server.run();

    System.out.println("Okay, server has been set up!");
    while (!scan.next().equals("stop")); //Just wait until told to stop
    server.close(); //Terminate server
  }

  /*END SERVER FUNCTIONALITY*/
  //Method exclusively for testing
  //Specifically for sending data to hosts without packet headers
  private static void simpleSenderRoutine(Scanner scan) {
    int sourcePort, targetPort;

    //IP
    System.out.println("What is the target's IP address?"); //Get target IP
    byte[] targetAddress = new byte[4];
    for (int i = 0; i < 4; i++) {
      targetAddress[i] = (byte) (scan.nextInt());
    }

    //Source port
    System.out.println("What is the host's port (the port that data is sent from and data received by client)?");
    sourcePort = scan.nextInt();

    //Target port
    System.out.println("What is the target's port (the port that data is read from on server side)?"); //Get target port
    targetPort = scan.nextInt();

    //Socket
    DatagramSocket socket;
    try {
      socket = new DatagramSocket(sourcePort);
    } catch (Exception e) {
      socket = null;
      e.printStackTrace();
    }

    //Send exact data until 'stop'
    int data;
    while ((data = scan.nextInt()) != -1) {
      try {
        byte[] a = {(byte) data};
        socket.send(new DatagramPacket(a, 1, InetAddress.getByAddress(targetAddress), targetPort));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    socket.close();
  }
}
