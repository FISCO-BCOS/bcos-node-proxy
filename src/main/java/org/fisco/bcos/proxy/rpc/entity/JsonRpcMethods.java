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
package org.fisco.bcos.proxy.rpc.entity;

public class JsonRpcMethods {

    /** define the method name for all jsonRPC interfaces */
    public static final String GET_NODE_VERSION = "getClientVersion";

    public static final String GET_BLOCK_NUMBER = "getBlockNumber";
    public static final String SEND_RAWTRANSACTION = "sendRawTransaction";
    public static final String CALL = "call";
    public static final String GET_TRANSACTION_BY_HASH = "getTransactionByHash";
    public static final String GET_TRANSACTIONRECEIPT = "getTransactionReceipt";
}
