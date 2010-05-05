/**
 * Copyright (c) 2009 Mikael Lammentausta
 *               2010 Mikael Lammentausta, Túlio Ornelas dos Santos
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.celamanzi.liferay.portlets.rails286;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class OnlineClient {

	private final Log log = LogFactory.getLog(getClass().getName());

	private final String host    = "http://localhost:3000";
	private final String servlet = "";

	private final String railsJUnitRoute = "/caterpillar/test_bench/junit";

	private final String railsJUnitURL = host+servlet+railsJUnitRoute;

	private static int retries = 0;
	private static int timeout = 30000;

	private ArrayList<Header> headers;
	private String contentType;
	private String contentDisposition;

	// TODO: annotate getters
	protected URL      requestURL  = null;
	protected int      statusCode  = -1;

	// TODO: annotate setters + getters
	protected Cookie[] cookies     = null;
	protected URL      httpReferer = null;
	protected Locale   locale      = null;

	public OnlineClient(URL _requestURL) {
		requestURL  = _requestURL;
		cookies     = null;
		httpReferer = null;
		locale      = null;
	}

	public OnlineClient(URL _requestURL, Map<String,Cookie> _cookies, URL _httpReferer, Locale _locale) {
		requestURL  = _requestURL;
		cookies     = _cookies.values().toArray(new Cookie[0]);
		httpReferer = _httpReferer;
		locale      = _locale;
	}

	public int getStatusCode() {
		return statusCode;
	}
	
	private void configureHeader(Header[] headers) {
		setHeaders(headers);
		this.contentType = getHeaderValue("Content-Type", getHeaders());
		this.contentDisposition = getHeaderValue("Content-Disposition", getHeaders());
	}

	private void setHeaders(Header[] headers){
		this.headers = new ArrayList<Header>();

		for(Header header : headers){
			this.headers.add(header);
		}
	}

	public ArrayList<Header> getHeaders(){
		return headers;
	}

	public String getContentType(){ 
		return this.contentType;
	}

	public String getContentDisposition(){
		return contentDisposition;
	}

	private String getHeaderValue(String name, ArrayList<Header> headers) {
		for (Header header : headers) {
			if (header.getName().equals(name)){
				return header.getValue();
			}

		}
		return "";
	}

	/** GET
	 *
	 * Instantiates HttpClient, prepares it with session cookies,
	 * executes the request and returns the response body.
	 *
	 * TODO: set response headers to internal protected variable.
	 * 
	 * @since 0.8.0
	 */
	protected byte[] get()
	throws HttpException, IOException
	{
		// Response body from the web server
		byte[] responseBody = null ;

		statusCode = -1;

		HttpClient client = preparedClient();    

		// Create the GET method and prepare its headers
		GetMethod method = new GetMethod(requestURL.toString());
		HttpMethod _method = (HttpMethod)method;
		method = (GetMethod)prepareMethodHeaders(_method);

		//debugHeaders(method.getRequestHeaders());

		log.debug("GET request URL: " + requestURL.toString());

		try {
			// Execute the method
			statusCode = client.executeMethod(method);

			// log the status
			if (statusCode != HttpStatus.SC_OK) {
				log.error("Request failed: " + method.getStatusLine());
				throw new HttpException(method.getStatusLine().toString());
			}
			else {
				log.debug("Status code: " + method.getStatusLine());

				// Read the response body
				responseBody = method.getResponseBody();
				//method.getRequestHeader('');

				// Keep the headers for future usage (render phase)
				configureHeader(method.getResponseHeaders());

				// Get session cookies
				cookies = client.getState().getCookies();
				log.debug("Stored "+cookies.length+" cookies.");
			}

		} finally {
			// Release the connection
			method.releaseConnection();
		}

		return responseBody;
	}

	/** 
	 * POST
	 * 
	 * Posts the parametersBody
	 */
	protected byte[] post(NameValuePair[] parametersBody, Map<String, Object[]> files)
	throws HttpException, IOException{
		// Response body from the web server
		byte[] responseBody = null;
		statusCode = -1;

		List<File> tempFiles = null;

		HttpClient client = preparedClient();

		// Create a method instance.
		log.debug("POST action request URL: " + requestURL.toString());

		PostMethod method = new PostMethod(requestURL.toString());
		HttpMethod _method = (HttpMethod) method;
		method = (PostMethod) prepareMethodHeaders(_method);

		if (files != null && files.size() > 0){

			tempFiles = new ArrayList<File>();
			createMultipartRequest(parametersBody, files, method, tempFiles); 

		}else{
			method.setRequestBody(parametersBody);

			// Provide custom retry handler is necessary
			method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, 
					new DefaultHttpMethodRetryHandler(3, false));
		}

		try {
			// Execute the method.
			statusCode = client.executeMethod(method);

			if (
					(statusCode == HttpStatus.SC_MOVED_TEMPORARILY) ||
					(statusCode == HttpStatus.SC_MOVED_PERMANENTLY) ||
					(statusCode == HttpStatus.SC_SEE_OTHER) ||
					(statusCode == HttpStatus.SC_TEMPORARY_REDIRECT)
			)
			{

				// get Location
				String location = ((Header)method.getResponseHeader("Location")).getValue();
				requestURL = new URL(location);
				log.debug("POST status code: " + method.getStatusLine());
				log.debug("Redirect to location: "+location);

				// server may add another cookie before redirect..
				cookies = client.getState().getCookies();
				//log.debug("Stored "+cookies.length+" cookies.");

				// Note that this GET overwrites the previous POST method,
				// so it should set statusCode and cookies correctly.
				responseBody = get();
			}
			else {
				// the original POST method was OK, pass
				// No more redirects! Response should be 200 OK
				if (statusCode != HttpStatus.SC_OK) {
					log.error("Method failed: " + method.getStatusLine());
					throw new HttpException(method.getStatusLine().toString());
				}
				else {
					log.debug("POST status code: " + method.getStatusLine());
				}

				// Read the response body.
				responseBody = method.getResponseBody();

				// Get session cookies
				cookies = client.getState().getCookies();
			}


		} finally {
			// Release the connection
			method.releaseConnection();

			// Delete temp files
			deleteFiles(tempFiles);
		}

		return responseBody;
	}

	/** Returns a HttpState fixed with cookies.
	 *
	 * @since 0.8.0
	 */
	protected HttpState preparedHttpState()
	{
		HttpState state = new HttpState();

		if (cookies != null) {
			// Add cookies to the state
			state.addCookies(cookies);
		}

		if (log.isDebugEnabled()) {
			Cookie[] _cookies = state.getCookies();
			debugCookies(_cookies);
		}

		return state;
	}

	protected void debugCookies(Cookie[] cookies) {
		log.debug( "Cookie inspector found "+cookies.length+" cookies ------v");
		for (Cookie cookie : cookies)
			log.debug(cookie.toString()
					+ ", domain=" + cookie.getDomain()
					+ ", path=" + cookie.getPath()
					+ ", max-age=" + cookie.getExpiryDate()
					+ ", secure=" + cookie.getSecure());
		log.debug( "----------------------------");
	}

	/** Prepares client.
	 */
	protected HttpClient preparedClient()
	{
		// Create an instance of HttpClient and prepare it
		HttpClient client = new HttpClient();

		client.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
		client.getParams().setParameter(HttpMethodParams.SINGLE_COOKIE_HEADER, true);
		// magic cookie line (all cookies on one header line)
		client.getParams().setParameter("http.protocol.single-cookie-header", true);

		// Set state (session cookies)
		client.setState(preparedHttpState());
		// Set timeout
		client.getHttpConnectionManager().getParams().setConnectionTimeout(timeout);

		return client;
	}


	/** Prepares method headers.
	 *
	 * Modifies the given method by inserting, when defined:
	 *  - Referer
	 *  - Accept-Language
	 *
	 * @since 0.8.0
	 */
	protected HttpMethod prepareMethodHeaders(HttpMethod method)
	{
		// Insert the HTTP Referer
		if (httpReferer != null ) {
			log.debug("HTTP referer: "+httpReferer.toString());
			method.setRequestHeader("Referer",httpReferer.toString());
		}
		else {
			//log.debug("HTTP referer is null");
		}

		// Insert the Locale
		if (locale != null ) {
			log.debug("Request's locale language: "+locale.toString());
			method.setRequestHeader("Accept-Language",locale.toString());
		}
		else {
			//log.debug("Locale is null");
		}

		//Correct the charset problem
		method.getParams().setContentCharset("UTF-8");

		// Provide custom retry handler
		method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
				new DefaultHttpMethodRetryHandler(retries, false));

		return method;
	}

	protected void createMultipartRequest(NameValuePair[] parametersBody, Map<String, Object[]> files, PostMethod method, List<File> tempFiles)
	throws IOException, FileNotFoundException {

		List<Part> parts = new ArrayList<Part>();
		parametersBody = removeFileParams(parametersBody, files);

		for(NameValuePair param : parametersBody){
			parts.add(createStringPart(param));
		}

		for(String key : files.keySet()){
			File file = createFile(files.get(key));
			if (file != null){
				parts.add(new FilePart(key, file));
				tempFiles.add(file);
			}
		}

		Part[] array = new Part[parts.size()];
		method.setRequestEntity(new MultipartRequestEntity(parts.toArray(array), method.getParams()));

		method.getParams().setBooleanParameter(HttpMethodParams.USE_EXPECT_CONTINUE, false);
	}

	private NameValuePair[] removeFileParams(NameValuePair[] parametersBody, Map<String, Object[]> files){
		List<NameValuePair> list = new ArrayList<NameValuePair>();

		for(NameValuePair param : parametersBody){
			if (!files.containsKey(param.getName())){
				list.add(param);
			}
		}

		NameValuePair[] array = new NameValuePair[list.size()];
		return list.toArray(array);
	}

	private StringPart createStringPart(NameValuePair pair){
		StringPart part = new StringPart(pair.getName(), pair.getValue());

		//HACK: When content type is null, Rack will interpretate as string param, 
		//otherwise this will be treated like a file.
		part.setContentType(null);

		part.setCharSet("UTF-8");
		return part;
	}

	private File createFile(Object[] objs) throws IOException{
		if (objs != null && objs.length == 2){

			File file = (File) objs[0];
			byte[] bytes = (byte[]) objs[1];

			FileOutputStream fos = new FileOutputStream(file);
			fos.write(bytes);
			fos.flush();
			fos.close();

			return file;
		}

		return null;
	}

	private void deleteFiles(List<File> tempFiles) {
		if (tempFiles == null){
			return;
		}

		for (File file : tempFiles){
			if(!file.delete()){
				log.error("Failure to delete: " + file.getPath());
			}
		}
	}

	private void debugHeaders(Header[] headers) {
		log.debug("Headers:");
		for (Header h : headers)
			log.debug(h.toString().trim());
	}


}
