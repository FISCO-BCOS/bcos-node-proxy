/**
 * Copyright 2014-2020 the original author or authors.
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fisco.bcos.proxy.rpc;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import javax.validation.Valid;
import lombok.extern.log4j.Log4j2;
import org.fisco.bcos.proxy.base.code.ConstantCode;
import org.fisco.bcos.proxy.base.controller.BaseController;
import org.fisco.bcos.proxy.base.entity.BaseResponse;
import org.fisco.bcos.proxy.base.exception.BcosNodeProxyException;
import org.fisco.bcos.proxy.base.tools.JacksonUtils;
import org.fisco.bcos.proxy.rpc.entity.JsonRpcMethods;
import org.fisco.bcos.proxy.rpc.entity.JsonRpcRequest;
import org.fisco.bcos.sdk.BcosSDK;
import org.fisco.bcos.sdk.BcosSDKException;
import org.fisco.bcos.sdk.client.Client;
import org.fisco.bcos.sdk.model.ConstantConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Log4j2
@RestController
@RequestMapping(value = "rpc")
public class RPCController extends BaseController {

    @Autowired private RPCService rpcService;

    private Boolean initedSDK = false;
    private BcosSDK bcosSDK;
    private ConcurrentHashMap<Integer, Client> groupToClient = new ConcurrentHashMap<>();

    @PostMapping(value = "/v1")
    public BaseResponse rpcRequest(@RequestBody @Valid JsonRpcRequest info, BindingResult result)
            throws BcosNodeProxyException {
        checkBindResult(result);
        BaseResponse baseResponse;
        Instant startTime = Instant.now();
        log.info(
                "start rpcRequest. startTime:{} rpc request info:{}",
                startTime.toEpochMilli(),
                JacksonUtils.objToString(info));

        if (initedSDK == false) {
            initBcosSDK();
            initedSDK = true;
        }

        List<Object> params = info.getParams();
        if (params.size() < 1) {
            log.error("the size of `JsonRpcRequest.params` should be larger than 1");
            throw new BcosNodeProxyException(ConstantCode.PARAM_EXCEPTION);
        }
        Integer groupId = (Integer) params.get(0);
        Client client = getClientByGroupId(groupId);
        String method = info.getMethod();

        if (method.equals(JsonRpcMethods.GET_NODE_VERSION)) {
            baseResponse = rpcService.getClientVersion(info, client);
        } else if (method.equals(JsonRpcMethods.GET_BLOCK_NUMBER)) {
            baseResponse = rpcService.getBlockNumber(info, client);
        } else if (method.equals(JsonRpcMethods.SEND_RAWTRANSACTION)) {
            baseResponse = rpcService.sendRawTransaction(info, client);
        } else if (method.equals(JsonRpcMethods.CALL)) {
            baseResponse = rpcService.call(info, client);
        } else if (method.equals(JsonRpcMethods.GET_TRANSACTION_BY_HASH)) {
            baseResponse = rpcService.getTransactionByHash(info, client);
        } else if (method.equals(JsonRpcMethods.GET_TRANSACTIONRECEIPT)) {
            baseResponse = rpcService.getTransactionReceipt(info, client);
        } else {
            log.error("invalid method");
            throw new BcosNodeProxyException(ConstantCode.INVALID_RPC_METHOD);
        }

        log.info(
                "end rpcRequest. useTime:{} result:{}",
                Duration.between(startTime, Instant.now()).toMillis(),
                JacksonUtils.objToString(baseResponse));
        return baseResponse;
    }

    private void initBcosSDK() {
        log.info("init bcos sdk");
        final String configFile =
                RPCController.class
                        .getClassLoader()
                        .getResource("asClient/" + ConstantConfig.CONFIG_FILE_NAME)
                        .getPath();
        bcosSDK = BcosSDK.build(configFile);
    }

    private Client getClientByGroupId(Integer groupId) {
        if (!groupToClient.containsKey(groupId)) {
            try {
                Client client = bcosSDK.getClient(groupId);
                groupToClient.put(groupId, client);
            } catch (BcosSDKException e) {
                log.error("invalid groupId, id: " + groupId);
                throw new BcosNodeProxyException(ConstantCode.INVALID_GROUPID);
            }
        }
        return groupToClient.get(groupId);
    }
}
