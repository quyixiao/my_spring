/*
 * $Id: CannotAccessMethodException.java 709153 2008-10-30 12:54:10Z apetrelli $
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
package org.apache.tiles.reflect;

import org.apache.tiles.TilesException;

/**
 * Indicates that a method cannot be accessed.
 *
 * @version $Rev: 709153 $ $Date: 2008-10-30 13:54:10 +0100 (gio, 30 ott 2008) $
 * @since 2.1.0
 */
public class CannotAccessMethodException extends TilesException {

    /**
     * Constructor.
     *
     * @since 2.1.0
     */
    public CannotAccessMethodException() {
    }

    /**
     * Constructor.
     *
     * @param message The detail message.
     * @since 2.1.0
     */
    public CannotAccessMethodException(String message) {
        super(message);
    }

    /**
     * Constructor.
     *
     * @param e The exception to be wrapped.
     * @since 2.1.0
     */
    public CannotAccessMethodException(Exception e) {
        super(e);
    }

    /**
     * Constructor.
     *
     * @param message The detail message.
     * @param e The exception to be wrapped.
     * @since 2.1.0
     */
    public CannotAccessMethodException(String message, Exception e) {
        super(message, e);
    }

}
