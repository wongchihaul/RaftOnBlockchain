package raft.impl;

import client.KVAck;
import client.KVReq;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import raft.Consensus;
import raft.LogModule;
import raft.Node;
import raft.common.Code;
import raft.common.NodeStatus;
import raft.common.Peer;
import raft.entity.AppEntryParam;
import raft.entity.AppEntryResult;
import raft.entity.ReqVoteParam;
import raft.entity.ReqVoteResult;
import redis.clients.jedis.Jedis;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;


@Getter
@Setter
@ToString
@Builder
public class NodeIMPL implements Node {

    private static final Logger logger = Logger.getLogger(NodeIMPL.class.getName());

    // START of Raft properties configuration
    /**
     * initialized to 0 on first boot, increases monotonically
     */
    long currentTerm = 0;

    /**
     * candidateId that received vote in current term (or null if none)
     */
    String votedFor = null;

    /**
     * log entries; each entry contains command
     * for state machine, and term when entry
     * was received by leader (first index is 1)
     * And we will have CRUD of log entries in Redis
     */
    LogModule logModule;

    /**
     * Options: FOLLOWER(0), CANDIDATE(1), LEADER(2)
     * Initiate as a FOLLOWER;
     */
//    volatile int status = Code.NodeStatus.FOLLOWER;
    volatile NodeStatus status = NodeStatus.FOLLOWER;

    volatile Peer leader = null;

    long commitIndex = 0;

    long lastApplied = 0;

    /**
     * Set of peers, excluding self
     */
    private volatile Set<Peer> peerSet;

    public volatile long preElection = 0;

    public volatile long preHeartBeat = 0;
    // END of Raft properties configuration


    // START of Network and Redis configuration
    private String addr;

    Jedis jedis;

    private StateMachineIMPL stateMachine;
    // END of Network and Redis configuration

    private Consensus consensus;


    public NodeIMPL(String addr) {
        this.addr = addr;
        this.jedis = new Jedis(Peer.getIP(addr), Peer.getPort(addr));
        this.logModule = new LogModuleIMPL(this);
        this.peerSet = new HashSet<>();
        this.stateMachine = new StateMachineIMPL(this);
        this.consensus = new ConsensusIMPL(this);
    }

    @Override
    public ReqVoteResult handleReqVote(ReqVoteParam param) {
        logger.warning(String.format("request vote param info: %s", param));
        return consensus.requestVote(param);
    }

    @Override
    public AppEntryResult handlerAppEntry(AppEntryParam param) {
        logger.warning(String.format("Append Entry param info: %s", param));
        return consensus.appendEntry(param);
    }

    @Override
    public KVAck handleClientReq(KVReq req) {
        return null;
    }

    @Override
    public KVAck redirect(KVReq req) {
        return null;
    }
}
