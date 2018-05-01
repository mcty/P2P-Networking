package p2p;


import java.util.Scanner;
import p2p.udp.UDPServer;

/**
 *
 * @author Tyler PC
 */
public class CentralServer {
    
    public static final boolean DEBUG = true;
    
    public static void main(String[] args){
        Scanner scan = new Scanner(System.in);
        serverRoutine(scan);
    }
    
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
}
