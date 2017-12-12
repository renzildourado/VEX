/**
 * Created by Renzil Dourado on 12/2/2017.
 */

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;

public class Seller {

    public static void main(String args[]) throws IOException, ClassNotFoundException {

        Random rand = new Random();
        int auctionID = rand.nextInt(100) + 1;
        int space = rand.nextInt(500) + 1;
        Socket sock = new Socket("localhost",9999);
        long start = System.currentTimeMillis();
        AuctionRequest requestPacket = new AuctionRequest(auctionID, space);
        sendAucReq(requestPacket, sock);
        ArrayList signedBundle = receiveBundle(sock);
        sendSignedBundle(sock, signedBundle);

        ArrayList<Integer> winnerBids = receiveWinningBids(sock);
        long end = System.currentTimeMillis();

        System.out.println("Winning Amount:"+ winnerBids.get(0));
        System.out.println("Selling price:"+ winnerBids.get(1));
        System.out.println("Time(in millis):"+ (end-start));
    }

    private static ArrayList<Integer> receiveWinningBids(Socket sock) throws IOException, ClassNotFoundException {
        ObjectInputStream objectInput = new ObjectInputStream(sock.getInputStream());
        ArrayList<Integer> winnerBids = (ArrayList<Integer>) objectInput.readObject();

        return winnerBids;
    }

    public static void sendSignedBundle(Socket sock, ArrayList signedBundle) throws IOException {

        ObjectOutputStream objectOutput = new ObjectOutputStream(sock.getOutputStream());
        objectOutput.writeObject(signedBundle);
        System.out.println("Seller sent the signed Bundle back to the AdExchange");
    }

    public static ArrayList receiveBundle(Socket sock) throws IOException, ClassNotFoundException {
        ObjectInputStream objectInput = new ObjectInputStream(sock.getInputStream());
        ArrayList arr = null;
        arr = (ArrayList) objectInput.readObject();
        Boolean signature = (Boolean) arr.get(0);
        if(signature == false) {
            System.out.println("Received bundle from AdExchange for signing ");
            arr.remove(0);
            arr.add(0, true);
        }
        System.out.println("The Auction has been signed by the Seller");
        System.out.println("Sent the signed bundle to AdExchange: ");
        return arr;

    }

    public static void sendAucReq(AuctionRequest requestPacket, Socket sock) throws IOException {

            ObjectOutputStream objectOutput = new ObjectOutputStream(sock.getOutputStream());
            objectOutput.writeObject(requestPacket);
            System.out.println("Seller sent auction request with Auction ID "+requestPacket.auctionID);
    }

}
