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

import java.util.Enumeration;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.io.IOException;
import java.io.PrintWriter;

import javax.portlet.PortletException;
import javax.portlet.PortletSession;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import javax.portlet.filter.FilterChain;
import javax.portlet.filter.FilterConfig;
import javax.portlet.filter.RenderFilter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.MalformedURLException;


/** Parses the Rails URL in the Portlet parameter.
 * This filter is invoked before initializing the Rails286Portlet.
 * The doFilter function sets the Parameter into the PortletSession.
 * 
 * @author Mikael Lammentausta
 */
public class Rails286PortletRenderFilter implements RenderFilter {

  /** Designated Logger for this class. */
  private final Log log = LogFactory.getLog(getClass().getName());
  private FilterConfig filterConfig;

  /** the default rails URLs */
  private java.net.URL defaultRailsBaseUrl = null;
  private String       defaultRailsRoute = null;
  private String host    = null;
  private String servlet = null;
  private String route   = null;

  /** Constructors */
  public Rails286PortletRenderFilter() {}

  @Override

  /** Read the init parameters from XML.
    * There are three different parameters:
    * - host    (optional - if empty or null, the request server is used)
    * - servlet (the url to the Rails WAR)
    * - route   (the Rails route)
    *
    * Using the server address from the request is better than
    * specifying it in the init parameter for two reasons:
    * 1) Using the portlet on different servers is simpler as there is
    *    no need to edit the portlet-ext.xml file.
    * 2) If the server has several virtualhosts, this breaks them.
    *
    */
  public void init(final FilterConfig filterConfig) {
    this.filterConfig = filterConfig;
    host    = filterConfig.getInitParameter("host");
    // consider empty hosts as null
    if ((host != null) && (host.equals(""))) {
      host = null;
    }
    servlet = filterConfig.getInitParameter("servlet");
    route   = filterConfig.getInitParameter("route");
    log.debug("init host: "+host);
    log.debug("init servlet: "+servlet);
    log.debug("init route: "+route);
  }

  public void destroy() {}


  /** Filter the RenderRequest attributes and parameters and fix up PortletSession
      for the HttpClient request.

  The order of preferences for the view URL:
    1: Request params
    2: request attributes
    3: Session
    4: fallback value

  TODO: this needs cleanup. Just read/write appropriate values from the session.

  The doFilter() method of a portlet filter may create customized request and response objects by using *RequestWrapper and *ResponseWrapper classes and passing these wrappers to the doFilter() method of FilterChain. 

  */
  public void doFilter(RenderRequest request,
                       RenderResponse response,
                       FilterChain chain)
  throws IOException, PortletException, MalformedURLException {
    PortletSession session = request.getPortletSession(true);
    String session_id      = request.getRequestedSessionId();
    log.debug("Render Filter "+session_id+" activated");
    log.debug(session.getPortletContext().getServerInfo());

    log.debug("konteksti:");
    log.debug(session.getPortletContext().getPortletContextName());
//     log.debug(session.getPortletContext().());

    if (servlet==null) {
      throw new PortletException("Servlet is undefined");
    }

    log.debug("Request attributes -------v");
    for (Enumeration e = request.getAttributeNames() ; e.hasMoreElements();) {
      String a = (String)e.nextElement();
      log.debug(a+": "+request.getAttribute(a));
    }
    log.debug("Request parameters -------v");
    for (Enumeration e = request.getParameterNames() ; e.hasMoreElements();) {
      String a = (String)e.nextElement();
      log.debug(a+": "+request.getParameter(a));
    }
    log.debug("---------------------------");
    log.debug("*");

    /** Base URL (host + servlet) */
    // remove the last slash '/' from the base URL,
    // otherwise several things may fail in the TagVisitor
    String base = (
      host == null ?
        request.getScheme()+"://"+request.getServerName()+":"+request.getServerPort()+"/"+servlet :
        host.replaceFirst("/$","")+"/"+servlet
    );
    java.net.URL railsBaseUrl = new java.net.URL(
      base
    );
    log.debug("Using baseurl: "+railsBaseUrl.toString());


    /** Route.
      * First use the value to the init parameter, and update
      * from the request parameters or session.
      */
    String railsRoute = route;
    if ( request.getParameter("railsRoute") != null ) {
      railsRoute = request.getParameter("railsRoute");
      log.debug( "Received the request parameter railsRoute: " + railsRoute );
    }
    // attributes are set in processAction
    else if ( request.getAttribute("railsRoute") != null ) {
      railsRoute = (String)request.getAttribute("railsRoute");
      log.debug( "Received the request attribute railsRoute: " + railsRoute );
    }
    /** Then in the session */
    else if (session.getAttribute("railsRoute") != null) {
      railsRoute  = (String)session.getAttribute("railsRoute");
      log.debug("Using route from session: "+railsRoute);
    }

    // railsRoute may contain variables to be replaced at runtime
    railsRoute = Rails286PortletFunctions.decipherPath( railsRoute, request );


    /** Request method. GET or POST */
    String requestMethod = "get";
    if ( request.getAttribute("requestMethod") != null ) {
      requestMethod = (String)request.getAttribute("requestMethod");
      log.debug("Request method: "+requestMethod);
    }
    else {
      log.debug("Using default request method GET");
    }


    /** Set the HTTP Referer from session for GET requests */
    java.net.URL httpReferer = null;
    if ((session.getAttribute("railsRoute") != null) && (session.getAttribute("requestMethod") == "get")) {
      try {
        String redirectRoute = (String)session.getAttribute("railsRoute");
        RouteAnalyzer ra = new RouteAnalyzer(railsBaseUrl,servlet);
        httpReferer = ra.getFullURL(redirectRoute);
      }
      catch (java.net.MalformedURLException e) {
        log.error(e.getMessage());
      }
      catch (NullPointerException e) {
        log.error(e.getMessage());
      }
      log.debug("HTTP referer: "+httpReferer.toString());
    }
    else {
      log.debug("No HTTP referer");
    }


    /** update the PortletSession */
    try {
      log.debug( "Saving attributes to PortletSession id " + session_id );

      if (railsBaseUrl != null) {
        //log.debug( "railsBaseUrl = " + railsBaseUrl );
        session.setAttribute(
            "railsBaseUrl",
            railsBaseUrl,
            PortletSession.PORTLET_SCOPE);
      }
      session.setAttribute(
          "servlet",
          servlet,
          PortletSession.PORTLET_SCOPE);
      if (railsRoute != null) {
        //log.debug( "railsRoute = " + railsRoute );
        session.setAttribute(
            "railsRoute",
            railsRoute,
            PortletSession.PORTLET_SCOPE);
      }
      if (requestMethod != null) {
        session.setAttribute(
            "requestMethod",
            requestMethod,
            PortletSession.PORTLET_SCOPE);
      }
      if (httpReferer != null) {
        log.debug("Setting new referer to: "+httpReferer);
        session.setAttribute(
            "httpReferer",
            httpReferer,
            PortletSession.PORTLET_SCOPE);
      }
    }
    catch (IllegalStateException e) {
      log.error( e.getMessage() );
      throw e;
    }
    catch (IllegalArgumentException e) {
      log.error( e.getMessage() );
      throw e;
    }

    log.debug("*");
    log.debug("Session attributes -------v");
    for (Enumeration e = session.getAttributeNames() ; e.hasMoreElements();) {
      String a = (String)e.nextElement();
      log.debug(a+": "+session.getAttribute(a));
    }
    log.debug("---------------------------");

    // add the Filter to the chain
    chain.doFilter(request, response);

  }
}
