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

package org.apache.shenyu.examples.brpc.service;

import com.baidu.brpc.server.RpcServer;
import com.baidu.brpc.server.RpcServerOptions;
import org.apache.shenyu.examples.brpc.service.impl.BrpcDemoServiceImpl;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Brpc service starter.
 */
@SpringBootApplication
public class TestBrpcApplication {

    /**
     * Main Entrance.
     *
     * @param args startup arguments
     */
    public static void main(final String[] args) {
        SpringApplication.run(TestBrpcApplication.class, args);
        int port = 8002;
        if (args.length == 1) {
            port = Integer.valueOf(args[0]);
        }
        RpcServerOptions options = new RpcServerOptions();
        options.setNamingServiceUrl("zookeeper://127.0.0.1:2181/brpc");
        final RpcServer rpcServer = new RpcServer(port, options);
        rpcServer.registerService(new BrpcDemoServiceImpl());
        rpcServer.start();

        // make server keep running
        synchronized (TestBrpcApplication.class) {
            try {
                TestBrpcApplication.class.wait();
            } catch (Throwable e) {
            }
        }
    }

}
