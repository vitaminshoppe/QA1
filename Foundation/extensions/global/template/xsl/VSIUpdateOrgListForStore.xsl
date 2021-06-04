<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
 <xsl:output omit-xml-declaration="yes" indent="yes" />
 <xsl:strip-space elements="*"/>
<xsl:template match="node()|@*">
<xsl:copy>
       <xsl:apply-templates select="node()|@*"/>
     </xsl:copy>
 </xsl:template>
   <xsl:template match="EnterpriseOrgList">
    <xsl:copy>
      <xsl:copy-of select="@*"/>
      <xsl:copy-of select="node()"/>
     <OrgEnterprise EnterpriseOrganizationKey="VSI.com"/>
     <OrgEnterprise EnterpriseOrganizationKey="VS_AMAZON"/>
     <OrgEnterprise EnterpriseOrganizationKey="WALMART"/>
     <OrgEnterprise EnterpriseOrganizationKey="GOOGLE_EXPRESS"/>
     <OrgEnterprise EnterpriseOrganizationKey="ADP"/>
	 <OrgEnterprise EnterpriseOrganizationKey="NAVY_EXCHANGE"/>
    </xsl:copy>
 </xsl:template>
</xsl:stylesheet>