package org.fisco.bcos.proxy;

import lombok.extern.slf4j.Slf4j;
import org.fisco.bcos.sdk.BcosSDK;
import org.fisco.bcos.sdk.BcosSDKException;
import org.fisco.bcos.sdk.model.ConstantConfig;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ApplicationContextHelper implements ApplicationContextAware {

    private static ConfigurableApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        try {
            context = (ConfigurableApplicationContext) applicationContext;

            final String configFile =
                    ApplicationContextHelper.class
                            .getClassLoader()
                            .getResource("asClient/" + ConstantConfig.CONFIG_FILE_NAME)
                            .getPath();
            BcosSDK.build(configFile);
        } catch (BcosSDKException e) {
            log.debug("init bcos sdk failed, error info: {}", e.getMessage());
            context.close();
        }
    }
}
