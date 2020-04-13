/*
 * $Id: ViewPreparerSupport.java 652862 2008-05-02 18:22:56Z apetrelli $
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
import org.apache.tiles2.AttributeContext;

/**
 * Basic implementation of ViewPreparer.  Implementations can extend this class
 * to insulate themselves from changes in the <code>ViewPreparer</code>
 * interface.
 *
 * @since Tiles 2.0
 * @version $Rev: 652862 $ $Date: 2008-05-02 20:22:56 +0200 (ven, 02 mag 2008) $
 */
public class ViewPreparerSupport implements ViewPreparer {

    /** {@inheritDoc} */
    public void execute(TilesRequestContext tilesContext,
                        AttributeContext attributeContext) {
    }
}
