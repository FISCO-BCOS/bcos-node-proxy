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

package org.fisco.bcos.node.model;

import java.util.Map;
import org.fisco.bcos.sdk.config.exceptions.ConfigException;
import org.fisco.bcos.sdk.config.model.ConfigProperty;
import org.fisco.bcos.sdk.utils.Host;

public class ServerNetworkConfig {
    private String listenPort;

    public ServerNetworkConfig(ConfigProperty configProperty) throws ConfigException {
        Map<String, Object> networkProperty = configProperty.getNetwork();
        if (networkProperty != null) {
            listenPort = (String) networkProperty.get("listenPort");
            checkListerPort(listenPort);
        }
    }

    private void checkListerPort(String listerPort) throws ConfigException {
        if (listerPort == null) {
            throw new ConfigException(
                    "Invalid configuration, listenPort not configured, please config peers in yaml server config file.");
        }
        if (!Host.validPort(listerPort)) {
            throw new ConfigException(
                    " Invalid configuration, tcp port should from 1 to 65535, value: "
                            + listerPort);
        }
    }

    public String getListenPort() {
        return listenPort;
    }
}
