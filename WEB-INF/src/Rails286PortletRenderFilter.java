/**
 * Copyright (c) 2008,2009,2010 Mikael Lammentausta
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

import java.net.URL;
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
  public String host    = null;
  public String servlet = null;
  public String route   = null;

  /** Constructors */
  public Rails286PortletRenderFilter() {}

  @Override

  /** Read the init parameters from FilterConfig (XML).
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
    if (servlet==null) { servlet = ""; }
    route   = filterConfig.getInitParameter("route");

    if (log.isDebugEnabled()) {
      log.debug("init host: "+host);
      log.debug("init servlet: "+servlet);
      log.debug("init route: "+route);
    }
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
    log.debug("Render Filter activated for session "+session_id);
    //log.debug(session.getPortletContext().getServerInfo());
    //log.debug(session.getPortletContext().getPortletContextName());

    if (log.isDebugEnabled()) {
    	debugRequest(request);
    }

    /** Base URL (host + servlet).
     *
     * Remove the last slash '/' from the base URL,
     * otherwise several things may fail in the TagVisitor
     */
    String base = (
                   host == null ?
                   request.getScheme()+
                    "://"+
                    request.getServerName()+
                    ":"+
                    request.getServerPort()+
                    "/"+
                    servlet :
                   
                    host.replaceFirst("/$","")+"/"+servlet
                   );
    URL railsBaseUrl = new java.net.URL(base);

    
    String railsRoute    = null;
    String requestMethod = "get";
    URL    httpReferer   = null;
    
    if (
      ( request.getParameter("railsRoute") == null ) &&
      ( request.getAttribute("railsRoute") == null )
    ) {
      /*
       * If the request does not define the route, it is reset to default.
       */
      log.debug("Unset request parameter \"railsRoute\" - reset portlet");
      railsRoute = route;
    }
    else {
      /** Request method. If POST via actionURL, this is set. */
      if ( request.getAttribute("requestMethod") != null ) {
        railsRoute = (String)request.getAttribute("railsRoute");
        requestMethod = (String)request.getAttribute("requestMethod");
      }
      else {
        /** Set the route from request parameter "railsRoute".
         */
        railsRoute = request.getParameter("railsRoute");
      }

      /** Set the HTTP Referer from session */
      if (session.getAttribute("railsRoute") != null) {
        try {
          String oldRoute = (String)session.getAttribute("railsRoute");
          RouteAnalyzer ra = new RouteAnalyzer(railsBaseUrl,servlet);
          httpReferer = ra.getFullURL(oldRoute);
        }
        catch (java.net.MalformedURLException e) {
          log.error(e.getMessage());
        }
        catch (NullPointerException e) {
          log.error(e.getMessage());
        }
        log.debug("Set HTTP referer: "+httpReferer.toString());
      }
    }

    
    // railsRoute may contain variables to be replaced at runtime
    railsRoute = Rails286PortletFunctions.decipherPath( railsRoute, request );

    
    /** update the PortletSession */
    try {
      log.debug( "Updating session id " + session_id );

      session.setAttribute(
          "railsBaseUrl",
          railsBaseUrl,
          PortletSession.PORTLET_SCOPE);

      session.setAttribute(
          "servlet",
          servlet,
          PortletSession.PORTLET_SCOPE);

      session.setAttribute(
          "railsRoute",
          railsRoute,
          PortletSession.PORTLET_SCOPE);

      session.setAttribute(
          "requestMethod",
          requestMethod,
          PortletSession.PORTLET_SCOPE);

      session.setAttribute(
          "httpReferer",
          httpReferer,
          PortletSession.PORTLET_SCOPE);
    }
    catch (IllegalStateException e) {
      log.error( e.getMessage() );
      throw e;
    }
    catch (IllegalArgumentException e) {
      log.error( e.getMessage() );
      throw e;
    }

    if (log.isDebugEnabled()) {
    	debugSession(session);
    }

    // add the Filter to the chain
    chain.doFilter(request, response);

  }

  /** Debug */
  private void debugRequest(RenderRequest request) {
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
  }

  /** Debug */
  private void debugSession(PortletSession session) {
      log.debug("Session attributes -------v");
      for (Enumeration e = session.getAttributeNames() ; e.hasMoreElements();) {
        String a = (String)e.nextElement();
        log.debug(a+": "+session.getAttribute(a));
      }
      log.debug("---------------------------");
  }  

}
