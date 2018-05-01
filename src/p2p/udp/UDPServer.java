package p2p.udp;

import java.io.ByteArrayInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.*;
import p2p.FileData;
import p2p.PeerData;
import p2p.db.DatabaseManager;

/**
 * The server of the application. Will be expecting packets sent from peers.
 *
 * @authors: Tyler & Austin
 */
public class UDPServer extends Host {

    private int listeningPort;
    private DatagramSocket receivingSocket = null;
    private HashMap<String, PeerData> currentConnections = new HashMap<>();

    //Create server
    public UDPServer(int port) {
        this.listeningPort = port;    //Set the port that the server listens to
        DatabaseManager.initialize(); //Initialize database
    }

    //Runs the server
    //Sets the server up to listen to all incoming requests
    //Upon packet being received, it checks if that user has previously sent a message or not
    //If they haven't, it adds a new connection to the hashmap, connecting the user's ip and hostname 
    //to it's other data, including the current request (this PeerData contains the request as
    //it's being recreated, which is necessary since packets can only contain MSS bytes of payload)
    //If they have already sent data, it adds the new data from the packet to the incomplete message.
    //If the full message is complete, it processes this complete message on a new thread.
    public void run() {

        try { //Create Socket
            receivingSocket = new DatagramSocket(listeningPort);
            System.out.println("Host is listening for UDP data on port "
                    + listeningPort + " with IP address " + getIPAddress());
        } catch (Exception e) {
            System.out.println("Could not create server's socket");
            e.printStackTrace();
            close();
            System.exit(10);
        }

        while (true) { //Get packets forever
            //Wait for request
            System.out.println("Waiting for data...");
            byte[] buf = new byte[MSS];

            //Receive request
            DatagramPacket packet = new DatagramPacket(buf, MSS);
            try {
                receivingSocket.receive(packet);
            } catch (Exception e) {
                System.out.println("Could not receive packet");
                e.printStackTrace();
                close();
                System.exit(11);
            }
            byte[] packetData = Arrays.copyOf(packet.getData(), packet.getLength());
            System.out.println("TTS: Socket info: " + packet.getSocketAddress() + "");

            //Handle packet in new thread
            new PacketHandler().setPacket(packet)
                    .start();
        }
    }

    //Close UDPServer components
    public void close() {
        //Close socket
        if (receivingSocket != null) {
            receivingSocket.close();
        }
        System.out.println("Server socket was closed");

        //Close DB connection
        DatabaseManager.exitDatabase();
    }

    //Create headers for messages sent from server (used in host's method)
    @Override
    public String createHeaders(String[] params) {
        if (params == null) {
            return null;
        }

        String statusCode = params[0];
        String statusPhrase = params[1];

        String headerData = statusCode + " " + statusPhrase + CRLF;
        return headerData;
    }

    //Send message to 
    /*private void performMessage(byte[] data, String[] headerData, String IP,
            int port, DatagramSocket socket) {
        try {
            performMessage(data, headerData, InetAddress.getByName(IP), port,
                    socket);
        } catch (Exception e) {
            System.out.println("Unable to send response to peer");
            e.printStackTrace();
        }
    }*/

    private class PacketHandler extends Thread {

        private DatagramPacket packet = null;
        private PeerData peer = null;
        private String messageType = null;
        //private String[] payloadParts = null;

        public PacketHandler setPacket(DatagramPacket packet) {
            this.packet = packet;
            return this;
        }

