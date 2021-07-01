package com.vsi.oms.userexit;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.rmi.RemoteException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.vsi.oms.utils.VSIConstants;
import com.vsi.oms.utils.VSIUtils;
import com.vsi.oms.utils.XMLUtil;
import com.yantra.interop.japi.YIFClientCreationException;
import com.yantra.ydm.japi.ue.YDMBeforeChangeShipment;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.core.YFSSystem;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;
import com.yantra.yfs.japi.YFSUserExitException;

public class VSIBeforeChangeShipmentUE implements YDMBeforeChangeShipment,VSIConstants{
	
	private static YFCLogCategory log = YFCLogCategory.instance(VSIBeforeChangeShipmentUE.class);
	
	private static final String TAG = VSIBeforeChangeShipmentUE.class.getSimpleName();

	@Override
	public Document beforeChangeShipment(YFSEnvironment env, Document inXML)
			throws YFSUserExitException {
		
		try{
			
			printLogs("================Inside VSIBeforeChangeShipmentUE class and beforeChangeShipment Method================================");
			printLogs("Printing Input XML: "+SCXmlUtil.getString(inXML));
			
			Element eleShipment=inXML.getDocumentElement();			
			String strRestockRequired=eleShipment.getAttribute("RestockRequired");
			String strAction=eleShipment.getAttribute(ATTR_ACTION);
			
			//OMS-3178 Changes -- Start
			boolean bLnCancel=false;
			if(!YFCCommon.isVoid(strAction) && !ACTION_CANCEL.equals(strAction)) {
				Element eleShpmntLns=SCXmlUtil.getChildElement(eleShipment, ELE_SHIPMENT_LINES);
				if(!YFCCommon.isVoid(eleShpmntLns)) {
					Element eleShpmntLn=SCXmlUtil.getChildElement(eleShpmntLns, ELE_SHIPMENT_LINE);
					if(!YFCCommon.isVoid(eleShpmntLn)) {
						String strLnAction=eleShpmntLn.getAttribute(ATTR_ACTION);
						if(ACTION_CANCEL.equals(strLnAction)) {
							bLnCancel=true;
						}
					}
				}				
			}
			
			String strShpmntKey=eleShipment.getAttribute(ATTR_SHIPMENT_KEY);
			
			//OMS-3178 Changes -- End
			
			if(!YFCCommon.isVoid(strRestockRequired) && FLAG_N.equals(strRestockRequired)){
				
				printLogs("Restock Required Flag is N, hence exiting UE");
				
				eleShipment.removeAttribute("RestockRequired");
			}						
			else if(!YFCCommon.isVoid(strAction) && ACTION_CANCEL.equals(strAction)){
				printLogs("Shipment Action is Cancel, UE will be executed");
				
				//OMS-3178 Changes -- Start
				Document getShipmentListOuputDoc = callGetShipmentList(env, strShpmntKey);
				//OMS-3178 Changes -- End
				
				Element eleShpmntsOut=getShipmentListOuputDoc.getDocumentElement();
				Element eleShpmntOut=SCXmlUtil.getChildElement(eleShpmntsOut, ELE_SHIPMENT);
				
				Element eleShpmentLines=SCXmlUtil.getChildElement(eleShpmntOut, ELE_SHIPMENT_LINES);
				Element eleShpmntLn=(Element)eleShpmentLines.getElementsByTagName(ELE_SHIPMENT_LINE).item(0);
				Element eleOrder=SCXmlUtil.getChildElement(eleShpmntLn, ELE_ORDER);
				Element eleOrdLn=SCXmlUtil.getChildElement(eleShpmntLn, ELE_ORDER_LINE);
				String strMaxLnSts=eleOrdLn.getAttribute(ATTR_MAX_LINE_STATUS);
				String strMinLnSts=eleOrdLn.getAttribute(ATTR_MIN_LINE_STATUS);
				
				//OMS-2827 Changes -- Start
				String strDeliveryMethod=eleShpmntOut.getAttribute(ATTR_DELIVERY_METHOD);
				if(ATTR_DEL_METHOD_SHP.equals(strDeliveryMethod)) {
					printLogs("BOSS Shipment");
					
					if(STATUS_BACKORDERED.equals(strMaxLnSts) && STATUS_BACKORDERED.equals(strMinLnSts)) {
						printLogs("Record Shortage scenario for BOSS");
					}else {
						printLogs("COM cancellation scenario");
						
						Document docJDARequest = prepareJDARequest(env, eleShpmntOut, eleShpmentLines, eleShpmntLn,
								eleOrder);						
						
						invokeJDAWebservice(env, docJDARequest);
					}
					
				}else if(ATTR_DEL_METHOD_PICK.equals(strDeliveryMethod)) {
					printLogs("Non-BOSS Shipment");
				//OMS-2827 Changes -- End
					
					if(!YFCCommon.isVoid(strMaxLnSts)&&STATUS_CANCEL.equals(strMaxLnSts)&&!YFCCommon.isVoid(strMinLnSts)&&STATUS_CANCEL.equals(strMinLnSts)){
						printLogs("Record Shortage Scenario, Restock Alert not required");
					}else{
						String strOrderHeaderKey=eleShpmntLn.getAttribute(ATTR_ORDER_HEADER_KEY);			
									
						String strCustFstName=eleOrder.getAttribute(ATTR_CUSTOMER_FIRST_NAME);
						String strCustLstName=eleOrder.getAttribute(ATTR_CUSTOMER_LAST_NAME);
						String strCustName=strCustFstName+" "+strCustLstName;
						
						String strShpmntSts=eleShpmntOut.getAttribute(ATTR_STATUS);
						if(!YFCCommon.isVoid(strShpmntSts) && "1100.70.06.30".equals(strShpmntSts)){
							printLogs("Shipment in Ready For Customer status, Restock alert needs to be raised");
							
							String strShipNode=eleShpmntOut.getAttribute(ATTR_SHIP_NODE);
							String strQueueId="VSI_RESTOCK_"+strShipNode;
							NodeList nlShipmentLine=eleShpmentLines.getElementsByTagName(ELE_SHIPMENT_LINE);
							//OMS-3171 Changes -- Start
							Document docInboxIn=null;							
							//OMS-3171 Changes -- End
							for(int i=0; i<nlShipmentLine.getLength(); i++){
								Element eleShpmntLine=(Element)nlShipmentLine.item(i);
								String strItemId=eleShpmntLine.getAttribute(ATTR_ITEM_ID);
								String strQuantity=eleShpmntLine.getAttribute("BackroomPickedQuantity");
								Element eleOrderLine=SCXmlUtil.getChildElement(eleShpmntLine, ELE_ORDER_LINE);
								String strOrderLineKey=eleOrderLine.getAttribute(ATTR_ORDER_LINE_KEY);
								String strCustPONo=eleOrderLine.getAttribute(ATTR_CUST_PO_NO);
								
								printLogs("Invoking raiseRestockAlert method with strShipNode as "+strShipNode+" strOrderHeaderKey as "+strOrderHeaderKey+
			    						" orderLineKey as "+strOrderLineKey+" strCustPONo as "+strCustPONo+" strItemId as "+strItemId+" strQuantity as "+strQuantity+
			    						" and strCustName as "+strCustName);
								
								//OMS-3178 Changes -- Start
								docInboxIn=raiseRestockAlert(env, strOrderHeaderKey, strCustName, strShipNode, strQueueId, strItemId,			//OMS-3171 Change
										strQuantity, strOrderLineKey, strCustPONo);
								//OMS-3178 Changes -- End
							}
							//OMS-3171 Changes -- Start
							sendRestockPushNotification(env, docInboxIn);
							//OMS-3171 Changes -- End
						}else{
							printLogs("Shipment is not in Ready For Customer status, Restock alert not required");
						}
					}
				//OMS-2827 Changes -- Start
				}
				//OMS-2827 Changes -- End
			}
			//OMS-3178 Changes -- Start
			else if(bLnCancel) {
				printLogs("Shipment Line Level cancellation scenario");
				
				Document getShipmentListOuputDoc = callGetShipmentList(env, strShpmntKey);
				
				//OMS-2827 Changes -- Start
				Element eleShpmntsOut=getShipmentListOuputDoc.getDocumentElement();
				Element eleShpmntOut=SCXmlUtil.getChildElement(eleShpmntsOut, ELE_SHIPMENT);
				
				String strDeliveryMethod=eleShpmntOut.getAttribute(ATTR_DELIVERY_METHOD);				
				setPartialCancellationEmailMsg(env,inXML);				
				if(ATTR_DEL_METHOD_SHP.equals(strDeliveryMethod)) {
					printLogs("BOSS Shipment");
					
					Element eleShpmntLns=SCXmlUtil.getChildElement(eleShipment, ELE_SHIPMENT_LINES);
					NodeList nlShipmentLine=eleShpmntLns.getElementsByTagName(ELE_SHIPMENT_LINE);
					for(int i=0; i<nlShipmentLine.getLength(); i++){
						Element eleShipmentLine=(Element)nlShipmentLine.item(i);
						String strShpmntLnKey=eleShipmentLine.getAttribute(ATTR_SHIPMENT_LINE_KEY);
												
						Element eleAPIShpmntLn=XMLUtil.getElementByXPath(getShipmentListOuputDoc, "/Shipments/Shipment/ShipmentLines/ShipmentLine[@ShipmentLineKey='"+strShpmntLnKey+"']");
						if(!YFCCommon.isVoid(eleAPIShpmntLn)) {
							Element eleOrderLine=SCXmlUtil.getChildElement(eleAPIShpmntLn, ELE_ORDER_LINE);
							String strMaxLneSts=eleOrderLine.getAttribute(ATTR_MAX_LINE_STATUS);
							String strMinLneSts=eleOrderLine.getAttribute(ATTR_MIN_LINE_STATUS);
							
							if(STATUS_BACKORDERED.equals(strMaxLneSts) && STATUS_BACKORDERED.equals(strMinLneSts)) {
								printLogs("Record Shortage scenario for BOSS");
							}else {
								printLogs("COM cancellation scenario");
								
								String strShipNode=eleShpmntOut.getAttribute(ATTR_SHIP_NODE);
								Element eleShipmentLns=SCXmlUtil.getChildElement(eleShpmntOut, ELE_SHIPMENT_LINES);
								NodeList nlShpmntLn=eleShipmentLns.getElementsByTagName(ELE_SHIPMENT_LINE);
								int iNoofShpLns=nlShpmntLn.getLength();
								if(iNoofShpLns==1) {
									Element eleShpmntLne=SCXmlUtil.getChildElement(eleShipmentLns, ELE_SHIPMENT_LINE);
									String strShpLnKey=eleShpmntLne.getAttribute(ATTR_SHIPMENT_LINE_KEY);
									if(strShpLnKey.equals(strShpmntLnKey)) {
										eleShipment.setAttribute(ATTR_ACTION, ACTION_CANCEL);
										printLogs("Setting Shipment Action as Cancel");
									}
								}
								
								Document docJDARequest=XMLUtil.createDocument("VSIEnvelope");
								Element eleJDARequest=docJDARequest.getDocumentElement();
								
								putElementValue(eleJDARequest,"MessageType", "AllocationRequest");
								
								Element eleMessage = SCXmlUtil.createChild(eleJDARequest, "Message");
								
								String strReleaseNo=eleAPIShpmntLn.getAttribute(ATTR_RELEASE_NO);
								
								Element eleOrder=SCXmlUtil.getChildElement(eleAPIShpmntLn, ELE_ORDER);								
								String strCreatets=eleOrder.getAttribute(ATTR_CREATETS);
								String strOrdNo=eleOrder.getAttribute(ATTR_ORDER_NO);
								String strOrderDate=eleOrder.getAttribute(ATTR_ORDER_DATE);
								String strCustNo=eleOrder.getAttribute(ATTR_BILL_TO_ID);
								//OMS-3729 Changes -- Start
								String strOrderType=eleOrder.getAttribute(ATTR_ORDER_TYPE);
								String strEnteredBy=eleOrder.getAttribute(ATTR_ENTERED_BY);
								String strStore=null;
								//OMS-3729 Changes -- End
								String strOrderNo=strOrdNo+"*"+strReleaseNo;
								
								putElementValue(eleMessage,"DateTimeStamp", strCreatets);
								putElementValue(eleMessage,"OrderNo", strOrderNo);
								putElementValue(eleMessage,"OrderType", "Ship_to_Home");
								putElementValue(eleMessage,"IntOrderDate", strOrderDate);								
								//OMS-3729 Changes -- Start
								if(MARKETPLACE.equals(strOrderType)) {
									strStore=strEnteredBy;
								}else {
									strStore=SHIP_NODE_6102_VALUE;
								}
								putElementValue(eleMessage,"Store", strStore);			//OMS-3011 Change
								//OMS-3729 Changes -- End
								putElementValue(eleMessage,"WhseNo", strShipNode);
								putElementValue(eleMessage,"CustNo", strCustNo);
								
								Element eleItem=SCXmlUtil.createChild(eleMessage, ELE_ITEM);
								String strJdaSku=eleAPIShpmntLn.getAttribute(ATTR_ITEM_ID);								
								String strOrigQty=eleAPIShpmntLn.getAttribute(ATTR_QUANTITY);				
															
								putElementValue(eleItem,"JdaSku", strJdaSku);
								putElementValue(eleItem,"OrigQty", strOrigQty);
								putElementValue(eleItem,"QtyOrdered", "0");
								
								printLogs("JDA Reverse Allocation Request Prepared is: "+SCXmlUtil.getString(docJDARequest));
								
								VSIUtils.invokeService(env, "VSISOMJDAReverseAllocation_DB", docJDARequest);
								printLogs("JDA Reverse Allocation Request is posted to DB successfully");
								
								invokeJDAWebservice(env, docJDARequest);
																
							}
						}
						eleShipmentLine.setAttribute(ATTR_ACTION, DELETE);
						env.setTxnObject("IsCOMPartialCancellation", "Y"); 
					}
				}else if(ATTR_DEL_METHOD_PICK.equals(strDeliveryMethod)) {
				//OMS-2827 Changes -- End
				
					Element eleShpmntLns=SCXmlUtil.getChildElement(eleShipment, ELE_SHIPMENT_LINES);
					NodeList nlShipmentLine=eleShpmntLns.getElementsByTagName(ELE_SHIPMENT_LINE);
					//OMS-3171 Changes -- Start
					Document docInboxIn=null;					
					//OMS-3171 Changes -- End
					for(int i=0; i<nlShipmentLine.getLength(); i++){
						Element eleShipmentLine=(Element)nlShipmentLine.item(i);
						String strShpmntLnKey=eleShipmentLine.getAttribute(ATTR_SHIPMENT_LINE_KEY);
						String strOrderLineKey=eleShipmentLine.getAttribute(ATTR_ORDER_LINE_KEY);
						String strShipNode=null;
						Element eleAPIShpmnt=XMLUtil.getElementByXPath(getShipmentListOuputDoc, "/Shipments/Shipment[ShipmentLines/ShipmentLine[@ShipmentLineKey='"+strShpmntLnKey+"']]");
						if(!YFCCommon.isVoid(eleAPIShpmnt)) {
							strShipNode=eleAPIShpmnt.getAttribute(ATTR_SHIP_NODE);
							Element eleShipmentLns=SCXmlUtil.getChildElement(eleAPIShpmnt, ELE_SHIPMENT_LINES);
							NodeList nlShpmntLn=eleShipmentLns.getElementsByTagName(ELE_SHIPMENT_LINE);
							int iNoofShpLns=nlShpmntLn.getLength();
							if(iNoofShpLns==1) {
								Element eleShpmntLne=SCXmlUtil.getChildElement(eleShipmentLns, ELE_SHIPMENT_LINE);
								String strShpLnKey=eleShpmntLne.getAttribute(ATTR_SHIPMENT_LINE_KEY);
								if(strShpLnKey.equals(strShpmntLnKey)) {
									eleShipment.setAttribute(ATTR_ACTION, ACTION_CANCEL);
									printLogs("Setting Shipment Action as Cancel");
								}
							}else if(iNoofShpLns > 1) {
								int noOfOpenShipments = 0;
								for(int j=0; j<nlShpmntLn.getLength(); j++){
									Element eleShipmentLn=(Element)nlShpmntLn.item(j);
									String strShpmntLineKey=eleShipmentLn.getAttribute(ATTR_SHIPMENT_LINE_KEY);
									if(strShpmntLnKey != strShpmntLineKey) {
										int quantity = Integer.parseInt(eleShipmentLn.getAttribute(ATTR_QUANTITY));
										Element eleOrderLn=SCXmlUtil.getChildElement(eleShipmentLn, ELE_ORDER_LINE);
										String strMaxLineSts=eleOrderLn.getAttribute(ATTR_MAX_LINE_STATUS);
										String strMinLineSts=eleOrderLn.getAttribute(ATTR_MIN_LINE_STATUS);
										if(quantity == 0 && STATUS_CANCEL.equals(strMinLineSts) && STATUS_CANCEL.equals(strMaxLineSts)) {
											noOfOpenShipments++;																			
										}
									}
								}
								if(noOfOpenShipments == (iNoofShpLns-1)) {
									eleShipment.setAttribute(ATTR_ACTION, ACTION_CANCEL);
									printLogs("Setting Shipment Action as Cancel, as the open shipmentlines are shorted.");
								}
							}
						}
						Element eleAPIShpmntLn=XMLUtil.getElementByXPath(getShipmentListOuputDoc, "/Shipments/Shipment/ShipmentLines/ShipmentLine[@ShipmentLineKey='"+strShpmntLnKey+"']");
						if(!YFCCommon.isVoid(eleAPIShpmntLn)) {
							Element eleOrderLine=SCXmlUtil.getChildElement(eleAPIShpmntLn, ELE_ORDER_LINE);
							String strMaxLneSts=eleOrderLine.getAttribute(ATTR_MAX_LINE_STATUS);
							String strMinLneSts=eleOrderLine.getAttribute(ATTR_MIN_LINE_STATUS);
							if(STATUS_SOM_READY_FOR_PICKUP.equals(strMaxLneSts) && STATUS_SOM_READY_FOR_PICKUP.equals(strMinLneSts)) {
								printLogs("Cancelled line is in Ready For Customer status, Restock alert needs to be raised");
								
								String strOrderHeaderKey=eleShipmentLine.getAttribute(ATTR_ORDER_HEADER_KEY);			
								Element eleOrdr=SCXmlUtil.getChildElement(eleAPIShpmntLn, ELE_ORDER);			
								String strCustFstName=eleOrdr.getAttribute(ATTR_CUSTOMER_FIRST_NAME);
								String strCustLstName=eleOrdr.getAttribute(ATTR_CUSTOMER_LAST_NAME);
								String strCustName=strCustFstName+" "+strCustLstName;
								
								String strQueueId="VSI_RESTOCK_"+strShipNode;
								String strItemId=eleAPIShpmntLn.getAttribute(ATTR_ITEM_ID);
								String strQuantity=eleAPIShpmntLn.getAttribute("BackroomPickedQuantity");
								String strCustPONo=eleOrderLine.getAttribute(ATTR_CUST_PO_NO);
								
								printLogs("Invoking raiseRestockAlert method with strShipNode as "+strShipNode+" strOrderHeaderKey as "+strOrderHeaderKey+
			    						" orderLineKey as "+strOrderLineKey+" strCustPONo as "+strCustPONo+" strItemId as "+strItemId+" strQuantity as "+strQuantity+
			    						" and strCustName as "+strCustName);
								
								docInboxIn=raiseRestockAlert(env, strOrderHeaderKey, strCustName, strShipNode, strQueueId, strItemId,		//OMS-3171 Change
										strQuantity, strOrderLineKey, strCustPONo);
							}
						}					
						eleShipmentLine.setAttribute(ATTR_ACTION, DELETE);
						env.setTxnObject("IsCOMPartialCancellation", "Y");
					}
					//OMS-3171 Changes -- Start
					if(!YFCCommon.isVoid(docInboxIn)) {
						sendRestockPushNotification(env, docInboxIn);
					}
					//OMS-3171 Changes -- End
				//OMS-2827 Changes -- Start
				}
				//OMS-2827 Changes -- End
			}
			//OMS-3178 Changes -- End
			else{
				printLogs("Shipment Action is Not Cancel, exiting UE");
			}
		}catch (Exception e) {
			printLogs("Exception in VSIBeforeChangeShipmentUE Class and beforeChangeShipment Method");
			printLogs("The exception is [ "+ e.getMessage() +" ]");
			throw new YFSException();
		}
		
		//OMS-3178 Changes -- Start
		printLogs("changeShipment API Input while exiting UE: "+SCXmlUtil.getString(inXML));
		//OMS-3178 Changes -- End
		printLogs("================Exiting VSIBeforeChangeShipmentUE class and beforeChangeShipment Method================================");
		return inXML;
	}
	//OMS-3171 Changes -- Start
	private void sendRestockPushNotification(YFSEnvironment env, Document docInboxIn)
			throws Exception {
		
		printLogs("================Inside sendRestockPushNotification Method================");
		
		printLogs("Preparing the document to be sent to VBook Push Notification Q for Restock Alerts");
		Element eleInboxIn=docInboxIn.getDocumentElement();		
		Element eleInbxRfrncsLst=SCXmlUtil.getChildElement(eleInboxIn, ELE_INBOX_REFERANCES_LIST);
		NodeList nlInbxRfrncs=eleInbxRfrncsLst.getElementsByTagName(ELE_INBOX_REFERANCES);
		for(int i=0; i<nlInbxRfrncs.getLength(); i++) {
			Element eleInbxRfrncs=(Element)nlInbxRfrncs.item(i);
			String strName=eleInbxRfrncs.getAttribute(ATTR_NAME);
			if(ATTR_ITEM_ID.equals(strName) || "Qty".equals(strName)){
				eleInbxRfrncs.getParentNode().removeChild(eleInbxRfrncs);
				i--;
			}
		}
		printLogs("Document prepared for sending to VBook Push Notification Q for Restock Alerts is: "+SCXmlUtil.getString(docInboxIn));
		printLogs("Sending the Restock Alert details to Push Notification Queue");
		VSIUtils.invokeService(env, "VSISOMRestockAlertPush", docInboxIn);
		printLogs("Restock Alert details were sent to Push Notofication Queue");
		
		printLogs("================Exiting sendRestockPushNotification Method================");
	}
	//OMS-3171 Changes -- End
	//OMS-2827 Changes -- Start
	private Document prepareJDARequest(YFSEnvironment env, Element eleShpmntOut, Element eleShpmentLines, Element eleShpmntLn,
			Element eleOrder) throws Exception {
		
		printLogs("================Inside prepareJDARequest Method================");
		
		String strShipNode=eleShpmntOut.getAttribute(ATTR_SHIP_NODE);
		
		Document docJDARequest=XMLUtil.createDocument("VSIEnvelope");
		Element eleJDARequest=docJDARequest.getDocumentElement();
		
		putElementValue(eleJDARequest,"MessageType", "AllocationRequest");
		
		Element eleMessage = SCXmlUtil.createChild(eleJDARequest, "Message");
		
		String strReleaseNo=eleShpmntLn.getAttribute(ATTR_RELEASE_NO);
		
		String strCreatets=eleOrder.getAttribute(ATTR_CREATETS);
		String strOrdNo=eleOrder.getAttribute(ATTR_ORDER_NO);
		String strOrderDate=eleOrder.getAttribute(ATTR_ORDER_DATE);
		String strCustNo=eleOrder.getAttribute(ATTR_BILL_TO_ID);
		//OMS-3729 Changes -- Start
		String strOrderType=eleOrder.getAttribute(ATTR_ORDER_TYPE);
		String strEnteredBy=eleOrder.getAttribute(ATTR_ENTERED_BY);
		String strStore=null;
		//OMS-3729 Changes -- End
		String strOrderNo=strOrdNo+"*"+strReleaseNo;
		
		putElementValue(eleMessage,"DateTimeStamp", strCreatets);
		putElementValue(eleMessage,"OrderNo", strOrderNo);
		putElementValue(eleMessage,"OrderType", "Ship_to_Home");
		putElementValue(eleMessage,"IntOrderDate", strOrderDate);		
		//OMS-3729 Changes -- Start
		if(MARKETPLACE.equals(strOrderType)) {
			strStore=strEnteredBy;
		}else {
			strStore=SHIP_NODE_6102_VALUE;
		}
		putElementValue(eleMessage,"Store", strStore);			//OMS-3011 Change
		//OMS-3729 Changes -- End
		putElementValue(eleMessage,"WhseNo", strShipNode);
		putElementValue(eleMessage,"CustNo", strCustNo);
		
		NodeList nlShipmentLine=eleShpmentLines.getElementsByTagName(ELE_SHIPMENT_LINE);
		for(int i=0; i<nlShipmentLine.getLength(); i++){
			Element eleShipmentLne=(Element)nlShipmentLine.item(i);
			Element eleItem=SCXmlUtil.createChild(eleMessage, ELE_ITEM);
			String strJdaSku=eleShipmentLne.getAttribute(ATTR_ITEM_ID);			
			String strOrigQty=eleShipmentLne.getAttribute(ATTR_QUANTITY);				
										
			putElementValue(eleItem,"JdaSku", strJdaSku);
			putElementValue(eleItem,"OrigQty", strOrigQty);
			putElementValue(eleItem,"QtyOrdered", "0");
		}
		
		printLogs("JDA Reverse Allocation Request Prepared is: "+SCXmlUtil.getString(docJDARequest));
		
		VSIUtils.invokeService(env, "VSISOMJDAReverseAllocation_DB", docJDARequest);
		printLogs("JDA Reverse Allocation Request is posted to DB successfully");
		
		printLogs("================Exiting prepareJDARequest Method================");
		
		return docJDARequest;
	}
	
