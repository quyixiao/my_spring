/*
 * $Id: NotAServletEnvironmentException.java 788034 2009-06-24 14:20:32Z apetrelli $
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

package org.apache.tiles.servlet.context;

import org.apache.tiles.TilesException;

/**
 * Exception that indicates that a resource could not be used because it is not
 * in a servlet environment.
 *
 * @version $Rev: 788034 $ $Date: 2009-06-24 16:20:32 +0200 (mer, 24 giu 2009) $
 * @since 2.2.0
 */
public class NotAServletEnvironmentException extends TilesException {

    /**
     * Constructor.
     *
     * @since 2.2.0
     */
    public NotAServletEnvironmentException() {
    }

    /**
     * Constructor.
     *
     * @param message The detail message.
     * @since 2.2.0
     */
    public NotAServletEnvironmentException(String message) {
        super(message);
    }

    /**
     * Constructor.
     *
     * @param e The exception to be wrapped.
     * @since 2.2.0
     */
    public NotAServletEnvironmentException(Exception e) {
        super(e);
    }

    /**
     * Constructor.
     *
     * @param message The detail message.
     * @param e The exception to be wrapped.
     * @since 2.2.0
     */
    public NotAServletEnvironmentException(String message, Exception e) {
        super(message, e);
    }

}
