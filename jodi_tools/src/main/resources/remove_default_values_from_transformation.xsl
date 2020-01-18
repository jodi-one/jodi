<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>
  
  <xsl:template match="Dataset[position() = 1]/SetOperator" />
  
  <xsl:template match="Source/SubSelect[text() = 'false']" />
  <xsl:template match="Source/Journalized[text() = 'false']" />
  <xsl:template match="Source/JoinType[text() = 'INNER']" />
  <xsl:template match="Source/Alias[text() = ../Name/text()]" />
  
  <xsl:template match="Lookup/LookupType[text() = 'LEFT OUTER']" />
  <xsl:template match="Lookup/Journalized[text() = 'false']" />
  <xsl:template match="Lookup/SubSelect[text() = 'false']" />
  <xsl:template match="Lookup/Alias[text() = ../LookupDataStore/text()]" />
  
  
  <xsl:template match="Mappings/Distinct[text() = 'false']" />
</xsl:stylesheet>

