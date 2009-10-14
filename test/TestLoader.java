package com.celamanzi.liferay.portlets.rails286;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	PageProcessorTest.class,
	HeadProcessorTest.class,
	BodyTagVisitorTest.class,
	RouteAnalyzerTest.class,
	PortletFunctionsTest.class
/*   RenderFilterTest.class, */
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

