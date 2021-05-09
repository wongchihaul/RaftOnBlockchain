package chainUtils;

import java.io.Serializable;
import java.util.ArrayList;

public class NoobChain implements Serializable {

    private ArrayList<Block> blockchain = new ArrayList<>();

    public NoobChain() {
        Block genesis = new Block("0");
        addBlock(genesis);
    }

    public ArrayList<Block> getBlockchain() {
        return blockchain;
    }

    public synchronized void addBlock(Block block) {
        blockchain.add(block);
    }

    // show the blockchain
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        blockchain.forEach(b -> sb.append(b.blockJson()));
        return sb.toString();
    }
}
