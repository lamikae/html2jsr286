package com.celamanzi.liferay.portlets.rails286;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

import java.net.MalformedURLException;
import java.net.URL;

import java.io.StringReader;
import java.io.StringWriter;

import org.springframework.mock.web.portlet.MockPortletSession;
import javax.portlet.PortletSession;

import org.htmlparser.util.ParserException;
import javax.xml.transform.TransformerException;
import org.xml.sax.SAXException;
import java.io.IOException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import org.cyberneko.html.parsers.DOMParser;



import com.celamanzi.liferay.portlets.rails286.PageTransformer;


/** 
 */
public class PageTransformerTest {

    private URL baseUrl = null;
    private final String host    = PortletTest.host;
    private final String servlet = PortletTest.servlet;

    private final String railsTestBenchRoute = PortletTest.railsTestBenchRoute;
    private final String railsJUnitRoute = PortletTest.railsJUnitRoute;
    private final String railsJUnitURL = PortletTest.railsJUnitURL;

    private String namespace = "__TEST_PORTLET__";

    private String html = null;
	private PortletSession session = null;
	
	@Before
	public void setTestServer()
	throws java.net.MalformedURLException {
		baseUrl = new URL(host+"/"+servlet);
		session = (PortletSession) new MockPortletSession();
		session.setAttribute("namespace", namespace);
	}

	@Test
	public void empty_document()
	throws TransformerException, SAXException, IOException {
		html = "<html></html>";
		StringWriter output = PageTransformer.transform(html,session);
		
		DOMParser parser = new DOMParser();
		parser.parse(new InputSource(new StringReader(output.toString())));
		Document doc = parser.getDocument();
		assertNotNull(doc);
		Node portlet_div = doc.getElementById(namespace+"_body");
		assertNotNull(portlet_div);
	}

	//@Test
	public void not_html()
	throws TransformerException, SAXException, IOException {
		html = "this is not HTML";
		StringWriter output = PageTransformer.transform(html,session);
		assertEquals(html,output.toString());
	}

	@Test
	public void title()
	throws TransformerException, SAXException, IOException {
		// yay for Java multiline strings...
		String html = "<html><head>" +
		"<title>Portlet title</title>" +
		"</head>" +
		"<body>" +
		"Hello world" +
		"</body></html>";
		
		StringWriter out = PageTransformer.transform(html,session);
		//System.out.println(out);
	}
    
	
	
}
