package com.vsi.oms.api;

import java.util.HashSet;

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

public class VSICancelOnNoAutoAckUpdated extends VSIBaseCustomAPI implements VSIConstants{
	
	private static YFCLogCategory log = YFCLogCategory.instance(VSICancelOnNoAutoAckUpdated.class);
	
	private static final String TAG = VSICancelOnNoAutoAckUpdated.class.getSimpleName();
	
	public void cancelNoAckOrders(YFSEnvironment env, Document inXml){
		
		printLogs("================Inside VSICancelOnNoAutoAckUpdated Class and cancelNoAckOrders Method================");
		printLogs("Printing Input XML: "+SCXmlUtil.getString(inXml));
		
		try{
			
			Element orderEle = (Element) inXml.getElementsByTagName(VSIConstants.ELE_ORDER).item(0);				
			String orderHeaderKey= orderEle.getAttribute(VSIConstants.ATTR_ORDER_HEADER_KEY);
			
			boolean bCnclLnsPrsnt=false;
			
			HashSet<String> setCnclOrdLnKey = new HashSet<String>();
			
			Element eleOrdLines=SCXmlUtil.getChildElement(orderEle, ELE_ORDER_LINES);
			NodeList nlOrdLine=eleOrdLines.getElementsByTagName(ELE_ORDER_LINE);
			
			for(int i=0; i<nlOrdLine.getLength(); i++){
				Element eleOrdLine=(Element)nlOrdLine.item(i);
				String strStatus=eleOrdLine.getAttribute(ATTR_STATUS);
				printLogs("OrderLine Status is: "+strStatus);
				if(!STATUS_BACKORDERED_FROM_NODE.equals(strStatus) && !STATUS_CANCEL.equals(strStatus)){
					bCnclLnsPrsnt=true;
					String strOrdLnKey=eleOrdLine.getAttribute(ATTR_ORDER_LINE_KEY);
					printLogs("OrderLine with OrderLineKey "+strOrdLnKey+" is a valid cancel line and will be added to Hashset");
					setCnclOrdLnKey.add(strOrdLnKey);
				}
			}
			
			if(bCnclLnsPrsnt){
				
				printLogs("OrderLines are available for cancellation");
		
				Document getOrderListInputDoc = SCXmlUtil.createDocument(VSIConstants.ELE_ORDER);
				Element eleOrder = getOrderListInputDoc.getDocumentElement();
				eleOrder.setAttribute(ATTR_ORDER_HEADER_KEY, orderHeaderKey);
				
				printLogs("getOrderList API input: "+SCXmlUtil.getString(getOrderListInputDoc));
				Document getOrderListOutputDoc = VSIUtils.invokeAPI(env, TEMPLATE_ORDER_LIST_ORDER_MONITOR,VSIConstants.API_GET_ORDER_LIST, getOrderListInputDoc);			
				printLogs("getOrderList API output: "+SCXmlUtil.getString(getOrderListOutputDoc));
				
				NodeList orderLineNode = getOrderListOutputDoc.getElementsByTagName(ELE_ORDER_LINE);
				int orderLineNodeLength = orderLineNode.getLength();
				
				printLogs("orderLineNodeLength => "+orderLineNodeLength);				
				
				for(int l=0; l< orderLineNodeLength; l++){
										
					Element orderLineEle = (Element) orderLineNode.item(l);
					String strOrdLneKey=orderLineEle.getAttribute(ATTR_ORDER_LINE_KEY);
					printLogs("OrderLineKey is: "+strOrdLneKey);
					if(setCnclOrdLnKey.contains(strOrdLneKey)){
						printLogs("OrderLine with OrderLineKey "+strOrdLneKey+" will be cancelled");
						String conditionVariable1 = orderLineEle.getAttribute(ATTR_CONDITION_VARIBALE1);			
						String strLineType=orderLineEle.getAttribute(VSIConstants.ATTR_LINE_TYPE);
						
						if(!YFCCommon.isVoid(strLineType) && VSIConstants.LINETYPE_PUS.equals(strLineType)){	
							printLogs("conditionVariable1 => "+conditionVariable1);					
			                if(conditionVariable1.equalsIgnoreCase(FLAG_Y)) {
								changeOrderLineCancellation(orderHeaderKey, FLAG_Y, strOrdLneKey, env);
			                }
			                else {
			                	changeOrderLineCancellation(orderHeaderKey, FLAG_N, strOrdLneKey, env);
			                }
						}
						
					}else{
						printLogs("OrderLine with OrderLineKey "+strOrdLneKey+" will not be cancelled");
					}
				}				
			}else{
				printLogs("OrderLines are not available for Cancellation, hence exiting the class");
			}
    	}
		catch(Exception e){
			printLogs("Exception in VSICancelOnNoAutoAckUpdated Class and cancelNoAckOrders Method");
			printLogs("The exception is [ "+ e.getMessage() +" ]");
			e.printStackTrace();
		}
		
		printLogs("================Exiting VSICancelOnNoAutoAckUpdated Class and cancelNoAckOrders Method================");
	}
	
