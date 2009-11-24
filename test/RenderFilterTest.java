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
    
    Rails286PortletRenderFilter filter = new Rails286PortletRenderFilter();
    FilterConfig filterConfig = null;
    PortletContext portletContext = null;
    PortletSession session = null;
    
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

}