	private void invokeJDAWebservice(YFSEnvironment env, Document docJDARequest)
			throws Exception {
		
		printLogs("================Inside invokeJDAWebservice Method================");
		
		DataOutputStream wr=null;
		
		String strJDAURL= YFSSystem.getProperty(PROP_FORCED_ALLOCATION_URL);
		
		String reqString=XMLUtil.getXMLString(docJDARequest);
		printLogs("JDA Request in String format: "+reqString);
		
		String strSplit[]=strJDAURL.split("\\?");
		String endPointURL=strSplit[0].concat("?");
		String strArg1=strSplit[1];
		printLogs("endPointURL is: "+endPointURL);
		printLogs("strArg1: "+strArg1);
		
		String xmlencoded = URLEncoder.encode(reqString,UTF8_ENCODING);
		String strContent=strArg1+"&request="+xmlencoded;
		printLogs("xmlencoded => "+xmlencoded);
		printLogs("strContent => "+strContent);
		
		URL url = new URL(endPointURL);	
		 
		//open connection using url
		HttpURLConnection connection =  (HttpURLConnection) url.openConnection();
		
		//get web service response
		connection.setRequestMethod(JDA_POST_METHOD);
		byte[] bContent = strContent.getBytes();	
		connection.setRequestProperty(JDA_REQUEST_PROP, String.valueOf(bContent.length));
		connection.setDefaultUseCaches(false);
		connection.setDoOutput(true);

		Document responseDoc=null;
		printLogs("Invoking the JDA web service");
			 
		wr = new DataOutputStream (connection.getOutputStream());
		wr.write(bContent);
		printLogs("Invoked the JDA service");
		
		if(connection.getResponseCode() == -1 || connection.getResponseMessage() == null){
			printLogs("Inside If condition when ResponseCode is -1 or ResponseMessage is null");
		}
			 
		if(connection.getResponseCode() != 200)
		{
			printLogs("Inside If condition when ResponseCode not equals 200");
			printLogs(connection.getResponseCode()+" - "+connection.getResponseMessage());				
		}
		
		if(connection.getResponseCode() == 200)
		{
			printLogs("Inside If condition when ResponseCode equals 200");
			responseDoc=parseDoc(connection.getInputStream());
			printLogs("Response from JDA WebService: "+XMLUtil.getXMLString(responseDoc));
			
			VSIUtils.invokeService(env, "VSISOMJDAReverseAllocation_DB", responseDoc);
			printLogs("JDA Reverse Allocation Response is posted to DB successfully");
		}
		
		printLogs("================Exiting invokeJDAWebservice Method================");
	}
	//OMS-2827 Changes -- End
	
