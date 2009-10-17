package com.celamanzi.liferay.portlets.rails286;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

import com.celamanzi.liferay.portlets.rails286.RouteAnalyzer;

public class RouteAnalyzerTest {

	private java.net.URL baseUrl = null;
	private String servlet = null;

	@Before
	public void setTestServer()
	throws java.net.MalformedURLException
	{
		baseUrl = new java.net.URL("http://localhost:3000");
		servlet = "__servlet__";
	}


	@Test
	public void test_getRequestRoute()
	throws java.net.MalformedURLException
	{
		String path  = null;
		String url   = null;
		String route = null;

		// RouteAnalyzer without servlet
		RouteAnalyzer ra = new RouteAnalyzer(baseUrl); // no servlet
		assertNotNull(ra);
		//  -"- with servlet
		RouteAnalyzer ra_srvl = new RouteAnalyzer(baseUrl,servlet);
		assertNotNull(ra_srvl);

		String[] paths = {
			"/",
			"/a/b/c",
			"/a/b/c?d=e"
		};

		for(int i=0;i<paths.length;i++){
			path  = paths[i]; // includes query parameters
			System.out.println("Path: "+path);

			// TODO: both tests with and without baseUrl

			// test without servlet
			// url:   http://baseUrl/path
			url = baseUrl.toString() + path;
			route = ra.getRequestRoute(url);
			assertNotNull(route);
			assertEquals(path,route);

			route = null;

			// test with servlet
			// url:   http://baseUrl/servlet/path
			url = baseUrl.toString() + "/" + servlet + path;
			route = ra_srvl.getRequestRoute(url);
			assertNotNull(route);
			assertEquals(path,route);

			route = null;

		}

		/* special cases */

		// test empty
		route = ra.getRequestRoute("");
		assertNotNull(route);
		assertEquals("",route);

		route = ra_srvl.getRequestRoute("");
		assertNotNull(route);
		assertEquals("",route);

		// plain baseurl (no slash)
		route = ra.getRequestRoute(baseUrl.toString());
		assertNotNull(route);
		assertEquals("/",route);

		route = ra_srvl.getRequestRoute(baseUrl.toString()+"/"+servlet);
		assertNotNull(route);
		assertEquals("/",route);

		// url:   (relative file)
		// route: index.html
		path = "index.html";
		route = ra.getRequestRoute(path);
		assertNotNull(route);
		assertEquals(path,route);

		// how does relative file behave in servlet?
		// -- how is the link used?

	}

	public void test_getFullURL() {}

}
