/*
 * $Id: ServletTilesApplicationContextFactory.java 797540 2009-07-24 15:42:00Z apetrelli $
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

package org.apache.tiles2.servlet.context;

import org.apache.tiles2.Initializable;
import org.apache.tiles2.TilesApplicationContext;
import org.apache.tiles2.context.AbstractTilesApplicationContextFactory;
import org.apache.tiles2.startup.AbstractTilesInitializer;
import org.apache.tiles2.startup.TilesInitializer;

import javax.servlet.ServletContext;
import java.util.Map;

/**
 * Creates an instance of the appropriate TilesApplicationContext implementation
 * under a servlet environment.
 *
 * @version $Rev: 797540 $ $Date: 2009-07-24 17:42:00 +0200 (ven, 24 lug 2009) $
 * @since 2.1.1
 * @deprecated Create an instance of {@link ServletTilesApplicationContext}
 * yourself, by implementing {@link TilesInitializer}
 * or extending {@link AbstractTilesInitializer} and
 * overriding <code>createTilesApplicationContext</code> method.<br>
 */
public class ServletTilesApplicationContextFactory extends
        AbstractTilesApplicationContextFactory implements Initializable {

    /** {@inheritDoc} */
    public void init(Map<String, String> configParameters) {
    }

    /** {@inheritDoc} */
    public TilesApplicationContext createApplicationContext(Object context) {
        if (context instanceof ServletContext) {
            ServletContext servletContext = (ServletContext) context;
            return new ServletTilesApplicationContext(servletContext);
        }
        return null;
    }
}
