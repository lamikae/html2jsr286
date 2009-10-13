/**
 * Copyright (c) 2008 Mikael Lammentausta
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.celamanzi.liferay.portlets.rails286;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/** Formulates full URLs from little pieces. Requires cleanup. */
public class RouteAnalyzer {

  private final Log log = LogFactory.getLog(getClass().getName());

  private java.net.URL baseUrl;
  private String       servlet = null;


  protected RouteAnalyzer( java.net.URL bu, String srvl ) {
    baseUrl = bu;
    servlet = srvl;
	log.debug("Configured for servlet: "+servlet);
  }
  protected RouteAnalyzer( java.net.URL bu ) {
	baseUrl     = bu;
  }


  /** Gets the href attribute of a link, and returns the request path */
  public String getRequestRoute( String href )
  throws java.net.MalformedURLException
  {
    log.debug("Parsing the request route from: "+href);
	java.net.URL url = null;
	String path = null;
    String route = null;
	// needsParamsStrip is for optimization, since this method may well be one of the bottlenecks.
	Boolean needsParamsStrip = false;

	// first extract path component,
	// without servlet definition, the path from a valid URL is returned.
	try {
		url = new java.net.URL(href);
		path = url.getPath();
		needsParamsStrip = false;
		if (servlet == null) {
			if (path.equals("")) {
				return "/";
			}
			else {
				// the most common situation
				// if the HTML contains absolute URLs
				return path;
			}
		}
	}
	catch (java.net.MalformedURLException e) {
		path = href;
		needsParamsStrip = true;
	}

	// without any valid href, return empty string
	if ((path == null) || (path == "")) {
		return "";
	}

	if (servlet != null) {
		// if servlet is defined, and the url contains only the servlet path,
		// consider returning root route as well.
		if (path.equals("/"+servlet)) {
			return "/";
		}

		// strip servlet from the path
		route = path.replaceFirst("/?"+servlet,"");
	}
	else {
		route = path;
	}

	if (needsParamsStrip) {
		// this route may still hold encoded parameters, so they should be stripped.
		// keep all characters up until "?"
		Matcher url_params = Pattern.compile("^([^?]*)").matcher(route);
		if (url_params.find()) {
			route = url_params.group(0);
		}
	}

	log.debug("Route: "+route);
	return route;
  }


  /** Drop the servlet from baseUrl, and use only the protocol, host and port.
    * if url = baseUrl + path, the servlet portion is duplicated => error 404 on Tomcat
    */
  public java.net.URL getFullURL( String path )
  throws java.net.MalformedURLException {
    log.debug("Parsing the full URL for "+path);

    // lookout for relative links
    Pattern rel_link_pattern = Pattern.compile("^http");
    Matcher rel_link_matcher = rel_link_pattern.matcher(path);
    if ( rel_link_matcher.find() ) {
      log.debug(path + " is already an URL");
      return new java.net.URL(path);
    }
    log.debug(path + " is a relative path");

    // Clear out the servlet from path
    path = path.replaceFirst("/?"+servlet,"");

    //baseUrl.getPort() returns -1 if the port is not explicitly defined!!
    int port = baseUrl.getPort();
    if (port == -1) { port = baseUrl.getDefaultPort(); }

    return new java.net.URL(
      baseUrl.getProtocol()+"://"+baseUrl.getHost()+":"+port+"/"+servlet+path
    );
  }

}