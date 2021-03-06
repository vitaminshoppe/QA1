<%/*******************************************************************************
Licensed Materials - Property of IBM
IBM Sterling Selling And Fulfillment Suite
(C) Copyright IBM Corp. 2005, 2013 All Rights Reserved.
US Government Users Restricted Rights - Use, duplication or disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 ********************************************************************************/%>
<%@ include file="/yfsjspcommon/yfsutil.jspf" %>
<%@ include file="/console/jsp/modificationutils.jspf" %>
<%@ include file="/console/jsp/order.jspf" %>
<%@page import="java.util.Iterator" %>

<% 
setHistoryFlags((YFCElement) request.getAttribute("Order")); 
setHoldFuncFlags((YFCElement) request.getAttribute("Order"), isTrue("xml:/Rules/@RuleSetValue"));
%>

<yfc:callAPI apiID="AP1"/> <!-- getOrderInvoiceList [Populates DerivedOrderList] -->
<yfc:callAPI apiID="AP2"/> <!-- getOrderLineList [Populates ChainedOrderLineList] -->
<yfc:callAPI apiID="AP3"/> <!-- getOrderLineList [Populates DerivedOrderLineList] -->
<yfc:callAPI apiID="AP4"/> <!-- getShipmentListForOrder -->

<%
    prepareOrderElement((YFCElement) request.getAttribute("Order"),(YFCElement) request.getAttribute("DerivedOrderList"),(YFCElement) request.getAttribute("ChainedOrderList"));
    getTotalNumInvoices((YFCElement) request.getAttribute("OrderInvoiceList"),(YFCElement) request.getAttribute("DerivedOrderInvoiceList"));

	YFCElement oOrder = (YFCElement)request.getAttribute("Order");
	if (oOrder != null) {
		String sReceivingNode = oOrder.getAttribute("ReceivingNode");
		if (!equals(oOrder.getAttribute("DraftOrderFlag"),"Y"))
			if (isVoid(sReceivingNode) && oOrder.getChildElement("OrderLines") != null) {
				YFCElement oOrderLines = oOrder.getChildElement("OrderLines");
				for (Iterator oIter = oOrderLines.getChildren();oIter.hasNext();) {
					YFCElement oOrderLine = (YFCElement) oIter.next();
					if (!isVoid(oOrderLine.getAttribute("ReceivingNode"))) {
						oOrder.setAttribute("ReceiveFlag","Y");
						oOrder.setAttribute("ReceivingNodeReceiveFlag",oOrderLine.getAttribute("ReceivingNode"));
						break;
					}
				}
			} else {
				oOrder.setAttribute("ReceiveFlag","Y");
				oOrder.setAttribute("ReceivingNodeReceiveFlag",sReceivingNode);
			}
	}
	setActiveAlerts((YFCElement) request.getAttribute("InboxList"));
%>

<table cellSpacing=0 class="anchor" cellpadding="7px">
<tr>
    <td>
        <yfc:makeXMLInput name="orderHeaderKey">
            <yfc:makeXMLKey binding="xml:/Order/@OrderHeaderKey" value="xml:/Order/@OrderHeaderKey"/>
        </yfc:makeXMLInput>
        <input type="hidden" value='<%=getParameter("orderHeaderKey")%>' name="OrderEntityKey"/>
        <input type="hidden" <%=getTextOptions("xml:/Order/@OrderHeaderKey")%>/>
        <yfc:makeXMLInput name="receiveKey">
            <yfc:makeXMLKey binding="xml:/Receipt/@OrderHeaderKey" value="xml:/Order/@OrderHeaderKey"/>
            <yfc:makeXMLKey binding="xml:/Receipt/@DocumentType" value="xml:/Order/@DocumentType"/>
            <yfc:makeXMLKey binding="xml:/Receipt/@ReceivingNode" value="xml:/Order/@ReceivingNodeReceiveFlag"/>
        </yfc:makeXMLInput>
        <input type="hidden" value='<%=getParameter("receiveKey")%>' name="ReceiveKey"/>
    </td>
</tr>
<tr>
    <td colspan="4">
	    <jsp:include page="/yfc/innerpanel.jsp" flush="true" >
            <jsp:param name="CurrentInnerPanelID" value="I01"/>
            <jsp:param name="ModifyView" value="true"/>
			  <jsp:param name="getRequestDOM" value="Y"/>
        </jsp:include>
    </td>
</tr>
<tr>
    <td height="100%" width="33%" addressip="true">
        <jsp:include page="/yfc/innerpanel.jsp" flush="true" >
            <jsp:param name="CurrentInnerPanelID" value="I02"/>
            <jsp:param name="Path" value="xml:/Order/PersonInfoShipTo"/>
            <jsp:param name="DataXML" value="Order"/>
            <jsp:param name="AllowedModValue" value='<%=getModificationAllowedValueWithPermission("ShipToAddress", "xml:/Order/AllowedModifications")%>'/>
        </jsp:include>
    </td>
    <td height="100%" width="33%" addressip="true">
        <jsp:include page="/yfc/innerpanel.jsp" flush="true" >
            <jsp:param name="CurrentInnerPanelID" value="I05"/>
            <jsp:param name="Path" value="xml:/Order/PersonInfoBillTo"/>
            <jsp:param name="DataXML" value="Order"/>
            <jsp:param name="AllowedModValue" value='<%=getModificationAllowedValueWithPermission("BillToAddress", "xml:/Order/AllowedModifications")%>'/>
        </jsp:include>
    </td>
    <td height="100%" width="33%">
        <jsp:include page="/yfc/innerpanel.jsp" flush="true" >
            <jsp:param name="CurrentInnerPanelID" value="I03"/>
        </jsp:include>
    </td>
</tr>
<tr>
    <td colspan="4">
        <jsp:include page="/yfc/innerpanel.jsp" flush="true" >
            <jsp:param name="CurrentInnerPanelID" value="I06"/>
        </jsp:include>
    </td>
</tr>
</table>
