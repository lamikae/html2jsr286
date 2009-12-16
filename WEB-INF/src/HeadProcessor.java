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

import org.htmlparser.Parser;
import org.htmlparser.util.ParserException;

import org.htmlparser.util.NodeList;
import org.htmlparser.util.SimpleNodeIterator;

import org.htmlparser.nodes.TagNode;
import org.htmlparser.tags.*;

import org.htmlparser.filters.NodeClassFilter;
import org.htmlparser.filters.AndFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.filters.HasAttributeFilter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class HeadProcessor {

  // logger
  private final Log log = LogFactory.getLog(getClass().getName());

  // class variables
  private   java.net.URL baseUrl      = null;
  private   String       servlet      = null;
  private   String       namespace    = "";

  // the page header information is stored into these variables.
  protected String       title        = "";
  protected String       content_type = null;
  protected String       encoding     = null;
  private   String[]     javascript_blacklist = {"jquery"}; // regexps


  /** Constructors */
  protected HeadProcessor(String s, String ns) {
    servlet   = s;
    namespace = ns;
  }
  protected HeadProcessor(String s, java.net.URL bu,String ns) {
    servlet   = s;
    namespace = ns;
    baseUrl   = bu;
    log.debug("Initializing HeadProcessor with baseUrl: "+baseUrl.toString());
  }


  /** Parses <head>.
    *
    * Replaces <html><head> with <div id="%namespace%_head">.
    * Extracts title, some of the metadata, JavaScript and CSS tags.
    *
    */
  protected NodeList process(NodeList head)
  throws ParserException, Exception {
    String headString = "\n<div id=\"" + namespace + "_head\">\n";
    Pattern pattern = null;
    Matcher matcher = null;


    /** Extract <title> */
    NodeList titletags = head.extractAllNodesThatMatch(new NodeClassFilter(TitleTag.class),true);
    log.debug(titletags.size() + " title tags found");
    for (SimpleNodeIterator i = titletags.elements(); i.hasMoreNodes();) {
      TitleTag n = (TitleTag)i.nextNode();
      title += n.getTitle();
      log.debug(title);
    }


    /** Parse <meta> */
    NodeList meta = head.extractAllNodesThatMatch(new NodeClassFilter(MetaTag.class),true);
    log.debug(meta.size() + " meta tags found");
    for (SimpleNodeIterator i = meta.elements(); i.hasMoreNodes();) {
      MetaTag n = (MetaTag)i.nextNode();
      if (n.getHttpEquiv().equals("content-type")) {
        log.debug("Parsing meta content-type");
        String c = n.getMetaContent();
        //<meta http-equiv="content-type" content="text/html; charset=UTF-8"></meta>
        content_type = c.split(";")[0];
        encoding     = c.split("=")[1];

        log.debug("content_type: "+content_type);
        log.debug("encoding: "+encoding);
      }
    }


    /** CSS */
    NodeList css_link = head.extractAllNodesThatMatch(
      new AndFilter(new TagNameFilter("link"),new HasAttributeFilter("rel","stylesheet")),true
    );
    log.debug(css_link.size() + " css link tags found");

    NodeList css_style = head.extractAllNodesThatMatch(new NodeClassFilter(StyleTag.class),true);
    log.debug(css_style.size() + " style tags found");
    if ( (css_link.size() > 0) || (css_style.size() > 0) ) {
      // headString += makeInlineCSS(css_link,css_style);
      /** Just include the original CSS.
        * @since 0.3.0
        */
      log.debug("StyleTag encountered, including it to body");
      // NOTE: breaks body {} tag
      headString += css_link.toHtml();
      headString += css_style.toHtml();
    }


    /** JavaScript */
    NodeList scriptTags = head.extractAllNodesThatMatch(
      new NodeClassFilter(ScriptTag.class),true);
    log.debug(scriptTags.size() + " script tags found");
    if ( scriptTags.size() > 0 ) {

      log.debug("Parsing <script> tags");
      for (SimpleNodeIterator i = scriptTags.elements(); i.hasMoreNodes();) {
        ScriptTag tag = (ScriptTag)i.nextNode();

        /** Does the tag contain inline code, or a link to src? */
        if (tag.getScriptCode().length() > 0) {
          log.warn("Script tag contains inline JavaScript -- note that IT IS NOT PARSED CORRECTLY.");
          /**
            FIXME: HTMLparser 1.6 cannot handle inline JavaScript that contains HTML tags.
            */
          headString += tag.toHtml();
        }
        else {
          log.debug("Link to src - checking JavaScript blacklist");
          if (JavaScriptBlacklistFilter(tag)) {
            // blacklist check passed, include the original JS.
            headString += tag.toHtml();
          }
        }
      } // iterator
    } // JS tags


    /** Close the <namespace>_head Div. */
    headString += "\n</div>\n";

    // Parse the String form to NodeList
    NodeList ret = new NodeList();

    /* @since 0.7.0: does not handle ParserException
     */
    // try {
      Parser parser = new Parser(headString);
      ret = parser.parse(null);
    // }
    // catch (ParserException pe) {
    //   log.error( pe.getMessage() );
    // }

    log.debug("Done");
    return ret;
  }


  /** Compare the JavaScript src to the blacklist.
    * @since 0.3.0
    */
  private boolean JavaScriptBlacklistFilter(ScriptTag tag)
  throws Exception {
    String src = tag.getAttribute("src");
    if (src == null) {
      // inline script tag has already been catched, so this may mean that the tag is invalid?
      return false;
    }
    else {
      //log.debug("Checking if "+src+" is in blacklist");
      for (int j=0; j<javascript_blacklist.length; j++) {
        /** Blacklist match */
        if (src.matches(".*"+javascript_blacklist[j]+".*")) {
          log.debug(src+" is blacklisted!");
          return false;
        }
      }
      /** Accept JS */
      log.debug(src+" is ok");
      return true;
    }
  }

}
