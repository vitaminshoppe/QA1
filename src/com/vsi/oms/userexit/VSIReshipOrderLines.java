package com.vsi.oms.userexit;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.sterlingcommerce.tools.datavalidator.XmlUtils;
import com.vsi.oms.utils.VSIConstants;
import com.vsi.oms.utils.VSIUtils;
import com.vsi.oms.utils.XMLUtil;
import com.yantra.interop.japi.YIFClientCreationException;
import com.yantra.yfc.core.YFCObject;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;
import com.yantra.yfs.japi.ue.YFSReshipOrderLinesUE;

public class VSIReshipOrderLines implements YFSReshipOrderLinesUE, VSIConstants{
	public Properties prop = null;
	private YFCLogCategory log = YFCLogCategory.instance(VSIReshipOrderLines.class);
	//OMS-3459 Changes -- Start
	private static final String TAG = VSIReshipOrderLines.class.getSimpleName();
	//OMS-3459 Changes -- End
	/**
	 * 
	 * @param env
	 * @param inXML
	 * @return
	 * @throws Exception
	 */
	
	public Document reshipOrderLines(YFSEnvironment env, Document inXML){
		
		printLogs("================Inside VSIReshipOrderLines class and reshipOrderLines method================================");
		printLogs("Printing Input XML: " + XmlUtils.getString(inXML));		
		 
		Element eleOrder = inXML.getDocumentElement();
		if(!YFCObject.isNull(eleOrder)){			
			Element eleOrderLines = SCXmlUtil.getChildElement(eleOrder, VSIConstants.ELE_ORDER_LINES);
			if(!YFCObject.isNull(eleOrderLines)){				
				ArrayList<Element> alOrderLines = SCXmlUtil.getChildren(eleOrderLines, VSIConstants.ELE_ORDER_LINE);
				//OMS-3821 Changes -- Start
				boolean bMailInovsnsPrsnt=false;
				//OMS-3821 Changes -- End
				for(Element eleOrderLine:alOrderLines){					
					String strDeliveryMethod = eleOrderLine.getAttribute(VSIConstants.ATTR_DELIVERY_METHOD);
					String strLineType = eleOrderLine.getAttribute(VSIConstants.ATTR_LINE_TYPE);
					//OMS-3459 Changes -- Start
					String strReshipParentLineKey=eleOrderLine.getAttribute(VSIConstants.A_RESHIP_PARENT_LINE_KEY);
					//OMS-3459 Changes -- End
					eleOrderLine.setAttribute(ATTR_CARRIER_SERVICE_CODE, (String) env.getTxnObject(ATTR_RESHIP_CSC));
					//OMS-3821 Changes -- Start
					String strCarrierServiceCode=eleOrderLine.getAttribute(ATTR_CARRIER_SERVICE_CODE);
					if(MAIL_INNOVATIONS_CSC.equals(strCarrierServiceCode)) {
						bMailInovsnsPrsnt=true;
					}
					//OMS-3821 Changes -- End
					setFulfillmentType(eleOrderLine, strDeliveryMethod, strLineType);	
					//OMS-1082 : Start: adding RESHIP_LINE_HOLD //
					Element eleOrderHoldTypes = inXML.createElement("OrderHoldTypes");
					eleOrderLine.appendChild(eleOrderHoldTypes);
					Element eleOrderHoldType = inXML.createElement("OrderHoldType");
				    eleOrderHoldType.setAttribute(VSIConstants.ATTR_HOLD_TYPE,"RESHIP_LINE_HOLD");
					eleOrderHoldType.setAttribute(VSIConstants.ATTR_REASON_TEXT,"OrderLine Hold for ReShipped Line");
					eleOrderHoldType.setAttribute(VSIConstants.ATTR_STATUS, "1100");
					eleOrderHoldTypes.appendChild(eleOrderHoldType);
					//OMS-1082 : End : adding RESHIP_LINE_HOLD//
					//OMS-1693 : Start
					//creating the INPUT document for getCommonCodeList API
					try {
					processLines(env, eleOrder, eleOrderLine, strDeliveryMethod, strLineType, strReshipParentLineKey);
					}
					catch (Exception e) {
						//OMS-3459 Changes -- Start
						printLogs("Exception in VSIReshipOrderLines Class and reshipOrderLines Method");
						printLogs("The exception is [ "+ e.getMessage() +" ]");
						//OMS-3459 Changes -- End
					}
					//OMS-1693 : End
				}//end of for
				//OMS-3821 Changes -- Start
				setSourcingClassification(eleOrder, bMailInovsnsPrsnt);
				//OMS-3821 Changes -- End
			}//end of checking OrderLines			
		}//end of checking Order
		
		printLogs("OUTPUT_FROM_CLASS: "+SCXmlUtil.getString(inXML));
		printLogs("================Exiting VSIReshipOrderLines class and reshipOrderLines method================================");
		
		return inXML;		
	}

