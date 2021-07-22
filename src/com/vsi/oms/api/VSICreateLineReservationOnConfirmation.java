package com.vsi.oms.api;


import java.rmi.RemoteException;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.vsi.oms.utils.VSIConstants;
import com.vsi.oms.utils.VSIDBUtil;
import com.vsi.oms.utils.VSIUtils;
import com.vsi.oms.utils.XMLUtil;
import com.yantra.interop.japi.YIFClientCreationException;
import com.yantra.yfc.core.YFCObject;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;

public class VSICreateLineReservationOnConfirmation implements VSIConstants{

	private YFCLogCategory log = YFCLogCategory.instance(VSICreateLineReservationOnConfirmation.class);
	//OMS-3821 Changes -- Start
	private static final String TAG = VSICreateLineReservationOnConfirmation.class.getSimpleName();
	//OMS-3821 Changes -- End
	public Document stampOrderLineReservation(YFSEnvironment env,Document inXML){
		printLogs("Input for VSICreateLineReservationOnConfirmation.stampOrderLineReservation : "+XMLUtil.getXMLString(inXML));
		
		try {
			boolean isChangeOrderReq=false;
			Element eleOrder=inXML.getDocumentElement();
			String strOrganizationCode=eleOrder.getAttribute(VSIConstants.ATTR_ENTERPRISE_CODE);
			String strAllocationRuleID=eleOrder.getAttribute(VSIConstants.ATTR_ALLOCATION_RULE_ID);
			
			Element elePersonInfoShipTo=SCXmlUtil.getChildElement(eleOrder, VSIConstants.ELE_PERSON_INFO_SHIP_TO);
			
			//create input for createInventoryActivityList
			Document docInventoryActivityListIP=SCXmlUtil.createDocument("InventoryActivityList");
			Element eleInventoryActivityList=docInventoryActivityListIP.getDocumentElement();
			
			//creating input for changeOrder
			Document docChangeOrderInput=XMLUtil.createDocument(VSIConstants.ELE_ORDER);
			Element eleOrderChangeIp=docChangeOrderInput.getDocumentElement();
			eleOrderChangeIp.setAttribute(VSIConstants.ATTR_ORDER_HEADER_KEY, eleOrder.getAttribute(VSIConstants.ATTR_ORDER_HEADER_KEY));
			Element eleOrderLinesChangeIp=SCXmlUtil.createChild(eleOrderChangeIp, VSIConstants.ELE_ORDER_LINES);
			//OMS-3821 Changes -- Start
			boolean bIsSHPLnPrsnt=false;
			boolean bIsMailInvsnsCSC=false;
			//OMS-3821 Changes -- End
			NodeList nlOrderLine = inXML.getElementsByTagName(VSIConstants.ELE_ORDER_LINE);
			int iLineCount=nlOrderLine.getLength();
			for (int i = 0; i < iLineCount; i++) {
				Element eleOrderLine=(Element)nlOrderLine.item(i);
				//OMS-3821 Changes -- Start
				String sDeliveryMethod = SCXmlUtil.getAttribute(eleOrderLine, ATTR_DELIVERY_METHOD);
				if(ATTR_DEL_METHOD_SHP.equals(sDeliveryMethod)) {
					bIsSHPLnPrsnt=true;
					String strCarrierSrvcCode=eleOrderLine.getAttribute(ATTR_CARRIER_SERVICE_CODE);
					if(MAIL_INNOVATIONS_CSC.equals(strCarrierSrvcCode)) {
						bIsMailInvsnsCSC=true;
					}
				}
				//OMS-3821 Changes -- End
				Element eleOrderLineChangeIp=SCXmlUtil.createChild(eleOrderLinesChangeIp, VSIConstants.ELE_ORDER_LINE);
				eleOrderLineChangeIp.setAttribute(VSIConstants.ATTR_ORDER_LINE_KEY, eleOrderLine.getAttribute(VSIConstants.ATTR_ORDER_LINE_KEY));
				Element eleOrderLineReservations=SCXmlUtil.createChild(eleOrderLineChangeIp, "OrderLineReservations");				
				
				Document docReserveInvOP=reserveAvailableInvetory(env,eleOrderLine,elePersonInfoShipTo,strOrganizationCode,
						strAllocationRuleID);
				isChangeOrderReq = processReserverInvOutput(isChangeOrderReq, eleOrderLineReservations,
						docReserveInvOP);
				//creating inventory activity entry for newly created item
				Element eleItem=SCXmlUtil.getChildElement(eleOrderLine, VSIConstants.ELE_ITEM);
				String strItemID=eleItem.getAttribute(VSIConstants.ATTR_ITEM_ID);
				String strProductClass=eleItem.getAttribute(VSIConstants.ATTR_PRODUCT_CLASS);
				String strUnitOfMeasure=eleItem.getAttribute(VSIConstants.ATTR_UOM);
				Element eleInventoryActivity=SCXmlUtil.createChild(eleInventoryActivityList, "InventoryActivity");
				eleInventoryActivity.setAttribute("CreateForInvItemsAtNode", "N");
				eleInventoryActivity.setAttribute(ATTR_ITEM_ID, strItemID);
				eleInventoryActivity.setAttribute("OrganizationCode", "ADP");
				eleInventoryActivity.setAttribute(ATTR_PRODUCT_CLASS, strProductClass);
				eleInventoryActivity.setAttribute(ATTR_UOM, strUnitOfMeasure);
					
				Element eleInventoryActivity1=SCXmlUtil.createChild(eleInventoryActivityList, "InventoryActivity");
				eleInventoryActivity1.setAttribute("CreateForInvItemsAtNode", "N");
				eleInventoryActivity1.setAttribute(ATTR_ITEM_ID, strItemID);
				eleInventoryActivity1.setAttribute("OrganizationCode", "VSI.com");
				eleInventoryActivity1.setAttribute(ATTR_PRODUCT_CLASS, strProductClass);
				eleInventoryActivity1.setAttribute(ATTR_UOM, strUnitOfMeasure);				
			}
			
			//OMS-3821 Changes -- Start			
			setSourcingClassification(env, eleOrder, elePersonInfoShipTo, bIsSHPLnPrsnt, bIsMailInvsnsCSC);
			//OMS-3821 Changes -- End
			
			printLogs("Input for createInventoryActivityList:  "+SCXmlUtil.getString(docInventoryActivityListIP));
			
			if(eleInventoryActivityList.hasChildNodes()) {
				VSIUtils.invokeAPI(env, "createInventoryActivityList", docInventoryActivityListIP);
			}
			if(isChangeOrderReq){
				printLogs("VSICreateLineReservationOnConfirmation: Input for ChangeOrder : "+XMLUtil.getXMLString(docChangeOrderInput));				
				VSIUtils.invokeAPI(env, VSIConstants.API_CHANGE_ORDER, docChangeOrderInput);
			}
			
			printLogs("output from  stampOrderLineReservation : "+XMLUtil.getXMLString(inXML));
			
			
		} catch (Exception e) {
			printLogs("Exception in VSICreateLineReservationOnConfirmation Class and stampOrderLineReservation Method");
			printLogs(EXCEPTION_PRINT_3+ e.getMessage() +EXCEPTION_PRINT_2);
		}
		
		return inXML;
	}

