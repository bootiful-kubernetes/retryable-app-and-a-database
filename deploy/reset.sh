#!/usr/bin/env bash
## Reset Everything
kubectl delete deployments/mysql || echo "there is no mysql deployment to delete"
kubectl delete deployments/app || echo "there is no app deployment to delete"
kubectl delete services/mysql || echo "there is no app service to delete"
kubectl delete services/app || echo "there is no app service to delete"
kubectl delete configmap/app-config
kubectl delete configmap/mysql-config
