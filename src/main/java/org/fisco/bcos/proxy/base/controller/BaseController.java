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
package org.fisco.bcos.proxy.base.controller;

import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.fisco.bcos.proxy.base.code.ConstantCode;
import org.fisco.bcos.proxy.base.exception.ParamException;
import org.fisco.bcos.proxy.base.tools.JacksonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;

public class BaseController {

    @Autowired protected HttpServletRequest request;

    /** check param valid result. */
    protected void checkBindResult(BindingResult result) {
        if (result.hasErrors()) {
            String errFieldStr =
                    result.getAllErrors()
                            .stream()
                            .map(
                                    obj ->
                                            JacksonUtils.stringToJsonNode(
                                                    JacksonUtils.objToString(obj)))
                            .map(err -> err.findValue("field").asText())
                            .collect(Collectors.joining(","));
            StringUtils.removeEnd(errFieldStr, ",");
            String message = "these fields can not be empty:" + errFieldStr;
            throw new ParamException(ConstantCode.PARAM_EXCEPTION.getCode(), message);
        }
    }
}
