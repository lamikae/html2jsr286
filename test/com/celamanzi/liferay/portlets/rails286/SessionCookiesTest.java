package com.celamanzi.liferay.portlets.rails286;

import org.junit.*;
import static org.junit.Assert.*;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Locale;
import java.util.Map;
import java.io.IOException;

import java.net.URL;
import java.net.MalformedURLException;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.*;
import org.apache.commons.httpclient.params.*;
import org.apache.commons.httpclient.cookie.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.mock.web.portlet.*;

import javax.portlet.*;

import com.celamanzi.liferay.portlets.rails286.Rails286Portlet;

/** Test portlet cookies that carry authorization and Liferay UID.
 */ 
public class SessionCookiesTest {

	private static final Log log = LogFactory.getLog(SessionCookiesTest.class);

	private final String host    = PortletTest.host;
	private final String servlet = PortletTest.servlet;

	private final String railsTestBenchRoute = PortletTest.railsTestBenchRoute;
	private final String railsJUnitRoute = PortletTest.railsJUnitRoute;
	private final String railsJUnitURL = PortletTest.railsJUnitURL;

	private String sessionSecret = PortletTest.sessionSecret;

	private Rails286Portlet portlet = new Rails286Portlet();
	private PortletConfig portletConfig = null;
	private PortletContext portletContext = new MockPortletContext();
	private PortletSession session = null;
	private String portletName = "__TEST__";

	@Before
	public void setup()
	throws MalformedURLException
	{
		assertNotNull(portlet);
		assertNotNull(portletContext);
		MockPortletConfig _portletConfig = new MockPortletConfig(portletContext,portletName);
		assertNotNull(_portletConfig);
		_portletConfig.addInitParameter("secret", sessionSecret);
		portletConfig = (PortletConfig)_portletConfig;

		session = new MockPortletSession();
		assertNotNull(session);

		// set session as set by RenderFilter
		session.setAttribute(
				"railsBaseUrl",
				new URL(host+"/"+servlet),
				PortletSession.PORTLET_SCOPE);

		session.setAttribute(
				"servlet",
				servlet,
				PortletSession.PORTLET_SCOPE);

		session.setAttribute(
				"railsRoute",
				null,
				PortletSession.PORTLET_SCOPE);

		session.setAttribute(
				"requestMethod",
				null,
				PortletSession.PORTLET_SCOPE);

		session.setAttribute(
				"httpReferer",
				null,
				PortletSession.PORTLET_SCOPE);


	}


	@Test
	public void test_liferay_uid()
	throws Exception
	{
		// insert session secret
		MockPortletConfig _portletConfig = new MockPortletConfig(portletContext,portletName);
		assertNotNull(_portletConfig);
		_portletConfig.addInitParameter("secret", PortletTest.sessionSecret);
		portlet.init((PortletConfig)_portletConfig);

		HttpClient client = new HttpClient();
		assertNotNull(client);
		HttpState state = new HttpState();

		String uid = "10000";
		session.setAttribute("uid",uid,PortletSession.PORTLET_SCOPE);

		Cookie[] sessionCookies = new Cookie[2];
		sessionCookies[0] = portlet.uidCookie(session);
		assertEquals(uid, sessionCookies[0].getValue());

		String url =  host+servlet+railsJUnitRoute+"/liferay_uid_auth";

		GetMethod method = new GetMethod(url);
		assertNotNull(method);
		method.getParams().setCookiePolicy(CookiePolicy.RFC_2109);
		client.getParams().setParameter("http.protocol.single-cookie-header", true);

		try {
			int statusCode = -1;
			state.addCookies(sessionCookies);
			client.setState(state);

			// Try 404 without secret cookie
			statusCode = client.executeMethod(method);
			assertEquals(403,statusCode);

			// Add secret cookie to the state and try again
			state.clearCookies();
			sessionCookies[1] = portlet.secretCookie(session);
			state.addCookies(sessionCookies);
			client.setState(state);

			statusCode = client.executeMethod(method);
			assertEquals(200, statusCode);

			byte[] responseBody = method.getResponseBody();
			String body = new String(responseBody);
			assertEquals(uid,body);

		} catch (HttpException e) {
			fail(e.getMessage());
		} catch (IOException e) {
			fail(e.getMessage());
		} finally {
			method.releaseConnection();
		}

	}    