	//OMS-3178 Changes -- Start
	private Document raiseRestockAlert(YFSEnvironment env, String strOrderHeaderKey, String strCustName, String strShipNode,		//OMS-3171 Change
			String strQueueId, String strItemId, String strQuantity, String strOrderLineKey, String strCustPONo)
			throws Exception {
		
		printLogs("================Inside raiseRestockAlert Method================");
		
		Document docInboxIn=XMLUtil.createDocument(ELE_INBOX);
		Element eleInboxIn=docInboxIn.getDocumentElement();
		eleInboxIn.setAttribute(ATTR_ACTIVE_FLAG, FLAG_Y);
		eleInboxIn.setAttribute(ATTR_ORDER_HEADER_KEY, strOrderHeaderKey);
		eleInboxIn.setAttribute(ATTR_ORDER_LINE_KEY, strOrderLineKey);
		eleInboxIn.setAttribute(ATTR_ORDER_NO, strCustPONo);
		eleInboxIn.setAttribute(ATTR_EXCEPTION_TYPE, "Restock");
		eleInboxIn.setAttribute(ATTR_QUEUE_ID, strQueueId);
		eleInboxIn.setAttribute(ATTR_SHIPNODE_KEY, strShipNode);
		Element eleInbxRfrncsLst=SCXmlUtil.createChild(eleInboxIn, ELE_INBOX_REFERANCES_LIST);
		Element eleItemReference=SCXmlUtil.createChild(eleInbxRfrncsLst, ELE_INBOX_REFERANCES);
		eleItemReference.setAttribute(ATTR_NAME, ATTR_ITEM_ID);
		eleItemReference.setAttribute(ATTR_REFERENCE_TYPE, "TEXT");
		eleItemReference.setAttribute(ATTR_VALUE, strItemId);
		Element eleQtyReference=SCXmlUtil.createChild(eleInbxRfrncsLst, ELE_INBOX_REFERANCES);
		eleQtyReference.setAttribute(ATTR_NAME, "Qty");
		eleQtyReference.setAttribute(ATTR_REFERENCE_TYPE, "TEXT");
		eleQtyReference.setAttribute(ATTR_VALUE, strQuantity);						
		Element eleCustNameRef=SCXmlUtil.createChild(eleInbxRfrncsLst, ELE_INBOX_REFERANCES);
		eleCustNameRef.setAttribute(ATTR_NAME, "Name");
		eleCustNameRef.setAttribute(ATTR_REFERENCE_TYPE, "TEXT");
		eleCustNameRef.setAttribute(ATTR_VALUE, strCustName);
								
		printLogs("createException API Input: "+SCXmlUtil.getString(docInboxIn));				
		
		VSIUtils.invokeAPI(env, API_CREATE_EXCEPTION, docInboxIn);
		
		printLogs("createException API was invoked successfully");
		
		printLogs("================Exiting raiseRestockAlert Method================");
		//OMS-3171 Changes -- Start
		return docInboxIn;
		//OMS-3171 Changes -- End
	}

