<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes"/>
	<xsl:template match="/">
		<xsl:element name="Order">
			 <xsl:copy-of select="/VSIOrderDetailsWEB/@*"></xsl:copy-of>
			 <xsl:attribute name="DocumentType">0001</xsl:attribute>
		</xsl:element>
	</xsl:template>	
</xsl:stylesheet>