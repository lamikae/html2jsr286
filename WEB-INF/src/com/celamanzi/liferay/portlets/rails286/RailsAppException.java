package com.celamanzi.liferay.portlets.rails286;

@SuppressWarnings("serial")
public class RailsAppException extends Exception {
	
	private String message;
	private String html;
	
	public RailsAppException(String message, String html) {
		this.message = message;
		this.html = html;
	}
	
	@Override
	public String getMessage() {
		return message;
	}
	
	public String getHtml() {
		return html;
	}

}
