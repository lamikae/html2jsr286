package com.celamanzi.liferay.portlets.rails286;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;


/**
 * Created by IntelliJ IDEA.
 * User: BKirillov
 * Date: 27.09.2010
 * Time: 13:55:02
 * To change this template use File | Settings | File Templates.
 */
public class Trans {

    private String result;
    private String head;

    public Trans() {}

    public Trans(String xsl, String input)
            throws IOException, TransformerException {

        TransformerFactory tFactory = TransformerFactory.newInstance();

        Transformer transformer = tFactory.newTransformer(new StreamSource(new FileInputStream(xsl)));

        StringReader sr = new StringReader(cutHead(replaceHTML(input)));
        StringWriter sw = new StringWriter(3000);

        transformer.transform(new StreamSource(sr), new StreamResult(sw));

        String trans = reReplaceHTML(sw.getBuffer().toString());

        StringBuilder sb = new StringBuilder("");

        sb.append(head);
        sb.append(trans);

        result = sb.toString();
    }

    public String getResult() {
        return result;
    }

    private String replaceHTML(String html) {
        return html.replace("&", "|");
    }

    private String reReplaceHTML(String html) {
        html = html.replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", "");
        return html.replace("|", "&");
    }

    private String cutHead(String html) {

        int start = html.indexOf("<div id=");
        int finish = html.indexOf("</div>");

        head = html.substring(start, finish+6);

        return html.substring(finish+6, html.length());
    }

}