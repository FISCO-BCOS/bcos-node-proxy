#!/bin/bash

dirpath="$(cd "$(dirname "$0")" && pwd)"
cd $dirpath

APP_MAIN=org.fisco.bcos.node.proxy.Main
CURRENT_DIR=$(pwd)/

processPid=0
checkProcess(){
    proxy_pid=$(ps aux | grep java | grep $CURRENT_DIR | grep $APP_MAIN | awk '{print $2}')
    if [ -n "$proxy_pid" ]; then
        processPid=$proxy_pid
    else
        processPid=0
    fi
}

stop(){
	echo "try to stop proxy $APP_MAIN"
    recordTime=`date "+%Y-%m-%d %H:%M:%S"`
    echo "[ $recordTime ] try to stop bcos-node-proxy $APP_MAIN" >> record.log
	checkProcess
	if [ $processPid -ne 0 ]; then
	    kill -15 $processPid
	    if [ $? -eq 0 ]; then
	        echo "    proxy $APP_MAIN stop successfully."
	    else
	        echo "    proxy $APP_MAIN stop fail, please query logs for the cause of failure."
	    fi
	else
	    echo "    proxy $APP_MAIN isn't running."
	fi
}

stop