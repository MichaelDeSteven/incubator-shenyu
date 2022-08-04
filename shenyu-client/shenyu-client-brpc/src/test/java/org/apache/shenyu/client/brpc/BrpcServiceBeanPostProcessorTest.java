/*
 *   Licensed to the Apache Software Foundation (ASF) under one or more
 *   contributor license agreements.  See the NOTICE file distributed with
 *   this work for additional information regarding copyright ownership.
 *   The ASF licenses this file to You under the Apache License, Version 2.0
 *   (the "License"); you may not use this file except in compliance with
 *   the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.apache.shenyu.client.brpc;

import org.apache.shenyu.client.core.register.ShenyuClientRegisterRepositoryFactory;
import org.apache.shenyu.client.brpc.common.annotation.ShenyuBrpcClient;
import org.apache.shenyu.client.brpc.common.annotation.ShenyuBrpcService;
import org.apache.shenyu.register.client.http.utils.RegisterUtils;
import org.apache.shenyu.register.common.config.PropertiesConfig;
import org.apache.shenyu.register.common.config.ShenyuRegisterCenterConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Properties;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

/**
 * Test case for {@link BrpcServiceBeanPostProcessor}.
 */
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.Alphanumeric.class)
public final class BrpcServiceBeanPostProcessorTest {
    private static BrpcServiceBeanPostProcessor brpcServiceBeanPostProcessor;

    @BeforeAll
    public static void init() {
        Properties properties = new Properties();
        properties.setProperty("contextPath", "/brpc");
        properties.setProperty("port", "8080");
        properties.setProperty("host", "localhost");
        properties.setProperty("username", "admin");
        properties.setProperty("password", "123456");

        PropertiesConfig config = new PropertiesConfig();
        config.setProps(properties);

        ShenyuRegisterCenterConfig mockRegisterCenter = new ShenyuRegisterCenterConfig();
        mockRegisterCenter.setServerLists("http://localhost:58080");
        mockRegisterCenter.setRegisterType("http");
        mockRegisterCenter.setProps(properties);
        MockedStatic<RegisterUtils> registerUtilsMockedStatic = mockStatic(RegisterUtils.class);
        registerUtilsMockedStatic.when(() -> RegisterUtils.doLogin(any(), any(), any())).thenReturn(Optional.of("token"));
        brpcServiceBeanPostProcessor = new BrpcServiceBeanPostProcessor(config, ShenyuClientRegisterRepositoryFactory.newInstance(mockRegisterCenter));
        registerUtilsMockedStatic.close();
    }

    @Test
    public void testPostProcessAfterInitialization() {
        BrpcDemoService serviceFactoryBean = new BrpcDemoService();
        brpcServiceBeanPostProcessor
                .postProcessAfterInitialization(serviceFactoryBean, "ShenyuBrpcTest");
    }

    @Test
    public void testPostProcessNormalBean() {
        brpcServiceBeanPostProcessor
                .postProcessAfterInitialization(new Object(), "normalBean");
    }

    @ShenyuBrpcService(serviceName = "testObj")
    static class BrpcDemoService {
        @ShenyuBrpcClient(path = "hello")
        public String test(final String hello) {
            return hello + "";
        }
    }
}
