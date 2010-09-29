package com.celamanzi.liferay.portlets.rails286;

import javax.xml.transform.TransformerException;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: BKirillov
 * Date: 27.09.2010
 * Time: 16:56:54
 * To change this template use File | Settings | File Templates.
 */
public class TransformImpl {

    private String result;

    public TransformImpl() {}

    public TransformImpl(String body) throws TransformerException, IOException {

        body = body.replace("&copy;", "");

        Trans tr = new Trans("D:/xsl/body.xsl", body);

        result = tr.getResult();

    }

    public String getResult() {
        return result;
    }

}
