## Introduction
In is the project, we implement the raft algorithm on the blockchain to solve the consensus problem.
## Requirements
- Java 11 
- Maven 3.6.0
- Redis 6.2.3

## Install Redis
### mac
```bash
brew install redis
```
### Ubuntu
```bash
sudo add-apt-repository ppa:redislabs/redis
sudo apt-get update
sudo apt-get install redis
```

## How To Run
### Start Redis first
#### Change directory to the folder where redis.conf located and start servers. Or;
```bash
cd ..../RaftOnBlockchain/redisConfigs/redis-6380 && redis-server redis.conf
cd ..../RaftOnBlockchain/redisConfigs/redis-6381 && redis-server redis.conf
cd ..../RaftOnBlockchain/redisConfigs/redis-6382 && redis-server redis.conf
cd ..../RaftOnBlockchain/redisConfigs/redis-6383 && redis-server redis.conf
cd ..../RaftOnBlockchain/redisConfigs/redis-6384 && redis-server redis.conf
cd ..../RaftOnBlockchain/redisConfigs/redis-6385 && redis-server redis.conf
cd ..../RaftOnBlockchain/redisConfigs/redis-6386 && redis-server redis.conf
```
#### Do it in a for loop
```shell
cd redisConfigs

for dir in */
  do
    (cd $dir && redis-server redis.conf)
  done
```

### Server
```bash
run demo.Raftpool in Intellij IDEA
```

### Client
#### Auto entry
```bash
run client.BlockChainAutoClient in Intellij IDEA
```
#### Manually entry
```bash
run client.BlockChainTestClient in Intellij IDEA
Or 
java -jar RaftOnBlockchain-2.0-SNAPSHOT.jar
```

