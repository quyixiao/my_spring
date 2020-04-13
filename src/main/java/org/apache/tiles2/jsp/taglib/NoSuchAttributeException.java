/*
 * $Id: NoSuchAttributeException.java 653384 2008-05-05 09:31:24Z apetrelli $
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
package org.apache.tiles2.jsp.taglib;

import org.apache.tiles2.TilesException;

/**
 * Indicates that a named attribute has not been found.
 *
 * @version $Rev: 653384 $ $Date: 2008-05-05 11:31:24 +0200 (lun, 05 mag 2008) $
 * @since 2.1.0
 */
public class NoSuchAttributeException extends TilesException {

    /**
     * Constructor.
     *
     * @since 2.1.0
     */
    public NoSuchAttributeException() {
    }

    /**
     * Constructor.
     *
     * @param message The detail message.
     * @since 2.1.0
     */
    public NoSuchAttributeException(String message) {
        super(message);
    }

    /**
     * Constructor.
     *
     * @param e The exception to be wrapped.
     * @since 2.1.0
     */
    public NoSuchAttributeException(Exception e) {
        super(e);
    }

    /**
     * Constructor.
     *
     * @param message The detail message.
     * @param e The exception to be wrapped.
     * @since 2.1.0
     */
    public NoSuchAttributeException(String message, Exception e) {
        super(message, e);
    }

}
