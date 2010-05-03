package com.celamanzi.liferay.portlets.rails286.mock;

import javax.portlet.ResourceURL;

public class MockRenderResponse extends org.springframework.mock.web.portlet.MockRenderResponse{
	public ResourceURL createResourceURL(){
		return new MockResourceURL();
	}
}
