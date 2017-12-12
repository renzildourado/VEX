import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class SenderThread extends Thread {
    Object aucReq;
    Socket sockToBidder;
    int port_no;
    String message;

    public SenderThread(Object aucReq, Socket sockToBidder, int port_no, String message) {
        this.aucReq = aucReq;
        this.sockToBidder = sockToBidder;
        this.port_no = port_no;
        this.message = message;
    }

    public void run() {
        try {
            ObjectOutputStream objectOutput = new ObjectOutputStream(sockToBidder.getOutputStream());
            objectOutput.writeObject(aucReq);
            System.out.println(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
