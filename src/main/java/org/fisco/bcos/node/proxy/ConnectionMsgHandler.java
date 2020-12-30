/*
 * Copyright 2014-2020  [fisco-dev]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.fisco.bcos.node.proxy;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import org.fisco.bcos.sdk.channel.ChannelVersionNegotiation;
import org.fisco.bcos.sdk.model.Message;
import org.fisco.bcos.sdk.network.MsgHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Sharable
public class ConnectionMsgHandler implements MsgHandler {
    private static Logger logger = LoggerFactory.getLogger(ConnectionMsgHandler.class);
    private Server server;
    private Boolean fromNodes = false;

    public void setFromNodes(Boolean fromNodes) {
        this.fromNodes = fromNodes;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    @Override
    public void onConnect(ChannelHandlerContext ctx) {
        String host = ChannelVersionNegotiation.getPeerHost(ctx);
        if (fromNodes) {
            logger.debug("connect node in proxy, host : {}", host);
            server.onNodeConnect(ctx);
        } else {
            logger.debug("connect sdk in proxy, host : {}", host);
        }
    }

    @Override
    public void onMessage(ChannelHandlerContext ctx, Message msg) {
        logger.debug(
                "onMessage from {}, host: {}, seq: {}, msgType: {}",
                fromNodes ? "node" : "sdk",
                ChannelVersionNegotiation.getPeerHost(ctx),
                msg.getSeq(),
                (int) msg.getType());
        if (fromNodes) {
            server.onNodeMessage(ctx, msg);
        } else {
            server.onSDKMessage(ctx, msg);
        }
    }

    @Override
    public void onDisconnect(ChannelHandlerContext ctx) {
        String host = ChannelVersionNegotiation.getPeerHost(ctx);
        if (fromNodes) {
            logger.debug("disconnect node in proxy, host : {}", host);
        } else {
            logger.debug("disconnect sdk in proxy, host : {}", host);
            server.onSDKDisconnect(ctx);
        }
    }
}
