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

import java.util.Optional;
import lombok.extern.log4j.Log4j2;
import org.fisco.bcos.proxy.base.code.ConstantCode;
import org.fisco.bcos.proxy.base.code.RetCode;
import org.fisco.bcos.proxy.base.entity.BaseResponse;
import org.fisco.bcos.proxy.base.tools.JacksonUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/** catch an handler exception. */
@ControllerAdvice
@Log4j2
public class ExceptionsHandler {

    /** catch：BcosNodeProxyException. */
    @ResponseBody
    @ExceptionHandler(value = BcosNodeProxyException.class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public BaseResponse myExceptionHandler(BcosNodeProxyException nodeMgrException) {
        log.warn("catch business exception", nodeMgrException);
        RetCode retCode =
                Optional.ofNullable(nodeMgrException)
                        .map(BcosNodeProxyException::getRetCode)
                        .orElse(ConstantCode.SYSTEM_EXCEPTION);

        BaseResponse bre = new BaseResponse(retCode);
        log.warn("business exception return:{}", JacksonUtils.objToString(bre));
        return bre;
    }

    /** catch:paramException */
    @ResponseBody
    @ExceptionHandler(value = ParamException.class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public BaseResponse paramExceptionHandler(ParamException paramException) {
        log.warn("catch param exception", paramException);
        RetCode retCode =
                Optional.ofNullable(paramException)
                        .map(ParamException::getRetCode)
                        .orElse(ConstantCode.SYSTEM_EXCEPTION);

        BaseResponse bre = new BaseResponse(retCode);
        log.warn("param exception return:{}", JacksonUtils.objToString(bre));
        return bre;
    }

    /** catch：RuntimeException. */
    @ResponseBody
    @ExceptionHandler(value = RuntimeException.class)
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    public BaseResponse exceptionHandler(RuntimeException exc) {
        log.warn("catch RuntimeException", exc);
        // 默认系统异常
        RetCode retCode = ConstantCode.SYSTEM_EXCEPTION;

        BaseResponse bre = new BaseResponse(retCode);
        log.warn("system RuntimeException return:{}", JacksonUtils.objToString(bre));
        return bre;
    }
}
