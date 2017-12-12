import java.io.Serializable;

/**
 * Created by Darryl Pinto on 12/3/2017.
 */
public class Vex implements Serializable{

    int bidder_id;
    int auction_id;
    int amount;

    Vex(int b, int amt, int auction_id){
        this.bidder_id = b;
        this.auction_id = auction_id;
        this.amount = amt;
    }

    public boolean equals(Vex v){

        return v.amount == this.amount && v.bidder_id == this.bidder_id && v.auction_id == this.auction_id;
    }

    public String toString() {
        return "Bidder ID: " + this.bidder_id + " Amount:" + this.amount;
    }
}
