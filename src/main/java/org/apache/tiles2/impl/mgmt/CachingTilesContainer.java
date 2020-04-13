/*
 * $Id: CachingTilesContainer.java 711885 2008-11-06 16:06:38Z apetrelli $
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

import org.apache.tiles2.Definition;
import org.apache.tiles2.context.TilesRequestContext;
import org.apache.tiles2.mgmt.MutableTilesContainer;
import org.apache.tiles2.definition.DefinitionsFactory;
import org.apache.tiles2.impl.BasicTilesContainer;

/**
 * Mutable container which caches (in memory) the definitions
 * registered to it.  If a definition is not found in cache, it
 * will revert back to it's definitions factory.
 *
 * @since Tiles 2.0
 * @version $Rev: 711885 $ $Date: 2008-11-06 17:06:38 +0100 (gio, 06 nov 2008) $
 */
public class CachingTilesContainer extends BasicTilesContainer
    implements MutableTilesContainer {

    /**
     * The definition manager to store custom and main definitions.
     */
    private DefinitionManager mgr = new DefinitionManager();

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
        return mgr.getDefinition(definition, context);
    }


    /** {@inheritDoc} */
    @Override
    public org.apache.tiles2.definition.DefinitionsFactory getDefinitionsFactory() {
        return mgr.getFactory();
    }

    /** {@inheritDoc} */
    @Override
    public void setDefinitionsFactory(DefinitionsFactory definitionsFactory) {
        super.setDefinitionsFactory(definitionsFactory);
        mgr.setFactory(definitionsFactory);
    }

    /**
     * Registers a custom definition.
     *
     * @param definition The definition to register.
     * @param request The request inside which the definition should be
     * registered.
     */
    protected void register(Definition definition,
            TilesRequestContext request) {
        mgr.addDefinition(definition, request);
    }
}
