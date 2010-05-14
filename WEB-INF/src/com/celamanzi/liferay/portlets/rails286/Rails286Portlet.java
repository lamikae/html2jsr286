/**
 * Copyright (c) 2008,2009 Mikael Lammentausta
 *               2010 Mikael Lammentausta, Tulio Ornelas dos Santos
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.GenericPortlet;
import javax.portlet.PortletConfig;
import javax.portlet.PortletException;
import javax.portlet.PortletMode;
import javax.portlet.PortletPreferences;
import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;
import javax.portlet.PortletSession;
import javax.portlet.ReadOnlyException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;
import javax.portlet.ValidatorException;

import org.apache.commons.fileupload.portlet.PortletFileUpload;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.htmlparser.util.ParserException;

import com.liferay.portal.kernel.upload.UploadPortletRequest;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.util.PortalUtil;
import com.liferay.util.servlet.PortletResponseUtil;


/**
 * Presents a web application in a JSR286 portlet.
 * Rails is the only tested framework, but the same principles apply to all HTTP servers.
 *
 * Supports the GET and POST methods (also unofficially PUT).
 * Session cookies are supported.
 * TODO: explain cookie system and security
 *
 * XHRs do not convey through the portlet. They should be directed to the Rails server in the HTML.
 * This presents a problem that the returned HTML is not processed anymore, and cannot have any PortletURLs.
 * Maybe XHR could be sent to the portal, which would trigger XMLPortletRequest,
 * then the returned HTML could be passed through portlet transformation.
 *
 * http://www.subbu.org/blog/2007/08/update-on-jsr-286-and-ajax 
 *
 * JSR-286 introduces a new URL type ResourceURL that can be used to invoke 
 * the serveResource method of a portlet.
 * Since serveResource mimics the service method of the servlet API,
 * portlets can use XMLHttpRequest with a ResourceURL,
 * generate some textual or XML response, and process it in the browser.
 *
 * http://ibmdw.blogspot.com/2009/04/jsr-268-portlets-standard-portlet-and.html
 * http://www.ibm.com/developerworks/websphere/library/techarticles/0803_hepper/0803_hepper.html
 *
 * Implemented @since 0.10.0
 *
 * @author Mikael Lammentausta
 */
public class Rails286Portlet extends GenericPortlet implements PreferencesAttributes, PublicRenderParameterAttributes {

	/** 
	 * Class variables and the logger.
	 */
	private final Log log = LogFactory.getLog(getClass().getName());

	private String sessionKey    = null;
	private String sessionSecret = null;
	protected int responseStatusCode = -1;

	/** 
	 * Base + Request URLs are set in the RenderFilter 
	 * and are read from the PortletSession.
	 */
	private URL      railsBaseUrl  = null;
	private String   servlet       = null;
	private String   railsRoute    = null;

	private OnlineClient client  = null;

	/** 
	 * Portlet initialization at portal startup.
	 */
	@Override
	public void init(PortletConfig config) throws PortletException {
		log.info(
				"Initializing Rails-portlet "+config.getPortletName()+
				" (version "+PortletVersion.PORTLET_VERSION+")"
		);

		// store session secret to private instance variable
		sessionKey    = config.getInitParameter("session_key");
		sessionSecret = config.getInitParameter("secret");

		if (sessionSecret == null) {
			log.info("Session security not established");
		} else {
			log.info("Session is secured by a shared secret");
			log.debug("Session key: "+sessionKey);
		}

		super.init(config);
	}

	/**
	 * Main method, render(). Other portlet modes are not supported.
	 *
	 * Downloads the Rails HTML, runs the HTML processor and
	 * inserts it into RenderResponse.
	 * 
	 * @since 0.10.0
	 *  It uses the callRails method
	 *  
	 * @since 0.8.2
	 * 	Split to subfunctions, cookie system...
	 *
	 * @since 0.6.1
	 *  Changed to use Liferay 5.2.0 API.
	 *
	 */
	public void render(RenderRequest request, RenderResponse response)
	throws PortletException, IOException {

		loggerInfo(request, response); 

		byte[] railsBytes = callRails(request, response);
		String outputHTML = processResponseBody(response, new String(railsBytes));

		log.debug("Response status code: " + responseStatusCode);

		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
		out.println(outputHTML);
	}

