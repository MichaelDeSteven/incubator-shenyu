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

package org.apache.shenyu.examples.brpc.api.service;

import org.apache.shenyu.examples.brpc.api.entity.BrpcComplexTypeBean;
import org.apache.shenyu.examples.brpc.api.entity.BrpcSimpleTypeBean;

import java.util.List;
import java.util.Map;

/**
 * Brpc multi parameter service.
 */
public interface BrpcMultiParamService {

    /**
     * Find by ids and name.
     * body: {"ids":["1232","456"],"name":"hello world"}
     *
     * @param ids  the ids
     * @param name the name
     * @return BrpcSimpleTypeBean
     */
    BrpcSimpleTypeBean findByIdsAndName(List<Integer> ids, String name);

    /**
     * Find by array ids and name.
     * body :{"ids":[123,4561],"name":"hello world"}
     *
     * @param ids  the ids
     * @param name the name
     * @return BrpcSimpleTypeBean
     */
    BrpcSimpleTypeBean findByArrayIdsAndName(Integer[] ids, String name);

    /**
     * Find by string array.
     * body :{"ids":["1232","456"]}
     *
     * @param ids the ids
     * @return BrpcSimpleTypeBean
     */
    BrpcSimpleTypeBean findByStringArray(String[] ids);

    /**
     * Find by list id.
     * body :{"ids":["1232","456"]}
     *
     * @param ids the ids
     * @return BrpcSimpleTypeBean
     */
    BrpcSimpleTypeBean findByListId(List<String> ids);

    /**
     * Batch save BrpcSimpleTypeBean.
     * body :{"brpcTestList":[{"id":"123","name":"zhuangsongtao"},{"id":"456","name":"myth"}]}
     *
     * @param brpcTestList the brpc test list
     * @return BrpcSimpleTypeBean
     */
    BrpcSimpleTypeBean batchSave(List<BrpcSimpleTypeBean> brpcTestList);

    /**
     * Batch save name and id.
     * body: {"brpcTestList":[{"id":"123","name":"zhuangsongtao"},{"id":"456","name":"myth"}],"id":"789","name":"ttt"}
     *
     * @param brpcTestList the brpc test list.
     * @param id            the id
     * @param name          the name
     * @return BrpcSimpleTypeBean
     */
    BrpcSimpleTypeBean batchSaveNameAndId(List<BrpcSimpleTypeBean> brpcTestList, String id, String name);


    /**
     * Save brpc complex type bean.
     * body : {"brpcSimpleTypeBean":{"id":"123","name":"zhuangsongtao"},"idLists":["456","789"],"idMaps":{"id2":"2","id1":"1"}}
     *
     * @param brpcComplexTypeBean the brpc complex type bean.
     * @return BrpcSimpleTypeBean
     */
    BrpcSimpleTypeBean saveComplexBean(BrpcComplexTypeBean brpcComplexTypeBean);


    /**
     * Save complex bean and name test.
     * body : {"brpcComplexTypeBean":{"brpcSimpleTypeBean":{"id":"123","name":"zhuangsongtao"},"idLists":["456","789"],"idMaps":{"id2":"2","id1":"1"}},"name":"zhuangsongtao"}
     *
     * @param brpcComplexTypeBean the brpc complex type bean.
     * @param name            the name
     * @return BrpcSimpleTypeBean
     */
    BrpcSimpleTypeBean saveComplexBeanAndName(BrpcComplexTypeBean brpcComplexTypeBean, String name);

    /**
     * Save complex bean and name test.
     * body : {"brpcComplexTypeBean":[{"brpcSimpleTypeBean":{"id":"123","name":"zhuangsongtao"},"idLists":["456","789"],"idMaps":{"id2":"2","id1":"1"}}],
     * "brpcSimpleTypeBeanMap":[{"id":"123","name":"zhuangsongtao"}]}
     *
     * @param brpcComplexTypeBeanList the brpc complex type bean.
     * @param brpcSimpleTypeBeanMap            the name
     * @return BrpcSimpleTypeBean
     */
    BrpcSimpleTypeBean saveTwoList(List<BrpcComplexTypeBean> brpcComplexTypeBeanList, Map<String, BrpcSimpleTypeBean> brpcSimpleTypeBeanMap);

}
