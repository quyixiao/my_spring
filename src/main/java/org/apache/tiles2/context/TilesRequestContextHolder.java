/*
 * $Id: TilesRequestContextHolder.java 788032 2009-06-24 14:08:32Z apetrelli $
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

/**
 * Holds in a {@link ThreadLocal} object a {@link TilesRequestContext}.
 *
 * @version $Rev: 788032 $ $Date: 2009-06-24 16:08:32 +0200 (mer, 24 giu 2009) $
 * @since 2.2.0
 */
public class TilesRequestContextHolder {

    /**
     * The Tiles request context holder.
     */
    private ThreadLocal<TilesRequestContext> requestHolder = new ThreadLocal<TilesRequestContext>();

    /**
     * Sets the Tiles request context to use.
     *
     * @param request The Tiles request.
     * @since 2.2.0
     */
    public void setTilesRequestContext(TilesRequestContext request) {
        requestHolder.set(request);
    }

    /**
     * Returns the Tiles request context to use.
     *
     * @return The Tiles request.
     * @since 2.2.0
     */
    public TilesRequestContext getTilesRequestContext() {
        return requestHolder.get();
    }
}
