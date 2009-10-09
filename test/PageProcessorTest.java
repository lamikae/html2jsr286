package com.celamanzi.liferay.portlets.rails286.test;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

import com.celamanzi.liferay.portlets.rails286.PageProcessor;

public class PageProcessorTest {

	private java.net.URL baseUrl = null;

	@Before
	public void setTestServer() {
		try {
			baseUrl = new java.net.URL("http://localhost:3000");
		}
		catch (java.net.MalformedURLException e) {
		}
	}

	@Test
	public void process_invalid_url() {
//       String html    = OnlineUtils.getWebPage( host + servlet + path, null );
		String html = "<html><head></head></html>";
		String servlet = "";
		String path    = "";

		PageProcessor p = new PageProcessor(html,servlet);
	}

	@Test
	public void process() {
		String servlet = "";
		String route = "/";

//       String html    = OnlineUtils.getWebPage( host + servlet + path, null );
		String html = "<html><head></head></html>";
		PageProcessor p = new PageProcessor(html,servlet);

		try {
			String output = p.process(baseUrl,route);

		} catch (Exception e) {
			AssertionError ae = new AssertionError("");
			ae.initCause(e);
			throw ae;
		}
	}

}
