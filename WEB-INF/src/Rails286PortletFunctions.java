/**
 * Copyright (c) 2008,2009 Mikael Lammentausta
 *               2010 Mikael Lammentausta, Túlio Ornelas dos Santos
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.portlet.PortletRequest;
import javax.portlet.RenderRequest;

import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
	Shared static functions.
	This class includes the only Liferay-specific functionality.
*/
public class Rails286PortletFunctions {

  private static final Log log = LogFactory.getLog(Rails286PortletFunctions.class);


  /** Transforms parameter Map to NameValuePair. */
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

    // cast Object[] => NameValuePair[]
    for (int x=0 ; x<n ; x++) {
      ret[x] = (NameValuePair)o[x];
    }

    // Check
    if (log.isDebugEnabled()) {
      log.debug(ret.length + " parameters: --------------------");
      for (int x=0 ; x<ret.length ; x++) {
        log.debug(ret[x].toString());
      }
      log.debug("----------------------------------");
    }
    
    return ret;
  }

	/** Is the supported Liferay version same or newer than the given parameter.

		For instance, if the running version is 5.2, and the argument is [5,1];
		 this returns true.

		Compares only up to two decimals (x.y).
	  */
	protected static Boolean isMinimumLiferayVersionMet(int[] version) {
		// bail out if major version is sufficient
		if (version[0] < PortletVersion.LIFERAY_VERSION[0]) {
			return false;
		}
		// no not check minor version if major is newer
		else if (version[0] > PortletVersion.LIFERAY_VERSION[0]) {
			return true;
		}

		for (int x=1; x < version.length; x++) {
			if (version[x] < PortletVersion.LIFERAY_VERSION[x]) {
				return false;
			}
		}
		return true;
	}

	/** Is the supported Liferay version same than the given parameter.

		Compares only two decimals (x.y).
	  */
	protected static Boolean isLiferayVersionEqual(int[] version) {
		if (
			(PortletVersion.LIFERAY_VERSION[0] == version[0]) &&
			(PortletVersion.LIFERAY_VERSION[1] == version[1])
		) {
			return true;
		}
		return false;
	}

  /** Processes the request path.
    * Replaces variables with runtime user/portlet values.
    */
  protected static String decipherPath( String path, RenderRequest _request )
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

//	log.debug("Compiled for Liferay version "+LIFERAY_VERSION[0]);

    for (int i=0; i<pathParameters.length; i++) {
      String var = pathParameters[i];
      //log.debug("Matching: "+var);
      pattern = Pattern.compile(var);
      matcher = pattern.matcher(path);

      try {
        /** UID *************************************/
        if (matcher.find()) {
          if (var.equals("%UID%")) {
            log.debug("Matched variable: "+var);
            log.warn("Formulating unsecure URL with UID .. plz fixme");
            String uid = null;

            try {
              //Map userInfo = (Map)request.getAttribute(PortletRequest.USER_INFO);

				// Liferay 5.2 +
				/* comment out version checking, since conditional compiling may not be nice. */
				/*if (isMinimumLiferayVersionMet(new int[] {5,2})) { */
					PortletRequest request = (PortletRequest)_request;
					uid = new Long(
						com.liferay.portal.util.PortalUtil.getUserId(request)
					).toString();
				/*}

				// Liferay 5.1.x
				else if (isLiferayVersionEqual(new int[] {5,1})) {
					//String old_uid = (userInfo != null) ? (String)userInfo.get("liferay.user.id") : "0";
					uid = new Long(
						com.liferay.portal.util.PortalUtil.getUserId(_request)
					).toString();
				}

				else {
					throw new NullPointerException("Cannot get "+var+" on this version of Liferay");
				}
				*/
            }
            catch (NullPointerException e) {
              log.error(e.getMessage());
              uid = "0";
            }
			log.debug("Liferay UID: " + uid);
            path = path.replaceAll(var,uid);
          } // UID

          /** Portlet's group ID **********************/
          if (var.equals("%GID%")) {
            log.debug("Matched variable: "+var);
            String gid = null;

            try {

				// Liferay 5 +
				/*if (isMinimumLiferayVersionMet(new int[] {5})) { */
					gid = new Long(
						com.liferay.portal.util.PortalUtil.getScopeGroupId(_request)
					).toString();
				/*}

				else {
					throw new NullPointerException("Cannot get "+var+" on this version of Liferay");
				}
    */
            }
            catch (NullPointerException e) {
              log.error(e.getMessage());
              gid = "0";
            }
			log.debug("Liferay portlet GID: "+gid);
            path = path.replaceAll(var,gid);
          } // GID
          // ------------------------------------------------------------
        } // matcher
      }
      catch (NullPointerException e) {
        log.error(e.getMessage());
      }
    } // end iterate

    //After replaced the runtime variables we need to clear the unused Rails wildcards
    path = clearRailsWildcards(path);
    
    log.debug("New path: " + path);
    return path;
  }
  
  /**
   * 
   * Clear unused Rails wildcards
   *
   **/
  private static String clearRailsWildcards(String path) {
	  log.debug("Original path (with Rails wildcards): " + path);

	  String newPath = path.replaceAll("(:[a-zA-Z]*/?)", "");
	  if (newPath.endsWith("/")){
		  newPath = newPath.substring(0, newPath.length() - 1); //cut the last char
	  }

	  log.debug("Path after cleanup (withour Rails wildcards): " + newPath);

	  return newPath;
  }
  
}
