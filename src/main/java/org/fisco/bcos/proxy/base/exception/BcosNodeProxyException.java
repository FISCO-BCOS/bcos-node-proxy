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
package org.fisco.bcos.proxy.base.exception;

import org.fisco.bcos.proxy.base.code.RetCode;
import org.fisco.bcos.proxy.base.tools.JacksonUtils;

/** business exception. */
public class BcosNodeProxyException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private RetCode retCode;

    /** init by RetCode. */
    public BcosNodeProxyException(RetCode retCode) {
        super(retCode.getMessage());
        this.retCode = retCode;
    }

    /** init by RetCode and Throwable. */
    public BcosNodeProxyException(RetCode retCode, Throwable cause) {
        super(retCode.getMessage(), cause);
        retCode.setMessage(JacksonUtils.objToString(cause.getMessage()));
        this.retCode = retCode;
    }

    /** init by code and msg. */
    public BcosNodeProxyException(int code, String msg) {
        super(msg);
        this.retCode = new RetCode(code, msg);
    }

    /** init by code/msg and Throwable. */
    public BcosNodeProxyException(int code, String msg, Throwable cause) {
        super(msg, cause);
        this.retCode = new RetCode(code, msg);
    }

    public RetCode getRetCode() {
        return retCode;
    }
}
