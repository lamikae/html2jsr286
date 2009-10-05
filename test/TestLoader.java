package com.youleaf.jsrproxy.test;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	PageProcessorTest.class,
	BodyTagVisitorTest.class
// 	HeadProcessorTest.class,
// 	RouteAnalyzerTest.class
})

public class TestLoader {

	@BeforeClass
	public static void setUp() {
// 		System.out.println("setting up\n");
	}

	@AfterClass
	public static void tearDown() {
// 		System.out.println("tearing down\n");
	}

}

