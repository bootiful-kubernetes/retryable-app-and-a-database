#!/usr/bin/env bash

PROJECT_ID=${GKE_PROJECT:-pgtm-jlong}
APP_NAME=app-and-db
cd $(dirname $0)/..
root_dir=$(pwd)
##
## Push the App To The Container Registry
docker rmi -f $(docker images -a -q)
mvn -DskipTests=true clean spring-boot:build-image
image_id=$(docker images -q $APP_NAME)
docker tag $image_id gcr.io/${PROJECT_ID}/${APP_NAME}
docker push gcr.io/${PROJECT_ID}/${APP_NAME}
docker pull gcr.io/${PROJECT_ID}/${APP_NAME}:latest

##
## Reset
kubectl delete -f $root_dir/deploy/mysql.yaml
kubectl delete -f $root_dir/deploy/app.yaml
kubectl delete secrets mysql-secrets

##
## Deploy
kubectl apply -f <(echo "
---
apiVersion: v1
kind: Secret
metadata:
  name: mysql-secrets
type: Opaque
stringData:
  MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
  MYSQL_DATABASE: bp
  MYSQL_USER: bp
  MYSQL_PASSWORD: ${MYSQL_PASSWORD}
")
kubectl apply -f $root_dir/deploy/mysql.yaml
kubectl apply -f $root_dir/deploy/app.yaml


# kubectl expose deployment  app --name=app --type=LoadBalancer --port 80 --target-port 8080
# to proxy the k8s service to a local port, you can use the following incantation
# kubectl port-forward wag 8080:8080
# run k get all and you'll see the services including the external IP