package org.fisco.bcos.node.proxy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.AttributeKey;
import java.io.File;
import java.io.FileInputStream;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.fisco.bcos.node.config.ServerConfig;
import org.fisco.bcos.node.config.ServerConfigOption;
import org.fisco.bcos.node.model.EventMsgContent;
import org.fisco.bcos.sdk.channel.ChannelVersionNegotiation;
import org.fisco.bcos.sdk.channel.ResponseCallback;
import org.fisco.bcos.sdk.channel.model.*;
import org.fisco.bcos.sdk.config.Config;
import org.fisco.bcos.sdk.config.ConfigOption;
import org.fisco.bcos.sdk.config.exceptions.ConfigException;
import org.fisco.bcos.sdk.model.Message;
import org.fisco.bcos.sdk.model.MsgType;
import org.fisco.bcos.sdk.model.NodeVersion;
import org.fisco.bcos.sdk.model.Response;
import org.fisco.bcos.sdk.network.*;
import org.fisco.bcos.sdk.network.ChannelHandler;
import org.fisco.bcos.sdk.utils.ChannelUtils;
import org.fisco.bcos.sdk.utils.ObjectMapperFactory;
import org.fisco.bcos.sdk.utils.ThreadPoolService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Server {
    private static Logger logger = LoggerFactory.getLogger(Server.class);
    private final ObjectMapper objectMapper = ObjectMapperFactory.getObjectMapper();

    private Boolean running = false;
    ConfigOption clientConfigOption;
    ServerConfigOption serverConfigOption;
    private ServerBootstrap serverBootstrap = new ServerBootstrap();
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    ConnectionManager sdkConnectionManager;
    ConnectionManager nodeConnectionManager;
    private Map<String, ConnectionPair> seq2Connections = new ConcurrentHashMap<>();
    private ThreadPoolService threadPoolServiceSDK;
    private Map<String, String> filterId2SDKConnect = new ConcurrentHashMap<>();
    private Map<String, EnumChannelProtocolVersion> sdkHost2Version = new ConcurrentHashMap<>();

    // only with nodes
    private Integer connectSeconds = 30;
    private Integer connectSleepPerMillis = 30;
    private long heartBeatDelay = (long) 2000;
    private ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(1);
    private Map<String, ResponseCallback> seq2CallbackNodes = new ConcurrentHashMap<>();
    private ThreadPoolService threadPoolServiceNode;
    private boolean startHeardBeat = false;
    // only with nodes end

    Server(String clientConfigPath, String serverConfigFile) {
        try {
            logger.debug(
                    "Init proxy server, clientConfigPath: {}, serverConfigPath: {}",
                    clientConfigPath,
                    serverConfigFile);
            clientConfigOption = Config.load(clientConfigPath);
            serverConfigOption = ServerConfig.load(serverConfigFile);
        } catch (ConfigException e) {
            logger.error("load config failed, error info: " + e.getMessage());
        }
    }

    public boolean start() {
        Security.setProperty("jdk.disabled.namedCurves", "");
        System.setProperty("jdk.sunec.disableNative", "false");
        try {
            if (running) {
                logger.error("proxy server is already running");
                return false;
            }
            running =
                    startConnect()
                            && startListen(
                                    clientConfigOption
                                            .getCryptoMaterialConfig()
                                            .getSslCryptoType());
            logger.info("start proxy server result: " + running);
            if (!running) {
                stop();
            }
        } catch (Exception e) {
            logger.error("start proxy server failed, error info: " + e.getMessage());
            stop();
        }
        return running;
    }

    public void stop() {
        if (!running) {
            logger.error("proxy server is already stopped");
        } else {
            if (bossGroup != null) {
                bossGroup.shutdownGracefully();
            }
            if (workerGroup != null) {
                workerGroup.shutdownGracefully();
            }
            if (threadPoolServiceSDK != null) {
                threadPoolServiceSDK.stop();
            }
            if (threadPoolServiceNode != null) {
                threadPoolServiceNode.stop();
            }
            ThreadPoolService.stopThreadPool(scheduledExecutorService);
            running = false;
        }
    }

    private boolean startListen(int sslCryptoType) {
        logger.info("try to start proxy listen, sslCryptoType:" + sslCryptoType);
        try {
            // Init SslContext
            FileInputStream caCert =
                    new FileInputStream(
                            new File(serverConfigOption.getCryptoMaterialConfig().getCaCertPath()));
            FileInputStream proxyCert =
                    new FileInputStream(
                            new File(
                                    serverConfigOption.getCryptoMaterialConfig().getSdkCertPath()));
            FileInputStream proxyKey =
                    new FileInputStream(
                            new File(
                                    serverConfigOption
                                            .getCryptoMaterialConfig()
                                            .getSdkPrivateKeyPath()));
            SslContext sslContext =
                    SslContextBuilder.forServer(proxyCert, proxyKey)
                            .trustManager(caCert)
                            .sslProvider(SslProvider.OPENSSL)
                            .build();

            ConnectionMsgHandler sdkConnectionCallback = new ConnectionMsgHandler();
            sdkConnectionCallback.setServer(this);
            sdkConnectionCallback.setFromNodes(false);
            List<String> ipList = new ArrayList<>();
            sdkConnectionManager = new ConnectionManager(ipList, sdkConnectionCallback);
            ChannelHandler channelHandler =
                    new ChannelHandler(sdkConnectionManager, sdkConnectionCallback);
            threadPoolServiceSDK =
                    new ThreadPoolService(
                            "proxySDKProcessor",
                            clientConfigOption
                                    .getThreadPoolConfig()
                                    .getChannelProcessorThreadSize(),
                            clientConfigOption.getThreadPoolConfig().getMaxBlockingQueueSize());
            channelHandler.setMsgHandleThreadPool(threadPoolServiceSDK.getThreadPool());

            bossGroup = new NioEventLoopGroup();
            workerGroup = new NioEventLoopGroup();
            serverBootstrap.group(bossGroup, workerGroup);
            serverBootstrap.channel(NioServerSocketChannel.class);
            serverBootstrap.childHandler(
                    new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            SslHandler sslHandler = sslContext.newHandler(ch.alloc());
                            ch.pipeline()
                                    .addLast(
                                            sslHandler,
                                            new LengthFieldBasedFrameDecoder(
                                                    Integer.MAX_VALUE, 0, 4, -4, 0),
                                            new IdleStateHandler(
                                                    TimeoutConfig.idleTimeout,
                                                    TimeoutConfig.idleTimeout,
                                                    TimeoutConfig.idleTimeout,
                                                    TimeUnit.MILLISECONDS),
                                            new HttpObjectAggregator(512 * 1024),
                                            new MessageEncoder(),
                                            new MessageDecoder(),
                                            channelHandler);
                        }
                    });
            serverBootstrap.option(ChannelOption.SO_BACKLOG, 128);
            serverBootstrap.childOption(ChannelOption.SO_SNDBUF, 32 * 1024);
            serverBootstrap.childOption(ChannelOption.SO_RCVBUF, 32 * 1024);
            serverBootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
            int listenPort =
                    Integer.parseInt(serverConfigOption.getNetworkConfig().getListenPort());
            ChannelFuture future = serverBootstrap.bind(listenPort);
            future.get();
            logger.info("proxy server started at port {} successfully ", listenPort);
            return true;
        } catch (Exception e) {
            logger.error("start proxy listen failed, error info: " + e.getMessage());
            return false;
        }
    }

    private boolean startConnect() {
        logger.info("try to start proxy connect");
        try {
            ConnectionMsgHandler nodeConnectionCallback = new ConnectionMsgHandler();
            nodeConnectionCallback.setServer(this);
            nodeConnectionCallback.setFromNodes(true);
            Network network = Network.build(clientConfigOption, nodeConnectionCallback);
            network.start();
            nodeConnectionManager = network.getConnManager();
            checkConnectionWithNodesToStartPeriodTask();
            threadPoolServiceNode =
                    new ThreadPoolService(
                            "proxyNodeProcessor",
                            clientConfigOption
                                    .getThreadPoolConfig()
                                    .getChannelProcessorThreadSize(),
                            clientConfigOption.getThreadPoolConfig().getMaxBlockingQueueSize());
            network.setMsgHandleThreadPool(threadPoolServiceNode.getThreadPool());
        } catch (Exception e) {
            logger.error("start proxy connect failed, error info: " + e.getMessage());
        }
        return getAvailableNodePeers().size() > 0;
    }

    public void onNodeConnect(ChannelHandlerContext ctx) {
        queryNodeVersion(ctx);
    }

    public void onNodeMessage(ChannelHandlerContext ctx, Message message) {
        if (!startHeardBeat
                || message.getType() == Short.valueOf((short) MsgType.CLIENT_HEARTBEAT.getType())) {
            // queryNodeVersion message
            // queryChannelProtocolVersion message
            // heardBeat message
            onNodeMessageWithNode(ctx, message);
        } else {
            // other message
            onNodeMessageToSDK(ctx, message);
        }
    }

    public void onSDKMessage(ChannelHandlerContext ctx, Message message) {
        if (message.getType() == Short.valueOf((short) MsgType.CLIENT_HEARTBEAT.getType())) {
            onHeartBeatFromSDK(ctx, message);
        } else {
            if (message.getType()
                    == Short.valueOf((short) MsgType.CLIENT_REGISTER_EVENT_LOG.getType())) {
                onRegisterEventMsg(ctx, message);
            } else if (message.getType()
                    == Short.valueOf((short) MsgType.CLIENT_UNREGISTER_EVENT_LOG.getType())) {
                onUnregisterEventMsg(ctx, message);
            }
            onSDKMessageToNode(ctx, message);
        }
    }

    public void onSDKDisconnect(ChannelHandlerContext ctx) {
        String sdkHost = ChannelVersionNegotiation.getPeerHost(ctx);
        if (sdkHost2Version.get(sdkHost) != null) {
            sdkHost2Version.remove(sdkHost);
            logger.info("onDisconnectSDK, drop recode version with sdk " + sdkHost);
        } else {
            logger.error("onDisconnectSDK failed, cannot get version from " + sdkHost);
        }
    }

    private void onNodeMessageWithNode(ChannelHandlerContext ctx, Message message) {
        ResponseCallback callback = seq2CallbackNodes.get(message.getSeq());
        seq2CallbackNodes.remove(message.getSeq());
        if (callback != null) {
            callback.cancelTimeout();
            logger.trace(
                    "call registered callback only with node, seq: {}, type: {} ,result: {}",
                    message.getSeq(),
                    message.getType(),
                    message.getResult());

            Response response = new Response();
            if (message.getResult() != 0) {
                response.setErrorMessage("Response error");
            }
            response.setErrorCode(message.getResult());
            response.setMessageID(message.getSeq());
            response.setContentBytes(message.getData());
            response.setCtx(ctx);
            callback.onResponse(response);
        } else {
            logger.error("callback only with node is null in onNodeMessage");
        }
    }

    private void onNodeMessageToSDK(ChannelHandlerContext ctx, Message message) {
        try {
            String seq = message.getSeq();
            Short type = message.getType();
            if (seq.equals("00000000000000000000000000000000")) {
                if (type.shortValue() == (short) MsgType.BLOCK_NOTIFY.getType()) {
                    onBlockNotifyMessage(message);
                } else if (type.shortValue() == MsgType.EVENT_LOG_PUSH.getType()) {
                    onEventLogPushMessage(message);
                } else { // 4096 with no 0 seq, TRANSACTION_NOTIFY(0x1000)
                    logger.error("onNodeMessageToSDK, error {} type with 0 seq", type);
                }
                return;
            }

            ChannelHandlerContext sdkCtx = null;
            ConnectionPair pair = seq2Connections.get(seq);
            if (pair != null) {
                logger.debug("seq existed");
                sdkCtx = pair.sdkConnection;
            }
            if (sdkCtx != null && sdkCtx.channel().isActive()) {
                String sdkHost = ChannelVersionNegotiation.getPeerHost(sdkCtx);
                logger.debug("send node message to sdk " + sdkHost);
                if (type == Short.valueOf((short) MsgType.CLIENT_HANDSHAKE.getType())) {
                    onHandShakeMsg(sdkHost, message);
                }
                sdkCtx.writeAndFlush(message);
            } else {
                logger.error("send node message to sdk failed, ctx is null");
            }
        } catch (Exception e) {
            logger.error("send node message to sdk failed, error info:", e.getMessage());
        }
    }

    private void onHeartBeatFromSDK(ChannelHandlerContext ctx, Message message) {
        try {
            String sdkHost = ChannelVersionNegotiation.getPeerHost(ctx);
            EnumChannelProtocolVersion version = sdkHost2Version.get(sdkHost);
            if (version == null) {
                logger.error("onHeartBeatFromSDK failed, cannot get version from " + sdkHost);
            }
            String content;
            if (version == EnumChannelProtocolVersion.VERSION_1) {
                content = "1";
            } else {
                content = "{\"heartBeat\":1}";
            }
            message.setData(content.getBytes());
            ctx.writeAndFlush(message);
        } catch (Exception e) {
            logger.error("onHeartBeatFromSDK failed, error info: " + e.getMessage());
        }
    }

    private void onBlockNotifyMessage(Message message) {
        Map<String, ChannelHandlerContext> contextMap =
                sdkConnectionManager.getAvailableConnections();
        if (contextMap.size() == 0) {
            logger.warn("onBlockNotifyMessage failed, none sdk can be sent to");
        }
        contextMap.forEach(
                (peer, ctx) -> {
                    logger.trace("onBlockNotifyMessage is sent to sdk " + peer);
                    ctx.writeAndFlush(message);
                });
    }

    private void onEventLogPushMessage(Message message) {
        try {
            String content = new String(message.getData()).trim();
            EventMsgContent resp = objectMapper.readValue(content, EventMsgContent.class);
            String peerSDK = filterId2SDKConnect.get(resp.getFilterID());
            ChannelHandlerContext ctxSDK =
                    sdkConnectionManager.getAvailableConnections().get(peerSDK);
            if (ctxSDK != null) {
                ctxSDK.writeAndFlush(message);
                logger.trace("onEventLogPushMessage is sent to sdk " + peerSDK);
            } else {
                logger.warn("onEventLogPushMessage failed, none sdk can be sent to");
            }
        } catch (JsonProcessingException e) {
            logger.error("onEventLogPushMessage error, error info: " + e.getMessage());
        }
    }

    private void onHandShakeMsg(String sdkHost, Message message) {
        try {
            String content = new String(message.getData());
            ChannelProtocol channelProtocol =
                    objectMapper.readValue(content, ChannelProtocol.class);
            EnumChannelProtocolVersion enumChannelProtocolVersion =
                    EnumChannelProtocolVersion.toEnum(channelProtocol.getProtocol());
            sdkHost2Version.put(sdkHost, enumChannelProtocolVersion);
            logger.info(
                    "onHandShakeMsg, recode version {} to sdk {}",
                    enumChannelProtocolVersion,
                    sdkHost);
        } catch (Exception e) {
            logger.error("onHandShakeMsg failed, error info: " + e.getMessage());
        }
    }

    private void onSDKMessageToNode(ChannelHandlerContext ctx, Message message) {
        try {
            ConnectionPair pair = seq2Connections.get(message.getSeq());
            if (pair != null) {
                logger.warn("seq existed");
            } else {
                pair = new ConnectionPair();
                pair.sdkConnection = ctx;
                pair.setServer(this);
                pair.setMessage(message);
                pair.setNodeChannelConnections(nodeConnectionManager.getAvailableConnections());
                pair.setNodeConnectionInfos(nodeConnectionManager.getConnectionInfoList());
                seq2Connections.put(message.getSeq(), pair);
                pair.sendNodeMessage();
            }
        } catch (Exception e) {
            logger.error("send sdk request to node failed, error info: ", e.getMessage());
        }
    }

    private void onRegisterEventMsg(ChannelHandlerContext ctx, Message message) {
        try {
            String content = new String(message.getData()).trim();
            EventMsgContent resp = objectMapper.readValue(content, EventMsgContent.class);
            filterId2SDKConnect.put(resp.getFilterID(), ChannelVersionNegotiation.getPeerHost(ctx));
            logger.info(
                    "onRegisterEventMsg, register {} to {}",
                    resp.getFilterID(),
                    ChannelVersionNegotiation.getPeerHost(ctx));
        } catch (JsonProcessingException e) {
            logger.error("onRegisterEventMsg failed, error info: " + e.getMessage());
        }
    }

    private void onUnregisterEventMsg(ChannelHandlerContext ctx, Message message) {
        try {
            String content = new String(message.getData()).trim();
            EventMsgContent resp = objectMapper.readValue(content, EventMsgContent.class);
            filterId2SDKConnect.remove(resp.getFilterID());
            logger.info(
                    "onUnregisterEventMsg, unregister {} to {}",
                    resp.getFilterID(),
                    ChannelVersionNegotiation.getPeerHost(ctx));
        } catch (JsonProcessingException e) {
            logger.error("onUnregisterEventMsg failed, error info: " + e.getMessage());
        }
    }

    private List<String> getAvailableNodePeers() {
        List<String> peerList = new ArrayList<>();
        nodeConnectionManager
                .getAvailableConnections()
                .forEach(
                        (peer, ctx) -> {
                            peerList.add(peer);
                        });
        return peerList;
    }

    private void checkConnectionWithNodesToStartPeriodTask() {
        try {
            int sleepTime = 0;
            while (true) {
                if (getAvailableNodePeers().size() > 0 || sleepTime > connectSeconds * 1000) {
                    break;
                } else {
                    Thread.sleep(connectSleepPerMillis);
                    sleepTime += connectSleepPerMillis;
                }
            }

            List<String> peers = getAvailableNodePeers();
            String connectionInfoStr = "";
            for (String peer : peers) {
                connectionInfoStr += peer + ", ";
            }

            String baseMessage =
                    "node list from proxy client: "
                            + connectionInfoStr
                            + "java version: "
                            + System.getProperty("java.version")
                            + " ,java vendor: "
                            + System.getProperty("java.vm.vendor");

            if (getAvailableNodePeers().size() == 0) {
                String errorMessage = "failed to connect to " + baseMessage;
                logger.error(errorMessage);
                throw new Exception(errorMessage);
            }

            logger.info("connect to " + baseMessage);

            startPeriodTaskToNodes();
        } catch (InterruptedException e) {
            logger.warn("thread interrupted, error info: " + e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("start proxy connect failed, error info: " + e.getMessage());
        }
    }

    private void startPeriodTaskToNodes() {
        /** periodically send heartbeat message to all connected node, default period : 2s */
        scheduledExecutorService.scheduleAtFixedRate(
                () -> broadcastHeartbeatToNodes(), 0, heartBeatDelay, TimeUnit.MILLISECONDS);
    }

    private void broadcastHeartbeatToNodes() {
        try {
            nodeConnectionManager
                    .getAvailableConnections()
                    .forEach(
                            (peer, ctx) -> {
                                sendHeartbeatMessageToNode(ctx);
                                logger.trace("sendHeartbeatMessage to {} successfully ", peer);
                            });
        } catch (Exception e) {
            logger.error("broadcastHeartbeatToNodes failed, error info: " + e.getMessage());
        }
    }

    private void sendHeartbeatMessageToNode(ChannelHandlerContext ctx) {
        String seq = ChannelUtils.newSeq();
        Message message = new Message();

        try {
            message.setSeq(seq);
            message.setResult(0);
            message.setType(Short.valueOf((short) MsgType.CLIENT_HEARTBEAT.getType()));
            HeartBeatParser heartBeatParser =
                    new HeartBeatParser(ChannelVersionNegotiation.getProtocolVersion(ctx));
            message.setData(heartBeatParser.encode("0"));
            logger.trace(
                    "sendHeartbeatMessageToNode, seq: {}, messageType: {}",
                    message.getSeq(),
                    message.getType());
        } catch (JsonProcessingException e) {
            logger.error(
                    "sendHeartbeatMessageToNode failed for decode the message exception, error info: "
                            + e.getMessage());
            return;
        }

        ResponseCallback callback =
                new ResponseCallback() {
                    @Override
                    public void onResponse(Response response) {
                        Boolean disconnect = true;
                        try {
                            if (response.getErrorCode() != 0) {
                                logger.error(
                                        "channel protocol heartbeat request failed, code: {}, message: {}",
                                        response.getErrorCode(),
                                        response.getErrorMessage());
                                throw new ChannelPrococolExceiption(
                                        "channel protocol heartbeat request failed, code: "
                                                + response.getErrorCode()
                                                + ", message: "
                                                + response.getErrorMessage());
                            }

                            NodeHeartbeat nodeHeartbeat =
                                    objectMapper.readValue(
                                            response.getContent(), NodeHeartbeat.class);
                            int heartBeat = nodeHeartbeat.getHeartBeat();
                            logger.trace("heartbeat packet from node, heartbeat is {} ", heartBeat);
                            disconnect = false;
                        } catch (Exception e) {
                            logger.error(
                                    "channel protocol heartbeat failed, error info: "
                                            + e.getMessage());
                        }
                        if (disconnect) {
                            String host = ChannelVersionNegotiation.getPeerHost(ctx);
                            nodeConnectionManager.removeConnection(host);
                        } else {
                            startHeardBeat = true;
                        }
                    }
                };

        ctx.writeAndFlush(message);
        seq2CallbackNodes.put(seq, callback);
    }

    private void queryNodeVersion(ChannelHandlerContext ctx) {
        ChannelRequest request = new ChannelRequest("getClientVersion", Arrays.asList());
        String seq = ChannelUtils.newSeq();
        Message message = new Message();
        try {
            byte[] payload = objectMapper.writeValueAsBytes(request);
            message.setSeq(seq);
            message.setResult(0);
            message.setType((short) MsgType.CHANNEL_RPC_REQUEST.getType());
            message.setData(payload);
            logger.trace(
                    "queryNodeVersion, seq: {}, method: {}, messageType: {}",
                    message.getSeq(),
                    request.getMethod(),
                    message.getType());
        } catch (JsonProcessingException e) {
            logger.error(
                    "queryNodeVersion failed for decode the message exception, error info: "
                            + e.getMessage());
        }

        ResponseCallback callback =
                new ResponseCallback() {
                    @Override
                    public void onResponse(Response response) {
                        Boolean disconnect = true;
                        try {
                            if (response.getErrorCode()
                                    == ChannelMessageError.MESSAGE_TIMEOUT.getError()) {
                                // The node version number is below 2.1.0 when request timeout
                                ChannelVersionNegotiation.setProtocolVersion(
                                        ctx,
                                        EnumChannelProtocolVersion.VERSION_1,
                                        "below-2.1.0-timeout");

                                logger.info(
                                        "query node version timeout, content: {}",
                                        response.getContent());
                                return;
                            } else if (response.getErrorCode() != 0) {
                                logger.error(
                                        "node version response, code: {}, message: {}",
                                        response.getErrorCode(),
                                        response.getErrorMessage());

                                throw new ChannelPrococolExceiption(
                                        "query node version failed, code: "
                                                + response.getErrorCode()
                                                + ", message: "
                                                + response.getErrorMessage());
                            }

                            NodeVersion nodeVersion =
                                    objectMapper.readValue(
                                            response.getContent(), NodeVersion.class);
                            logger.info(
                                    "node: {}, content: {}",
                                    nodeVersion.getResult(),
                                    response.getContent());

                            if (EnumNodeVersion.channelProtocolHandleShakeSupport(
                                    nodeVersion.getResult().getSupportedVersion())) {
                                // node support channel protocol handshake, start it
                                logger.info("support channel handshake node");
                                queryChannelProtocolVersion(ctx);
                            } else { // default channel protocol
                                logger.info("not support channel handshake set default");
                                ChannelVersionNegotiation.setProtocolVersion(
                                        ctx,
                                        EnumChannelProtocolVersion.VERSION_1,
                                        nodeVersion.getResult().getSupportedVersion());
                            }
                            disconnect = false;
                        } catch (Exception e) {
                            logger.error(
                                    "query node version failed, error info: " + e.getMessage());
                        }

                        if (disconnect) {
                            ctx.disconnect();
                            ctx.close();
                        }
                    }
                };

        ctx.writeAndFlush(message);
        seq2CallbackNodes.put(seq, callback);
    }

    private void queryChannelProtocolVersion(ChannelHandlerContext ctx)
            throws ChannelPrococolExceiption {
        final String host = ChannelVersionNegotiation.getPeerHost(ctx);
        String seq = ChannelUtils.newSeq();
        Message message = new Message();

        try {
            ChannelHandshake channelHandshake = new ChannelHandshake();
            byte[] payload = objectMapper.writeValueAsBytes(channelHandshake);
            message.setSeq(seq);
            message.setResult(0);
            message.setType(Short.valueOf((short) MsgType.CLIENT_HANDSHAKE.getType()));
            message.setData(payload);
            logger.trace(
                    "queryChannelProtocolVersion, seq: {}, data: {}, messageType: {}",
                    message.getSeq(),
                    channelHandshake.toString(),
                    message.getType());
        } catch (JsonProcessingException e) {
            logger.error(
                    "queryChannelProtocolVersion failed for decode the message exception, errorMessage: {}",
                    e.getMessage());
            throw new ChannelPrococolExceiption(e.getMessage());
        }

        ResponseCallback callback =
                new ResponseCallback() {
                    @Override
                    public void onResponse(Response response) {
                        Boolean disconnect = true;
                        try {
                            if (response.getErrorCode() != 0) {
                                logger.error(
                                        "channel protocol handshake request failed, code: {}, message: {}",
                                        response.getErrorCode(),
                                        response.getErrorMessage());
                                throw new ChannelPrococolExceiption(
                                        "channel protocol handshake request failed, code: "
                                                + response.getErrorCode()
                                                + ", message: "
                                                + response.getErrorMessage());
                            }

                            ChannelProtocol channelProtocol =
                                    objectMapper.readValue(
                                            response.getContent(), ChannelProtocol.class);
                            EnumChannelProtocolVersion enumChannelProtocolVersion =
                                    EnumChannelProtocolVersion.toEnum(
                                            channelProtocol.getProtocol());
                            channelProtocol.setEnumProtocol(enumChannelProtocolVersion);
                            logger.info(
                                    "channel protocol handshake success, set socket channel protocol, host: {}, channel protocol: {}",
                                    host,
                                    channelProtocol);

                            ctx.channel()
                                    .attr(
                                            AttributeKey.valueOf(
                                                    EnumSocketChannelAttributeKey
                                                            .CHANNEL_PROTOCOL_KEY.getKey()))
                                    .set(channelProtocol);
                            disconnect = false;
                        } catch (Exception e) {
                            logger.error(
                                    "channel protocol handshake failed, error info: "
                                            + e.getMessage());
                        }
                        if (disconnect) {
                            ctx.disconnect();
                            ctx.close();
                        }
                    }
                };

        ctx.writeAndFlush(message);
        seq2CallbackNodes.put(seq, callback);
    }
}
