package chainUtils;

import java.security.Security;
import java.util.ArrayList;
import java.util.Base64;
import com.google.gson.GsonBuilder;



import java.security.*;



public class BlockChainTest {

    public static void main(String[] args) {
        NoobChain nc = new NoobChain();
        Block b1 = new Block(nc.getBlockchain().get(nc.getBlockchain().size()-1).hash);


        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        Wallet walletA = new Wallet();

        Transaction transaction = new Transaction(walletA.publicKey,"kkkk");
        //transaction.generateSignature(walletA.privateKey);

        b1.addTransaction("kkkk");
        nc.addBlock(b1);
        System.out.println(nc);



    }



}