	private void setSourcingClassification(Element eleOrder, boolean bMailInovsnsPrsnt) {
		
		printLogs("================Inside VSIReshipOrderLines class and setSourcingClassification method================================");
		
		if(bMailInovsnsPrsnt) {
			eleOrder.setAttribute(ATTR_SOURCING_CLASSIFICATION, NO_SFS_CLASSIFICATION);
		}
		
		printLogs("================Exiting VSIReshipOrderLines class and setSourcingClassification method================================");
	}

	private void processLines(YFSEnvironment env, Element eleOrder, Element eleOrderLine, String strDeliveryMethod,
			String strLineType, String strReshipParentLineKey)
			throws Exception {
		
		printLogs("================Inside VSIReshipOrderLines class and processLines method================================");
		
		if(!YFCObject.isVoid(strDeliveryMethod) && ATTR_DEL_METHOD_SHP.equals(strDeliveryMethod)
					&& !YFCObject.isVoid(strLineType) && LINETYPE_STH.equals(strLineType)){	
		Document docGetOrderListInput=	SCXmlUtil.createDocument(ELE_ORDER);
		docGetOrderListInput.getDocumentElement().setAttribute(ATTR_ORDER_HEADER_KEY, eleOrder.getAttribute(ATTR_ORDER_HEADER_KEY));
		Document docGetOrderListTemplate=SCXmlUtil.createDocument(ELE_ORDER_LIST);
		Element eleDocGetOrderListTemp = docGetOrderListTemplate.getDocumentElement();
		Element eleOrderGetOrderListTemp=SCXmlUtil.createChild(eleDocGetOrderListTemp, ELE_ORDER);
		eleOrderGetOrderListTemp.setAttribute(VSIConstants.ATTR_ORDER_NO, "");
		eleOrderGetOrderListTemp.setAttribute(ATTR_ENTERPRISE_CODE, "");
		eleOrderGetOrderListTemp.setAttribute(ATTR_ORDER_TYPE, "");
		//OMS-3459 Changes -- Start
		Element eleOrderLinesOut=SCXmlUtil.createChild(eleOrderGetOrderListTemp, ELE_ORDER_LINES);
		Element eleOrderLineOut=SCXmlUtil.createChild(eleOrderLinesOut, ELE_ORDER_LINE);
		eleOrderLineOut.setAttribute(ATTR_ORDER_LINE_KEY, "");
		eleOrderLineOut.setAttribute(ATTR_PRIME_LINE_NO, "");
		eleOrderLineOut.setAttribute(ATTR_DELIVERY_METHOD, "");
		eleOrderLineOut.setAttribute(ATTR_LINE_TYPE, "");
		Element eleOrdLnExtnOut=SCXmlUtil.createChild(eleOrderLineOut, ELE_EXTN); 
		eleOrdLnExtnOut.setAttribute(ATTR_EXTN_CHANNEL_ITEM_ID, "");
		printLogs("Template for getOrderList API: "+SCXmlUtil.getString(docGetOrderListTemplate));
		printLogs("Input to getOrderList API: "+SCXmlUtil.getString(docGetOrderListInput));
		//OMS-3459 Changes -- End
		Document docGetOrderListOp=VSIUtils.invokeAPI(env,docGetOrderListTemplate, VSIConstants.API_GET_ORDER_LIST,docGetOrderListInput);
		//OMS-3459 Changes -- Start
		printLogs("Output from getOrderList API: "+SCXmlUtil.getString(docGetOrderListOp));
		//OMS-3459 Changes -- End
		
		processAPIOutput(env, eleOrderLine, strReshipParentLineKey, docGetOrderListOp);
		
		}
		
		printLogs("================Exiting VSIReshipOrderLines class and processLines method================================");
	}

