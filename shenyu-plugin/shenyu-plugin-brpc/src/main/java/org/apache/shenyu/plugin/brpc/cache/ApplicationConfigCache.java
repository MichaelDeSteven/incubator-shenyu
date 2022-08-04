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

import com.baidu.brpc.client.BrpcProxy;
import com.baidu.brpc.client.RpcClient;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.shenyu.common.constant.Constants;
import org.apache.shenyu.common.dto.MetaData;
import org.apache.shenyu.common.dto.SelectorData;
import org.apache.shenyu.common.dto.convert.selector.CommonUpstream;
import org.apache.shenyu.common.exception.ShenyuException;
import org.apache.shenyu.common.utils.GsonUtils;
import org.apache.shenyu.plugin.brpc.exception.ShenyuBrpcPluginException;
import org.apache.shenyu.plugin.brpc.proxy.BrpcInvokePrx;
import org.apache.shenyu.plugin.brpc.proxy.BrpcInvokePrxList;
import org.apache.shenyu.plugin.brpc.util.ProxyInfoUtil;
import org.apache.shenyu.plugin.brpc.util.ReturnValueResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Brpc config cache.
 */
public final class ApplicationConfigCache {

    private static final Logger LOG = LoggerFactory.getLogger(ApplicationConfigCache.class);

    private static final ReentrantLock LOCK = new ReentrantLock();

    private final ConcurrentHashMap<String, List<MetaData>> ctxPathCache = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Class<?>> proxyClassCache = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, BrpcParamInfo> proxyParamCache = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, List<CommonUpstream>> refreshUpstreamCache = new ConcurrentHashMap<>();

    private final LoadingCache<String, BrpcInvokePrxList> cache = CacheBuilder.newBuilder()
            .maximumSize(Constants.CACHE_MAX_COUNT)
            .build(new CacheLoader<String, BrpcInvokePrxList>() {
                @NonNull
                @Override
                public BrpcInvokePrxList load(@NonNull final String key) {
                    return new BrpcInvokePrxList();
                }
            });

    private ApplicationConfigCache() {
    }


    /**
     * Get reference config.
     *
     * @param path path
     * @return the reference config
     */
    public BrpcInvokePrxList get(final String path) {
        try {
            return cache.get(path);
        } catch (ExecutionException e) {
            throw new ShenyuBrpcPluginException(e.getCause());
        }
    }

    /**
     * Init proxy.<br>
     * Try to load the meta information defined by meta data to the local cache.<br>
     * eg: class definition, all method definition params,context path.<br>
     *
     * @param metaData metaData
     */
    public void initProxy(final MetaData metaData) {
        while (true) {
            Class<?> prxClass = proxyClassCache.get(metaData.getPath());
            try {
                if (Objects.isNull(prxClass)) {
                    // Spin's Attempt to Load
                    tryLockedLoadMetaData(metaData);
                } else {
//                    if (Objects.nonNull(metaData.getContextPath()) && Objects.nonNull(refreshUpstreamCache.get(metaData.getContextPath()))) {
//                        refreshBrpcProxyList(metaData, refreshUpstreamCache.get(metaData.getContextPath()));
//                    }
                    refreshBrpcProxyList(metaData, refreshUpstreamCache.get(metaData.getContextPath()));
                    break;
                }
            } catch (Exception e) {
                LOG.error("ShenyuBrpcPluginInitializeException: init brpc ref ex:{}", e.getMessage());
                break;
            }
        }
    }

