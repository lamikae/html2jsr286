package com.celamanzi.liferay.portlets.rails286;

import java.net.URL;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

import com.celamanzi.liferay.portlets.rails286.PageProcessor;


/** Tests PageProcessor.
 */
public class PageProcessorTest {

    private URL baseUrl = null;
    private final String host    = PortletTest.host;
    private final String servlet = PortletTest.servlet;

    private final String railsTestBenchRoute = PortletTest.railsTestBenchRoute;
    private final String railsJUnitRoute = PortletTest.railsJUnitRoute;
    private final String railsJUnitURL = PortletTest.railsJUnitURL;

    private String namespace = "__TEST_PORTLET__";

    private String html = null;
    private PageProcessor pp = null;

	@Before
	public void setTestServer()
	throws java.net.MalformedURLException {
		baseUrl = new URL(host+"/"+servlet);
	}


	@Test
	public void process_empty_head() throws org.htmlparser.util.ParserException
	{
		html = "<html><head></head></html>";
		pp = new PageProcessor(html,servlet,namespace);
		String route = "/";
		String output = pp.process(baseUrl,route);
		TestHelpers.assertPageRegexp(output,"<div id=\""+namespace+"_head\">[\\n ]*</div>");
	}

	@Test
	public void process_empty_body() throws org.htmlparser.util.ParserException
	{
		html = "<html><body></body></html>";
		pp = new PageProcessor(html,servlet,namespace);
		String route = "/";
		String output = pp.process(baseUrl,route);
		// assert a new head tag..
		TestHelpers.assertPageRegexp(output,"<div id=\""+namespace+"_head\">[\\n ]*</div>");
		TestHelpers.assertPageRegexp(output,"<div id=\""+namespace+"_body\">[\\n ]*</div>");
	}

	@Test
	public void process_not_html() throws org.htmlparser.util.ParserException
	{
		html = "this is not HTML";
		pp = new PageProcessor(html,servlet);
		String route = "/";
		String output = pp.process(baseUrl,route);
		assertEquals(html,output);
	}

}
