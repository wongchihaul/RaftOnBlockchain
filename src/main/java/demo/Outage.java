package demo;

import client.BlockChainTestClient;
import raft.common.Peer;
import raft.common.PeerSet;
import raft.impl.NodeIMPL;

import java.util.ArrayList;

import static org.junit.Assert.assertNotEquals;

public class Outage {
    static final int FIRST = 6380;
    static final int LAST = 6386;

    public static void main(String[] args) throws InterruptedException {
        BlockChainTestClient.disableWarning();
        ArrayList<NodeIMPL> nodeList = new ArrayList<>();
        NodeIMPL nodeIMPL = null;


        for (int i = FIRST; i <= LAST; i++) {
            String addr = "localhost:" + (i + 100);
            String redisAddr = "localhost:" + i;
            nodeIMPL = new NodeIMPL(addr, redisAddr);
            nodeList.add(nodeIMPL);
        }

        nodeList.forEach(NodeIMPL::init);

        Thread.sleep(1000 * 30);

        Peer leader = PeerSet.leader;


        NodeIMPL victim = null;
        for (NodeIMPL node : nodeList) {
            if (node.getPeer().equals(leader)) {
                victim = node;
            }
        }

        String lock = "lock";

        synchronized (lock){
            assert victim != null;
            System.out.println("=====================");
            System.out.println("Now stop the leader " + leader.getAddr());
            System.out.println("=====================");
            victim.destroy();
            PeerSet.leader = null;
            PeerSet.peerSet.remove(victim.getPeer());
            nodeList.remove(victim);
        }

//        // Remove another node to make the number of nodes odd.
//        synchronized (lock){
//            NodeIMPL anotherVictim = nodeList.get(nodeList.size() - 1);
//            System.out.println("=====================");
//            System.out.println("Now stop the one more server " + anotherVictim.getAddr());
//            System.out.println("=====================");
//            anotherVictim.destroy();
//            PeerSet.peerSet.remove(anotherVictim.getPeer());
//            nodeList.remove(anotherVictim);
//        }


        Thread.sleep(1000 * 10);

        // It should start a new election now
        assertNotEquals(leader, PeerSet.leader);
    }
}
