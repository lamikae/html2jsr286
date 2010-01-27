package com.celamanzi.liferay.portlets.rails286;

import org.junit.*;
import static org.junit.Assert.*;

import java.util.Locale;
import java.util.Map;
import java.io.IOException;

import java.net.URL;
import java.net.MalformedURLException;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.*;
import org.apache.commons.httpclient.params.*;
import org.apache.commons.httpclient.cookie.*;

import org.springframework.mock.web.portlet.*;

import javax.portlet.*;

import com.celamanzi.liferay.portlets.rails286.Rails286Portlet;

/** Tests request authorization by knowing session name and secret.
 */ 
public class AuthorizationTest {
    
    private final String host    = PortletTest.host;
    private final String servlet = PortletTest.servlet;
    
    private final String railsTestBenchRoute = PortletTest.railsTestBenchRoute;
    private final String railsJUnitRoute = PortletTest.railsJUnitRoute;
    private final String railsJUnitURL = PortletTest.railsJUnitURL;

    private String sessionSecret = PortletTest.sessionSecret;

    private Rails286Portlet portlet = new Rails286Portlet();
    private PortletConfig portletConfig = null;
    private PortletContext portletContext = new MockPortletContext();
    private PortletSession session = null;
    private String portletName = "__TEST__";

    @Before
    public void setup()
    throws MalformedURLException
    {
        assertNotNull(portlet);
        assertNotNull(portletContext);
        MockPortletConfig _portletConfig = new MockPortletConfig(portletContext,portletName);
        assertNotNull(_portletConfig);
        _portletConfig.addInitParameter("secret", sessionSecret);
        portletConfig = (PortletConfig)_portletConfig;
        
        session = new MockPortletSession();
        assertNotNull(session);
      
        // set session as set by RenderFilter
        session.setAttribute(
                             "railsBaseUrl",
                             new URL(host+"/"+servlet),
                             PortletSession.PORTLET_SCOPE);
        
        session.setAttribute(
                             "servlet",
                             servlet,
                             PortletSession.PORTLET_SCOPE);
        
        session.setAttribute(
                             "railsRoute",
                             null,
                             PortletSession.PORTLET_SCOPE);
        
        session.setAttribute(
                             "requestMethod",
                             null,
                             PortletSession.PORTLET_SCOPE);

        session.setAttribute(
                             "httpReferer",
                             null,
                             PortletSession.PORTLET_SCOPE);
        
      
    }



    @Test
    /** High level portlet authorization
    	*/
    public void test_portlet_authorization()
    throws PortletException, IOException
    {
        portlet.init(portletConfig);
        session.setAttribute(
                             "railsRoute",
                             railsJUnitRoute+"/authorized",
                             PortletSession.PORTLET_SCOPE);

        MockRenderRequest _request = new MockRenderRequest(PortletMode.VIEW);
        _request.setSession(session);
        RenderRequest request = (RenderRequest)_request;
        assertNotNull(request);
        
        RenderResponse response = new MockRenderResponse();
        assertNotNull(response);
        
        portlet.render(request,response);
        assertEquals(200,portlet.responseStatusCode);
    }


    @Test
    /** High level portlet authorization failure
    	*/
    public void test_portlet_authorization_failure()
    throws PortletException, IOException
    {
		    /* Create new config without the session secret */
        MockPortletConfig _portletConfig = new MockPortletConfig(portletContext,portletName);
        assertNotNull(_portletConfig);
    
        portlet.init((PortletConfig)_portletConfig);
        session.setAttribute(
                             "railsRoute",
                             railsJUnitRoute+"/authorized",
                             PortletSession.PORTLET_SCOPE);

        MockRenderRequest _request = new MockRenderRequest(PortletMode.VIEW);
        _request.setSession(session);
        RenderRequest request = (RenderRequest)_request;
        assertNotNull(request);
        
        RenderResponse response = new MockRenderResponse();
        assertNotNull(response);
                
        portlet.render(request,response);
        assertEquals(403,portlet.responseStatusCode);
    }


    @Test
    /** Low level authorization test.
        
     Debug by logging 
     	log4j.logger.httpclient.wire.header=DEBUG
    	*/
    public void test_authenticate_request()
    throws
        javax.xml.parsers.ParserConfigurationException,
        javax.xml.xpath.XPathExpressionException,
        Exception
    {
        HttpClient client = new HttpClient();
        assertNotNull(client);
        HttpState state = new HttpState();
        
        Cookie[] sessionCookies = new Cookie[1];
        portlet.init(portletConfig);
        sessionCookies[0] = portlet.secretCookie();

        String url =  host+servlet+railsJUnitRoute+"/authorized";

        GetMethod method = new GetMethod(url);
        assertNotNull(method);
        method.getParams().setCookiePolicy(CookiePolicy.RFC_2109);
        client.getParams().setParameter("http.protocol.single-cookie-header", true);

        try {
	        // Try 404 without cookies
          int statusCode = client.executeMethod(method);
          assertEquals(403,statusCode);

          // Add cookies to the state and try again
          state.addCookies(sessionCookies);
    			client.setState(state);
          statusCode = client.executeMethod(method);
          assertEquals(200,statusCode);
                        
        } catch (HttpException e) {
            fail(e.getMessage());
        } catch (IOException e) {
            fail(e.getMessage());
        } finally {
            method.releaseConnection();
        }
    }    

}
