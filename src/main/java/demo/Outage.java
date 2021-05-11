package demo;

import raft.common.Peer;
import raft.common.PeerSet;
import raft.impl.NodeIMPL;

import java.util.ArrayList;

import static org.junit.Assert.assertNotEquals;

public class Outage {
    static final int FIRST = 6380;
    static final int LAST = 6384;

    public static void main(String[] args) throws InterruptedException {

        ArrayList<NodeIMPL> nodeList = new ArrayList<>();
        NodeIMPL nodeIMPL = null;

        for (int i = FIRST; i <= LAST; i++) {
            String addr = "localhost:" + (i + 100);
            String redisAddr = "localhost:" + i;
            Peer peer = new Peer(addr, redisAddr);
            PeerSet.peerSet.add(peer);
        }

        for (int i = FIRST; i <= LAST; i++) {
            String addr = "localhost:" + (i + 100);
            String redisAddr = "localhost:" + i;
            nodeIMPL = new NodeIMPL(addr, redisAddr);
            nodeList.add(nodeIMPL);
        }


        Thread.sleep(1000 * 10);

        Peer leader = PeerSet.leader;

        System.out.println("Now stop the leader " + leader.getAddr());

        for (NodeIMPL node : nodeList) {
            if (node.getPeer().equals(leader)) {
                node.destroy();
                // It should start a new election now
            }
        }

        Thread.sleep(1000 * 10);

        assertNotEquals(leader, PeerSet.leader);
    }
}
