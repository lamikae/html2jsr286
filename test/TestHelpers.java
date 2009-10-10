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

/** Helpers */
public class TestHelpers {

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

}