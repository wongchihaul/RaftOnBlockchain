package chainUtils;

import lombok.Builder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;


public class Block implements Serializable {
    public String hash;
    public String previousHash;
    public String merkleRoot;
    public ArrayList<String> transactions = new ArrayList<>(); //List of CRUD
    public long timeStamp; //as number of milliseconds since 1/1/1970.

    //Block Constructor.
    public Block(String previousHash) {
        this.previousHash = previousHash;
        this.timeStamp = new Date().getTime();
        this.hash = calculateHash(); //Making sure we do this after we set the other values.
    }

    public Block(String hash, String previousHash, String merkleRoot, ArrayList<String> transactions, long timeStamp) {
        this.hash = hash;
        this.previousHash = previousHash;
        this.merkleRoot = merkleRoot;
        this.transactions = transactions;
        this.timeStamp = timeStamp;
    }

    //Add one transaction to this block
    public synchronized boolean addTransaction(String transaction) {
        //add transactions, then re-calculate the hash of this block
        if (transaction == null) {
            return false;
        } else {
            transactions.add(transaction);
            merkleRoot = StringUtil.getMerkleRoot(transactions);
            hash = calculateHash();
            return true;
        }
    }

    //Add bulk of transactions to this block
//    public synchronized boolean addTransaction(ArrayList<Transaction> transactions) {
//        //add transactions, then re-calculate the hash of this block
//        if (transactions == null) {
//            return false;
//        } else {
//            this.transactions.addAll(transactions);
//            merkleRoot = StringUtil.getMerkleRoot(this.transactions);
//            hash = calculateHash();
//            return true;
//        }
//    }

    // Jsonify the block
    public String blockJson() {
        return StringUtil.getJson(this);
    }

    //Calculate new hash based on blocks contents
    private String calculateHash() {
        return StringUtil.applySha256(
                previousHash +
                        timeStamp +
                        transactions
        );
    }

    @Override
    public String toString() {
        return "\nBlock{" +
                "\nhash='" + hash + '\'' +
                ", \npreviousHash='" + previousHash + '\'' +
                ", \nmerkleRoot='" + merkleRoot + '\'' +
                ", \ntransactions=" + transactions +
                ", \ntimeStamp=" + timeStamp +
                '}';
    }
}
