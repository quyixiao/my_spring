/*
 * $Id: PreparerFactory.java 637434 2008-03-15 15:48:38Z apetrelli $
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
package org.apache.tiles2.preparer;

import org.apache.tiles2.context.TilesRequestContext;

/**
 * <p>
 * Factory interface used to create/retrieve instances of
 * the {@link org.apache.tiles2.preparer.ViewPreparer} interface.
 * <p/>
 *
 * <p>
 * This factory provides an extension point into the default
 * tiles implementation. Implementors wishing to provide
 * per request initialization of the ViewPreparer (for instance)
 * may provide a custom prerparer.
 * </p>
 *
 * @since 2.0
 *
 * @version $Rev: 637434 $ $Date: 2008-03-15 16:48:38 +0100 (sab, 15 mar 2008) $
 */
public interface PreparerFactory {

    /**
     * Create the named {link ViewPreparer} for the specified context.
     *
     * @param name    ViewPreparer name, commonly the qualified classname.
     * @param context the context within which the preparerInstance will be invoked.
     * @return instance of the ViewPreparer
     */
    ViewPreparer getPreparer(String name, TilesRequestContext context);
}
