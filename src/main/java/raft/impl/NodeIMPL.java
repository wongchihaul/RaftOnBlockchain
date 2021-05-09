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
import raft.common.ReqType;
import raft.concurrent.RaftConcurrent;
import raft.entity.*;
import raft.rpc.RPCClient;
import raft.rpc.RPCReq;
import raft.rpc.RPCResp;
import raft.rpc.RPCServer;
import raft.tasks.HeartBeatTask;
import raft.tasks.LeaderElection;
import raft.tasks.Replication;
import redis.clients.jedis.JedisPool;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static raft.common.PeerSet.getOthers;
import static raft.concurrent.RaftConcurrent.RaftThreadPool;
import static raft.concurrent.RedisPool.setConfig;


@Getter
@Setter
@ToString
//@Builder
public class NodeIMPL implements Node {


    public static final int HEARTBEAT_TICK = 125;

    public static final int ELECTION_TIMEOUT = 300;

    public static final int REPLICATION_TIMEOUT = 4000;

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
    volatile String votedFor = null;

    private String addr;

    /**
     * Options: FOLLOWER(0), CANDIDATE(1), LEADER(2)
     * Initiate as a FOLLOWER;
     */
    volatile NodeStatus status = NodeStatus.FOLLOWER;

    volatile Peer leader = null;

    /* ============ 所有服务器上经常变的 ============= */

    /**
     * 已知的最大的已经被提交的日志条目的索引值
     */
    volatile long commitIndex;

    /**
     * 最后被应用到状态机的日志条目索引值（初始化为 0，持续递增)
     */
    volatile long lastApplied = 0;



    /* ========== 在领导人里经常改变的(选举后重新初始化) ================== */

    /**
     * 对于每一个服务器，需要发送给他的下一个日志条目的索引值（初始化为领导人最后索引值加一）
     */
    Map<Peer, Long> nextIndexes = new HashMap<>() ;

    /**
     * 对于每一个服务器，已经复制给他的日志的最高索引值
     */
    Map<Peer, Long> latestIndexes = new HashMap<>();

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

    private Peer peer;

    JedisPool jedisPool;
    private String redisAddr;

    private StateMachineIMPL stateMachine;
    // END of Network and Redis configuration

    private Consensus consensus;

    private long lastHeartBeatTime = 0;

    private RPCClient rpcClient;
    private RPCServer rpcServer;

    private HeartBeatTask heartBeatTask = new HeartBeatTask(this);

    //这里用来取消scheduled tasks
    private ScheduledFuture<?> scheduledHeartBeatTask;

    /**
     * log entries; each entry contains command
     * for state machine, and term when entry
     * was received by leader (first index is 1)
     * And we will have CRUD of log entries in Redis
     */
    LogModule logModule;


    public NodeIMPL(String addr, String redisAddr) {
        this.addr = addr;
        this.redisAddr = redisAddr;
        this.peer = new Peer(addr, redisAddr);
        this.jedisPool = new JedisPool(setConfig(), Peer.getIP(redisAddr), Peer.getPort(redisAddr));
        this.logModule = new LogModuleIMPL(this);
        this.peerSet = getOthers(this.peer);
        this.stateMachine = new StateMachineIMPL(this);
        this.consensus = new ConsensusIMPL(this);
        this.rpcClient = new RPCClient();
        this.rpcServer = new RPCServer(Peer.getPort(addr), this);
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
//            LeaderElectionTask leaderElectionTask = new LeaderElectionTask(this);
//            RaftThreadPool.submit(leaderElection);
            Random rand = new Random();
//            RaftConcurrent.scheduler.scheduleAtFixedRate(leaderElection, 2000 + rand.nextInt(5) * 1000, 500, TimeUnit.MILLISECONDS);
            RaftConcurrent.scheduler.scheduleAtFixedRate(leaderElection, 3000, 500, TimeUnit.MILLISECONDS);
//            scheduler.scheduleAtFixedRate(leaderElectionTask, 6000, 500, TimeUnit.MICROSECONDS);
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
        LOGGER.warning(String.format("Node{%s} handle request vote param info: %s", this.getAddr(), param));
        return consensus.requestVote(param);
    }

    @Override
    public AppEntryResult handleAppEntry(AppEntryParam param) {
        LOGGER.info(String.format("Append Entry param info: %s", param));
        return consensus.appendEntry(param);
    }

    public KVAck handleClientReq(KVReq req) {
        LOGGER.warning(String.format("handlerClientRequest handler %s operation", req));

        //System.out.println("==============");
        if (status != NodeStatus.LEADER) {
            LOGGER.warning("I not am leader , only invoke redirect method");
            return redirect(req);
        }

        LogEntry logEntry = LogEntry.builder()
                .transaction(Transaction.builder().
                        key(req.getKey()).
                        value(req.getValue()).
                        noobChain(req.getNoobChain()).
                        build())
                .term(currentTerm)
                .build();
        logModule.write(logEntry);

        var replicaResult = RaftThreadPool.submit(new Replication(this, logEntry));

        while (!replicaResult.isDone()) {
            // wait
        }

        // in case interrupted before applying to state machine
        if (this.commitIndex == logEntry.getIndex() && this.lastApplied == logEntry.getIndex()) {
            return KVAck.builder().success(true).build();
        } else {
            return KVAck.builder().success(false).build();
        }

//        final AtomicInteger success = new AtomicInteger(0);
//        List<Future<Boolean>> futureList = new CopyOnWriteArrayList<>();

//        int count = 0;

        //  复制到其他机器
//        for (Peer peer : peerSet.getPeersWithOutSelf()) {
//            // TODO check self and RaftThreadPool
//            count++;
//            // 并行发起 RPC 复制.
//            futureList.add(replication(peer, logEntry));

//        return null;

    }

    @Override
    public KVAck redirect(KVReq req) {
        RPCReq redirectReq = RPCReq.builder()
                .addr(this.getLeader().getAddr())
                .param(req)
                .requestType(ReqType.KV)
                .build();
        RPCResp resp = rpcClient.sendReq(redirectReq);
        return (KVAck) resp.getResult();
    }
}
