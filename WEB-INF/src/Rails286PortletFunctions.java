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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.io.IOException;

import javax.portlet.PortletContext;
import javax.portlet.PortletException;
import javax.portlet.PortletRequest;
import javax.portlet.PortletSession;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.Cookie;

/** Liferay classes to get the portlet's group! */
import com.liferay.portal.util.PortalUtil;
// import com.liferay.portal.theme.ThemeDisplay;


public class Rails286PortletFunctions {

  private static final Log log = LogFactory.getLog(Rails286PortletFunctions.class);
  private static final Boolean LIFERAY_HACKS = true;


  /** Transform the parameter Map from RenderRequest to NameValuePair[] */
  protected static NameValuePair[] paramsToNameValuePairs(Map<String,String[]> params) {

    // create a new dynamic ArrayList
    List<NameValuePair> list = new ArrayList<NameValuePair>();

    // iterate the entrySet
    for (Iterator i = params.entrySet().iterator() ; i.hasNext() ;) {
      java.util.Map.Entry e = (java.util.Map.Entry)i.next();

      String   key    = (String)e.getKey();
      String[] values = (String[])e.getValue();

      // iterate the values and add them to the List
      for (int ii=0 ; ii<values.length ; ii++) {
        list.add(new NameValuePair(key,values[ii]));
      }
    }

    // get the Array representation
    Object[] o = list.toArray();
    int n = o.length;
    // create a fixed size NameValuePair Array
    NameValuePair[] ret = new NameValuePair[n];

    // Transform from Object[] => NameValuePair[]
    for (int x=0 ; x<n ; x++) {
      ret[x] = (NameValuePair)o[x];
    }

    // Check
    log.debug(ret.length + " parameters: --------------------");
    for (int x=0 ; x<ret.length ; x++) {
      log.debug(ret[x].toString());
    }
    log.debug("----------------------------------");

    return ret;
  }


  /** Processes the request path.
    * Replaces variables with runtime user/portlet values.
    */
  protected static String decipherPath( String path, RenderRequest request )
  {
    if (path == null) {
      log.debug("Path is null, cannot extract variables");
      return path;
    }

    //ThemeDisplay themeDisplay = (ThemeDisplay)request.getAttribute(WebKeys.THEME_DISPLAY);

    log.debug("Deciphering portlet directive variables from path: "+path);
    String[] pathParameters = { "%UID%", "%GID%" };
    Pattern pattern = null;
    Matcher matcher = null;

    for (int i=0; i<pathParameters.length; i++) {
      String var = pathParameters[i];
      log.debug("Matching: "+var);
      pattern = Pattern.compile(var);
      matcher = pattern.matcher(path);

      try {
        /** UID *************************************/
        if (matcher.find()) {
          if (var == "%UID%") {
            log.debug("Matched variable: "+var);
            String uid = null;

            try {
              //Map userInfo = (Map)request.getAttribute(PortletRequest.USER_INFO);

              if (LIFERAY_HACKS==true) {
                log.debug("Liferay hacks enabled");
                //String old_uid = (userInfo != null) ? (String)userInfo.get("liferay.user.id") : "0";
                uid = new Long(
                  com.liferay.portal.util.PortalUtil.getUserId(request)
                ).toString();
                log.debug("Liferay UID: " + uid);
              }
              else {
                log.debug("UID is only supported on Liferay");
                uid = "0";
              }
            }
            catch (NullPointerException e) {
              log.error(e.getMessage());
              uid = "0";
            }
            path = path.replaceAll(var,uid);
          } // UID

          /** Portlet's group ID **********************/
          if (var == "%GID%") {
            log.debug("Matched variable: "+var);
            String gid = null;

            try {
              if (LIFERAY_HACKS==true) {
                log.debug("Liferay hacks enabled");
                gid = new Long(
                  com.liferay.portal.util.PortalUtil.getPortletGroupId(request)
                ).toString();
                log.debug("Liferay portlet GID: "+gid);
              } // Liferay hacks
            }
            catch (NullPointerException e) {
              log.error(e.getMessage());
              gid = "0";
            }
            path = path.replaceAll(var,gid);
          } // GID
          // ------------------------------------------------------------
        } // matcher
      }
      catch (NullPointerException e) {
        log.error(e.getMessage());
      }
    } // end iterate
    log.debug("New path: " + path);
    return path;
  }



  private void doDebug(RenderRequest request, RenderResponse response)
  throws PortletException, IOException {

    String session_id = request.getRequestedSessionId();
    PortletSession session = request.getPortletSession(true);
    // http://www.bluesunrise.com/portlet-api/javax/portlet/PortletContext.html
    PortletContext cx = session.getPortletContext();
  
    //  for (Enumeration attributes = session.getAttributeNames() ; attributes.hasMoreElements() ;) {
    //     String attr = (String)attributes.nextElement();
    //     outputHTML += attr;
    //     log.info( attr );
    //   }
    
    //   java.net.URL u = cx.getResource("/");
    //   log.info( u.toString() );
    //
    //   pu.setParameter("url","/debug/req");
    //   log.info( pu.toString() );
    
    
    //log.info(  );
    
    //response.setContentType("text/html");
    //PortletRequestDispatcher dispatcher = getPortletContext().getRequestDispatcher(debugUrl);
    //dispatcher.include(request,response);
  }
  


}
