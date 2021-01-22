#!/bin/bash

dirpath="$(cd "$(dirname "$0")" && pwd)"
cd $dirpath

APP_MAIN=org.fisco.bcos.proxy.Application
CURRENT_DIR=$(pwd)/
CONF_DIR=${CURRENT_DIR}conf

SERVER_PORT=$(cat $CONF_DIR/application.yml| grep "port" | awk '{print $2}'| sed 's/\r//')
if [ ${SERVER_PORT}"" = "" ];then
    echo "$CONF_DIR/application.yml server port has not been configured"
    exit -1
fi

processPid=0
checkProcess(){
    server_pid=$(ps aux | grep java | grep $CURRENT_DIR | grep $APP_MAIN | awk '{print $2}')
    if [ -n "$server_pid" ]; then
        processPid=$server_pid
    else
        processPid=0
    fi
}

stop(){
	echo "try to stop server $APP_MAIN"
    recordTime=`date "+%Y-%m-%d %H:%M:%S"`
    echo "[ $recordTime ] try to stop server $APP_MAIN" >> record.log
	checkProcess
	if [ $processPid -ne 0 ]; then
	    kill -15 $processPid
	    if [ $? -eq 0 ]; then
	        echo "    server $APP_MAIN stop successfully."
	    else
	        echo "    server $APP_MAIN stop fail, please query logs for the cause of failure."
	    fi
	else
	    echo "    server $APP_MAIN isn't running."
	fi
}

stop