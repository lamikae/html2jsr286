package com.celamanzi.liferay.portlets.rails286;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

import org.htmlparser.tags.*;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
// import org.htmlparser.visitors.NodeVisitor;

import com.celamanzi.liferay.portlets.rails286.HeadProcessor;

public class HeadProcessorTest {

	private String servlet = "";
	private java.net.URL baseUrl = null;
	private String namespace = "__TEST_PORTLET__";

	private HeadProcessor hp = null;

	@Before
	public void setTestServer()
	throws java.net.MalformedURLException {
		baseUrl = new java.net.URL("http://localhost:3000");
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

	/** TODO: test CSS and JavaScript parsing */

// 	@Test
// 	public void test_process_css()
// 	throws org.htmlparser.util.ParserException, Exception
// 	{
// 		String html = "<html><head>"+
// 			"</head></html>";
// 
// 		NodeList head = TestHelpers.getHead(html);
//
// 		System.out.println("TODO: test_process_css");
// 	}
// 
// 	@Test
// 	public void test_process_js()
// 	throws org.htmlparser.util.ParserException, Exception
// 	{
// 		String html = "<html><head>"+
// 			"</head></html>";
// 
// 		NodeList head = TestHelpers.getHead(html);
//
// 		System.out.println("TODO: test_process_js");
// 	}

}
