/**
 * Copyright (c) 2009 Mikael Lammentausta
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

import java.net.URL;
import java.io.IOException;

import java.util.Locale;

import javax.portlet.PortletURL;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.cookie.CookieSpec;
import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class OnlineClient {

  private final Log log = LogFactory.getLog(getClass().getName());

  private final String host    = "http://localhost:3000";
  private final String servlet = "";
  
  private final String railsJUnitRoute = "/caterpillar/test_bench/junit";
  
  private final String railsJUnitURL = host+servlet+railsJUnitRoute;
  
  private static int retries = 0;
  private static int timeout = 30000;
  
  
  // TODO: annotate getters
  protected URL      requestURL  = null;
  protected int      statusCode  = -1;


  // TODO: annotate setters + getters
  protected Cookie[] cookies     = null;
  protected URL      httpReferer = null;
  protected Locale   locale      = null;
  
  
  OnlineClient(URL _requestURL) {
    requestURL  = _requestURL;
    cookies     = null;
    httpReferer = null;
    locale      = null;
  }
  
  OnlineClient(URL _requestURL, Cookie[] _cookies, URL _httpReferer, Locale _locale) {
    requestURL  = _requestURL;
    cookies     = _cookies;
    httpReferer = _httpReferer;
    locale      = _locale;
  }
  

  
  /** GET
   *
   * Instantiates HttpClient, prepares it with session cookies,
   * executes the request and returns the response body.
   *
   * TODO: set response headers to internal protected variable.
   * 
   * @since 0.8.0
   */
  protected byte[] get()
  throws HttpException, IOException
  {
    // Response body from the web server
    byte[] responseBody = null;
    statusCode = -1;
    
    // Create an instance of HttpClient and prepare it
    HttpClient client = new HttpClient();
    
    client.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
    client.getParams().setParameter(HttpMethodParams.SINGLE_COOKIE_HEADER, true);

    // Set state (session cookies)
    client.setState(preparedHttpState());
    // Set timeout
    client.getHttpConnectionManager().getParams().setConnectionTimeout(timeout);
    
    
    // Create the GET method and prepare its headers
    GetMethod method = new GetMethod(requestURL.toString());
    HttpMethod _method = (HttpMethod)method;
    method = (GetMethod)prepareMethodHeaders(_method);
    
    //debugHeaders(method.getRequestHeaders());

    log.debug("Request URL: " + requestURL.toString());
    
    try {
      // Execute the method
      statusCode = client.executeMethod(method);
      
      // log the status
      if (statusCode != HttpStatus.SC_OK) {
        log.error("Request failed: " + method.getStatusLine());
        //throw new HttpException(method.getStatusLine());
      }
      else {
        log.debug("Status code: " + method.getStatusLine());
      }
      
      // Read the response body
      responseBody = method.getResponseBody();
      
      // Get session cookies
      cookies = client.getState().getCookies();
      
    } finally {
      // Release the connection
      method.releaseConnection();
    }
    
    return responseBody;
  }
  
  
  /** Returns a HttpState fixed with cookies.
   *
   * @since 0.8.0
   */
  protected HttpState preparedHttpState()
  {
    HttpState state = new HttpState();

    if (cookies != null) {
      // Add cookies to the state
      state.addCookies(cookies);
    }
      
    if (log.isDebugEnabled()) {
      Cookie[] _cookies = state.getCookies();
      log.debug("Using " + _cookies.length + " authorized cookies");
      for (Cookie cookie : _cookies)
        log.debug("HttpState-Cookie: "
                  + cookie.toString()
                  + ", domain=" + cookie.getDomain()
                  + ", path=" + cookie.getPath()
                  + ", max-age=" + cookie.getExpiryDate()
                  + ", secure=" + cookie.getSecure());
    }

    return state;
  }
  
  
  /** Prepares method headers.
   *
   * Modifies the given method by inserting, when defined:
   *  - Referer
   *  - Accept-Language
   *
   * @since 0.8.0
   */
  protected HttpMethod prepareMethodHeaders(HttpMethod method)
  {
    // Insert the HTTP Referer
    if (httpReferer != null ) {
      log.debug("HTTP referer: "+httpReferer.toString());
      method.setRequestHeader("Referer",httpReferer.toString());
    }
    else {
      //log.debug("HTTP referer is null");
    }
    
    // Insert the Locale
    if (locale != null ) {
      log.debug("Request's locale language: "+locale.toString());
      method.setRequestHeader("Accept-Language",locale.toString());
    }
    else {
      //log.debug("Locale is null");
    }
    
    // Provide custom retry handler
    method.getParams().setParameter(                                    HttpMethodParams.RETRY_HANDLER,
        new DefaultHttpMethodRetryHandler(retries, false));
           
    return method;
  }
  
  
      
  private void debugHeaders(Header[] headers) {
    log.debug("Headers:");
    for (Header h : headers)
      log.debug(h.toString());
  }
      
      
}