package com.celamanzi.liferay.portlets.rails286;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.tags.*;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
// import org.htmlparser.visitors.NodeVisitor;

import com.celamanzi.liferay.portlets.rails286.HeadProcessor;

public class HeadProcessorTest {

	private String host = null;
	private String servlet = "";
	private java.net.URL baseUrl = null;
	private String namespace = "__TEST_PORTLET__";

	private HeadProcessor hp = null;

	@Before
	public void setTestServer()
	throws java.net.MalformedURLException {
		host = PortletTest.host;
		baseUrl = new java.net.URL(host);
		hp = new HeadProcessor(servlet,baseUrl,namespace);
	}

	@Test
	public void test_process_title()
	throws org.htmlparser.util.ParserException, Exception
	{
		String title = "Page Title";
		String html = "<html><head>"+
			"<title>"+title+"</title>"+
			"</head></html>";

		NodeList head = TestHelpers.getHead(html);

		assertEquals("",hp.title);
		NodeList newHead = hp.process(head);
		assertEquals(title,hp.title);
	}

	@Test
	public void test_process_meta()
	throws org.htmlparser.util.ParserException, Exception
	{
		String html = "<html><head>"+
			"<meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\"></meta>"+
			"</head></html>";

		NodeList head = TestHelpers.getHead(html);

		assertNull(hp.content_type);
		assertNull(hp.encoding);
		NodeList newHead = hp.process(head);
		assertEquals("text/html",hp.content_type);
		assertEquals("UTF-8",hp.encoding);
	}

	@Test
	public void test_process_css()
	throws org.htmlparser.util.ParserException, Exception {
		String html = "<html><head>"+
		    "<link href=\"/stylesheets/portlet_test_bench/main.css\" media=\"screen\" rel=\"stylesheet\" type=\"text/css\" />"+
			"</head></html>";
	
		NodeList head = TestHelpers.getHead(html);
		NodeList newHead = hp.process(head);
	
		Pattern pattern = Pattern.compile("<link href=\"([^\"]*)");
		Matcher matcher = pattern.matcher(newHead.toHtml());
		matcher.find();
		String css_href = matcher.group(1);
		assertEquals(host+"/stylesheets/portlet_test_bench/main.css", css_href);
	}

	@Test
	public void test_process_js()
	throws org.htmlparser.util.ParserException, Exception {
		String html = "<html><head>"+
		    "<script src=\"/javascripts/prototype.js\" type=\"text/javascript\"></script>"+
			"</head></html>";
		
		NodeList head = TestHelpers.getHead(html);
		NodeList newHead = hp.process(head);
		
		Pattern pattern = Pattern.compile("<script src=\"([^\"]*)");
		Matcher matcher = pattern.matcher(newHead.toHtml());
		matcher.find();
		String js_src = matcher.group(1);
		assertEquals(host+"/javascripts/prototype.js", js_src);
	}

}
