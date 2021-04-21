package com.vsi.som.shipment;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.vsi.oms.utils.VSIConstants;
import com.vsi.oms.utils.VSIUtils;
import com.vsi.oms.utils.XMLUtil;
import com.yantra.interop.japi.YIFApi;
import com.yantra.interop.japi.YIFClientCreationException;
import com.yantra.interop.japi.YIFClientFactory;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.core.YFSSystem;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;

public class VSISOMProcessShortage {
	
	private static YFCLogCategory log = YFCLogCategory.instance(VSISOMProcessShortage.class);
	private static final String TAG = VSISOMProcessShortage.class.getSimpleName();
	YIFApi api;
	
	public Document processShortage(YFSEnvironment env, Document inXML){
		
		printLogs("================Inside VSISOMProcessShortage Class and processShortage Method================");
		printLogs("Printing Input XML :"+SCXmlUtil.getString(inXML));
		
		try{
			
			Element eleInXML=inXML.getDocumentElement();
			String strDeliveryMethod=eleInXML.getAttribute(VSIConstants.ATTR_DELIVERY_METHOD);
			String strStatus=eleInXML.getAttribute(VSIConstants.ATTR_STATUS);
			String strShipNode=eleInXML.getAttribute(VSIConstants.ATTR_SHIP_NODE);
			
			//OMS-2809 Changes -- Start
			Element eleShpmentLines=SCXmlUtil.getChildElement(eleInXML, VSIConstants.ELE_SHIPMENT_LINES);
			Element eleShpmntLn=(Element)eleShpmentLines.getElementsByTagName(VSIConstants.ELE_SHIPMENT_LINE).item(0);
			Element eleOrder=SCXmlUtil.getChildElement(eleShpmntLn, VSIConstants.ELE_ORDER);
			String strModifyProgId=eleOrder.getAttribute("Modifyprogid");
			//OMS-3002 Changes -- Start			
			String strCustFstName=eleOrder.getAttribute(VSIConstants.ATTR_CUSTOMER_FIRST_NAME);
			String strCustLstName=eleOrder.getAttribute(VSIConstants.ATTR_CUSTOMER_LAST_NAME);
			String strCustName=strCustFstName+" "+strCustLstName;
			//OMS-3002 Changes -- End
			if(VSIConstants.PROG_ID_CALL_CENTER.equals(strModifyProgId)){
				printLogs("Call Center Invocation, hence exiting the class");
				return inXML;
			}
			//OMS-2809 Changes -- End
			
			String strOrderHeaderKey=null;
			
			if("1100.70.06.20".equals(strStatus) || "1100.70.06.70".equals(strStatus)){
				
				if("1100.70.06.20".equals(strStatus)){
					printLogs("Shortage Recorded during Backroom Pick Process");
				}else if("1100.70.06.70".equals(strStatus)){
					printLogs("Shortage Recorded during Pack Shipment Process");
				}
				
				Document docAdjInvIn=XMLUtil.createDocument(VSIConstants.ELE_ITEMS);
				Element eleAdjInvIn=docAdjInvIn.getDocumentElement();
				
				Document docChngOrdIn=XMLUtil.createDocument(VSIConstants.ELE_ORDER);
				Element eleChngOrdIn=docChngOrdIn.getDocumentElement();
				Element eleOrderLines=SCXmlUtil.createChild(eleChngOrdIn, VSIConstants.ELE_ORDER_LINES);
				
				Element eleShipmentLines=SCXmlUtil.getChildElement(eleInXML, VSIConstants.ELE_SHIPMENT_LINES);
				NodeList nlShipmentLine=eleShipmentLines.getElementsByTagName(VSIConstants.ELE_SHIPMENT_LINE);
				for(int i=0; i<nlShipmentLine.getLength(); i++){
					Element eleShipmentLine=(Element) nlShipmentLine.item(i);
					String strOHK=eleShipmentLine.getAttribute(VSIConstants.ATTR_ORDER_HEADER_KEY);
					if(YFCCommon.isVoid(strOrderHeaderKey)){
						strOrderHeaderKey=strOHK;
					}
					String strQtyReduced=eleShipmentLine.getAttribute("QuantityReduced");
					double dQtyReduced=Double.parseDouble(strQtyReduced);					
					if(dQtyReduced>0){				
						String strAdjType=null;
						String strQtyUpdt=null;
						String strItemID=eleShipmentLine.getAttribute(VSIConstants.ATTR_ITEM_ID);
						String strQuantity=eleShipmentLine.getAttribute(VSIConstants.ATTR_QUANTITY);
						int iQty=Integer.parseInt(strQuantity);
						if(iQty==0){
							strAdjType="ABSOLUTE";
							strQtyUpdt=strQuantity;
						}else if(iQty>0){
							strAdjType="ADJUSTMENT";
							double dQtyUpdt=dQtyReduced * (-1);
							strQtyUpdt=Double.toString(dQtyUpdt);
						}						
						String strProductClass=eleShipmentLine.getAttribute(VSIConstants.ATTR_PRODUCT_CLASS);
						String strUOM=eleShipmentLine.getAttribute(VSIConstants.ATTR_UOM);						
						
						Element eleItem=SCXmlUtil.createChild(eleAdjInvIn, VSIConstants.ELE_ITEM);
						eleItem.setAttribute("AdjustmentType", strAdjType);
						eleItem.setAttribute(VSIConstants.ATTR_ITEM_ID, strItemID);
						eleItem.setAttribute(VSIConstants.ATTR_ORG_CODE, "VSIINV");
						//OMS-2746-start
						if(strProductClass.isEmpty()) {
							strProductClass="GOOD";
						}
						//OMS-2746-end
						eleItem.setAttribute(VSIConstants.ATTR_PRODUCT_CLASS, strProductClass);
						eleItem.setAttribute(VSIConstants.ATTR_QUANTITY, strQtyUpdt);
						eleItem.setAttribute(VSIConstants.ATTR_SHIP_NODE, strShipNode);
						eleItem.setAttribute("SupplyType", "ONHAND");
						eleItem.setAttribute(VSIConstants.ATTR_UOM, strUOM);
						
						Element eleOrderLine=SCXmlUtil.createChild(eleOrderLines, VSIConstants.ELE_ORDER_LINE);
						String strOrdLnKey=eleShipmentLine.getAttribute(VSIConstants.ATTR_ORDER_LINE_KEY);
						eleOrderLine.setAttribute(VSIConstants.ATTR_ACTION, VSIConstants.ACTION_CAPS_CANCEL);
						eleOrderLine.setAttribute(VSIConstants.ATTR_FROM_STATUS, VSIConstants.STATUS_BACKORDERED);
						eleOrderLine.setAttribute(VSIConstants.ATTR_ORDER_LINE_KEY, strOrdLnKey);
						eleOrderLine.setAttribute(VSIConstants.ATTR_ORD_QTY, strQuantity);
					}
				}
				
				NodeList nlItems=eleAdjInvIn.getElementsByTagName(VSIConstants.ELE_ITEM);
				if(nlItems.getLength()>0){
					printLogs("Input to adjustInventory API for Backroom Pick scenario: "+SCXmlUtil.getString(docAdjInvIn));
					api = YIFClientFactory.getInstance().getLocalApi();
					api.invoke(env, "adjustInventory", docAdjInvIn);
					printLogs("adjustInventory API was invoked successfully for Backroom Pick scenario");
				}
				
				if(VSIConstants.ATTR_DEL_METHOD_PICK.equalsIgnoreCase(strDeliveryMethod)){
					printLogs("Status is "+strStatus+" and Delivery Method is PICK");
					NodeList nlOrderLine=eleOrderLines.getElementsByTagName(VSIConstants.ELE_ORDER_LINE);
					if(nlOrderLine.getLength()>0){
						printLogs("changeOrder API will be invoked");
						eleChngOrdIn.setAttribute(VSIConstants.ATTR_ORDER_HEADER_KEY, strOrderHeaderKey);
						eleChngOrdIn.setAttribute(VSIConstants.ATTR_MODIFICATION_REASON_CODE, "VSI_CANCEL_NO_INV");
						eleChngOrdIn.setAttribute(VSIConstants.ATTR_MODIFICATION_REASON_TEXT, "VSI_CANCEL_NO_INV");
						printLogs("Input to changeOrder API for Backroom Pick scenario: "+SCXmlUtil.getString(docChngOrdIn));
						api = YIFClientFactory.getInstance().getLocalApi();
						api.invoke(env, VSIConstants.API_CHANGE_ORDER, docChngOrdIn);
						printLogs("changeOrder API was invoked successfully for Backroom Pick scenario");
					}
				}else if(VSIConstants.ATTR_DEL_METHOD_SHP.equalsIgnoreCase(strDeliveryMethod)){
					printLogs("Status is "+strStatus+" and Delivery Method is SHP");
					printLogs("JDA Reverse Allocation Service will be invoked");
					printLogs("Input to VSISOMJDAReverseAllocation_Sync Service: "+SCXmlUtil.getString(inXML));
					VSIUtils.invokeService(env, "VSISOMJDAReverseAllocation_Sync", inXML);
					printLogs("JDA Reverse Allocation Service was invoked successfully");
				}
			}else if("1100.70.06.30".equals(strStatus)){
				printLogs("Shortage Recorded during Customer Pick Process");
				
				Document docAdjInvIn=XMLUtil.createDocument(VSIConstants.ELE_ITEMS);
				Element eleAdjInvIn=docAdjInvIn.getDocumentElement();
				
				Document docChngOrdIn=XMLUtil.createDocument(VSIConstants.ELE_ORDER);
				Element eleChngOrdIn=docChngOrdIn.getDocumentElement();
				Element eleOrderLines=SCXmlUtil.createChild(eleChngOrdIn, VSIConstants.ELE_ORDER_LINES);
				
				boolean bIsCancellation=false;
				
				//OMS-3171 Changes -- Start
				Document docInboxIn=null;				
				//OMS-3171 Changes -- End
								
				Element eleShipmentLines=SCXmlUtil.getChildElement(eleInXML, VSIConstants.ELE_SHIPMENT_LINES);
				NodeList nlShipmentLine=eleShipmentLines.getElementsByTagName(VSIConstants.ELE_SHIPMENT_LINE);
				for(int i=0; i<nlShipmentLine.getLength(); i++){
					Element eleShipmentLine=(Element) nlShipmentLine.item(i);
					String strOHK=eleShipmentLine.getAttribute(VSIConstants.ATTR_ORDER_HEADER_KEY);
					if(YFCCommon.isVoid(strOrderHeaderKey)){
						strOrderHeaderKey=strOHK;
					}
					String strQtyReduced=eleShipmentLine.getAttribute("QuantityReduced");
					double dQtyReduced=Double.parseDouble(strQtyReduced);
					if(dQtyReduced>0){
						String strOrdLnKey=eleShipmentLine.getAttribute(VSIConstants.ATTR_ORDER_LINE_KEY);
						
						Document docGetOrdLnLstIn=XMLUtil.createDocument(VSIConstants.ELE_ORDER_LINE);
						Element eleGetOrdLnLstIn=docGetOrdLnLstIn.getDocumentElement();
						eleGetOrdLnLstIn.setAttribute(VSIConstants.ATTR_ORDER_LINE_KEY, strOrdLnKey);
						
						printLogs("Input to getOrderLineList API: "+SCXmlUtil.getString(docGetOrdLnLstIn));
						api = YIFClientFactory.getInstance().getLocalApi();
						Document docGetOrdLnLstOut = api.invoke(env, "getOrderLineList", docGetOrdLnLstIn);
						printLogs("Output from getOrderLineList API: "+SCXmlUtil.getString(docGetOrdLnLstOut));
						
						Element eleGetOrdLnLstOut=docGetOrdLnLstOut.getDocumentElement();
						Element eleOrdLn=SCXmlUtil.getChildElement(eleGetOrdLnLstOut, VSIConstants.ELE_ORDER_LINE);
						String strMinLineSts=eleOrdLn.getAttribute(VSIConstants.ATTR_MIN_LINE_STATUS);
						String strCustPONo=eleOrdLn.getAttribute(VSIConstants.ATTR_CUST_PO_NO);
						
						String strItemID=eleShipmentLine.getAttribute(VSIConstants.ATTR_ITEM_ID);
						
						if(VSIConstants.STATUS_BACKORDERED.equals(strMinLineSts) || VSIConstants.STATUS_BACKORDERED_FROM_NODE.equals(strMinLineSts)){
							
							printLogs("BackOrder Scenario");
							String strAdjType=null;
							String strQtyUpdt=null;
							
							String strQuantity=eleShipmentLine.getAttribute(VSIConstants.ATTR_QUANTITY);
							int iQty=Integer.parseInt(strQuantity);
							if(iQty==0){
								strAdjType="ABSOLUTE";
								strQtyUpdt=strQuantity;
							}else if(iQty>0){
								strAdjType="ADJUSTMENT";
								double dQtyUpdt=dQtyReduced * (-1);
								strQtyUpdt=Double.toString(dQtyUpdt);
							}							
							String strProductClass=eleShipmentLine.getAttribute(VSIConstants.ATTR_PRODUCT_CLASS);
							String strUOM=eleShipmentLine.getAttribute(VSIConstants.ATTR_UOM);
							
							Element eleItem=SCXmlUtil.createChild(eleAdjInvIn, VSIConstants.ELE_ITEM);
							eleItem.setAttribute("AdjustmentType", strAdjType);
							eleItem.setAttribute(VSIConstants.ATTR_ITEM_ID, strItemID);
							eleItem.setAttribute(VSIConstants.ATTR_ORG_CODE, "VSIINV");
							//OMS-2746-start
							if(strProductClass.isEmpty()) {
							strProductClass="GOOD";
							}
							//OMS-2746-end
							eleItem.setAttribute(VSIConstants.ATTR_PRODUCT_CLASS, strProductClass);
							eleItem.setAttribute(VSIConstants.ATTR_QUANTITY, strQtyUpdt);
							eleItem.setAttribute(VSIConstants.ATTR_SHIP_NODE, strShipNode);
							eleItem.setAttribute("SupplyType", "ONHAND");
							eleItem.setAttribute(VSIConstants.ATTR_UOM, strUOM);
							
							Element eleOrderLine=SCXmlUtil.createChild(eleOrderLines, VSIConstants.ELE_ORDER_LINE);							
							eleOrderLine.setAttribute(VSIConstants.ATTR_ACTION, VSIConstants.ACTION_CAPS_CANCEL);
							eleOrderLine.setAttribute(VSIConstants.ATTR_FROM_STATUS, VSIConstants.STATUS_BACKORDERED);
							eleOrderLine.setAttribute(VSIConstants.ATTR_ORDER_LINE_KEY, strOrdLnKey);
							eleOrderLine.setAttribute(VSIConstants.ATTR_ORD_QTY, strQuantity);
							
						}else{
							
							printLogs("Non-BackOrder Scenario");
							String strOrderedQty=eleOrdLn.getAttribute(VSIConstants.ATTR_ORD_QTY);
							String strOrigOrdQty=eleOrdLn.getAttribute(VSIConstants.ATTR_ORIGINAL_ORDERED_QTY);
							if(!strOrderedQty.equals(strOrigOrdQty)){
								bIsCancellation=true;
								
								//SOM Restock changes -- Start
								String strQueueId="VSI_RESTOCK_"+strShipNode;
								docInboxIn=XMLUtil.createDocument(VSIConstants.ELE_INBOX);		//OMS-3171 Change
								Element eleInboxIn=docInboxIn.getDocumentElement();
								eleInboxIn.setAttribute(VSIConstants.ATTR_ACTIVE_FLAG, VSIConstants.FLAG_Y);
								eleInboxIn.setAttribute(VSIConstants.ATTR_ORDER_HEADER_KEY, strOrderHeaderKey);
								eleInboxIn.setAttribute(VSIConstants.ATTR_ORDER_LINE_KEY, strOrdLnKey);
								eleInboxIn.setAttribute(VSIConstants.ATTR_ORDER_NO, strCustPONo);
								eleInboxIn.setAttribute(VSIConstants.ATTR_EXCEPTION_TYPE, "Restock");
								eleInboxIn.setAttribute(VSIConstants.ATTR_QUEUE_ID, strQueueId);
								eleInboxIn.setAttribute(VSIConstants.ATTR_SHIPNODE_KEY, strShipNode);
								Element eleInbxRfrncsLst=SCXmlUtil.createChild(eleInboxIn, VSIConstants.ELE_INBOX_REFERANCES_LIST);
								Element eleItemReference=SCXmlUtil.createChild(eleInbxRfrncsLst, VSIConstants.ELE_INBOX_REFERANCES);
								eleItemReference.setAttribute(VSIConstants.ATTR_NAME, VSIConstants.ATTR_ITEM_ID);
								eleItemReference.setAttribute(VSIConstants.ATTR_REFERENCE_TYPE, "TEXT");
								eleItemReference.setAttribute(VSIConstants.ATTR_VALUE, strItemID);
								Element eleQtyReference=SCXmlUtil.createChild(eleInbxRfrncsLst, VSIConstants.ELE_INBOX_REFERANCES);
								eleQtyReference.setAttribute(VSIConstants.ATTR_NAME, "Qty");
								eleQtyReference.setAttribute(VSIConstants.ATTR_REFERENCE_TYPE, "TEXT");
								eleQtyReference.setAttribute(VSIConstants.ATTR_VALUE, strQtyReduced);
								//OMS-3002 Changes -- Start
			    				Element eleCustNameRef=SCXmlUtil.createChild(eleInbxRfrncsLst, VSIConstants.ELE_INBOX_REFERANCES);
			    				eleCustNameRef.setAttribute(VSIConstants.ATTR_NAME, "Name");
			    				eleCustNameRef.setAttribute(VSIConstants.ATTR_REFERENCE_TYPE, "TEXT");
			    				eleCustNameRef.setAttribute(VSIConstants.ATTR_VALUE, strCustName);
			    				//OMS-3002 Changes -- End								
								
								printLogs("createException API Input: "+SCXmlUtil.getString(docInboxIn));								
								
								VSIUtils.invokeAPI(env, VSIConstants.API_CREATE_EXCEPTION, docInboxIn);
								
								printLogs("createException API was invoked successfully");								
								//SOM Restock changes -- End
							}
						}
					}
				}
				
				//OMS-3171 Changes -- Start
				if(!YFCCommon.isVoid(docInboxIn)) {
					printLogs("Preparing the document to be sent to VBook Push Notification Q for Restock Alerts");
					Element eleInboxIn=docInboxIn.getDocumentElement();					
					Element eleInbxRfrncsLst=SCXmlUtil.getChildElement(eleInboxIn, VSIConstants.ELE_INBOX_REFERANCES_LIST);
					NodeList nlInbxRfrncs=eleInbxRfrncsLst.getElementsByTagName(VSIConstants.ELE_INBOX_REFERANCES);
					for(int i=0; i<nlInbxRfrncs.getLength(); i++) {
						Element eleInbxRfrncs=(Element)nlInbxRfrncs.item(i);
						String strName=eleInbxRfrncs.getAttribute(VSIConstants.ATTR_NAME);
						if(VSIConstants.ATTR_ITEM_ID.equals(strName) || "Qty".equals(strName)){
							eleInbxRfrncs.getParentNode().removeChild(eleInbxRfrncs);
							i--;
						}
					}
					printLogs("Document prepared for sending to VBook Push Notification Q for Restock Alerts is: "+SCXmlUtil.getString(docInboxIn));
					printLogs("Sending the Restock Alert details to Push Notification Queue");
					VSIUtils.invokeService(env, "VSISOMRestockAlertPush", docInboxIn);
					printLogs("Restock Alert details were sent to Push Notofication Queue");
				}
				//OMS-3171 Changes -- End
				
				NodeList nlItems=eleAdjInvIn.getElementsByTagName(VSIConstants.ELE_ITEM);
				if(nlItems.getLength()>0){
					printLogs("Input XML to adjustInventory API for Customer Pick scenario: "+SCXmlUtil.getString(docAdjInvIn));
					api = YIFClientFactory.getInstance().getLocalApi();
					api.invoke(env, "adjustInventory", docAdjInvIn);
					printLogs("adjustInventory API was invoked successfully for Customer Pick scenario");
				}
				NodeList nlOrderLine=eleOrderLines.getElementsByTagName(VSIConstants.ELE_ORDER_LINE);
				if(nlOrderLine.getLength()>0){
					eleChngOrdIn.setAttribute(VSIConstants.ATTR_ORDER_HEADER_KEY, strOrderHeaderKey);
					eleChngOrdIn.setAttribute(VSIConstants.ATTR_MODIFICATION_REASON_CODE, "VSI_CANCEL_NO_INV");
					eleChngOrdIn.setAttribute(VSIConstants.ATTR_MODIFICATION_REASON_TEXT, "VSI_CANCEL_NO_INV");
					printLogs("Input to changeOrder API for Customer Pick scenario: "+SCXmlUtil.getString(docChngOrdIn));
					api = YIFClientFactory.getInstance().getLocalApi();
					api.invoke(env, VSIConstants.API_CHANGE_ORDER, docChngOrdIn);
					printLogs("changeOrder API was invoked successfully for Customer Pick scenario");
				}
				if(bIsCancellation){
					processShortageCancellation(env, inXML, strOrderHeaderKey);
				}
			}			
		}catch (YFSException e) {
			e.printStackTrace();
			throw new YFSException();
		} catch (Exception e){
			e.printStackTrace();
			throw new YFSException();
		}		
		
		printLogs("================Exiting VSISOMProcessShortage Class and processShortage Method================");
		return inXML;
		
	}

