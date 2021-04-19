package chainUtils;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;

/////////////////////////////////////////////////////
// Our transaction is a String of CURD by default, //
// but still create this Transaction class in case //
// of we wanna have some fancy implementation      //
/////////////////////////////////////////////////////
public class Transaction {
    public String transactionId; //Contains a hash of transaction*
    public PublicKey sender; //Senders address/public key.
    public byte[] signature;
    public String input; // List of CRUD
    public long timeStamp;

    // Constructor:
    public Transaction(PublicKey from, String input) {
        this.sender = from;
        this.input = input;
        this.timeStamp = new Date().getTime();
        this.transactionId = calulateHash();
    }


    public void generateSignature(PrivateKey privateKey) {
        String data = StringUtil.getStringFromKey(sender) + input;
        signature = StringUtil.applyECDSASig(privateKey, data);
    }


    //   This is  Node's responsibility
    public boolean verifySignature() {
        String data = StringUtil.getStringFromKey(sender) + input;
        return StringUtil.verifyECDSASig(sender, data, signature);
    }

    private String calulateHash() {
        return StringUtil.applySha256(
                StringUtil.getStringFromKey(sender) +
                        input +
                        timeStamp
        );
    }
}
