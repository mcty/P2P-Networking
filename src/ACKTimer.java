
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Timer;
import java.util.TimerTask;

public class ACKTimer{
  //Timer vars
  private Timer timer;
  private int msToWait = 1000;
  
  //Values kept track of
  private DatagramSocket hostSocket;
  private DatagramPacket packetToResend;
  
  //Timer task
  private class waitForACKTask extends TimerTask{
    public void run(){
      //Resend
      System.out.println("Did not receive ACK flag in time, resending packet");
      try{
        hostSocket.send(packetToResend);
      }
      catch(IOException e){
        e.printStackTrace();
      }
    }
  }
  
  public ACKTimer(DatagramSocket sender, DatagramPacket packetToResend, int msToWait){
    //Initalizer timer
    timer = new Timer();
    
    //Set up values
    this.hostSocket = sender;
    this.packetToResend = packetToResend;
    this.msToWait = msToWait;
  }
  
  public void start(){
    timer.schedule(new waitForACKTask(), msToWait, msToWait);
  }
  
  public void stop(){
    timer.cancel();
    timer.purge();
  }
  
}