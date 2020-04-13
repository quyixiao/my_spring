/*
 * $Id: KeyedDefinitionsFactoryTilesContainerFactory.java 798944 2009-07-29 15:20:27Z apetrelli $
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.tiles2.factory;

import java.util.HashMap;
import java.util.Map;

import org.apache.tiles2.TilesApplicationContext;
import org.apache.tiles2.TilesContainer;
import org.apache.tiles2.awareness.TilesApplicationContextAware;
import org.apache.tiles2.context.TilesRequestContextFactory;
import org.apache.tiles2.impl.KeyedDefinitionsFactoryTilesContainer;
import org.apache.tiles2.impl.mgmt.CachingKeyedDefinitionsFactoryTilesContainer;
import org.apache.tiles2.mgmt.MutableTilesContainer;
import org.apache.tiles2.reflect.ClassUtil;
import org.apache.tiles2.renderer.RendererFactory;
import org.apache.tiles2.definition.DefinitionsFactory;
import org.apache.tiles2.evaluator.AttributeEvaluator;
import org.apache.tiles2.impl.BasicTilesContainer;
import org.apache.tiles2.startup.AbstractTilesInitializer;

/**
 * Factory that creates instances of container that will extend the
 * {@link KeyedDefinitionsFactoryTilesContainer} class.
 *
 * @version $Rev: 798944 $ $Date: 2009-07-29 17:20:27 +0200 (mer, 29 lug 2009) $
 * @deprecated This class can be replaced by registering different
 * {@link TilesContainer} with different keys, by overriding
 * {@link AbstractTilesInitializer}
 * <code>getContainerKey</code> method.
 */
public class KeyedDefinitionsFactoryTilesContainerFactory extends
        TilesContainerFactory {

    /**
     * The name of the initialization parameter that will contain a
     * comma-separated list of keys to use.
     */
    public static final String CONTAINER_KEYS_INIT_PARAM =
        "org.apache.tiles.factory.KeyedDefinitionsFactoryTilesContainerFactory.KEYS";

    /**
     * Init parameter name that contains the class name for the key extractor.
     */
    public static final String KEY_EXTRACTOR_CLASS_INIT_PARAM =
        "org.apache.tiles.impl.KeyedDefinitionsFactoryTilesContainer.KeyExtractor";

    /**
     * The application context.
     *
     * @since 2.1.0
     */
    protected TilesApplicationContext applicationContext;

    /** {@inheritDoc} */
    @Override
    public MutableTilesContainer createMutableTilesContainer(
            TilesApplicationContext context) {
        CachingKeyedDefinitionsFactoryTilesContainer container =
            new CachingKeyedDefinitionsFactoryTilesContainer();
        initializeContainer(context, container);
        return container;
    }

    /** {@inheritDoc} */
    @Override
    public TilesContainer createTilesContainer(TilesApplicationContext context) {
        KeyedDefinitionsFactoryTilesContainer container =
            new KeyedDefinitionsFactoryTilesContainer();
        initializeContainer(context, container);
        return container;
    }

    /**
     * Creates a definitions factory.
     * @param context The context object to use.
     * @return The newly created definitions factory.
     * @throws org.apache.tiles2.factory.TilesContainerFactoryException If something goes wrong.
     * @deprecated Use
     * {@link #createDefinitionsFactory(TilesApplicationContext)}.
     */
    @Deprecated
    public org.apache.tiles2.definition.DefinitionsFactory createDefinitionsFactory(Object context) {
        if (context instanceof TilesApplicationContext) {
            createDefinitionsFactory((TilesApplicationContext) context);
        }

        throw new UnsupportedOperationException("Class "
                + context.getClass().getName()
                + " not recognized a TilesApplicationContext");
    }

    /**
     * Creates a definitions factory.
     * @param context The Tiles application context object to use.
     * @return The newly created definitions factory.
     * @throws TilesContainerFactoryException If something goes wrong.
     */
    public org.apache.tiles2.definition.DefinitionsFactory createDefinitionsFactory(
            TilesApplicationContext context) {
        org.apache.tiles2.definition.DefinitionsFactory retValue;
        Map<String, String> config = new HashMap<String, String>(defaultConfiguration);
        config.putAll(context.getInitParams());
        retValue = (org.apache.tiles2.definition.DefinitionsFactory) createFactory(config,
                    DEFINITIONS_FACTORY_INIT_PARAM);
        if (retValue instanceof TilesApplicationContextAware) {
            ((TilesApplicationContextAware) retValue)
                    .setApplicationContext(applicationContext);
        }

        return retValue;
    }

    /** {@inheritDoc} */
    @Override
    protected void storeContainerDependencies(TilesApplicationContext context,
            Map<String, String> initParameters,
            Map<String, String> configuration, org.apache.tiles2.impl.BasicTilesContainer container) {
        super.storeContainerDependencies(context, initParameters, configuration, container);

        String keyExtractorClassName = configuration.get(
                KEY_EXTRACTOR_CLASS_INIT_PARAM);
        if (keyExtractorClassName != null
                && container instanceof KeyedDefinitionsFactoryTilesContainer) {
            ((KeyedDefinitionsFactoryTilesContainer) container).setKeyExtractor(
                    (KeyedDefinitionsFactoryTilesContainer.KeyExtractor) ClassUtil.instantiate(keyExtractorClassName));
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void postCreationOperations(TilesRequestContextFactory contextFactory,
                                          TilesApplicationContext tilesContext,
                                          RendererFactory rendererFactory, AttributeEvaluator evaluator,
                                          Map<String, String> initParameters,
                                          Map<String, String> configuration, BasicTilesContainer container) {
        super.postCreationOperations(contextFactory, tilesContext,
                rendererFactory, evaluator, initParameters, configuration,
                container);
        this.applicationContext = tilesContext;
        String keysString = initParameters.get(CONTAINER_KEYS_INIT_PARAM);
        if (keysString != null
                && container instanceof KeyedDefinitionsFactoryTilesContainer) {
            String[] keys = keysString.split(",");
            Map<String, String> initParams = new HashMap<String, String>(initParameters);
            for (int i = 0; i < keys.length; i++) {
                String param = initParameters.get(
                        KeyedDefinitionsFactoryTilesContainer.DEFINITIONS_CONFIG_PREFIX + keys[i]);
                if (param != null) {
                    initParams.put(org.apache.tiles2.definition.DefinitionsFactory.DEFINITIONS_CONFIG,
                            param);
                } else {
                    initParams.remove(org.apache.tiles2.definition.DefinitionsFactory.DEFINITIONS_CONFIG);
                }

                org.apache.tiles2.definition.DefinitionsFactory defsFactory =
                    (DefinitionsFactory) createFactory(configuration,
                            DEFINITIONS_FACTORY_INIT_PARAM);
                if (defsFactory instanceof TilesApplicationContextAware) {
                    ((TilesApplicationContextAware) defsFactory).setApplicationContext(tilesContext);
                }

                defsFactory.init(initParams);
                ((KeyedDefinitionsFactoryTilesContainer) container)
                        .setDefinitionsFactory(keys[i], defsFactory);
            }
        }
    }
}
