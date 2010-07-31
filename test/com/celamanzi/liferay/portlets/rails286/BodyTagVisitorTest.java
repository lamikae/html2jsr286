package com.celamanzi.liferay.portlets.rails286;

import java.net.URL;
import java.net.URLEncoder;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

import javax.portlet.PortletURL;
import javax.portlet.RenderResponse;

import org.htmlparser.visitors.NodeVisitor;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.tags.*;

import org.w3c.dom.Document;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.springframework.mock.web.portlet.*;

import com.celamanzi.liferay.portlets.rails286.BodyTagVisitor;


/** Tests BodyTagVisitor. This class alters the HTML body tags.

Instantiates one Visitor and test with various HTML tags how it behaves in unique isolated cases.

TODO: test standard HTML4 and HTML5 tags.

*/
public class BodyTagVisitorTest {
    private URL baseUrl = null;
    private final String host    = PortletTest.host;
    private final String servlet = PortletTest.servlet;

    private final String railsTestBenchRoute = PortletTest.railsTestBenchRoute;
    private final String railsJUnitRoute = PortletTest.railsJUnitRoute;
    private final String railsJUnitURL = PortletTest.railsJUnitURL;

    private String namespace = "__TEST_PORTLET__";

    PortletURL portletURL   = null;
    PortletURL actionURL    = null; 
    NodeVisitor visitor = null;
    XPath xpath = null;
    XPathExpression expr = null;
    org.w3c.dom.NodeList nodes = null;


	@Before
	public void setTestServer()
	throws java.net.MalformedURLException
	{
		baseUrl = new URL(host+"/"+servlet);
        
        javax.portlet.PortalContext portalContext = new MockPortalContext();
        
        MockPortletURL _portletURL = new MockPortletURL(portalContext,"render");
        MockPortletURL _actionURL  = new MockPortletURL(portalContext,"action");
        
        portletURL = (PortletURL)_portletURL;
        assertNotNull(portletURL);

        actionURL = (PortletURL)_actionURL;
        assertNotNull(actionURL);

        visitor = new BodyTagVisitor(baseUrl, servlet, railsJUnitRoute, namespace, portletURL, actionURL);
		assertNotNull(visitor);
		xpath = XPathFactory.newInstance().newXPath();
		nodes = null;
	}


	@Test
	public void testEmptyBody()
	throws Exception
	{
		String html = "<html><body></body></html>";
		NodeList body = TestHelpers.getBody(html);
		body.visitAllNodesWith(visitor); // visit all nodes
		String output = body.toHtml();

		Document doc = TestHelpers.html2doc(body.toHtml());

		expr = xpath.compile("/div/@id");
		nodes = TestHelpers.evalExpr(expr, doc);
		assertEquals(1,nodes.getLength());
		assertEquals(namespace+"_body",nodes.item(0).getNodeValue());
	}

	/** Test links.

	TODO: link formation cannot be tested properly without PortletURL.
	*/

	@Test
	public void testLinkExitPortletParameterGET()
	throws Exception, XPathExpressionException
	{
		String url = railsJUnitURL;
        String url_with_extra_param = url+"?exit_portlet=true";
		String html = "<html><body>"+
        "<a href=\""+url_with_extra_param+"\" alt=\"alt_txt\">Link text</a>"+
        "</body></html>";
		NodeList body = TestHelpers.getBody(html);
		body.visitAllNodesWith(visitor); // visit all nodes
        
		String output = body.toHtml();
		Document doc = TestHelpers.html2doc(body.toHtml());
        
		expr = xpath.compile("/div/a/@href");
		nodes = TestHelpers.evalExpr(expr, doc);
		assertEquals(1,nodes.getLength());
        assertEquals(url,nodes.item(0).getNodeValue());
	}

