# bcos-node-proxy

Bcos-node-proxy 作为 FISCO-BCOS 节点的接入代理，负责接收来自 Android/iOS sdk 的 http/https 请求，再通过 ChannelMessage 协议向节点转发相关信息。节点的信息回复也经由 proxy 返回到 Android/iOS sdk。Proxy 一方面监听 Android/iOS sdk 的 http/https 请求，另一方面与区块链节点进行通信。

```
Android/iOS sdk <---http/https---> bcos-node-proxy <---ChannelMessage---> FISCO—BCOS node
```

说明：

1. 用户使用`Bcos-node-proxy`前需搭建 FISCO-BCOS 区块链，具体搭建方法可参考[文档](https://fisco-bcos-documentation.readthedocs.io/zh_CN/latest/docs/installation.html);
2. `Bcos-node-proxy`已实现以下6种请求的转发：交易发送（sendRawTransaction 和 call）、查询节点二进制版本信息（getClientVersion）、查询块高（getBlockNumber）、基于交易 hash 查询交易内容（getTransactionByHash），以及基于交易 hash 查询交易回执（getTransactionReceipt）；
3. `Bcos-node-proxy`尚不支持块高推送、事件推送、交易推送和 AMOP 通信等涉及节点推送的相关功能。 

## 部署操作

### 1. 前提条件

| 序号  | 软件                       |
| ---- | -------------------------- |
| 1    | Java8或以上版本，使用OpenJDK |

### 2. 拉取代码

执行命令：

```shell
git clone https://github.com/FISCO-BCOS/bcos-node-proxy.git && cd bcos-node-proxy && git checkout feature_mobile_http
```

### 3. 编译代码

使用 gradlew 编译：

```shell
./gradlew build
```

构建完成后，在根目录`bcos-node-proxy`下生成目录`dist`。

### 4. 服务配置及启停

#### 4.1 服务配置修改

（1）在`dist`目录，根据配置模板生成一份实际配置`conf`。

```shell
cp -r conf_template conf
```

（2）Proxy 默认使用端口`8170`监听 Android/iOS sdk 的 http/https 请求。该监听端口可在文件`conf/asServer/application.yml`中进行修改。

（3）Proxy 使用`fisco-bcos-java-sdk`与节点通信。用户需根据已搭建的 FISCO-BCOS 区块链情况在`conf/asClient/config.toml`中的[network.peers]设置节点`Ip 和 Port`，以及添加 sdk 证书（包括`ca.crt`、`sdk.crt`和`sdk.key`）到`conf/asClient`目录下。

修改配置后，`dist/conf`目录内容如下：

```
.
├── asClient
│   ├── ca.crt
│   ├── config.toml
│   ├── log4j.properties
│   ├── sdk.crt
│   └── sdk.key
└── asServer
    ├── application.yml
    └── log4j2.xml
```

#### 4.2 服务启停及状态检查

在`dist`目录下执行：

```shell
启动：
[app@VM_0_1_centos dist]$ ./start.sh
try to start server org.fisco.bcos.proxy.Application
    server org.fisco.bcos.proxy.Application start successfully.
停止：
[app@VM_0_1_centos dist]$ ./stop.sh
try to stop server org.fisco.bcos.proxy.Application
    server org.fisco.bcos.proxy.Application stop successfully.
检查：
[app@VM_0_1_centos dist]$ ps aux | grep "org.fisco.bcos.proxy.Application" | grep java
```

#### 4.3 查看日志

在`dist`目录查看：

```shell
全量日志：tail -f log/Bcos-node-proxy.log
错误日志：tail -f log/Bcos-node-proxy-error.log
启动日志：tail -f log/proxy.out
```

## 接口说明

### <span id="1">1 RPC 模块</span>

RPC 模块的接口可以用于：

- 部署合约和调用合约写函数，接口的请求操作将在链上进行记录；
- 调用合约读函数，接口的请求操作不在链上进行记录；
- 查询链状态（如块高）和链数据（如 Block 内容），接口的请求操作不在链上进行记录。

#### 1.1.1 传输协议规范

* 网络传输协议：使用`HTTP`协议
* 请求地址：`/rpc/v1/`
* 请求方式：POST
* 请求头：Content-type:application/json
* 返回格式：JSON

#### 1.1.2 参数信息详情

| 序号 | 请求body | 类型     | 可为空 | 备注                                   |
| --- | -------- | ------- | ----- | ------------------------------------- |
| 1   | jsonrpc  | String  | 否    | jsonrpc 版本，当前为 2.0                  |
| 2   | method   | String  | 否    | jsonrpc 方法名（目前支持6种，1.1.5 说明）  |
| 3   | params   | List    | 否    | 请求内容列表（第一个内容固定为群组 id)       |
| 4   | id       | Integer | 否    | jsonrpc 序号                             |

