/**
 * Copyright (c) 2008 - 2011 Mikael Lammentausta
 *               2010 Tulio Ornelas dos Santos
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.portlet.PortletRequest;

import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.util.PropsUtil;


/**
	Shared static functions.
	This class includes the only Liferay-specific functionality.
 */
public class Rails286PortletFunctions {

	private static final Log log = LogFactory.getLog(Rails286PortletFunctions.class);
	private static String tempPath;
	
	static {
		// Initializing singleton tempPath
		getTempPath();
	}
	
	/**
	 * The getTempPath method will search for LIFERAY_PORTAL_TEMP variable on environment,
	 * if the method not find our variable the catalina.base property will be used. The last attempt is
	 * the path '../temp' that will work if you have started the server inside the bin folder.
	 */
	public static String getTempPath(){
		if (tempPath != null) {
			log.debug("TempPath: " + tempPath);
			return tempPath;
		}
		
		tempPath = System.getenv("LIFERAY_PORTAL_TEMP");
		if (tempPath != null && tempPath.length() > 0){
			tempPath = tempPath.replaceAll("/\\Z", "");
			log.info("Using LIFERAY_PORTAL_TEMP variable for tempPath, defined path: " + tempPath);
			return tempPath;
		}
		
		// In case of catalina (tomcat)
		try {
			String catalinaDir = PropsUtil.get("catalina.base");
			if (catalinaDir != null && catalinaDir.length() > 0){
				catalinaDir = catalinaDir.replaceAll("/\\Z", "");
				tempPath = catalinaDir + "/temp";
				log.info("Using catalina.base for tempPath, defined path: " + tempPath);
				return tempPath;
			}
		} catch (Exception e) {
			log.error(e.getMessage());
		}
		
		// default one
		// XXX: maybe use /tmp ?
		tempPath = "../temp";
		log.info("Using '../temp' as tempPath, this is a not very good aproach. Be aware");
		return tempPath;
	}
	
	public static java.net.URL getRequestURL(java.net.URL railsBaseURL, String servlet, String route){
		try {
			RouteAnalyzer routeAnalyzer = new RouteAnalyzer(railsBaseURL, servlet);
			URL requestURL = routeAnalyzer.getFullURL(route);
			log.debug("Request URL: "+requestURL.toString());
			return requestURL;

		} catch (java.net.MalformedURLException e) {
			log.error("getRequestURL: " + e.getMessage());
		}
		return null;
	}

	/** Process the request parameters.
	 * These are set in BodyTagVisitor.
	 * The returned parameters are "x-www-form-urlencoded" decoded.
	 */
	@SuppressWarnings("rawtypes")
	public static Map<String,String[]> mapRequestParameters(PortletRequest request) {
		/** Proprietary _encoding_ hack.
		 * Caterpillar will be able to set a hidden form parameter
		 * "_encoding_", by JavaScript, if the browser is IE.
		 */
		String encoding = request.getParameter("_encoding_");
		if (encoding != null) {
			log.debug("Encoding: "+encoding);
		}
		else {
			encoding = "UTF-8";
		}
		//log.debug(request.getProperty("Accept-Encoding"));
		//log.debug(request.getProperty("Accept-Charset"));

		Map<String,String[]> params = new HashMap<String,String[]>();

		Iterator i = request.getParameterMap().entrySet().iterator();
		while(i.hasNext()){
			Map.Entry entry = (Map.Entry)i.next();

			/** IE hack.
			 * If the page charset is UTF8, but the form accept-encoding is ISO-8859-x,
			 * IE sends CP1252 encoded data.
			 *
			 * @see http://www.alanflavell.org.uk/charset/form-i18n.html
			 */
			if (encoding.equals("CP1252")) {
				//log.debug("IE hack activated");
				String[] values = (String[])entry.getValue();
				for (int x=0; x<values.length ; x++) {
					String param = values[x];
					log.debug("Converting "+entry.getKey()+": "+param);
					values[x] = fromMiscoded1252toUnicode(param);
					log.debug(entry.getKey() + ": " + values[x] );
				}
				params.put((String)entry.getKey(), values);
			}
			else {
				params.put((String)entry.getKey(), (String[])entry.getValue());
			}
		}
		return params;
	}

