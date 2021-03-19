# bcos-node-proxy

bcos-node-proxy 作为 FISCO-BCOS 节点的接入代理，负责接受 Android/iOS 终端 SDK 的 http/https 连接，对请求的内容进行解析，并通过内置的 java-sdk 走 ChannelMessage 协议向节点进行转发。bcos-node-proxy 层本身是无状态的，实践中可以启动多个 bcos-node-proxy 实例，通过负载均衡组件（如 LVS、HAProxy 或 F5）对外提供统一的接入地址，终端 SDK 的请求可以均匀地分摊在多个 bcos-node-proxy 实例上以达到负载均衡的效果。bcos-node-proxy 本身并不存储数据，只是解析终端 SDK 的请求，将实际的数据读取请求/交易请求转发给底层的 FISCO-BCOS 节点。进一步的，bcos-node-proxy 对请求中的上链操作（部署合约、调用合约写接口），进行了异步请求转同步的实现。

目前，bcos-node-proxy 支持 FISCO BCOS 2.0+，能处理以下6种请求：

- 交易发送（sendRawTransaction 和 call）
- 查询节点二进制版本信息（getClientVersion）
- 查询块高（getBlockNumber）
- 基于交易 hash 查询交易内容（getTransactionByHash）
- 基于交易 hash 查询交易回执（getTransactionReceipt）

关于 bcos-node-proxy 的部署流程及接口说明，请看考[详细文档](https://fisco-bcos-documentation.readthedocs.io/zh_CN/latest/docs/manual/bcos_node_proxy.html)。