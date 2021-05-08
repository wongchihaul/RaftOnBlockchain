package raft.common;

import java.util.HashSet;

public class PeerSet {
    public static HashSet<Peer> peerSet = new HashSet<>();

    public static Peer leader = null;

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
