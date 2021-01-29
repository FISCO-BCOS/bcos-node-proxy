#!/bin/bash

dirpath="$(cd "$(dirname "$0")" && pwd)"
cd $dirpath

APP_MAIN=org.fisco.bcos.proxy.Application
CLASSPATH='conf/:apps/*:lib/*'
CURRENT_DIR=$(pwd)/

processStatus=0
checkProcess(){
    server_pid=$(ps aux | grep java | grep $CURRENT_DIR | grep $APP_MAIN | awk '{print $2}')
    if [ -n "$server_pid" ]; then
        processStatus=1
    else
        processStatus=0
    fi
}

check(){
    checkProcess
    if [ $processStatus == 0 ]; then
        recordTime=`date "+%Y-%m-%d %H:%M:%S"`
        echo "[ $recordTime ] server $APP_MAIN is not running, now restart it." >> monitor.log
        bash start.sh >> monitor.log
    fi
}

while (( 1 ))
do
    check
    sleep 1m
done