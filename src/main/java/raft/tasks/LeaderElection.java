package raft.tasks;

import raft.LogModule;
import raft.common.NodeStatus;
import raft.common.Peer;
import raft.common.ReqType;
import raft.entity.LogEntry;
import raft.entity.ReqVoteParam;
import raft.entity.ReqVoteResult;
import raft.impl.NodeIMPL;
import raft.rpc.RPCReq;

import java.util.Random;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static raft.concurrent.RaftConcurrent.scheduler;

/**
 * When servers start up, they begin as followers.
 * 1. begins election when election timeout
 * - increment its current term (currentTerm ++
 * - transitions to candidate state
 * - vote for itself
 * - issues RequestVote RPCs in parallel to each of the other servers in the cluster.
 * 2. win election if it receives votes from a majority of the servers in the full cluster
 * for the same term.
 * - send heartbeats to all
 * 3. candidate receive AppendEntries RPC
 * - if leader's term as large as currentTerm
 * - return to follower
 * 4. if timeout， start a new election
 */
public class LeaderElection implements Runnable {
    private final static Logger LOGGER = Logger.getLogger(LeaderElection.class.getName());

    NodeIMPL node;

    ExecutorService exs;

    AtomicInteger votesCount;

    public LeaderElection(NodeIMPL node) {
        this.node = node;
        this.exs = Executors.newFixedThreadPool(4);     // 1 self +  4 peers = 5 nodes in total
    }

    @Override
    public void run() {
        if (node.getStatus() == NodeStatus.LEADER || node.getStatus() == NodeStatus.CANDIDATE) {
            return;
        }
        startElection();
    }


    void startElection() {
        if (node.getLeader() != null) {
            return;
        } else {

            Random rand = new Random();

            //begin election
            node.setStatus(NodeStatus.CANDIDATE);

            //increment term and vote for itself
            node.setCurrentTerm(node.getCurrentTerm() + 1);
            node.setVotedFor(node.getAddr());

            LOGGER.info("node " + node.getPeer() + "becomes a candidate and begins the election," +
                    " " +
                    "current term: " + node.getCurrentTerm() + " LastEntry: " + node.getLogModule().getLast()
                    + "peerset: " + node.getPeerSet());

            votesCount = new AtomicInteger(0);

            Set<Peer> peerSet = node.getPeerSet();

            //election timeout is (150-300ms)
            int timeout = NodeIMPL.ELECTION_TIMEOUT + rand.nextInt(150);

            CompletableFuture[] cfs = peerSet.stream()
                    .map(peer -> CompletableFuture.supplyAsync(() -> sendVoteReq(peer), this.exs)
                            .completeOnTimeout(ReqVoteResult.fail(null), timeout, TimeUnit.MILLISECONDS)
                            .thenAccept(voteResult -> handleVoteResp(voteResult)))
                    .toArray(CompletableFuture[]::new);

            CompletableFuture.allOf(cfs).join();

            //candidate receive AppendEntries RPC,  if leader's term as large as currentTerm, return
            // to follower
            if (node.getStatus() == NodeStatus.FOLLOWER) {
                LOGGER.info("candidate receive AppendEntries RPC from valid leader, return to " +
                        "follower");
                return;
            }

            //check votes from a majority of the servers, add vote from itself
            if (votesCount.get() + 1 > (node.getPeerSet().size() + 1) / 2) {
                LOGGER.info("The Node " + node.getAddr() + " becomes leader");
                node.setStatus(NodeStatus.LEADER);

                // Start heartbeat task
                HeartBeat heartBeat = new HeartBeat(node);
                ScheduledFuture<?> scheduledHB = scheduler.scheduleAtFixedRate(heartBeat, 0, NodeIMPL.HEARTBEAT_TICK, TimeUnit.MILLISECONDS);
                node.setScheduledHeartBeatTask(scheduledHB);

                node.setLeader(node.getPeer()); //set itself to leader

            } else {
                // no leader elected yet and start over
                node.setVotedFor(null);
                startElection();
            }
        }
    }


    ReqVoteResult sendVoteReq(Peer peer) {
        LogModule logModule = node.getLogModule();
        long lastTerm = 0;
        LogEntry currLast = logModule.getLast();
        if (currLast != null) {
            lastTerm = currLast.getTerm();
        }
        //update vote request param
        ReqVoteParam reqVoteParam = ReqVoteParam.builder()
                .term(node.getCurrentTerm())
                .candidateId(node.getAddr())
                .lastLogIndex(logModule.getLastIndex())
                .lastLogTerm(lastTerm)
                .build();
        //update RPC request object
        RPCReq rpcReq = RPCReq.builder()
                .requestType(ReqType.REQ_VOTE)
                .param(reqVoteParam)
                .addr(peer.getAddr())
                .build();
        //get the RPC response from client, and add the response to future list
        ReqVoteResult result = (ReqVoteResult) node.getRpcClient().sendReq(rpcReq).getResult();
        return result;
    }

    void handleVoteResp(ReqVoteResult voteResult) {
        if (voteResult.isVoteGranted()) {
            votesCount.incrementAndGet();
        } else {
            long paramTerm = voteResult.getTerm();
            if (paramTerm > node.getCurrentTerm()) {
                node.setCurrentTerm(paramTerm);
            }
        }
    }


}
