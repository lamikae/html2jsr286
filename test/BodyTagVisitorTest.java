package com.celamanzi.liferay.portlets.rails286;

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
	java.net.URL baseUrl = null;
	String servlet      = "";
	String requestPath  = "request/path";
	String documentPath = "/";
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
		baseUrl = new java.net.URL("http://localhost:3000");
        
        javax.portlet.PortalContext portalContext = new MockPortalContext();
        
        MockPortletURL _portletURL = new MockPortletURL(portalContext,"render");
        MockPortletURL _actionURL  = new MockPortletURL(portalContext,"action");
        
        portletURL = (PortletURL)_portletURL;
        assertNotNull(portletURL);

        actionURL = (PortletURL)_actionURL;
        assertNotNull(actionURL);

        visitor = new BodyTagVisitor(baseUrl, servlet, requestPath, namespace, portletURL, actionURL);
		assertNotNull(visitor);
		xpath = XPathFactory.newInstance().newXPath();
		nodes = null;
        
    }


	@Test
	public void testEmptyBody()
	throws Exception {
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
	throws Exception,
        XPathExpressionException
    {
		String url = baseUrl.toString()+"/"+requestPath;
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
	throws Exception,
        XPathExpressionException
    {
		String url = baseUrl.toString()+"/"+requestPath;
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
	throws Exception,
		XPathExpressionException
	{
		String url = baseUrl.toString()+"/"+requestPath;
		String html = "<html><body>"+
			"<a href=\""+url+"\" alt=\"alt_txt\">Link text</a>"+
			"</body></html>";
		NodeList body = TestHelpers.getBody(html);
		body.visitAllNodesWith(visitor); // visit all nodes

		String output = body.toHtml();
		Document doc = TestHelpers.html2doc(body.toHtml());

		expr = xpath.compile("/div/a/@href");
		nodes = TestHelpers.evalExpr(expr, doc);
		assertEquals(1,nodes.getLength());
		//System.out.println(nodes.item(0).getNodeValue());

		expr = xpath.compile("/div/a/@alt");
		nodes = TestHelpers.evalExpr(expr, doc);
		assertEquals(1,nodes.getLength());
		assertEquals("alt_txt",nodes.item(0).getNodeValue());


// 		for (int i = 0; i < nodes.getLength(); i++) {
// 			System.out.println(nodes.item(i).getNodeValue());
// 		}
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

	public void testFormPOST() {}

	public void testFormPUT() {}

	public void testFormGET() {}


}
