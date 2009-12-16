package com.celamanzi.liferay.portlets.rails286;

import org.junit.*;
import static org.junit.Assert.*;

import javax.portlet.*;

import com.celamanzi.liferay.portlets.rails286.Rails286PortletFunctions;


public class PortletFunctionsTest {

	@Test
	public void test_LiferayVersion() {
		// version 5.2
		assertEquals(5,PortletVersion.LIFERAY_VERSION[0]);
		assertEquals(2,PortletVersion.LIFERAY_VERSION[1]);
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
		assertTrue( Rails286PortletFunctions.isMinimumLiferayVersionMet(version) );

		version = new int[] {5,0};
		assertFalse( Rails286PortletFunctions.isMinimumLiferayVersionMet(version) );

		version = new int[] {5,1};
		assertFalse( Rails286PortletFunctions.isMinimumLiferayVersionMet(version) );

		version = new int[] {5,2};
		assertTrue( Rails286PortletFunctions.isMinimumLiferayVersionMet(version) );

		// upcoming..
		version = new int[] {5,3};
		assertTrue( Rails286PortletFunctions.isMinimumLiferayVersionMet(version) );

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
		assertTrue( Rails286PortletFunctions.isLiferayVersionEqual(version) );

		// upcoming..
		version = new int[] {5,3};
		assertFalse( Rails286PortletFunctions.isLiferayVersionEqual(version) );
	}
  
	// This would be very important to test
// 	@Test
	public void test_decipherPath() {
		fail( "Needs to instantiate RenderRequest request" );
	}


  
}