	/**
	 * Used to download files
	 */
	@Override
	public void serveResource(ResourceRequest request, ResourceResponse response)
	throws PortletException, IOException {
		super.serveResource(request, response);

		log.debug("serveResource has been called");

		byte[] railsBytes = callRails(request, response);
		String filename = getFilename();

		if (!filename.equals("")) {
			File file = new File("../temp/" + filename);

			FileOutputStream fos = new FileOutputStream(file);
			fos.write(railsBytes);

			fos.flush();
			fos.close();

			// This if is to avoid this call when in a test environment, because liferay test environment is too
			// heavy and dirty to be implemented =[ (sorry...)
			if (FileUtil.getFile() != null) {
				PortletResponseUtil.sendFile(response, filename, new FileInputStream(file));
			}
		}
	}

	/** 
	 * Handles POST requests.
	 */
	public void processAction(ActionRequest request, ActionResponse response)
	throws PortletException, IOException {

		// In case of a multipart request, retrieve the files
		if (PortletFileUpload.isMultipartContent(request)){
			log.debug("Multipart request");
			retrieveFiles(request);
		}

		log.debug("Received ActionRequest from the web page.");

		/** 
		 * Process the request parameters.
		 * These are set in BodyTagVisitor.
		 * The returned parameters "x-www-form-urlencoded" are decoded.
		 */
		Map<String,String[]> p = new HashMap<String,String[]>(request.getParameterMap());
		// create a clone of the parameter Map
		Map<String,String[]> params = new HashMap<String,String[]>(p);

		/** 
		 * Process an action from the web page.
		 * This can be a classic HTML form or a JavaScript-generator form POST.
		 */
		// default to POST for actions
		String actionMethod = (params.containsKey("originalActionMethod") ? 
				params.remove("originalActionMethod")[0] : "post"
		);

		String actionUrl    = null;

		// set the action URL
		if (params.containsKey("originalActionUrl")) {
			actionUrl = params.remove("originalActionUrl")[0];
			log.debug("Received a form action: " + actionUrl);

			// formulate NameValuePair[]
			NameValuePair[] parametersBody = Rails286PortletFunctions.paramsToNameValuePairs(params);
			debugParams(parametersBody);

			// Retrieving preference parameters and store them on liferay
			if(request.getPortletMode().equals(PortletMode.EDIT)){
				savePreferences(request, parametersBody);
			}
			
			// Sending public render parameters
			publishRenderParameters(response, parametersBody);

			// save the attributes to the RenderRequest (set custom parameters now..)
			request.setAttribute("parametersBody", parametersBody);

		} else {
			log.warn("No action URL given! Halting action.");
			actionUrl = (String) request.getParameter("railsRoute");
			actionMethod = "get";
		}

		request.setAttribute("requestMethod",actionMethod);
		request.setAttribute("railsRoute",actionUrl);
	}

	public Map<String, String[]> getContainerRuntimeOptions() {
		return this.getPortletConfig().getContainerRuntimeOptions();
	}

	public URL getRailsBaseUrl() {
		return railsBaseUrl;
	}

	public void setRailsBaseUrl(URL railsBaseUrl) {
		this.railsBaseUrl = railsBaseUrl;
	}

	public void setRailsBaseUrl(PortletSession session) {
		setRailsBaseUrl((java.net.URL)session.getAttribute("railsBaseUrl"));
	}

	public String getServlet() {
		return servlet;
	}

	public void setServlet(String servlet) {
		this.servlet = servlet;
	}

	public void setServlet(PortletSession session) {
		setServlet((String)session.getAttribute("servlet"));
	}

	public String getRailsRoute() {
		return railsRoute;
	}

	public void setRailsRoute(String railsRoute) {
		this.railsRoute = railsRoute;
	}

