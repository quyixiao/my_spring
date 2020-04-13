/*
 * $Id: TilesServlet.java 797540 2009-07-24 15:42:00Z apetrelli $
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
package org.apache.tiles2.web.startup;

import org.apache.tiles2.startup.BasicTilesInitializer;
import org.apache.tiles2.startup.TilesInitializer;

/**
 * Initialization Servlet.
 *
 * @see TilesListener
 * @version $Rev: 797540 $ $Date: 2009-07-24 17:42:00 +0200 (ven, 24 lug 2009) $
 * @deprecated Please extend {@link AbstractTilesInitializerServlet}.
 */
public class TilesServlet extends AbstractTilesInitializerServlet {

    /**
     * Creates a new instance of {@link BasicTilesInitializer}. Override it to use a different initializer.
     *
     * @return The Tiles servlet-based initializer.
     * @since 2.1.2
     */
    protected TilesInitializer createTilesInitializer() {
        return new BasicTilesInitializer();
    }
}
