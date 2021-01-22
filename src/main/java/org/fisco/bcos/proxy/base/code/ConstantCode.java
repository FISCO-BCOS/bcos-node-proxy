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
package org.fisco.bcos.proxy.base.code;

/**
 * A-BB-CCC <br>
 * A:error level. <br>
 * 1:system exception <br>
 * 2:business exception <br>
 * 3:param exception <br>
 * B:project number <br>
 * BcosNodeProxy:00 <br>
 * C: error code <br>
 * 0XX:rpc exception <br>
 * 9XX:other exception <br>
 */
public class ConstantCode {

    /** Return success. */
    public static final RetCode SUCCESS = RetCode.mark(0, "success");

    /** System exception. */
    public static final RetCode SYSTEM_EXCEPTION = RetCode.mark(100000, "system exception");

    /** Business exception. */
    public static final RetCode INVALID_RPC_METHOD = RetCode.mark(200100, "invalid rpc method");

    public static final RetCode UNSUPPORTED_RPC_METHOD =
            RetCode.mark(200101, "unsupported rpc method");
    public static final RetCode INVALID_GROUPID = RetCode.mark(200102, "invalid groupId");
    public static final RetCode INSIDE_JSON_PARSER_ERROR =
            RetCode.mark(200102, "inside json parser error");

    /* param exception */
    public static final RetCode PARAM_EXCEPTION = RetCode.mark(300000, "param exception");
}
