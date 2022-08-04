/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shenyu.plugin.brpc.cache;

import org.apache.shenyu.common.concurrent.ShenyuThreadFactory;
import org.apache.shenyu.common.dto.MetaData;
import org.apache.shenyu.common.enums.RpcTypeEnum;
import org.apache.shenyu.plugin.brpc.proxy.BrpcInvokePrxList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test case for {@link ApplicationConfigCache}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public final class ApplicationConfigCacheTest {

    private ApplicationConfigCache applicationConfigCacheUnderTest;

    @BeforeEach
    public void setUp() {
        applicationConfigCacheUnderTest = ApplicationConfigCache.getInstance();
    }

    @Test
    public void testGet() {
        final String rpcExt = "{\"methodInfo\":[{\"methodName\":\"method1\",\"params\":"
                + "[{\"left\":\"int\",\"right\":\"param1\"},{\"left\":\"java.lang.Integer\","
                + "\"right\":\"param2\"}],\"returnType\":\"java.lang.String\"}]}";

        final MetaData metaData = new MetaData("id", "127.0.0.1:8080", "contextPath",
                "path5", RpcTypeEnum.BRPC.getName(), "serviceName5", "method1",
                "parameterTypes", rpcExt, false);
        assertThrows(NullPointerException.class, () -> {
            applicationConfigCacheUnderTest.initProxy(metaData);
            final BrpcInvokePrxList result = applicationConfigCacheUnderTest.get("path5");
            assertNotNull(result);
            assertEquals("method1", result.getMethod().getName());
            assertEquals(2, result.getParamTypes().length);
            assertEquals(2, result.getParamNames().length);
        });
    }

    @Test
    public void testInitPrx() {
        final MetaData metaData = new MetaData("id", "127.0.0.1:8080", "contextPath",
                "path6", RpcTypeEnum.BRPC.getName(), "serviceName6", "method1",
                "parameterTypes", "{\"methodInfo\":[{\"methodName\":\"method1\",\"params\":[{\"left\":\"int\",\"right\":\"param1\"},"
                + "{\"left\":\"java.lang.Integer\",\"right\":\"param2\"}],\"returnType\":\"java.lang.String\"}]}", false);
        assertThrows(NullPointerException.class, () -> {
            applicationConfigCacheUnderTest.initProxy(metaData);
            final BrpcInvokePrxList result = applicationConfigCacheUnderTest.get("path6");
            assertEquals("method1", result.getMethod().getName());
        });
    }

    @Test
    public void testConcurrentInitPrx() {
        final String rpcExt1 = "{\"methodInfo\":[{\"methodName\":\"method1\",\"params\":"
                + "[{\"left\":\"int\",\"right\":\"param1\"},{\"left\":\"java.lang.Integer\","
                + "\"right\":\"param2\"}],\"returnType\":\"java.lang.String\"}]}";
        final String rpcExt2 = "{\"methodInfo\":[{\"methodName\":\"method2\",\"params\":"
                + "[{\"left\":\"int\",\"right\":\"param1\"},{\"left\":\"java.lang.Integer\","
                + "\"right\":\"param2\"}],\"returnType\":\"java.lang.String\"}]}";
        final String rpcExt3 = "{\"methodInfo\":[{\"methodName\":\"method3\",\"params\":"
                + "[{\"left\":\"int\",\"right\":\"param1\"},{\"left\":\"java.lang.Integer\","
                + "\"right\":\"param2\"}],\"returnType\":\"java.lang.String\"}]}";
        final String rpcExt4 = "{\"methodInfo\":[{\"methodName\":\"method4\",\"params\":"
                + "[{\"left\":\"int\",\"right\":\"param1\"},{\"left\":\"java.lang.Integer\","
                + "\"right\":\"param2\"}],\"returnType\":\"java.lang.String\"}]}";

        final MetaData metaData1 = new MetaData("id", "127.0.0.1:8080", "contextPath",
                "path1", RpcTypeEnum.BRPC.getName(), "serviceName1", "method1",
                "parameterTypes", rpcExt1, false);
        final MetaData metaData2 = new MetaData("id", "127.0.0.1:8080", "contextPath",
                "path2", RpcTypeEnum.BRPC.getName(), "serviceName2", "method2",
                "parameterTypes", rpcExt2, false);
        final MetaData metaData3 = new MetaData("id", "127.0.0.1:8080", "contextPath",
                "path3", RpcTypeEnum.BRPC.getName(), "serviceName3", "method3",
                "parameterTypes", rpcExt3, false);
        final MetaData metaData4 = new MetaData("id", "127.0.0.1:8080", "contextPath",
                "path4", RpcTypeEnum.BRPC.getName(), "serviceName4", "method4",
                "parameterTypes", rpcExt4, false);
        final List<MetaData> metaDataList = new ArrayList<>();
        metaDataList.add(metaData1);
        metaDataList.add(metaData2);
        metaDataList.add(metaData3);
        metaDataList.add(metaData4);

        assertThrows(NullPointerException.class, () -> {
            ExecutorService executorService = Executors.newFixedThreadPool(4,
                    ShenyuThreadFactory.create("ApplicationConfigCache-brpc-initPrx", false));
            CountDownLatch countDownLatch = new CountDownLatch(4);
            metaDataList.forEach(metaData -> executorService.execute(() -> {
                applicationConfigCacheUnderTest.initProxy(metaData);
                countDownLatch.countDown();
            }));
            countDownLatch.await();
            assertEquals("method1", applicationConfigCacheUnderTest.get("path1").getMethod().getName());
            assertEquals("method2", applicationConfigCacheUnderTest.get("path2").getMethod().getName());
            assertEquals("method3", applicationConfigCacheUnderTest.get("path3").getMethod().getName());
            assertEquals("method4", applicationConfigCacheUnderTest.get("path4").getMethod().getName());
        });
    }

    @Test
    public void testGetClassMethodKey() {
        assertEquals("className_methodName", ApplicationConfigCache.getClassMethodKey("className", "methodName"));
    }

    @Test
    public void testGetInstance() {
        assertNotNull(this.applicationConfigCacheUnderTest);
    }
}
