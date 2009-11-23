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

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.cookie.CookieSpec;
import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;

import javax.portlet.PortletURL;


public class OnlineUtils {

  private static final Log log     = LogFactory.getLog(OnlineUtils.class);
  public  static       int retries = 0;
  public  static       int timeout = 30000;


  /** GETs a web page by HttpClient.executeMethod(GetMethod(url)) */
  private static String getWebPage( java.net.URL url, Cookie[] authCookies, java.net.URL httpReferer, java.util.Locale locale )
  throws HttpException, IOException {

    // the httpclient request response will be stored in this variable.
    String html = null;

    // Create an instance of HttpClient.
    HttpClient client = new HttpClient();

    // Create a method instance.
    log.debug("Requesting URL: " + url.toString());
    GetMethod method = new GetMethod(url.toString());

    // Insert the HTTP Referer
    if (httpReferer != null ) {
      log.debug("HTTP referer: "+httpReferer.toString());
      method.setRequestHeader("Referer",httpReferer.toString());
    }
    else {
      log.debug("HTTP referer is null");
    }

    // Insert the Locale
    if (locale != null ) {
      log.debug("Request's locale language: "+locale.toString());
      method.setRequestHeader("Accept-Language",locale.toString());
    }
    else {
      log.debug("Locale is null");
    }

    // Provide custom retry handler is necessary
    method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, 
      new DefaultHttpMethodRetryHandler(retries, false));

    // set timeout
    client.getHttpConnectionManager().
        getParams().setConnectionTimeout(timeout);


    /** use the authorization cookies */
    if (authCookies == null) {
      log.debug("No pre-set cookies found.");
    }
    else {
      log.debug("Using " + authCookies.length + " authorized cookies");
      for (int i = 0; i < authCookies.length; i++) {
        log.debug(authCookies[i].toExternalForm());
      }

      // Get initial state object
      HttpState initialState = new HttpState();
      initialState.addCookies(authCookies);
      client.setState(initialState);
    }


    try {
      // Execute the method.
      int statusCode = client.executeMethod(method);

      // log the status
      if (statusCode != HttpStatus.SC_OK) {
        log.error("Method failed: " + method.getStatusLine());
      }
      else {
        log.debug("Status code: " + method.getStatusLine());
      }

      // Read the response body.
      byte[] responseBody = method.getResponseBody();

      // Deal with the response.
      html = new String(responseBody);

    /** FIXME: move the catch handling somewhere else. */

/*    } catch (HttpException e) {
      log.error("Fatal protocol violation: " + e.getMessage());
      throw e;
      //e.printStackTrace();
    } catch (IOException e) {
      log.error("Fatal transport error: " + e.getMessage());
      throw e;
      //e.printStackTrace();*/
    } finally {
      // Release the connection.
      method.releaseConnection();
    }

    /** This class should not return self-generated HTML */

