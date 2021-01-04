# bcos-node-proxy

bcos-node-proxy （以下简称 proxy）一方面作为客户端与区块链节点进行通信，另一方面作为服务端监听 SDK 应用的请求。

```
SDK <------> bcos-node-proxy <------> FISCO BCOS node
```

proxy 作为区块链节点的代理，通过在 SDK 应用与区块链节点间的消息透传，实现以下功能：

- **网络隔离**：生产应用可通过在隔离区部署 bcos-node-proxy 进行消息转发，实现内外网网络隔离。
- **连接管理**：生产应用在具有大量 SDK 应用接入的场景下，可通过 proxy 控制区块链节点对外建立连接的数量。
- **证书扩展**：proxy 作为服务端/客户端建立的两类连接都需进行 SSL 双向验证，但允许分别使用不同证书（两类证书不要求同一 ca 签发，不要求相同证书类型）进行验证。

## 支持的 SDK 版本及功能说明

|             | 支持连接 proxy 的 SDK 版本 | 是否支持 SDK 国密连接 | 支持 SDK 的功能说明                    |
| ----------- | -------------------------- | --------------------- | -------------------------------------- |
| java sdk    | 2.0+                       | 否                    | 部署合约、调用合约、块高推送、事件推送 |

## 使用方法

本章节通过在单机上部署一条4节点的非国密版FISCO BCOS区块链、并将控制台作为具体的 SDK 应用为例，描述 bcos-node-proxy 的使用方法。以下按**区块链部署**、**proxy 部署**、**控制台部署**的顺序进行说明。

### 区块链部署

```bash
## 创建操作目录
cd ~ && mkdir -p fisco && cd fisco

## 下载脚本
curl -#LO https://github.com/FISCO-BCOS/FISCO-BCOS/releases/download/v2.7.1/build_chain.sh && chmod u+x build_chain.sh

## 生成节点目录
bash build_chain.sh -l 127.0.0.1:4 -p 30300,20200,8545

## 启动区块链
bash nodes/127.0.0.1/start_all.sh

## 检查节点启动情况
ps -ef | grep -v grep | grep fisco-bcos
tail -f nodes/127.0.0.1/node0/log/log* | grep "++++ Generating seal"
```

如果进程数为4，同时查询节点日志`nodes/127.0.0.1/node0/log/`发现不停输出`++++ Generating seal`，表示区块链部署成功。如果某步骤报错，或者进程数/日志不符合预期，可参考[文档](https://fisco-bcos-documentation.readthedocs.io/zh_CN/latest/docs/installation.html#fisco-bcos)。

### bcos-node-proxy 部署

```bash
## 切回 fisco 目录，获取源码
cd ~/fisco && git clone https://github.com/FISCO-BCOS/bcos-node-proxy.git

## 编译源码
## 注：目前源码位于`dev`分支
cd bcos-node-proxy/ && git checkout dev && ./gradlew build

## 修改配置文件
## 若上述区块链部署的操作中，节点未采用默认端口，需将`dist/conf/client/config.toml`中的20200替换成节点对应的`channel`端口。

## 配置证书
## `dist/conf/client`目录下证书需从节点的 sdk 目录获取，但`dist/conf/client`和`dist/conf/server`目录下的证书不要求同一 ca 签发
## 为简单化部署操作，client 和 server 使用同一套证书
cp -r ../nodes/127.0.0.1/sdk/* dist/conf/client
cp -r ../nodes/127.0.0.1/sdk/* dist/conf/server

## 启动 proxy
cd dist && bash start.sh
```

输出下述信息表明启动成功。

```
try to start proxy org.fisco.bcos.node.proxy.Main
    proxy org.fisco.bcos.node.proxy.Main start successfully.
```

### 控制台部署

```bash
## 切回 fisco 目录，获取控制台
cd ~/fisco && curl -#LO https://github.com/FISCO-BCOS/console/releases/download/v2.7.1/download_console.sh && bash download_console.sh

## 拷贝配置文件
cp -n console/conf/config-example.toml console/conf/config.toml

## 修改配置文件的端口为 proxy 监听的端口
sed -i "s/20200/8170/g" console/conf/config.toml

## 配置证书，需与 proxy 的 server 使用同一套证书
cp bcos-node-proxy/dist/conf/server/ca.crt console/conf/
cp bcos-node-proxy/dist/conf/server/sdk.crt console/conf/
cp bcos-node-proxy/dist/conf/server/sdk.key console/conf/

## 启动控制台
cd ~/fisco/console && bash start.sh
```

输出下述信息表明启动成功。

```
=============================================================================================
Welcome to FISCO BCOS console(2.6.0)！
Type 'help' or 'h' for help. Type 'quit' or 'q' to quit console.
 ________  ______   ______    ______    ______         _______    ______    ______    ______
|        \|      \ /      \  /      \  /      \       |       \  /      \  /      \  /      \
| $$$$$$$$ \$$$$$$|  $$$$$$\|  $$$$$$\|  $$$$$$\      | $$$$$$$\|  $$$$$$\|  $$$$$$\|  $$$$$$\
| $$__      | $$  | $$___\$$| $$   \$$| $$  | $$      | $$__/ $$| $$   \$$| $$  | $$| $$___\$$
| $$  \     | $$   \$$    \ | $$      | $$  | $$      | $$    $$| $$      | $$  | $$ \$$    \
| $$$$$     | $$   _\$$$$$$\| $$   __ | $$  | $$      | $$$$$$$\| $$   __ | $$  | $$ _\$$$$$$\
| $$       _| $$_ |  \__| $$| $$__/  \| $$__/ $$      | $$__/ $$| $$__/  \| $$__/ $$|  \__| $$
| $$      |   $$ \ \$$    $$ \$$    $$ \$$    $$      | $$    $$ \$$    $$ \$$    $$ \$$    $$
 \$$       \$$$$$$  \$$$$$$   \$$$$$$   \$$$$$$        \$$$$$$$   \$$$$$$   \$$$$$$   \$$$$$$

=============================================================================================
```

