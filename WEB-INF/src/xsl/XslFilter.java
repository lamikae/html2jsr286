/** Implemented from Portletbridge. Apache license?
 */
package com.celamanzi.liferay.portlets.rails286.xsl;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;

import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;
import org.xml.sax.helpers.XMLReaderFactory;

public class XslFilter extends XMLFilterImpl {

    private Templates templates;

    private TransformerHandler transformerHandler;

    private Map context = null;

    /**
     * Construct an empty XML filter, with no parent.
     * 
     * <p>
     * This filter will have no parent: you must assign a parent before you
     * start a parse or do any configuration with setFeature or setProperty.
     * </p>
     * 
     * @see org.xml.sax.XMLReader#setFeature
     * @see org.xml.sax.XMLReader#setProperty
     */
    public XslFilter(Templates templates)
            throws TransformerConfigurationException {
        this.templates = templates;
        // Use JAXP1.1 ( if possible )
        try {
            TransformerFactory tfactory = TransformerFactory.newInstance();
            // Does this factory support SAX features?
            if (tfactory.getFeature(SAXSource.FEATURE)) {
                // If so, we can safely cast.
                SAXTransformerFactory stfactory = ((SAXTransformerFactory) tfactory);
                transformerHandler = stfactory.newTransformerHandler(templates);
            } else {
                throw new TransformerConfigurationException(
                        "Can't do ChainedXslFilter because tfactory is not a SAXTransformerFactory");
            }
        } catch (javax.xml.parsers.FactoryConfigurationError ex1) {
            throw new TransformerConfigurationException(ex1.toString());
        }
    }

    /**
     * Set the parent reader.
     * 
     * <p>
     * This is the {@link org.xml.sax.XMLReader XMLReader}from which this
     * filter will obtain its events and to which it will pass its configuration
     * requests. The parent may itself be another filter.
     * </p>
     * 
     * <p>
     * If there is no parent reader set, any attempt to parse or to set or get a
     * feature or property will fail.
     * </p>
     * 
     * @param parent
     *            The parent XML reader.
     * @throws java.lang.NullPointerException
     *             If the parent is null.
     */
    public void setParent(XMLReader parent) {
        super.setParent(parent);

        if (null != parent.getContentHandler())
            this.setContentHandler(parent.getContentHandler());

        // Not really sure if we should do this here, but
        // it seems safer in case someone calls parse() on
        // the parent.
        setupParse();
    }

