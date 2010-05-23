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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.GenericPortlet;
import javax.portlet.PortletConfig;
import javax.portlet.PortletException;
import javax.portlet.PortletMode;
import javax.portlet.PortletSession;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

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
 * This is not implemented yet.
 *
 *
 * @author Mikael Lammentausta
 */
public class Rails286Portlet extends GenericPortlet {

  /** Class variables and the logger.
   */
  private final Log log = LogFactory.getLog(getClass().getName());

  private String sessionKey    = null;
  private String sessionSecret = null;
  protected int responseStatusCode = -1;

  /**** Override the GenericPortlet functions.
   */
  @Override


  /** 
   * Portlet initialization at portal startup.
   */
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
      }
      else {
        log.info("Session is secured by a shared secret");
        log.debug("Session key: "+sessionKey);
      }

      super.init(config);
  }

  public Map<String, String[]> getContainerRuntimeOptions() {
    return this.getPortletConfig().getContainerRuntimeOptions();
  }
    
  /**
    * Main method, render(). Other portlet modes are not supported.
    *
    * Downloads the Rails HTML, runs the HTML processor and
    * inserts it into RenderResponse.
    *
    * This is still a horribly long function that could be split up to
    * subfunctions for clarity.
    *
    * @since 0.8.2
    * 	Split to subfunctions, cookie system...
    *
    * @since 0.6.1
    *   Changed to use Liferay 5.2.0 API.
    *
    */
  @SuppressWarnings("unchecked")
  public void render(RenderRequest request, RenderResponse response)
  throws PortletException, IOException {
    if (log.isDebugEnabled()) {
      log.debug("View "+response.getNamespace());
      try {
        log.debug("Remote user: "+request.getRemoteUser());
        //log.debug("User principal name: "+request.getUserPrincipal().getName());
        //log.debug(request.getAuthType());
      }
      catch (java.lang.NullPointerException e) {
        log.error("No user "+e.getMessage());
      }
    } 
	  
    /* The preferences are never used.
    PortletPreferences preferences = request.getPreferences();
     */
    
    /** 
     Base + Request URLs are set in the RenderFilter 
     and are read from the PortletSession.
     */
    URL      railsBaseUrl  = null;
    String   servlet       = null;
    String   railsRoute    = null;
    URL      httpReferer   = null;
            

    /** The processed web page is catenated to the RenderResponse */
    String outputHTML      = null;
    
    
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
      *
      */
    railsBaseUrl = (java.net.URL)session.getAttribute("railsBaseUrl");
    railsRoute = (String)session.getAttribute("railsRoute");

    /** TODO: cleanup!

    Move the checks from render() to RenderFilter, which will stop the whole process, and
    forward to another method altogether.
      */

    // save the new path to session, needed for HTTP_REFERER,
    // which is set in the RENDER FILTER
    // -- breaks when using POST, and render filter should take care of this itself
    //     session.setAttribute(
    //         "railsRoute",
    //         railsRoute,
    //         PortletSession.PORTLET_SCOPE);

    // get the host section from the base URL
    String railsHost = null;
    railsHost = railsBaseUrl.getHost();
    //log.debug("railsHost in session: " + railsHost);

    OnlineClient client = null;

    try {
      // check the server and route
      if ( railsHost == null ) {
        throw new PortletException("The host is undefined!");
      }
      // TODO: if the server is unreachable
      //if () {
      //  throw new PortletException("The server " + railsHost + " was unreachable.");
      //}

      if ( railsRoute == null ) {
        log.warn( "The requested route is undefined" );
        railsRoute = "/";
      }


      String       railsResponse = null;
      java.net.URL requestUrl    = null;


      /** Form the request URL, 
      TODO : move to subfunction  */
      servlet = (String)session.getAttribute("servlet");
      RouteAnalyzer ra = new RouteAnalyzer(railsBaseUrl,servlet);
      try {
        requestUrl = ra.getFullURL(railsRoute);
      }
      catch (java.net.MalformedURLException e) {
        log.error(e.getMessage());
      }
      log.debug("Request URL: "+requestUrl.toString());


      Map<String,Cookie> cookies = getCookies(session);

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
      // get the referer
      httpReferer = getHttpReferer(session);
      // Language
      java.util.Locale locale = request.getLocale();
      
      /**
       *
       * Execute the request
       *
       */
      client = new OnlineClient(requestUrl,cookies,httpReferer,locale);

      /**
       * GET
       */
      if (requestMethod.equals("get")) {
        railsResponse = executeGet(session, client);
      }

      /**
       * POST, PUT (PUT is sent as POST)
       */
      else if (requestMethod.equals("post") || requestMethod.equals("put")) {
        railsResponse = executePost(request, httpReferer, session, client);
      }
      
      // DELETE?

      /** FAIL */
      else {
        throw new PortletException("Unknown request method: "+requestMethod);
      }

      /**
       * Process the response body
       * TODO: only if response was in 2xx range
       */
      outputHTML = processResponseBody(response, railsBaseUrl, servlet, railsRoute, railsResponse);

    } // try
    catch(Exception e) {
      outputHTML = e.getMessage();
      log.error(outputHTML);
    }

    // set the response status code (for tests)
    responseStatusCode = client.statusCode;
    //log.debug("Response status code: " +responseStatusCode);
    
    // Write the HTML to RenderResponse
      //log.debug(outputHTML);
    response.setContentType("text/html"); // TODO: get from the actual response
    PrintWriter out = response.getWriter();
    out.println( outputHTML );
  }

  // suppress request.getParameterMap unchecked cast for processAction,
  // since it should always return <String,String[]>

  /** 
   * Handles POST requests.
   */
  @SuppressWarnings("unchecked")
  public void processAction(ActionRequest request, ActionResponse response)
  throws PortletException, IOException {

	// In case of a multipart request, retrieve the files
	if (PortletFileUpload.isMultipartContent(request)){
		log.debug("Multipart request");
		retrieveFiles(request);
	}
	  
    PortletSession session = request.getPortletSession(true);
    /** Process an action from the web page.
      * This can be a classic HTML form or a JavaScript-generator form POST.
      */
    if(request.getPortletMode().equals(PortletMode.VIEW)) {
      log.debug("Received ActionRequest from the web page.");
      log.debug("Request character encoding: "+ request.getCharacterEncoding());
      log.debug("http.protocol.content-charset: " + System.getProperty("http.protocol.content-charset"));
      // TODO: request may be UTF-8, but the form data may be in different
      // encoding, set by <form accept-charset="..">

      String actionUrl    = null;
      String actionMethod = null;

      /** Process the request parameters.
        * These are set in BodyTagVisitor.
        * The returned parameters are "x-www-form-urlencoded" decoded.
        */
      Map<String,String[]> p = new HashMap<String,String[]>(request.getParameterMap());
      // create a clone of the parameter Map
      Map<String,String[]> params = new HashMap<String,String[]>(p);

      // default to POST for actions
      actionMethod = (params.containsKey("originalActionMethod") ? 
        params.remove("originalActionMethod")[0] : "post"
      );
      // set the action URL
      if (params.containsKey("originalActionUrl")) {
        actionUrl = params.remove("originalActionUrl")[0];
        log.debug("Received a form action: " + actionUrl);

        // formulate NameValuePair[]
        NameValuePair[] parametersBody = Rails286PortletFunctions.paramsToNameValuePairs(params);
        debugParams(parametersBody);
        // save the attributes to the RenderRequest (set custom parameters now..)
        request.setAttribute("parametersBody",parametersBody);
      }
      else {
        log.warn("No action URL given! Halting action.");
        actionUrl = (String)request.getParameter("railsRoute");
        actionMethod = "get";
      }
      request.setAttribute("requestMethod",actionMethod);
      request.setAttribute("railsRoute",actionUrl);
    }
	/*
    // Processes an internal portlet action from the doEdit() function.
    else if(request.getPortletMode().equals(PortletMode.EDIT)) {
		// not implemented
    }
	*/
  }

 /*
  * Subfunctions
  * 
  */

	/** Executes a POST request.
   */
  @SuppressWarnings("unchecked")
  private String executePost(RenderRequest request, URL httpReferer, PortletSession session, OnlineClient client) 
  throws HttpException,IOException {

	  String railsResponse = null;
	  
	  // retrieve POST parameters        
	  NameValuePair[] parametersBody = (NameValuePair[]) request.getAttribute("parametersBody");
	  
	  if (parametersBody == null) {
		  log.warn("Empty parametersBody in the request, no POST data?");
	  }
	  else if (log.isDebugEnabled()) {
		  debugParams(parametersBody);
	  }
	  
	  // retrieve files
	  Map<String, Object[]> files = (Map<String, Object[]>) request.getAttribute("files");

	  // POST the parametersBody
	  // OnlineClient handles cases where POST redirects.
	  railsResponse = new String(client.post(parametersBody, files));

	  // store new cookies into PortletSession.
	  session.setAttribute("cookies", client.cookies, PortletSession.PORTLET_SCOPE);

	  // do not leave the POST method hanging around in the session.
	  session.setAttribute("requestMethod", "get", PortletSession.PORTLET_SCOPE);

	  // ???
	  if (session.getAttribute("httpReferer") != null) {
		  log.debug("Saving route from httpReferer: "+httpReferer.toString());
		  session.setAttribute("railsRoute", httpReferer.toString(), PortletSession.PORTLET_SCOPE);
	  }
	  return railsResponse;
  }

  /**
   *  Executes a GET request.
   */
  private String executeGet(PortletSession session, OnlineClient client)
  throws HttpException, IOException {
	  
	  String railsResponse = null;
	  railsResponse = new String(client.get());
	  // TODO: get responseHeader and responseBody

	  // save/update client Cookie[] into cookies HashMap
	  /*
		for (Cookie c : client.cookies)
		  cookies.put((String)c.getName(), (Cookie)c);
	   */

	  // should servlet cookies be stored to the session? here they are not.

	  session.setAttribute("cookies", client.cookies, PortletSession.PORTLET_SCOPE);
	  return railsResponse;
  }
  
  /**
   * Create the {@link PageProcessor} and process the remote response HTML.
   * 
   * @param response - {@link RenderResponse}
   * @param railsBaseUrl - {@link URL}
   * @param servlet - {@link String}
   * @param railsRoute - {@link String}
   * @param railsResponse - {@link String}
   * @return outputHTML - {@link String}
   */
	private String processResponseBody(
		RenderResponse response, 
		URL railsBaseUrl, 
		String servlet, 
		String railsRoute, 
		String railsResponse)
	{
		String outputHTML = null;
		//log.debug("Response length: "+railsResponse.length());
		if ( (railsResponse != null ) && (railsResponse.length() > 1) ) {
			try {
				//log.debug("Processing page");
				// instantiate the PageProcessor
				// PageProcessor => HeadProcessor, BodyTagVisitor (uses RouteAnalyzer)
				PageProcessor p = new PageProcessor(railsResponse,servlet,response);
				outputHTML   = p.process(railsBaseUrl,railsRoute);
				
				/** Set the portlet title by HTML title */
				String title = p.title;
				log.debug("Title: "+title);
				if ( title==null || title=="" ) {
					response.setTitle( " " ); // nbsp, because Liferay post-processes blank strings
				}
				else { 
					response.setTitle( title ); 
				}
			}
			// p.process throws ParserException when input is invalid. Should it be catched?
			catch (ParserException e) {
			  log.error(e.getMessage());
			}
			catch (IllegalStateException e) {
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
	  String requestMethod = session.getAttribute("requestMethod") == null ? "get" : (String)session.getAttribute("requestMethod");
	  log.debug("Request method: "+requestMethod);
	  return requestMethod;
  }
  
  private URL getHttpReferer(PortletSession session) {
    if (session.getAttribute("httpReferer") != null) {
	    return (java.net.URL)session.getAttribute("httpReferer");
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
  private Map<String, Cookie> getCookies(PortletSession session) {
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
    if ( sessionSecret != null ) {
      Cookie _secretCookie = secretCookie(session);
      cookies.put((String)_secretCookie.getName(), _secretCookie);
    }
    
    // UID cookie
    if ( session.getAttribute("uid") != null ) {
      Cookie _uidCookie = uidCookie(session);
      cookies.put((String)_uidCookie.getName(), _uidCookie);
    }

    log.debug(cookies.size() + " cookies");
    return cookies;
  }

  /*
  Cookie with session secret.
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

	/*
  Cookie with UID.
  */
  protected Cookie uidCookie(PortletSession session) {
    // no. this is not the best way to handle this.
    URL base = (java.net.URL)session.getAttribute("railsBaseUrl");
    String host = base.getHost();
  	return new Cookie(
        host,
        "Liferay_UID",
        (String)session.getAttribute("uid"),
        "/",
        null,
        false);
  }


  /** Debug */
  private void debugParams(NameValuePair[] parametersBody) {
	  log.debug(parametersBody.length + " parameters: --------------------");
	  for (int x=0 ; x<parametersBody.length ; x++) {
		  log.debug(parametersBody[x].toString());
	  }
	  log.debug("----------------------------------");
  }

}
