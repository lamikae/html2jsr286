package com.celamanzi.liferay.portlets.rails286;

import static org.junit.Assert.*;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Header;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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

/** Helpers */
public class TestHelpers {

	private static final Log log = LogFactory.getLog(TestHelpers.class);

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

	/** Log request headers.
	
	If "name" is given, just that header is printed. Otherwise all.
	*/
	protected static void debugHeaders(Header[] headers, String name) {
		log.debug(String.format("%s: %s", name, getHeaderValue(name, headers)));
	}
	protected static void debugHeaders(Header[] headers) {
		for (Header header : headers) {
			log.debug(String.format("%s: %s", header.getName(), header.getValue()));
		}
	}

	protected static String getHeaderValue(String name, Header[] headers) {
		for (Header header : headers) {
			if (header.getName().equals(name)){
				return header.getValue();
			}
		}
		return null;
	}
	
	protected static Cookie getCookie(String name, Cookie[] cookies) {
		for (Cookie cookie : cookies) {
			if (cookie.toString().matches("^"+name+"=.*")) {
				return cookie;
			}
		}
		return null;
	}

	protected static void debugCookies(Cookie[] cookies) {
		log.debug( "Cookie inspector found "+cookies.length+" cookies ------v");
		for (Cookie cookie : cookies)
			log.debug(cookie.toString()
					+ ", domain=" + cookie.getDomain()
					+ ", path=" + cookie.getPath()
					+ ", max-age=" + cookie.getExpiryDate()
					+ ", secure=" + cookie.getSecure());
		log.debug( "----------------------------");
	}

}