	private void processAPIOutput(YFSEnvironment env, Element eleOrderLine, String strReshipParentLineKey,
			Document docGetOrderListOp) throws Exception {
		
		printLogs("================Inside VSIReshipOrderLines class and processAPIOutput method================================");
		
		if(!YFCCommon.isVoid(docGetOrderListOp))
		{
			Element eleOrder1=(Element)docGetOrderListOp.getDocumentElement().getElementsByTagName(VSIConstants.ELE_ORDER).item(0);						
			//Call getCommonCode to get the list of FTC values for marketplace.
			String strFDD = null;
			String strCD = null;
			String sEnterpriseCode=eleOrder1.getAttribute(ATTR_ENTERPRISE_CODE);
			String strOrderType=eleOrder1.getAttribute(ATTR_ORDER_TYPE);
			//OMS-3459 Changes -- Start
			setChannelItemId(eleOrderLine, strReshipParentLineKey, docGetOrderListOp, strOrderType);
			//OMS-3459 Changes -- End
			ArrayList<Element> arrFTCRules;
			arrFTCRules = VSIUtils.getCommonCodeList(env, VSI_FTC_RULES, sEnterpriseCode, ATTR_DEFAULT);
			if(!arrFTCRules.isEmpty()){
				Element eleFTCRule = arrFTCRules.get(0);
				strFDD = eleFTCRule.getAttribute(ATTR_CODE_SHORT_DESCRIPTION);
				strCD = eleFTCRule.getAttribute(ATTR_CODE_LONG_DESCRIPTION);

				Element eleOrderDates=SCXmlUtil.createChild(eleOrderLine, ELE_ORDER_DATES);
				DateFormat dateFormat = new SimpleDateFormat(YYYY_MM_DD);
				Date date = new Date();
				String strCurrentdate = dateFormat.format(date);
				if(WEB.equals(strOrderType)){
					
					Element eleFirstFTCPromiseDate = SCXmlUtil.createChild(eleOrderDates, ELE_ORDER_DATE);
					eleFirstFTCPromiseDate.setAttribute(ATTR_DATE_TYPE_ID,DATE_TYPE_FTC_FIRST_PROMISE_DATE);
					eleFirstFTCPromiseDate.setAttribute(ATTR_EXPECTED_DATE, strCurrentdate);
					Element eleFTCPromiseDate = SCXmlUtil.createChild(eleOrderDates, ELE_ORDER_DATE);
					eleFTCPromiseDate.setAttribute(ATTR_DATE_TYPE_ID, DATE_TYPE_FTC_PROMISE_DATE);
					eleFTCPromiseDate.setAttribute(ATTR_EXPECTED_DATE, strCurrentdate);
				//Set ReqCancelDate
				String sReqCancelDate = VSIUtils.addDaysToPassedDateTime(strCurrentdate, strCD);
				SCXmlUtil.setAttribute(eleOrderLine, VSIConstants.ATTR_REQ_CANCEL_DATE, sReqCancelDate);
				}//end of if checking for enterprise code and orderType
				
				setFTCDates(eleOrderLine, strFDD, strCD, strOrderType, eleOrderDates, strCurrentdate);//end of checking marketplace enterprise code			
			}
			
		}
		
		printLogs("================Exiting VSIReshipOrderLines class and processAPIOutput method================================");
	}

