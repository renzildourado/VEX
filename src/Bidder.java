import javax.crypto.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Created by Darryl Pinto on 12/3/2017.
 */
public class Bidder {

    int bidder_id;

    private Cipher encryptCipher;
    private Cipher decryptCipher;
    private SecretKey key;


    public static void main(String[] args) throws IOException, ClassNotFoundException, IllegalBlockSizeException, BadPaddingException {

        Random rand = new Random();
        Bidder b = new Bidder();
        b.bidder_id = rand.nextInt(10000);

        try {
            b.key = KeyGenerator.getInstance("AES").generateKey();

            b.encryptCipher = Cipher.getInstance("AES");
            b.decryptCipher = Cipher.getInstance("AES");

            b.encryptCipher.init(Cipher.ENCRYPT_MODE, b.key);
            b.decryptCipher.init(Cipher.DECRYPT_MODE, b.key);

        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
            System.out.println("ENCRYPTION ERROR");
            System.exit(8);
        }

        int port_no = Integer.parseInt(args[0]);

//        Scanner sc = new Scanner(System.in);
//        int port_no = sc.nextInt();
        System.out.println("Bidder port number: "+ port_no);

        ServerSocket serverSoc = new ServerSocket(port_no);

        System.out.println("Bidder " + b.bidder_id + " waiting to place a bid");
        Socket soc = serverSoc.accept();

        ObjectInputStream in_stream = new ObjectInputStream(soc.getInputStream());
        AuctionRequest auctionRequest = (AuctionRequest) in_stream.readObject();

        System.out.println("Received Auction Request:" + auctionRequest.auctionID);

        Vex myVex = new Vex(b.bidder_id, rand.nextInt(1000), auctionRequest.auctionID);
        System.out.println("Bidder " + b.bidder_id + " placed a bid of " + myVex.amount);

        // encrypt(myVex);
        SealedObject sealed = new SealedObject(myVex, b.encryptCipher);

        ArrayList arr = new ArrayList(2);
        arr.add(auctionRequest.auctionID);
        arr.add(sealed);
        ObjectOutputStream out_stream = new ObjectOutputStream(soc.getOutputStream());
        out_stream.writeObject(arr);
        //System.out.printf("SENT (%d, %s)\n", auctionRequest.auctionID, sealed);

        // Reinitalise the stream to prevent corruption
        in_stream = new ObjectInputStream(soc.getInputStream());

        HashMap<Integer, SealedObject> encryptedVexMap = (HashMap<Integer, SealedObject>) in_stream.readObject();
        SealedObject encryptedVex = encryptedVexMap.get(port_no);

//        SealedObject receivedVexEN = (SealedObject) in_stream.readObject();

        Vex receivedVex = (Vex) encryptedVex.getObject(b.decryptCipher);

        if(receivedVex.equals(myVex)){
            System.out.println("My bid is present in the encrypted list");
        }
        else{
            System.out.println("My bid is not present! Exiting");
            System.exit(5);
        }

        out_stream = new ObjectOutputStream(soc.getOutputStream());
        out_stream.writeObject(b.key);
        System.out.println("Sent Key:" + b.key);

//        try {
//            Thread.sleep(10000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

        in_stream = new ObjectInputStream(soc.getInputStream());
        ArrayList<Vex> winnerBids = (ArrayList<Vex>) in_stream.readObject();

        if(winnerBids.get(0).equals(myVex)){
            System.out.println("I AM THE WINNER!!!");
        }
        else if(winnerBids.get(1).equals(myVex)){
            System.out.println("ONE BID BEAT ME!!");
        }
        else
            System.out.println("I lost!");
    }
}