	private void processShortageCancellation(YFSEnvironment env,
			Document inXML, String strOHK) throws ParserConfigurationException,
			YIFClientCreationException, RemoteException, TransformerException,
			Exception {
		
		printLogs("================Inside processShortageCancellation Method================");
		
		Document docGetOrdLstIn=XMLUtil.createDocument(VSIConstants.ELE_ORDER);
		Element eleGetOrdLstIn=docGetOrdLstIn.getDocumentElement();
		eleGetOrdLstIn.setAttribute(VSIConstants.ATTR_ORDER_HEADER_KEY, strOHK);
		
		printLogs("Input to getOrderList API: "+SCXmlUtil.getString(docGetOrdLstIn));
		Document docGetOrdLstOut = VSIUtils.invokeAPI(env,"global/template/api/getOrderList_SOMProcessShortage.xml","getOrderList", docGetOrdLstIn);
		printLogs("Output from getOrderList API: "+SCXmlUtil.getString(docGetOrdLstOut));
		
		Element eleGetOrdLstOut=docGetOrdLstOut.getDocumentElement();
		//OMS-3020 Changes -- Start
		eleGetOrdLstOut.setAttribute(VSIConstants.ATTR_EMAIL_TYPE, "CCM");
		eleGetOrdLstOut.setAttribute(VSIConstants.MESSAGE_TYPE, VSIConstants.ACTION_CAPS_CANCEL);
		//OMS-3020 Changes -- End
		Element eleOrder=(Element)eleGetOrdLstOut.getElementsByTagName(VSIConstants.ELE_ORDER).item(0);
		//OMS-3020 Changes -- Start
		eleOrder.setAttribute(VSIConstants.MESSAGE_TYPE, VSIConstants.ACTION_CAPS_CANCEL);
		//OMS-3020 Changes -- End
		String strMinOrdSts=eleOrder.getAttribute(VSIConstants.ATTR_MIN_ORDER_STATUS);
		String strMaxOrdSts=eleOrder.getAttribute(VSIConstants.ATTR_MAX_ORDER_STATUS);
		//OMS-2860 Changes -- Start
		boolean bATGCallRqrd=false;
		String strOrderNo=eleOrder.getAttribute(VSIConstants.ATTR_ORDER_NO);
		String strStatus=eleOrder.getAttribute(VSIConstants.ATTR_STATUS);
		Calendar calendar = Calendar.getInstance();
		String statusDate = new SimpleDateFormat("yyyyMMddHHmmss").format(calendar.getTime());
		if(VSIConstants.STATUS_CANCELLED.equals(strStatus)){
			bATGCallRqrd=true;
			printLogs("Order Status is Cancelled, ATG call will be invoked");
		}
		//OMS-2860 Changes -- End
		if(VSIConstants.STATUS_CANCEL.equals(strMinOrdSts) && VSIConstants.STATUS_CANCEL.equals(strMaxOrdSts)){
			printLogs("Order is fully Cancelled");
			//OMS-3020 Changes -- Start
			eleOrder.setAttribute("Uncommit", VSIConstants.FLAG_Y);
			//OMS-3020 Changes -- End
			//OMS-2860 Changes -- Start
			bATGCallRqrd=true;
			//OMS-2860 Changes -- End
		}
		
		//OMS-2860 Changes -- Start
		if(bATGCallRqrd){
			printLogs("Preparing ATG Call Input");
			Document docOrderStatusToWEB = XMLUtil.createDocument(VSIConstants.ELE_ORDER);
			Element eleOrderStatusToWEB = docOrderStatusToWEB.getDocumentElement();
			eleOrderStatusToWEB.setAttribute(VSIConstants.ATTR_ORDER_NO,strOrderNo);
			eleOrderStatusToWEB.setAttribute("TimeStamp",statusDate);
			eleOrderStatusToWEB.setAttribute(VSIConstants.ATTR_STATUS,VSIConstants.STATUS_CANCELLED);
			
			printLogs("Input to VSIWEBStatusUpdate service: "+SCXmlUtil.getString(docOrderStatusToWEB));
			VSIUtils.invokeService(env,"VSIWEBStatusUpdate",docOrderStatusToWEB);
			printLogs("Cancellation XML was sent to VSIWEBStatusUpdate service successfully");
		}
		//OMS-2860 Changes -- End
		
		//Mixed Cart Changes -- Start		
		printLogs("JDA BOPUS Cancel Allocation Webservice Call Input will be prepared");
		HashSet<String> setShipNode = new HashSet<String>();
		
		String strOrderHeaderKey=eleOrder.getAttribute(VSIConstants.ATTR_ORDER_HEADER_KEY);		
		String strOrderDate=eleOrder.getAttribute(VSIConstants.ATTR_ORDER_DATE);
		Element eleOrderLines=SCXmlUtil.getChildElement(eleOrder, VSIConstants.ELE_ORDER_LINES);
		NodeList nlOrderLine=eleOrderLines.getElementsByTagName(VSIConstants.ELE_ORDER_LINE);								
		for(int j=0; j<nlOrderLine.getLength(); j++){
			boolean bLnToBeRemoved=true;			
			Element eleOrderLine=(Element) nlOrderLine.item(j);
			String strOrdLneKey=eleOrderLine.getAttribute(VSIConstants.ATTR_ORDER_LINE_KEY);
			String strLineType=eleOrderLine.getAttribute(VSIConstants.ATTR_LINE_TYPE);
			printLogs("Line Type is: "+strLineType);
			if(VSIConstants.LINETYPE_PUS.equals(strLineType) || VSIConstants.LINETYPE_STS.equals(strLineType)){				
				Element eleShpmntLnFrmIn=XMLUtil.getElementByXPath(inXML, "/Shipment/ShipmentLines/ShipmentLine[@OrderLineKey='"+strOrdLneKey+"']");
				if(!YFCCommon.isVoid(eleShpmntLnFrmIn)){
					String strQtyRducd=eleShpmntLnFrmIn.getAttribute("QuantityReduced");
					double dQtyRducd=Double.parseDouble(strQtyRducd);				
					if(dQtyRducd>0){
						bLnToBeRemoved=false;
						eleOrderLine.setAttribute(VSIConstants.ATTR_ORD_QTY, strQtyRducd);
						String shipNode=eleOrderLine.getAttribute(VSIConstants.ATTR_SHIP_NODE);
						setShipNode.add(shipNode);						
					}
				}				
			}						
						
			if(bLnToBeRemoved){
				printLogs("OrderLine with OrderLineKey as "+strOrdLneKey+" will be removed from getOrderList Output");
				eleOrderLine.getParentNode().removeChild(eleOrderLine);
				j--;
			}			
		}
		printLogs("Ship Node HashSet Contents after processing all lines: "+Arrays.toString(setShipNode.toArray()));
		
		for(String shpNd:setShipNode){
			printLogs("Ship Node being processed: "+shpNd);
			
			String strBOPUSOrderNo=null;
			String strBOPUSStatusDate=null;
			String strSTSOrderNo=null;
			String strSTSStatusDate=null;
			
			Document docBOPUSJDARequest=XMLUtil.createDocument(VSIConstants.ELE_ORDER);
			Element eleBOPUSJDARequest=docBOPUSJDARequest.getDocumentElement();
			
			putElementValue(eleBOPUSJDARequest,"OrderDate", strOrderDate);
			Element eleBOPUSOrderLines=XMLUtil.createElement(docBOPUSJDARequest, VSIConstants.ELE_ORDER_LINES, "");
			
			Document docSTSJDARequest=XMLUtil.createDocument(VSIConstants.ELE_ORDER);
			Element eleSTSJDARequest=docSTSJDARequest.getDocumentElement();
			
			putElementValue(eleSTSJDARequest,"OrderDate", strOrderDate);
			Element eleSTSOrderLines=XMLUtil.createElement(docSTSJDARequest, VSIConstants.ELE_ORDER_LINES, "");
			
			for(int j=0; j<nlOrderLine.getLength(); j++){
				Element eleOrderLn=(Element)nlOrderLine.item(j);
				String strLineType=eleOrderLn.getAttribute(VSIConstants.ATTR_LINE_TYPE);
				String strPrimeLineNo=eleOrderLn.getAttribute(VSIConstants.ATTR_PRIME_LINE_NO);
				printLogs("Line Type is: "+strLineType);
				if(VSIConstants.LINETYPE_PUS.equals(strLineType) || VSIConstants.LINETYPE_STS.equals(strLineType)){
					String shipNode=eleOrderLn.getAttribute(VSIConstants.ATTR_SHIP_NODE);
					printLogs("Ship Node of current Line: "+shipNode);
					if(shipNode.equals(shpNd)){
						printLogs("Ship Node Matches");	
						String strOrdLneKey=eleOrderLn.getAttribute(VSIConstants.ATTR_ORDER_LINE_KEY);
						Element eleShpmntLnFrmIn=XMLUtil.getElementByXPath(inXML, "/Shipment/ShipmentLines/ShipmentLine[@OrderLineKey='"+strOrdLneKey+"']");
						if(!YFCCommon.isVoid(eleShpmntLnFrmIn)){
							String strQtyRducd=eleShpmntLnFrmIn.getAttribute("QuantityReduced");
							double dQtyRducd=Double.parseDouble(strQtyRducd);				
							if(dQtyRducd>0){
								if(VSIConstants.LINETYPE_PUS.equals(strLineType)){
									printLogs("OrderLine with PrimeLineNo "+strPrimeLineNo+" is a BOPUS line, hence will be added to BOPUS JDA Request");
									
									if(YFCCommon.isVoid(strBOPUSOrderNo)){
										printLogs("OrderNo is not set yet");
										String strCustPONo=eleOrderLn.getAttribute(VSIConstants.ATTR_CUST_PO_NO);
										strBOPUSOrderNo=strCustPONo;
										putElementValue(eleBOPUSJDARequest,"OrderNo", strBOPUSOrderNo);
										putElementValue(eleBOPUSJDARequest,"OrderType", "BOPUS");
										printLogs("OrderNo and OrderType is set");
									}
									if(YFCCommon.isVoid(strBOPUSStatusDate)){
										printLogs("StatusDate is not set yet");
										Element eleOrdStss=SCXmlUtil.getChildElement(eleOrderLn, VSIConstants.ELE_ORDER_STATUSES);
										Element eleOrdSts=(Element)eleOrdStss.getElementsByTagName(VSIConstants.ELE_ORDER_STATUS).item(0);
										String strStsDt=eleOrdSts.getAttribute("StatusDate");			
										strBOPUSStatusDate=strStsDt;
										putElementValue(eleBOPUSJDARequest,"StatusDate", strBOPUSStatusDate);
										putElementValue(eleBOPUSJDARequest,"Status", "CANCEL");
										putElementValue(eleBOPUSJDARequest,"FullfillingStore", shpNd);
										printLogs("StatusDate, Status and FullfillingStore are set");
									}
									Element eleBOPUSOrdLnOut=SCXmlUtil.createChild(eleBOPUSOrderLines, VSIConstants.ELE_ORDER_LINE);									
									putElementValue(eleBOPUSOrdLnOut,"OrderedQty", strQtyRducd);			//OMS-3020 Changes
									Element eleBOPUSItemOut=SCXmlUtil.createChild(eleBOPUSOrdLnOut, VSIConstants.ELE_ITEM);
									Element eleItem=SCXmlUtil.getChildElement(eleOrderLn, VSIConstants.ELE_ITEM);
									String strItemId=eleItem.getAttribute(VSIConstants.ATTR_ITEM_ID);
									putElementValue(eleBOPUSItemOut,"ItemID", strItemId);
									
								}else if(VSIConstants.LINETYPE_STS.equals(strLineType)){
									printLogs("OrderLine with PrimeLineNo "+strPrimeLineNo+" is a STS line, hence will be added to STS JDA Request");
									
									if(YFCCommon.isVoid(strSTSOrderNo)){
										printLogs("OrderNo is not set yet");
										String strCustPONo=eleOrderLn.getAttribute(VSIConstants.ATTR_CUST_PO_NO);
										strSTSOrderNo=strCustPONo;
										putElementValue(eleSTSJDARequest,"OrderNo", strSTSOrderNo);
										putElementValue(eleSTSJDARequest,"OrderType", "BOPUS");
										printLogs("OrderNo and OrderType is set");
									}
									if(YFCCommon.isVoid(strSTSStatusDate)){
										printLogs("StatusDate is not set yet");
										Element eleOrdStss=SCXmlUtil.getChildElement(eleOrderLn, VSIConstants.ELE_ORDER_STATUSES);
										Element eleOrdSts=(Element)eleOrdStss.getElementsByTagName(VSIConstants.ELE_ORDER_STATUS).item(0);
										String strStsDt=eleOrdSts.getAttribute("StatusDate");			
										strSTSStatusDate=strStsDt;
										putElementValue(eleSTSJDARequest,"StatusDate", strSTSStatusDate);
										putElementValue(eleSTSJDARequest,"Status", "CANCEL");
										putElementValue(eleSTSJDARequest,"FullfillingStore", shpNd);
										printLogs("StatusDate, Status and FullfillingStore are set");
									}
									Element eleSTSOrdLnOut=SCXmlUtil.createChild(eleSTSOrderLines, VSIConstants.ELE_ORDER_LINE);											
									putElementValue(eleSTSOrdLnOut,"OrderedQty", strQtyRducd);			//OMS-3020 Changes			
									Element eleSTSItemOut=SCXmlUtil.createChild(eleSTSOrdLnOut, VSIConstants.ELE_ITEM);
									Element eleItem=SCXmlUtil.getChildElement(eleOrderLn, VSIConstants.ELE_ITEM);
									String strItemId=eleItem.getAttribute(VSIConstants.ATTR_ITEM_ID);
									putElementValue(eleSTSItemOut,"ItemID", strItemId);
								}				
							}
						}
					}					
				}
			}
			
			eleBOPUSJDARequest.appendChild(eleBOPUSOrderLines);
			eleSTSJDARequest.appendChild(eleSTSOrderLines);
			
			printLogs("JDA Cancel Allocation Request Prepared for BOPUS lines is: "+SCXmlUtil.getString(docBOPUSJDARequest));
			
			printLogs("JDA Cancel Allocation Request Prepared for STS lines is: "+SCXmlUtil.getString(docSTSJDARequest));
			
			NodeList nlBOPUSOrdLn=eleBOPUSOrderLines.getElementsByTagName(VSIConstants.ELE_ORDER_LINE);
			if(nlBOPUSOrdLn.getLength()>0){
				
				printLogs("BOPUS Lines are available for shipnode "+shpNd+" hence JDA call will be invoked for BOPUS lines");
				invokeJDA(env, strOrderHeaderKey, strOrderNo,
						docBOPUSJDARequest);
			}
			
			NodeList nlSTSOrdLn=eleSTSOrderLines.getElementsByTagName(VSIConstants.ELE_ORDER_LINE);
			if(nlSTSOrdLn.getLength()>0){
				
				printLogs("STS Lines are available for shipnode "+shpNd+" hence JDA call will be invoked for STS lines");
				invokeJDA(env, strOrderHeaderKey, strOrderNo,
						docSTSJDARequest);
			}
		}
		
		printLogs("Input to VSISOMCancelPublishToJDA service: "+SCXmlUtil.getString(docGetOrdLstOut));
		VSIUtils.invokeService(env, "VSISOMCancelPublishToJDA", docGetOrdLstOut);
		printLogs("Cancellation XML was sent to service VSISOMCancelPublishToJDA service successfully");
		
		//Mixed Cart Changes -- End
		printLogs("================Exiting processShortageCancellation Method================");
	}
	
