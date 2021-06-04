package com.vsi.oms.api;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.vsi.oms.utils.VSIBaseCustomAPI;
import com.vsi.oms.utils.VSIConstants;
import com.vsi.oms.utils.VSIUtils;
import com.vsi.oms.utils.XMLUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;


	
public class VSIRemoveShpChrgAndShpTax extends VSIBaseCustomAPI implements VSIConstants
{
	private YFCLogCategory log = YFCLogCategory.instance(VSIRemoveShpChrgAndShpTax.class);	
	
	public Document removeShpChrgAndShpTax(YFSEnvironment env, Document inXml)
	{
         
         if(log.isDebugEnabled()){
				log.debug("VSIRemoveShpChrgAndShpTax.removeShpChrgAndShpTax() i/p xml =>"+SCXmlUtil.getString(inXml));
			}
         
		try {	
			Element eleOrder = (Element) inXml.getElementsByTagName(ELE_ORDER).item(0);
			String strOrderType= eleOrder.getAttribute(ATTR_ORDER_TYPE);
			String strOrderHeaderKey=eleOrder.getAttribute(ATTR_ORDER_HEADER_KEY);
			Element eleHeaderCharges = (Element) inXml.getElementsByTagName(ELE_HEADER_CHARGES).item(0);	
			NodeList nleleHeaderCharge = eleHeaderCharges.getElementsByTagName(ELE_HEADER_CHARGE);			
			int headerChargeLength = nleleHeaderCharge.getLength();
			if(headerChargeLength > 0)
			{
				for (int j = 0; j < headerChargeLength; j++) {	
					Element eleHeaderCharge= (Element) nleleHeaderCharge.item(j);
					String strChargeName= eleHeaderCharge.getAttribute(ATTR_CHARGE_NAME);
					if (strChargeName.equalsIgnoreCase(SHIPPING_CHARGE_CTGY)) {
						String strChargeAmount= eleHeaderCharge.getAttribute(ATTR_CHARGE_AMOUNT);
						double chargeAmount = Double.parseDouble(strChargeAmount);
						if (chargeAmount > 0.00)
						{
							if ((!YFCCommon.isStringVoid(strOrderType)) && !(ATTR_ORDER_TYPE_POS.equalsIgnoreCase(strOrderType) || MARKETPLACE.equalsIgnoreCase(strOrderType) || WHOLESALE.equalsIgnoreCase(strOrderType))){
								Document getOrderListInXML=XMLUtil.createDocument(ELE_ORDER);
								Element getOrderListEle = getOrderListInXML.getDocumentElement();
								getOrderListEle.setAttribute(ATTR_ORDER_HEADER_KEY,strOrderHeaderKey);	
								Document getOrderListOutXML  = VSIUtils.invokeAPI(env,TEMPLATE_GET_ORDER_LIST_FOR_ORDER_MONITOR,API_GET_ORDER_LIST,getOrderListInXML);		
								Element eleOrderLines = (Element) getOrderListOutXML.getElementsByTagName(ELE_ORDER_LINES).item(0);
								NodeList orderLineList = eleOrderLines.getElementsByTagName(ELE_ORDER_LINE);
								int linelength = orderLineList.getLength();
								int shipToHomeCancelCount = 0,shipToHomeCount =0;
								for(int i=0;i<linelength;i++){
							    Element orderLine= (Element) orderLineList.item(i);
								String maxLineStatus = orderLine.getAttribute(ATTR_MAX_LINE_STATUS);
								String lineType= orderLine.getAttribute(ATTR_LINE_TYPE);
								if (maxLineStatus.equalsIgnoreCase(STATUS_CANCEL) && lineType.equalsIgnoreCase(LINETYPE_STH))
									shipToHomeCancelCount++;
							    if (lineType.equalsIgnoreCase(LINETYPE_STH))
							    	shipToHomeCount++;
							}
						     if (shipToHomeCount==shipToHomeCancelCount) {
						    	 log.info("Inside Count Success =>");		
						    	 eleHeaderCharge.setAttribute(ATTR_CHARGE_AMOUNT, ZERO_DOUBLE);
						    	 eleHeaderCharge.setAttribute(ATTR_REMAINING_CHARGE_AMOUNT, ZERO_DOUBLE);
						    	 Element eleHeaderTaxes = (Element) inXml.getElementsByTagName(ELE_HEADER_TAXES).item(0);	
								 NodeList nleleHeaderTax = eleHeaderTaxes.getElementsByTagName(ELE_HEADER_TAX);
								 int headerTaxLength = nleleHeaderTax.getLength();
									if(headerTaxLength > 0)
									{
										for (int k = 0; k < headerTaxLength; k++) {	
											Element eleHeaderTax= (Element) nleleHeaderTax.item(k);
											String strTaxChargeName= eleHeaderTax.getAttribute(ATTR_CHARGE_NAME);
											if (strTaxChargeName.equalsIgnoreCase(SHIPPING_CHARGE_CTGY)) {
												eleHeaderTax.setAttribute(ATTR_REMAINING_TAX, ZERO_DOUBLE);
												eleHeaderTax.setAttribute(ATTR_TAX, ZERO_DOUBLE);
											}
						    	 
						     }
						    	 
						     } 
									Element taxSummary = (Element) eleHeaderTaxes.getElementsByTagName(ELE_TAX_SUMMARY).item(0);	
									NodeList nlTaxSummaryDetail = taxSummary.getElementsByTagName(TAX_SUMMARY_DETAIL);
									int taxSummaryDetailLength = nlTaxSummaryDetail.getLength();
									if(taxSummaryDetailLength > 0)
									{
										for (int l = 0; l < taxSummaryDetailLength; l++) {	
											Element eleTaxSummaryDetail= (Element) nlTaxSummaryDetail.item(l);
											String strTaxName= eleTaxSummaryDetail.getAttribute(ATTR_TAX_NAME);
											if (strTaxName.equalsIgnoreCase(SHIPPING_TAX_VALUE)) {
												eleTaxSummaryDetail.setAttribute(ATTR_OVERALL_TAX, ZERO_DOUBLE);
												eleTaxSummaryDetail.setAttribute(ATTR_REMAINING_TAX, ZERO_DOUBLE);
												}
											
											}
										}
									eleOrder.setAttribute(ATTR_OVERRIDE,FLAG_Y);
									eleOrder.setAttribute(ATTR_ACTION, ACTION_CAPS_MODIFY);
									VSIUtils.invokeAPI(env, API_CHANGE_ORDER, inXml);
									if(log.isDebugEnabled()){
										log.debug("VSIRemoveShpChrgAndShpTax.removeShpChrgAndShpTax() post changeorderxml => "+SCXmlUtil.getString(inXml));
									}
						     		}
								}
					
							}
						}
					}
				}

			}
		catch(Exception e) {
			e.printStackTrace();
			}
		if(log.isDebugEnabled()){
			log.debug("VSIRemoveShpChrgAndShpTax.removeShpChrgAndShpTax() final xml => "+SCXmlUtil.getString(inXml));
		}	
		return inXml;
		}
		
}


