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

import java.io.IOException;
/*
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.cookie.CookieSpec;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.NameValuePair;
// import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
*/

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class SessionAuthenticator {

  private final Log log = LogFactory.getLog(getClass().getName());

    public static void debugCookie()
    {
    }

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

}