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

import java.util.LinkedHashMap;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.fisco.bcos.proxy.base.code.ConstantCode;
import org.fisco.bcos.proxy.base.entity.BaseResponse;
import org.fisco.bcos.proxy.base.exception.BcosNodeProxyException;
import org.fisco.bcos.proxy.rpc.entity.JsonRpcRequest;
import org.fisco.bcos.proxy.rpc.entity.JsonRpcResponse;
import org.fisco.bcos.sdk.client.Client;
import org.fisco.bcos.sdk.client.protocol.request.Transaction;
import org.fisco.bcos.sdk.client.protocol.response.*;
import org.fisco.bcos.sdk.model.NodeVersion;
import org.fisco.bcos.sdk.model.TransactionReceipt;
import org.springframework.stereotype.Service;

/** services for rpc. */
@Log4j2
@Service
public class RPCService {

    /** getClientVersion. */
    public BaseResponse getClientVersion(JsonRpcRequest info, Client client)
            throws BcosNodeProxyException {
        log.info("start getClientVersion");
        BaseResponse baseResponse = new BaseResponse(ConstantCode.SUCCESS);
        NodeVersion nodeVersion = client.getNodeVersion();
        JsonRpcResponse jsonRpcResponse = new JsonRpcResponse();
        jsonRpcResponse.setId(info.getId());
        jsonRpcResponse.setJsonrpc(info.getJsonrpc());
        jsonRpcResponse.setResult(nodeVersion.getResult());
        baseResponse.setData(jsonRpcResponse);
        return baseResponse;
    }

    /** getBlockNumber. */
    public BaseResponse getBlockNumber(JsonRpcRequest info, Client client)
            throws BcosNodeProxyException {
        log.info("start getBlockNumber");
        BaseResponse baseResponse = new BaseResponse(ConstantCode.SUCCESS);
        BlockNumber blockNumber = client.getBlockNumber();
        JsonRpcResponse jsonRpcResponse = new JsonRpcResponse();
        jsonRpcResponse.setId(info.getId());
        jsonRpcResponse.setJsonrpc(info.getJsonrpc());
        jsonRpcResponse.setResult(blockNumber.getResult());
        baseResponse.setData(jsonRpcResponse);
        return baseResponse;
    }

    /** sendRawTransaction. */
    public BaseResponse sendRawTransaction(JsonRpcRequest info, Client client)
            throws BcosNodeProxyException {
        log.info("start sendRawTransaction");
        List<Object> params = info.getParams();
        if (params.size() != 2) {
            log.error("the size of `JsonRpcRequest.params` should be 2");
            throw new BcosNodeProxyException(ConstantCode.PARAM_EXCEPTION);
        }
        BaseResponse baseResponse = new BaseResponse(ConstantCode.SUCCESS);
        String data = (String) params.get(1);
        TransactionReceipt receipt = client.sendRawTransactionAndGetReceipt(data);
        JsonRpcResponse jsonRpcResponse = new JsonRpcResponse();
        jsonRpcResponse.setId(info.getId());
        jsonRpcResponse.setJsonrpc(info.getJsonrpc());
        jsonRpcResponse.setResult(receipt);
        baseResponse.setData(jsonRpcResponse);
        return baseResponse;
    }

    /** call. */
    public BaseResponse call(JsonRpcRequest info, Client client) throws BcosNodeProxyException {
        log.info("start call");
        List<Object> params = info.getParams();
        if (params.size() != 2) {
            log.error("the size of `JsonRpcRequest.params` should be 2");
            throw new BcosNodeProxyException(ConstantCode.PARAM_EXCEPTION);
        }
        BaseResponse baseResponse = new BaseResponse(ConstantCode.SUCCESS);
        LinkedHashMap<String, String> data = (LinkedHashMap<String, String>) params.get(1);
        Transaction transaction =
                new Transaction(data.get("from"), data.get("to"), data.get("data"));
        Call callFuncRet = client.call(transaction);
        JsonRpcResponse jsonRpcResponse = new JsonRpcResponse();
        jsonRpcResponse.setId(info.getId());
        jsonRpcResponse.setJsonrpc(info.getJsonrpc());
        jsonRpcResponse.setResult(callFuncRet.getResult());
        baseResponse.setData(jsonRpcResponse);
        return baseResponse;
    }

