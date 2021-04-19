package chainUtils;

import java.util.ArrayList;

public class NoobChain {

    public static ArrayList<Block> blockchain = new ArrayList<>();

    public NoobChain() {
        Block genesis = new Block("0");
        addBlock(genesis);
    }

    public synchronized void addBlock(Block block) {
        blockchain.add(block);
    }

    // show the blockchain
    public void show() {
        blockchain.forEach(Block::blockJson);
    }

}
