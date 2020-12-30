package org.fisco.bcos.node.proxy;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.Timeout;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import org.fisco.bcos.sdk.model.Message;
import org.fisco.bcos.sdk.network.ConnectionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ConnectionPair {
    private static Logger logger = LoggerFactory.getLogger(ConnectionPair.class);

    public ChannelHandlerContext sdkConnection;
    public ChannelHandlerContext nodeConnection;
    public Timeout timeout;

    private Message message;
    private ConnectionInfo nodeConnectionInfo;
    private List<ConnectionInfo> nodeConnectionInfos;
    private Map<String, ChannelHandlerContext> nodeChannelConnections;
    private Server server;

    public void setServer(Server server) {
        this.server = server;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public void setNodeChannelConnections(
            Map<String, ChannelHandlerContext> nodeChannelConnections) {
        this.nodeChannelConnections = nodeChannelConnections;
    }

    public void setNodeConnectionInfos(List<ConnectionInfo> nodeConnectionInfos) {
        this.nodeConnectionInfos = nodeConnectionInfos;
    }

    public void sendNodeMessage() {
        if (nodeConnectionInfos.size() > 0) {
            Random random = new SecureRandom();
            Integer index = random.nextInt(nodeConnectionInfos.size());
            String peerIpPort = nodeConnectionInfos.get(index).getEndPoint();
            ChannelHandlerContext ctx = nodeChannelConnections.get(peerIpPort);
            if (!Objects.isNull(ctx)) {
                ctx.writeAndFlush(message);
                logger.trace("send message to node {} success ", peerIpPort);
            } else {
                logger.warn("send message to node {} failed ", peerIpPort);
            }
        }
    }
}