	private void setSourcingClassification(YFSEnvironment env, Element eleOrder, Element elePersonInfoShipTo,
			boolean bIsSHPLnPrsnt, boolean bIsMailInvsnsCSC) throws YIFClientCreationException, RemoteException {
		
		if(bIsSHPLnPrsnt){
			
			String	poBoxPatternString = ".*[Pp][ ]*[.]?[ ]*[Oo][ ]*[-.]?[ ]*([Bb][Oo][Xx])+.*";
			String addressLine1 = SCXmlUtil.getAttribute(elePersonInfoShipTo, ATTR_ADDRESS1);
			String addressLine2 = SCXmlUtil.getAttribute(elePersonInfoShipTo, ATTR_ADDRESS2);
			String sCountry = SCXmlUtil.getAttribute(elePersonInfoShipTo, ATTR_COUNTRY);
			String sStateIp = SCXmlUtil.getAttribute(elePersonInfoShipTo, ATTR_STATE);
			
			if(US.equals(sCountry)){
				if(Pattern.matches(poBoxPatternString, addressLine1) || Pattern.matches(poBoxPatternString, addressLine2) ||(bIsMailInvsnsCSC)) {
					changeOrderClassification(env, eleOrder.getAttribute(ATTR_ORDER_HEADER_KEY),NO_SFS_CLASSIFICATION);
				}
				if(!YFCObject.isVoid(sStateIp)){
					Document docgetCommonCodeInput = SCXmlUtil.createDocument("CommonCode");
					Element eleCommonCodeElement = docgetCommonCodeInput.getDocumentElement();
					eleCommonCodeElement.setAttribute(ATTR_CODE_TYPE,VSI_US_TERRITORIE_SR);
					eleCommonCodeElement.setAttribute(ATTR_CODE_VALUE, sStateIp);
					Document docgetCommonCodeOutput = VSIUtils.invokeAPI(env,API_COMMON_CODE_LIST,docgetCommonCodeInput);
					Element commonCodeListElement = docgetCommonCodeOutput.getDocumentElement();
					if(commonCodeListElement.hasChildNodes()) {
						changeOrderClassification(env, eleOrder.getAttribute(ATTR_ORDER_HEADER_KEY),US_TERRITORIES);
					}
				}
			}
		}
	}