    return html;
  }

  /** overloaded variations */

  protected static String getWebPage( String url, Cookie[] authCookies ) 
  throws HttpException, IOException {
    return getWebPage( new java.net.URL(url), authCookies, null, null );
  }

  protected static String getWebPage( String url, Cookie[] authCookies, java.net.URL httpReferer, java.util.Locale locale )
  throws HttpException, IOException {
    return getWebPage( new java.net.URL(url), authCookies, httpReferer, locale );
  }

  private static String getWebPage( java.net.URL url, Cookie[] authCookies ) 
  throws HttpException, IOException {
    return getWebPage( url, authCookies, null, null );
  }



  /** POST */

  protected static String postForm(
    java.net.URL url, NameValuePair[] parametersBody, Cookie[] authCookies )
  throws HttpException, IOException {

    // the httpclient request response will be stored in this variable.
    String html = null;

    // Create an instance of HttpClient.
    HttpClient client = new HttpClient();

    // Create a method instance.
    log.debug("POST URL: " + url.toString());
    PostMethod method = new PostMethod(url.toString());
    method.setRequestBody( parametersBody );

    // Provide custom retry handler is necessary
    method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, 
      new DefaultHttpMethodRetryHandler(3, false));

    // set timeout
    client.getHttpConnectionManager().
        getParams().setConnectionTimeout(30000);


    /** use the authorization cookies */
    if (authCookies == null) {
      log.debug("No pre-set cookies found.");
    }
    else {
      log.debug("Using " + authCookies.length + " authorized cookies");
      for (int i = 0; i < authCookies.length; i++) {
        log.debug(authCookies[i].toExternalForm());
      }

      // Get initial state object
      HttpState initialState = new HttpState();
      initialState.addCookies(authCookies);
      client.setState(initialState);
    }


    try {
      // Execute the method.
      int statusCode = client.executeMethod(method);

      // log the status
      if ((statusCode != HttpStatus.SC_OK) && (statusCode != 302)) {
        log.error("Method failed: " + method.getStatusLine());
      }
      else {
        log.info("POST status code: " + method.getStatusLine());
      }

      // Read the response body.
      byte[] responseBody = method.getResponseBody();

      // Deal with the response.
      html = new String(responseBody);

    /** FIXME: This class should not return self-generated HTML */
    } catch (HttpException e) {
      log.error("Fatal protocol violation: " + e.getMessage());
      // due to a design mistake, this title is never processed to portlet HTML.
      html = "<html><head><title>Protocol violation error</title></head>";
      html += "<body>" + e.getMessage() + "</body></html>";
      //e.printStackTrace();
    } catch (IOException e) {
      log.error("Fatal transport error: " + e.getMessage());
      html = "<html><head><title>Transport error</title></head>";
      html += "<body>" + e.getMessage() + "</body></html>";
      //e.printStackTrace();
    } finally {
      // Release the connection.
      method.releaseConnection();
    }
    /** This class should not return self-generated HTML */

    return html;
  }


  /** GETs and fixes the Rails' HTTP response body */
  protected static String getRailsHTML( java.net.URL url, Cookie[] authCookies, java.net.URL httpReferer, java.util.Locale locale )
  throws HttpException, IOException {
    // TODO: instantiate a Request, inspect the headers, send proper data in the Request (loggable)
    String validHTML = "";

    // this is a simple method to get the page
    log.debug("GET URL: " + url.toString());
    try {
      String railsResponse = getWebPage(url,authCookies,httpReferer,locale);
      // TODO: validate HTML
      validHTML = railsResponse;
    }
    catch (Exception e) {
      log.error(e.getMessage());
      log.info("Failed to get valid response from " + url.toString());
      /** FIXME: This class should not return self-generated HTML */
      validHTML = "<html><body>" + e.getMessage() + "</body></html>";
    }

    return validHTML;
  }

  // without reference
  protected static String getRailsHTML( java.net.URL url, Cookie[] authCookies )
  throws HttpException, IOException {
    return getRailsHTML( url, authCookies, null, null );
  }



  /** POSTs the parametersBody, and fixes the Rails' HTTP response body */
  protected static String postActionRequest(
    java.net.URL url, NameValuePair[] parametersBody, Cookie[] cookies, java.net.URL httpReferer )
  throws HttpException, IOException {
    String validHTML = "";

    // this is a simple method to get the page
    log.debug("POST parametersBody to URL: " + url.toString());

    try {
      String railsResponse = postForm(url,parametersBody,cookies);
      // TODO: validate HTML
      validHTML = railsResponse;
    }
    catch (Exception e) {
      log.error(e.getMessage());
      validHTML = "<html><body>" + e.getMessage() + "</body></html>";
    }

    return validHTML;
  }

  // without reference
  protected static String postActionRequest(
    java.net.URL url, NameValuePair[] parametersBody, Cookie[] cookies )
  throws HttpException, IOException {
    return postActionRequest( url, parametersBody, cookies, null );
  }


  protected static Boolean serverIsAlive ( String host ) {
    // expect that the server is always online
    return true;
  }


  /** GETs innocent cookies */
  protected static Cookie[] getCookies(java.net.URL url)
  throws HttpException, Exception {
    Cookie[] cookies = null;

    HttpClient client = new HttpClient();
    client.getHostConfiguration().setHost(url.getHost(), url.getPort(), url.getProtocol());
    GetMethod cookget = new GetMethod(url.toString());
    int statusCode = -1;

    try {
      log.debug("Requesting cookies from: " + url.toString());
      statusCode = client.executeMethod(cookget);

    } catch (HttpException e) {
      log.error("Fatal protocol violation: " + e.getMessage());
      throw e;
//       e.printStackTrace();
    } catch (IOException e) {
      log.error("Fatal transport error: " + e.getMessage());
      throw e;
//       e.printStackTrace();
    } finally {
      log.debug(cookget.getStatusLine());
      // release any connection resources used by the method
      cookget.releaseConnection();
    }

    // Bail out if the status code is not OK (200)
    if (statusCode != HttpStatus.SC_OK) {
      log.warn("Got status code "+statusCode+" while requesting cookies from "+url.toString());
      return null;
    }

    // See if we got any cookies
    CookieSpec cookiespec = CookiePolicy.getDefaultSpec();
    cookies = client.getState().getCookies();

    log.debug("Received "+cookies.length+" cookies:");
    for (int i = 0; i < cookies.length; i++) {
      log.debug(cookies[i].toString());
    }

    // pick the desired cookie
    cookies = cookiespec.match(
      url.getHost(), url.getPort(), url.getPath(), false, cookies);

    if (cookies.length == 0) {
      log.debug("No matching cookies");
    } else {
      log.debug("Matched cookies:");
      for (int i = 0; i < cookies.length; i++) {
        log.debug(cookies[i].toString());
      }
    }

    return cookies;
  }

  protected static Cookie[] getCookies(String url)
  throws Exception {
    return getCookies(new java.net.URL(url));
  }


  private static void debugUrl(java.net.URL url) {
    System.out.println( "Protocol : " + url.getProtocol() );
    System.out.println( "Host     : " + url.getHost()     );
    System.out.println( "Port     : " + url.getPort() );
    System.out.println( "Path     : " + url.getPath() );
    System.out.println( "Query    : " + url.getQuery() );
    System.out.println( "Ref      : " + url.getRef() );
  }



}