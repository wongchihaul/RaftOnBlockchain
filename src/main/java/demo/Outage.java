package demo;

import raft.common.Peer;
import raft.common.PeerSet;
import raft.impl.NodeIMPL;

import java.util.ArrayList;

import static org.junit.Assert.assertNotEquals;

public class Outage {
    public static void main(String[] args) throws InterruptedException {

        ArrayList<NodeIMPL> nodeList = new ArrayList<>();
        NodeIMPL nodeIMPL = null;

        for (int i = 6380; i <= 6386; i++) {
            String addr = "localhost:" + (i + 100);
            String redisAddr = "localhost:" + i;
            Peer peer = new Peer(addr, redisAddr);
            PeerSet.peerSet.add(peer);
        }

        for (int i = 6380; i <= 6386; i++) {
            String addr = "localhost:" + (i + 100);
            String redisAddr = "localhost:" + i;
            nodeIMPL = new NodeIMPL(addr, redisAddr);
            nodeList.add(nodeIMPL);
        }

        nodeList.forEach(NodeIMPL::init);

        Thread.sleep(1000 * 10);

        Peer leader = PeerSet.leader;

        System.out.println(leader.getAddr());

        for (NodeIMPL node : nodeList) {
            if (node.getPeer().equals(leader)) {
                node.destroy();
                // It should start a new election now
            }
        }

        Thread.sleep(1000 * 10);

        nodeList.forEach(System.out::println);

        assertNotEquals(leader, PeerSet.leader);
    }
}
