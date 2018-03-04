#!/usr/bin/env bash

#Producer
#Single thread, no replication
bin/kafka-topics.sh --create --zookeeper 192.168.99.100:12181,192.168.99.101:22181,192.168.99.102:32181 --replication-factor 1 --partitions 1 --topic benchmark-1-1-none
bin/kafka-producer-perf-test.sh --topic benchmark-1-1-none --num-records 15000000 --record-size 100 --throughput 15000000 --producer-props acks=1 bootstrap.servers=192.168.99.100:19092,192.168.99.101:29092,192.168.99.102:39092 buffer.memory=67108864 compression.type=none batch.size=8196