	@Test
	public void testLinkExitPortletParameterPOST()
	throws Exception, XPathExpressionException
	{
		String url = railsJUnitURL;
        String url_with_extra_param = url+"?exit_portlet=true";
        String form = "<form action=\""+url_with_extra_param+"\" enctype=\"multipart/form-data\" "+
            "id=\"form_id\" method=\"post\" onsubmit=\"someJavaScript(); return true;\">"+
            "</form>";
        
        String html = "<html><body>" +form+ "</body></html>";
        
		NodeList body = TestHelpers.getBody(html);
		body.visitAllNodesWith(visitor); // visit all nodes
        
		String output = body.toHtml();
		Document doc = TestHelpers.html2doc(body.toHtml());

		expr = xpath.compile("/div/form/@action");
		nodes = TestHelpers.evalExpr(expr, doc);
		assertEquals(1,nodes.getLength());
        assertEquals(url,nodes.item(0).getNodeValue());
	}

	public void testLinkAmpersandAndSlashValidity() {}

	@Test
	public void testLinkHTTP()
	throws Exception, XPathExpressionException
	{
		String url = railsJUnitURL; // full url
		String html = "<html><body>"+
			"<a href=\""+url+"\" alt=\"alt_txt\">Link text</a>"+
			"</body></html>";
		NodeList body = TestHelpers.getBody(html);
		body.visitAllNodesWith(visitor); // visit all nodes

		String output = body.toHtml();
		Document doc = TestHelpers.html2doc(body.toHtml());

		expr = xpath.compile("//a/@href");
		nodes = TestHelpers.evalExpr(expr, doc);
		assertEquals(1,nodes.getLength());
		// host should not be passed to route!
		
		//System.out.println(nodes.item(0).getNodeValue());
		Pattern pattern = Pattern.compile("railsRoute=(.*)");
		Matcher matcher = pattern.matcher(nodes.item(0).getNodeValue());
		matcher.find();
		String _route = matcher.group(1);
		assertEquals(URLEncoder.encode(PortletTest.railsJUnitRoute, "UTF-8"), _route);

		expr = xpath.compile("//a/@alt");
		nodes = TestHelpers.evalExpr(expr, doc);
		assertEquals(1,nodes.getLength());
		assertEquals("alt_txt",nodes.item(0).getNodeValue());
	}

	// skip Ajax links with href="#"
	public void testLinkAjax() {}

	// the link might be Ajax '#', or plain "some_file.htm(l)",
	// that will raise MalformedURLException.
	public void testLinkProtocolless() {}

	public void testLinkOnclickJavaScript() {}

	public void testLinkOnclickJavaScriptForm() {}

	public void testLinkTarget() {}


	/** Test images.
	*/

	public void testImageAbsolute() {}

	public void testImageRelative() {}


	/** Test forms.
	*/

	public void testFormWithoutActionUrl() {}

    @Test
    public void testFormPOST()
    throws Exception, XPathExpressionException
    {
        // yay for Java multiline strings...
        String html = "<html><body>"+
        "<form action=\"/caterpillar/test_bench/http_methods/post\" method=\"post\">"+
        "  <p>"+
        "    <input id=\"msg\" name=\"msg\" size=\"42\" type=\"text\" value=\"jääneekö unicode matkalle..\" />"+
        "  </p>"+
        "</form>"+
        "</body></html>";

        NodeList body = TestHelpers.getBody(html);
        body.visitAllNodesWith(visitor); // visit all nodes
        //System.out.println(body.toHtml());

        Document doc = TestHelpers.html2doc(body.toHtml());
        // actionURL
        expr = xpath.compile("//form/@action");
        nodes = TestHelpers.evalExpr(expr, doc);
        assertEquals(1,nodes.getLength());
        assertEquals("http://localhost/mockportlet?urlType=action",nodes.item(0).getNodeValue());

        // actual form action URL + method
        nodes = doc.getElementsByTagName("input");
        assertEquals(3,nodes.getLength());
        assertEquals("msg", nodes.item(0).getAttributes().getNamedItem("name").getNodeValue());
        assertEquals(
             namespace+"originalActionUrl",
             nodes.item(1).getAttributes().getNamedItem("name").getNodeValue());
        assertEquals(
             "caterpillar/test_bench/http_methods/post",
             nodes.item(1).getAttributes().getNamedItem("value").getNodeValue());
    }

	public void testFormPUT() {}

	public void testFormGET() {}


}
