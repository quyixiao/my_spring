/*
 * $Id: InvalidTemplateException.java 637434 2008-03-15 15:48:38Z apetrelli $
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
package org.apache.tiles.impl;

import org.apache.tiles.TilesException;

/**
 * An invalid template has been identified.
 *
 * @version $Rev: 637434 $ $Date: 2008-03-15 16:48:38 +0100 (sab, 15 mar 2008) $
 * @since 2.1.0
 */
public class InvalidTemplateException extends TilesException {

    /**
     * Constructor.
     *
     * @since 2.1.0
     */
    public InvalidTemplateException() {
    }

    /**
     * Constructor.
     *
     * @param message The detail message.
     * @since 2.1.0
     */
    public InvalidTemplateException(String message) {
        super(message);
    }

    /**
     * Constructor.
     *
     * @param e The exception to be wrapped.
     * @since 2.1.0
     */
    public InvalidTemplateException(Exception e) {
        super(e);
    }

    /**
     * Constructor.
     *
     * @param message The detail message.
     * @param e The exception to be wrapped.
     * @since 2.1.0
     */
    public InvalidTemplateException(String message, Exception e) {
        super(message, e);
    }
}
