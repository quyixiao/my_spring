/*
 * $Id: DefinitionsFactoryUtil.java 734110 2009-01-13 11:49:19Z apetrelli $
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

package org.apache.tiles.definition.util;

import org.apache.tiles.TilesContainer;
import org.apache.tiles.access.TilesAccess;
import org.apache.tiles.definition.DefinitionsFactory;
import org.apache.tiles.definition.Refreshable;
import org.apache.tiles.impl.BasicTilesContainer;

/**
 * Various {@link DefinitionsFactory} utilities.
 *
 * @version $Rev: 734110 $ $Date: 2009-01-13 12:49:19 +0100 (mar, 13 gen 2009) $
 */
public final class DefinitionsFactoryUtil {

    /**
     * Private constructor to avoid instatiation.
     */
    private DefinitionsFactoryUtil() {
    }

    /**
     * Reloads the definitions factory content, if necessary.
     *
     * @param context The context object to use
     * @throws org.apache.tiles.definition.DefinitionsFactoryException If
     * something goes wrong during reload.
     * @deprecated Let the definitions DAO manage auto-reload.
     */
    @Deprecated
    public static void reloadDefinitionsFactory(Object context) {
        TilesContainer container = TilesAccess.getContainer(context);
        if (container instanceof BasicTilesContainer) {
            BasicTilesContainer basic = (BasicTilesContainer) container;
            DefinitionsFactory factory = basic.getDefinitionsFactory();
            if (factory instanceof Refreshable) {
                Refreshable rFactory = (Refreshable) factory;
                if (rFactory.refreshRequired()) {
                    rFactory.refresh();
                }
            }
        }
    }
}
