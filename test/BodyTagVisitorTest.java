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
	PortletURL portletUrl   = null; // TODO: instantiate PortletURL
	NodeVisitor visitor = null;

	@Before
	public void setTestServer()
	throws java.net.MalformedURLException
	{
		baseUrl = new java.net.URL("http://localhost:3000");
		visitor = new BodyTagVisitor(baseUrl, servlet, requestPath, namespace, portletUrl);
		assertNotNull(visitor);
	}


	@Test
	public void testEmptyBody()
	throws Exception {
		String html = "<html><body></body></html>";
		NodeList body = TestHelpers.getBody(html);
		body.visitAllNodesWith(visitor); // visit all nodes
		String output = body.toHtml();
		TestHelpers.assertPageRegexp(output,"<div id=\""+namespace+"_body\">[\\n ]*</div>");
	}

	/** TODO: links cannot be tested without PortletURL. */
// 	@Test
// 	public void testLink()
// 	throws Exception {
// 		String html = "<html><body>"+
// 			"<a href=\"/some/action\" alt=\"Action\">Action</a>"+
// 			"</body></html>";
// 		NodeList body = TestHelpers.getBody(html);
// 		body.visitAllNodesWith(visitor); // visit all nodes
// 		String output = body.toHtml();
// 	}


}