	private boolean processReserverInvOutput(boolean isChangeOrderReq, Element eleOrderLineReservations,
			Document docReserveInvOP) {
		
		if(docReserveInvOP!=null) {
			NodeList nlReservation=docReserveInvOP.getElementsByTagName("Reservation");
			int iReservationCount=nlReservation.getLength();

			for (int j = 0; j < iReservationCount; j++) {
				isChangeOrderReq=true;
				Element eleOrderLineReservation=SCXmlUtil.createChild(eleOrderLineReservations, "OrderLineReservation");
				Element eleReservation=(Element)nlReservation.item(j);
				eleOrderLineReservation.setAttribute(ATTR_RESERVATIONID, eleReservation.getAttribute(ATTR_RESERVATIONID));
				eleOrderLineReservation.setAttribute(ATTR_ITEM_ID, eleReservation.getAttribute(ATTR_ITEM_ID));
				eleOrderLineReservation.setAttribute(ATTR_PRODUCT_CLASS, eleReservation.getAttribute(ATTR_PRODUCT_CLASS));
				eleOrderLineReservation.setAttribute(ATTR_UOM, eleReservation.getAttribute(ATTR_UOM));
				eleOrderLineReservation.setAttribute("Quantity", eleReservation.getAttribute("ReservedQty"));
				eleOrderLineReservation.setAttribute("Node", eleReservation.getAttribute("ShipNode"));
				eleOrderLineReservation.setAttribute("RequestedReservationDate", eleReservation.getAttribute("ReservationNodeShipDate"));
			}
		}
		return isChangeOrderReq;
	}

	private void changeOrderClassification(YFSEnvironment env, String orderHeaderKey, String classification) {
		try
			{
			Document changeOrderDoc = SCXmlUtil.createDocument(ELE_ORDER);
			Element elemOrder = changeOrderDoc.getDocumentElement();
			elemOrder.setAttribute(ATTR_OVERRIDE, FLAG_Y);
			elemOrder.setAttribute(ATTR_ORDER_HEADER_KEY, orderHeaderKey);
			elemOrder.setAttribute(ATTR_ACTION, ACTION_CAPS_MODIFY);
			elemOrder.setAttribute(ATTR_SOURCING_CLASSIFICATION, classification);
			VSIUtils.invokeAPI(env, API_CHANGE_ORDER, changeOrderDoc);
			}
			catch(Exception e)
			{
				printLogs("Exception in VSICreateLineReservationOnConfirmation Class and changeOrderClassification Method");
				printLogs(EXCEPTION_PRINT_3+ e.getMessage() +EXCEPTION_PRINT_2);
			}
		
	}

