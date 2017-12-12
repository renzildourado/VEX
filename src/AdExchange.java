import javax.crypto.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

/**
 * Created by Renzil Dourado on 12/2/2017.
 */

public class AdExchange implements Serializable {

    static int numberOfBidders = 0;
    static String bidderIP = "localhost";
    static HashMap<Integer, String> bidders = new HashMap<>();

    public static void initialise_Bidder_communication() {
        for (int i = 10000; i < 10000 + numberOfBidders; i++)
            bidders.put(i, bidderIP);
    }

    public static void main(String args[]) throws IOException, ClassNotFoundException {
        System.out.println("Number of bidders:");
        if (args.length == 1) {
            numberOfBidders = Integer.parseInt(args[0]);
            System.out.println(numberOfBidders);
        } else {

            Scanner sc = new Scanner(System.in);
            numberOfBidders = sc.nextInt();
        }

        System.out.println("AdExchange waiting for Auction request...");
        ServerSocket serverSock = new ServerSocket(9999);
        Socket socToSeller;
        socToSeller = serverSock.accept();

        AuctionRequest aucReq = receiveAucReq(socToSeller);
        System.out.println("AdExchange received auction request with Auction ID " + aucReq.auctionID);

        initialise_Bidder_communication();

        HashMap<Integer, SealedObject> encryptedVexMap = new HashMap<>();
        HashMap<Integer, Vex> decryptedVexMap = new HashMap<>();
        HashMap<Integer, Socket> socketMap = new HashMap<>();
        SenderThread[] senderThread = new SenderThread[numberOfBidders];
        ReceiverThread[] receiverThreads = new ReceiverThread[numberOfBidders];

        int i = 0;
        for (Integer port_no : bidders.keySet()) {
            String message = "AdExchange sent auction request with Auction ID  " + aucReq.auctionID + "  to Bidder" + "with port no: "+port_no;
            Socket sockToBidder = new Socket(bidders.get(port_no), port_no);
            senderThread[i] = new SenderThread(aucReq, sockToBidder, port_no, message);
            senderThread[i].start();
            i++;
//            SealedObject encryptedVex = receiveEncVex(sockToBidder);
//            encryptedVexMap.put(port_no, encryptedVex);
//            socketMap.put(port_no, sockToBidder);
        }

        for (int j = 0; j < senderThread.length; j++) {
//            try {
//                senderThread[j].join();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
            receiverThreads[j] = new ReceiverThread(senderThread[j].sockToBidder, senderThread[j].port_no);
            receiverThreads[j].start();
        }

        // I think we need to join on only receiver threads because if we wouldn't be receiving a response if the msg wasnt'
        // sent in the first place. Please confirm

        for (int j = 0; j < receiverThreads.length; j++) {
            try {
                receiverThreads[j].join();
                //System.out.println(receiverThreads[j].getId() + "joined");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


            ArrayList arr = null;

                arr = (ArrayList) receiverThreads[j].o;

            SealedObject encryptedVex= (SealedObject) arr.get(1);
            System.out.println("Received Encrypted Vex object from Bidder "+receiverThreads[j].port_no);


            encryptedVexMap.put(receiverThreads[j].port_no, encryptedVex);
            socketMap.put(receiverThreads[j].port_no, receiverThreads[j].sockToBidder);
        }

        // JOIN THREAD FIRST TIME
        // Send list of vex object Seller to sign it

        sendForSignature(encryptedVexMap, socToSeller);

        HashMap<Integer, SealedObject> signedEncryptVexMap = receiveSignedObject(socToSeller);

        //I thought I could get away by using the same sender class but can't ATM.
        //SenderSigned[] senderSigneds = new SenderSigned[numberOfBidders];
        // Send list of vex object to All Bidders
        //for (Integer port_no : bidders.keySet()) {
        for (int j = 0; j < senderThread.length; j++) {
            String message = "Sending Encrypted Object bundle to bidder at port " + senderThread[j].port_no;
            senderThread[j] = new SenderThread(signedEncryptVexMap, senderThread[j].sockToBidder, senderThread[j].port_no, message);
            //senderSigneds[j] = new SenderSigned(senderThread[j].sockToBidder, senderThread[j].port_no, signedEncryptVexMap);
            //It already has a socket? So can get rid of a hashmap?
            //Socket sockToBidder = socketMap.get(port_no);
            //senderSigneds[j].start();
            senderThread[j].start();
        }

        // Receive decrypted vex objects from all bidders, here we should receive key actually

        // I cannot reuse the same ReceiverThread coz too specific. First it returns a Vex Object, now a key... I guess
        //we can handle it... but for now, creating new Class :P
        // this is ugly

        //for (Integer port_no : bidders.keySet()) {

        // ReceiverKey[] receiverKeys = new ReceiverKey[numberOfBidders];

        for (int j = 0; j < senderThread.length; j++) {
            //receiverThreads[j] = new ReceiverThread()
            receiverThreads[j] = new ReceiverThread(senderThread[j].sockToBidder, senderThread[j].port_no);
            //receiverKeys[j] = new ReceiverKey(senderThread[j].sockToBidder, senderThread[j].port_no);
            receiverThreads[j].start();
        }


        for (int j = 0; j < receiverThreads.length; j++) {
            try {
                receiverThreads[j].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            SecretKey key = (SecretKey) receiverThreads[j].o;

            System.out.println("Key received from bidder at port "+receiverThreads[j].port_no );
            try {
                Cipher decrypter = Cipher.getInstance("AES");
                decrypter.init(Cipher.DECRYPT_MODE, key);
                Vex decryptedVex = (Vex) encryptedVexMap.get(receiverThreads[j].port_no).getObject(decrypter);
                decryptedVexMap.put(receiverThreads[j].port_no, decryptedVex);

            } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
                e.printStackTrace();
            }

        }

        // JOIN THREAD SECOND TIME
        // Check for Winner

        Vex winner = new Vex(0, Integer.MIN_VALUE, 0);
        Vex secondWinner = winner;
        for (Integer port_no : bidders.keySet()) {
            Vex currentVex = decryptedVexMap.get(port_no);
            if (currentVex.amount >= winner.amount) {
                secondWinner = winner;
                winner = currentVex;
            } else if (currentVex.amount >= secondWinner.amount) {
                secondWinner = currentVex;
            }
        }
        ArrayList<Integer> winningBids = new ArrayList<>(2);
        winningBids.add(winner.amount);
        winningBids.add(secondWinner.amount);

        sendWinnerToSeller(socToSeller, winningBids);

        System.out.println(" The winner is " + winner.bidder_id + " with amount " + winner.amount);
        System.out.println(" The next Highest Bid is from " + secondWinner.bidder_id + " with amount " + secondWinner.amount);


        ArrayList<Vex> winningBidsVex = new ArrayList<>(2);
        winningBidsVex.add(winner);
        winningBidsVex.add(secondWinner);


        for (int j = 0; j < senderThread.length; j++) {
            String message = "Sent Winning Bid Vex Object to Bidder at port:" + senderThread[j].port_no;
            senderThread[j] = new SenderThread(winningBidsVex, senderThread[j].sockToBidder, senderThread[j].port_no, message);
            senderThread[j].start();
        }

        ///// NEW PART

        for (int j = 0; j < senderThread.length; j++) {
            try {
                senderThread[j].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


    }


//    public static SecretKey receiveKey(Socket sockToBidder) throws IOException {
//
//        ObjectInputStream objectInput = new ObjectInputStream(sockToBidder.getInputStream());
//        SecretKey key = null;
//        try {
//            key = (SecretKey) objectInput.readObject();
//        } catch (ClassNotFoundException e) {
//            e.printStackTrace();
//        }
//
//        System.out.println("Received Key from Bidder");
//        return key;
//    }


    private static void sendWinnerToSeller(Socket socToSeller, ArrayList<Integer> winningBids) throws IOException {

        ObjectOutputStream objectOutput = new ObjectOutputStream(socToSeller.getOutputStream());
        objectOutput.writeObject(winningBids);
        System.out.println("Sent to Seller:" + winningBids);

    }

    private static HashMap<Integer, SealedObject> receiveSignedObject(Socket socToSeller) throws IOException {

        ObjectInputStream objectFromSeller = new ObjectInputStream(socToSeller.getInputStream());
        ArrayList arrFromSeller = null;
        try {
            arrFromSeller = (ArrayList) objectFromSeller.readObject();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        if (arrFromSeller.get(0).equals(true)) {
            System.out.println("Received Signed bundle From Seller");
            return (HashMap<Integer, SealedObject>) arrFromSeller.get(1);
        } else {
            System.out.println("Expected a signature from seller..\n Not Received. \n Exiting");
            System.exit(-1);

        }
        return null;

    }

    private static void sendForSignature(HashMap<Integer, SealedObject> encryptedVexMap, Socket socToSeller) throws IOException {
        ObjectOutputStream objectToSeller = new ObjectOutputStream(socToSeller.getOutputStream());
        ArrayList arrToSeller = new ArrayList(2);
        arrToSeller.add(false);
        arrToSeller.add(encryptedVexMap);
        objectToSeller.writeObject(arrToSeller);
        System.out.println("Encrypted Object bundle sent for Signature to the Seller");
    }


//    public static Vex receiveKey(Socket sockToBidder) throws IOException {
//
//        ObjectInputStream objectInput = new ObjectInputStream(sockToBidder.getInputStream());
//        Vex decryptedVexObj = null;
//        try {
//            decryptedVexObj = (Vex) objectInput.readObject();
//        } catch (ClassNotFoundException e) {
//            e.printStackTrace();
//        }
//
//        System.out.println("Received Decrypted Vex object from Bidder");
//        return decryptedVexObj;
//    }


    public static AuctionRequest receiveAucReq(Socket soc) {

        AuctionRequest aucReq = null;

        try {
            ObjectInputStream objectInput = new ObjectInputStream(soc.getInputStream());
            Object object = null;
            try {
                object = objectInput.readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            aucReq = (AuctionRequest) object;

        } catch (IOException e) {
            e.printStackTrace();
        }
//
//        try {
//            serverSock.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        return aucReq;
    }


}
