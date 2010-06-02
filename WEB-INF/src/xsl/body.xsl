<xsl:stylesheet version="1.0"
     xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
     xmlns:format="com.celamanzi.liferay.portlets.rails286.PortletTagFormat"
     exclude-result-prefixes="portlet"
     >

    <xsl:output method="html"
      encoding="UTF-8"
      indent="yes"
      standalone="no"
      omit-xml-declaration="yes"/>

    <xsl:variable
        name="session"
        select="//*[local-name()='portlet-session']" />
    <xsl:variable
        name="namespace"
        select="//*[local-name()='portlet-session']/@namespace" />
    <xsl:variable
        name="baseurl"
        select="//*[local-name()='portlet-session']/@baseURL" />


    <!-- Fetch some info from head, and all of body -->
    <xsl:template match="*[local-name()='html']">
        <div id="{$namespace}" class="portlet">
          <div class="{$namespace}_head">
            <xsl:apply-templates select="*[local-name()='head']/link"/>
            <xsl:apply-templates select="*[local-name()='head']/style"/>
          </div>
          <div class="{$namespace}_body">
            <xsl:apply-templates select="*[local-name()='body']"/>
          </div>
        </div>
    </xsl:template>

    <xsl:template match="*[local-name()='body']">
        <xsl:apply-templates select="node()"/>
    </xsl:template>

    <!-- Rewrite links -->
    <xsl:template match="*[local-name()='a']">
        <xsl:copy-of select="format:link()"/>
    </xsl:template>

    <!-- Copy through everything that hasn't been modified by the processor -->
    <xsl:template match="text()|@*|*">
        <xsl:copy>
          <xsl:apply-templates select="*|@*|text()"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
