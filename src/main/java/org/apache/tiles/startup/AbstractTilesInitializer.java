/*
 * $Id: AbstractTilesInitializer.java 797540 2009-07-24 15:42:00Z apetrelli $
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

package org.apache.tiles.startup;

import org.apache.tiles.TilesApplicationContext;
import org.apache.tiles.TilesContainer;
import org.apache.tiles.TilesException;
import org.apache.tiles.access.TilesAccess;
import org.apache.tiles.factory.AbstractTilesContainerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default Tiles initialization delegate implementation under a servlet
 * environment. It uses init parameters to create the
 * {@link TilesApplicationContext} and the {@link TilesContainer}.
 *
 * @version $Rev: 797540 $ $Date: 2009-07-24 17:42:00 +0200 (ven, 24 lug 2009) $
 * @since 2.2.0
 */
public abstract class AbstractTilesInitializer implements TilesInitializer {

    /**
     * The logging object.
     */
    private Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Init parameter to define the key under which the container will be
     * stored.
     *
     * @since 2.1.2
     */
    public static final String CONTAINER_KEY_INIT_PARAMETER =
        "org.apache.tiles.startup.AbstractTilesInitializer.CONTAINER_KEY";

    /**
     * The initialized application context.
     */
    private TilesApplicationContext applicationContext;

    /**
     * The initialized container.
     */
    private TilesContainer container;

    /** {@inheritDoc} */
    public void initialize(TilesApplicationContext applicationContext) {
        this.applicationContext = createTilesApplicationContext(applicationContext);
        String key = getContainerKey(this.applicationContext);
        container = createContainer(this.applicationContext);
        TilesAccess.setContainer(this.applicationContext, container, key);
    }

    /** {@inheritDoc} */
    public void destroy() {
        try {
            TilesAccess.setContainer(applicationContext, null,
                    getContainerKey(applicationContext));
        } catch (TilesException e) {
            log.warn("Unable to remove tiles container from service.", e);
        }
    }

    /**
     * Creates the Tiles application context, to be used across all the
     * Tiles-based application. If you override this class, please override this
     * method or
     * {@link #createAndInitializeTilesApplicationContextFactory(TilesApplicationContext)}
     * .<br>
     * This implementation returns the preliminary context passed as a parameter
     *
     * @param preliminaryContext The preliminary application context to use.
     * @return The Tiles application context.
     * @since 2.2.0
     */
    protected TilesApplicationContext createTilesApplicationContext(
            TilesApplicationContext preliminaryContext) {
        return preliminaryContext;
    }

    /**
     * Returns the container key under which the container will be stored.
     * This implementation returns <code>null</code> so that the container will
     * be the default one.
     *
     * @param applicationContext The Tiles application context to use.
     * @return The container key.
     * @since 2.2.0
     */
    protected String getContainerKey(TilesApplicationContext applicationContext) {
        return null;
    }

    /**
     * Creates a Tiles container. If you override this class, please override
     * this method or {@link #createContainerFactory(TilesApplicationContext)}.
     *
     * @param context The servlet context to use.
     * @return The created container.
     * @since 2.2.0
     */
    protected TilesContainer createContainer(TilesApplicationContext context) {
        AbstractTilesContainerFactory factory = createContainerFactory(context);
        return factory.createContainer(context);
    }

    /**
     * Creates a Tiles container factory. If you override this class, please
     * override this method or {@link #createContainer(TilesApplicationContext)}.
     *
     * @param context The servlet context to use.
     * @return The created container factory.
     * @since 2.2.0
     */
    protected abstract AbstractTilesContainerFactory createContainerFactory(
            TilesApplicationContext context);
}
