/*
 * $Id: TilesContextFactory.java 784215 2009-06-12 17:36:13Z apetrelli $
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
package org.apache.tiles.context;

import org.apache.tiles.TilesApplicationContext;

/**
 * Creates an instance of the appropriate TilesApplicationContext
 * implementation.
 *
 * @version $Rev: 784215 $ $Date: 2009-06-12 19:36:13 +0200 (ven, 12 giu 2009) $
 * @deprecated Use {@link TilesRequestContextFactory}.
 */
public interface TilesContextFactory extends TilesRequestContextFactory {

    /**
     * Create a TilesApplicationContext for the given context.
     *
     * @param context The (application) context to use.
     * @return TilesApplicationContext The Tiles application context.
     */
    TilesApplicationContext createApplicationContext(
            Object context);
}