| 序号   | 返回body | 类型     | 可为空 | 备注                                   |
| ----- | -------- | ------- | ----- | ------------------------------------- |
| 1     | code     | Integer | 否    | 返回码，0：成功，其它：失败                |
| 2     | message  | String  | 否    | code 对应的描述                          |
| 3     | data     | Object  | 是    | 返回信息实体，JsonRpcResponse Json 序列化结果|
| 3.1   | id       | Integer | 否    | jsonrpc 序号，与请求 body 的 id 对应      |
| 3.2   | jsonrpc  | String  | 否    | jsonrpc 版本，与请求 body 的 jsonrpc 对应 |
| 3.3   | result   | Object  | 是    | 请求结果，与错误信息二选一                 |
| 3.4   | error    | Object  | 是    | 错误信息，与交易回执二选一                 |
| 3.4.1 | code     | Int     | 否    | FISCO BCOS 内部错误码，0：成功 其它：失败  |
| 3.4.2 | message  | String  | 否    | 描述                                   |
| 3.4.3 | data     | String  | 否    | 预留，默认为空                           |

#### 1.1.3 入参示例

`http://127.0.0.1:8170/Bcos-node-proxy/rpc/v1`

```
// 查询节点信息
{
    "jsonrpc": "2.0",
    "method": "getClientVersion",
    "params": [1],
    "id": 3
}
```

#### 1.1.4 出参示例

* 成功：

```
// 查询节点信息
{
    "code": 0,
    "message": "Success",
    "data": {
        "id": 3,
        "jsonrpc": "2.0",
        "result": {
            "Build Time": "20201208 11:00:41",
            "Build Type": "Linux/clang/Release",
            "Chain Id": "1",
            "FISCO-BCOS Version": "2.7.1",
            "Git Branch": "HEAD",
            "Git Commit Hash": "50a7ffba26a7ebc925b7e98483000daaacc67a8a",
            "Supported Version": "2.7.0"
        }
    }, 
    "error": null
}
```

* 失败：

```
{
    "code": 100000,
    "message": "system exception",
    "data": null
}
```

#### 1.1.5 RPC方法名及传参说明

| 方法名                 | 参数序号  | 参数类型     | 参数描述                 |
| --------------------- | -------- | ---------- | ------------------------ |
| getClientVersion      | 1        | Integer    | 群组 id                  |
| getBlockNumber        | 1        | Integer    | 群组 id                  |
| sendRawTransaction    | 1        | Integer    | 群组 id                  |
|                       | 2        | String     | 签名的交易数据（0x开头） |
| call                  | 1        | Integer    | 群组 id                  |
|                       | 2        | Object     | 交易                     |
|                       | 2.1      | String     | 交易发送方               |
|                       | 2.2      | String     | 合约地址                 |
|                       | 2.3      | String     | 交易数据（0x开头）       |
| getTransactionByHash  | 0        | Integer    | 群组 id                  |
|                       | 1        | String     | 交易 hash                |
| getTransactionReceipt | 0        | Integer    | 群组 id                  |
|                       | 1        | Integer    | 交易 hash                |

### <span id="2">2 返回码说明</span>

| code   | message                                                         | 
| ------ | --------------------------------------------------------------- |
| 0      | success                                                         | 
| 100000 | system exception                                                |
| 200100 | invalid rpc method                                              | 
| 200101 | unsupported rpc method                                          | 
| 200102 | invalid groupId                                                 |
| 200103 | inside json parser error                                        | 
| 300000 | param exception                                                 | 