/*
 * $Id: DefinitionsReader.java 652862 2008-05-02 18:22:56Z apetrelli $
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

package org.apache.tiles2.definition;

import java.util.Map;

import org.apache.tiles2.Definition;

/**
 * Interface for reading <code>{@link Definition}</code> from a source.
 * <p/>
 * <p>This interface provides a standard way to read
 * <code>{@link Definition}</code> objects from a source.  Implementations
 * should define what the source is, whether it be a persistent store such as a
 * configuration file or database, or something like a web service.  The
 * DefinitionsReader is responsible for reading from a single location.  It does
 * not perform any internationalization duties or inheritance of Definitions.
 * It only reads from the source and returns a Map of objects read.</p>
 *
 * @version $Rev: 652862 $ $Date: 2008-05-02 20:22:56 +0200 (ven, 02 mag 2008) $
 */
public interface DefinitionsReader {

    /**
     * Initializes the <code>DefinitionsReader</code> object.
     * <p/>
     * This method must be called before the {@link #read(Object)} method is called.
     *
     * @param params A map of properties used to set up the reader.
     * @throws DefinitionsFactoryException if required properties are not
     *                                     passed in or the initialization fails.
     */
    void init(Map<String, String> params);

    /**
     * Reads <code>{@link Definition}</code> objects from a source.
     * <p/>
     * Implementations should publish what type of source object is expected.
     *
     * @param source The source from which definitions will be read.
     * @return a Map of <code>Definition</code> objects read from
     *         the source.
     * @throws DefinitionsFactoryException if the source is invalid or
     *                                     an error occurs when reading definitions.
     */
    Map<String, Definition> read(Object source);

}