	//Mixed Cart Changes -- Start
/*	private void prepareJDARequest(String shpNd, String strBOPUSOrderNo,
			String strBOPUSStatusDate, Element eleBOPUSJDARequest,
			Element eleBOPUSOrderLines, Element eleOrderLn) {
		
		printLogs("Inside prepareJDARequest Method");
		
		if(YFCCommon.isVoid(strBOPUSOrderNo)){
			printLogs("OrderNo is not set yet");
			String strCustPONo=eleOrderLn.getAttribute(VSIConstants.ATTR_CUST_PO_NO);
			strBOPUSOrderNo=strCustPONo;
			putElementValue(eleBOPUSJDARequest,"OrderNo", strBOPUSOrderNo);
			putElementValue(eleBOPUSJDARequest,"OrderType", "BOPUS");
			printLogs("OrderNo and OrderType is set");
		}
		if(YFCCommon.isVoid(strBOPUSStatusDate)){
			printLogs("StatusDate is not set yet");
			Element eleOrdStss=SCXmlUtil.getChildElement(eleOrderLn, VSIConstants.ELE_ORDER_STATUSES);
			Element eleOrdSts=(Element)eleOrdStss.getElementsByTagName(VSIConstants.ELE_ORDER_STATUS).item(0);
			String strStsDt=eleOrdSts.getAttribute("StatusDate");			
			strBOPUSStatusDate=strStsDt;
			putElementValue(eleBOPUSJDARequest,"StatusDate", strBOPUSStatusDate);
			putElementValue(eleBOPUSJDARequest,"Status", "CANCEL");
			putElementValue(eleBOPUSJDARequest,"FullfillingStore", shpNd);
			printLogs("StatusDate, Status and FullfillingStore are set");
		}
		Element eleBOPUSOrdLnOut=SCXmlUtil.createChild(eleBOPUSOrderLines, VSIConstants.ELE_ORDER_LINE);
		String strOrigOrdQty=eleOrderLn.getAttribute(VSIConstants.ATTR_ORIGINAL_ORDERED_QTY);
		String strOrdQty=eleOrderLn.getAttribute(VSIConstants.ATTR_ORD_QTY);
		int iOrigOrdQty=Integer.parseInt(strOrigOrdQty);
		int iOrdQty=Integer.parseInt(strOrdQty);
		int iChngInOrdQty=iOrigOrdQty-iOrdQty;
		String strChngInOrdQty=Integer.toString(iChngInOrdQty);		
		putElementValue(eleBOPUSOrdLnOut,"OrderedQty", strChngInOrdQty);
		Element eleBOPUSItemOut=SCXmlUtil.createChild(eleBOPUSOrdLnOut, VSIConstants.ELE_ITEM);
		Element eleItem=SCXmlUtil.getChildElement(eleOrderLn, VSIConstants.ELE_ITEM);
		String strItemId=eleItem.getAttribute(VSIConstants.ATTR_ITEM_ID);
		putElementValue(eleBOPUSItemOut,"ItemID", strItemId);
		
		printLogs("Exiting prepareJDARequest Method");
	}*/

