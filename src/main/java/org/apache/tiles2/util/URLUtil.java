/*
 * $Id: URLUtil.java 798956 2009-07-29 15:41:10Z apetrelli $
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

package org.apache.tiles2.util;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Utilities to manage URLs in the Tiles environment.
 *
 * @version $Rev: 798956 $ $Date: 2009-07-29 17:41:10 +0200 (mer, 29 lug 2009) $
 * @since 2.2.0
 */
public final class URLUtil {

    /**
     * Private constructor to avoid instantiation.
     */
    private URLUtil() { }

    /**
     * Filters a collection of URLs and removes all that have an underscore in
     * their name (not in their path).
     *
     * @param urlSet The set of URLs to filter.
     * @return A new list containing only those URLs that does not have an
     * underscore in their name.
     * @since 2.2.0
     */
    public static List<URL> getBaseTilesDefinitionURLs(Collection<? extends URL> urlSet) {
        List<URL> filteredUrls = new ArrayList<URL>();
        for (URL url : urlSet) {
            String externalForm = url.toExternalForm();
            if (externalForm.indexOf('_', externalForm.lastIndexOf("/")) < 0) {
                filteredUrls.add(url);
            }
        }
        return filteredUrls;
    }
}
