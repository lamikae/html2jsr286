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

import java.io.IOException;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.cookie.CookieSpec;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.NameValuePair;
// import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class SessionAuthenticator {

  private final Log log = LogFactory.getLog(getClass().getName());

  private String LOGON_SITE     = null;
  private int    LOGON_PORT     = 80;
  private String LOGON_PROTOCOL = "http";
  private String LOGON_ACTION   = null;

  // path to get the initial cookie
//   String loginForm   = "/login";



  SessionAuthenticator(java.net.URL logonUrl) {
    super();

    LOGON_PROTOCOL = logonUrl.getProtocol();
    LOGON_SITE     = logonUrl.getHost();
    LOGON_PORT     = logonUrl.getPort();
    LOGON_ACTION   = logonUrl.getPath();
  }


  protected Cookie[] sendLoginCredentials(NameValuePair[] loginCredentials) 
  throws Exception {


    HttpClient client = new HttpClient();
    client.getHostConfiguration().setHost(LOGON_SITE, LOGON_PORT, LOGON_PROTOCOL);
//     GetMethod authget = new GetMethod(loginForm);

    Cookie[] logoncookies = null;
    int statusCode = -1;



      /** POST the credentials */

      PostMethod authpost = new PostMethod(LOGON_ACTION);
      authpost.setRequestBody( loginCredentials );
  
      try {
        log.debug("POSTing the login credentials to: " + LOGON_ACTION);
        client.executeMethod(authpost);
  
      } catch (HttpException e) {
        log.error("Fatal protocol violation: " + e.getMessage());
        e.printStackTrace();
      } catch (IOException e) {
        log.error("Fatal transport error: " + e.getMessage());
        e.printStackTrace();
      } finally {
        log.debug("Login form post: " + authpost.getStatusLine().toString());
        // release any connection resources used by the method
        authpost.releaseConnection();
      }
  
  
  
      // See if we got any cookies
      // The only way of telling whether logon succeeded is 
      // by finding a session cookie
      CookieSpec cookiespec = CookiePolicy.getDefaultSpec();
      logoncookies = cookiespec.match(
        LOGON_SITE, LOGON_PORT, "/", false, client.getState().getCookies());

      if (logoncookies.length == 0) {
        log.warn("No authorized cookies");
      } else {
        log.debug("Authorized cookies:");
        for (int i = 0; i < logoncookies.length; i++) {
          log.debug(logoncookies[i].toString());
        }
      }
//     } // statusCode ? 200


    // Usually a successful form-based login results in a redirect to another url
//     int statuscode = authpost.getStatusCode();
//     if ((statuscode == HttpStatus.SC_MOVED_TEMPORARILY) ||
//         (statuscode == HttpStatus.SC_MOVED_PERMANENTLY) ||
//         (statuscode == HttpStatus.SC_SEE_OTHER) ||
//         (statuscode == HttpStatus.SC_TEMPORARY_REDIRECT)) {
//         Header header = authpost.getResponseHeader("location");
//         if (header != null) {
//             String newuri = header.getValue();
//             if ((newuri == null) || (newuri.equals(""))) {
//                 newuri = "/";
//             }
//             System.out.println("Redirect target: " + newuri); 
//             GetMethod redirect = new GetMethod(newuri);
// 
//             client.executeMethod(redirect);
//             System.out.println("Redirect: " + redirect.getStatusLine().toString()); 
//             // release any connection resources used by the method
//             redirect.releaseConnection();
//         } else {
//             System.out.println("Invalid redirect");
//             System.exit(1);
//         }
//     }

    return logoncookies;

  }


}