	public void setRailsRoute(PortletSession session) {
		setRailsRoute((String)session.getAttribute("railsRoute"));
	}

	public OnlineClient getClient() {
		return client;
	}

	public void setClient(OnlineClient client) {
		this.client = client;
	}

	/**
	 *	Cookie with session secret.
	 */
	protected Cookie secretCookie(PortletSession session) {
		// no. this is not the best way to handle this.
		URL base = (java.net.URL)session.getAttribute("railsBaseUrl");
		String host = base.getHost();
		return new Cookie(
				host,
				"session_secret",
				sessionSecret, // instance variable
				"/",
				null,
				false);
	}

	/**
	 * Cookie with UID.
	 */
	protected Cookie uidCookie(PortletSession session) {
		// no. this is not the best way to handle this.
		URL base = (java.net.URL)session.getAttribute("railsBaseUrl");
		String host = base.getHost();
		return new Cookie(
				host,
				"Liferay_UID",
				(String) session.getAttribute("uid"),
				"/",
				null,
				false);
	}

	protected Cookie resourceUrlCookie(PortletSession session, String value){
		URL base = (java.net.URL) session.getAttribute("railsBaseUrl");
		String host = base.getHost();

		// Adding the resource url generated by liferay to achieve the serveResource method
		return new Cookie(
				host,
				"Liferay_resourceUrl",
				value,
				"/",
				null,
				false);
	}
	
