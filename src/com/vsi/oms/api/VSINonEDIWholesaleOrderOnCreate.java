package com.vsi.oms.api;

import java.rmi.RemoteException;
import java.util.ArrayList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.vsi.oms.utils.VSIConstants;
import com.vsi.oms.utils.VSIUtils;
import com.vsi.oms.utils.XMLUtil;
import com.yantra.interop.japi.YIFClientCreationException;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.core.YFSSystem;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;

public class VSINonEDIWholesaleOrderOnCreate {
	/**
	 * This custom implementation is used to create the NON EDI wholesale Orders
	 * We will manually update the Elements/Atrributes we require in OMS
	 * 
	 * @param env
	 * @param inXML
	 * @throws Exception
	 */
	private YFCLogCategory log = YFCLogCategory.instance(VSINonEDIWholesaleOrderOnCreate.class);
	private static final String TAG = VSINonEDIWholesaleOrderOnCreate.class.getSimpleName();

	public Document createNonEDIOrder(YFSEnvironment env, Document inXML) throws Exception {
		
		log.beginTimer("VSINonEDIWholesaleOrderOnCreate.createNonEDIOrder");		
		printLogs("================Inside VSINonEDIWholesaleOrderOnCreate================================");
		printLogs("Printing Input XML :" + SCXmlUtil.getString(inXML));		

		try {
			Element rootElement = inXML.getDocumentElement();			
			String strEnterprise = rootElement.getAttribute(VSIConstants.ATTR_ENTERPRISE_CODE);
		
			//check in git hub
			String strFirstName=YFSSystem.getProperty(strEnterprise.concat(VSIConstants.ATTR_PERSON_INFO_FIRSTNAME));
			String strAddressLine1=YFSSystem.getProperty(strEnterprise.concat(VSIConstants.ATTR_PERSON_INFO_ADDR_LINE_1));
			String strAddressLine2=YFSSystem.getProperty(strEnterprise.concat(VSIConstants.ATTR_PERSON_INFO_ADDR_LINE_2));
			String strCity=YFSSystem.getProperty(strEnterprise.concat(VSIConstants.ATTR_PERSON_INFO_CITY));
			String strState=YFSSystem.getProperty(strEnterprise.concat(VSIConstants.ATTR_PERSON_INFO_STATE));
			String strCountry=YFSSystem.getProperty(strEnterprise.concat(VSIConstants.ATTR_PERSON_INFO_COUNTRY));
			String strZipCode=YFSSystem.getProperty(strEnterprise.concat(VSIConstants.ATTR_PERSON_INFO_ZIPCODE));
				
			//OMS-3472 Changes -- Start
			String strBillToFirstName=YFSSystem.getProperty(strEnterprise.concat(VSIConstants.ATTR_PERSON_INFO_BILLTO_FIRSTNAME));
			String strBillToAddressLine1=YFSSystem.getProperty(strEnterprise.concat(VSIConstants.ATTR_PERSON_INFO_BILLTO_ADDR_LINE_1));
			String strBillToAddressLine2=YFSSystem.getProperty(strEnterprise.concat(VSIConstants.ATTR_PERSON_INFO_BILLTO_ADDR_LINE_2));
			String strBillToCity=YFSSystem.getProperty(strEnterprise.concat(VSIConstants.ATTR_PERSON_INFO_BILLTO_CITY));
			String strBillToState=YFSSystem.getProperty(strEnterprise.concat(VSIConstants.ATTR_PERSON_INFO_BILLTO_STATE));
			String strBillToCountry=YFSSystem.getProperty(strEnterprise.concat(VSIConstants.ATTR_PERSON_INFO_BILLTO_COUNTRY));
			String strBillToZipCode=YFSSystem.getProperty(strEnterprise.concat(VSIConstants.ATTR_PERSON_INFO_BILLTO_ZIPCODE));
			
			String strDTCOrder=YFSSystem.getProperty(strEnterprise.concat(VSIConstants.ATTR_EXTN_DTCORDER));
			String strGuestCheckout=YFSSystem.getProperty(strEnterprise.concat(VSIConstants.ATTR_EXTN_GUESTCHECKOUT));
			String strTaxCalculated=YFSSystem.getProperty(strEnterprise.concat(VSIConstants.ATTR_EXTN_TAXCALCULATED));
			String strType=YFSSystem.getProperty(strEnterprise.concat(VSIConstants.ATTR_EXTN_TYPE_WH));
			String strDepartmentID=YFSSystem.getProperty(strEnterprise.concat(VSIConstants.ATTR_EXTN_DEPARTMENTID));
			//OMS-3472 Changes -- End

			Element orderElement = inXML.getDocumentElement();
			Element eleOrderPersonInfoShipTo = SCXmlUtil.createChild(orderElement,  VSIConstants.ELE_PERSON_INFO_SHIP_TO);
			eleOrderPersonInfoShipTo.setAttribute(VSIConstants.ATTR_FIRST_NAME, strFirstName);
			eleOrderPersonInfoShipTo.setAttribute(VSIConstants.ATTR_ADDR_LINE_1, strAddressLine1);
			eleOrderPersonInfoShipTo.setAttribute(VSIConstants.ATTR_ADDR_LINE_2, strAddressLine2);
			eleOrderPersonInfoShipTo.setAttribute(VSIConstants.ATTR_CITY, strCity);
			eleOrderPersonInfoShipTo.setAttribute(VSIConstants.ATTR_STATE, strState);
			eleOrderPersonInfoShipTo.setAttribute(VSIConstants.ATTR_COUNTRY, strCountry);
			eleOrderPersonInfoShipTo.setAttribute(VSIConstants.ATTR_ZIPCODE, strZipCode);
			

			Element eleOrderPersonInfoBillTo= SCXmlUtil.createChild(orderElement,  VSIConstants.ELE_PERSON_INFO_BILL_TO);
			eleOrderPersonInfoBillTo.setAttribute(VSIConstants.ATTR_FIRST_NAME, strBillToFirstName);		//OMS-3472 Change
			eleOrderPersonInfoBillTo.setAttribute(VSIConstants.ATTR_ADDR_LINE_1, strBillToAddressLine1);		//OMS-3472 Change
			eleOrderPersonInfoBillTo.setAttribute(VSIConstants.ATTR_ADDR_LINE_2, strBillToAddressLine2);		//OMS-3472 Change
			eleOrderPersonInfoBillTo.setAttribute(VSIConstants.ATTR_CITY, strBillToCity);		//OMS-3472 Change
			eleOrderPersonInfoBillTo.setAttribute(VSIConstants.ATTR_STATE, strBillToState);		//OMS-3472 Change
			eleOrderPersonInfoBillTo.setAttribute(VSIConstants.ATTR_COUNTRY, strBillToCountry);		//OMS-3472 Change
			eleOrderPersonInfoBillTo.setAttribute(VSIConstants.ATTR_ZIPCODE, strBillToZipCode);		//OMS-3472 Change
			
			//OMS-3472 Changes -- Start
			Element eleOrderExtn=SCXmlUtil.createChild(orderElement, VSIConstants.ELE_EXTN);
			eleOrderExtn.setAttribute(VSIConstants.ATTR_EXTN_DTC_ORDER, strDTCOrder);
			eleOrderExtn.setAttribute(VSIConstants.ATTR_GUEST_CHECKOUT, strGuestCheckout);
			eleOrderExtn.setAttribute("ExtnIsTaxCalculated", strTaxCalculated);
			eleOrderExtn.setAttribute("Type", strType);
			eleOrderExtn.setAttribute("ExtnDepartmentID", strDepartmentID);
			
			String strOrderType=orderElement.getAttribute(VSIConstants.ATTR_ORDER_TYPE);
			if(YFCCommon.isVoid(strOrderType)) {
				orderElement.setAttribute(VSIConstants.ATTR_ORDER_TYPE, VSIConstants.WHOLESALE);
			}
			
			String strEntCode=orderElement.getAttribute(VSIConstants.ATTR_ENTERPRISE_CODE);
			
			//OMS-3472 Changes -- End
			
			Element eleOrderLines = SCXmlUtil.getChildElement(orderElement, VSIConstants.ELE_ORDER_LINES);
			ArrayList<Element> alOrderLines = SCXmlUtil.getChildren(eleOrderLines, VSIConstants.ELE_ORDER_LINE);						

			for (Element eleOrderLine : alOrderLines) {
				//OMS-3472 Changes -- Start
				String strMarkForStore=null;
				Element elePersonInfoShip = SCXmlUtil.getChildElement(eleOrderLine, "PersonInfoShip");
				if(!YFCCommon.isVoid(elePersonInfoShip)) {
					Element eleOrdLnShpExtn=SCXmlUtil.getChildElement(elePersonInfoShip, VSIConstants.ELE_EXTN);
					if(!YFCCommon.isVoid(eleOrdLnShpExtn)) {
						strMarkForStore=eleOrdLnShpExtn.getAttribute(VSIConstants.ATTR_EXTN_MARK_FOR_STORE_NO);						
					}
					eleOrderLine.removeChild(elePersonInfoShip);
				}
				
				Element eleOrdLnShipTo=SCXmlUtil.createChild(eleOrderLine, VSIConstants.ELE_PERSON_INFO_SHIP_TO);
				eleOrdLnShipTo.setAttribute(VSIConstants.ATTR_FIRST_NAME, strFirstName);
				eleOrdLnShipTo.setAttribute(VSIConstants.ATTR_ADDR_LINE_1, strAddressLine1);
				eleOrdLnShipTo.setAttribute(VSIConstants.ATTR_ADDR_LINE_2, strAddressLine2);
				eleOrdLnShipTo.setAttribute(VSIConstants.ATTR_CITY, strCity);
				eleOrdLnShipTo.setAttribute(VSIConstants.ATTR_STATE, strState);
				eleOrdLnShipTo.setAttribute(VSIConstants.ATTR_COUNTRY, strCountry);
				eleOrdLnShipTo.setAttribute(VSIConstants.ATTR_ZIPCODE, strZipCode);
				
				if(!YFCCommon.isVoid(strMarkForStore)) {
					Element eleOrdLnShipToExtn=SCXmlUtil.createChild(eleOrdLnShipTo, VSIConstants.ELE_EXTN);
					eleOrdLnShipToExtn.setAttribute(VSIConstants.ATTR_EXTN_MARK_FOR_STORE_NO, strMarkForStore);
				}
				
				Element eleItemIn=SCXmlUtil.getChildElement(eleOrderLine, VSIConstants.ELE_ITEM);
				String strItemId=eleItemIn.getAttribute(VSIConstants.ATTR_ITEM_ID);
				Element eleLinePriceInfo=SCXmlUtil.getChildElement(eleOrderLine, VSIConstants.ELE_LINE_PRICE);
				
				validateLinePrice(env, strEntCode, eleOrderLine, strItemId, eleLinePriceInfo);
				
				Document docGetItemListIn=SCXmlUtil.createDocument(VSIConstants.ELE_ITEM);
				Element eleGetItemListIn=docGetItemListIn.getDocumentElement();
				eleGetItemListIn.setAttribute(VSIConstants.ATTR_ITEM_ID, strItemId);
				
				printLogs("Input to getItemList API: "+SCXmlUtil.getString(docGetItemListIn));				
				Document docGetItemListOut=VSIUtils.invokeAPI(env, "global/template/api/getItemList_NonEDIOrderCreate.xml",VSIConstants.API_GET_ITEM_LIST, docGetItemListIn);				
				printLogs("Output from getItemList API: "+SCXmlUtil.getString(docGetItemListOut));				
				
				updateItemAttributes(eleOrderLine, eleItemIn, docGetItemListOut);				
			}
			//OMS-3472 Changes -- End
			
		}
		catch(Exception ex) {
			printLogs("Exception in VSINonEDIWholesaleOrderOnCreate Class and createNonEDIOrder Method");
			printLogs("The exception is [ "+ ex.getMessage() +" ]");
			throw new YFSException();			
		}
		
		
		printLogs("Printing Output XML: "+SCXmlUtil.getString(inXML));
		printLogs("================Exiting VSINonEDIWholesaleOrderOnCreate Class and createNonEDIOrder Method================================");		
		
		log.endTimer("VSINonEDIWholesaleOrderOnCreate.createNonEDIOrder");

		return inXML;
	}