	@Test
	/** Test  */
	public void test_cookies_liferay_auth()
	throws
	javax.xml.parsers.ParserConfigurationException,
	javax.xml.xpath.XPathExpressionException,
	Exception
	{
		// insert session secret
		MockPortletConfig _portletConfig = new MockPortletConfig(portletContext,portletName);
		assertNotNull(_portletConfig);
		_portletConfig.addInitParameter("secret", PortletTest.sessionSecret);
		portlet.init((PortletConfig)_portletConfig);

		// get foobarcookies
		session.setAttribute("railsBaseUrl",new URL(host));
		session.setAttribute("servlet",servlet);
		session.setAttribute("railsRoute",railsJUnitRoute+"/foobarcookies");

		MockRenderRequest _request = new MockRenderRequest(PortletMode.VIEW);
		_request.setSession(session);
		RenderRequest request = (RenderRequest)_request;
		assertNotNull(request);

		RenderResponse response = new MockRenderResponse();
		assertNotNull(response);

		portlet.render(request,response);
		assertEquals(200,portlet.responseStatusCode);

		// assert that cookies were stored
		// 3 from Rails, 1 with session_secret
		Cookie[] cookies = (Cookie[])session.getAttribute("cookies");
		assertNotNull(cookies);
		TestHelpers.debugCookies(cookies);
		assertEquals(6,cookies.length);
		assertNotNull(TestHelpers.getCookie("session_secret", cookies));
		assertNotNull(TestHelpers.getCookie("Liferay_preferences", cookies));
		assertNotNull(TestHelpers.getCookie("Portlet_namespace", cookies));
		assertNotNull(TestHelpers.getCookie("foo", cookies));
		assertNotNull(TestHelpers.getCookie("bar", cookies));
		assertNotNull(TestHelpers.getCookie("baz", cookies));

		HttpClient client = new HttpClient();
		assertNotNull(client);
		HttpState state = new HttpState();

		String uid = "10000";
		session.setAttribute("uid",uid,PortletSession.PORTLET_SCOPE);

		Cookie[] sessionCookies = new Cookie[6];
		// sessionCookies[0] = cookies[0]; remove session_secret
		sessionCookies[1] = cookies[1];
		sessionCookies[2] = cookies[2];
		sessionCookies[3] = cookies[3];

		sessionCookies[4] = portlet.uidCookie(session);
		assertEquals(uid, sessionCookies[4].getValue());

		String url =  host+servlet+railsJUnitRoute+"/cookies_liferay_auth";

		GetMethod method = new GetMethod(url);
		assertNotNull(method);
		method.getParams().setCookiePolicy(CookiePolicy.RFC_2109);
		client.getParams().setParameter("http.protocol.single-cookie-header", true);

		try {
			int statusCode = -1;
			state.addCookies(sessionCookies);
			client.setState(state);

			// Try 403 without secret cookie
			statusCode = client.executeMethod(method);
			assertEquals(403,statusCode);

			// Add secret cookie to the state and try again
			state.clearCookies();
			sessionCookies[5] = portlet.secretCookie(session);
			state.addCookies(sessionCookies);
			client.setState(state);

			statusCode = client.executeMethod(method);
			assertEquals(200,statusCode);

			byte[] responseBody = method.getResponseBody();
			String body = new String(responseBody);
			//System.out.println(body);

			Pattern p = Pattern.compile("__g00d____yrcl____3ver__",Pattern.MULTILINE);
			Matcher txt = p.matcher(body);
			assert(txt.find());

			p = Pattern.compile("Liferay_UID: "+uid,Pattern.MULTILINE);
			txt = p.matcher(body);
			assert(txt.find());

		} catch (HttpException e) {
			fail(e.getMessage());
		} catch (IOException e) {
			fail(e.getMessage());
		} finally {
			method.releaseConnection();
		}

	}    


}