        @Override
        public void run() {
            if (packet == null) {
                return; //If no packet, do nothing
            }
            //Get peer data from packet (via payload or packet itself)
            byte[] payload = Arrays.copyOf(packet.getData(), packet.getLength()); //Payload as byte array
            String payloadStr = new String(payload);
            //Scanner payloadScan = new Scanner(payloadStr);

            String[] payloadParts = payloadStr.split(" |" + CRLF);

            //Print
            //for(String part: payloadParts) System.out.println(part);
            //Peer data (guaranteed by format)
            messageType = payloadParts[0];//payloadScan.next();//= payloadParts[0];
            String peerName = payloadParts[1];//payloadScan.next();//payloadParts[1];
            String peerIP = payloadParts[2];//payloadScan.next();//payloadParts[2];
            int peerPort = packet.getPort();

            //Print
            System.out.println(messageType);
            System.out.println(peerName);
            System.out.println(peerIP);
            System.out.println(peerPort);

            //Get peer
            String peerKey = peerIP + peerName + peerPort;
            peer = currentConnections.get(peerKey);
            if (peer == null) { //If peer doesn't exist yet, add them
                peer = new PeerData(peerIP, peerName, peerPort); //Create new PeerData object, will hold the request
                currentConnections.put(peerKey, peer);   //Add to map
                DatabaseManager.addPeer(peerName, peerIP);      //Update database with new peer
            }

            if (messageType.equals("ACK")) {
                //Process ACK
                int ACK = Integer.parseInt(payloadParts[3]);
                System.out.println("Got ACK " + ACK + " from peer " + peerName + " at " + peerIP);
                peer.setCurrentACKValue(ACK);
                peer.print();
                //End of thread for ACK
                
            } else {
                //Process other type of message
                payloadParts = payloadStr.split(" |" + CRLF, 6);
                int SEQ = Integer.parseInt(payloadParts[3]);//payloadScan.nextInt();
                int EOM = Integer.parseInt(payloadParts[4]);//payloadScan.nextInt();
                //payloadScan.useDelimiter("*");
                String userData = payloadParts[5];
                userData = userData.substring(0, userData.length() - 2);
                //TODO verfiy correct

                //Print
                System.out.println(SEQ);
                System.out.println(EOM);
                System.out.println(userData);

                //Print packet
                printPacketReceived(peer.getPacketCount() + 1, SEQ, EOM, peer.getIP(), packet.getLength());

                //SEQ and EOM
                if (SEQ == peer.getExpectedSEQ()) { //If SEQ is correct..
                    peer.updateExpectedSEQ();    //Progress SEQ
                    peer.addToRequest(userData); //Add data to request
                    sendACK(receivingSocket, SEQ, peer.getIP(), peer.getPort());
                    //If message complete, process full message
                    if (EOM == 1) {
                        System.out.println(peer.getRequest()); //TODO REMOVE
                        processMessage(peer.getRequest());
                        peer.finishRequest();
                    }
                } else {
                    sendACK(receivingSocket, peer.getExpectedSEQ(), peer.getIP(), peer.getPort()); //Got wrong SEQ
                }
            }
        }

        private void processMessage(String message) {
            //Depending on message method, do certain code
            switch (messageType) {
                case "inform":
                    processInformAndUpdate(message);
                    break;
                case "query":
                    processQuery(message);
                    break;
                case "exit":
                    processExit(message);
                    break;
                default:
                //Error, unsupported message type
            }

            //Print database to show change from request (end of thread)
            DatabaseManager.printCurrentDBState();
        }

        //Submit selected files to server db
        private void processInformAndUpdate(String message) {
            String currentFileName;
            long currentFileSize;
            Scanner scan;

            //Remove previously existing files from peer 
            DatabaseManager.removePeerFiles(peer.getHostname(), peer.getIP());

            //Process inform and update request
            scan = new Scanner(message);
            while (scan.hasNext()) { //While there's another file... //TODO handle misformatted messages
                //Get file data from payload
                while (!scan.next().equals("Filename:")); //Ignore any newlines, and ignore 'Filename:'
                currentFileName = decodeString(scan.next()); //Readd spaces to filename
                currentFileSize = scan.nextLong();

                //Insert file into db
                System.out.println("Adding file record: {File: '" + currentFileName
                        + "', File size: '" + currentFileSize + " bytes'}, associated with host "
                        + peer.getHostname() + " at IP address " + peer.getIP());
                DatabaseManager.addFile(currentFileName, currentFileSize, peer.getHostname(), peer.getIP());

                //Remove old files no longer shared??
                //TODO
            }

            //Send response to sender TODO error?
            performMessage("Entries have been added".getBytes(),
                    new String[]{"200", "OK"}, peer.getIP(), peer.getPort(), receivingSocket);
        }

