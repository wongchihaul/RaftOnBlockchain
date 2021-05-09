## Install Redis
```
brew install redis
```
##### Recommended version: Redis server v=6.2.3

## Change directory to the folder where redis.conf located and start the 5 servers
```
cd ..../RaftOnBlockchain/redisConfigs/redis-6380 && redis-server redis.conf
cd ..../RaftOnBlockchain/redisConfigs/redis-6381 && redis-server redis.conf
cd ..../RaftOnBlockchain/redisConfigs/redis-6382 && redis-server redis.conf
cd ..../RaftOnBlockchain/redisConfigs/redis-6383 && redis-server redis.conf
cd ..../RaftOnBlockchain/redisConfigs/redis-6384 && redis-server redis.conf
```