	private void updateItemAttributes(Element eleOrderLine, Element eleItemIn, Document docGetItemListOut)
			throws Exception {
		String strExtndDesc=null;
		String strUPCCode=null;
		
		Element eleGetItemListOut=docGetItemListOut.getDocumentElement();
		if(eleGetItemListOut.hasChildNodes()) {
			Element eleItem=SCXmlUtil.getChildElement(eleGetItemListOut, VSIConstants.ELE_ITEM);
			Element elePrimaryInfo=SCXmlUtil.getChildElement(eleItem, VSIConstants.ELE_PRIMARY_INFORMATION);
			strExtndDesc=elePrimaryInfo.getAttribute("ExtendedDescription");					
			Document docItem=XMLUtil.createDocumentFromElement(eleItem);
			Element eleItemAlias=XMLUtil.getElementByXPath(docItem, "/Item/ItemAliasList/ItemAlias[@AliasName='UPC']");
			if(!YFCCommon.isVoid(eleItemAlias)) {
				strUPCCode=eleItemAlias.getAttribute(VSIConstants.ATTR_ALIAS_VALUE);
			}
		}
		
		Element eleOrdLnExtn=SCXmlUtil.getChildElement(eleOrderLine, VSIConstants.ELE_EXTN);
		if(YFCCommon.isVoid(eleOrdLnExtn)) {
			eleOrdLnExtn=SCXmlUtil.createChild(eleOrderLine, VSIConstants.ELE_EXTN);
		}
		
		if(!YFCCommon.isVoid(strExtndDesc)) {					
			eleOrdLnExtn.setAttribute("ExtendedDescription", strExtndDesc);
		}
		
		if(!YFCCommon.isVoid(strUPCCode)) {					
			eleItemIn.setAttribute(VSIConstants.ATTR_UPCCODE, strUPCCode);
		}
	}