        private void processQuery(String message) {
            Scanner scan;
            String query;
            FileData[] results;
            String resultText = "";

            //Process query request
            //Get query
            scan = new Scanner(message);
            scan.next(); //Ignore 'Query'
            if (!scan.hasNext()) {
                query = "";
            } else {
                query = decodeString(scan.next());
            }
            System.out.println("Searching files with keyword '" + query + "'.");

            //Perform query on db, create response to peer
            results = DatabaseManager.queryFiles(query);
            for (FileData file : results) {
                resultText += encodeString(file.getPath()) + " " + +file.getSize() + " "
                        + file.getHostPeerIP() + CRLF;
            }
            if (resultText.length() == 0) {
                resultText = "*";
            }

            //Send response specifying results of query to sender
            performMessage(resultText.getBytes(),
                    new String[]{"200", "OK"}, peer.getIP(), peer.getPort(), receivingSocket);
        }

        private void processExit(String message) {
            //Complete exit request by removing entries from db
            System.out.println("Removing file records for peer " + hostname + " with "
                    + "IP address " + peer.getIP());
            DatabaseManager.removePeer(hostname, peer.getIP());

            //Send response to sender
            performMessage("Entries removed".getBytes(),
                    new String[]{"200", "OK"}, peer.getIP(), peer.getPort(), receivingSocket);
        }

        private void performMessage(byte[] data, String[] headerData, String IP,
            int port, DatagramSocket socket) {
        try {
            sendMessage(data, headerData, InetAddress.getByName(IP), port,
                    socket);
        } catch (Exception e) {
            System.out.println("Unable to send response to peer");
            e.printStackTrace();
        }
    }
        //Send ACK
        public void sendACK(DatagramSocket socket, int SEQ, String SenderIP, int SenderPort) {
            byte[] packetData = {(byte) SEQ};
            try {
                DatagramPacket packet = new DatagramPacket(packetData, packetData.length, InetAddress.getByName(SenderIP), SenderPort);
                socket.send(packet);
            } catch (Exception e) {
                System.out.println("Could not send ACK from server to " + SenderIP);
                e.printStackTrace();
                close();
                System.exit(12);
            }
            System.out.println("ACK Sent: " + SEQ);
        }

        // Prints packet data
        public void printDataReceived(int SEQ, int EOM, String SenderIP, String message) {
            System.out.println("|---*Received data packet info*---|");
            System.out.println("SEQ:" + SEQ);
            System.out.println("EOM:" + EOM);
            System.out.println("SenderIP:" + SenderIP);
            System.out.println("message:" + message + "\n");
        }

        //Packet Recieved Print Statement
        public void printPacketReceived(int pnum, int SEQ, int EOM, String SenderIP, int length) {
            System.out.println("|----Packet " + pnum + " recieved----|");
            System.out.println("Recieved: " + SenderIP + "\nSEQ: " + SEQ + "\tEOM: " + EOM);
            System.out.println("|----Packet Length: " + length + " ----|\n");
        }

        public void sendMessage(byte[] data, String[] headerData,
                InetAddress targetIPAddr, int targetPort, DatagramSocket socket) {
            //Message data
            ByteArrayInputStream byteStream = new ByteArrayInputStream(data); //Stream of full-message payload 

            //Packet specific data
            boolean SEQ = false; //SEQ flag of message, start with SEQ = 0
            String constantHeaders = createHeaders(headerData); //Create app-header data
            DatagramPacket currentPacket;                //Current packet being sent
            int packetNum = 0;  //Current packet number being sent

            //Manage RDT via timer corresponding with peer
            ACKTimer timer = peer.getTimer();
            if (timer == null) { 
                timer = new ACKTimer(socket);
                peer.setTimer(timer);
            }

            //While data still needs to be sent..
            while (byteStream.available() > 0) {
                //Create packet of next data
                currentPacket = createPacket(byteStream, constantHeaders, SEQ,
                        targetIPAddr, targetPort); //Create packet with rdt app-headers and headers specific to required format
                if (currentPacket == null) {
                    return; //If no packet created, packet size is an issue. Don't send null packet.
                }

                //Print packet data
                byte[] packetData = currentPacket.getData();
                System.out.println(
                        "\n\n| - - - - START PACKET (" + packetNum++ + ") - - - - - - - - |\n"
                        + new String(packetData)
                        + "|- - - - END PACKET (total length: " + packetData.length + ") - - - - -| \n");

                //Send packet
                rdt_send(socket, currentPacket, SEQ, timer);
                SEQ = !SEQ;
                try { //Give time so that everything doesn't happen too quickly
                    Thread.sleep(1200);
                } catch (Exception e) {
                    System.out.println("Unable to have thread wait");
                    e.printStackTrace();
                }
            }
            System.out.println("Full message sent!");
        }

