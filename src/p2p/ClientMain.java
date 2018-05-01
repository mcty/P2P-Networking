package p2p;

import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Scanner;
import p2p.tcp.tcpListen;
import p2p.udp.UDPSender;
import p2p.tcp.tcpClient;

/**
 *
 * @author Austin
 */
public class ClientMain {

  public static final boolean DEBUG = true;
  public static boolean actserve = false;
  
  public static tcpListen listenThread = null;
  public static void main(String[] args) {
    Scanner scan = new Scanner(System.in);
    senderRoutine(scan);
  }

    /* START SENDER FUNCTIONALITY */
  
    private static void senderRoutine(Scanner scan) {
        int targetPort, senderPort;
        String data; //User's input

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
            System.out.println("Okay, starting communication with server. Use 'inform', 'query', 'exit' or 'download'. Type 'exit' to end communication.\n");
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
                    //tcp connect to tcpserver
                    /*
                    / Get file by sending request to central server which then notifies
                    / peer to open thread and wait for client to connect to get file.
                    */
                    break;
                case "exit":
                    exit(sender);
                    break;
                case "download":
                    download(scan);
                    break;
                default:
                    defaultSend(data, sender);
                }
        
                if(data.equals("exit")){
                    //exit code here...
                    System.exit(0);
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
    if(actserve){
        listenThread.kill();
        listenThread = null;
    }
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
    listenThread = new tcpListen("listener", 5009, fileNames);
    listenThread.start();
    actserve = true;
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

  //Download a file from peer over tcp
  private static void download(Scanner scan){
      System.out.println("Please enter file to download");
      String f = scan.nextLine();
      System.out.println("Please enter IP");
      String i = scan.nextLine();
      System.out.println("Please enter Port.");
      String p = scan.nextLine();
      System.out.println("Please enter destination.");
      String d = scan.nextLine();
      System.out.println("Please enter file name and type.");
      String s = scan.nextLine();
      tcpClient downloadThread = new tcpClient(i,Integer.parseInt(p),f,d,s);
      downloadThread.start();
  }
  
  //Perform exit
  //a. send exit message
  private static void exit(UDPSender sender) throws SocketException, 
          IOException, InterruptedException {
    String responseText = sender.sendExit();
    System.out.println(responseText);
  }
  
  public static void defaultSend(String method, UDPSender sender) throws SocketException, 
          IOException, InterruptedException{
        String responseText = sender.sendUnknown(method);
        System.out.println(responseText);
    }
  /* END SENDER FUNCTIONALITY */
}
