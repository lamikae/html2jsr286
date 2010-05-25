package com.celamanzi.liferay.portlets.rails286;

import static org.junit.Assert.*;

import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.Parser;
// import org.htmlparser.tags.*;
import org.htmlparser.tags.BodyTag;
import org.htmlparser.tags.HeadTag;
import org.htmlparser.filters.NodeClassFilter;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.w3c.dom.Document;

import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathConstants;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.io.IOException;
import org.xml.sax.SAXException;

import org.xml.sax.InputSource;
import java.io.StringReader;

import org.apache.commons.httpclient.HttpException;
import java.net.MalformedURLException;
import java.net.URL;


/** Helpers */
public class TestHelpers {

	private static final String host    = PortletTest.host;
    private static final String servlet = PortletTest.servlet;
    private static final String railsTestBenchRoute = PortletTest.railsTestBenchRoute;

	protected static String getTestBench(String route)
	throws MalformedURLException, HttpException, IOException, Exception {
		OnlineClient client = new OnlineClient(
			new URL(host+servlet+railsTestBenchRoute+route));
		byte[] body = client.get();
		assertEquals(200,client.statusCode);

		return new String(body);
	}

	protected static void assertPageRegexp(String page, String regexp)
	{
		// compile a regexp
		Pattern p = Pattern.compile(regexp);
		Matcher match = p.matcher(page);
		assertTrue("\""+page+"\" did not match \""+regexp+"\"", match.find());
	}

	protected static NodeList getHead(String html)
	throws Exception
	{
		Parser   parser  = new Parser(html);
		NodeList pg = parser.parse (null);
		return pg.extractAllNodesThatMatch(new NodeClassFilter(HeadTag.class),true);
	}

	protected static NodeList getBody(String html)
	throws Exception
	{
		Parser   parser  = new Parser(html);
		NodeList pg = parser.parse (null);
		return pg.extractAllNodesThatMatch(new NodeClassFilter(BodyTag.class),true);
	}

	/**
	  */
	protected static Document html2doc(String input)
	throws SAXException, IOException, ParserConfigurationException
	{
		DocumentBuilder builder = null;
		DocumentBuilderFactory domFactory =
			DocumentBuilderFactory.newInstance();
		domFactory.setNamespaceAware(true);
		builder = domFactory.newDocumentBuilder();
		return builder.parse(
			new InputSource(new StringReader(input))
		);
	}

	/**
	  */
	protected static org.w3c.dom.NodeList evalExpr(XPathExpression expr, Document doc)
	throws Exception
	{
		Object result = expr.evaluate(doc, XPathConstants.NODESET);
		return (org.w3c.dom.NodeList) result;
	}


}