	private void validateLinePrice(YFSEnvironment env, String strEntCode, Element eleOrderLine, String strItemId,
			Element eleLinePriceInfo) throws YIFClientCreationException, RemoteException {
		
		if(YFCCommon.isVoid(eleLinePriceInfo)) {
			eleLinePriceInfo=SCXmlUtil.createChild(eleOrderLine, VSIConstants.ELE_LINE_PRICE);
		}
		String strUnitPrice=eleLinePriceInfo.getAttribute(VSIConstants.ATTR_UNIT_PRICE);
		if(YFCCommon.isVoid(strUnitPrice)) {
			
			Document getPriceListLine = SCXmlUtil.createDocument(VSIConstants.ELE_PRICELIST_LINE);
			Element elePricelistLine = getPriceListLine.getDocumentElement();
			SCXmlUtil.setAttribute(elePricelistLine, VSIConstants.ATTR_ORGANIZATION_CODE, strEntCode);
			Element eleItem = SCXmlUtil.createChild(elePricelistLine, VSIConstants.ELE_ITEM);
			eleItem.setAttribute(VSIConstants.ATTR_ITEM_ID, strItemId);

			Element elePricelistHeader = SCXmlUtil.createChild(elePricelistLine, VSIConstants.ELE_PRICE_LIST_HEADER);
			elePricelistHeader.setAttribute(VSIConstants.ATTR_PRICING_STATUS, "ACTIVE");
			
			if(log.isDebugEnabled()){
				log.debug("Input for getPricelistLineListForItem: "
						+ SCXmlUtil.getString(getPriceListLine));
			}
			
			Document getPricelistLineListForItemOutput = VSIUtils.invokeAPI(env, "",
					VSIConstants.API_GET_PRICE_LIST_LINE_LIST_FOR_ITEM, getPriceListLine);

			if(log.isDebugEnabled()){
				log.debug("Output for getPricelistLineListForItem: "
						+ SCXmlUtil.getString(getPricelistLineListForItemOutput));
			}
			
			Element elePricelistLineList = getPricelistLineListForItemOutput.getDocumentElement();
			if (elePricelistLineList.hasChildNodes()) {
				Element elePriceLineList = SCXmlUtil.getChildElement(elePricelistLineList, VSIConstants.ELE_PRICELIST_LINE);
				String unitPrice = elePriceLineList.getAttribute(VSIConstants.ATTR_UNIT_PRICE);
				eleLinePriceInfo.setAttribute(VSIConstants.ATTR_UNIT_PRICE, unitPrice);
			}
		}
	}
	
	private void printLogs(String mesg) {
		if(log.isDebugEnabled()){
			log.debug(TAG +" : "+mesg);
		}
	}
}

