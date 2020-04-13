/*
 * $Id: EnhancedTilesApplicationContextFactory.java 797540 2009-07-24 15:42:00Z apetrelli $
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
package org.apache.tiles.context.enhanced;

import org.apache.tiles.TilesApplicationContext;
import org.apache.tiles.context.ChainedTilesApplicationContextFactory;

/**
 * Tiles context factory to be used together with
 * {@link EnhancedTilesApplicationContext}.
 *
 * @version $Rev: 797540 $ $Date: 2009-07-24 17:42:00 +0200 (ven, 24 lug 2009) $
 * @deprecated Create an instance of {@link EnhancedTilesApplicationContext}
 * yourself, by implementing {@link org.apache.tiles.startup.TilesInitializer}
 * or extending {@link org.apache.tiles.startup.AbstractTilesInitializer} and
 * overriding <code>createTilesApplicationContext</code> method.<br>
 */
public class EnhancedTilesApplicationContextFactory extends
        ChainedTilesApplicationContextFactory {

    /** {@inheritDoc} */
    @Override
    public TilesApplicationContext createApplicationContext(Object context) {
        TilesApplicationContext root = super.createApplicationContext(context);
        return new EnhancedTilesApplicationContext(root);
    }
}