	private void invokeJDA(YFSEnvironment env, String strOrderHeaderKey,
			String strBOPUSOrderNo, Document docBOPUSJDARequest)
			throws Exception, UnsupportedEncodingException,
			MalformedURLException, IOException, ProtocolException,
			ParserConfigurationException, SAXException {
		
		printLogs("Inside invokeJDA Method");
		
		VSIUtils.invokeService(env, "VSIMixedCartJDACancelAllocation_DB", docBOPUSJDARequest);
		printLogs("JDA Cancel Allocation Request is posted to DB successfully");
		
		DataOutputStream wr=null;
		
		String strJDAURL= YFSSystem.getProperty("SFS_FORCED_ALLOCATION_URL");
		printLogs("JDA URL: "+strJDAURL);
		
		String reqString=XMLUtil.getXMLString(docBOPUSJDARequest);
		printLogs("JDA Request in String format: "+reqString);
		
		String strSplit[]=strJDAURL.split("\\?");
		String endPointURL=strSplit[0].concat("?");
		String strArg1=strSplit[1];
		printLogs("endPointURL is: "+endPointURL);
		printLogs("strArg1: "+strArg1);
		
		String xmlencoded = URLEncoder.encode(reqString,VSIConstants.UTF8_ENCODING);
		String strContent=strArg1+"&request="+xmlencoded;
		printLogs("xmlencoded => "+xmlencoded);
		printLogs("strContent => "+strContent);
		
		URL url = new URL(endPointURL);	
		 
		//open connection using url
		HttpURLConnection connection =  (HttpURLConnection) url.openConnection();
		
		//get web service response
		connection.setRequestMethod(VSIConstants.JDA_POST_METHOD);
		byte[] bContent = strContent.getBytes();	
		connection.setRequestProperty(VSIConstants.JDA_REQUEST_PROP, String.valueOf(bContent.length));
		connection.setDefaultUseCaches(false);
		connection.setDoOutput(true);

		Document responseDoc=null;
		printLogs("Invoking the JDA web service");
			 
		wr = new DataOutputStream (connection.getOutputStream());
		wr.write(bContent);
		printLogs("Invoked the JDA web service");
		
		if(connection.getResponseCode() == -1 || connection.getResponseMessage() == null){
			printLogs("Inside If condition when ResponseCode is -1 or ResponseMessage is null");
			alertForJDAFailure(env,strBOPUSOrderNo,strOrderHeaderKey,"VSI_MC_JDA_CANCEL_ALLOCATION_FAILURE","MixedCart JDA Cancel Allocation Failure Alert","VSI_MC_JDA_CANCEL_ALLOCATION_FAILURE");
		}
			 
		if(connection.getResponseCode() != 200)
		{
			printLogs("Inside If condition when ResponseCode not equals 200");
			printLogs(connection.getResponseCode()+" - "+connection.getResponseMessage());	
			alertForJDAFailure(env,strBOPUSOrderNo,strOrderHeaderKey,"VSI_MC_JDA_CANCEL_ALLOCATION_FAILURE",connection.getResponseCode()+" - "+connection.getResponseMessage(),"VSI_MC_JDA_CANCEL_ALLOCATION_FAILURE");
		}
		
		if(connection.getResponseCode() == 200)
		{
			printLogs("Inside If condition when ResponseCode equals 200");
			responseDoc=parseDoc(connection.getInputStream());
			printLogs("Response from JDA WebService:"+XMLUtil.getXMLString(responseDoc));
			
			VSIUtils.invokeService(env, "VSIMixedCartJDACancelAllocation_DB", responseDoc);
			printLogs("JDA BOPUS Cancel Allocation Response is posted to DB successfully");
		}
		
		printLogs("Exiting invokeJDA Method");
	}
	
