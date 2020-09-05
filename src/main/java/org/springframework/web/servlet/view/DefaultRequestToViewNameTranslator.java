/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.web.servlet.view;

import javax.servlet.http.HttpServletRequest;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.RequestToViewNameTranslator;
import org.springframework.web.util.UrlPathHelper;

/**
 * {@link org.springframework.web.servlet.RequestToViewNameTranslator}
 * that simply transforms the URI of the incoming request into a view name.
 *
 * <p>Can be explicitly defined as the "viewNameTranslator" bean in a
 * {@link org.springframework.web.servlet.DispatcherServlet} context.
 * Otherwise, a plain default instance will be used.
 *
 * <p>The default transformation simply strips leading and trailing slashes
 * as well as the file extension of the URI, and returns the result as the
 * view name with the configured {@link #setPrefix "prefix"} and a
 * {@link #setSuffix "suffix"} added as appropriate.
 *
 * <p>The stripping of the leading slash and file extension can be disabled
 * using the {@link #setStripLeadingSlash "stripLeadingSlash"} and
 * {@link #setStripExtension "stripExtension"} properties, respectively.
 *
 * <p>Find below some examples of request to view name translation.
 *
 * <pre class="code">http://localhost:8080/gamecast/display.html -> display
 * http://localhost:8080/gamecast/displayShoppingCart.html -> displayShoppingCart
 * http://localhost:8080/gamecast/admin/index.html -> admin/index
 * </pre>
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 * @see org.springframework.web.servlet.RequestToViewNameTranslator
 * @see org.springframework.web.servlet.ViewResolver
 * 当我们没有在 Spring mvc 的配置中手动定义一个名为 viewNameTranslator 的 bean 的时候，Spring就会为我们提供一个默认的 viewName
 * Translator，即 DefaultRequestToViewNameTranslator 会获得到请求的 URI,然后根据提供的属性做一些改造，把改造后的结果作为视图名称返回
 * 这里以请求的路径 http://locahost/app/test/index.html 为例，来说明一下 DefaultRequestToViewNameTranslator 是如何工作的，该请求
 * 的路径对应的请求 URI为 index.html，我们来看以下的几种情况，它分别是对应的逻辑视图的名称是什么
 * prefix 和 suffix 如果都存在，其他的为默认值，那么对应的返回的逻辑视图名称应该是 prefixtest/indexsuffix
 * stripLeadingSlash 和 stripExtension 都为 false, 其他的为默认值，这个时候对应的逻辑视图名称就是不去前缀 /，也不去后缀.html
 * 如果采用默认的配置时，返回的逻辑视图名称应该是 product/index
 *
 *
 *
 */
public class DefaultRequestToViewNameTranslator implements RequestToViewNameTranslator {

	private static final String SLASH = "/";

	// 前缀，表示约定好的视图名称需要加上的前缀，默认的是空串
	private String prefix = "";
	// 后缀，表示约定好的视图名称需要加上的后缀，默认是空串
	private String suffix = "";
	//  分隔符，默认是斜杠，"/"
	private String separator = SLASH;
	// 如果首字符是分隔符，是否需要去掉，默认 true
	private boolean stripLeadingSlash = true;
	// 如果最后一个字符是分隔符，是否需要去除，默认是 true
	private boolean stripTrailingSlash = true;
	// 如果请求路径包含扩展名，是否需要去除，默认是 true
	private boolean stripExtension = true;
	//
	private UrlPathHelper urlPathHelper = new UrlPathHelper();


	/**
	 * Set the prefix to prepend to generated view names.
	 * @param prefix the prefix to prepend to generated view names
	 */
	public void setPrefix(String prefix) {
		this.prefix = (prefix != null ? prefix : "");
	}

	/**
	 * Set the suffix to append to generated view names.
	 * @param suffix the suffix to append to generated view names
	 */
	public void setSuffix(String suffix) {
		this.suffix = (suffix != null ? suffix : "");
	}

