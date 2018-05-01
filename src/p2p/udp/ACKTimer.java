package p2p.udp;


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Timer;
import java.util.TimerTask;

public class ACKTimer{
  //Timer vars
  private Timer timer;
  private int msToWait = 1000;
  
  //Values determing the next time to wait
  private static final double ALPHA = .125;
  private static final double BETA = .25;
  private int estimatedRTT = 1000;
  private int devRTT = 0;
  
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
  
  public ACKTimer(DatagramSocket sender){
      this(sender, null, 1000);
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
    if(packetToResend == null)
        System.out.println("Cannot start timer when no packet exists");
    else
        timer.schedule(new waitForACKTask(), msToWait, msToWait);
  }
  
  public void stop(){
    timer.cancel();
    timer.purge();
    timer = new Timer();
  }
  
  public void updateInterval(int sampleRTT){
      System.out.println("Timer wait time Before:");
      System.out.println("\t>>estimatedRTT = " + estimatedRTT);
      System.out.println("\t>>deviation RTT = " + devRTT);
      System.out.println("\t>>ms to wait = " + msToWait);
      
      int oldEstimatedRTT = estimatedRTT;
      estimatedRTT = (int)((1-ALPHA)*estimatedRTT) + (int)(ALPHA*sampleRTT);
      devRTT = (int)((1-BETA)*devRTT) + (int)((BETA)*Math.abs(sampleRTT - oldEstimatedRTT));
      msToWait = estimatedRTT + 4*devRTT;
      if(msToWait == 0){ msToWait = 1; } //Cannot have 0 ms for the timer
      
      System.out.println("\nTimer wait time after sample RTT of " + sampleRTT + ":");
      System.out.println("\t>>estimatedRTT = " + estimatedRTT);
      System.out.println("\t>>deviation RTT = " + devRTT);
      System.out.println("\t>>ms to wait = " + msToWait);
      System.out.println("");
  }
  
  public void setPacketToResend(DatagramPacket packet){
      packetToResend = packet;
  }
}