/*
 * $Id: CachingKeyedDefinitionsFactoryTilesContainer.java 797754 2009-07-25 11:42:03Z apetrelli $
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

package org.apache.tiles2.impl.mgmt;

import java.util.HashMap;
import java.util.Map;

import org.apache.tiles2.Definition;
import org.apache.tiles2.TilesApplicationContext;
import org.apache.tiles2.TilesContainer;
import org.apache.tiles2.access.TilesAccess;
import org.apache.tiles2.context.TilesRequestContext;
import org.apache.tiles2.impl.KeyedDefinitionsFactoryTilesContainer;
import org.apache.tiles2.mgmt.MutableTilesContainer;
import org.apache.tiles2.definition.DefinitionsFactory;

/**
 * Container that can be used to store multiple {@link org.apache.tiles2.definition.DefinitionsFactory}
 * instances mapped to different keys, with the addition of being "mutable",
 * i.e.  caches (in memory) the definitions registered to it.  If a definition
 * is not found in cache, it will revert back to it's definitions factory.
 *
 * @version $Rev: 797754 $ $Date: 2009-07-25 13:42:03 +0200 (sab, 25 lug 2009) $
 * @deprecated Register different containers using
 * {@link TilesAccess#setContainer(
 * TilesApplicationContext, TilesContainer, String)}
 * and using {@link CachingTilesContainer} as container implementation.
 */
public class CachingKeyedDefinitionsFactoryTilesContainer extends
        KeyedDefinitionsFactoryTilesContainer implements MutableTilesContainer {

    /**
     * The name prefix of the attribute that will contain custom definitions for
     * the current request.
     */
    private static final String DEFINITIONS_ATTRIBUTE_NAME_BASE =
        "org.apache.tiles.impl.mgmt.CachingKeyedDefinitionsFactoryTilesContainer.DEFINITIONS.";

    /**
     * The default definition manager, when no key is identified.
     */
    private DefinitionManager mgr = new DefinitionManager();

    /**
     * Maps a key to its definition manager.
     */
    private Map<String, DefinitionManager> key2definitionManager
            = new HashMap<String, DefinitionManager>();

    /** {@inheritDoc} */
    public void register(Definition definition, Object... requestItems) {
        TilesRequestContext requestContext = getRequestContextFactory()
                .createRequestContext(getApplicationContext(), requestItems);
        register(definition, requestContext);
    }

    /** {@inheritDoc} */
    @Override
    protected Definition getDefinition(String definition,
            TilesRequestContext context) {
        DefinitionManager mgr = getProperDefinitionManager(
                getDefinitionsFactoryKey(context));
        return mgr.getDefinition(definition, context);
    }

    /** {@inheritDoc} */
    @Override
    public org.apache.tiles2.definition.DefinitionsFactory getDefinitionsFactory() {
        return mgr.getFactory();
    }

    /** {@inheritDoc} */
    @Override
    public org.apache.tiles2.definition.DefinitionsFactory getDefinitionsFactory(String key) {
        DefinitionManager mgr = getProperDefinitionManager(key);
        return mgr.getFactory();
    }

    /** {@inheritDoc} */
    @Override
    public void setDefinitionsFactory(org.apache.tiles2.definition.DefinitionsFactory definitionsFactory) {
        super.setDefinitionsFactory(definitionsFactory);
        mgr.setFactory(definitionsFactory);
    }

    /** {@inheritDoc} */
    @Override
    @Deprecated
    public void setDefinitionsFactory(String key, DefinitionsFactory definitionsFactory,
            Map<String, String> initParameters) {
        if (key != null) {
            initializeDefinitionsFactory(definitionsFactory,
                    getResourceString(initParameters), initParameters);
        }
        DefinitionManager mgr = getOrCreateDefinitionManager(key);
        mgr.setFactory(definitionsFactory);
    }

    /**
     * Registers a custom definition.
     *
     * @param definition The definition to register.
     * @param request The request inside which the definition should be
     * registered.
     */
    protected void register(Definition definition, TilesRequestContext request) {
        DefinitionManager mgr = getProperDefinitionManager(
                getDefinitionsFactoryKey(request));
        mgr.addDefinition(definition, request);
    }

    /**
     * Returns a definition manager if found, otherwise it will create a new
     * one.
     *
     * @param key The key of the definition manager.
     * @return The needed definition manager.
     */
    protected DefinitionManager getOrCreateDefinitionManager(String key) {
        DefinitionManager mgr = key2definitionManager.get(key);
        if (mgr == null) {
            mgr = new DefinitionManager(DEFINITIONS_ATTRIBUTE_NAME_BASE + key);
            key2definitionManager.put(key, mgr);
        }

        return mgr;
    }

    /**
     * Returns a definition manager if found.
     *
     * @param key The key of the definition manager.
     * @return The needed definition manager.
     */
    protected DefinitionManager getProperDefinitionManager(String key) {
        DefinitionManager mgr = null;

        if (key != null) {
            mgr = key2definitionManager.get(key);
        }
        if (mgr == null) {
            mgr = this.mgr;
        }

        return mgr;
    }
}
