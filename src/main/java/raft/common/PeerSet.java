package raft.common;

import raft.impl.NodeIMPL;

import java.util.HashSet;

public class PeerSet {
    public volatile static HashSet<Peer> peerSet = new HashSet<>();

    public volatile static Peer leader = null;


    public volatile static HashSet<NodeIMPL> nodes = new HashSet<>();

    public static HashSet<Peer> getOthers(Peer peer) {
        HashSet<Peer> otherPeers = new HashSet<>();
        for (Peer p : peerSet) {
            if (!p.equals(peer)) {
                otherPeers.add(p);
            }
        }
        return otherPeers;
    }
}
