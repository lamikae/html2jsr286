/*
 * Copyright 2002-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.mock.web;

import javax.portlet.filter.FilterChain;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;
import javax.portlet.*;
import javax.portlet.EventResponse;

import org.springframework.util.Assert;

/**
 * Mock implementation of the {@link javax.servlet.FilterConfig} interface.
 *
 * <p>Used for testing the web framework; also usefol for testing
 * custom {@link javax.servlet.Filter} implementations.
 *
 * @author Juergen Hoeller
 * @tuned for portlets by Mikael Lammentausta
 * @since 2.0.3
 * @see MockFilterConfig
 * @see PassThroughFilterChain
 */
public class MockFilterChain implements FilterChain {

	private RenderRequest render_request;
	private RenderResponse render_response;

    private ActionRequest action_request;
	private ActionResponse action_response;

    private ResourceRequest resource_request;
	private ResourceResponse resource_response;

    private EventRequest event_request;
	private EventResponse event_response;



	/**
	 * Records the request and response.
	 */
	public void doFilter(RenderRequest render_request, RenderResponse render_response) {
		Assert.notNull(render_request, "Request must not be null");
		Assert.notNull(render_response, "Response must not be null");
		if (this.render_request != null) {
			throw new IllegalStateException("This FilterChain has already been called!");
		}
		this.render_request = render_request;
		this.render_response = render_response;
	}

    public void doFilter(ResourceRequest resource_request, ResourceResponse resource_response) {
		Assert.notNull(resource_request, "Request must not be null");
		Assert.notNull(resource_response, "Response must not be null");
		if (this.resource_request != null) {
			throw new IllegalStateException("This FilterChain has already been called!");
		}
		this.resource_request = resource_request;
		this.resource_response = resource_response;
	}

    public void doFilter(EventRequest event_request, EventResponse event_response) {
		Assert.notNull(event_request, "Request must not be null");
		Assert.notNull(event_response, "Response must not be null");
		if (this.event_request != null) {
			throw new IllegalStateException("This FilterChain has already been called!");
		}
		this.event_request = event_request;
		this.event_response = event_response;
	}

    public void doFilter(ActionRequest action_request, ActionResponse action_response) {
		Assert.notNull(action_request, "Request must not be null");
		Assert.notNull(action_response, "Response must not be null");
		if (this.action_request != null) {
			throw new IllegalStateException("This FilterChain has already been called!");
		}
		this.action_request = action_request;
		this.action_response = action_response;
	}
    

}
