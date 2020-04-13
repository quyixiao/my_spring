/*
 * $Id: WebappClassTemplateLoader.java 821407 2009-10-03 20:12:01Z apetrelli $
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
import java.io.Reader;

import javax.servlet.ServletContext;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.cache.WebappTemplateLoader;

/**
 * Delegates loading templates using a {@link WebappTemplateLoader} and, if not
 * found, a {@link ClassTemplateLoader}. The resources are loaded from the
 * webapp root and from the classpath root.
 *
 * @version $Rev: 821407 $ $Date: 2009-10-03 22:12:01 +0200 (sab, 03 ott 2009) $
 * @since 2.2.1
 */
public class WebappClassTemplateLoader implements TemplateLoader {

    /**
     * The webapp template loader.
     */
    private WebappTemplateLoader webappTemplateLoader;

    /**
     * The webapp template loader.
     */
    private ClassTemplateLoader classTemplateLoader;

    /**
     * Constructor.
     *
     * @param servletContext The servlet context.
     */
    public WebappClassTemplateLoader(ServletContext servletContext) {
        webappTemplateLoader = new WebappTemplateLoader(servletContext);
        classTemplateLoader = new ClassTemplateLoader(getClass(), "/");
    }

    /** {@inheritDoc} */
    public Object findTemplateSource(String name) throws IOException {
        Object retValue = webappTemplateLoader.findTemplateSource(name);
        if (retValue == null) {
            retValue = classTemplateLoader.findTemplateSource(name);
        }
        return retValue;
    }

    /** {@inheritDoc} */
    public void closeTemplateSource(Object templateSource) throws IOException {
        webappTemplateLoader.closeTemplateSource(templateSource);
    }

    /** {@inheritDoc} */
    public long getLastModified(Object templateSource) {
        return webappTemplateLoader.getLastModified(templateSource);
    }

    /** {@inheritDoc} */
    public Reader getReader(Object templateSource, String encoding)
            throws IOException {
        return webappTemplateLoader.getReader(templateSource, encoding);
    }
}
