/**
 * Copyright (c) 2008 Mikael Lammentausta
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

import java.util.regex.Pattern;
import java.util.regex.Matcher;

// import java.io.PrintWriter;

// import javax.portlet.PortletURL;
import javax.portlet.RenderResponse;

import org.htmlparser.Parser;
import org.htmlparser.tags.*;
import org.htmlparser.filters.NodeClassFilter;

import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.visitors.NodeVisitor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/** Processes a web page received from Rails 
  *
  * Exchanges the URLs and whatnot with htmlparser.
  *
  * The portlet encodes each link on the page into Portlet parameter in an url which
  * points to response.createRenderURL(). 
  * 
  * The Rails286RenderFilter receives the Request before the rendering operation, 
  * and saves the Rails path into the PortletSession. 
  * */
public class PageProcessor {

  private final Log log = LogFactory.getLog(getClass().getName());

  private   String         page       = null;
//   private PortletURL portletUrl = null;
  private   RenderResponse resp       = null;
  private   String         servlet    = null;
  private   String         namespace  = "";

  // during parsing the page, the page header information is stored into these variables.
  protected String         title      = null;
  protected String         encoding   = null;
  protected String         language   = null;


  PageProcessor(String html,String s) {
    page      = html;
    servlet   = s;
  }
  PageProcessor(String html,String s,String ns) {
    page      = html;
    servlet   = s;
    namespace = ns;
    log.debug("Parsing the page with namespace: " + namespace);
  }
  PageProcessor(String html,String s,RenderResponse rp) {
    page      = html;
    servlet   = s;
    resp      = rp;
    namespace = rp.getNamespace();
    log.debug("Parsing the page with namespace: " + namespace);
  }


  /** Processes the HTML page.
    *
    * Removes the <head> section, takes whatever JavaScript or CSS tags there is,
    * and copies them to the new body. External file resources remain external, while
    * inline JS/CSS is parsed into the body inline.
    *
    * The <body> section's NodeList is iterated over in BodyTagVisitor 
    *
    */
  protected String process(java.net.URL railsBaseUrl, String railsRoute)
  throws java.net.MalformedURLException, IllegalStateException, ParserException {

    /** Check that the input is HTML.
      *
      * The page might be inline text, which makes the parser to throw ParserException
      */
    if (!isHTML(page)) {
      log.info("Page is not HTML - returning as such");
      return page;
    }


    /** Parse the HTML String to NodeList.
      *
      * Try-catch the Parser instantiation - return the error msg.
      */
    Parser   parser  = null;
    try {
      parser = new Parser(page);
    }
    catch (ParserException pe) {
      String err = pe.getMessage();
      log.error(err);
      return err;
    }


    /** OK - proceed.
      *
      * The page is valid HTML -- create a new output NodeList.
      */
    NodeList output  = new NodeList();
    NodeList newHead = null;
    NodeList head    = null;
    NodeList body    = null;


    /** Extract the head and body NodeLists */
    log.debug("Attempting to extract the <head> and <body> nodes.");
    try
    {
      NodeList pg = parser.parse (null);
      head = pg.extractAllNodesThatMatch(new NodeClassFilter(HeadTag.class),true);
      body = pg.extractAllNodesThatMatch(new NodeClassFilter(BodyTag.class),true);
    }
    catch (ParserException pe)
    {
      log.error(pe.getMessage());
      pe.printStackTrace();
    }


    /** Parse the <head> NodeList.
      *
      * Extracts CSS and JavaScript tags.
      * No content filtering is done. It would be possible to download the external
      * files introduced in the 'src' tag, but it would be horribly expensive and would
      * break all major libraries like Prototype and jQuery.
      *
      */
    if (head == null) {
      log.debug("The head content is empty!");
    }
    else {
      log.debug("Parsing <head>");
      try {
        HeadProcessor hp = new HeadProcessor(servlet,railsBaseUrl,namespace);
        newHead = hp.process(head);
        title = hp.title;
      }
      catch (Exception e) {
        log.error( e.getMessage() );
      }

      if (newHead == null ) {
        log.warn("The parsed head seems to be null - buggy HeadProcessor?");
      } else {
        output.add(newHead);
        log.debug("Added embedded head into output NodeList");
      }
    }


    /** Iterate the <body> NodeList over the NodeList with TagVisitor; change <a href> and <img> tags */
    if ((body == null) || (body.equals(""))) {
      log.debug("The body content is empty!");
    }
    else {
      /** The <body> tag is replaced to <div id=%namespace%_body> in BodyTagVisitor */

      log.debug("Parsing <body>");
      try
      {
        NodeVisitor visitor = new BodyTagVisitor(railsBaseUrl, servlet, railsRoute, namespace, resp);
        body.visitAllNodesWith(visitor); // visit all nodes
        output.add(body);                // add the altered body to output
      }
      catch (ParserException pe)
      {
        log.error(pe.getMessage());
        pe.printStackTrace ();
      }
    }

    log.debug("Done");
    return output.toHtml();
  }


  /** Overload process(), accept String urls. */
  protected String process(String railsBaseUrl, String railsRoute)
  throws java.net.MalformedURLException, IllegalStateException, ParserException {
    return process( new java.net.URL(railsBaseUrl), railsRoute );
  }


  /** Checks whether the input String is HTML */
  public static boolean isHTML(String page) {

    // compile a regexp
    String html_regexp = "<html";
    Pattern p = Pattern.compile(html_regexp);
    Matcher html = p.matcher(page);

    if (html.find()) {
      return true;
    }
    else {
      return false;
    }
  }


}