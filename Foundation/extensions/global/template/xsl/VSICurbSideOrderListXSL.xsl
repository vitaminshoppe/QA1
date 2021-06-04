<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes" />
	<xsl:template match="/VSICurbSideOrder">
		<Order ReadFromHistory="N" DraftOrderFlag="N" DocumentType="0001">
			 <xsl:attribute name="BillToID"><xsl:value-of select="/VSICurbSideOrder/@BillToID" /></xsl:attribute>			 
			 <xsl:attribute name="FromOrderDate"><xsl:value-of select="/VSICurbSideOrder/@FromOrderDate" /></xsl:attribute>
			 <xsl:attribute name="ToOrderDate"><xsl:value-of select="/VSICurbSideOrder/@ToOrderDate" /></xsl:attribute>
			 <xsl:attribute name="OrderDateQryType">BETWEEN</xsl:attribute>
			 <OrderStatus>
				<ComplexQuery Operator="AND">		
					<Exp Name="Status" Value="3350.30" StatusQryType="GE"/>
					<Exp Name="Status" Value="9000" StatusQryType="NE"/> 
					<Exp Name="Status" Value="3700" StatusQryType="NE"/>		
				</ComplexQuery>
			 </OrderStatus>
			 <OrderLine LineType="SHIP_TO_HOME" LineTypeQryType="NE"/>
		</Order>	
	</xsl:template>	
</xsl:stylesheet>