package com.celamanzi.liferay.portlets.rails286;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import com.celamanzi.liferay.portlets.rails286.PageProcessor;

/** Tests PageProcessor.

TODO: test standard HTML4 and HTML5 tags.

*/
public class PageProcessorTest {

	private java.net.URL baseUrl = null;

	private String servlet = "";
	private String route = "/";

	private String html = null;
	private PageProcessor pp = null;

	private String namespace = "__TEST_PORTLET__";

	@Before
	public void setTestServer()
	throws java.net.MalformedURLException {
		baseUrl = new java.net.URL("http://localhost:3000");
	}

// 	@Test
// 	public void process_invalid_url() {
// 		try {
// //       String html    = OnlineUtils.getWebPage( host + servlet + path, null );
// 		String html = "<html><head></head></html>";
// 		String servlet = "";
// 		String path    = "";
// 
// 		PageProcessor p = new PageProcessor(html,servlet);
	/*	} catch (Exception e) {
			AssertionError ae = new AssertionError("");
			ae.initCause(e);
			throw ae;
		}
	*/
// 	}

	@Test
	public void process_empty_head() throws org.htmlparser.util.ParserException
	{
		html = "<html><head></head></html>";
		pp = new PageProcessor(html,servlet,namespace);
		String output = pp.process(baseUrl,route);
		assertPageRegexp(output,"<div id=\""+namespace+"_head\">[\\n ]*</div>");
	}

	@Test
	public void process_empty_body() throws org.htmlparser.util.ParserException
	{
		html = "<html><body></body></html>";
		pp = new PageProcessor(html,servlet,namespace);
		String output = pp.process(baseUrl,route);
		// assert a new head tag..
		assertPageRegexp(output,"<div id=\""+namespace+"_head\">[\\n ]*</div>");
		assertPageRegexp(output,"<div id=\""+namespace+"_body\">[\\n ]*</div>");
// 		System.out.println(output);
	}

	@Test
	public void process_not_html() throws org.htmlparser.util.ParserException
	{
		html = "this is not HTML";
		pp = new PageProcessor(html,servlet);
		String output = pp.process(baseUrl,route);
		assertEquals(html,output);
	}


	/** Helpers */

	private void assertPageRegexp(String page, String regexp)
	{
		// compile a regexp
		Pattern p = Pattern.compile(regexp);
		Matcher match = p.matcher(page);
		assertTrue("\""+page+"\" did not match \""+regexp+"\"", match.find());
	}

}
