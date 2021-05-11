package demo;

import raft.common.Peer;
import raft.common.PeerSet;
import raft.impl.NodeIMPL;

import java.util.ArrayList;

public class RaftPool {
    public static void main(String[] args) {

        ArrayList<NodeIMPL> nodeList = new ArrayList<>();
        NodeIMPL nodeIMPL = null;
        for (int i = 6380; i <= 6384; i++) {
            String addr = "localhost:" + (i + 100);
            String redisAddr = "localhost:" + i;
            Peer peer = new Peer(addr, redisAddr);
            PeerSet.peerSet.add(peer);
        }
        for (int i = 6380; i <= 6384; i++) {
            String addr = "localhost:" + (i + 100);
            String redisAddr = "localhost:" + i;
            nodeIMPL = new NodeIMPL(addr, redisAddr);
            nodeList.add(nodeIMPL);
        }

        nodeList.forEach(NodeIMPL::init);
        for (; ; ) {

            try {
                Thread.sleep(1000 * 10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            nodeList.forEach(System.out::println);
            System.out.println(PeerSet.leader);
        }

    }
}
