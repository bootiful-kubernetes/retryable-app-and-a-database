#!/usr/bin/env bash

# this works!
# docker run -d -p 80:80 docker/getting-started
#
#
# docker run -it  --name postgres-db -e POSTGRES_USER=bp -e POSTGRES_PASSWORD=pw -it -p 5432:5432 -d postgres
docker run -it --name pdb -e POSTGRES_PASSWORD=pw -d postgres
container_id=$(docker ps -a  | grep postgres | cut -f1 -d\ ) 
echo $container_id
docker logs $container_id