	/** 
	 * Transforms parameter Map to NameValuePair. 
	 */
	@SuppressWarnings("rawtypes")
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
	 *
	 * DEPRECATED since version 0.12.
	 * The correct way to pass this information is via cookie.
	 */
	protected static String decipherPath(String path, PortletRequest request)
	{
		String deprecationMsg = "DEPRECATION WARNING: You are using a deprecated method for passing Liferay UID/GID values to Rails in URL. This data is now passed in a session cookie. Please do not use :uid or :gid in your routes.";

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
			pattern = Pattern.compile(var);
			matcher = pattern.matcher(path);

			try {
				if (matcher.find()) {

					/*
					 * UID - User ID
					 */
					if (var.equals("%UID%")) {
						log.warn(deprecationMsg);
						log.debug("Matched variable: "+var);
						String uid = null;

						try {
							//Map userInfo = (Map)request.getAttribute(PortletRequest.USER_INFO);

							// Liferay 5.2 +
							/* comment out version checking, since conditional compiling may not be nice. */
							/*if (isMinimumLiferayVersionMet(new int[] {5,2})) { */
							uid = new Long(
									com.liferay.portal.util.PortalUtil.getUserId(request)
							).toString();
						}
						catch (NullPointerException e) {
							log.error(e.getMessage());
							uid = "0";
						}
						log.debug("Liferay UID: " + uid);
						path = path.replaceAll(var,uid);
					}

					/*
					 * GID - Group ID
					 */
					if (var.equals("%GID%")) {
						log.warn(deprecationMsg);
						log.debug("Matched variable: "+var);
						String gid = null;

						try {
							
							long scopeGroupId = 0;
							try {
								scopeGroupId = com.liferay.portal.util.PortalUtil.getScopeGroupId(request);
							/*
							 exception com.liferay.portal.kernel.exception.PortalException is never thrown in body of corresponding try statement
							 exception com.liferay.portal.kernel.exception.SystemException is never thrown in body of corresponding try statement
							} catch (PortalException e) {
								log.error("decipherPath: " + e.getMessage());
							} catch (SystemException e) {
								log.error("decipherPath: " + e.getMessage());
							}
							*/
							} catch (Exception e) {
								log.error("decipherPath: " + e.getMessage());
							}
							
							// Liferay 5 +
							/*if (isMinimumLiferayVersionMet(new int[] {5})) { */
							gid = new Long(scopeGroupId).toString();
						}
						catch (NullPointerException e) {
							log.error(e.getMessage());
							gid = "0";
						}
						log.debug("Liferay portlet GID: "+gid);
						path = path.replaceAll(var,gid);
					}
				}

			} catch (NullPointerException e) {
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
	 * Clear unused Rails wildcards.
	 *
	 * DEPRECATED since 0.12 with decipherPath().
	 *
	 **/
	private static String clearRailsWildcards(String path) {
		log.warn("DEPRECATION WARNING: custom path parameters are deprecated");
		log.debug("Original path (with Rails wildcards): " + path);

		String newPath = path.replaceAll("(:[a-zA-Z]*/?)", "");
		if (newPath.endsWith("/")){
			newPath = newPath.substring(0, newPath.length() - 1); //cut the last char
		}
		
		// Since the portlet filter may now call this function with a full route,
		// this breaks the "http://" to "http/".
		// Custom path parameters are deprecated, so this is fixed with a hack:
		newPath = newPath.replaceAll("http/", "http://");

		log.debug("Path after cleanup (without Rails wildcards): " + newPath);

		return newPath;
	}

	/**
	 * Fix broken CP1252 encoding, sent by IE if form post charset is
	 * set to ISO-8859-1.
	 */
	public static String fromMiscoded1252toUnicode(String cp1252)
	{
		try {
			byte[] b = cp1252.getBytes("windows-1252");
			return new String(b, "UTF-8");
		} catch (Exception e)
		{
			log.error(e);
			return null;
		}
	}

}
