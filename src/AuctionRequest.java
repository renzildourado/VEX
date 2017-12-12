import java.io.Serializable;

/**
 * Created by Renzil Dourado on 12/2/2017.
 */

class AuctionRequest implements Serializable {
    int auctionID;
    int space;

    public AuctionRequest(int auctionID, int space) {
        this.auctionID = auctionID;
        this.space = space;
    }
}