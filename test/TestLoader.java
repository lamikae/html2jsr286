package com.celamanzi.liferay.portlets.rails286;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@Suite.SuiteClasses({
/*   RenderFilterTest.class, */
	PageProcessorTest.class,
	HeadProcessorTest.class,
	BodyTagVisitorTest.class
/*   PortletFunctionsTest.class, */
/*   RouteAnalyzerTest.class */
/* 	PortletTest.class */
})

public class TestLoader {

  @BeforeClass
  public static void setUp() {
	/** TODO: how to set common variables in setUp? */
//     System.out.println("setting up\n");
  }

  @AfterClass
  public static void tearDown() {
//     System.out.println("tearing down\n");
  }

}

