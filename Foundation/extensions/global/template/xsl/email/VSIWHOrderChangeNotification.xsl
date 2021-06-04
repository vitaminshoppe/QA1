<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format">
	<xsl:output method="html" indent="yes"/>
	<xsl:template match="*">
		<xsl:comment>CONTENT_TYPE=text/html</xsl:comment>
		<html>
			<body style="margin-left: 25px">
				<p>Partner Name: <xsl:value-of select="//@EnterpriseCode"/> </p>				
				<p>OMS Order Number: <xsl:value-of select="//@OrderNo"/> </p>				
				<p>Partner PO Number: <xsl:value-of select="//@CustomerPONo"/> </p>			
				<p>Type of Change: <br/>
				<xsl:for-each select="/Order/OrderLines/OrderLine">
				<b>Line Details: </b><br/>
						PrimeLineNo: <xsl:value-of select="@PrimeLineNo"/><br/>
						ItemId: <xsl:value-of select="@ItemID"/><br/>
						StoreNo: <xsl:value-of select="@ExtnMarkForStoreNo"/><br/>
					<xsl:for-each select="LineChanges/LineChange">											
						<xsl:choose>                                                       
						<xsl:when test="@ChangeType='AddLine'">
						<b>Change Details: </b><br/>
							ChangeType: New Line added <br/>
							NewLineNo: <xsl:value-of select="@NewValue"/><br/>
							Quantity: <xsl:value-of select="@Quantity"/><br/>
						</xsl:when>						
						<xsl:otherwise>
						<xsl:choose>                                                       
						<xsl:when test="@ChangeType='DateChange'">
						<b>Change Details: </b><br/>
							ChangeType: <xsl:value-of select="@ChangeType"/><br/>
							DateType: <xsl:value-of select="@DateType"/><br/>
							OldValue: <xsl:value-of select="@OldValue"/><br/>
							NewValue: <xsl:value-of select="@NewValue"/><br/> 
						</xsl:when>
						</xsl:choose>
						<xsl:choose>                                                       
						<xsl:when test="@ChangeType='QuantityChange' and @OldValue!='0.00'">
						<b>Change Details: </b><br/>
							ChangeType: <xsl:value-of select="@ChangeType"/><br/>
							OldValue: <xsl:value-of select="@OldValue"/><br/>
							NewValue: <xsl:value-of select="@NewValue"/><br/>
						</xsl:when>
						</xsl:choose>												
						</xsl:otherwise>						
						</xsl:choose>
					</xsl:for-each>
				</xsl:for-each>
				</p>
				<p>Current Order Status: <xsl:value-of select="//@Status"/> </p>				
				<p>Change Request Status : <xsl:value-of select="//@ChangeStatus"/> </p>				
				</body>
		</html>
	</xsl:template>
</xsl:stylesheet>