        private void rdt_send(DatagramSocket socket, DatagramPacket packet,
                boolean SEQ, ACKTimer timer) {
            long startTime, endTime;
            //Send
            try {
                socket.send(packet);
            } catch (Exception e) {
                System.out.println("Could not send packet");
                e.printStackTrace();
                return;
            }

            //Wait for correct ACK
            timer.setPacketToResend(packet);
            startTime = System.currentTimeMillis();
            timer.start(); //Start timer
            
            //Wait for ACK
            System.out.println("Expecting ACK " + (SEQ? 1:0) + " from peer " + peer.getHostname() + " at " + peer.getIP() + "...");
            while(peer.getCurrentACKValue()!= (SEQ? 1:0)); //peer.print();
            System.out.println("Peer " + peer.getHostname() + "at" + peer.getIP() + "sent ACK.");
            System.out.println("\tACK Data Expected:" + (SEQ? 1:0) + "\tAck Data Got " + peer.getCurrentACKValue());
            System.out.println("Correct ACK received, server will continue sending data.\n");
           
            //Timer
            endTime = System.currentTimeMillis();
            timer.stop(); //End timer after correct ACK
            timer.updateInterval((int) (endTime - startTime));
        }
    }
}


/*
        try {
                
                //Breakdown request using the designed format
                String rph = new String(packetData);	//Save the string from packet.
                String[] arr = rph.split(" |\r\n", 6);

                //Variables saved from packetData
                String message_Type = arr[0];
                String Sender = arr[1];
                String SenderIP = arr[2];
                int SEQ = Integer.parseInt(arr[3]);
                int EOM = Integer.parseInt(arr[4]);
                String userData = arr[5];
                userData = userData.substring(0, userData.length() - 2);

                //Check if this peer has been connected to the server already
                String key = Sender + SenderIP;                         //Key to peer data
                PeerData currentPeerData = currentConnections.get(key); //Peer data
                if (currentPeerData == null) { //If peer data doesn't already exist, meaning this is a new connection, add the data to the map
                    currentPeerData = new PeerData(SenderIP, Sender, packet.getPort()); //Create new PeerData object, will hold the request
                    currentConnections.put(key, currentPeerData);   //Add to map
                    DatabaseManager.addPeer(Sender, SenderIP);      //Update database with new peer
                }
                
                //Is packet correct?
                if (SEQ != SEQ_previous) { //If correct SEQ, add packet data to full message, send ACK
                    PacketNum = PacketNum + 1;
                    currentPeerData.addToRequest(userData); //Add data to full peer request
                    printPacketReceived(PacketNum, SEQ, EOM, SenderIP, packet.getLength());
                    if (EOM == 1) { //If End of message, process message on new thread
                        String fullRequest = currentPeerData.getRequest();
                        printDataReceived(SEQ, EOM, SenderIP, fullRequest);
                        sendACK(receivingSocket, SEQ, packet.getAddress(), packet.getPort());
                        processMessage(message_Type, currentPeerData);
                        PacketNum = 0;
                        SEQ_previous = 1;
                        //currentPeerData.finishRequest();
                    } else {
                        SEQ_previous = SEQ;
                        sendACK(receivingSocket, SEQ, packet.getAddress(), packet.getPort());
                    }

                } else {
                    sendACK(receivingSocket, SEQ_previous, packet.getAddress(), packet.getPort());
                }
            }
        } catch (Exception e) {
            stopListening();
            e.printStackTrace();
        }
    }
 */
