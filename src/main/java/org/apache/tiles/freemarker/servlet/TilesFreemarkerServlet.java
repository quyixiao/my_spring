/*
 * $Id: TilesFreemarkerServlet.java 821407 2009-10-03 20:12:01Z apetrelli $
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

package org.apache.tiles.freemarker.servlet;

import java.io.IOException;

import org.apache.tiles.freemarker.template.TilesFMModelRepository;

import freemarker.cache.TemplateLoader;
import freemarker.ext.beans.BeanModel;
import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.servlet.FreemarkerServlet;
import freemarker.template.Configuration;

/**
 * Extends FreemarkerServlet to load Tiles directives as a shared variable.
 *
 * @version $Rev: 821407 $ $Date: 2009-10-03 22:12:01 +0200 (sab, 03 ott 2009) $
 * @since 2.2.0
 */
public class TilesFreemarkerServlet extends FreemarkerServlet {

    /** {@inheritDoc} */
    @Override
    protected Configuration createConfiguration() {
        Configuration configuration = super.createConfiguration();

        BeanModel tilesBeanModel = new BeanModel(new TilesFMModelRepository(),
                BeansWrapper.getDefaultInstance());
        configuration.setSharedVariable("tiles", tilesBeanModel);
        return configuration;
    }

    /** {@inheritDoc} */

    @Override
    protected TemplateLoader createTemplateLoader(String templatePath)
            throws IOException {
        return new WebappClassTemplateLoader(getServletContext());
    }
}
