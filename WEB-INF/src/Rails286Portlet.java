/**
 * Copyright (c) 2008,2009 Mikael Lammentausta
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
import java.util.HashMap;
import java.util.Map;

import java.net.URL;
import java.net.MalformedURLException;

import java.io.IOException;
import java.io.PrintWriter;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.GenericPortlet;
import javax.portlet.PortletConfig;
import javax.portlet.PortletContext;
import javax.portlet.PortletException;
import javax.portlet.PortletMode;
import javax.portlet.PortletPreferences;
import javax.portlet.PortletRequest;
import javax.portlet.PortletRequestDispatcher;
import javax.portlet.PortletResponse;
import javax.portlet.PortletSession;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
// import javax.portlet.ValidatorException;
// import javax.portlet.PortletURL;

import org.htmlparser.util.ParserException;

import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.Cookie;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/** Presents a Rails application in a JSR286 portlet.
 * Supports the GET and POST methods.
 * XHRs do not convey through the portlet. They are sent directly to the Rails server.
 *
 * @author Mikael Lammentausta
 */
public class Rails286Portlet extends GenericPortlet {

  /** Class variables and the logger.
   */
  private final Log log = LogFactory.getLog(getClass().getName());


  /**** Override the GenericPortlet functions.
   */
  @Override


