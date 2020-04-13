/*
 * Copyright 2002-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet.view.tiles2;

import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;


import org.apache.tiles.locale.impl.DefaultLocaleResolver;
import org.apache.tiles.request.Request;
import org.springframework.web.servlet.support.RequestContextUtils;

/**
 * Tiles LocaleResolver adapter that delegates to a Spring
 * {@link org.springframework.web.servlet.LocaleResolver},
 * exposing the DispatcherServlet-managed locale.
 *
 * <p>This adapter gets automatically registered by {@link TilesConfigurer}.
 * If you are using standard Tiles bootstrap, specify the name of this class
 * as value for the init-param "org.apache.tiles.locale.LocaleResolver".
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see UrlDefinitionsFactory#LOCALE_RESOLVER_IMPL_PROPERTY
 * @deprecated as of Spring 4.2, in favor of Tiles 3
 */
@Deprecated
public class SpringLocaleResolver extends DefaultLocaleResolver {

/*
	@Override
	public Locale resolveLocale(TilesRequestContext context) {
		if (context instanceof JspTilesRequestContext) {
			PageContext pc = ((JspTilesRequestContext) context).getPageContext();
			//return RequestContextUtils.getLocale((HttpServletRequest) pc.getRequest());
			return null;
		}
		else if (context instanceof ServletTilesRequestContext) {
			HttpServletRequest request = ((ServletTilesRequestContext) context).getRequest();
			if (request != null) {
				//return RequestContextUtils.getLocale(request);
				return null;
			}
		}
		return super.resolveLocale(context);
	}
*/

	/** {@inheritDoc} */
	public Locale resolveLocale(Request request) {
		Locale retValue = null;
		Map<String, Object> session = request.getContext("session");
		if (session != null) {
			retValue = (Locale) session.get(DefaultLocaleResolver.LOCALE_KEY);
		}
		if (retValue == null) {
			retValue = request.getRequestLocale();
		}

		return retValue;
	}

}
