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

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.HashMap;
import java.util.Map;

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

  private final String PORTLET_VERSION    = "0.6.2";

  /** Class variables and the logger.
   * Set in the initializer.
   */
  private final Log log = LogFactory.getLog(getClass().getName());

  // the EDIT mode is supported since 0.6.0
  private String editUrl;
  private String helpUrl;
  private String errorUrl;
  private String debugUrl;
//   protected static String host = null;

  // the servlet is defined in XML
  protected String servlet = null;

  /** Login parameters -- debugging stage -- not used */
  String username = "";
  String userpw = "";
//   String logonAction = "/"+servlet+"";
  NameValuePair userid   = new NameValuePair("username", username);
  NameValuePair password = new NameValuePair("userpw", userpw);
  NameValuePair[] loginCredentials = new NameValuePair[] {userid, password};


  /**** Override the GenericPortlet functions.
   */
  @Override


  public void init(PortletConfig config) throws PortletException {
    log.info("Initializing Rails-portlet "+PORTLET_VERSION);
    super.init(config);

//     editUrl           = config.getInitParameter("edit_url");
//     helpUrl           = config.getInitParameter("help_url");
//     errorUrl          = config.getInitParameter("error_url");
//     debugUrl          = config.getInitParameter("debug_url");
  }

  /**
    * Main method, doView. The other portlet modes are not supported.
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
  protected void doView(RenderRequest request, RenderResponse response)
  throws PortletException, IOException {
    log.debug("View "+response.getNamespace());
    PortletPreferences preferences = request.getPreferences();

    /** Base + Request URLs are set in the RenderFilter and are read from the PortletSession. */
    java.net.URL railsBaseUrl  = null;
    String       railsRoute    = null;
    java.net.URL httpReferer   = null;

    /** The processed web page is catenated to the RenderResponse */
    String outputHTML          = null;


    /**
      * Session storage.
      *
      * The current host, servlet and route are stored into the session.
      * The session is manipulated in the Render Filter.
      *
      * APPLICATION_SCOPE stores data across all user portlets in the same session.
      * PORTLET_SCOPE stores information only accessible to the user's QueryPortlet instance.
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


        /** Cookie handling.
          *
          * Get the authentication cookies, if not found from the session.
          * Store the new cookies into the session.
          *
          * TODO: clean up this cruft to another function.
          * Besides, cookies are not currently used for anything.
          *
          */
        Cookie[] logonCookies = null;
        Cookie[] cookies = null;
        /*
        if (session.getAttribute("cookies") == null) {
          // request new cookies
          log.debug("Cookies not found in the session - requesting new cookies");
          try {
            //logonCookies = OnlineUtils.getCookies(requestUrl,loginCredentials);
            cookies = OnlineUtils.getCookies(requestUrl);
          }
          catch (Exception e) {
            log.error(e.getMessage());
          }

          // in case we got new cookies
          if (cookies != null) {
            log.info("Storing "+cookies.length+" cookies into session");
            session.setAttribute(
                "cookies",
                cookies,
                PortletSession.PORTLET_SCOPE);
          }
          else {
            log.warn("No cookies received");
          }
        }
        // pick up existing cookies from the session store
        else {
          cookies = (Cookie[])session.getAttribute("cookies");
          log.debug("Using cookies stored in the session");

          for (int i = 0; i < cookies.length; i++) {
            log.debug(cookies[i].toString());
          }
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
      httpReferer = (java.net.URL)session.getAttribute("httpReferer");
      if (httpReferer != null ) {
        log.debug("HTTP referer: "+httpReferer.toString());
      }
      else {
        log.debug("HTTP referer is null!");
      }

      // Language
      java.util.Locale locale = request.getLocale();


      /** GET */
      if (requestMethod.equals("get")) {
        railsResponse = OnlineUtils.getRailsHTML(requestUrl,cookies,httpReferer,locale);
      }

      /** POST */
      else if (requestMethod.equals("post") || requestMethod.equals("put")) {
        // parametersBody for the POST action
        NameValuePair[] parametersBody = (NameValuePair[])request.getAttribute("parametersBody");
        if (parametersBody != null) {
          debugParams(parametersBody);
        }
        else {
          log.warn("Empty parametersBody in the request, no POST data?");
        }
        // POST the parametersBody
        railsResponse = OnlineUtils.postActionRequest(requestUrl,parametersBody,cookies,httpReferer);

        // do not leave the POST method hanging around in the session
        if (session.getAttribute("httpReferer") != null) {
          log.debug("Saving route from httpReferer: "+httpReferer.toString());
          session.setAttribute(
              "railsRoute",
              httpReferer.toString(),
              PortletSession.PORTLET_SCOPE);
        }


        // TODO: check for status code 302 instead of regexp
        if (true) {
          log.debug("You are being redirected .. GET the directed page");

          /** Extract the url and create another request.
            *
            * The railsResponse is a page with the contents:
            * You are being <a href="...">redirected</a>.</div>
            *
            */

          /* REGULAR EXPRESSIONS */
          String a_regexp = "You are being <a ([^\\>])*>redirected";
          String href_regexp = "href=[\"]?(([^\"|>| ])*)";
          Pattern a_pattern = Pattern.compile(a_regexp);
          Matcher a_matcher = a_pattern.matcher(railsResponse);

          while (a_matcher.find()) {
            String html_a = null;
            log.debug( "groups   : " + a_matcher.groupCount() );
            html_a = a_matcher.group();
            log.debug( "Redirect link: " + html_a );

            String a_href = null;
            Pattern href_pattern = Pattern.compile(href_regexp);
            Matcher href_matcher = href_pattern.matcher(html_a);

            if (href_matcher.find()) {
              a_href = href_matcher.group(1);
              log.debug( "href: " + a_href );

              /** Form the request URL */
              java.net.URL post_get = null;
              try {
                post_get = ra.getFullURL(a_href);
              }
              catch (java.net.MalformedURLException e) {
                log.error(e.getMessage());
              }
              log.debug("post-POST GET: "+post_get.toString());

              /** GET after POST */
              railsResponse = OnlineUtils.getRailsHTML(post_get,null,httpReferer,locale);
              // railsResponse = OnlineUtils.getRailsHTML(post_get,cookies,httpReferer);
            } // match!
          } // match!
        } // response 302
      } // POST

      /** FAIL */
      else {
        throw new PortletException("Unknown request method: "+requestMethod);
      }


      /** Process the response body */
      if ( railsResponse != null ) {
        try {
          // instantiate the PageProcessor
          // PageProcessor => HeadProcessor, BodyTagVisitor (uses RouteAnalyzer)
          PageProcessor p = new PageProcessor(railsResponse,servlet,response);
          outputHTML   = p.process(railsBaseUrl,railsRoute);

          /** Set the portlet title to the HTML page title */
          String title = p.title;
          log.debug("<html><head><title>: "+title);
          if (title != null) {
            response.setTitle( title );
          }
          //response.setTitle( title == null ? "rails286-portlet" : title );
        }
        // p.process throws ParserException is input is invalid. Should it be catched?
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
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    out.println( outputHTML );
  }


  protected void doEdit(RenderRequest request, RenderResponse response)
  throws PortletException, IOException {
    log.info("Edit");

    String outputHTML = "This will become the edit view";

    // Catenate the HTML page to the RenderResponse
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    out.println( outputHTML );
  }


  protected void doHelp(RenderRequest request, RenderResponse response)
  throws PortletException, IOException {
    log.debug("Help: Collecting information about the portlet and the engine.");

    PortletSession session = request.getPortletSession(true);
    PortletContext cx = session.getPortletContext();

    log.debug( request.getAttribute("javax.portlet.portletc.portletname") );
    log.debug( request.getAttribute("com.sun.portal.portletcontainer.portlet.title") );
    log.debug( "context name: " + cx.getPortletContextName() );
    log.debug( "real path: " + cx.getRealPath("/") );
    log.debug( "resource . URL: " + cx.getResource(".").toString() );
    // javax.portlet.portletc.httpServletRequest
    // javax.portlet.lifecycle_phase
    //     log.debug( "generic title: " + com.sun.portal.portletcontainer.portlet.generic.title );
    //     log.debug( "ccpp: " + javax.portlet.ccpp );
    // javax.portlet.portletc.httpServletResponse
    log.debug( "server info: " + cx.getServerInfo() );
    log.debug( cx.getMajorVersion() );

    response.setContentType("text/html");
    PortletRequestDispatcher dispatcher = getPortletContext().getRequestDispatcher(helpUrl);
    dispatcher.include(request,response);
  }


  // suppress request.getParameterMap unchecked cast for processAction, 
  // since it should always return <String,String[]>
  @SuppressWarnings("unchecked")


  /** Processes both internal portlet actions from the doEdit() function, and
    * handles the actions from the web page from the VIEW mode.
    */
  public void processAction(ActionRequest request, ActionResponse response)
  throws PortletException, IOException {
    PortletSession session = request.getPortletSession(true);

    /** Processes an internal portlet action from the doEdit() function. */
    if(request.getPortletMode().equals(PortletMode.EDIT)) {
      log.debug("Received ActionRequest from portlet.");
//       String errorMessage       = null;
      boolean isValid           = true;
      java.net.URL railsBaseUrl = null;

      try {
        railsBaseUrl = new java.net.URL(
          (String)request.getParameter("railsBaseUrl")
        );
      }
      // TODO: VALIDATE
      catch (java.net.MalformedURLException mue) {
        // user entered an invalid URL
        log.error(mue.getMessage());
        // do not change the stored value
        isValid = false;
      }

      String railsRoute  = (String)request.getParameter("railsRoute");

      // delete cookies?
      if (request.getParameter("deleteCookies") != null) {
        log.debug("Asked to delete existing cookies..deleting..");
        session.setAttribute(
            "cookies",
            null,
            PortletSession.PORTLET_SCOPE);
      }

      // if entered data is valid, save to session and return to the view mode
      if (isValid) {

        log.debug("railsBaseUrl: " + railsBaseUrl.toString());
        log.debug("railsRoute: " + railsRoute);

        request.setAttribute("railsBaseUrl",railsBaseUrl);
        request.setAttribute("railsRoute",railsRoute);
        log.debug("Saved to request");

        response.setPortletMode(PortletMode.VIEW);
      }
    }


    /** Process an action from the web page.
      * This can be a classic HTML form or a JavaScript-generator form POST.
      */
    else if(request.getPortletMode().equals(PortletMode.VIEW)) {
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
