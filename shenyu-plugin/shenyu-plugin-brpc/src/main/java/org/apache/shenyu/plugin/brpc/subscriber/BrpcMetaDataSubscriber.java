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

package org.apache.shenyu.plugin.brpc.subscriber;

import com.google.common.collect.Maps;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.shenyu.common.dto.MetaData;
import org.apache.shenyu.common.enums.RpcTypeEnum;
import org.apache.shenyu.plugin.brpc.cache.ApplicationConfigCache;
import org.apache.shenyu.plugin.brpc.proxy.BrpcInvokePrx;
import org.apache.shenyu.sync.data.api.MetaDataSubscriber;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * The brpc metadata subscribe.
 */
public class BrpcMetaDataSubscriber implements MetaDataSubscriber {

    private static final ConcurrentMap<String, MetaData> META_DATA = Maps.newConcurrentMap();

    @Override
    public void onSubscribe(final MetaData metaData) {
        metaData.updateContextPath();
        if (Objects.equals(RpcTypeEnum.BRPC.getName(), metaData.getRpcType())) {
            MetaData metaExist = META_DATA.get(metaData.getPath());
            List<BrpcInvokePrx> proxyList = ApplicationConfigCache.getInstance()
                    .get(metaData.getPath()).getBrpcInvokePrxList();
            boolean exist = proxyList.stream().anyMatch(proxy -> proxy.getHost().equals(metaData.getAppName()));
            if (!exist) {
                ApplicationConfigCache.getInstance().initProxy(metaData);
            }
            if (Objects.isNull(metaExist)) {
                META_DATA.put(metaData.getPath(), metaData);
            }
        }
    }

    @Override
    public void unSubscribe(final MetaData metaData) {
        metaData.updateContextPath();
        if (Objects.equals(RpcTypeEnum.BRPC.getName(), metaData.getRpcType())) {
            List<BrpcInvokePrx> proxyList = ApplicationConfigCache.getInstance()
                    .get(metaData.getPath()).getBrpcInvokePrxList();
            List<BrpcInvokePrx> removePrxList = proxyList.stream()
                    .filter(proxy -> proxy.getHost().equals(metaData.getAppName()))
                    .collect(Collectors.toList());
            proxyList.removeAll(removePrxList);
            if (CollectionUtils.isEmpty(proxyList)) {
                META_DATA.remove(metaData.getPath());
            }
        }
    }
}
