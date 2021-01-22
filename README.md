# bcos-node-proxy

FISCO BCOS 节点接入服务。Android/iOS 应用部署 proxy 后，Android/iOS 应用通过 http/https 请求 proxy，进而与节点进行通信。proxy 目前不支持涉及节点推送的相关特性。

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

（2）Proxy 作为服务端，需配置`application.yml`，默认监听端口为`8170`。

（3）Proxy 引入了`fisco-bcos-java-sdk`与节点通信，需配置`config.toml`中的节点地址，以及添加 sdk 证书到该目录下。

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

* 网络传输协议：使用HTTPS协议
* 请求地址：`/rpc/v1/`
* 请求方式：POST
* 请求头：Content-type: application/json
* 返回格式：JSON

#### 1.1.2 参数信息详情

| 序号 | 请求body | 类型     | 可为空 | 备注                                   |
| --- | -------- | ------- | ----- | ------------------------------------- |
| 1   | jsonrpc  | String  | 否    | jsonrpc版本，当前为2.0                   |
| 2   | method   | String  | 否    | jsonrpc方法名                           |
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

`https://127.0.0.1:8170/Bcos-node-proxy/rpc/v1`

```
// 调用合约写函数
{
    "jsonrpc": "2.0",
    "method": "sendRawTransaction",
    "params": [
        1,
        "0xf8ef9f65f0d06e39dc3c08e32ac10a5070858962bc6c0f5760baca823f2d5582d03f85174876e7ff8609184e729fff82020394d6f1a71052366dbae2f7ab2d5d5845e77965cf0d80b86448f85bce000000000000000000000000000000000000000000000000000000000000001bf5bd8a9e7ba8b936ea704292ff4aaa5797bf671fdc8526dcd159f23c1f5a05f44e9fa862834dc7cb4541558f2b4961dc39eaaf0af7f7395028658d0e01b86a371ca00b2b3fabd8598fefdda4efdb54f626367fc68e1735a8047f0f1c4f840255ca1ea0512500bc29f4cfe18ee1c88683006d73e56c934100b8abf4d2334560e1d2f75e"
    ],
    "id": 1
}
// 调用合约读函数
{
    "jsonrpc": "2.0",
    "method": "call",
    "params": [
        1,
        {
            "from": "0x0fc3c4bb89bd90299db4c62be0174c4966286c00",
            "to": "0x474d2d0e726f2f73ad40d2ed52bc2c82153bd0c1",
            "data": "0xd51033db"
        }
    ],
    "id": 2
}
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
// 调用合约写函数
{
    "code": 0,
    "message": "Success",
    "data": {
        "id": 1,
        "jsonrpc": "2.0",
        "result": {
            "blockHash": "0x1810d41ee8cf3e1ac423d08d6ee84164353e23407ec0acad5da4e008118678e7",
            "blockNumber": "0x472",
            "contractAddress": "0x0000000000000000000000000000000000000000",
            "from": "0x0fc3c4bb89bd90299db4c62be0174c4966286c00",
            "gasUsed": "0xab6a",
            "input": "0x7c1bf3c50000000000000000000000000000000000000000000000000000000000000014",
            "logs": [
                {
                    "address": "0x6a2a4699a7ac3f963c6026184850ebc28ec8ea6a",
                    "blockNumber": null,
                    "data": "0x0000000000000000000000000fc3c4bb89bd90299db4c62be0174c4966286c000000000000000000000000000000000000000000000000000000000000000014",
                    "topics": [
                        "0xaca9a02cfe513f3f88c54a860469369849c8fa0a2119a8d1f3f75c67ac0c9547"
                    ]
                }
            ],
            "logsBloom": "0x00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000080000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000008000000000080000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000080000000000000000400000",
            "message": null,
            "output": "0x0000000000000000000000000000000000000000000000000000000000000015",
            "receiptProof": null,
            "root": "0xc1c2231fb86a2e7351a810e1462553b5d6561f305e7362392326f0ac644c1d27",
            "status": "0x0",
            "statusMsg": "None",
            "to": "0x6a2a4699a7ac3f963c6026184850ebc28ec8ea6a",
            "transactionHash": "0x802f83a1ccd78f345daea3c8edb289fb3068bc565693439fa5ba6f0d570a4566",
            "transactionIndex": "0x0",
            "txProof": null,
            "statusOK": true
        },
        "error": null
    }
}
// 调用合约读函数
{
    "code": 0,
    "message": "Success",
    "data": {
        "id": 2,
        "jsonrpc": "2.0",
        "result": [
            1,
            "2"
        ]
    },
    "error": null
}
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

## <span id="2">2 返回码说明</span>

| code   | message                                                         | 
| ------ | --------------------------------------------------------------- |
| 0      | success                                                         | 
| 100000 | system exception                                                |
| 200100 | invalid rpc method                                              | 
| 200101 | unsupported rpc method                                          | 
| 200102 | invalid groupId                                                 |
| 200103 | inside json parser error                                        | 
| 300000 | param exception                                                 | 