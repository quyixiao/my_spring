/*
 * $Id: ChainedTilesApplicationContextFactory.java 797540 2009-07-24 15:42:00Z apetrelli $
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

package org.apache.tiles2.context;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.tiles2.Initializable;
import org.apache.tiles2.TilesApplicationContext;
import org.apache.tiles2.awareness.AbstractTilesApplicationContextFactoryAware;
import org.apache.tiles2.reflect.ClassUtil;
import org.apache.tiles2.startup.AbstractTilesInitializer;
import org.apache.tiles2.startup.TilesInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation for TilesApplicationContextFactory, that creates a chain of
 * sub-factories, trying each one until it returns a not-null value.
 *
 * @version $Rev: 797540 $ $Date: 2009-07-24 17:42:00 +0200 (ven, 24 lug 2009) $
 * @since 2.1.1
 * @deprecated Create an instance of {@link TilesApplicationContext} yourself,
 * by implementing {@link TilesInitializer} or
 * extending {@link AbstractTilesInitializer} and
 * overriding <code>createTilesApplicationContext</code> method.<br>
 * Moreover, it does not make sense to "try" if your application is
 * servlet-based, portlet-based, etc. You know it, right?
 */
public class ChainedTilesApplicationContextFactory extends
        AbstractTilesApplicationContextFactory implements Initializable {

    /**
     * Factory class names initialization parameter to use.
     *
     * @since 2.1.1
     */
    public static final String FACTORY_CLASS_NAMES =
        "org.apache.tiles.context.ChainedTilesApplicationContextFactory.FACTORY_CLASS_NAMES";

    /**
     * The default class names to instantiate that compose the chain..
     *
     * @since 2.1.1
     */
    public static final String[] DEFAULT_FACTORY_CLASS_NAMES = {
            "org.apache.tiles.servlet.context.ServletTilesApplicationContextFactory",
            "org.apache.tiles.portlet.context.PortletTilesApplicationContextFactory"};

    /**
     * The logging object.
     */
    private final Logger log = LoggerFactory
            .getLogger(ChainedTilesApplicationContextFactory.class);

    /**
     * The Tiles context factories composing the chain.
     */
    private List<AbstractTilesApplicationContextFactory> factories;

    /**
     * Sets the factories to be used.
     *
     * @param factories The factories to be used.
     */
    public void setFactories(
            List<AbstractTilesApplicationContextFactory> factories) {
        this.factories = factories;
    }

    /** {@inheritDoc} */
    public void init(Map<String, String> configParameters) {
        String[] classNames = null;
        String classNamesString = configParameters.get(FACTORY_CLASS_NAMES);
        if (classNamesString != null) {
            classNames = classNamesString.split("\\s*,\\s*");
        }
        if (classNames == null || classNames.length <= 0) {
            classNames = DEFAULT_FACTORY_CLASS_NAMES;
        }

        factories = new ArrayList<AbstractTilesApplicationContextFactory>();
        for (int i = 0; i < classNames.length; i++) {
            try {
                Class<? extends AbstractTilesApplicationContextFactory> clazz = ClassUtil
                        .getClass(classNames[i],
                                AbstractTilesApplicationContextFactory.class);
                AbstractTilesApplicationContextFactory factory = clazz
                        .newInstance();
                if (factory instanceof AbstractTilesApplicationContextFactoryAware) {
                    ((AbstractTilesApplicationContextFactoryAware) factory)
                            .setApplicationContextFactory(this);
                }
                factories.add(factory);
            } catch (ClassNotFoundException e) {
                // We log it, because it could be a default configuration class that
                // is simply not present.
                log.warn("Cannot find TilesContextFactory class "
                        + classNames[i]);
                if (log.isDebugEnabled()) {
                    log.debug("Cannot find TilesContextFactory class "
                            + classNames[i], e);
                }
            } catch (InstantiationException e) {
                throw new IllegalArgumentException(
                        "Cannot instantiate TilesFactoryClass " + classNames[i],
                        e);
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException(
                        "Cannot access TilesFactoryClass " + classNames[i]
                                + " default constructor", e);
            }
        }
    }

    /** {@inheritDoc} */
    public TilesApplicationContext createApplicationContext(Object context) {
        TilesApplicationContext retValue = null;

        for (Iterator<AbstractTilesApplicationContextFactory> factoryIt = factories
                .iterator(); factoryIt.hasNext() && retValue == null;) {
            retValue = factoryIt.next().createApplicationContext(context);
        }

        if (retValue == null) {
            throw new IllegalArgumentException(
                    "Cannot find a factory to create the application context");
        }

        return retValue;
    }
}
