# bcos-node-proxy

Bcos-node-proxy 作为 FISCO-BCOS 节点的接入代理，负责接收来自 Android/iOS sdk 等 http/https 请求，再以 ChannelMessage 协议向节点转发相关信息，节点的信息回复也通过 proxy 返回到 Android/iOS sdk。Proxy 一方面作为客户端与区块链节点进行通信，另一方面作为服务端监听 sdk 应用的请求。

```
Android/iOS sdk <---http/https---> bcos-node-proxy <---channel---> FISCO BCOS node
```

## 部署操作

### 1. 前提条件

| 序号 | 软件                         |
| ---- | ---------------------------- |
| 1    | Java8或以上版本，使用OpenJDK |

### 2. 拉取代码

执行命令：

```shell
git clone https://github.com/FISCO-BCOS/bcos-node-proxy.git && cd bcos-node-proxy && git checkout feature_mobile_http
```

### 3. 编译代码

使用gradlew编译：

```shell
./gradlew build
```

构建完成后，在根目录`bcos-node-proxy`下生成目录`dist`。

### 4. 服务配置及启停

#### 4.1 服务配置修改

（1）在dist目录，根据配置模板生成一份实际配置`conf`。

```shell
cp -r conf_template conf
```

（2）Proxy 作为服务端，默认监听端口为`8170`，该端口配置位于文件`conf/asServer/application.yml`，

（3）Proxy 作为客户端，通过引入`fisco-bcos-java-sdk`与节点通信，需配置`conf/asClient/config.toml`中的节点`Ip 和 Port`，以及添加 sdk（`ca.crt`、`sdk.crt`和`sdk.key`） 证书到`conf/asClient`目录下。

修改配置后，`conf`目录内容如下：

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

在dist目录下执行：

```shell
启动：
[app@VM_0_1_centos dist]$ bash start.sh
try to start server org.fisco.bcos.proxy.Application
    server org.fisco.bcos.proxy.Application start successfully.
停止：
[app@VM_0_1_centos dist]$ bash stop.sh
try to stop server org.fisco.bcos.proxy.Application
    server org.fisco.bcos.proxy.Application stop successfully.
检查：
[app@VM_0_1_centos dist]$ ps aux | grep "org.fisco.bcos.proxy.Application" | grep java
```

#### 4.3 查看日志

在dist目录查看：

```shell
全量日志：tail -f log/Bcos-node-proxy.log
错误日志：tail -f log/Bcos-node-proxy-error.log
```

## 接口说明

### <span id="1">1.1 RPC模块</span>

本接口可以用于：

- 部署合约和调用合约写函数，接口的请求操作将在链上进行记录；
- 调用合约读函数，接口的请求操作不在链上进行记录；
- 查询链状态（如块高）和数据（如Block内容），接口的请求操作不在链上进行记录。

#### 1.1.1 传输协议规范

* 网络传输协议：使用HTTP协议
* 请求地址：`/rpc/v1/`
* 请求方式：POST
* 请求头：Content-type: application/json
* 返回格式：JSON

#### 1.1.2 参数信息详情

| 序号 | 请求body | 类型     | 可为空 | 备注                                   |
| --- | -------- | ------- | ----- | ------------------------------------- |
| 1   | jsonrpc  | String  | 否    | jsonrpc版本，当前为2.0                   |
| 2   | method   | String  | 否    | jsonrpc方法名（目前支持6种，1.1.5说明）  |
| 3   | params   | List    | 否    | 请求内容列表（第一个内容为群组id)           |
| 4   | id       | Integer | 否    | jsonrpc序号                             |

| 序号   | 返回body | 类型     | 可为空 | 备注                                   |
| ----- | -------- | ------- | ----- | ------------------------------------- |
| 1     | code     | Integer | 否    | 返回码，0：成功，其它：失败                |
| 2     | message  | String  | 否    | code对应的描述                          |
| 3     | data     | Object  | 是    | 返回信息实体，JsonRpcResponse Json序列化结果|
| 3.1   | id       | Integer | 否    | jsonrpc序号，与请求body的id对应           |
| 3.2   | jsonrpc  | String  | 否    | jsonrpc版本，与请求body的jsonrpc对应      |
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

| method                | param id | param type | param content            |
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