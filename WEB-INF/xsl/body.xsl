<xsl:stylesheet version="1.0"
     xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
     xmlns:marionet="http://github.com/youleaf/django-marionet"
     >

    <xsl:namespace-alias stylesheet-prefix="xmlns:marionet" 
          result-prefix=""/>

    <xsl:output method="html" omit-xml-declaration="yes"/>

    <!--
    <xsl:param name="foo" required="yes" />
    -->

    <xsl:variable
        name="location"
        select="//*[local-name()='head']/portlet-session/@location" />

    <xsl:variable
        name="query"
        select="//*[local-name()='head']/portlet-session/@query" />

    <xsl:variable
        name="namespace"
        select="//*[local-name()='head']/portlet-session/@namespace" />

    <xsl:variable
        name="base"
        select="//*[local-name()='head']/portlet-session/@baseURL" />


    <!-- Fetch some info from head, and all of body -->
    <xsl:template match="*[local-name()='html']">
        <div id="{$namespace}_body">
            <xsl:apply-templates select="*[local-name()='head']/link"/>
            <xsl:apply-templates select="*[local-name()='head']/style"/>
            <xsl:apply-templates select="*[local-name()='body']"/>
        </div>
    </xsl:template>

    <xsl:template match="*[local-name()='body']">
        <xsl:apply-templates select="node()"/>
    </xsl:template>

    <!-- Rewrite links -->
    <xsl:template match="*[local-name()='a']">
        <xsl:copy-of select="marionet:link(.,string($location),string($query),string($namespace),string($base))"/>
    </xsl:template>

    <!-- Rewrite image references -->
    <xsl:template match="*[local-name()='img']">
      <xsl:copy-of select="marionet:image(.,string($base))"/>
    </xsl:template>

    <!-- Convert link tags in head to style tags -->
    <xsl:template match="*[local-name()='html']/head/link">
        <style type="text/css" id="{@id}">
        @import "<xsl:value-of select="marionet:href(string(@href),string($base))"/>";
        </style>
    </xsl:template>

    <!-- Form POST action -->
    <xsl:template match="*[local-name()='form']">
        <xsl:copy-of select="marionet:form(.,string($location),string($query),string($namespace),string($base))"/>
    </xsl:template>

    <!-- Copy through everything that hasn't been modified by the processor -->
    <xsl:template match="text()|@*|*">
        <xsl:copy>
          <xsl:apply-templates select="*|@*|text()"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