	/**
	 * Set the value that will replace '{@code /}' as the separator
	 * in the view name. The default behavior simply leaves '{@code /}'
	 * as the separator.
	 */
	public void setSeparator(String separator) {
		this.separator = separator;
	}

	/**
	 * Set whether or not leading slashes should be stripped from the URI when
	 * generating the view name. Default is "true".
	 */
	public void setStripLeadingSlash(boolean stripLeadingSlash) {
		this.stripLeadingSlash = stripLeadingSlash;
	}

	/**
	 * Set whether or not trailing slashes should be stripped from the URI when
	 * generating the view name. Default is "true".
	 */
	public void setStripTrailingSlash(boolean stripTrailingSlash) {
		this.stripTrailingSlash = stripTrailingSlash;
	}

	/**
	 * Set whether or not file extensions should be stripped from the URI when
	 * generating the view name. Default is "true".
	 */
	public void setStripExtension(boolean stripExtension) {
		this.stripExtension = stripExtension;
	}

	/**
	 * Set if URL lookup should always use the full path within the current servlet
	 * context. Else, the path within the current servlet mapping is used
	 * if applicable (i.e. in the case of a ".../*" servlet mapping in web.xml).
	 * Default is "false".
	 * @see UrlPathHelper#setAlwaysUseFullPath
	 */
	public void setAlwaysUseFullPath(boolean alwaysUseFullPath) {
		this.urlPathHelper.setAlwaysUseFullPath(alwaysUseFullPath);
	}

	/**
	 * Set if the context path and request URI should be URL-decoded.
	 * Both are returned <i>undecoded</i> by the Servlet API,
	 * in contrast to the servlet path.
	 * <p>Uses either the request encoding or the default encoding according
	 * to the Servlet spec (ISO-8859-1).
	 * @see UrlPathHelper#setUrlDecode
	 */
	public void setUrlDecode(boolean urlDecode) {
		this.urlPathHelper.setUrlDecode(urlDecode);
	}

	/**
	 * Set if ";" (semicolon) content should be stripped from the request URI.
	 * @see UrlPathHelper#setRemoveSemicolonContent(boolean)
	 */
	public void setRemoveSemicolonContent(boolean removeSemicolonContent) {
		this.urlPathHelper.setRemoveSemicolonContent(removeSemicolonContent);
	}

	/**
	 * Set the {@link UrlPathHelper} to use for
	 * the resolution of lookup paths.
	 * <p>Use this to override the default UrlPathHelper with a custom subclass,
	 * or to share common UrlPathHelper settings across multiple web components.
	 */
	public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
		Assert.notNull(urlPathHelper, "UrlPathHelper must not be null");
		this.urlPathHelper = urlPathHelper;
	}


	/**
	 * Translates the request URI of the incoming {@link HttpServletRequest}
	 * into the view name based on the configured parameters.
	 * @see UrlPathHelper#getLookupPathForRequest
	 * @see #transformPath
	 * prefix 和 suffix 如果都存在，其他的为默认值，那么对应的返回的逻辑视图名称应该是 prifix+path+suffix
	 */
	@Override
	public String getViewName(HttpServletRequest request) {
		String lookupPath = this.urlPathHelper.getLookupPathForRequest(request);
		return (this.prefix + transformPath(lookupPath) + this.suffix);
	}

	/**
	 * Transform the request URI (in the context of the webapp) stripping
	 * slashes and extensions, and replacing the separator as required.
	 * @param lookupPath the lookup path for the current request,
	 * as determined by the UrlPathHelper
	 * @return the transformed path, with slashes and extensions stripped
	 * if desired
	 */
	protected String transformPath(String lookupPath) {
		String path = lookupPath;
		if (this.stripLeadingSlash && path.startsWith(SLASH)) {
			path = path.substring(1);
		}
		if (this.stripTrailingSlash && path.endsWith(SLASH)) {
			path = path.substring(0, path.length() - 1);
		}
		if (this.stripExtension) {
			path = StringUtils.stripFilenameExtension(path);
		}
		if (!SLASH.equals(this.separator)) {
			path = StringUtils.replace(path, SLASH, this.separator);
		}
		return path;
	}

}
