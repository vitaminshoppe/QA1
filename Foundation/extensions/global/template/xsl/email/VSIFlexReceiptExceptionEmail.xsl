<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format">
	<xsl:output method="html" indent="yes"/>
	<xsl:template match="*">
		<xsl:comment>CONTENT_TYPE=text/html</xsl:comment>
		<html>
			<body style="margin-left: 25px">
				<p>Flex Receipt Exception occurred for Order No. <xsl:value-of select="//@OrderNo"/> in Store <xsl:value-of select="//@StoreNo"/>.</p>
				<br/>
				<p>Response Code is: <xsl:value-of select="//@ResponceCode"/> and Response Message is: <xsl:value-of select="//@ResponseMessage"/>.</p>
				<p>Exception is: <xsl:value-of select="//@ExceptionMessage"/></p>
			</body>
		</html>
	</xsl:template>
</xsl:stylesheet>
