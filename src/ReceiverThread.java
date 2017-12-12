import javax.crypto.SealedObject;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.ArrayList;

public class ReceiverThread extends Thread {
    Socket sockToBidder;
    Object o;
    //SealedObject encryptedVex;
    int port_no;

    public ReceiverThread(Socket sockToBidder, int port_no){
        this.sockToBidder = sockToBidder;
        this.port_no = port_no;
    }

    public void run(){
        ObjectInputStream objectInput = null;
        try {
            objectInput = new ObjectInputStream(sockToBidder.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            o = objectInput.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

//        ArrayList arr = null;
//        try {
//            arr = (ArrayList) objectInput.readObject();
//        } catch (ClassNotFoundException | IOException e) {
//            e.printStackTrace();
//        }
//
//        encryptedVex = (SealedObject) arr.get(1);
//        System.out.println("Received Encrypted Vex object from Bidder");
    }
}
