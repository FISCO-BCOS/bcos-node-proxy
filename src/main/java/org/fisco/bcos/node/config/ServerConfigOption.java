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

package org.fisco.bcos.node.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.fisco.bcos.node.model.ServerNetworkConfig;
import org.fisco.bcos.sdk.config.exceptions.ConfigException;
import org.fisco.bcos.sdk.config.model.ConfigProperty;
import org.fisco.bcos.sdk.config.model.CryptoMaterialConfig;
import org.fisco.bcos.sdk.config.model.ThreadPoolConfig;
import org.fisco.bcos.sdk.model.CryptoType;

/** ConfigOption is the java object of the config file. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServerConfigOption {
    private CryptoMaterialConfig cryptoMaterialConfig;
    private ServerNetworkConfig networkConfig;
    private ThreadPoolConfig threadPoolConfig;
    private ConfigProperty configProperty;

    public ServerConfigOption(ConfigProperty configProperty) throws ConfigException {
        this(configProperty, CryptoType.ECDSA_TYPE);
    }

    public ServerConfigOption(ConfigProperty configProperty, int cryptoType)
            throws ConfigException {
        // load cryptoMaterialConfig
        cryptoMaterialConfig = new CryptoMaterialConfig(configProperty, cryptoType);
        // load networkConfig
        networkConfig = new ServerNetworkConfig(configProperty);
        // load threadPoolConfig
        threadPoolConfig = new ThreadPoolConfig(configProperty);
        // init configProperty
        this.configProperty = configProperty;
    }

    public void reloadConfig(int cryptoType) throws ConfigException {
        cryptoMaterialConfig = new CryptoMaterialConfig(configProperty, cryptoType);
    }

    public CryptoMaterialConfig getCryptoMaterialConfig() {
        return cryptoMaterialConfig;
    }

    public void setCryptoMaterialConfig(CryptoMaterialConfig cryptoMaterialConfig) {
        this.cryptoMaterialConfig = cryptoMaterialConfig;
    }

    public void setNetworkConfig(ServerNetworkConfig networkConfig) {
        this.networkConfig = networkConfig;
    }

    public ServerNetworkConfig getNetworkConfig() {
        return networkConfig;
    }

    public ThreadPoolConfig getThreadPoolConfig() {
        return threadPoolConfig;
    }

    public void setThreadPoolConfig(ThreadPoolConfig threadPoolConfig) {
        this.threadPoolConfig = threadPoolConfig;
    }
}
