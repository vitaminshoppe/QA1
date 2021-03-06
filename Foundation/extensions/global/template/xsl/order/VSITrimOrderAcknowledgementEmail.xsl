<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
   <xsl:output version="1.0" indent="yes" encoding="UTF-8" method="xml" />
   <xsl:template match="/">
   <Order>
   
   
   <xsl:copy-of select="/Order/@*"/>
     <xsl:copy-of select="/Order/*[not(self::OrderLines )]"/>
   <OrderLines>
   <xsl:copy-of select="/Order/OrderLines/@*"/>
   
   <xsl:for-each select = "/Order/OrderLines/OrderLine[@Status != 'Cancelled']">
   
  
   <OrderLine>
   <xsl:copy-of select="./@*"/>
     <xsl:copy-of select="./*"/>
   </OrderLine>
    </xsl:for-each>
   </OrderLines>
   </Order>
      
   </xsl:template>
</xsl:stylesheet>