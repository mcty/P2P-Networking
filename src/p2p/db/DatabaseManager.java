package p2p.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
//import java.sql.*;

public class DatabaseManager {
  //Database and connectivity info
  private static final String DATABASE_NAME = "p2p-file-transfer-db";
  private static final String DB_USER = "sa";
  private static final String DB_PASS = "";
  private static final String DB_URL = "jdbc:h2:~/"+DATABASE_NAME;
  private static final String JDBC_DRIVER = "org.h2.Driver";
  
  private static Connection dbConnection = null;
  
  private DatabaseManager(){;}
  
  public static boolean initialize(){
    System.out.println("Initializing Database...");
    
    //Register JDBC driver
    System.out.print("\t>>Registering JDBC driver..");
    try{
      Class.forName(JDBC_DRIVER).newInstance();
    }catch(Exception e){
      System.out.println("Could not use JDBC driver");
      e.printStackTrace();
    }
    System.out.println("\tSuccess!");
    
    //Initalize connection with DB
    System.out.print("\t>>Establishing connection with database..");
    try{
      dbConnection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }catch(Exception e){
      System.out.println("Could not establish connection - make sure only app is connected to db");
      e.printStackTrace();
    }
    System.out.println("\tSuccess!");
    
    //Initialize DB Table PEER if it doesn't exist already
    System.out.println("\t>>Creating tables..");
    System.out.print("\t\t>>Creating table PEER..");
    try{
    PreparedStatement stmt = dbConnection.prepareStatement(""+
            "CREATE TABLE IF NOT EXISTS PEER(\n"+
            "peerID  INT NOT NULL AUTO_INCREMENT,\n" +
            "hostName    VARCHAR(50) NOT NULL,\n" +
            "IP  VARCHAR(16) NOT NULL,\n" +
            "PRIMARY KEY(peerID),\n" +
            "UNIQUE(hostname, IP)\n" +
            ");");
    stmt.executeUpdate();
    stmt.close();
    }
    catch(Exception e){
      System.out.println("Could not create table PEER");
      e.printStackTrace();
    }
    System.out.println("\tSuccess!");

    
    //Initialize DB Table FILE if it doesn't exist already
    System.out.print("\t\t>>Creating table FILE..");
    try{
    PreparedStatement stmt = dbConnection.prepareStatement(""+
            "CREATE TABLE IF NOT EXISTS FILE(\n" +
            "filePath  VARCHAR(100) NOT NULL,\n" +
            "fileSize  INTEGER   NOT NULL,\n" +
            "hostPeerID  INTEGER NOT NULL,\n" +
            "PRIMARY KEY(filePath, fileSize, hostPeerId),\n" +
            "FOREIGN KEY(hostPeerID) REFERENCES PEER(peerID)\n" +
            ");");
    stmt.executeUpdate();
    stmt.close();
    }
    catch(Exception e){
      System.out.println("Could not create table FILE");
      e.printStackTrace();
    }
    System.out.println("\tSuccess!");

    System.out.println("Database initalized");
    return true;
  }
  
  //Add peer to database
  public static boolean addPeer(String hostname, String IP){
    try{
      //Create statement
      PreparedStatement stmt = dbConnection.prepareStatement(""
              + "INSERT INTO PEER (HOSTNAME, IP) VALUES (?, ?)");
      stmt.setString(1,hostname);
      stmt.setString(2, IP);
      
      //Execute statement
      stmt.executeUpdate();
      stmt.close();
    }catch(Exception e){
      System.out.println("Unable to add peer");
      e.printStackTrace();
      return false;
    }
    
    return true;
  }
 
  //Add peer to database using hostname and ip
  public static boolean addFile(String filePath, long fileSize,
          String hostname, String IP){
    return addFile(filePath, fileSize, getPeerID(hostname, IP));
  }
  
  //Add peer to database
  public static boolean addFile(String filePath, long fileSize, int peerID){
    try{
      //Create statement
      PreparedStatement stmt = dbConnection.prepareStatement(""
              + "INSERT INTO FILE VALUES (?, ?, ?)");
      stmt.setString(1,filePath);
      stmt.setLong(2, fileSize);
      stmt.setInt(3, peerID);
      
      //Execute statement, close
      stmt.executeUpdate();
      stmt.close();
    }
    catch(Exception e){
      System.out.println("Unable to add file");
      e.printStackTrace();
      return false;
    }
    
    return true;
  }
  
  //Get the peer ID
  public static int getPeerID(String hostname, String IP){
    try{
      //Create statement
      PreparedStatement stmt = dbConnection.prepareStatement(""
              + "SELECT PEERID\n"
              + "FROM PEER\n"
              + "WHERE HOSTNAME = ?\n"
              + "AND IP = ?");
      stmt.setString(1, hostname);
      stmt.setString(2, IP);
      
      //Execute, get results
      ResultSet results = stmt.executeQuery();
      int result = -1;
      if(results.next()){ //If result exists, get it..
        result = results.getInt("PEERID"); //And return peerid
      }
      else{
        result = -1;
      }
      
      //Close
      results.close();
      stmt.close();
      return result;
    }catch(Exception e){
      System.out.println("Unable to get peer id - peer does not exist");
      e.printStackTrace();
      return -1;
    }
  }

  //Remove peer
  public static boolean removePeer(String hostname, String IP){
    return removePeer(getPeerID(hostname, IP));
  }
  
  public static boolean removePeer(int peerID){
    try{
      //Create statement to remove all files associated with peer
      PreparedStatement stmt = dbConnection.prepareStatement(""
              + "DELETE FROM FILE\n"
              + "WHERE HOSTPEERID = ?");
      stmt.setInt(1, peerID);
      
      //Execute, 
      stmt.executeUpdate();
      stmt.close();
      
      //Create statement to remove peer
      stmt = dbConnection.prepareStatement(""
              + "DELETE FROM PEER\n"
              + "WHERE PEERID = ?");
      stmt.setInt(1, peerID);
      
      //Execute
      stmt.executeUpdate();
      stmt.close();
      
    }catch(Exception e){
      System.out.println("Unable to remove peer or peer's files");
      e.printStackTrace();
      return false;
    }
    
    return true;
  }
  
  public static void queryFiles(String query){
    ResultSet results = null;
    
    try{
      //Create statement
      PreparedStatement stmt = dbConnection.prepareStatement(""
              + "SELECT *\n"
              + "FROM FILE AS F\n"
              + "INNER JOIN PEER P ON F.HOSTPEERID = P.PEERID\n"
              + "WHERE F.FILEPATH LIKE ?");
      stmt.setString(1, "%"+query+"%");
      
      //Execute, get results
      results = stmt.executeQuery();
      
      
      //Print all results
      while(results.next()){
        String path = results.getString("FILEPATH");
        long fileSize = results.getLong("FILESIZE");
        String host = results.getString("HOSTNAME");
        String IP = results.getString("IP");
        System.out.println(path + "\t" + fileSize + "bytes\t" + host + "\t" + IP);
      }
      
      results.close();
      stmt.close();
    }catch(Exception e){
      System.out.println("Unable to get peer id - peer does not exist");
      e.printStackTrace();
      return;
    }
  }
  
  public static void exitDatabase(){
    if(dbConnection != null)
      try{
        dbConnection.close();
      }catch(Exception e){
        System.out.println("Could not close database connection");
        e.printStackTrace();
      }
  }
}
