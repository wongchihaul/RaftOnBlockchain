## Dependency
Java v=11.*.* +
redIs v=6.2.3
## Deployment
##### Install Redis server
```
brew install redis
```
##### Change directory to the folder where redis.conf located and start the 5 servers
```
cd ..../RaftOnBlockchain/redisConfigs/redis-6380 && redis-server redis.conf
cd ..../RaftOnBlockchain/redisConfigs/redis-6381 && redis-server redis.conf
cd ..../RaftOnBlockchain/redisConfigs/redis-6382 && redis-server redis.conf
cd ..../RaftOnBlockchain/redisConfigs/redis-6383 && redis-server redis.conf
cd ..../RaftOnBlockchain/redisConfigs/redis-6384 && redis-server redis.conf
```
## Directory structure description
├── redisConfigs // redis data and configuration files
│   ├── redis-6380
│   ├── redis-6381
│   ├── redis-6382
│   ├── redis-6383
│   ├── redis-6384
│   ├── redis-6385
│   └── redis-6386
└── src
   ├── log4j2.xml //log configuration
   └── main
       └── java
           ├── chainUtils //Blockchain related documents
           │   ├── Block.java
           │   ├── BlockChainTest.java
           │   ├── NoobChain.java
           │   ├── StringUtil.java
           │   ├── Transaction.java
           │   └── Wallet.java
           ├── client
           │   ├── BlockChainAutoClient.java // Customer request test（without input）
           │   ├── BlockChainTestClient.java // Customer request test（with input）
           │   ├── KVAck.java
           │   └── KVReq.java
           ├── demo
           │   ├── NoRaftPool.java // Run server without raft 
           │   ├── Outage.java
           │   └── RaftPool.java // Run raft server 
           └── raft
               ├── common
               │   ├── NodeStatus.java
               │   ├── Peer.java
               │   ├── PeerSet.java
               │   ├── RDBParser.java
               │   ├── RedisPool.java
               │   └── ReqType.java
               ├── concurrent
               │   └── RaftConcurrent.java
               ├── entity // entity used in raft 
               │   ├── AppEntryParam.java
               │   ├── AppEntryResult.java
               │   ├── LogEntry.java
               │   ├── ReqVoteParam.java
               │   ├── ReqVoteResult.java
               │   └── Transaction.java
               ├── impl // interfave implement
               │   ├── ConsensusIMPL.java
               │   ├── LogModuleIMPL.java
               │   ├── NodeIMPL.java
               │   └── StateMachineIMPL.java
               ├── rpc
               │   ├── RPCClient.java
               │   ├── RPCReq.java
               │   ├── RPCResp.java
               │   └── RPCServer.java
               └── tasks
                   ├── HeartBeat.java
                   ├── LeaderElection.java
                   └── Replication.java