	private void setFTCDates(Element eleOrderLine, String strFDD, String strCD, String strOrderType,
			Element eleOrderDates, String strCurrentdate) throws Exception {
		
		printLogs("================Inside VSIReshipOrderLines class and setFTCDates method================================");
		
		if(MARKETPLACE.equals(strOrderType)|| ATTR_ORDER_TYPE_POS.equals(strOrderType) ){								
			//set First FTC date
			Element eleFirstFTCPromiseDate = SCXmlUtil.createChild(eleOrderDates, ELE_ORDER_DATE);
			eleFirstFTCPromiseDate.setAttribute(ATTR_DATE_TYPE_ID,DATE_TYPE_FTC_FIRST_PROMISE_DATE);
			eleFirstFTCPromiseDate.setAttribute(ATTR_EXPECTED_DATE, strCurrentdate);
			//set FTC Promise date only for POS. this is to make sure FTC Delay email is sent right then and there post schedule. 
			if(ATTR_ORDER_TYPE_POS.equals(strOrderType))
			{
				Element eleFTCPromiseDate = SCXmlUtil.createChild(eleOrderDates, ELE_ORDER_DATE);
				eleFTCPromiseDate.setAttribute(ATTR_DATE_TYPE_ID, DATE_TYPE_FTC_PROMISE_DATE);
				if(!YFCCommon.isVoid(strFDD)){
					eleFTCPromiseDate.setAttribute(ATTR_ACTUAL_DATE, VSIUtils.addDaysToPassedDateTime(strCurrentdate, strFDD)); 
				}	
			}	
			
			//set FTC cancel date
			Element eleFTCCancelDate = SCXmlUtil.createChild(eleOrderDates, ELE_ORDER_DATE);
			eleFTCCancelDate.setAttribute(ATTR_DATE_TYPE_ID, DATE_TYPE_FTC_CANCEL_DATE);
			if(!YFCCommon.isVoid(strCD)){
				eleFTCCancelDate.setAttribute(ATTR_ACTUAL_DATE, VSIUtils.addDaysToPassedDateTime(strCurrentdate, strCD)); 
			}	
			
			SCXmlUtil.setAttribute(eleOrderLine, VSIConstants.ATTR_REQ_CANCEL_DATE, VSIUtils.addDaysToPassedDateTime(strCurrentdate, strCD));
		
		}
		
		printLogs("================Exiting VSIReshipOrderLines class and setFTCDates method================================");
	}

	private void setChannelItemId(Element eleOrderLine, String strReshipParentLineKey, Document docGetOrderListOp,
			String strOrderType) throws TransformerException {
		
		printLogs("================Inside VSIReshipOrderLines class and setChannelItemId method================================");
		
		if(MARKETPLACE.equals(strOrderType) && !YFCCommon.isVoid(strReshipParentLineKey)) {
			Element eleOrderLineAPI=XMLUtil.getElementByXPath(docGetOrderListOp, "/OrderList/Order/OrderLines/OrderLine[@OrderLineKey='"+strReshipParentLineKey+"']");
			if(!YFCCommon.isVoid(eleOrderLineAPI)) {
				Element eleOrdLnExtnAPI=SCXmlUtil.getChildElement(eleOrderLineAPI, ELE_EXTN);
				String strParentChnlItemId=eleOrdLnExtnAPI.getAttribute(ATTR_EXTN_CHANNEL_ITEM_ID);
				if(!YFCCommon.isVoid(strParentChnlItemId)) {
					printLogs("ExtnChannelItemID of parent line is "+strParentChnlItemId);
					Element eleOrdLnExtnAPIOut=SCXmlUtil.createChild(eleOrderLine, ELE_EXTN);
					eleOrdLnExtnAPIOut.setAttribute(ATTR_EXTN_CHANNEL_ITEM_ID, strParentChnlItemId);
					printLogs("ExtnChannelItemID is added for the Reship Line");
					printLogs("Reship OrderLine after adding ExtnChannelItemID "+SCXmlUtil.getString(eleOrderLine));
				}
			}
		}
		
		printLogs("================Exiting VSIReshipOrderLines class and setChannelItemId method================================");
	}

	private void setFulfillmentType(Element eleOrderLine, String strDeliveryMethod, String strLineType) {
		
		printLogs("================Inside VSIReshipOrderLines class and setFulfillmentType method================================");
		
		if(!YFCObject.isVoid(strDeliveryMethod) && "SHP".equals(strDeliveryMethod)
				&& !YFCObject.isVoid(strLineType) && "SHIP_TO_HOME".equals(strLineType)){
			
			eleOrderLine.setAttribute(VSIConstants.ATTR_FULFILLMENT_TYPE, "SHIP_TO_HOME");
			
		}else if(!YFCObject.isVoid(strDeliveryMethod) && "PICK".equals(strDeliveryMethod)
				&& !YFCObject.isVoid(strLineType) && "SHIP_TO_STORE".equals(strLineType)){
			
			eleOrderLine.setAttribute(VSIConstants.ATTR_FULFILLMENT_TYPE, "SHIP_TO_STORE");
		}
		
		printLogs("================Exiting VSIReshipOrderLines class and setFulfillmentType method================================");
	}
	
