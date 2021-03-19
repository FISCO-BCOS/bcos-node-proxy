### v1.0.0

(2021-03-18)

**新增**

bcos-node-proxy 通过 ChannelMessage 协议与 FISCO BCOS 节点进行交互，支持转发来自 Android/iOS SDK 的6种请求：

- 交易发送（sendRawTransaction 和 call）
- 查询节点二进制版本信息（getClientVersion）
- 查询块高（getBlockNumber）
- 基于交易 hash 查询交易内容（getTransactionByHash）
- 基于交易 hash 查询交易回执（getTransactionReceipt）

关于 bcos-node-proxy 的具体使用，请参考[文档](https://fisco-bcos-documentation.readthedocs.io/zh_CN/latest/docs/manual/bcos_node_proxy.html)。