	public void changeOrderLineCancellation(String orderHeaderKey, String ConditionVariable1, String orderLineKey, YFSEnvironment env){
		
		printLogs("================Inside changeOrderLineCancellation Method================");
		
		try{
			
			printLogs("Invoking changeOrderLineCancellation => OH Key "+orderHeaderKey+"Con Var1 "+ConditionVariable1+"orderLineKey "+orderLineKey);
			
			if(ConditionVariable1.equalsIgnoreCase(FLAG_Y)){
				Document getShipmentListInputDoc = SCXmlUtil.createDocument(ELE_SHIPMENT);
				Element eleShipment = getShipmentListInputDoc.getDocumentElement();
				eleShipment.setAttribute(ATTR_ORDER_HEADER_KEY, orderHeaderKey);
				
				printLogs("Input to getShipmentList API "+SCXmlUtil.getString(getShipmentListInputDoc));				
				Document getShipmentListOuputDoc = VSIUtils.invokeAPI(env, TEMPLATE_SHIPMENT_LIST, API_GET_SHIPMENT_LIST, getShipmentListInputDoc);
				printLogs("Output from getShipmentList API "+SCXmlUtil.getString(getShipmentListOuputDoc));
				
				Element eleShpmntFrmIn=XMLUtil.getElementByXPath(getShipmentListOuputDoc, "/Shipments/Shipment[ShipmentLines/ShipmentLine/OrderLine[@OrderLineKey='"+orderLineKey+"']]");
				String shipmentkey=eleShpmntFrmIn.getAttribute(ATTR_SHIPMENT_KEY);
				String strShpmtSts=eleShpmntFrmIn.getAttribute(ATTR_STATUS);
				if(!STATUS_CANCEL.equals(strShpmtSts)){
					Document getChangeShipmentInputDoc = SCXmlUtil.createDocument(ELE_SHIPMENT);
					Element eleChngShipment = getChangeShipmentInputDoc.getDocumentElement();
					eleChngShipment.setAttribute(ATTR_SHIPMENT_KEY, shipmentkey);
					eleChngShipment.setAttribute(ATTR_ACTION, "Cancel");
					
					printLogs("Input to changeShipment API "+SCXmlUtil.getString(getChangeShipmentInputDoc));
					Document docOutChangeShipment = VSIUtils.invokeAPI(env, VSIConstants.API_CHANGE_SHIPMENT,
								getChangeShipmentInputDoc);
					printLogs("Output from changeShipment API "+SCXmlUtil.getString(docOutChangeShipment));
				}
			}		
		
			Document changeOrderDoc = SCXmlUtil.createDocument(ELE_ORDER);
			Element eleOrder = changeOrderDoc.getDocumentElement();
			eleOrder.setAttribute(ATTR_OVERRIDE, VSIConstants.FLAG_Y);
			eleOrder.setAttribute(ATTR_ORDER_HEADER_KEY, orderHeaderKey);
			Element eleOrderLines = SCXmlUtil.createChild(eleOrder, ELE_ORDER_LINES);
			Element elemOrderLine = SCXmlUtil.createChild(eleOrderLines, ELE_ORDER_LINE);
			elemOrderLine.setAttribute(ATTR_ORDER_LINE_KEY, orderLineKey);
			elemOrderLine.setAttribute(ATTR_ACTION, ACTION_RELEASE_CANCEL);			
			eleOrder.setAttribute(ATTR_MODIFICATION_REASON_CODE, NO_STORE_PICK);			
			
			printLogs("Input to changeOrder API "+SCXmlUtil.getString(changeOrderDoc));
			VSIUtils.invokeAPI(env, API_CHANGE_ORDER, changeOrderDoc);
			printLogs("changeOrder API was invoked successfully");
		}
		catch(Exception e)
		{
			printLogs("Exception in VSICancelOnNoAutoAckUpdated Class and changeOrderLineCancellation Method");
			printLogs("The exception is [ "+ e.getMessage() +" ]");
			e.printStackTrace();
		}
		
		printLogs("================Exiting changeOrderLineCancellation Method================");
	}
	
	private void printLogs(String mesg) {
		if(log.isDebugEnabled()){
			log.debug(TAG +" : "+mesg);
		}
	}
}