  /** Portlet initialization at portal startup.
    */
  public void init(PortletConfig config) throws PortletException {
      log.info("Initializing Rails-portlet %s (v.%s)".format(
        config.getPortletName(),PortletVersion.PORTLET_VERSION));
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
    * This is a horribly long function that could be split up to several
    * subfunctions for clarity.
    *
    * @since 0.6.1
    *   Changed to use Liferay 5.2.0 API, using deprecated methods.
    *
    */
  public void render(RenderRequest request, RenderResponse response)
  throws PortletException, IOException {
    if (log.isDebugEnabled()) {
      try {
        log.debug("View "+response.getNamespace());
        log.debug(request.getAuthType());
        log.debug(request.getRemoteUser());
        log.debug(request.getUserPrincipal().getName());
      }
      catch (java.lang.NullPointerException e) {
        log.error(e.getMessage());
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
    String outputHTML          = null;
    
    
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
    log.debug("railsHost in session: " + railsHost);


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

      // this always returns true
      if ( ! OnlineUtils.serverIsAlive( railsHost ) ) {
        throw new PortletException("The server " + railsHost + " is online, but did not respond to the request.");
      }

      /**
        * server is alive!
        */
      String       railsResponse = null;
      java.net.URL requestUrl    = null;


      /** Form the request URL */
      servlet = (String)session.getAttribute("servlet");
      RouteAnalyzer ra = new RouteAnalyzer(railsBaseUrl,servlet);
      try {
        requestUrl = ra.getFullURL(railsRoute);
      }
      catch (java.net.MalformedURLException e) {
        log.error(e.getMessage());
      }

      /** nothing in the previous try-catch belongs to this method */


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
      HashMap<String,Cookie> cookies = new HashMap<String,Cookie>();
        
      // get cookies from PortletSession to HashMap
      if (session.getAttribute("cookies") != null) {
        log.debug("Stored session cookies found");
        for (Cookie cookie : (Cookie[])session.getAttribute("cookies")) {
          cookies.put((String)cookie.getName(), cookie);
        }
      }

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
      
      /**
        * Select the request method (GET or POST).
        *
        * Use requestMethod if defined, otherwise use GET.
        */
      String requestMethod = (
        session.getAttribute("requestMethod") == null ? "get" : (String)session.getAttribute("requestMethod")
      );
      log.debug("Request method: "+requestMethod);

      // get the referer
      if (session.getAttribute("httpReferer") != null) {
        httpReferer = (java.net.URL)session.getAttribute("httpReferer");
        log.debug("HTTP referer: "+httpReferer.toString());
      }
      else {
        log.debug("HTTP referer is null!");
      }

      // Language
      java.util.Locale locale = request.getLocale();
      
      
      /**
       *
       * Execute the request
       *
       */
      
      
      OnlineClient client = null;
      client = new OnlineClient(requestUrl,cookies,httpReferer,locale);

      /**
       * GET
       */
      if (requestMethod.equals("get")) {
        railsResponse = new String(client.get());
        // TODO: get responseHeader and responseBody
        
        // save/update client Cookie[] into cookies HashMap
        /*
        for (Cookie c : client.cookies)
          cookies.put((String)c.getName(), (Cookie)c);
         */
        
        // should servlet cookies be stored to the session? here they are not.

        session.setAttribute("cookies",
                             client.cookies,
                             PortletSession.PORTLET_SCOPE);
      }

      /**
       * POST
       */
      else if (requestMethod.equals("post") || requestMethod.equals("put")) {
        // retrieve POST parameters        
        NameValuePair[] parametersBody = (NameValuePair[])request.getAttribute("parametersBody");
        if (parametersBody == null) {
          log.warn("Empty parametersBody in the request, no POST data?");
        }
        else if (log.isDebugEnabled()) {
          debugParams(parametersBody);
        }
        
        // POST the parametersBody
        // OnlineClient handles cases where POST redirects.
        railsResponse = new String(client.post(parametersBody));

        // store new cookies into PortletSession.
        session.setAttribute("cookies",
                             client.cookies,
                             PortletSession.PORTLET_SCOPE);

        // do not leave the POST method hanging around in the session.
        session.setAttribute("requestMethod",
                             "get",
                             PortletSession.PORTLET_SCOPE);
        
        // ???
        if (session.getAttribute("httpReferer") != null) {
          log.debug("Saving route from httpReferer: "+httpReferer.toString());
          session.setAttribute(
              "railsRoute",
              httpReferer.toString(),
              PortletSession.PORTLET_SCOPE);
        }
      }

      /** FAIL */
      else {
        throw new PortletException("Unknown request method: "+requestMethod);
      }


      /**
       *
       * Process the response body
       *
       */
      if ( railsResponse != null ) {
        try {
          // instantiate the PageProcessor
          // PageProcessor => HeadProcessor, BodyTagVisitor (uses RouteAnalyzer)
          PageProcessor p = new PageProcessor(railsResponse,servlet,response);
          outputHTML   = p.process(railsBaseUrl,railsRoute);

          /** Set the portlet title by HTML title */
          String title = p.title;
          log.info("Page title: "+title);
          if ( title==null || title=="" ) {
            response.setTitle( " " ); // nbsp, because Liferay post-processes blank strings
          }
          else { response.setTitle( title ); }
        }
        // p.process throws ParserException when input is invalid. Should it be catched?
        catch (ParserException e) {
          log.error(e.getMessage());
        }
        catch (IllegalStateException e) {
          log.error(e.getMessage());
        }
      }
      /*****************************************************/

    } // try
    catch(Exception e) {
      outputHTML = e.getMessage();
      log.error(outputHTML);
    }

    // Catenate the HTML page to the RenderResponse
      //log.debug(outputHTML);
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    out.println( outputHTML );
  }


  // suppress request.getParameterMap unchecked cast for processAction,
  // since it should always return <String,String[]>
  @SuppressWarnings("unchecked")


  /** Handles POST requests.
    */
  public void processAction(ActionRequest request, ActionResponse response)
  throws PortletException, IOException {
    PortletSession session = request.getPortletSession(true);
    /** Process an action from the web page.
      * This can be a classic HTML form or a JavaScript-generator form POST.
      */
    if(request.getPortletMode().equals(PortletMode.VIEW)) {
      log.debug("Received ActionRequest from the web page.");

      String actionUrl    = null;
      String actionMethod = null;

      /** Process the request parameters.
        * These are set in BodyTagVisitor.
        * The returned parameters are "x-www-form-urlencoded" decoded.
        */
      Map<String,String[]> p = new HashMap<String,String[]>(request.getParameterMap());
      // create a clone of the parameter Map
      Map<String,String[]> params = new HashMap<String,String[]>();
      params.putAll(p);

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
        // save the attributes to the RenderRequest (no can do set parameter)
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

  @SuppressWarnings("")

  /** Debug */
  private void debugParams(NameValuePair[] parametersBody) {
    log.debug(parametersBody.length + " parameters: --------------------");
    for (int x=0 ; x<parametersBody.length ; x++) {
      log.debug(parametersBody[x].toString());
    }
    log.debug("----------------------------------");
  }

}
