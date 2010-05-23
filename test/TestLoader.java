package com.celamanzi.liferay.portlets.rails286;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@Suite.SuiteClasses({
 	PortletTest.class,
	OnlineClientTest.class,
	RenderFilterTest.class,
	PageProcessorTest.class,
	HeadProcessorTest.class,
	BodyTagVisitorTest.class,
	RouteAnalyzerTest.class,
	PortletFunctionsTest.class,
	AuthorizationTest.class,
	RemoteCookiesTest.class,
	SessionCookiesTest.class,
	XslTest.class
})

public class TestLoader {

  @BeforeClass
  public static void setUp() {
	/* TODO: how to set common variables in setUp?
     */
     //     System.out.println("setting up\n");
  }

  @AfterClass
  public static void tearDown() {
      //     System.out.println("tearing down\n");
  }

}

