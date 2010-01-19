package com.celamanzi.liferay.portlets.rails286;

import org.junit.*;
import static org.junit.Assert.*;

import java.io.*;
import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.cookie.*;
import org.apache.commons.httpclient.Header;


import org.springframework.mock.web.portlet.*;

import javax.portlet.*;

import com.celamanzi.liferay.portlets.rails286.Rails286Portlet;


public class OnlineClientTest {

  private OnlineClient client = null;
  
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
		xpath = XPathFactory.newInstance().newXPath();
    expr = null;
		nodes = null;
    client = null;
    // assert railsJUnitURL responds
  }
  
  @After
  public void teardown() {
  }
  
  @Test
  public void test_get()
  throws MalformedURLException, HttpException, IOException
  {
    client = new OnlineClient(new URL(railsJUnitURL));
    assertNotNull(client);
    
    byte[] body = client.get();
    assertEquals(200,client.statusCode);

    // no cookies
    assertEquals(0,client.cookies.length);
  }
  
  @Test
  public void test_get_cookie()
  throws
    MalformedURLException, HttpException, IOException, Exception,
    ParserConfigurationException, XPathExpressionException, SAXException
  {
    URL url = new URL(railsJUnitURL+"/session_cookie");
    client = new OnlineClient(url);
    assertNotNull(client);
    
    String sessionId = null;
    
    byte[] body = client.get();
    assertEquals(200,client.statusCode);
    
    // test cookies
    Cookie [] cookies = client.cookies;
    assertEquals(1,cookies.length);
    //System.out.println(cookies[0].toExternalForm());
    
    
    String xml = new String(body);
    //System.out.println(xml);
    Document doc = TestHelpers.html2doc(xml);
    assertNotNull(doc);
    
    expr = xpath.compile("//id/text()");
    nodes = TestHelpers.evalExpr(expr, doc);
    assertEquals(1,nodes.getLength());
    sessionId = nodes.item(0).getNodeValue();
    assertNotNull(sessionId);
    
    // GET again and assert that the session remains the same
    body = null;
    xml = null;
    doc = null;
    nodes = null;
    client = new OnlineClient(url);
    assertNull(client.cookies);
    
    // insert the cookies
    client.cookies = cookies;
    
    body = client.get();
    assertEquals(1,cookies.length);
    
    xml = new String(body);
    //System.out.println(xml);
    doc = TestHelpers.html2doc(xml);
    assertNotNull(doc);
    
    expr = xpath.compile("//id/text()");
    nodes = TestHelpers.evalExpr(expr, doc);
    assertEquals(1,nodes.getLength());
    String _sessionId = nodes.item(0).getNodeValue();
    assertNotNull(_sessionId);
    
    assertEquals(sessionId,_sessionId);
  } 
  
  @Test
  public void test_post()
  throws MalformedURLException, HttpException, IOException
  {
    client = new OnlineClient(new URL(railsJUnitURL+"/post_redirect_get"));
    assertNotNull(client);
    
    NameValuePair[] params = {
       new NameValuePair("foo", "bar")
    };
    
    byte[] body = client.post(params);
    assertEquals(200,client.statusCode);
    
    Cookie[] cookies = client.cookies;
    assertEquals(1,cookies.length);
  }
  
  
  
  
  
  
  
  
  
  
  
  
}
