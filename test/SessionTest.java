  package com.celamanzi.liferay.portlets.rails286;

import org.junit.*;
import static org.junit.Assert.*;

import java.io.*;
import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;

import javax.portlet.*;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.cookie.*;

import org.springframework.mock.web.portlet.*;


import com.celamanzi.liferay.portlets.rails286.Rails286Portlet;


public class SessionTest {
    
    private Rails286Portlet portlet = new Rails286Portlet();
    private PortletConfig portletConfig = null;
    private PortletContext portletContext = new MockPortletContext();
    private PortletSession session = null;
    private String portletName = "__TEST__";
    
    private final String host    = PortletTest.host;
    private final String servlet = PortletTest.servlet;
    
    private final String railsTestBenchRoute = PortletTest.railsTestBenchRoute;
    private final String railsJUnitRoute = PortletTest.railsJUnitRoute;
    private final String railsJUnitURL = PortletTest.railsJUnitURL;

    private XPath xpath = null;
    private XPathExpression expr = null;
    private NodeList nodes = null;
    
    
    
    @Before
    public void setup() {
        assertNotNull(portlet);
        assertNotNull(portletContext);
        MockPortletConfig _portletConfig = new MockPortletConfig(portletContext,portletName);
        assertNotNull(_portletConfig);
        portletConfig = (PortletConfig)_portletConfig;
        
        session = new MockPortletSession();
        assertNotNull(session);
        
        xpath = XPathFactory.newInstance().newXPath();
        expr = null;
        nodes = null;
        // assert railsJUnitURL responds
    }

    @After
    public void teardown() {
    }
    
    @Test
    public void test_session_cookie()
    throws 
    Exception, IOException, PortletException,
    ParserConfigurationException,
    XPathExpressionException
    {
        portlet.init(portletConfig);
    
        session.setAttribute("railsBaseUrl",new URL(host));
        session.setAttribute("servlet",servlet);
        session.setAttribute("railsRoute",railsJUnitRoute+"/session_cookie");
        
        String sessionId = null;
                
        MockRenderRequest _request = new MockRenderRequest(PortletMode.VIEW);
        _request.setSession(session);
        RenderRequest request = (RenderRequest)_request;
        assertNotNull(request);
        
        RenderResponse response = new MockRenderResponse();
        assertNotNull(response);
        
        portlet.render(request,response);
      
        // assert that cookies were stored
        Cookie[] cookies = (Cookie[])session.getAttribute("cookies");
        assertNotNull(cookies);
        assertEquals(1,cookies.length);
        
        // re-cast
        MockRenderResponse _response = (MockRenderResponse)response;
        
        String xml = _response.getContentAsString();
        //System.out.println(xml);
        Document doc = TestHelpers.html2doc(xml);
        assertNotNull(doc);
        
        expr = xpath.compile("//id/text()");
        nodes = TestHelpers.evalExpr(expr, doc);
        assertEquals(1,nodes.getLength());
        sessionId = nodes.item(0).getNodeValue();
        assertNotNull(sessionId);
        
        // render again and assert that the session remains the same

        response = new MockRenderResponse();
        assertNotNull(response);
        
        portlet.render(request,response);
        
        // re-cast to use mock method getContentAsString()
        _response = (MockRenderResponse)response;
        
        xml = _response.getContentAsString();
        //System.out.println(xml);
        doc = TestHelpers.html2doc(xml);
        assertNotNull(doc);
        
        expr = xpath.compile("//id/text()");
        nodes = TestHelpers.evalExpr(expr, doc);
        assertEquals(1,nodes.getLength());
        String _sessionId = nodes.item(0).getNodeValue();
        //System.out.println(sessionId);
        //System.out.println(_sessionId);
        
        assertEquals(sessionId,_sessionId);
        
	}

  
  //@Test
  // FIXME: cannot be tested until redirect has been tested
  public void test_cookies_with_redirect()
  throws 
  Exception, IOException, PortletException,
  ParserConfigurationException,
  XPathExpressionException
  {
    portlet.init(portletConfig);
    
    session.setAttribute("railsBaseUrl",new URL(host));
    session.setAttribute("servlet",servlet);
    session.setAttribute("railsRoute",railsJUnitRoute+"/cookies_with_redirect");
    
    String sessionId = null;
    
    MockRenderRequest _request = new MockRenderRequest(PortletMode.VIEW);
    _request.setSession(session);
    RenderRequest request = (RenderRequest)_request;
    assertNotNull(request);
    
    RenderResponse response = new MockRenderResponse();
    assertNotNull(response);
    
    portlet.render(request,response);
    
    // assert that cookies were stored
    Cookie[] cookies = (Cookie[])session.getAttribute("cookies");
    assertNotNull(cookies);
    assertEquals(2,cookies.length);

    // re-cast
    MockRenderResponse _response = (MockRenderResponse)response;
    
    String xml = _response.getContentAsString();
    System.out.println(xml);
    /*
    Document doc = TestHelpers.html2doc(xml);
    assertNotNull(doc);
    
    expr = xpath.compile("//id/text()");
    nodes = TestHelpers.evalExpr(expr, doc);
    assertEquals(1,nodes.getLength());
    sessionId = nodes.item(0).getNodeValue();
    assertNotNull(sessionId);
    */
    // render again and assert that the session remains the same
    
    response = new MockRenderResponse();
    assertNotNull(response);
    
    portlet.render(request,response);
    
    // re-cast to use mock method getContentAsString()
    _response = (MockRenderResponse)response;
    
    xml = _response.getContentAsString();
    //System.out.println(xml);
    /*
    doc = TestHelpers.html2doc(xml);
    assertNotNull(doc);
    
    expr = xpath.compile("//id/text()");
    nodes = TestHelpers.evalExpr(expr, doc);
    assertEquals(1,nodes.getLength());
    String _sessionId = nodes.item(0).getNodeValue();
    //System.out.println(sessionId);
    //System.out.println(_sessionId);
    
    assertEquals(sessionId,_sessionId);
    */
	}
  
}
