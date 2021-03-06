<%/*******************************************************************************
Licensed Materials - Property of IBM
IBM Sterling Selling And Fulfillment Suite
(C) Copyright IBM Corp. 2005, 2013 All Rights Reserved.
US Government Users Restricted Rights - Use, duplication or disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 ********************************************************************************/%>
<%@include file="/yfsjspcommon/yfsutil.jspf"%>
<%@include file="/console/jsp/currencyutils.jspf"%>
<%@include file="/console/jsp/modificationutils.jspf"%>
<%@ page import="com.yantra.yfs.ui.backend.*"%>
<script language="javascript" src="<%=request.getContextPath()%>/console/scripts/modificationreason.js"></script>
<script language="javascript" src="<%=request.getContextPath()%>/console/scripts/om.js"></script>
	
	<table  editable="false" width="100%" class="table" cellspacing="0" cellpadding="0">
		<thead>
			<tr>
				<td class="checkboxheader" sortable="no">
						<input type="checkbox"     onclick='doCheckAll(this)'   ></input>
				</td>
				<td class="tablecolumnheader" sortable="yes">
					<yfc:i18n>Line_#</yfc:i18n>
				</td>
				<td class="tablecolumnheader" sortable="yes">
					<yfc:i18n>Item_ID</yfc:i18n>
				</td>
				<td class="tablecolumnheader" sortable="yes">
					<yfc:i18n>PC</yfc:i18n>
				</td>
				<td class="tablecolumnheader" sortable="yes">
					<yfc:i18n>UOM</yfc:i18n>
				</td>
				<td class="tablecolumnheader" sortable="yes">
					<yfc:i18n>Description</yfc:i18n>
				</td>
				<td class="tablecolumnheader" sortable="yes">
					<yfc:i18n>Line_Quantity</yfc:i18n>
				</td>
				<td class="tablecolumnheader" sortable="yes">
					<yfc:i18n>Total_Amount</yfc:i18n>
				</td>
				<td class="tablecolumnheader" sortable="yes">
					<yfc:i18n>Status</yfc:i18n>
				</td>
			</tr>
		</thead>
		<tbody>
			<yfc:loopXML binding="xml:/OrderLine/ProductAssociations/@ProductAssociation" id="DeliveryLine">
				<tr>
					<td class="checkboxcolumn">
							<yfc:makeXMLInput  name="orderLineKey"  >
								<yfc:makeXMLKey  value="xml:DeliveryLine:/ProductAssociation/ProductLine/@OrderLineKey" binding="xml:/OrderLineDetail/@OrderLineKey" />
								<yfc:makeXMLKey  value="xml:DeliveryLine:/ProductAssociation/ServiceLine/@OrderLineKey" binding="xml:/OrderLineDetail/@DeliveryOrderLineKey" />
							</yfc:makeXMLInput>

							<input type="checkbox" name="chkEntityKey" value='<%=getParameter("orderLineKey")%>'/>
					</td>
					<td class="tablecolumn">
						<a   <%=getDetailHrefOptions("L01",getParameter("orderLineKey"),"ShowReleaseNo=Y")%>   >
							<yfc:getXMLValue  binding="xml:DeliveryLine:/ProductAssociation/ProductLine/@PrimeLineNo" />
						</a>
					</td>
					<td class="tablecolumn">
							<% request.setAttribute("DeliveryLine", pageContext.getAttribute("DeliveryLine")); %>
							<yfc:callAPI  apiID="AP1" />
							<yfc:getXMLValue  binding="xml:DeliveryItem:/OrderLine/Item/@ItemID" />
					</td>
					<td class="tablecolumn">
							<yfc:getXMLValue  binding="xml:DeliveryItem:/OrderLine/Item/@ProductClass" />
					</td>
					<td class="tablecolumn">
							<yfc:getXMLValue  binding="xml:DeliveryItem:/OrderLine/Item/@UnitOfMeasure" />
					</td>
					<td class="tablecolumn">
							<yfc:getXMLValue  binding="xml:DeliveryItem:/OrderLine/Item/@ItemShortDesc" />
					</td>
					<td class="tablecolumn">
							<yfc:getXMLValue  binding="xml:DeliveryItem:/OrderLine/@OrderedQty" />
					</td>
					<td class="numerictablecolumn" nowrap="true" sortValue="<%=getNumericValue("xml:DeliveryItem:/OrderLine/LinePriceInfo/@LineTotal")%>">
						<% String[] curr0 = getLocalizedCurrencySymbol( getValue("CurrencyList", "xml:/CurrencyList/Currency/@PrefixSymbol"), getValue("CurrencyList", "xml:/CurrencyList/Currency/@Currency"), getValue("CurrencyList", "xml:/CurrencyList/Currency/@PostfixSymbol"));%><%=curr0[0]%>&nbsp;<yfc:getXMLValue binding="xml:DeliveryItem:/OrderLine/LinePriceInfo/@LineTotal"/>&nbsp;<%=curr0[1]%>
					</td>
					<td class="tablecolumn">
							<%=displayOrderStatus(getValue("DeliveryItem","xml:/OrderLine/@MultipleStatusesExist"),getValue("DeliveryItem","xml:/OrderLine/@MaxLineStatusDesc"),true)%>
					</td>
				</tr>
			</yfc:loopXML>
		</tbody>
	</table>
