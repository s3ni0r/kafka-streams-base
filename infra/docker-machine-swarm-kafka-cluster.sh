#!/usr/bin/env bash


docker-machine create --driver virtualbox --virtualbox-no-dns-proxy --virtualbox-memory "4096" --virtualbox-cpu-count "2" node-1
docker-machine create --driver virtualbox --virtualbox-no-dns-proxy --virtualbox-memory "4096" --virtualbox-cpu-count "2" node-2
docker-machine create --driver virtualbox --virtualbox-no-dns-proxy --virtualbox-memory "4096" --virtualbox-cpu-count "2" node-3

MANAGERIP=$(docker-machine ssh node-1 "ifconfig | sed -En 's/127.0.0.1//;s/.*inet (addr:)?(([0-9]*\.){3}[0-9]*).*/\2/p' | grep 99")

docker-machine ssh node-1 "docker swarm init --advertise-addr $MANAGERIP --listen-addr $MANAGERIP"

VBOXSWARMTOKEN=$(docker-machine ssh node-1 "docker swarm join-token -q worker")

docker-machine ssh node-2 "docker swarm join --token $VBOXSWARMTOKEN $MANAGERIP:2377"

docker-machine ssh node-3 "docker swarm join --token $VBOXSWARMTOKEN $MANAGERIP:2377"