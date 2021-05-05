package raft.impl;

import client.KVAck;
import client.KVReq;
import com.alipay.remoting.rpc.RpcClient;
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
import raft.concurrent.RaftThreadPool;
import raft.entity.AppEntryParam;
import raft.entity.AppEntryResult;
import raft.entity.ReqVoteParam;
import raft.entity.ReqVoteResult;
import raft.rpc.RPCClient;
import raft.rpc.RPCServer;
import raft.tasks.HeartBeatTask;
import redis.clients.jedis.Jedis;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;


@Getter
@Setter
@ToString
//@Builder
public class NodeIMPL implements Node {


    public static final int HEARTBEAT_TICK = 125;

    public static final Logger logger = Logger.getLogger(NodeIMPL.class.getName());


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
    volatile NodeStatus status = NodeStatus.FOLLOWER;

    volatile Peer leader = null;

    long commitIndex = 0;

    long lastApplied = 0;

    /**
     * Set of peers, excluding self
     */
    private volatile Set<Peer> peerSet;


    public volatile long prevElectionTime = 0;
    //the election timeouts are chosen randomly from a ﬁxed interval(150-300ms) as suggested
    public volatile int electionTimeOut = 150;

    public volatile long preHeartBeat = 0;
    // END of Raft properties configuration


    // START of Network and Redis configuration
    private String addr;
    private Peer peer;

    Jedis jedis;

    private StateMachineIMPL stateMachine;
    // END of Network and Redis configuration

    private Consensus consensus;

    private long lastHeartBeatTime = 0;

    private RPCClient rpcClient = new RPCClient();
//    private RPCServer rpcServer = new RPCServer(9000, this); //这里先随便写了个

    private HeartBeatTask heartBeatTask = new HeartBeatTask(this);


    public NodeIMPL(String addr) {
        this.addr = addr;
        this.peer = new Peer(addr);
        this.jedis = new Jedis(Peer.getIP(addr), Peer.getPort(addr));
        this.logModule = new LogModuleIMPL(this);
        this.peerSet = new HashSet<>();
        this.stateMachine = new StateMachineIMPL(this);
        this.consensus = new ConsensusIMPL(this);
    }

    public void run() {
        consensus = new ConsensusIMPL(this);
        RaftThreadPool.scheduleWithFixedDelay(heartBeatTask, 1000);

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
