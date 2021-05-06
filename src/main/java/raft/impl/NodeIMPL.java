package raft.impl;

import client.KVAck;
import client.KVReq;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import raft.Consensus;
import raft.LogModule;
import raft.Node;
import raft.common.NodeStatus;
import raft.common.Peer;
import raft.entity.*;
import raft.rpc.RPCClient;
import raft.rpc.RPCServer;
import raft.tasks.HeartBeatTask;
import raft.tasks.LeaderElection;
import redis.clients.jedis.Jedis;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import static raft.concurrent.RaftConcurrent.RaftThreadPool;


@Getter
@Setter
@ToString
//@Builder
public class NodeIMPL implements Node {


    public static final int HEARTBEAT_TICK = 125;

    public static final Logger LOGGER = Logger.getLogger(NodeIMPL.class.getName());

    public volatile boolean started;

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
    public static final int ELECTION_TIMEOUT = 150;


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
    private RPCServer rpcServer = new RPCServer(9000, this); //这里先随便写了个

    private HeartBeatTask heartBeatTask = new HeartBeatTask(this);

    //这里用来取消scheduled tasks
    private ScheduledFuture<?> scheduledHeartBeatTask;


    public NodeIMPL(String addr) {
        this.addr = addr;
        this.peer = new Peer(addr);
        this.jedis = new Jedis(Peer.getIP(addr), Peer.getPort(addr));
        this.logModule = new LogModuleIMPL(this);
        this.peerSet = new HashSet<>();
        this.stateMachine = new StateMachineIMPL(this);
        this.consensus = new ConsensusIMPL(this);
    }

    @Override
    public void init() {
        if (started) {
            return;
        }
        synchronized (this) {
            if (started) {
                return;
            }
            rpcServer.start();

            consensus = new ConsensusIMPL(this);
            LeaderElection leaderElection = new LeaderElection(this);

            RaftThreadPool.submit(leaderElection);

            LogEntry logEntry = logModule.getLast();
            if (logEntry != null) {
                currentTerm = logEntry.getTerm();
            }

            started = true;

            LOGGER.info("start success, selfId : " + this.getAddr());
        }
    }

    @Override
    public void destroy() {
        rpcServer.stop();
    }


    @Override
    public ReqVoteResult handleReqVote(ReqVoteParam param) {
        LOGGER.warning(String.format("request vote param info: %s", param));
        return consensus.requestVote(param);
    }

    @Override
    public AppEntryResult handleAppEntry(AppEntryParam param) {
        LOGGER.warning(String.format("Append Entry param info: %s", param));
        return consensus.appendEntry(param);
    }

    public KVAck handleClientReq(KVReq req) {
        LOGGER.warning(String.format("handlerClientRequest handler %s operation", KVReq.Type.value(req.getType())));


        if (status != NodeStatus.LEADER) {
            LOGGER.warning("I not am leader , only invoke redirect method");
            return redirect(req);
        }

        LogEntry logEntry = LogEntry.builder()
                .command(Command.newBuilder().
                        key(req.getKey()).
                        value(req.getValue()).
                        noobChain(req.getNoobChain()).
                        build())
                .term(currentTerm)
                .build();
        logModule.write(logEntry);
//        final AtomicInteger success = new AtomicInteger(0);
//        List<Future<Boolean>> futureList = new CopyOnWriteArrayList<>();
        int count = 0;
        //  复制到其他机器
//        for (Peer peer : peerSet.getPeersWithOutSelf()) {
//            // TODO check self and RaftThreadPool
//            count++;
//            // 并行发起 RPC 复制.
//            futureList.add(replication(peer, logEntry));
        return null;
    }

    @Override
    public KVAck redirect(KVReq req) {
        return null;
    }
}
