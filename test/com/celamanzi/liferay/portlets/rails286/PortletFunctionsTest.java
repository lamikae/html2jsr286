package com.celamanzi.liferay.portlets.rails286;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.portlet.PortletMode;

import org.junit.Test;
import org.springframework.mock.web.portlet.MockRenderRequest;


public class PortletFunctionsTest {

	@Test
	public void test_LiferayVersion() {
		// supports Liferay 6
		assertEquals(6,PortletVersion.LIFERAY_VERSION[0]);
	}

	@Test
	public void test_paramsToNameValuePairs() {}

	@Test
	public void test_isMinimumLiferayVersionMet() {
		int[] version = null;

		version = new int[] {4};
		assertFalse( Rails286PortletFunctions.isMinimumLiferayVersionMet(version) );

		version = new int[] {4,4};
		assertFalse( Rails286PortletFunctions.isMinimumLiferayVersionMet(version) );

		version = new int[] {5};
		assertFalse( Rails286PortletFunctions.isMinimumLiferayVersionMet(version) );

		version = new int[] {5,0};
		assertFalse( Rails286PortletFunctions.isMinimumLiferayVersionMet(version) );

		version = new int[] {5,1};
		assertFalse( Rails286PortletFunctions.isMinimumLiferayVersionMet(version) );

		version = new int[] {5,2};
		assertFalse( Rails286PortletFunctions.isMinimumLiferayVersionMet(version) );

		version = new int[] {5,3};
		assertFalse( Rails286PortletFunctions.isMinimumLiferayVersionMet(version) );

		version = new int[] {6};
		assertTrue( Rails286PortletFunctions.isMinimumLiferayVersionMet(version) );

		version = new int[] {6,0};
		assertTrue( Rails286PortletFunctions.isMinimumLiferayVersionMet(version) );

	}

	@Test
	public void test_isLiferayVersionEqual() {
		int[] version = null;

		version = new int[] {4,4};
		assertFalse( Rails286PortletFunctions.isLiferayVersionEqual(version) );

		version = new int[] {5,0};
		assertFalse( Rails286PortletFunctions.isLiferayVersionEqual(version) );

		version = new int[] {5,1};
		assertFalse( Rails286PortletFunctions.isLiferayVersionEqual(version) );

		version = new int[] {5,2};
		assertFalse( Rails286PortletFunctions.isLiferayVersionEqual(version) );

		version = new int[] {5,3};
		assertFalse( Rails286PortletFunctions.isLiferayVersionEqual(version) );

		version = new int[] {6,0};
		assertTrue( Rails286PortletFunctions.isLiferayVersionEqual(version) );
	}
  
	/**
     * Should clean unused Rails wildcards, like:
     * 	/10145/10136/otters/:action/ => /10145/10136/otters/ 
     */
    @Test
    public void test_decipherPathCleaningRailsWildcards(){
    	MockRenderRequest request = new MockRenderRequest(PortletMode.VIEW);
    	String path = Rails286PortletFunctions.decipherPath("/otters/:action", request);
    	assertEquals("/otters", path);
    	
    	path = Rails286PortletFunctions.decipherPath("/otters/:action/:another/:another", request);
    	assertEquals("/otters", path);
    	
    	path = Rails286PortletFunctions.decipherPath("/otters/:action/view/:another", request);
    	assertEquals("/otters/view", path);
    	
    	path = Rails286PortletFunctions.decipherPath("/otters/:action/view/12/:another", request);
    	assertEquals("/otters/view/12", path);
    	
    	path = Rails286PortletFunctions.decipherPath("/otters/index", request);
    	assertEquals("/otters/index", path);

    	path = Rails286PortletFunctions.decipherPath("http://host/otters/index", request);
    	assertEquals("http://host/otters/index", path);
    }
  
}
