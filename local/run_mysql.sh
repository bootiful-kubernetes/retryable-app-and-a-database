#!/usr/bin/env bash

CN=mysql-db
PW=password

rm -rf `pwd`/data

docker run --restart always -d  --name mysqldb \
 -e MYSQL_USER=bp \
 -e MYSQL_PASSWORD=bp \
 -e MYSQL_ROOT_PASSWORD=root \
 -e MYSQL_DATABASE=bp    \
 -p 3306:3306  \
 -v `pwd`/data:/var/lib/mysql mysql:8.0.20
echo "the password is '${PW}'."

