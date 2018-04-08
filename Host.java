import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;

//Contains functionality shared between servers and senders
public class Host extends Thread{
  public static boolean DEBUG = true;
  protected String IPAddress = null;
  protected String hostName = null;
  
  public Host(){
    this("default");
  }
  
  public Host(String name){
    super(name);
    getIPAddress(); //Get IPAddress
    getHostName();  //Get Hostname
    if(DEBUG){
      System.out.println("\t>>Created Sender with IP " + getIPAddress());
      System.out.println("\t>>Created Sender with hostname " + getHostName()+"\n");
    }
    
  }
  
  //Get the IP address of the host
  public final String getIPAddress(){
    //If IP address is not currently set, set it, then return it
    if(IPAddress == null){
      try{
        IPAddress = InetAddress.getLocalHost().getHostAddress();
      }
      catch(Exception e){
        IPAddress = null;
        e.printStackTrace();
      }
    }
    return IPAddress;
  }
  
  //Get the hostname of the host
  public final String getHostName(){
    //If hostname is not currently set, set it, then return it
    if(hostName == null){ 
      hostName = "";
      String line;        //Holds each line
      ProcessBuilder pb;  //Used to build procress being ran
      Process pr;         //The actual process
      BufferedReader result; //Reader to read from process' input stream
      
      try{
        //Create process to execute 'hostname' process locally & run it
        pb = new ProcessBuilder("hostname");
        pb.redirectErrorStream(true); //Send standard output and error output to same stream
        pr = pb.start(); //Run process

        //Get input
        result = new BufferedReader(new InputStreamReader(pr.getInputStream()));
        while((line = result.readLine())!=null){ hostName += line;}
        pr.waitFor();   //Wait until process is complete
        result.close(); //Close reader
        }
      catch(Exception e){
        e.printStackTrace();
        hostName = null;
      }
    }
    return hostName;
  }
}