	private Document callGetShipmentList(YFSEnvironment env, String strShpmntKey)
			throws YIFClientCreationException, RemoteException {
		
		printLogs("================Inside callGetShipmentList Method================");
		
		Document getShipmentListInputDoc = SCXmlUtil.createDocument(ELE_SHIPMENT);
		Element eleShipmentListInputDoc = getShipmentListInputDoc.getDocumentElement();
		eleShipmentListInputDoc.setAttribute(ATTR_SHIPMENT_KEY, strShpmntKey);
		
		printLogs("getShipmentList API Input: "+SCXmlUtil.getString(getShipmentListInputDoc));				
		Document getShipmentListOuputDoc = VSIUtils.invokeAPI(env, "global/template/api/getShipmentList_beforeChngShpmntUE.xml", API_GET_SHIPMENT_LIST, getShipmentListInputDoc);
		printLogs("getShipmentList API Output: "+SCXmlUtil.getString(getShipmentListOuputDoc));
		
		printLogs("================Exiting callGetShipmentList Method================");
		
		return getShipmentListOuputDoc;
	}
	//OMS-3178 Changes -- End
	
	private void printLogs(String mesg) {
		if(log.isDebugEnabled()){
			log.debug(TAG +" : "+mesg);
		}
	}
	
	//OMS-2827 Changes -- Start
	public void putElementValue(Element childEle, String key, Object value) {
		Element ele = SCXmlUtil.createChild(childEle, key);
		if(value instanceof String ) {
			ele.setTextContent((String)value);
		}else if(value instanceof Element ) {
			ele.appendChild((Element)value);
		}
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
	 	printLogs("JDA WebService Response being Parsed "+instream);
	 	
	 	printLogs("================Exiting parseDoc Method================");
	 	return doc;
	}
	//OMS-2827 Changes -- End
	
	
	private void setPartialCancellationEmailMsg(YFSEnvironment env,Document inputShipment) throws Exception {
		log.info("In setPartialCancellationEmailMsg method");
	    Document partialCnclEnvObj = (Document) env.getTxnObject("JDA_PARTIAL_CNCL");
		
		Element eleShipment = inputShipment.getDocumentElement();
		Element eleShpmntLns=SCXmlUtil.getChildElement(eleShipment, ELE_SHIPMENT_LINES);
		Element eleShpmntLn=SCXmlUtil.getChildElement(eleShpmntLns, ELE_SHIPMENT_LINE);
		String ohk = eleShpmntLn.getAttribute("OrderHeaderKey");
		String olk = eleShpmntLn.getAttribute("OrderLineKey");
		Document getOrderListInput = XMLUtil.createDocument(ELE_ORDER);
		Element eleOrderInput = getOrderListInput.getDocumentElement();
		Boolean publishFlag=false;
		if(!YFCCommon.isVoid(ohk)){
			eleOrderInput.setAttribute(VSIConstants.ATTR_ORDER_HEADER_KEY, ohk);
			Document getOrderListoutDoc = VSIUtils.invokeAPI(env, TEMPLATE_GET_ORD_LIST_VSI_CNCL_EMAIL, VSIConstants.API_GET_ORDER_LIST,getOrderListInput);			
			Element eleOrderOut = (Element)getOrderListoutDoc.getElementsByTagName(ELE_ORDER).item(0);
			Element eleOrderLines = (Element)getOrderListoutDoc.getElementsByTagName(VSIConstants.ELE_ORDER_LINES).item(0);
			
			NodeList orderLineList = getOrderListoutDoc.getElementsByTagName(VSIConstants.ELE_ORDER_LINE);
			int linelength= orderLineList.getLength();
			for(int i=0;i<linelength;i++){
				Element orderLineElement = (Element)orderLineList.item(i);
				String strOrderLinekey = orderLineElement.getAttribute(VSIConstants.ATTR_ORDER_LINE_KEY);				
				if(!olk.equalsIgnoreCase(strOrderLinekey)){
					eleOrderLines.removeChild(orderLineElement);
					linelength--;
					i--;
				}else{
					publishFlag=true;
					orderLineElement.setAttribute("Publish", "Y"); 
					if(!YFCCommon.isVoid(partialCnclEnvObj)) {
						Element orderLineEle = (Element)partialCnclEnvObj.getElementsByTagName(VSIConstants.ELE_ORDER_LINES).item(0);
						Document docOrderLines = XMLUtil.getDocumentForElement(orderLineEle);
						XMLUtil.importElement(orderLineEle, orderLineElement);
						env.setTxnObject("JDA_PARTIAL_CNCL",partialCnclEnvObj);
						log.info("Setting partial cnl transaction obj::"+XMLUtil.getXMLString(partialCnclEnvObj));
					}
				}

			}
			if(YFCCommon.isVoid(partialCnclEnvObj)) {
				env.setTxnObject("JDA_PARTIAL_CNCL",getOrderListoutDoc);
				log.info("Setting partial cnl  obj::"+XMLUtil.getXMLString(getOrderListoutDoc));
			}
		}
		
	}
}
