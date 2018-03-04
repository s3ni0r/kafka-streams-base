# Setup a kafka cluster using docker-machine and docker-swarm

## Requirements
- Linux
```bash
~ ❯ cat /etc/*-release
...
NAME="Ubuntu"
VERSION="17.04 (Zesty Zapus)"
...
```

- docker
```bash
~ ❯ docker -v
Docker version 17.12.0-ce, build c97c6d6
```
- docker-compose
```bash
~ ❯ docker-compose -v
docker-compose version 1.13.0, build 1719ceb
```
- docker-machine
```bash
~ ❯ docker-machine -v
docker-machine version 0.13.0, build 9ba6da9
```
- virtualBox
```bash
~ ❯ vboxmanage -v                                                                                                                                                                                                                           ⏎
5.1.22_Ubuntur115126
```

## Hardware (Used in this setup)

- Intel(R) Core(TM) i7-4700HQ CPU @ 2.40GHz (8 cores)
- 1Gbit/s Killer E220x Gigabit Ethernet Controller
- 16GiB of Memory
- 1TB ATA Disk

## Setup docker-machine nodes

1. Create three docker-machine instances (node-1, node-2, node-3) with 4GB of memory and 2 CPU cores each:

```bash
# Use a loop if you like :)
docker-machine create --driver virtualbox --virtualbox-no-dns-proxy --virtualbox-memory "4096" --virtualbox-cpu-count "2" node-1
docker-machine create --driver virtualbox --virtualbox-no-dns-proxy --virtualbox-memory "4096" --virtualbox-cpu-count "2" node-2
docker-machine create --driver virtualbox --virtualbox-no-dns-proxy --virtualbox-memory "4096" --virtualbox-cpu-count "2" node-3
```

2. Setup node-1 as swarm manager :

```bash
# Get node-1 ip address
MANAGERIP=$(docker-machine ssh node-1 "ifconfig | sed -En 's/127.0.0.1//;s/.*inet (addr:)?(([0-9]*\.){3}[0-9]*).*/\2/p' | grep 99")
# Init swarm group and set node-1 as manager
docker-machine ssh node-1 "docker swarm init --advertise-addr $MANAGERIP --listen-addr $MANAGERIP"
```

3. Join node-2 and node-3 as workers:
```bash
# Get swarm group join token
SWARMTOKEN=$(docker-machine ssh node-1 "docker swarm join-token -q worker")
# join node-2 as workers
docker-machine ssh node-2 "docker swarm join --token $SWARMTOKEN $MANAGERIP:2377"
# join node-2 as workers
docker-machine ssh node-3 "docker swarm join --token $SWARMTOKEN $MANAGERIP:2377"
```

4. Make sure that your Linux host machine has the right dns in ```bash /etc/resolv.conf```
```bash
~ ❯ cat /etc/resolv.conf 
nameserver 8.8.8.8
```

5. Check that all nodes are up
```bash
~ ❯ docker-machine ls                                                                                                                                                                                                                       ⏎
NAME     ACTIVE   DRIVER       STATE     URL                         SWARM   DOCKER        ERRORS
node-1   -        virtualbox   Running   tcp://192.168.99.100:2376           v17.12.1-ce   
node-2   -        virtualbox   Running   tcp://192.168.99.101:2376           v17.12.1-ce   
node-3   -        virtualbox   Running   tcp://192.168.99.102:2376           v17.12.1-ce 
```

6. Check that swarm group is set up correctly
```bash
~ ❯ docker-machine ssh node-1 'docker node ls'
ID                            HOSTNAME            STATUS              AVAILABILITY        MANAGER STATUS
oqs2piqwdhl7axk59yunl4muu *   node-1              Ready               Active              Leader
wmu77zmdhq60ge579jxs6x7as     node-2              Ready               Active              
ukeq86oqsz0ove0lleic9xk0a     node-3              Ready               Active        
```

## Deploy docker stack into the swarm cluster

1. connect to the swarm manager node
```bash
~ ❯ docker-machine ssh node-1
                        ##         .
                  ## ## ##        ==
               ## ## ## ## ##    ===
           /"""""""""""""""""\___/ ===
      ~~~ {~~ ~~~~ ~~~ ~~~~ ~~~ ~ /  ===- ~~~
           \______ o           __/
             \    \         __/
              \____\_______/
 _                 _   ____     _            _
| |__   ___   ___ | |_|___ \ __| | ___   ___| | _____ _ __
| '_ \ / _ \ / _ \| __| __) / _` |/ _ \ / __| |/ / _ \ '__|
| |_) | (_) | (_) | |_ / __/ (_| | (_) | (__|   <  __/ |
|_.__/ \___/ \___/ \__|_____\__,_|\___/ \___|_|\_\___|_|
Boot2Docker version 17.12.1-ce, build HEAD : 42357fc - Wed Feb 28 17:52:00 UTC 2018
Docker version 17.12.1-ce, build 7390fc6
docker@node-1:~$ 
```

2. copy and paste the kafka-cluster-swarm.yml content and run the command below
```bash
docker@node-1:~$ docker stack deploy --compose-file kafka-cluster-swarm.yml kafka-cluster
```

3. navigate to container visualizer at http://192.168.99.100:8080 to check stack services status :)

## Fiddle with Kafka performance test 

`NB: You need to modify the stack Yaml file to suit configuration for your test here i show how to run them only`

1. on Linux host machine download Kafka and extract it somewhere

```bash
/tmp ❯ wget -qO- http://apache.crihan.fr/dist/kafka/1.0.0/kafka_2.12-1.0.0.tgz | tar xvz
/tmp ❯ cd kafka_2.12-1.0.0
```

2. launch a perf test
```bash
# create test topic
kafka_2.12-1.0.0 ❯ bin/kafka-topics.sh --create --zookeeper 192.168.99.100:12181,192.168.99.101:22181,192.168.99.102:32181 --replication-factor 1 --partitions 2 --topic benchmark-1-2-none
# run perf script
kafka_2.12-1.0.0 ❯ bin/kafka-producer-perf-test.sh --topic benchmark-2-2-none --num-records 15000000 --record-size 100 --throughput 15000000 --producer-props acks=1 bootstrap.servers=192.168.99.100:19092,192.168.99.101:29092 buffer.memory=67108864 compression.type=none batch.size=8196
1396651 records sent, 278272.8 records/sec (26.54 MB/sec), 1433.7 ms avg latency, 2278.0 max latency.
2113736 records sent, 422747.2 records/sec (40.32 MB/sec), 1423.1 ms avg latency, 1965.0 max latency.
2194914 records sent, 438982.8 records/sec (41.86 MB/sec), 1349.2 ms avg latency, 2281.0 max latency.
2185516 records sent, 437103.2 records/sec (41.69 MB/sec), 1398.3 ms avg latency, 2538.0 max latency.
2134530 records sent, 426906.0 records/sec (40.71 MB/sec), 1441.6 ms avg latency, 2425.0 max latency.
2186552 records sent, 437310.4 records/sec (41.71 MB/sec), 1360.0 ms avg latency, 2211.0 max latency.
2179966 records sent, 435993.2 records/sec (41.58 MB/sec), 1372.1 ms avg latency, 2256.0 max latency.
15000000 records sent, 407110.869860 records/sec (38.83 MB/sec), 1407.67 ms avg latency, 2538.00 ms max latency, 2010 ms 50th, 2385 ms 95th, 2487 ms 99th, 2536 ms 99.9th.
```


