/*
 * $Id: NotAvailableFeatureException.java 819641 2009-09-28 16:56:14Z apetrelli $
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

package org.apache.tiles.factory;

import org.apache.tiles.TilesException;

/**
 * Exception that indicates that a feature could not be used since it is not
 * available.
 *
 * @version $Rev: 819641 $ $Date: 2009-09-28 18:56:14 +0200 (lun, 28 set 2009) $
 * @since 2.1.4
 */
public class NotAvailableFeatureException extends TilesException {

    /**
     * Constructor.
     *
     * @since 2.1.4
     */
    public NotAvailableFeatureException() {
    }

    /**
     * Constructor.
     *
     * @param message The detail message.
     * @since 2.1.4
     */
    public NotAvailableFeatureException(String message) {
        super(message);
    }

    /**
     * Constructor.
     *
     * @param e The cause to be wrapped.
     * @since 2.1.4
     */
    public NotAvailableFeatureException(Throwable e) {
        super(e);
    }

    /**
     * Constructor.
     *
     * @param message The detail message.
     * @param e The cause to be wrapped.
     * @since 2.1.4
     */
    public NotAvailableFeatureException(String message, Throwable e) {
        super(message, e);
    }
}
