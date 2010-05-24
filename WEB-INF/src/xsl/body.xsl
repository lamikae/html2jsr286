<xsl:stylesheet version="1.0"
     xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
     xmlns:marionet="http://github.com/youleaf/django-marionet"
     xmlns:html2jsr286="java:com.celamanzi.liferay.portlets.rails286.xsl.XslFunctions"
     extension-element-prefixes="html2jsr286"
     >

    <xsl:namespace-alias stylesheet-prefix="xmlns:marionet" 
          result-prefix=""/>

    <xsl:output method="html"
      encoding="UTF-8"
      indent="yes"
      standalone="no"
      omit-xml-declaration="yes"/>


    <xsl:param name="namespace" />
    <xsl:param name="location" />
    <xsl:param name="query" />
    <xsl:param name="base" />


    <!-- Fetch some info from head, and all of body -->
    <xsl:template match="*[local-name()='html']">
        <div id="{$namespace}_body">
		<!--
            <xsl:apply-templates select="*[local-name()='head']/link"/>
            <xsl:apply-templates select="*[local-name()='head']/style"/>
            <xsl:apply-templates select="*[local-name()='body']"/>
			-->
        </div>
    </xsl:template>

    <!-- Copy through everything that hasn't been modified by the processor -->
    <xsl:template match="text()|@*|*">
        <xsl:copy>
          <xsl:apply-templates select="*|@*|text()"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
