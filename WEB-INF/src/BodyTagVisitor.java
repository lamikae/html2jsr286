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
import org.htmlparser.Tag;
import org.htmlparser.tags.*;
import org.htmlparser.Text;

import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.visitors.NodeVisitor;

import javax.portlet.PortletURL;
import javax.portlet.RenderResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class BodyTagVisitor extends NodeVisitor
{
  private final Log log = LogFactory.getLog(getClass().getName());

  private java.net.URL baseUrl      = null;
  private String       servlet      = null;
  private String       requestPath  = null;
  private String       documentPath = null; // TODO, get the path to this documet
  private PortletURL   portletUrl   = null; // needed to redirect links
  private PortletURL   actionUrl    = null; // needed to set form actions
  private String       namespace    = "";


//   BodyTagVisitor( java.net.URL bu, String rp, String id ) {
//     baseUrl      = bu;
//     requestPath  = rp;
//     documentPath = "/";
//     namespace    = id;
//   }

  /** Takes in RenderResponse and extracts portletUrl and actionUrl */
  public BodyTagVisitor( java.net.URL bu, String s, String rp, String ns, RenderResponse resp ) {
    baseUrl      = bu;
    servlet      = s;
    requestPath  = rp;
    documentPath = "/";
    namespace    = ns;
    if ( resp != null ) {
      portletUrl   = resp.createRenderURL();
      actionUrl    = resp.createActionURL();
    }
  }

  /** alternative that takes portletUrl directly */
  public BodyTagVisitor( java.net.URL bu, String s, String rp, String ns, PortletURL pu, PortletURL actionURL ) {
    baseUrl      = bu;
    servlet      = s;
    requestPath  = rp;
    documentPath = "/";
    namespace    = ns;
    portletUrl   = pu;
    actionUrl    = actionURL;
  }

  /** Recurses to every HTML tag.
    * This function converts the hyperlinks to parameters in the portletUrl.
    * The RenderFilter picks this parameter, stores it to session and launches an
    * http request. GET works fine, POST has some problems.
    *
    * It is possible to escape the portlet link by adding "exit_portlet=true"
    * to the HTTP request parameters. (www.example.com?exit_portlet=true)
    */
  public void visitTag (Tag tag)
  {
    //log.debug("Encountered a " + tag.getClass()); // Floods the debug channel
    RouteAnalyzer ra = new RouteAnalyzer(baseUrl,servlet);
      
      
      Pattern pattern = null;
      Matcher matcher = null;

    /**
     * Replace the <body> tag with <div id=%namespace%_body> 
     */
    if (tag instanceof BodyTag) {
      BodyTag b = (BodyTag)tag;
      String id = namespace+"_body"; // also defined in HeadProcessor()

      log.debug("Replacing <body> with <div id=" + id + ">");
      b.setTagName("div");
      b.getEndTag().setTagName("/div");
      b.setAttribute("id","\""+id+"\""); // damn, htmlparser does not produce clean XHTML
    }

    /**
     * Convert links ###(, only if portletUrl is defined).
     * - PortletURL is not available while "testing".
     * - skip if parameter 'exit_portlet=true'.
     */
//     else if ((tag instanceof LinkTag) && (portletUrl != null))
    else if (tag instanceof LinkTag)
    {
      LinkTag link = (LinkTag)tag;
      PortletURL newHref = portletUrl;

      NodeList children = link.getChildren();
      if (children.size() > 0) {
//         log.warn("Link has children tags -- there is a bug in handling them. Expect broken HTML.");
//         log.warn(tag.getText());
      }


      /** HTTP */
      if ( (link.isHTTPLink()) && (!link.getLink().equals(""))) {
        log.debug("Encountered a HTTPLink: " + link.getLink());
        String href = link.getAttribute("href");
        String route = null;
        String onclick = null;
        String newLink = null;


        // skip Ajax links with href="#"
        pattern = Pattern.compile("^#$");
        matcher = pattern.matcher(href);
        if (matcher.find()) {
          log.debug("Ignoring Ajax link: "+href);
          return;
        }

        // the link might be Ajax '#', or plain "some_file.htm(l)",
        // that will raise MalformedURLException.
        String local_file = "^[a-zA-Z0-9_%]*.htm";
        pattern = Pattern.compile(local_file);
        matcher = pattern.matcher(href);
        if (matcher.find()) {
          log.debug("Protocolless URL: "+href);
          href = baseUrl + href;
        }


        /** Exiting portlet?
          *
          * - check if the parameter 'exit_portlet' is 'true'.
          *   => return the link as such, ie. not wrapped in PortletURL.
          */
        String exit_param = "exit_portlet=(true|false)";
        pattern = Pattern.compile(exit_param);
        matcher = pattern.matcher(href);
        if (matcher.find()) {
          log.debug("Exit portlet parameter encountered: "+exit_param);
          // TODO: use a proper replace regexp, so sanitization is not required.
          href = href.replaceFirst(exit_param,""); // remove the parameter
          href = href.replaceFirst("&amp;$","");   // sanitize
          href = href.replaceFirst("\\?&amp;","?"); // sanitize
          href = href.replaceFirst("\\?$","");     // sanitize
          log.debug("Saving link: "+href);
          link.setLink(href);
        }


        /** URL -> PortletURL */
        else {
          /** onclick javascript */
          if (link.getAttribute("onclick") != null) {
            log.debug("Onclick attribute detected");
            onclick = link.getAttribute("onclick");
            //log.debug(onclick);

            pattern = Pattern.compile("document.createElement('form')");
            matcher = pattern.matcher(onclick);
            log.debug("Onclick creates a form");

            /** Replaces the onclick JavaScript's action URL to portlet actionUrl, and sets the originalActionUrl parameter. 
              * This is handled in processAction().
              * Some repetition with the regular HTML form handler (in this one giant function.
              */
            if (actionUrl != null) {
              log.debug("Replacing onclick url with actionUrl");

              // add originalActionUrl to form parameters by adding an input
              // element to the page.
              String js = null;
              try {
                js="var inp=document.createElement(\"input\");inp.name='originalActionUrl';inp.value='"+ra.getRequestRoute(href)+"';f.appendChild(inp);";
              }
              catch (java.net.MalformedURLException e) {
                log.error(e.getMessage());
              }
              //log.debug(js);

              // alter the onClick
              onclick = onclick.replaceFirst("this.href","'"+actionUrl.toString()+"'").replaceFirst("f.submit",js+"f.submit");
              // set the new link
              log.debug("Setting the onclick attribute to the new link");
              link.setAttribute("onclick",onclick);
              log.debug(onclick);
              newLink = "#";
            }
          } // onclick
          /** Extract the Rails route */
          else {
            try {
              route = ra.getRequestRoute(href);
              // prevent parameter separator '&amp;' from being sent as &amp;amp;
              route = route.replaceAll("&amp;","&");
            }
            catch (java.net.MalformedURLException e) {
              log.error(e.getMessage());
            }
          } // route


          /** Strip target.
            * If there is a target, the browser will open a new tab (or an instance) and fail to convert links on that page
            */
          String target = link.getAttribute("target");
          if (target != null) {
            log.debug("Removing the target attribute \""+target+"\"");
            link.removeAttribute("target");
          }

          /** Change the link */
          if (portletUrl != null) {
            if (route != null) {
              newHref.setParameter("railsRoute",route);
              log.debug("Added parameter railsRoute to the PortletURL: " + route);
              newLink = newHref.toString();
            }
            // else {}
            log.debug("Replacing the original link tag");
            link.setLink(newLink);
          }
          else {
            log.debug("portletUrl is null");
          }
        }
      } // exit portlet?
/*      else if (link.isEndTag()) {
        log.debug("Link end tag detected -- where is the begin tag? This is a bug.");
        log.debug(tag.getText());
      }*/
      else if (link.isHTTPSLink()) { log.warn("Cannot handle HTTPS links yet"); }
      //else if (link.isJavascriptLink()) { log.warn("Cannot handle JavaScript links yet"); }
      else if (link.isMailLink()) { log.debug("No changes to mail links"); }
      else if (link.isFTPLink())  { log.debug("No changes to FTP links"); }
      else if (link.isIRCLink())  { log.debug("No changes to IRC links"); }
      else {
        log.warn("fixme: Encountered an unknown link type: " + link.getLink());
      }
    }


    /**
     * Convert images
     */
    else if (tag instanceof ImageTag)
    {
      ImageTag img        = (ImageTag)tag;
      String src          = img.getImageURL();
      java.net.URL srcUrl = null;

      // break if the source is an external image
      if (isExternal(src)) {
        log.debug("Preserving image "+src);
        return;
      }

      /* If the src should be translated:
        * 1) check if the src is absolute or relative
        * 2) if absolute => add baseUrl prefix
        * 3) if relative => add baseUrl + documentPath prefix
        */

      String path = "";

      // TODO: move RouteAnalyzer up here

      // HACK: lookout for http, let RouteAnalyzer do this
      Pattern rel_link_pattern = Pattern.compile("^http");
      Matcher rel_link_matcher = rel_link_pattern.matcher(src);
      if ( rel_link_matcher.find() ) {
        log.debug(src + " is already a full URL, preserving");
        return;
      }

      log.debug("Converting image "+src);

      // test if the src begins with slash '/'
      rel_link_pattern = Pattern.compile("^/");
      rel_link_matcher = rel_link_pattern.matcher(src);
      if ( ! rel_link_matcher.find() ) {
        log.debug("The image path \"" + src + "\" is relative");
        path += '/';
      }
      else {
        log.debug("The image path \"" + src + "\" is absolute");
      }

      path += src;
      String url = null;
      try {
        url = ra.getFullURL(path).toString();
      }
      catch (java.net.MalformedURLException e) {
        log.error(e.getMessage());
        return;
      }

      log.debug("New image URL: " + url);
      img.setImageURL(url);
    }


    /**
     * Convert forms, only if actionUrl is defined
     */
    else if ((tag instanceof FormTag) && (actionUrl != null))
//    else if ((tag instanceof FormTag) )
    {
      FormTag frm = (FormTag)tag;
      String method = frm.getFormMethod();
      //String formAction = frm.extractFormLocn();
      String formAction = frm.getFormLocation();
      log.debug("Encountered a " + method + " FormTag to action: " + formAction);

        
        
        /** Exiting portlet?
         * Yes, this is a duplicate from the LinkTag handling.
         * Not good, not good at all.
         *
         */
        String exit_param = "exit_portlet=(true|false)";
        pattern = Pattern.compile(exit_param);
        matcher = pattern.matcher(formAction);
        if (matcher.find()) {
            log.debug("Exit portlet parameter encountered: "+exit_param);
            // TODO: use a proper replace regexp, so sanitization is not required.
            formAction = formAction.replaceFirst(exit_param,""); // remove the parameter
            formAction = formAction.replaceFirst("&amp;$","");   // sanitize
            formAction = formAction.replaceFirst("\\?&amp;","?"); // sanitize
            formAction = formAction.replaceFirst("\\?$","");     // sanitize
            log.debug("Saving form action: "+formAction);
            frm.setFormLocation(formAction);
            return;
        }
        
        
      /** RouteAnalyzer */
      try {
        formAction = ra.getRequestRoute(formAction);
      }
      catch (java.net.MalformedURLException e) {
        log.error(e.getMessage());
      }

      log.debug("Full action URL: " + formAction);

      if ( method.equals("post") || method.equals("put") ) {
        // replace the action URL with the portlet actionURL
        String portletAction = actionUrl.toString();
        log.debug("New form action URL: " + portletAction);
        frm.setFormLocation(portletAction);

        // TODO: iterate all form tags and add namespace

        // create a new hidden tag that stores the original action url
        String newtag = "<div style=\"display: none;\">";
        newtag += "<input name=\""+namespace+"originalActionUrl"+"\" type=\"text\" size=\"0\" value=\""+formAction+"\" />";
        newtag += "<input name=\""+namespace+"originalActionMethod"+"\" type=\"text\" size=\"0\" value=\""+method+"\" />";
        newtag += "</div>";

        // get the children to add a new node into
        NodeList inputs = frm.getChildren();
        try {
          Parser parser = new Parser(newtag);
          log.debug("Adding a new form child input tag: "+newtag);
          if (!newtag.equals("")) {
            inputs.add(parser.parse(null));
          }
        }
        catch (ParserException pe) {
          log.error( pe.getMessage() );
        }
        frm.setChildren(inputs);
      }
    }


    else // other specific tags and generic TagNode objects
    {
    }
  }

  /** an URL may be:
    *  a) http://foobar/baz.png
    *  b) /baz.png
    *  c) baz.png
    *
    * The URL may point to external location and therefore it should be determined wheter it should be translated.
    * 1) test if the src contains '://', and if true, convert it to java.net.URL
    * 2) if false, or if the host of the URL equals to baseUrl, the src should be translated
  */
  private boolean isExternal( String src ) {

    log.debug("Checking externality");
    Pattern p = Pattern.compile("://");
    Matcher m = p.matcher(src);

    if ( ! m.find() ) { return false; }
    else {
      // convert the src to URL
      try {
        java.net.URL srcUrl = new java.net.URL(src);
        log.debug("Comparing "+srcUrl.getHost()+" with "+baseUrl.getHost());
        if ( srcUrl.getHost().equals(baseUrl.getHost()) ) { return false; }
        else {
          log.debug(src+" is an external URL");
          return true;
        }
      }
      catch (java.net.MalformedURLException e) {
        log.error(e.getMessage());
        return false;
      }
    }
  }

}