    /**
     * Try to load once, if it fails, it will give up.<br>
     * add class cache to {@link #proxyClassCache}.<br>
     * add method params cache to {@link #proxyParamCache}.<br>
     * add paths cache to {@link #ctxPathCache}.<br>
     *
     * @param metaData metaData
     * @throws ClassNotFoundException meta data class definition not found
     * @see ReentrantLock
     */
    private void tryLockedLoadMetaData(final MetaData metaData) throws ClassNotFoundException {
        assert LOCK != null;
        if (LOCK.tryLock()) {
            try {
                if (StringUtils.isEmpty(metaData.getRpcExt())) {
                    throw new ShenyuBrpcPluginException("ShenyuBrpcPluginInitializeException: can't init proxy with empty ext string");
                }
                Class<?> prxClazz = buildClassDefinition(metaData);
                proxyClassCache.put(metaData.getPath(), prxClazz);
                List<MetaData> paths = ctxPathCache.getOrDefault(metaData.getContextPath(), new ArrayList<>());
                if (!IterableUtils.matchesAny(paths, p -> p.getPath().equals(metaData.getPath()))) {
                    paths.add(metaData);
                }
                ctxPathCache.put(metaData.getContextPath(), paths);
            } finally {
                LOCK.unlock();
            }
        }
    }

    /**
     * build target class definition.
     *
     * @param metaData metadata
     * @return class definition
     * @throws ClassNotFoundException meta data class definition not found
     */
    private Class<?> buildClassDefinition(final MetaData metaData) throws ClassNotFoundException {
        String clazzName = ProxyInfoUtil.getProxyName(metaData);
        clazzName = "BrpcDemoService";
        DynamicType.Builder<?> classDefinition = new ByteBuddy().makeInterface().name(clazzName);
        BrpcParamExtInfo brpcParamExtInfo = GsonUtils.getInstance().fromJson(metaData.getRpcExt(), BrpcParamExtInfo.class);
        for (MethodInfo methodInfo : brpcParamExtInfo.getMethodInfo()) {
            DynamicType.Builder.MethodDefinition.ParameterDefinition<?> definition =
                    classDefinition.defineMethod(ProxyInfoUtil.getMethodName(methodInfo.methodName),
                            ReturnValueResolver.getCallBackType(ProxyInfoUtil.getParamClass(methodInfo.getReturnType())),
                            Visibility.PUBLIC);
            if (CollectionUtils.isNotEmpty(methodInfo.getParams())) {
                Class<?>[] paramTypes = new Class[methodInfo.getParams().size()];
                String[] paramNames = new String[methodInfo.getParams().size()];
                for (int i = 0; i < methodInfo.getParams().size(); i++) {
                    Pair<String, String> pair = methodInfo.getParams().get(i);
                    paramTypes[i] = ProxyInfoUtil.getParamClass(pair.getKey());
                    paramNames[i] = pair.getValue();
                    definition = definition.withParameter(paramTypes[i], paramNames[i]);
                    proxyParamCache.put(getClassMethodKey(clazzName, methodInfo.getMethodName()), new BrpcParamInfo(paramTypes, paramNames));
                }
                classDefinition = definition.withoutCode();
            }
        }
        return classDefinition.make()
                .load(ClassLoader.getSystemClassLoader())
                .getLoaded();
    }

    /**
     * initProxyClass.
     *
     * @param selectorData selectorData
     */
    public void initProxyClass(final SelectorData selectorData) {
        try {
            final List<CommonUpstream> upstreamList = GsonUtils.getInstance().fromList(selectorData.getHandle(), CommonUpstream.class);
            if (CollectionUtils.isEmpty(upstreamList)) {
                invalidate(selectorData.getName());
                return;
            }
            refreshUpstreamCache.put(selectorData.getName(), upstreamList);
            List<MetaData> metaDataList = ctxPathCache.getOrDefault(selectorData.getName(), new ArrayList<>());
            for (MetaData metaData : metaDataList) {
                refreshBrpcProxyList(metaData, upstreamList);
            }
        } catch (ExecutionException | NoSuchMethodException e) {
            throw new ShenyuException(e.getCause());
        }
    }

    /**
     * invalidate.
     *
     * @param contextPath context path
     */
    public void invalidate(final String contextPath) {
        List<MetaData> metaDataList = ctxPathCache.getOrDefault(contextPath, new ArrayList<>());
        metaDataList.forEach(metaData -> cache.invalidate(metaData.getPath()));
    }