	private Document parseDoc(InputStream instream) throws ParserConfigurationException, SAXException, IOException{
		
		printLogs("================Inside parseDoc Method================");
		DocumentBuilderFactory factory =DocumentBuilderFactory.newInstance();
		factory.isIgnoringElementContentWhitespace();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc=null;
	 	if(instream.markSupported()){
	 		instream.mark(Integer.MAX_VALUE);
	 		BufferedReader buff_read = new BufferedReader(new InputStreamReader(instream,"UTF-8"));
		    String  inputLine = null;
	
		 while((inputLine = buff_read.readLine())!= null){
			 printLogs(inputLine);			 
		    }  
		    instream.reset();
	 	}
	 	doc=builder.parse(instream);
	 	printLogs("JDA WebService Response being Parsed"+instream);
	 	
	 	printLogs("================Exiting parseDoc Method================");
	 	return doc;
  }
	
	public void alertForJDAFailure(YFSEnvironment env, String orderNo, String orderHeaderKey, String exceptionType, String exceptionDescription, String queueId)
	{
		printLogs("================Inside alertForJDAFailure Method================");
		
		try
		{
		//Creation of Alert
		Document createExceptionDoc = SCXmlUtil.createDocument(VSIConstants.ELE_INBOX);
		Element eleInbox = createExceptionDoc.getDocumentElement();
		eleInbox.setAttribute(VSIConstants.ATTR_ORDER_HEADER_KEY, orderHeaderKey);		
		eleInbox.setAttribute(VSIConstants.ATTR_ORDER_NO, orderNo);
		eleInbox.setAttribute(VSIConstants.ATTR_EXCEPTION_TYPE, exceptionType);
		eleInbox.setAttribute(VSIConstants.ATTR_DETAIL_DESCRIPTION, exceptionDescription);
		eleInbox.setAttribute(VSIConstants.ATTR_QUEUE_ID, queueId);
		eleInbox.setAttribute(VSIConstants.ATTR_CONSOLIDATE,VSIConstants.FLAG_Y);
		eleInbox.setAttribute(VSIConstants.ATTR_CONS_WINDOW,VSIConstants.VAL_FOREVER);
		Element eleConsolidationTemplate = createExceptionDoc.createElement(VSIConstants.ELE_CONSOLIDATE_TEMPLATE);
		eleInbox.appendChild(eleConsolidationTemplate);
		Element eleInboxCpy = (Element) eleInbox.cloneNode(true);
		eleConsolidationTemplate.appendChild(eleInboxCpy);
		
		printLogs("createException API Input XML :"+SCXmlUtil.getString(createExceptionDoc));
		VSIUtils.invokeAPI(env, VSIConstants.API_CREATE_EXCEPTION,createExceptionDoc);
		printLogs("createException API invoked successfully");
		
		}
		catch(Exception e)
		{
			printLogs("alertForJDAFailure - in catch block => ");
			e.printStackTrace();
		}
		
		printLogs("================Exiting alertForJDAFailure Method================");
	}
	
	private void putElementValue(Element childEle, String key, Object value) {
		Element ele = SCXmlUtil.createChild(childEle, key);
		if(value instanceof String ) {
			ele.setTextContent((String)value);
		}else if(value instanceof Element ) {
			ele.appendChild((Element)value);
		}
	}
	//Mixed Cart Changes -- End
	
	private void printLogs(String mesg) {
		if(log.isDebugEnabled()){
			log.debug(TAG +" : "+mesg);
		}
	}

}
