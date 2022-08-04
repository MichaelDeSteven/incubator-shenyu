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

package org.apache.shenyu.examples.brpc.service.impl;

import com.baidu.brpc.spring.annotation.RpcExporter;
import org.apache.shenyu.client.brpc.common.annotation.ShenyuBrpcClient;
import org.apache.shenyu.examples.brpc.api.entity.BrpcComplexTypeBean;
import org.apache.shenyu.examples.brpc.api.entity.BrpcSimpleTypeBean;
import org.apache.shenyu.examples.brpc.api.service.BrpcClientMultiParamService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Brpc multi parameter service.
 */
//@Service
public class BrpcClientMultiParamServiceImpl implements BrpcClientMultiParamService {
    
    @Override
    @ShenyuBrpcClient(path = "/findByIdsAndName", desc = "findByIdsAndName")
    public BrpcSimpleTypeBean findByIdsAndName(final List<Integer> ids, final String name) {
        return new BrpcSimpleTypeBean(ids.toString(), "hello world shenyu brpc param findByIdsAndName ：" + name);
    }
    
    @Override
    @ShenyuBrpcClient(path = "/findByArrayIdsAndName", desc = "findByIdsAndName")
    public BrpcSimpleTypeBean findByArrayIdsAndName(final Integer[] ids, final String name) {
        return new BrpcSimpleTypeBean(Arrays.toString(ids), "hello world shenyu brpc param findByArrayIdsAndName ：" + name);
    }
    
    @Override
    @ShenyuBrpcClient(path = "/findByStringArray", desc = "findByStringArray")
    public BrpcSimpleTypeBean findByStringArray(final String[] ids) {
        return new BrpcSimpleTypeBean(Arrays.toString(ids), "hello world shenyu brpc param findByStringArray");
    }
    
    @Override
    @ShenyuBrpcClient(path = "/findByListId", desc = "findByListId")
    public BrpcSimpleTypeBean findByListId(final List<String> ids) {
        return new BrpcSimpleTypeBean(ids.toString(), "hello world shenyu brpc param findByListId");
    }
    
    @Override
    @ShenyuBrpcClient(path = "/batchSave", desc = "batchSave")
    public BrpcSimpleTypeBean batchSave(final List<BrpcSimpleTypeBean> brpcTestList) {
        final String id = brpcTestList.stream().map(BrpcSimpleTypeBean::getId).collect(Collectors.joining("-"));
        final String name = "hello world shenyu brpc param batchSave :"
                + brpcTestList.stream()
                .map(BrpcSimpleTypeBean::getName)
                .collect(Collectors.joining("-"));
        return new BrpcSimpleTypeBean(id, name);
    }
    
    @Override
    @ShenyuBrpcClient(path = "/batchSaveNameAndId", desc = "batchSaveNameAndId")
    public BrpcSimpleTypeBean batchSaveNameAndId(final List<BrpcSimpleTypeBean> brpcTestList, final String id, final String name) {
        final String newName = "hello world shenyu brpc param batchSaveAndNameAndId :" + name + ":"
                + brpcTestList.stream()
                .map(BrpcSimpleTypeBean::getName)
                .collect(Collectors.joining("-"));
        return new BrpcSimpleTypeBean(id, newName);
    }
    
    @Override
    @ShenyuBrpcClient(path = "/saveComplexBean", desc = "saveComplexBean")
    public BrpcSimpleTypeBean saveComplexBean(final BrpcComplexTypeBean brpcComplexTypeBean) {
        final String id = brpcComplexTypeBean.getIdLists().toString();
        final String typeName = "hello world shenyu brpc param saveComplexBean :" + brpcComplexTypeBean.getBrpcSimpleTypeBean().getName();
        return new BrpcSimpleTypeBean(id, typeName);
    }
    
    @Override
    @ShenyuBrpcClient(path = "/saveComplexBeanAndName", desc = "saveComplexBeanAndName")
    public BrpcSimpleTypeBean saveComplexBeanAndName(final BrpcComplexTypeBean brpcComplexTypeBean, final String name) {
        final String id = brpcComplexTypeBean.getIdLists().toString();
        final String typeName = "hello world shenyu brpc param saveComplexBeanAndName :" + brpcComplexTypeBean.getBrpcSimpleTypeBean().getName() + "-" + name;
        return new BrpcSimpleTypeBean(id, typeName);
    }
    
    @Override
    @ShenyuBrpcClient(path = "/saveTwoList", desc = "saveTwoList")
    public BrpcSimpleTypeBean saveTwoList(final List<BrpcComplexTypeBean> brpcComplexTypeBeanList, final Map<String, BrpcSimpleTypeBean> brpcSimpleTypeBeanMap) {
        BrpcSimpleTypeBean simpleTypeBean = new BrpcSimpleTypeBean();
        if (!CollectionUtils.isEmpty(brpcComplexTypeBeanList) && !CollectionUtils.isEmpty(brpcSimpleTypeBeanMap)) {
            final BrpcComplexTypeBean firstBean = brpcComplexTypeBeanList.get(0);
            final Optional<BrpcSimpleTypeBean> firstTypeOptional = brpcSimpleTypeBeanMap.values().stream().findFirst();
            simpleTypeBean.setId(firstBean.getIdLists().toString());
            simpleTypeBean.setName("hello world shenyu brpc param saveTwoList :" + firstBean.getBrpcSimpleTypeBean().getName()
                    + "-" + firstTypeOptional.map(BrpcSimpleTypeBean::getName));
        }
        return simpleTypeBean;
    }
}