	/**
	 * Creates the preference cookie (Liferay_preferences) with the values of request.getPreferences().
	 * It will encode the generated value (key=value;key=value;...) with UTF-8.
	 * 
	 * @see Rails286Porltlet#preferencesToString
	 * 
	 * @param session - {@link PortletSession}
	 * @param request - {@link PortletRequest}
	 * @return {@link Cookie}
	 */
	private Cookie preferencesCookie(PortletSession session, PortletRequest request){
		URL base = (java.net.URL) session.getAttribute("railsBaseUrl");
		String host = base.getHost();

		String value = preferencesToString(request.getPreferences());
		try {
			// Cookies do not accept space, brackets, parentheses, equals signs, commas, and a lot of chars, so the only
			// solution was encoding the content. UTF-8 was suggested as the better character encoding for this, 
			// but if something goes wrong we will send the raw value.
			value = URLEncoder.encode(value, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			log.error("preferencesCookie: " + e.getMessage());
		}

		return new Cookie(
				host,
				"Liferay_preferences",
				value,
				"/",
				null,
				false);
	}

	private byte[] callRails(PortletRequest request, PortletResponse response) throws PortletException{

		/**
		 * Session storage.
		 *
		 * The current host, servlet and route are stored into the session.
		 * The session is manipulated in the Render Filter.
		 *
		 * APPLICATION_SCOPE stores data across all user portlets in the same session.
		 * PORTLET_SCOPE stores information only accessible to the user's RailsPortlet instance.
		 *
		 * @see javax.portlet.PortletSession
		 * http://www.bluesunrise.com/portlet-api/javax/portlet/PortletSession.html
		 *
		 */
		PortletSession session = request.getPortletSession(true);

		/**
		 * Host and route.
		 *
		 * Gets the base URL and the route from the session.
		 */
		setRailsBaseUrl(session);
		setRailsRoute(session);
		setServlet(session);

		String railsHost = getRailsBaseUrl().getHost();

		byte[] railsBytes = new byte[]{};

		// check the server and route
		if (railsHost == null) {
			throw new PortletException("The host is undefined!");
		}
		if (getRailsRoute() == null) {
			log.warn( "The requested route is undefined" );
			setRailsRoute("/");
		}
		
		if (request.getPortletMode().equals(PortletMode.EDIT)){ 
			log.debug("Edit mode, defining preferences URL");
			definePreferencesURL();
			log.debug("RailsRoute: " + getRailsRoute());
		}
		
		// TODO: if the server is unreachable
		//if () {
		//  throw new PortletException("The server " + railsHost + " was unreachable.");
		//}

		try {
			java.net.URL requestUrl = getRequestURL();
			Map<String,Cookie> cookies = getCookies(session,request);

			// Retrieve servlet cookies.
			// Author Reinaldo Silva
			// Needs tests!
			/*
	      Cookie[] servletCookies = OnlineUtils.getRequestCookies(request, requestUrl);
	      for (int i = 0; i < servletCookies.length; i++) {
	        if (!cookies.containsKey(servletCookies[i].getName()))
	          cookies.put((String)servletCookies[i].getName(), (Cookie)servletCookies[i]);
	      }
			 */

			String requestMethod = getRequestMethod(session);
			URL httpReferer = getHttpReferer(session);
			java.util.Locale locale = request.getLocale();

			/**
			 * Execute the request
			 */
			setClient(new OnlineClient(requestUrl,cookies,httpReferer,locale));

			/**
			 * GET
			 */
			if (requestMethod.equals("get")) {			
				railsBytes = executeGet(session, getClient());
			}

			/**
			 * POST, PUT (PUT is sent as POST)
			 */
			else if (requestMethod.equals("post") || requestMethod.equals("put")) {
				railsBytes = executePost(request, httpReferer, session, getClient());
			}

			// DELETE?
			else {
				throw new PortletException("Unknown request method: "+requestMethod);
			}

		} catch(HttpException e) {
			log.error("callRails: HttpException: " + e.getMessage() + "\n" + new String(railsBytes));
			railsBytes = e.getMessage().getBytes();
		} catch (IOException e) {
			log.error("callRails: IOException: " + e.getMessage() + "\n" + new String(railsBytes));
			railsBytes = e.getMessage().getBytes();
		}

		// set the response status code (for tests)
		responseStatusCode = client.getStatusCode();
		return railsBytes;
	}

	/**
	 * Gets parameters with {@link PreferencesAttributes}#PREFERENCES_SUFIX from the request and saves.
	 * @param request
	 * @param parametersBody
	 * @throws IOException
	 */
	private void savePreferences(PortletRequest request, NameValuePair[] parametersBody) throws IOException {
		PortletPreferences preferences = request.getPreferences();

		for (NameValuePair nameValue : parametersBody) {
			if (nameValue.getName().endsWith(PREFERENCES_SUFIX)){
				try {
					preferences.setValue(nameValue.getName(), nameValue.getValue());
				} catch (ReadOnlyException e) {
					log.error("savePreferences: " + e.getMessage());
				}
			}
		}

		try {
			preferences.store();
		} catch (ValidatorException e) {
			log.error("savePreferences: " + e.getMessage());
		}
	}
	
	/**
	 * TODO: Javadoc
	 * @param response - {@link ActionResponse}
	 * @param parametersBody - {@link NameValuePair[]}
	 */
	private void publishRenderParameters(ActionResponse response, NameValuePair[] parametersBody) {
		for(NameValuePair param : parametersBody) {
			if(param.getName().endsWith(PUBLIC_RENDER_PARAMETER_SUFIX)) {
				String name = param.getName().replaceAll(PUBLIC_RENDER_PARAMETER_SUFIX, "");
				response.setRenderParameter(name, param.getValue());
			}
		}
	}

	/**
	 * Changes the railsRoute attribute to ensure that the preference method will be called.
	 */
	private void definePreferencesURL() {
		Pattern pattern = Pattern.compile("^/[^/]*/[^/]*");
		Matcher matcher = pattern.matcher(getRailsRoute());

		String route = getRailsRoute();
		if (matcher.matches()){
			String[] routes = getRailsRoute().split("/");

			route = "/"+ routes[1];
		}

		setRailsRoute(route+"/"+PREFERENCES_METHOD);
	}

	private java.net.URL getRequestURL(){
		return Rails286PortletFunctions.getRequestURL(getRailsBaseUrl(), getServlet(), getRailsRoute());
	}

	/**
	 * Retrieve the filename of contentDisposition. Return empty string ("")
	 * if the matcher didn't find the group (filename=\"([^\"]+)\").
	 * 
	 * @param contentDisposition - {@link String}
	 * @return {@link String}
	 */
	private String getFilename() {
		Pattern p = Pattern.compile("filename=\"([^\"]+)\"");
		Matcher matcher = p.matcher(getClient().getContentDisposition());
		return matcher.find() ? matcher.group(1) : "";
	}

	/** 
	 * Executes a POST request.
	 */
	@SuppressWarnings("unchecked")
	private byte[] executePost(PortletRequest request, URL httpReferer, PortletSession session, OnlineClient client) 
	throws HttpException,IOException {

		// retrieve POST parameters        
		NameValuePair[] parametersBody = (NameValuePair[]) request.getAttribute("parametersBody");

		if (parametersBody == null) {
			log.warn("Empty parametersBody in the request, no POST data?");

		} else if (log.isDebugEnabled()) {
			debugParams(parametersBody);
		}

		// retrieve files
		Map<String, Object[]> files = (Map<String, Object[]>) request.getAttribute("files");

		// POST the parametersBody
		// OnlineClient handles cases where POST redirects.
		byte[] railsBytes = client.post(parametersBody, files);

		// store new cookies into PortletSession.
		session.setAttribute("cookies", client.getCookies(), PortletSession.PORTLET_SCOPE);

		// do not leave the POST method hanging around in the session.
		session.setAttribute("requestMethod", "get", PortletSession.PORTLET_SCOPE);

		// ???
		if (session.getAttribute("httpReferer") != null) {
			log.debug("Saving route from httpReferer: "+httpReferer.toString());
			session.setAttribute("railsRoute", httpReferer.toString(), PortletSession.PORTLET_SCOPE);
		}

		return railsBytes; 
	}

	/**
	 *  Executes a GET request.
	 */
	private byte[] executeGet(PortletSession session, OnlineClient client)
	throws HttpException, IOException {

		// TODO: get responseHeader and responseBody

		// save/update client Cookie[] into cookies HashMap
		/*
		for (Cookie c : client.cookies)
		  cookies.put((String)c.getName(), (Cookie)c);
		 */

		// should servlet cookies be stored to the session? here they are not.

		byte[] railsBytes = client.get();
		session.setAttribute("cookies", client.getCookies(), PortletSession.PORTLET_SCOPE);

		return railsBytes;
	}

	/**
	 * Create the {@link PageProcessor} and process the railsRoute.
	 * 
	 * @param response - {@link PortletResponse}
	 * @param railsResponse - {@link String}
	 * @return outputHTML - {@link String}
	 */
	private String processResponseBody(RenderResponse response, String railsResponse) {

		String outputHTML = "";
		if ( (railsResponse != null ) && (railsResponse.length() > 1) ) {
			try {
				// PageProcessor => HeadProcessor, BodyTagVisitor (uses RouteAnalyzer)
				PageProcessor p = new PageProcessor(railsResponse, getServlet(), response);
				outputHTML   = p.process(getRailsBaseUrl(), getRailsRoute());

				/** Set the portlet title by HTML title */
				String title = p.getTitle();
				log.debug("Title: "+title);

				if (title == null || title.length() == 0) {
					response.setTitle("&nbsp;"); // nbsp, because Liferay post-processes blank strings

				} else { 
					response.setTitle( title ); 
				}
			} catch (ParserException e) {
				log.error(e.getMessage());

			} catch (IllegalStateException e) {
				log.error(e.getMessage());
			}
		}
		return outputHTML;
	}

	/**
	 * Select the request method (GET or POST).
	 *
	 * Use requestMethod if defined, otherwise use GET.
	 */
	private String getRequestMethod(PortletSession session) {
		String requestMethod = session.getAttribute("requestMethod") == null ? "get" : (String) session.getAttribute("requestMethod");
		log.debug("Request method: "+requestMethod);
		return requestMethod;
	}

	private URL getHttpReferer(PortletSession session) {
		if (session.getAttribute("httpReferer") != null) {
			return (java.net.URL) session.getAttribute("httpReferer");
		}
		return null; // otherwise
	}

	/** Cookie handling.
	 *
	 * If session cookies were found in the PortletSession,
	 * use them later with OnlineClient.
	 *
	 * Cookies are never explicitly removed from PortletSession.
	 *
	 * In the PortletSession, the cookies are stored in Cookie[].
	 * In this method they are handled in HashMap.
	 *
	 */
	private Map<String, Cookie> getCookies(PortletSession session, PortletRequest request) {
		Map<String, Cookie> cookies = new HashMap<String, Cookie>();
		// get cookies from PortletSession to HashMap
		if (session.getAttribute("cookies") != null) {
			log.debug("Stored session cookies found");
			for (Cookie cookie : (Cookie[])session.getAttribute("cookies")) {
				cookies.put((String)cookie.getName(), cookie);
			}
		}

		// Add dynamic session cookies.
		// These should not be saved to the database and the portlet session.

		// Create the secret cookie.
		// This is a weak symmetry-key algorithm.
		// Security could be boosted by using private and public key pairs.
		// http://en.wikipedia.org/wiki/Public-key_cryptography
		if (sessionSecret != null) {
			Cookie _secretCookie = secretCookie(session);
			cookies.put((String)_secretCookie.getName(), _secretCookie);
		}

		// UID cookie
		if (session.getAttribute("uid") != null) {
			Cookie _uidCookie = uidCookie(session);
			cookies.put((String)_uidCookie.getName(), _uidCookie);
		}

		// ResourceURL cookie
		String resourceUrlValue = (String) session.getAttribute("resourceURL");
		if (resourceUrlValue != null) {
			Cookie resourceUrlCookie = resourceUrlCookie(session, resourceUrlValue);
			cookies.put(resourceUrlCookie.getName(), resourceUrlCookie);
		}

		// Stores preferences in a cookie
		Cookie preferencesCookie = preferencesCookie(session, request);
		cookies.put(preferencesCookie.getName(), preferencesCookie);

		log.debug(cookies.size() + " cookies");

		return cookies;
	}

	/**
	 * Generates a string with all the preferences values, using the patter:
	 * key=value;key=value; and so on.
	 * 
	 * @param portletPreferences - {@link PortletPreferences}
	 * @return {@link String}
	 */
	private String preferencesToString(PortletPreferences portletPreferences){
		Enumeration<String> names = portletPreferences.getNames();

		StringBuilder builder = new StringBuilder();

		while (names.hasMoreElements()) {
			String key = (String) names.nextElement();
			builder.append(key+"="+portletPreferences.getValue(key, null)+";");
		}

		return builder.toString();
	}
	
	private void retrieveFiles(ActionRequest request) throws IOException {
		UploadPortletRequest uploadRequest = PortalUtil.getUploadPortletRequest(request);
		Map<String, Object[]> files = new HashMap<String, Object[]>();

		// Try to discover the file parameters
		for (Object key : uploadRequest.getParameterMap().keySet()){
			File file = uploadRequest.getFile(key.toString());

			if (file != null && file.length() > 0){
				// I need to store the bytes because the file will be deleted
				// after the method execution, so when OnlineClient request the
				// file it no longer exists. I need to store the file object
				// to store the path and the attributes of this file.
				byte[] bytes = FileUtil.getBytes(file);
				files.put(key.toString(), new Object[]{file, bytes});
			}
		}

		if (files.size() > 0){
			request.setAttribute("files", files);
		}
	}

	/** 
	 * Debug 
	 */
	private void loggerInfo(RenderRequest request, RenderResponse response) {
		if (log.isDebugEnabled()) {
			log.debug("View "+response.getNamespace());
			log.debug("Remote user: "+request.getRemoteUser());

			if (request.getUserPrincipal() != null) {
				// user principal is null in pre-prod
				log.debug("User principal name: "+request.getUserPrincipal().getName());
			}
		}
	}

	private void debugParams(NameValuePair[] parametersBody) {
		log.debug(parametersBody.length + " parameters: --------------------");
		for (int x=0 ; x<parametersBody.length ; x++) {
			log.debug(parametersBody[x].toString());
		}
		log.debug("----------------------------------");
	}

}
