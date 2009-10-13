package com.celamanzi.liferay.portlets.rails286;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

import com.celamanzi.liferay.portlets.rails286.RouteAnalyzer;

public class RouteAnalyzerTest {

	private RouteAnalyzer ra = null;
	private java.net.URL baseUrl = null;
	private String servlet = null;

	@Before
	public void setTestServer()
	throws java.net.MalformedURLException
	{
		baseUrl = new java.net.URL("http://localhost:3000");
	}


	@Test
	public void test_getRequestRoute()
	throws java.net.MalformedURLException
	{
		String path = null;
		String href = null;
		String route = null;

		ra = new RouteAnalyzer(baseUrl,null); // no servlet
		assertNotNull(ra);

		// url:   (empty)
		// route: (empty)
		route = ra.getRequestRoute("");
		assertNotNull(route);
		assertEquals("",route);

		// url:   http://baseUrl
		// route: /
		href = baseUrl.toString();
		route = ra.getRequestRoute(href);
		assertNotNull(route);
		assertEquals("/",route);

		// url:   http://baseUrl/
		// route: /
		href = baseUrl.toString() + "/";
		route = ra.getRequestRoute(href);
		assertNotNull(route);
		assertEquals("/",route);

		// url:   /
		// route: /
		href = "/";
		route = ra.getRequestRoute(href);
		assertNotNull(route);
		assertEquals("/",route);

		// url:   http://baseUrl/a/b/c?d=e
		// route: /a/b/c
		path = "/a/b/c?d=e";
		href = baseUrl.toString() + path;
		route = ra.getRequestRoute(href);
		assertNotNull(route);
		assertEquals("/a/b/c",route);

		// url:   /a/b/c?d=e
		// route: /a/b/c
		href = "/a/b/c?d=e";
		route = ra.getRequestRoute(href);
		assertNotNull(route);
		assertEquals("/a/b/c",route);

		// url:   a/b/c?d=e
		// route: a/b/c
		href = "a/b/c?d=e";
		route = ra.getRequestRoute(href);
		assertNotNull(route);
		assertEquals("a/b/c",route);

		// url:   (relative file)
		// route: index.html
		path = "index.html";
		route = ra.getRequestRoute(path);
		assertNotNull(route);
		assertEquals(path,route);

	}

	@Test
	public void test_getRequestRouteWithServlet()
	throws java.net.MalformedURLException
	{
		String path = null;
		String href = null;
		String route = null;

		servlet = "__servlet__";
		ra = new RouteAnalyzer(baseUrl,servlet);
		assertNotNull(ra);

		// url:   (empty)
		// route: (empty)
		route = ra.getRequestRoute("");
		assertNotNull(route);
		assertEquals("",route);

		// url:   http://baseUrl/servlet
		// route: /
		href = baseUrl.toString() + "/"+servlet;
		route = ra.getRequestRoute(href);
		assertNotNull(route);
		assertEquals("/",route);

		// url:   http://baseUrl/servlet/
		// route: /
		href = baseUrl.toString() + "/"+servlet+"/";
		route = ra.getRequestRoute(href);
		assertNotNull(route);
		assertEquals("/",route);

		// url:   /servlet
		// route: /
		href = "/"+servlet;
		route = ra.getRequestRoute(href);
		assertNotNull(route);
		assertEquals("/",route);

		// url:   /servlet/
		// route: /
		href = "/"+servlet+"/";
		route = ra.getRequestRoute(href);
		assertNotNull(route);
		assertEquals("/",route);

		// url:   http://baseUrl/servlet/a/b/c?d=e
		// route: /a/b/c
		path = "/a/b/c?d=e";
		href = baseUrl.toString() + "/"+servlet + path;
		route = ra.getRequestRoute(href);
		assertNotNull(route);
		assertEquals("/a/b/c",route);

		// url:   /a/b/c?d=e
		// route: /a/b/c
		path = "/a/b/c?d=e";
		href = "/"+servlet + path;
		route = ra.getRequestRoute(href);
		assertNotNull(route);
		assertEquals("/a/b/c",route);

	}

	public void test_getFullURL() {}

}
