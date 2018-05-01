package p2p.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import p2p.FileData;


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
    registerDriver();
    System.out.println("\tSuccess!");
    
    //Initalize connection with DB
    System.out.print("\t>>Establishing connection with database..");
    connectToDB();
    System.out.println("\tSuccess!");
    
    //Initialize DB Table PEER if it doesn't exist already
    System.out.println("\t>>Creating tables..");
    System.out.print("\t\t>>Creating table PEER..");
    addPeerTable();
    System.out.println("\tSuccess!");
    
    //Initialize DB Table FILE if it doesn't exist already
    System.out.print("\t\t>>Creating table FILE..");
    addFileTable();
    System.out.println("\tSuccess!");
    
    //Initialization complete
    System.out.println("Database initalized");
    return true;
  }
  
  /* START INITIALIZATION METHODS */
  //Registers JDBC driver
  private static void registerDriver(){
     try{
      Class.forName(JDBC_DRIVER).newInstance();
    }catch(Exception e){
      System.out.println("Could not use JDBC driver");
      e.printStackTrace();
      System.exit(1);
    }
  }
  
  //Establishes connection with database
  private static void connectToDB(){
    try{
      dbConnection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }catch(Exception e){
      System.out.println("Could not establish connection - make sure only app is connected to db");
      e.printStackTrace();
      System.exit(2);
    }
  }
  
  //Adds PEER table to database if it doesn't exist already
  private static void addPeerTable(){
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
      System.exit(3);
    }
  }
  
  //Adds FILE table to database if it doesn't exist already
  private static void addFileTable(){
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
      System.exit(4);
    }
  }
  /* END INITIALIZATION METHODS*/
  
 
  /* START DATABASE MANIPULATION METHODS*/  
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
      int result;
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
      /*
      PreparedStatement stmt = dbConnection.prepareStatement(""
              + "DELETE FROM FILE\n"
              + "WHERE HOSTPEERID = ?");
      stmt.setInt(1, peerID);
      
      //Execute, 
      stmt.executeUpdate();
      stmt.close();
      */
      
      removePeerFiles(peerID);
      
      //Create statement to remove peer
      PreparedStatement stmt = dbConnection.prepareStatement(""
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
  
  public static boolean removePeerFiles(String hostname, String IP){
    return removePeerFiles(getPeerID(hostname, IP));
  }
  
  public static boolean removePeerFiles(int peerID){
    //Create statement to remove all files associated with peer
    try{
      PreparedStatement stmt = dbConnection.prepareStatement(""
              + "DELETE FROM FILE\n"
              + "WHERE HOSTPEERID = ?");
      stmt.setInt(1, peerID);
      
      //Execute, 
      stmt.executeUpdate();
      stmt.close();
    }catch(Exception e){
      System.out.println("Unable to remove peer's files");
      e.printStackTrace();
      System.exit(0);
    }
    
    return true;
  }
  
  public static FileData[] queryFiles(String query){
    ResultSet results = null;
    ArrayList<FileData> files = new ArrayList<>();
    
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
        System.out.println(path + "\t\t" + fileSize + " bytes\t\t" + host + "\t\t" + IP);
        FileData file = new FileData(path, fileSize, IP, 55555); //TODO hardcoded port
        files.add(file);
      }
      
      results.close();
      stmt.close();
      return files.toArray(new FileData[files.size()]); //Return results
    }catch(Exception e){
      System.out.println("Unable to get peer id - peer does not exist");
      e.printStackTrace();
      return null;
    }
  }
  
  /* END DATABASE MANIPULATION METHODS */
  
  public static void exitDatabase(){
    if(dbConnection != null)
      try{
        dbConnection.close();
      }catch(Exception e){
        System.out.println("Could not close database connection");
        e.printStackTrace();
      }
  }
  
  public static void printCurrentDBState(){
    System.out.println("\n\nDATABASE STATE:");    
    System.out.println("FILEPATH \t\t FILE SIZE \t\t FILE's HOST \t\t HOST'S IP");
    queryFiles("");
    System.out.println("\n\n");
  }
}
