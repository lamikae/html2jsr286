package com.celamanzi.liferay.portlets.rails286;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.NameValuePair;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


public class OnlineClientTest {

	private OnlineClient client = null;

	private final String host    = PortletTest.host;
	private final String railsJUnitURL = PortletTest.railsJUnitURL;

	private XPath xpath = null;
	private XPathExpression expr = null;
	private NodeList nodes = null;

	@Before
	public void setup() {
		xpath = XPathFactory.newInstance().newXPath();
		expr = null;
		nodes = null;
		client = null;
		// assert railsJUnitURL responds
	}

	@After
	public void teardown() {
	}

	@Test
	public void test_get() throws Exception {
		client = new OnlineClient(new URL(railsJUnitURL+"/empty"));
		assertNotNull(client);

		byte[] body = client.get();
		assertNotNull(body);
		assertEquals(200, client.getStatusCode());

		// no cookies
		assertEquals(0, client.getCookies().length);
	}

	@Test
	public void test_get_cookie() throws Exception {
		URL url = new URL(railsJUnitURL+"/session_cookie");
		client = new OnlineClient(url);
		assertNotNull(client);

		String sessionId = null;

		byte[] body = client.get();
		assertEquals(200,client.getStatusCode());

		// test cookies
		Cookie [] cookies = client.getCookies();
		assertEquals(1,cookies.length);
		//System.out.println(cookies[0].toExternalForm());


		String xml = new String(body);
		//System.out.println(xml);
		Document doc = TestHelpers.html2doc(xml);
		assertNotNull(doc);

		expr = xpath.compile("//id/text()");
		nodes = TestHelpers.evalExpr(expr, doc);
		assertEquals(1,nodes.getLength());
		sessionId = nodes.item(0).getNodeValue();
		assertNotNull(sessionId);

		// GET again and assert that the session remains the same
		body = null;
		xml = null;
		doc = null;
		nodes = null;
		client = new OnlineClient(url);
		assertNull(client.getCookies());

		// insert the cookies
		client.setCookies(cookies);

		body = client.get();
		assertEquals(1,cookies.length);

		xml = new String(body);
		//System.out.println(xml);
		doc = TestHelpers.html2doc(xml);
		assertNotNull(doc);

		expr = xpath.compile("//id/text()");
		nodes = TestHelpers.evalExpr(expr, doc);
		assertEquals(1,nodes.getLength());
		String _sessionId = nodes.item(0).getNodeValue();
		assertNotNull(_sessionId);

		assertEquals(sessionId,_sessionId);
	} 

	@Test
	public void test_post_redirect()
	throws MalformedURLException, HttpException, IOException, RailsAppException
	{
		client = new OnlineClient(new URL(railsJUnitURL+"/post_redirect_get"));
		assertNotNull(client);

		NameValuePair[] params = {};

		byte[] body = client.post(params,null); //without params or files
		assertEquals(200,client.getStatusCode());

		Cookie[] cookies = client.getCookies();
		assertEquals(0,cookies.length);

		assertEquals("/caterpillar/test_bench/junit/redirect_target",new String(body));
	}

	@Test
	public void test_post_params()
	throws MalformedURLException, HttpException, IOException, Exception,
	ParserConfigurationException, XPathExpressionException, SAXException
	{
		client = new OnlineClient(new URL(railsJUnitURL+"/post_params"));
		assertNotNull(client);

		NameValuePair[] params = {
				new NameValuePair("foo", "bar"),
				new NameValuePair("baz", "xyz"),
		};

		byte[] body = client.post(params, null); //without files
		assertEquals(200,client.getStatusCode());

		Cookie[] cookies = client.getCookies();
		assertEquals(0,cookies.length);

		String xml = new String(body);
		Document doc = TestHelpers.html2doc(xml);
		assertNotNull(doc);

		for (int x=0 ; x<params.length ; x++) {
			String k = params[x].getName();
			expr = xpath.compile("//"+k+"/text()");
			nodes = TestHelpers.evalExpr(expr, doc);
			assertEquals(1,nodes.getLength());
			assertEquals(params[x].getValue(),nodes.item(0).getNodeValue());
		}
	}

	@Test
	public void test_post_cookies()
	throws MalformedURLException, HttpException, IOException, Exception,
	ParserConfigurationException, XPathExpressionException, SAXException
	{
		Map<String, Cookie> cookies = new HashMap<String, Cookie>();
		cookies.put("1",new Cookie(
				new URL(host).getHost(),
				"foo",
				"bar",
				"/",
				null,
				false));
		cookies.put("2",new Cookie(
				new URL(host).getHost(),
				"baz",
				"xyz",
				"/",
				null,
				false));
		assertEquals(2,cookies.size());

		client = new OnlineClient(new URL(railsJUnitURL+"/post_cookies"),
				cookies,null,null,false);
		assertNotNull(client);

		NameValuePair[] params = {};

		// server adds another cookie
		byte[] body = client.post(params, null); //without files
		assertNotNull(body);
		assertEquals(200, client.getStatusCode());

		Cookie[] _cookies = client.getCookies();
		assertEquals(3,_cookies.length);
		String xml = new String(body);

		Document doc = TestHelpers.html2doc(xml);
		assertNotNull(doc);

		for (java.util.Map.Entry<String,Cookie> entry : cookies.entrySet()) {
			Cookie cookie = entry.getValue();
			String key = cookie.getName();
			String value = cookie.getValue();
			
			expr = xpath.compile("//"+key+"/text()");
			nodes = TestHelpers.evalExpr(expr, doc);
			
			assertEquals(1,nodes.getLength());
			assertEquals(value, nodes.item(0).getNodeValue());
		}
	}

	@Test
	public void test_multipart_post()
	throws MalformedURLException, HttpException, IOException, RailsAppException{

		client = new OnlineClient(new URL(railsJUnitURL+"/upload_image"));
		assertNotNull(client);

		NameValuePair[] params = {
				new NameValuePair("normal_param", "importância") //require UTF-8 encode
		};

		File file = new File("test/resources/jake_sully.jpg");
		byte[] bytes = getBytes(file);

		// This is required to not lose the original file (just for test purpose)
		file = new File("test/resources/jake_sully_test.jpg");

		Map<String, Object[]> files = new HashMap<String, Object[]>();
		files.put("file_param", new Object[]{file, bytes});

		byte[] body = client.post(params, files);
		assertEquals(200,client.getStatusCode());

		assertEquals("", new String(body));
	}

	@Test
	/** Test that Ajax request is received as XHR by Rails. **/
	public void test_xhr()
	throws MalformedURLException, HttpException, IOException, RailsAppException {
		client = new OnlineClient(new URL(railsJUnitURL+"/check_xhr"),null,null,null,true);
		assertNotNull(client);
		byte[] body = client.post(null,null);
		assertEquals(200,client.getStatusCode());
		assertEquals("true", new String(body));
	}

	public static byte[] getBytes(File file) throws IOException {
		byte[] bytes = new byte[(int) file.length()];

		FileInputStream fis = new FileInputStream(file);
		int b = 0;
		int x = 0;
		while((b = fis.read()) != -1){
			bytes[x++] = (byte)b;
		}
		return bytes;
	}

}
