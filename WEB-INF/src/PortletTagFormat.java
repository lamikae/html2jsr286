package com.celamanzi.liferay.portlets.rails286;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class PortletTagFormat {

	private static final Log log = LogFactory.getLog(PortletTagFormat.class);

	
	public PortletTagFormat() {}

    public static Node link(Node anchor, Node session) {
		String href = anchor.getAttributes().getNamedItem("href").getNodeValue();
		log.debug(href);
		log.debug(session);
		if (href == null) {
			return anchor;
		}
		
		String base = session.getAttributes().getNamedItem("baseURL").getNodeValue();
		log.debug(base);
		
		return anchor;
	}

}