	public Document setEnvToCarrierServiceCode(YFSEnvironment env, Document inXML){
		
		//OMS-3459 Changes -- Start
		printLogs("================Inside VSIReshipOrderLines class and setEnvToCarrierServiceCode Method================================");
		printLogs("Printing Input XML: "+SCXmlUtil.getString(inXML));
		//OMS-3459 Changes -- End
		
		Element eleOrder = inXML.getDocumentElement();
		if(!YFCObject.isNull(eleOrder)){			
			Element eleOrderLines = SCXmlUtil.getChildElement(eleOrder, VSIConstants.ELE_ORDER_LINES);
			if(!YFCObject.isNull(eleOrderLines)){				
				ArrayList<Element> alOrderLines = SCXmlUtil.getChildren(eleOrderLines, VSIConstants.ELE_ORDER_LINE);
				for(Element eleOrderLine:alOrderLines){
					//OMS-3459 Changes -- Start
					String strOrdLnKey=eleOrderLine.getAttribute(ATTR_ORDER_LINE_KEY);
					printLogs("Processing orderline with key as "+strOrdLnKey);
					//OMS-3459 Changes -- End
					String strCarrierServiceCodeTmp = eleOrderLine.getAttribute(ATTR_RESHIP_CSC);
					env.setTxnObject(ATTR_RESHIP_CSC,strCarrierServiceCodeTmp);
					//OMS-3459 Changes -- Start
					printLogs("CarrierServiceCode is set up as "+strCarrierServiceCodeTmp+" in transaction object");
					//OMS-3459 Changes -- End
				}//end of for
			}//end of orderlines
		}//end of order
		
		//OMS-3459 Changes -- Start
		printLogs("================Exiting VSIReshipOrderLines class and setEnvToCarrierServiceCode Method================================");
		//OMS-3459 Changes -- End
		
		return inXML;
		
		
	}
	//OMS-3459 Changes -- Start
	private void printLogs(String mesg) {
		if(log.isDebugEnabled()){
			log.debug(TAG +" : "+mesg);
		}
	}
	//OMS-3459 Changes -- End
	
	
	public Document updateParentOrderLines(YFSEnvironment env, Document inXML) throws YFSException, RemoteException, YIFClientCreationException{
		
		printLogs("================Inside VSIReshipOrderLines class and updateParentOrderLines method================================");
		
		Element eleOrder = inXML.getDocumentElement();
		String ohKey = eleOrder.getAttribute(ATTR_ORDER_HEADER_KEY);
		Element nOrderLines = (Element)eleOrder.getElementsByTagName(ELE_ORDER_LINES).item(0);
		NodeList nOrderLine = nOrderLines.getElementsByTagName(ELE_ORDER_LINE);
		Document changeOrderDoc = SCXmlUtil.createDocument(ELE_ORDER);
		Element eleChangeOrder = changeOrderDoc.getDocumentElement();
		eleChangeOrder.setAttribute(ATTR_OVERRIDE, FLAG_Y);
		eleChangeOrder.setAttribute(ATTR_ORDER_HEADER_KEY, ohKey);
		eleChangeOrder.setAttribute(ATTR_SELECT_METHOD, SELECT_METHOD_WAIT);
		Element eleOrderLines = SCXmlUtil.createChild(eleChangeOrder, ELE_ORDER_LINES);
		for (int i = 0; i < nOrderLine.getLength(); i++) 
		{
			Element eleOrderLine = (Element) nOrderLine.item(i);
			Element elemOrderLine = SCXmlUtil.createChild(eleOrderLines, ELE_ORDER_LINE);
			elemOrderLine.setAttribute(ATTR_ACTION, ACTION_CAPS_MODIFY);
			elemOrderLine.setAttribute(ATTR_ORDER_LINE_KEY, eleOrderLine.getAttribute(ATTR_ORDER_LINE_KEY));
			Element eleExtn = SCXmlUtil.createChild(elemOrderLine, "Extn");
			eleExtn.setAttribute("ExtnReshippedLineFlag", "Y");
			eleExtn.setAttribute("ExtnReshippedQty",eleOrderLine.getAttribute("QuantityToReship"));
			eleExtn.setAttribute("ExtnReturnedLineFlag", "N");
		}
		 VSIUtils.invokeAPI(env, API_CHANGE_ORDER, changeOrderDoc);
	    
		printLogs("================Exiting VSIReshipOrderLines class and updateParentOrderLines method================================");
		 
		return inXML;
   }
}