    /**
     * Parse a document.
     * 
     * @param input
     *            The input source for the document entity.
     * @throws org.xml.sax.SAXException
     *             Any SAX exception, possibly wrapping another exception.
     * @throws java.io.IOException
     *             An IO exception from the parser, possibly from a byte stream
     *             or character stream supplied by the application.
     * @see org.xml.sax.XMLReader#parse(org.xml.sax.InputSource)
     */
    public void parse(InputSource input) throws org.xml.sax.SAXException,
            IOException {
        if (null == getParent()) {
            XMLReader reader = null;

            // Use JAXP1.1 ( if possible )
            try {
                javax.xml.parsers.SAXParserFactory factory = javax.xml.parsers.SAXParserFactory
                        .newInstance();
                factory.setNamespaceAware(true);
                javax.xml.parsers.SAXParser jaxpParser = factory.newSAXParser();
                reader = jaxpParser.getXMLReader();

            } catch (javax.xml.parsers.ParserConfigurationException ex) {
                throw new org.xml.sax.SAXException(ex);
            } catch (javax.xml.parsers.FactoryConfigurationError ex1) {
                throw new org.xml.sax.SAXException(ex1.toString());
            } catch (NoSuchMethodError ex2) {
            } catch (AbstractMethodError ame) {
            }

            XMLReader parent;
            if (reader == null)
                parent = XMLReaderFactory.createXMLReader();
            else
                parent = reader;
            try {
                parent.setFeature(
                        "http://xml.org/sax/features/namespace-prefixes", true);
            } catch (org.xml.sax.SAXException se) {
            }
            // setParent calls setupParse...
            setParent(parent);
        } else {
            // Make sure everything is set up.
            setupParse();
        }

        if (context != null) {
            for (Iterator iter = context.entrySet().iterator(); iter.hasNext();) {
                Map.Entry entry = (Map.Entry) iter.next();
                transformerHandler.getTransformer().setParameter(
                        (String) entry.getKey(), entry.getValue());
            }
        }

        //    final ErrorListener errorListener =
        // transformerHandler.getTransformer().getErrorListener();
        //    transformerHandler.getTransformer().setErrorListener(new
        // ErrorListener() {
        //        public void error(TransformerException exception)
        //                throws TransformerException {
        //            if(errorListener != null) {
        //                errorListener.error(exception);
        //            }
        //        }
        //
        //        public void fatalError(TransformerException exception)
        //                throws TransformerException {
        //            if(errorListener != null) {
        //                errorListener.fatalError(exception);
        //            }
        //        }
        //
        //        public void warning(TransformerException exception)
        //                throws TransformerException {
        //            if(errorListener != null) {
        //                errorListener.warning(exception);
        //            }
        //        }
        //    });

        getParent().parse(input);

        //    Exception e =
        // transformerHandler.getTransformer().getExceptionThrown();
        //    if(null != e)
        //    {
        //      if(e instanceof org.xml.sax.SAXException)
        //        throw (org.xml.sax.SAXException)e;
        //      else
        //        throw new org.xml.sax.SAXException(e);
        //    }

    }

    /**
     * Parse a document.
     * 
     * @param systemId
     *            The system identifier as a fully-qualified URI.
     * @throws org.xml.sax.SAXException
     *             Any SAX exception, possibly wrapping another exception.
     * @throws java.io.IOException
     *             An IO exception from the parser, possibly from a byte stream
     *             or character stream supplied by the application.
     * @see org.xml.sax.XMLReader#parse(java.lang.String)
     */
    public void parse(String systemId) throws org.xml.sax.SAXException,
            IOException {
        parse(new InputSource(systemId));
    }

    /**
     * Set up before a parse.
     * 
     * <p>
     * Before every parse, check whether the parent is non-null, and re-register
     * the filter for all of the events.
     * </p>
     */
    private void setupParse() {
        XMLReader p = getParent();
        if (p == null) {
            throw new NullPointerException("No parent for filter");
        }

        ContentHandler ch = transformerHandler;
        //    if(ch instanceof SourceTreeHandler)
        //      ((SourceTreeHandler)ch).setUseMultiThreading(true);
        p.setContentHandler(ch);

        if (ch instanceof EntityResolver)
            p.setEntityResolver((EntityResolver) ch);
        else
            p.setEntityResolver(this);

        if (ch instanceof DTDHandler)
            p.setDTDHandler((DTDHandler) ch);
        else
            p.setDTDHandler(this);

        ErrorListener elistener = transformerHandler.getTransformer()
                .getErrorListener();
        if ((null != elistener)
                && (elistener instanceof org.xml.sax.ErrorHandler))
            p.setErrorHandler((org.xml.sax.ErrorHandler) elistener);
        else
            p.setErrorHandler(this);
    }

    /**
     * Set the content event handler.
     * 
     * @param resolver
     *            The new content handler.
     * @throws java.lang.NullPointerException
     *             If the handler is null.
     * @see org.xml.sax.XMLReader#setContentHandler
     */
    public void setContentHandler(ContentHandler handler) {
        transformerHandler.setResult(new SAXResult(handler));
    }

    public void setErrorListener(ErrorListener handler) {
        transformerHandler.getTransformer().setErrorListener(handler);
    }

    public Map getContext() {
        return context;
    }

    public void setContext(Map context) {
        this.context = context;
    }
}