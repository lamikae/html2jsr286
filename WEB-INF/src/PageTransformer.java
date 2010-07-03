package com.celamanzi.liferay.portlets.rails286;

import java.io.IOException;
import java.io.StringWriter;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import javax.portlet.PortletSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/** XXX: Singleton
 */
public class PageTransformer {

	private Transformer transformer = null;

	private static final String[] params = { "namespace", "location", "query", "base" };

	private static final Log log = LogFactory.getLog(PageTransformer.class);

    /**
     * Define available XSLT stylesheets.
     *
     * Private so users do not create an instance of the singleton.
     */
    private PageTransformer()
    throws TransformerConfigurationException {
		transformer = loadXsl("body").newTransformer();
		// the stylesheet contains the "format" namespace
		// for tag format functions.
	}
    
	private Templates loadXsl(String sheet)
	throws TransformerConfigurationException {
		URL resourceUrl = getClass().getResource(
			"/com/celamanzi/liferay/portlets/rails286/xsl/"+sheet+".xsl");
		log.debug(resourceUrl.toString());
		Templates result = TransformerFactory.newInstance().newTemplates(
			new StreamSource(resourceUrl.toExternalForm()));
		log.debug(result);
		return result;
	}

	private static PageTransformer ref;

	public static synchronized PageTransformer getInstance()
	throws TransformerConfigurationException
	{
		if (ref == null)
			ref = new PageTransformer();
		return ref;
	}

	public Object clone()
	throws CloneNotSupportedException {
		throw new CloneNotSupportedException(); 
	}

	/**
	 * Transforms the HTML from a downstream site using a configured XSL
	 * stylesheet.
	 * 
	 * @param html
	 *            the http result from calling the downstream site.
	 * @param session
	 *            the portlet session
	 */
	public static StringWriter transform(String html, PortletSession session)
	throws TransformerException, SAXException, IOException {
		PageTransformer instance = PageTransformer.getInstance();
        StringWriter responseWriter = new StringWriter();

		Transformer transformer = instance.transformer;
		/** XXX: append metadata from portlet-session
		for (String param : params) {
			String attr = (String) session.getAttribute(param);
			if (attr != null) {
				log.debug(param+": "+attr);
				transformer.setParameter(param,attr);
			}
		}
		 */
		try {
			Document doc = html2doc("<portlet-session />");
			log.debug(doc);
			Node session_node = (Node)doc.getFirstChild();
			log.debug(session_node);
			transformer.setParameter("session", session_node);
			
			SAXSource src = new SAXSource(new InputSource(new StringReader(html)));
			transformer.transform(src, new StreamResult(responseWriter));
			return responseWriter;
		}
		catch (Exception e) {
			log.error(e.getMessage());
			return null;
		}
	}

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
	
}

