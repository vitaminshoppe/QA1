<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:template match="/">
     <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:koun="http://www.vsi.com/kount" >
   <soapenv:Header>
       
   </soapenv:Header>
   <soapenv:Body>
      <koun:kountRequest><xsl:value-of select = "/FraudRequest/@NVPRequest"/></koun:kountRequest>              
   </soapenv:Body>
</soapenv:Envelope>
      </xsl:template>
</xsl:stylesheet>