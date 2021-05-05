package raft.tasks;

import org.checkerframework.checker.units.qual.A;
import org.checkerframework.checker.units.qual.C;
import raft.LogModule;
import raft.common.NodeStatus;
import raft.common.Peer;
import raft.common.ReqType;
import raft.concurrent.RaftThread;
import raft.concurrent.RaftThreadPool;
import raft.entity.LogEntry;
import raft.entity.ReqVoteParam;
import raft.entity.ReqVoteResult;
import raft.impl.NodeIMPL;
import raft.rpc.RPCReq;
import raft.rpc.RPCResp;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * When servers start up, they begin as followers.
 * 1. begins election when election timeout
 *   - increment its current term (currentTerm ++
 *   - transitions to candidate state
 *   - vote for itself
 *   - issues RequestVote RPCs in parallel to each of the other servers in the cluster.
 * 2. win election if it receives votes from a majority of the servers in the full cluster
 * for the same term.
 *   - send heartbeats to all
 * 3. candidate receive AppendEntries RPC
 *   - if leader's term as large as currentTerm
 *   - return to follower
 * 4. if timeout， start a new election
 */

public class LeaderElectionTask implements Runnable{

    private NodeIMPL node;
    private final static Logger LOGGER = Logger.getLogger(LeaderElectionTask.class.getName());
    private HeartBeatTask HeartBeatTask;

    public LeaderElectionTask(NodeIMPL node) {
        this.node = node;
    }
    public void run(){
        if(node.getStatus()== NodeStatus.LEADER || node.getStatus() == NodeStatus.CANDIDATE) {
            return;
        }

        //election timeout is (150-300ms), check if timeout
        long curTime = System.currentTimeMillis();
        Random rand = new Random();
        if(curTime - node.prevElectionTime < node.electionTimeOut + rand.nextInt(150)){
            return;
        }

        //begin election
        node.setStatus(NodeStatus.CANDIDATE);

        //increment term and vote for itself
        node.setCurrentTerm(node.getCurrentTerm()+1);
        node.setVotedFor(node.getAddr());

        LOGGER.info("node "+node.getPeer()+ "becomes a candidate and begins the election," +
                " " +
                "current term: " + node.getCurrentTerm() +" LastEntry: " + node.getLogModule().getLast()
                + "peerset: " + node.getPeerSet());
        node.prevElectionTime = curTime;

        //use future list to store the voting results
        List<Future<ReqVoteResult>> futureList = new ArrayList<>();

        //issues RequestVote RPCs in parallel to each of the other servers
        for(Peer peer : node.getPeerSet()){
            LogModule logModule = node.getLogModule();

            //send req, and store the result as future in the futureList
            futureList.add(RaftThreadPool.submit(() -> {
                long lastTerm = 0;
                LogEntry curlast = logModule.getLast();
                if(curlast != null){
                    lastTerm = curlast.getTerm();
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
                RPCResp<ReqVoteResult> rpcResp = node.getRpcClient().sendReq(rpcReq);
                return rpcResp;
            }));
        }

        LOGGER.info("future list size: " + futureList.size());

        //read the results
        AtomicInteger votesCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(futureList.size());

        //Candidate receive AppendEntries RPC
        for(Future<ReqVoteResult> future: futureList){
            RaftThreadPool.submit(() -> {
                    ReqVoteResult voteResult = future.get();
                    if(voteResult.isVoteGranted()){
                        votesCount.incrementAndGet();
                    }else{
                        long newTerm = voteResult.getTerm();
                        if(newTerm > node.getCurrentTerm()){
                            node.setCurrentTerm(newTerm);
                        }
                    }
                    latch.countDown();
                    return 0;
            });
        }
        //candidate receive AppendEntries RPC,  if leader's term as large as currentTerm, return
        // to follower
        if(node.getStatus()==NodeStatus.FOLLOWER){
            LOGGER.info("candidate receive AppendEntries RPC from valid leader, return to " +
                    "follower");
            return;
        }

        //check votes from a majority of the servers, add vote from itself
        if(votesCount.get()+1 > (node.getPeerSet().size()+1)/2){
            LOGGER.info("The Node " +  node.getAddr() + " becomes leader");
            node.setStatus(NodeStatus.LEADER);
            RaftThreadPool.execute(HeartBeatTask);


            node.setLeader(node.getPeer()); //set itself to leader

        }








    }








}
