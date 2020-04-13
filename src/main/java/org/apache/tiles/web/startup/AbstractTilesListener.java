/*
 * $Id: AbstractTilesListener.java 797540 2009-07-24 15:42:00Z apetrelli $
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
package org.apache.tiles.web.startup;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.tiles.TilesException;
import org.apache.tiles.servlet.context.ServletTilesApplicationContext;
import org.apache.tiles.servlet.context.ServletUtil;
import org.apache.tiles.startup.TilesInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listener for the initialization of the Tiles container.
 *
 * @version $Rev: 797540 $ $Date: 2009-07-24 17:42:00 +0200 (ven, 24 lug 2009) $
 */
public abstract class AbstractTilesListener implements ServletContextListener {

    /**
     * Log instance.
     */
    private Logger log = LoggerFactory.getLogger(AbstractTilesListener.class);

    /**
     * The initializer object.
     *
     * @since 2.1.2
     */
    protected TilesInitializer initializer;

    /**
     * Initialize the TilesContainer and place it
     * into service.
     *
     * @param event The intercepted event.
     */
    public void contextInitialized(ServletContextEvent event) {
        ServletContext servletContext = event.getServletContext();
        initializer = createTilesInitializer();
        initializer.initialize(new ServletTilesApplicationContext(
                servletContext));
    }

    /**
     * Remove the tiles container from service.
     *
     * @param event The intercepted event.
     */
    public void contextDestroyed(ServletContextEvent event) {
        ServletContext servletContext = event.getServletContext();
        try {
            ServletUtil.setContainer(servletContext, null);
        } catch (TilesException e) {
            log.warn("Unable to remove tiles container from service.", e);
        }
    }

    /**
     * Creates a new instance of {@link TilesInitializer}. Implement it to use a
     * different initializer.
     *
     * @return The Tiles servlet-based initializer.
     * @since 2.2.0
     */
    protected abstract TilesInitializer createTilesInitializer();
}
