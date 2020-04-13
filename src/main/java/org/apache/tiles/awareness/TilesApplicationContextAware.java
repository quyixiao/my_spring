/*
 * $Id: TilesApplicationContextAware.java 642355 2008-03-28 20:02:36Z apetrelli $
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
package org.apache.tiles.awareness;

import org.apache.tiles.TilesApplicationContext;

/**
 * It represents an object that can have a reference to the
 * {@link TilesApplicationContext}.
 *
 * @version $Rev: 642355 $ $Date: 2008-03-28 21:02:36 +0100 (ven, 28 mar 2008) $
 * @since 2.1.0
 */
public interface TilesApplicationContextAware {

    /**
     * Sets the Tiles application context.
     *
     * @param applicationContext The Tiles application context.
     * @since 2.1.0
     */
    void setApplicationContext(TilesApplicationContext applicationContext);
}
