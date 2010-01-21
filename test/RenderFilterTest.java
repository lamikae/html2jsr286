package com.celamanzi.liferay.portlets.rails286;

import java.util.Enumeration;
import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

import org.springframework.mock.web.portlet.*;
import org.springframework.mock.web.*;
import javax.portlet.*;
import javax.portlet.filter.*;

import com.celamanzi.liferay.portlets.rails286.Rails286PortletRenderFilter;

public class RenderFilterTest {
    
    public String host     = "http://localhost";
    public String servlet  = "servlet";
    public String route    = "some/route?params";
    
    protected String sessionSecret = PortletTest.sessionSecret;
    
    private Rails286PortletRenderFilter filter = new Rails286PortletRenderFilter();
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
        portletConfig.addInitParameter("session_secret", sessionSecret);

        
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
        assertEquals(sessionSecret,filter.sessionSecret);
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

        String _secret = (String)session.getAttribute("sessionSecret");
        assertNotNull(_secret);
        assertEquals(sessionSecret,_secret);
        
        assertNull(session.getAttribute("httpReferer"));
        
        /*
        Enumeration names = session.getAttributeNames();
        while (names.hasMoreElements()) {
            System.out.println(names.nextElement()); } 
        */
    }


    @Test
    public void test_withoutSecret() 
    throws IOException, PortletException, MalformedURLException 
    {
        MockPortletConfig c = new MockPortletConfig();
        assertNotNull(c);
        c.addInitParameter("host", host);
        c.addInitParameter("servlet", servlet);
        c.addInitParameter("route", route);

        FilterConfig fc = (FilterConfig)c;
        assertNotNull(fc);

        filter.init(fc);
        
        assertNull(filter.sessionSecret);

        MockRenderRequest _request = new MockRenderRequest(PortletMode.VIEW);
        _request.setSession(session);
        RenderRequest request = (RenderRequest)_request;
        assertNotNull(request);

        RenderResponse response = new MockRenderResponse();
        assertNotNull(response);
        
        FilterChain chain = new MockFilterChain();
        assertNotNull(chain);
                
        filter.doFilter(request,response,chain);
        
        assertNull(session.getAttribute("sessionSecret"));
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
  

}