    /**
     * refresh metaData path upstream url.
     *
     * @param metaData     metaData
     * @param upstreamList upstream list
     */
    private void refreshBrpcProxyList(final MetaData metaData, final List<CommonUpstream> upstreamList) throws NoSuchMethodException, ExecutionException {
        Class<?> proxyClass = proxyClassCache.get(metaData.getPath());
        if (Objects.isNull(proxyClass)) {
            return;
        }
        BrpcInvokePrxList brpcInvokePrxList = cache.get(metaData.getPath());
        brpcInvokePrxList.getBrpcInvokePrxList().clear();
        if (Objects.isNull(brpcInvokePrxList.getMethod())) {
            BrpcParamInfo brpcParamInfo = proxyParamCache.get(getClassMethodKey(proxyClass.getName(), metaData.getMethodName()));
            Object proxy = BrpcProxy.getProxy(new RpcClient("zookeeper://127.0.0.1:2181/brpc"), proxyClass);
            Method method = proxy.getClass().getDeclaredMethod(
                    ProxyInfoUtil.getMethodName(metaData.getMethodName()), brpcParamInfo.getParamTypes());
            brpcInvokePrxList.setMethod(method);
            brpcInvokePrxList.setParamTypes(brpcParamInfo.getParamTypes());
            brpcInvokePrxList.setParamNames(brpcParamInfo.getParamNames());
        }
//        brpcInvokePrxList.getBrpcInvokePrxList().addAll(upstreamList.stream().map(upstream -> {
//            Object proxy = BrpcProxy.getProxy(new RpcClient(upstream.getUpstreamUrl()), proxyClass);
//            return new BrpcInvokePrx(proxy, upstream.getUpstreamUrl());
//        }).collect(Collectors.toList()));
        brpcInvokePrxList.getBrpcInvokePrxList()
                .add(new BrpcInvokePrx(BrpcProxy.getProxy(new RpcClient("zookeeper://127.0.0.1:2181"), proxyClass),
                        "127.0.0.1:2181")
        );
    }

    /**
     * Get param info key.
     *
     * @param className  className
     * @param methodName methodName
     * @return the key
     */
    public static String getClassMethodKey(final String className, final String methodName) {
        return String.join("_", className, methodName);
    }

    /**
     * Gets instance.
     *
     * @return the instance
     */
    public static ApplicationConfigCache getInstance() {
        return ApplicationConfigCacheInstance.INSTANCE;
    }

    /**
     * The type Application config cache instance.
     */
    static final class ApplicationConfigCacheInstance {

        /**
         * The Instance.
         */
        static final ApplicationConfigCache INSTANCE = new ApplicationConfigCache();

        private ApplicationConfigCacheInstance() {

        }

    }

    /**
     * The type Brpc param ext info.
     */
    static class MethodInfo {

        private String methodName;

        private List<Pair<String, String>> params;

        private String returnType;

        public String getMethodName() {
            return methodName;
        }

        public void setMethodName(final String methodName) {
            this.methodName = methodName;
        }

        public List<Pair<String, String>> getParams() {
            return params;
        }

        public void setParams(final List<Pair<String, String>> params) {
            this.params = params;
        }

        public String getReturnType() {
            return returnType;
        }

        public void setReturnType(final String returnType) {
            this.returnType = returnType;
        }
    }

    /**
     * The type Brpc param ext info.
     */
    static class BrpcParamExtInfo {

        private List<MethodInfo> methodInfo;

        public List<MethodInfo> getMethodInfo() {
            return methodInfo;
        }

        public void setMethodInfo(final List<MethodInfo> methodInfo) {
            this.methodInfo = methodInfo;
        }
    }

    /**
     * The type Brpc param ext info.
     */
    static class BrpcParamInfo {

        private Class<?>[] paramTypes;

        private String[] paramNames;

        BrpcParamInfo(final Class<?>[] paramTypes, final String[] paramNames) {
            this.paramTypes = paramTypes;
            this.paramNames = paramNames;
        }

        public Class<?>[] getParamTypes() {
            return paramTypes;
        }

        public void setParamTypes(final Class<?>[] paramTypes) {
            this.paramTypes = paramTypes;
        }

        public String[] getParamNames() {
            return paramNames;
        }

        public void setParamNames(final String[] paramNames) {
            this.paramNames = paramNames;
        }
    }
}
