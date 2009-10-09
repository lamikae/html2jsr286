package com.youleaf.jsrproxy.test;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

import javax.portlet.PortletURL;
import javax.portlet.RenderResponse;

import org.htmlparser.visitors.NodeVisitor;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.Parser;
import org.htmlparser.tags.*;
import org.htmlparser.filters.NodeClassFilter;

import com.celamanzi.liferay.portlets.rails286.BodyTagVisitor;


public class BodyTagVisitorTest {

	java.net.URL baseUrl = null;
	String servlet      = "";
	String requestPath  = "request/path";
	String documentPath = "/";
	String namespace    = "namespace";
	PortletURL portletUrl   = null;

	NodeVisitor visitor = null;

	@Before
	public void setTestServer() {
		try {
			baseUrl = new java.net.URL("http://localhost:3000");
		}
		catch (java.net.MalformedURLException e) {
		}
		visitor = new BodyTagVisitor(baseUrl, servlet, requestPath, namespace, portletUrl);
		assertNotNull(visitor);
	}


	@Test
	public void testEmptyBody() {
		String html = "<html><body></body></html>";
		NodeList body = getBody(html);

		try {
			body.visitAllNodesWith(visitor); // visit all nodes
		}
		catch (Exception e) {}

		assertEquals("<div id=\"namespace_body\"></div>",body.toHtml());
	}

	@Test
	public void testLink() {
		String html = "<html><body>";
		html += "<a href=\"/some/action\" alt=\"Action\">Action</a>";
		html += "</body></html>";
		NodeList body = getBody(html);

		try {
			body.visitAllNodesWith(visitor); // visit all nodes
		}
		catch (Exception e) {}

//		System.out.println(body.toHtml());
// 		assertEquals("<div id=\"namespace_body\"></div>",body.toHtml());
	}


	protected NodeList getBody(String html) {
		try {
			Parser   parser  = new Parser(html);
			NodeList pg = parser.parse (null);
			return pg.extractAllNodesThatMatch(new NodeClassFilter(BodyTag.class),true);
		}
		catch (Exception e) {}
		return null;
	}

}
