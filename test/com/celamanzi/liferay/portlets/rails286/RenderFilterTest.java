package com.celamanzi.liferay.portlets.rails286;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.portlet.PortletContext;
import javax.portlet.PortletException;
import javax.portlet.PortletMode;
import javax.portlet.PortletSession;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.filter.FilterChain;
import javax.portlet.filter.FilterConfig;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.portlet.MockPortletConfig;
import org.springframework.mock.web.portlet.MockPortletSession;
import org.springframework.mock.web.portlet.MockRenderRequest;

import com.celamanzi.liferay.portlets.rails286.mock.MockRenderResponse;

public class RenderFilterTest {
    
    public String host     = "http://localhost";
    public String servlet  = "servlet";
    public String route    = "some/route?params";
    
    private Rails286PortletFilter filter = new Rails286PortletFilter();
    private FilterConfig filterConfig = null;
    private PortletContext portletContext = null;
    private PortletSession session = null;
    
    @Before
    public void setup() {
        assertNotNull(filter);
        /* mock config
         
         FilterConfig for portlets wasn't available in
         spring framework 2.5.6, so it is typecast from PortletConfig.
         
         */
        //MockFilterConfig _filterConfig = new MockFilterConfig();
        MockPortletConfig portletConfig = new MockPortletConfig();
        assertNotNull(portletConfig);
        portletConfig.addInitParameter("host", host);
        portletConfig.addInitParameter("servlet", servlet);
        portletConfig.addInitParameter("route", route);

        
        //filterConfig = (FilterConfig)_filterConfig;
        filterConfig = (FilterConfig)portletConfig;
        assertNotNull(filterConfig);
        
        session = new MockPortletSession();
        assertNotNull(session);
                
    }

    
    @Test
    public void test_init() {
        filter.init(filterConfig);
        
        assertEquals(host,filter.host);
        assertEquals(servlet,filter.servlet);
        assertEquals(route,filter.route);
    }
    
    
    @Test
    public void test_doFilter() 
    throws IOException, PortletException, MalformedURLException 
    {
        filter.init(filterConfig);

        MockRenderRequest _request = new MockRenderRequest(PortletMode.VIEW);
        _request.setSession(session);
        RenderRequest request = (RenderRequest)_request;
        assertNotNull(request);

        RenderResponse response = new MockRenderResponse();
        assertNotNull(response);
        
        FilterChain chain = new MockFilterChain();
        assertNotNull(chain);
                
        filter.doFilter(request,response,chain);
        
        java.net.URL _baseUrl = (URL)session.getAttribute("railsBaseUrl");
        assertNotNull(_baseUrl);
        assertEquals(new URL(host+"/"+servlet),_baseUrl);
        // TODO: test the thing with different combinations
        
        String _servlet = (String)session.getAttribute("servlet");
        assertNotNull(_servlet);
        assertEquals(servlet,_servlet);
        
        String _route = (String)session.getAttribute("railsRoute");
        assertNotNull(_route);
        assertEquals(route,_route);
        
        String _method = (String)session.getAttribute("requestMethod");
        assertNotNull(_method);
        assertEquals("get",_method);
        
        assertNull(session.getAttribute("httpReferer"));
        
        /*
        Enumeration names = session.getAttributeNames();
        while (names.hasMoreElements()) {
            System.out.println(names.nextElement()); } 
        */
    }

    @Test
    /** When the portlet is loaded without portlet parameters,
     * the session should be reset.
     */
    public void test_railsRoute()
    throws PortletException, IOException, MalformedURLException
    {
      filter.init(filterConfig);
      
      String route_a = PortletTest.railsJUnitRoute;
      
      MockRenderRequest _request = new MockRenderRequest(PortletMode.VIEW);
      _request.setSession(session);
      
      _request.addParameter("railsRoute", route_a);

      RenderRequest request = (RenderRequest)_request;
      assertNotNull(request);
      RenderResponse response = new MockRenderResponse();
      assertNotNull(response);
      FilterChain chain = new MockFilterChain();
      assertNotNull(chain);
            
      filter.doFilter(request,response,chain);
      
      String _route = (String)session.getAttribute("railsRoute");
      assertNotNull(_route);
      assertEquals(route_a,_route);
      
      String _method = (String)session.getAttribute("requestMethod");
      assertNotNull(_method);
      assertEquals("get",_method);
      
      assertNull(session.getAttribute("httpReferer"));
    }
  
    @Test
    /** 
     */
    public void test_httpReferer()
    throws PortletException, IOException, MalformedURLException
    {
      filter.init(filterConfig);

      String route_a = PortletTest.railsJUnitRoute;
      String route_b = "/foo?bar";
      
      session.setAttribute(
                           "railsRoute",
                           route_a,
                           PortletSession.PORTLET_SCOPE);
      
      session.setAttribute(
                           "httpReferer",
                           null,
                           PortletSession.PORTLET_SCOPE);
      
      MockRenderRequest _request = new MockRenderRequest(PortletMode.VIEW);
      _request.setSession(session);
      
      _request.addParameter("railsRoute", route_b);

      RenderRequest request = (RenderRequest)_request;
      RenderResponse response = new MockRenderResponse();
      assertNotNull(response);
      FilterChain chain = new MockFilterChain();
      assertNotNull(chain);

      filter.doFilter(request,response,chain);

      String _route = (String)session.getAttribute("railsRoute");
      assertNotNull(_route);
      assertEquals(route_b,_route);

      URL _referer = (URL)session.getAttribute("httpReferer");
      assertNotNull(_referer);
      assertEquals(new URL(host+"/"+servlet+route_a),_referer);
      
    }

    @Test
    /** When the portlet is loaded without portlet parameters,
     * the session should be reset.
     */
    public void test_sessionReset()
    throws PortletException, IOException
    {
      filter.init(filterConfig);
      
      // test that this attribute will be reset,
      // when no request parameters are set.
      session.setAttribute("railsRoute",PortletTest.railsJUnitRoute);
      
      MockRenderRequest _request = new MockRenderRequest(PortletMode.VIEW);
      _request.setSession(session);
      RenderRequest request = (RenderRequest)_request;
      assertNotNull(request);
      
      RenderResponse response = new MockRenderResponse();
      assertNotNull(response);
      
      FilterChain chain = new MockFilterChain();
      assertNotNull(chain);
      
      filter.doFilter(request,response,chain);
      
      String _route = (String)session.getAttribute("railsRoute");
      assertNotNull(_route);
      assertEquals(route,_route);
      
      assertNull(session.getAttribute("httpReferer"));
    }
  
    @Test
    /** 
     */
    public void test_fullUrlRoute()
    throws PortletException, IOException, MalformedURLException
    {
      filter.init(filterConfig);
      MockRenderRequest _request = new MockRenderRequest(PortletMode.VIEW);

      String route = "http://some-other-host" + PortletTest.railsJUnitRoute;
      _request.setSession(session);
      _request.addParameter("railsRoute", route);

      RenderRequest request = (RenderRequest)_request;
      RenderResponse response = new MockRenderResponse();
      assertNotNull(response);
      FilterChain chain = new MockFilterChain();
      assertNotNull(chain);

      filter.doFilter(request,response,chain);

      String _route = (String)session.getAttribute("railsRoute");
      assertNotNull(_route);
      // host should not be passed to route!
      assertEquals(PortletTest.railsJUnitRoute,_route);
    }
}
