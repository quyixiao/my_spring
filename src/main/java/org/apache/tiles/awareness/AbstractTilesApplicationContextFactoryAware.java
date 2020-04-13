/*
 * $Id: AbstractTilesApplicationContextFactoryAware.java 797540 2009-07-24 15:42:00Z apetrelli $
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

import org.apache.tiles.context.AbstractTilesApplicationContextFactory;

/**
 * It represents an object that can have a reference to the
 * {@link AbstractTilesApplicationContextFactoryAware}.
 *
 * @version $Rev: 797540 $ $Date: 2009-07-24 17:42:00 +0200 (ven, 24 lug 2009) $
 * @since 2.1.1
 * @deprecated Since {@link AbstractTilesApplicationContextFactory} is
 * deprecated, this dependency injection interface is deprecated.
 */
public interface AbstractTilesApplicationContextFactoryAware {

    /**
     * Sets the Tiles application context factory.
     *
     * @param contextFactory The Tiles context factory.
     * @since 2.1.1
     */
    void setApplicationContextFactory(
            AbstractTilesApplicationContextFactory contextFactory);
}