    /** getTransactionByHash. */
    public BaseResponse getTransactionByHash(JsonRpcRequest info, Client client)
            throws BcosNodeProxyException {
        log.info("start getTransactionByHash");
        List<Object> params = info.getParams();
        if (params.size() != 2) {
            log.error("the size of `JsonRpcRequest.params` should be 2");
            throw new BcosNodeProxyException(ConstantCode.PARAM_EXCEPTION);
        }
        BaseResponse baseResponse = new BaseResponse(ConstantCode.SUCCESS);
        String data = (String) params.get(1);
        BcosTransaction transaction = client.getTransactionByHash(data);
        JsonRpcResponse jsonRpcResponse = new JsonRpcResponse();
        jsonRpcResponse.setId(info.getId());
        jsonRpcResponse.setJsonrpc(info.getJsonrpc());
        jsonRpcResponse.setResult(transaction.getResult());
        baseResponse.setData(jsonRpcResponse);
        return baseResponse;
    }

    /** getTransactionReceipt. */
    public BaseResponse getTransactionReceipt(JsonRpcRequest info, Client client)
            throws BcosNodeProxyException {
        log.info("start getTransactionReceipt");
        List<Object> params = info.getParams();
        if (params.size() != 2) {
            log.error("the size of `JsonRpcRequest.params` should be 2");
            throw new BcosNodeProxyException(ConstantCode.PARAM_EXCEPTION);
        }
        BaseResponse baseResponse = new BaseResponse(ConstantCode.SUCCESS);
        String data = (String) params.get(1);
        BcosTransactionReceipt receipt = client.getTransactionReceipt(data);
        JsonRpcResponse jsonRpcResponse = new JsonRpcResponse();
        jsonRpcResponse.setId(info.getId());
        jsonRpcResponse.setJsonrpc(info.getJsonrpc());
        jsonRpcResponse.setResult(receipt.getResult());
        baseResponse.setData(jsonRpcResponse);
        return baseResponse;
    }

    /** getSealerList. */
    public BaseResponse getSealerList(JsonRpcRequest info, Client client)
            throws BcosNodeProxyException {
        log.info("start getSealerList");
        BaseResponse baseResponse = new BaseResponse(ConstantCode.SUCCESS);
        SealerList sealerList = client.getSealerList();
        JsonRpcResponse jsonRpcResponse = new JsonRpcResponse();
        jsonRpcResponse.setId(info.getId());
        jsonRpcResponse.setJsonrpc(info.getJsonrpc());
        jsonRpcResponse.setResult(sealerList.getResult());
        baseResponse.setData(jsonRpcResponse);
        return baseResponse;
    }

    /** getObserverList. */
    public BaseResponse getObserverList(JsonRpcRequest info, Client client)
            throws BcosNodeProxyException {
        log.info("start getObserverList");
        BaseResponse baseResponse = new BaseResponse(ConstantCode.SUCCESS);
        ObserverList observerList = client.getObserverList();
        JsonRpcResponse jsonRpcResponse = new JsonRpcResponse();
        jsonRpcResponse.setId(info.getId());
        jsonRpcResponse.setJsonrpc(info.getJsonrpc());
        jsonRpcResponse.setResult(observerList.getResult());
        baseResponse.setData(jsonRpcResponse);
        return baseResponse;
    }

    /** getNodeIDList. */
    public BaseResponse getNodeIDList(JsonRpcRequest info, Client client)
            throws BcosNodeProxyException {
        log.info("start getNodeIDList");
        BaseResponse baseResponse = new BaseResponse(ConstantCode.SUCCESS);
        NodeIDList nodeIDList = client.getNodeIDList();
        JsonRpcResponse jsonRpcResponse = new JsonRpcResponse();
        jsonRpcResponse.setId(info.getId());
        jsonRpcResponse.setJsonrpc(info.getJsonrpc());
        jsonRpcResponse.setResult(nodeIDList.getResult());
        baseResponse.setData(jsonRpcResponse);
        return baseResponse;
    }

    /** getSystemConfigByKey. */
    public BaseResponse getSystemConfigByKey(JsonRpcRequest info, Client client)
            throws BcosNodeProxyException {
        log.info("start getSystemConfigByKey");
        List<Object> params = info.getParams();
        if (params.size() != 2) {
            log.error("the size of `JsonRpcRequest.params` should be 2");
            throw new BcosNodeProxyException(ConstantCode.PARAM_EXCEPTION);
        }
        BaseResponse baseResponse = new BaseResponse(ConstantCode.SUCCESS);
        String data = (String) params.get(1);
        SystemConfig systemConfig = client.getSystemConfigByKey(data);
        JsonRpcResponse jsonRpcResponse = new JsonRpcResponse();
        jsonRpcResponse.setId(info.getId());
        jsonRpcResponse.setJsonrpc(info.getJsonrpc());
        jsonRpcResponse.setResult(systemConfig.getResult());
        baseResponse.setData(jsonRpcResponse);
        return baseResponse;
    }
}