	private Document reserveAvailableInvetory(YFSEnvironment env,Element eleOrderLine,
			Element elePersonInfoShipTo,String strOrganizationCode, String strAllocationRuleID){
		Document docReserveInvIP=null;
		Document docReserveInvOP=null;
		try {
			docReserveInvIP=SCXmlUtil.createDocument("Promise");
			Element elePromise=docReserveInvIP.getDocumentElement();
			elePromise.setAttribute(VSIConstants.ATTR_ORGANIZATION_CODE, strOrganizationCode);
			if(!YFCCommon.isStringVoid(strAllocationRuleID)){
				elePromise.setAttribute(VSIConstants.ATTR_ALLOCATION_RULE_ID, strAllocationRuleID);
			}
			Element eleReservationParameters=SCXmlUtil.createChild(elePromise, "ReservationParameters");
			
			String strSeqName = "VSI_SEQ_RESERVATION_ID";
	    	String strSeqNumber = VSIDBUtil.getNextSequence(env, strSeqName);
	    	eleReservationParameters.setAttribute(ATTR_RESERVATIONID, strSeqNumber);
	    	
	    	Element elePromiseLines=SCXmlUtil.createChild(elePromise, "PromiseLines");
	    	Element elePromiseLine=SCXmlUtil.createChild(elePromiseLines, "PromiseLine");
	    	
	    	Element eleItem=SCXmlUtil.getChildElement(eleOrderLine, VSIConstants.ELE_ITEM);
	    	elePromiseLine.setAttribute("LineId", "1");
	    	elePromiseLine.setAttribute(ATTR_ITEM_ID, eleItem.getAttribute(VSIConstants.ATTR_ITEM_ID));
	    	elePromiseLine.setAttribute("RequiredQty", eleOrderLine.getAttribute(VSIConstants.ATTR_ORD_QTY));
	    	elePromiseLine.setAttribute(ATTR_UOM, eleItem.getAttribute(VSIConstants.ATTR_UOM));
	    	elePromiseLine.setAttribute(ATTR_PRODUCT_CLASS, eleItem.getAttribute(VSIConstants.ATTR_PRODUCT_CLASS));
	    	
	    	elePromiseLine.setAttribute("FulfillmentType", eleOrderLine.getAttribute(VSIConstants.ATTR_FULFILLMENT_TYPE));
	    	elePromiseLine.setAttribute("DeliveryMethod", eleOrderLine.getAttribute(VSIConstants.ATTR_DELIVERY_METHOD));
	    	String strShipNode=eleOrderLine.getAttribute(VSIConstants.ATTR_SHIP_NODE);
	    	if(!YFCCommon.isStringVoid(strShipNode))
	    		elePromiseLine.setAttribute(VSIConstants.ATTR_SHIP_NODE, strShipNode);
	    	
	    	Element eleShipToAddress=SCXmlUtil.createChild(elePromise, "ShipToAddress");
	    	eleShipToAddress.setAttribute("AddressLine1",  elePersonInfoShipTo.getAttribute("AddressLine1"));
	    	eleShipToAddress.setAttribute("City",  elePersonInfoShipTo.getAttribute("City"));
	    	eleShipToAddress.setAttribute("Country",  elePersonInfoShipTo.getAttribute("Country"));
	    	eleShipToAddress.setAttribute("DayPhone",  elePersonInfoShipTo.getAttribute("DayPhone"));
	    	eleShipToAddress.setAttribute("FirstName",  elePersonInfoShipTo.getAttribute("FirstName"));
	    	eleShipToAddress.setAttribute("LastName",  elePersonInfoShipTo.getAttribute("LastName"));
	    	eleShipToAddress.setAttribute("State",  elePersonInfoShipTo.getAttribute("State"));
	    	eleShipToAddress.setAttribute("ZipCode", elePersonInfoShipTo.getAttribute("ZipCode"));
	    	
	    	
	    	if(log.isDebugEnabled()){
	    		log.debug("input for reserveAvailableInventory API : "+SCXmlUtil.getString(docReserveInvIP));
	    	}
	    	
	    	//invoking reserveAvailableInventory 
	    	docReserveInvOP=VSIUtils.invokeAPI(env, "reserveAvailableInventory", docReserveInvIP);
			
		} catch (Exception e) {
			printLogs("Exception in VSICreateLineReservationOnConfirmation Class and reserveAvailableInvetory Method");
			printLogs(EXCEPTION_PRINT_3+ e.getMessage() +EXCEPTION_PRINT_2);
		}
		
		
		printLogs("reserveAvailableInvetory output : "+SCXmlUtil.getString(docReserveInvOP));
		
		return docReserveInvOP;
	}
	
	//OMS-3821 Changes -- Start
	private void printLogs(String mesg) {
		if(log.isDebugEnabled()){
			log.debug(TAG +" : "+mesg);
		}
	}
	//OMS-3821 Changes -- End
}