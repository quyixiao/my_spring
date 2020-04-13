/*
 * $Id: LocaleResolver.java 637434 2008-03-15 15:48:38Z apetrelli $
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
package org.apache.tiles2.locale;

import java.util.Locale;
import java.util.Map;

import org.apache.tiles2.context.TilesRequestContext;

/**
 * It represents an object able to resolve the current locale for the current
 * request, where its strategy depends on its implementation.
 *
 * @version $Rev: 637434 $ $Date: 2008-03-15 16:48:38 +0100 (sab, 15 mar 2008) $
 */
public interface LocaleResolver {

    /**
     * Initializes the <code>LocaleResolver</code> object. <p/> This method
     * must be called before the {@link #resolveLocale(TilesRequestContext)}
     * method is called.
     *
     * @param params A map of properties used to set up the resolver.
     */
    void init(Map<String, String> params);

    /**
     * Resolves the locale.
     *
     * @param request The Tiles request object.
     * @return The current locale for the current request.
     */
    Locale resolveLocale(TilesRequestContext request);
}
