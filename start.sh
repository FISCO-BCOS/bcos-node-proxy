#!/bin/bash

dirpath="$(cd "$(dirname "$0")" && pwd)"
cd $dirpath

APP_MAIN=org.fisco.bcos.proxy.Application
CLASSPATH='conf/:apps/*:lib/*'
CURRENT_DIR=$(pwd)/
LOG_DIR=${CURRENT_DIR}log
CONF_DIR=${CURRENT_DIR}conf

SERVER_PORT=$(cat $CONF_DIR/application.yml| grep "port" | awk '{print $2}'| sed 's/\r//')
if [ ${SERVER_PORT}"" = "" ];then
    echo "$CONF_DIR/application.yml server port has not been configured"
    exit -1
fi

if [ ${JAVA_HOME}"" = "" ];then
    echo "JAVA_HOME has not been configured"
    exit -1
fi

mkdir -p log

startWaitTime=30
processPid=0
processStatus=0
checkProcess(){
    server_pid=$(ps aux | grep java | grep $CURRENT_DIR | grep $APP_MAIN | awk '{print $2}')
    if [ -n "$server_pid" ]; then
        processPid=$server_pid
        processStatus=1
    else
        processPid=0
        processStatus=0
    fi
}

JAVA_OPTS=" -Dfile.encoding=UTF-8"
JAVA_OPTS+=" -Djava.security.egd=file:/dev/./urandom"
JAVA_OPTS+=" -Xmx256m -Xms256m -Xmn128m -Xss512k -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=256m"
JAVA_OPTS+=" -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=${LOG_DIR}/heap_error.log"


start(){
    echo "try to start server $APP_MAIN"
    recordTime=`date "+%Y-%m-%d %H:%M:%S"`
    echo "[ $recordTime ] try to start server $APP_MAIN" >> record.log
    checkProcess
    if [ $processStatus == 1 ]; then
        echo "    server $APP_MAIN is running, pid is $processPid."
    else
        nohup $JAVA_HOME/bin/java -Djdk.tls.namedGroups="secp256k1" $JAVA_OPTS -cp $CLASSPATH $APP_MAIN >> $LOG_DIR/proxy.out 2>&1 &
        
        count=1
        result=0
        while [ $count -lt $startWaitTime ] ; do
            checkProcess
            if [ $processPid -ne 0 ]; then
                result=1
                break
            fi
            let count++
            echo -n "."
            sleep 1
        done

        sleep 5
        checkProcess
        if [ $processPid -ne 0 ]; then
            result=1
        else
            result=0
        fi
        
        if [ $result -ne 0 ]; then
            echo "    server $APP_MAIN start successfully."
        else
            echo "    server $APP_MAIN start fail, please query logs for the cause of failure."
        fi
    fi
}

start
