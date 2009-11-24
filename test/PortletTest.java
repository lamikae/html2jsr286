package com.celamanzi.liferay.portlets.rails286;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Locale;
import java.util.Map;

//import junit.framework.TestCase;

//import org.springframework.web.portlet.context.*;
//import org.springframework.mock.web.portlet.MockPortletRequest;
import org.springframework.mock.web.portlet.*;


import javax.portlet.*;

import com.celamanzi.liferay.portlets.rails286.Rails286Portlet;
//import com.celamanzi.liferay.portlets.rails286.Rails286Portlet;

public class PortletTest {

    @Test
    public void test_instance()
    throws PortletException
    {
        PortletContext portletContext = new MockPortletContext();
        assertNotNull(portletContext);
        PortletConfig portletConfig = new MockPortletConfig(portletContext);
        assertNotNull(portletConfig);

        Rails286Portlet portlet = new Rails286Portlet();
        assertNotNull(portlet);
        //System.out.println(portlet);
        
        portlet.init(portletConfig);

    }
    
    
	@Test
	public void test_render() {
        
        MockPortletRequest portletRequest = new MockPortletRequest();
        portletRequest.addParameter("param1", "value1");
        portletRequest.addParameter("param2", "value2");
        portletRequest.addParameter("param2", "value2a");
        
        PortletRequest pr = (PortletRequest)portletRequest;
        assertNotNull(pr);
        //System.out.println(pr);
	}

}
