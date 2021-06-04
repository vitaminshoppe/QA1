package com.vsi.oms.api;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

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

public class VSICancelPickedSOMOrders extends VSIBaseCustomAPI implements VSIConstants
{
	private YFCLogCategory log = YFCLogCategory.instance(VSICancelPickedSOMOrders.class);
	//OMS-2946 Changes -- Start
	private static final String TAG = VSICancelPickedSOMOrders.class.getSimpleName();
	//OMS-2946 Changes -- End
	public void cancelPickedOrders(YFSEnvironment env, Document inXml)
	{
		try
		{
				log.info("================Inside VSICancelPickedSOMOrders Class and cancelPickedOrders Method================");
				printLogs("Printing Input XML :"+SCXmlUtil.getString(inXml));		//OMS-2946 Changes
			
				Element orderStatusEle = (Element) inXml.getElementsByTagName(ELE_ORDER_STATUS).item(0);
				String originalOrderLineKey = orderStatusEle.getAttribute(ATTR_ORDER_LINE_KEY);
				Document getOrderLineListDoc = SCXmlUtil.createDocument(ELE_ORDER_LINE);
				Element eleOrderLine = getOrderLineListDoc.getDocumentElement();
				eleOrderLine.setAttribute(ATTR_ORDER_LINE_KEY, originalOrderLineKey);
				eleOrderLine.setAttribute(ATTR_DOCUMENT_TYPE, ATTR_DOCUMENT_TYPE_SALES);
				//OMS-3084 Changes -- Start
				printLogs("Printing getOrderLineList Input: "+SCXmlUtil.getString(getOrderLineListDoc));
				//OMS-3084 Changes -- End
				//Add template to fetch condition variable1 for Order Monitor Cancel on backroom picked status change				
				Document getOrderLineListOutputDoc = VSIUtils.invokeAPI(env, TEMPLATE_ORDER_LINE_LIST,API_GET_ORDER_LINE_LIST, getOrderLineListDoc);
				//SOM Restock changes -- Start				
				printLogs("Printing getOrderLineList output: "+SCXmlUtil.getString(getOrderLineListOutputDoc));		//OMS-2946 Changes
				//OMS-3084 Changes -- Start
				//Element eleOrderLineList=getOrderLineListOutputDoc.getDocumentElement();
				//Element eleOrdLine=(Element)eleOrderLineList.getElementsByTagName(ELE_ORDER_LINE).item(0);
				//OMS-3084 Changes -- End
				String strOrderedQty=null;
				String strShipNode=null;
				String strCustPONo=null;
				String strItemId=null;
						
				
				//SOM Restock changes -- End
				Element eleExtn = (Element) getOrderLineListOutputDoc.getElementsByTagName(ELE_EXTN).item(0);				
				String lastPickDate = null;
				if(eleExtn.getAttribute(ATTR_EXTN_LAST_PICK_DATE_FOR_CANCEL) != ""){
					lastPickDate = eleExtn.getAttribute(ATTR_EXTN_LAST_PICK_DATE_FOR_CANCEL);
					//OMS-3084 Changes -- Start
					printLogs("ExtnLastPickDateForCancel value is available: "+lastPickDate);
					//OMS-3084 Changes -- End
				}
				else{
					lastPickDate = eleExtn.getAttribute(ATTR_EXTN_LAST_PICK_DATE);
					//OMS-3084 Changes -- Start
					printLogs("ExtnLastPickDateForCancel value is not available, ExtnLastPickDate will be used: "+lastPickDate);
					//OMS-3084 Changes -- End
				}
				String orderHeaderKey = orderStatusEle.getAttribute(ATTR_ORDER_HEADER_KEY);
				
				SimpleDateFormat formatter = new SimpleDateFormat(YYYY_MM_DD_T_HH_MM_SS);
                Calendar calculatedOrderTime = Calendar.getInstance();
                calculatedOrderTime.setTime(formatter.parse(lastPickDate));
                if(log.isDebugEnabled())
                	log.info("calculatedOrderTime(lastPickDate) => "+calculatedOrderTime.getTime());
                Date localTime = new Date();
                SimpleDateFormat converter = new SimpleDateFormat(YYYY_MM_DD_T_HH_MM_SS);
                converter.setTimeZone(TimeZone.getTimeZone(EST_TIME_ZONE));
                String currTime = converter.format(localTime);
                Calendar currentTime = Calendar.getInstance();
                currentTime.setTime(formatter.parse(currTime));
                if(log.isDebugEnabled())
                	log.info("currTime => "+ currTime + "currentTime => "+currentTime.getTime());   

                if (currentTime.compareTo(calculatedOrderTime) > 0)
                {
                	//OMS-3084 Changes -- Start
                	printLogs("ExtnLastPickDateForCancel time passed order will be cancelled");
					//OMS-3084 Changes -- End
                	Document getOrderListInXML=XMLUtil.createDocument(ELE_ORDER);
    				Element getOrderListEle = getOrderListInXML.getDocumentElement();
    				getOrderListEle.setAttribute(ATTR_ORDER_HEADER_KEY,orderHeaderKey);
    				getOrderListEle.setAttribute(ATTR_DOCUMENT_TYPE, ATTR_DOCUMENT_TYPE_SALES);
    				//OMS-3084 Changes -- Start
    				printLogs("Printing getOrderList Input: "+SCXmlUtil.getString(getOrderListInXML));
    				//OMS-3084 Changes -- End
					//Add template to fetch condition variable1 for Order Monitor Cancel on backroom picked status change
    				Document getOrderListOutXML = VSIUtils.invokeAPI(env, TEMPLATE_GET_ORDER_LIST_FOR_ORDER_MONITOR, API_GET_ORDER_LIST, getOrderListInXML);
    				//OMS-3002 Changes -- Start
    				//OMS-3084 Changes -- Start
    				printLogs("Printing getOrderList Output: "+SCXmlUtil.getString(getOrderListOutXML));
    				//OMS-3084 Changes -- End
    				Element OrderEle = (Element) getOrderListOutXML.getElementsByTagName(ELE_ORDER).item(0);
    				String strCustFstName=OrderEle.getAttribute(ATTR_CUSTOMER_FIRST_NAME);
    				String strCustLstName=OrderEle.getAttribute(ATTR_CUSTOMER_LAST_NAME);
    				String strCustName=strCustFstName+" "+strCustLstName;
    				//OMS-3002 Changes -- End
    				NodeList orderLineNode = getOrderListOutXML.getElementsByTagName(ELE_ORDER_LINE);
    				int orderLineNodeLength = orderLineNode.getLength();
    				//OMS-3084 Changes -- Start
    				printLogs("orderLineNodeLength: "+Integer.toString(orderLineNodeLength));
    				//OMS-3084 Changes -- End
    				if(orderLineNodeLength == 1)
    				{
    					//OMS-3084 Changes -- Start
    					printLogs("Single Line Order");
        				//OMS-3084 Changes -- End						
    					Element orderLineEle = (Element) getOrderListOutXML.getElementsByTagName(ELE_ORDER_LINE).item(0);
						Element eleItem=SCXmlUtil.getChildElement(orderLineEle, ELE_ITEM);
						strOrderedQty=orderLineEle.getAttribute(ATTR_ORD_QTY);
				        strShipNode=orderLineEle.getAttribute(ATTR_SHIP_NODE);
				        strCustPONo=orderLineEle.getAttribute(ATTR_CUST_PO_NO);
				        strItemId=eleItem.getAttribute(ATTR_ITEM_ID);
    					String lineType = orderLineEle.getAttribute(ATTR_LINE_TYPE);
    					//OMS-3171 Changes -- Start
    					if(lineType.equalsIgnoreCase(LINETYPE_PUS) || lineType.equalsIgnoreCase(LINETYPE_STS)) {
    							Document docInboxIn = changeOrderLineCancellation(orderHeaderKey, orderLineEle.getAttribute(ATTR_CONDITION_VARIBALE1),originalOrderLineKey, env,strItemId,strOrderedQty,strShipNode,strCustPONo,strCustName);    							
    							if(!YFCCommon.isVoid(docInboxIn)) {
    								sendRestockPushNotification(env, docInboxIn);
    							}
    					}
    					//OMS-3171 Changes -- End
    				}
    				else
    				{
    					//OMS-3084 Changes -- Start
    					printLogs("Multi Line Order");
        				//OMS-3084 Changes -- End
    					int shipToStore = 0, pickInStore = 0;
    					String lastPickDateVal = null;
    					//ArrayList<String> orderLineKeys = new ArrayList<String>();
						ArrayList<Element> orderLineObjects = new ArrayList<Element>();
						
    					for(int l=0; l< orderLineNodeLength; l++)
    					{
    						Element orderLineEle = (Element) getOrderListOutXML.getElementsByTagName(ELE_ORDER_LINE).item(l);
        					String lineTypeValue = orderLineEle.getAttribute(ATTR_LINE_TYPE);
        					if(lineTypeValue.equalsIgnoreCase(LINETYPE_STS))
        						shipToStore++;
        					else if(lineTypeValue.equalsIgnoreCase(LINETYPE_PUS))
        						pickInStore++;
    					}
    					//OMS-3084 Changes -- Start
    					printLogs("Number of PICK_IN_STORE lines: "+Integer.toString(pickInStore)+" Number of SHIP_TO_STORE lines: "+Integer.toString(shipToStore));
        				//OMS-3084 Changes -- End
    					if(orderLineNodeLength == shipToStore || orderLineNodeLength == pickInStore)
    					{
    						//OMS-3084 Changes -- Start
    						printLogs("It's either a full BOPUS or full STS order");
            				//OMS-3084 Changes -- End
    						//OMS-3171 Changes -- Start
    						Document docInboxIn = null;
    						//OMS-3171 Changes -- End
    						for(int l=0; l< orderLineNodeLength; l++)
    						{
    							Element orderLineEle = (Element) getOrderListOutXML.getElementsByTagName(ELE_ORDER_LINE).item(l);
    							//OMS-3084 Changes -- Start
    							String strOrdLnKey=orderLineEle.getAttribute(ATTR_ORDER_LINE_KEY);
    							printLogs("OrderLine with OLK as "+strOrdLnKey+" is being processed");
                				//OMS-3084 Changes -- End
							    Element eleItem=SCXmlUtil.getChildElement(orderLineEle, ELE_ITEM);
    							String conditionVar = orderLineEle.getAttribute(ATTR_CONDITION_VARIBALE1);
            					double maxLineStatus = Double.parseDouble(orderLineEle.getAttribute(ATTR_MAX_LINE_STATUS));
            					Element elemExtn = (Element) orderLineEle.getElementsByTagName(ELE_EXTN).item(0);
            					log.info("elemExtn.getAttribute(ATTR_EXTN_LAST_PICK_DATE_FOR_CANCEL) => "+elemExtn.getAttribute(ATTR_EXTN_LAST_PICK_DATE_FOR_CANCEL));
								//Element eleItem=SCXmlUtil.getChildElement(orderLineEle, ELE_ITEM);
						         strOrderedQty=orderLineEle.getAttribute(ATTR_ORD_QTY);
				                  strShipNode=orderLineEle.getAttribute(ATTR_SHIP_NODE);
				                  strCustPONo=orderLineEle.getAttribute(ATTR_CUST_PO_NO);
				                 strItemId=eleItem.getAttribute(ATTR_ITEM_ID);
            					if(elemExtn.getAttribute(ATTR_EXTN_LAST_PICK_DATE_FOR_CANCEL) != "")
            					{
            						lastPickDateVal = elemExtn.getAttribute(ATTR_EXTN_LAST_PICK_DATE_FOR_CANCEL);
            						log.info("In If - lastPickDateVal => "+lastPickDateVal);
            					}
            					else
            					{
            						lastPickDateVal = elemExtn.getAttribute(ATTR_EXTN_LAST_PICK_DATE);
            						log.info("In Else - lastPickDateVal => "+lastPickDateVal);
            					}
            					log.info("lastPickDateVal => "+lastPickDateVal);
        						SimpleDateFormat formater = new SimpleDateFormat(YYYY_MM_DD_T_HH_MM_SS);
        		                Calendar calculatedLastPickDate = Calendar.getInstance();
        		                calculatedLastPickDate.setTime(formater.parse(lastPickDateVal));
								//Add one more status condition for Order Monitor Cancel on backroom picked status change
        		                if(((maxLineStatus == 3200.500) && (currentTime.compareTo(calculatedLastPickDate) > 0)) || ((maxLineStatus == 3350.30) && (currentTime.compareTo(calculatedLastPickDate) > 0) && (conditionVar.equalsIgnoreCase("Y")))){
        		                	//OMS-3084 Changes -- Start        							
        		                	printLogs("OrderLine with OLK as "+strOrdLnKey+" will be cancelled");
        		                	docInboxIn = changeOrderLineCancellation(orderHeaderKey, conditionVar, strOrdLnKey, env,strItemId,strOrderedQty,strShipNode,strCustPONo,strCustName);		//OMS-3171 Change
        		                }
                    			//OMS-3084 Changes -- End
    						}
    						//OMS-3171 Changes -- Start
    						if(!YFCCommon.isVoid(docInboxIn)) {
    							sendRestockPushNotification(env, docInboxIn);
    						}
    						//OMS-3171 Changes -- End
    					}
    					else if(shipToStore > 0 && pickInStore > 0)
    					{
    						//OMS-3084 Changes -- Start
    						printLogs("Order contains both BOPUS and STS lines");
            				//OMS-3084 Changes -- End
    						boolean bosts = false;
    						for(int j=0; j< orderLineNodeLength; j++)
    						{
    							Element orderLineEle = (Element) getOrderListOutXML.getElementsByTagName(ELE_ORDER_LINE).item(j);
    							//OMS-3084 Changes -- Start
    							String strOrdLnKey=orderLineEle.getAttribute(ATTR_ORDER_LINE_KEY);
    							printLogs("OrderLine with OLK as "+strOrdLnKey+" is being processed");
                				//OMS-3084 Changes -- End
            					String lineTypeValue = orderLineEle.getAttribute(ATTR_LINE_TYPE);
            					//OMS-2946 Changes -- Start
            					printLogs("Line Type is: "+lineTypeValue);
            					if(LINETYPE_PUS.equals(lineTypeValue) || LINETYPE_STS.equals(lineTypeValue)){
            					//OMS-2946 Changes -- End
									String conditionVariable= orderLineEle.getAttribute(ATTR_CONDITION_VARIBALE1);
	            					double maxLineStatus = Double.parseDouble(orderLineEle.getAttribute(ATTR_MAX_LINE_STATUS));
	            					Element elemExtn = (Element) orderLineEle.getElementsByTagName(ELE_EXTN).item(0);
	            					log.info("Combo - elemExtn.getAttribute(ATTR_EXTN_LAST_PICK_DATE_FOR_CANCEL) => "+elemExtn.getAttribute(ATTR_EXTN_LAST_PICK_DATE_FOR_CANCEL));
	            					if(elemExtn.getAttribute(ATTR_EXTN_LAST_PICK_DATE_FOR_CANCEL) != "")
	            					{
	            						lastPickDateVal = elemExtn.getAttribute(ATTR_EXTN_LAST_PICK_DATE_FOR_CANCEL);
	            						log.info("Combo In If - lastPickDateVal => "+lastPickDateVal);
	            					}
	            					else
	            					{
	            						lastPickDateVal = elemExtn.getAttribute(ATTR_EXTN_LAST_PICK_DATE);
	            						log.info("Combo In Else - lastPickDateVal => "+lastPickDateVal);
	            					}
	            					log.info("lastPickDateVal in Combo => "+lastPickDateVal);
	            					SimpleDateFormat formater = new SimpleDateFormat(YYYY_MM_DD_T_HH_MM_SS);
	        		                Calendar calculatedLastPickDate = Calendar.getInstance();
	        		                calculatedLastPickDate.setTime(formater.parse(lastPickDateVal));
	            					if((lineTypeValue.equalsIgnoreCase(LINETYPE_PUS) && (maxLineStatus == 3200.500) && (currentTime.compareTo(calculatedLastPickDate) > 0)) || (lineTypeValue.equalsIgnoreCase(LINETYPE_PUS) && (maxLineStatus == 3350.30) && (currentTime.compareTo(calculatedLastPickDate) > 0) && (conditionVariable.equalsIgnoreCase("Y")))){
										orderLineObjects.add(orderLineEle);
	            						//orderLineKeys.add(orderLineEle.getAttribute(ATTR_ORDER_LINE_KEY));
										//OMS-3084 Changes -- Start	    							
										printLogs("PICK_IN_STORE with OLK as "+strOrdLnKey+" will be cancelled");
		                				//OMS-3084 Changes -- End
	            					}
	            					else if((lineTypeValue.equalsIgnoreCase(LINETYPE_STS) && (maxLineStatus < 3200.500) && (currentTime.compareTo(calculatedLastPickDate) > 0)) || (lineTypeValue.equalsIgnoreCase(LINETYPE_STS) && (maxLineStatus < 3350.30) && (currentTime.compareTo(calculatedLastPickDate) > 0) && (conditionVariable.equalsIgnoreCase("Y"))) )
	            					{
	            						//OMS-3084 Changes -- Start	    							
	            						printLogs("SHIP_TO_STORE with OLK as "+strOrdLnKey+" is not available for Customer Pick yet and hence orderlines will not be cancelled");
		                				//OMS-3084 Changes -- End
	            						bosts = true;
	            						break;
	            					}
	            					else if ((lineTypeValue.equalsIgnoreCase(LINETYPE_STS) && (maxLineStatus == 3200.500) && (currentTime.compareTo(calculatedLastPickDate) > 0))|| (lineTypeValue.equalsIgnoreCase(LINETYPE_STS) && (maxLineStatus == 3350.30) && (currentTime.compareTo(calculatedLastPickDate) > 0) && (conditionVariable.equalsIgnoreCase("Y"))) )
	            					{
										orderLineObjects.add(orderLineEle);
	            						//orderLineKeys.add(orderLineEle.getAttribute(ATTR_ORDER_LINE_KEY));
										//OMS-3084 Changes -- Start	    							
										printLogs("SHIP_TO_STORE with OLK as "+strOrdLnKey+" will be cancelled");
		                				//OMS-3084 Changes -- End
	            					}
	            				//OMS-2946 Changes -- Start
	    						}else{
    								printLogs("OrderLine with OLK as "+strOrdLnKey+" is not a BOPUS or STS line, hence will not be cancelled");
    							}
            					//OMS-2946 Changes -- End
    						}
    						if(!bosts)
    						{
    							//Iterator<String> iter = orderLineKeys.iterator();
    						   //   while (iter.hasNext())
    						    //	  changeOrderLineCancellation(orderHeaderKey, iter.next().toString(), env);
    							//OMS-3084 Changes -- Start
									/*Iterator<Element> iter = orderLineObjects.iterator();
    						      while (iter.hasNext())*/    								    							
    							printLogs("OrderLines are available for cancellation");
    							//OMS-3171 Changes -- Start
        						Document docInboxIn = null;
        						//OMS-3171 Changes -- End
    							for (Element eleOrdLn : orderLineObjects) {    									    							
    								printLogs("OrderLine Element: "+SCXmlUtil.getString(eleOrdLn));                    				
    								String strCondVar1=eleOrdLn.getAttribute(VSIConstants.ATTR_CONDITION_VARIBALE1);
    							    String strOrdLnKey=eleOrdLn.getAttribute(VSIConstants.ATTR_ORDER_LINE_KEY);    							    
    							    strOrderedQty=eleOrdLn.getAttribute(ATTR_ORD_QTY);
  				                  	strShipNode=eleOrdLn.getAttribute(ATTR_SHIP_NODE);
  				                  	strCustPONo=eleOrdLn.getAttribute(ATTR_CUST_PO_NO);
  				                  	Element eleItem=SCXmlUtil.getChildElement(eleOrdLn, ELE_ITEM);
  				                  	strItemId=eleItem.getAttribute(ATTR_ITEM_ID);
  				                  	
  				                  	printLogs("OrderLine with OLK as "+strOrdLnKey+" will be cancelled");
  				                  	docInboxIn = changeOrderLineCancellation(orderHeaderKey, strCondVar1, strOrdLnKey, env,strItemId,strOrderedQty,strShipNode,strCustPONo,strCustName);		//OMS-3171 Change
    							}
    							//OMS-3084 Changes -- End
    							//OMS-3171 Changes -- Start
        						if(!YFCCommon.isVoid(docInboxIn)) {
        							sendRestockPushNotification(env, docInboxIn);
        						}
        						//OMS-3171 Changes -- End
    						}
    					}
    					//OMS-2946 Changes -- Start
    					else if((pickInStore > 0) || (shipToStore > 0)){
    						printLogs("Order contains atleast one BOPUS line or a STS line");
    						//OMS-3171 Changes -- Start
    						Document docInboxIn = null;
    						//OMS-3171 Changes -- End
    						for(int k=0; k< orderLineNodeLength; k++)
    						{
    							Element eleOrderLn=(Element)orderLineNode.item(k);
    							String strLineType=eleOrderLn.getAttribute(ATTR_LINE_TYPE);
    							printLogs("Line Type is: "+strLineType);
    							String strOrdLnKey=eleOrderLn.getAttribute(ATTR_ORDER_LINE_KEY);
    							if(LINETYPE_PUS.equals(strLineType) || LINETYPE_STS.equals(strLineType)){    								
    								printLogs("OrderLine with OLK as "+strOrdLnKey+" is being processed");    							    
        							String strCondVar1 = eleOrderLn.getAttribute(ATTR_CONDITION_VARIBALE1);
                					double dMaxLineStatus = Double.parseDouble(eleOrderLn.getAttribute(ATTR_MAX_LINE_STATUS));                					
                					strOrderedQty=eleOrderLn.getAttribute(ATTR_ORD_QTY);
    				                strShipNode=eleOrderLn.getAttribute(ATTR_SHIP_NODE);
    				                strCustPONo=eleOrderLn.getAttribute(ATTR_CUST_PO_NO);
    				                Element eleItem=SCXmlUtil.getChildElement(eleOrderLn, ELE_ITEM);
    				                strItemId=eleItem.getAttribute(ATTR_ITEM_ID);
    				                Element eleOrdLnExtn = SCXmlUtil.getChildElement(eleOrderLn, ELE_EXTN);
                					if(eleOrdLnExtn.getAttribute(ATTR_EXTN_LAST_PICK_DATE_FOR_CANCEL) != "")
                					{
                						lastPickDateVal = eleOrdLnExtn.getAttribute(ATTR_EXTN_LAST_PICK_DATE_FOR_CANCEL);
                						printLogs("In If - lastPickDateVal => "+lastPickDateVal);
                					}
                					else
                					{
                						lastPickDateVal = eleOrdLnExtn.getAttribute(ATTR_EXTN_LAST_PICK_DATE);
                						printLogs("In Else - lastPickDateVal => "+lastPickDateVal);
                					}
                					printLogs("lastPickDateVal => "+lastPickDateVal);
            						SimpleDateFormat formater = new SimpleDateFormat(YYYY_MM_DD_T_HH_MM_SS);
            		                Calendar calculatedLastPickDate = Calendar.getInstance();
            		                calculatedLastPickDate.setTime(formater.parse(lastPickDateVal));
            		                if(((dMaxLineStatus == 3200.500) && (currentTime.compareTo(calculatedLastPickDate) > 0)) || ((dMaxLineStatus == 3350.30) && (currentTime.compareTo(calculatedLastPickDate) > 0) && (strCondVar1.equalsIgnoreCase("Y")))){
            		                	printLogs("OrderLine with OLK as "+strOrdLnKey+" will be cancelled");
            		                	docInboxIn = changeOrderLineCancellation(orderHeaderKey, strCondVar1, strOrdLnKey, env,strItemId,strOrderedQty,strShipNode,strCustPONo,strCustName);		//OMS-3171 Change
            		                }
    							}else{
    								printLogs("OrderLine with OLK as "+strOrdLnKey+" is not a BOPUS or STS line, hence will not be cancelled");
    							}
    						}
    						//OMS-3171 Changes -- Start
    						if(!YFCCommon.isVoid(docInboxIn)) {
    							sendRestockPushNotification(env, docInboxIn);
    						}
    						//OMS-3171 Changes -- End
    					}
    					//OMS-2946 Changes -- End
    				}
                }
                //OMS-2946 Changes -- Start
                else{
                	log.info("ExtnLastPickDateForCancel has not passed order will not be cancelled");
                }
                log.info("================Exiting VSICancelPickedSOMOrders Class and cancelPickedOrders Method================");
              	//OMS-2946 Changes -- End
		}
		catch(Exception e)
		{
			//OMS-3084 Changes -- Start
			printLogs("Exception in VSICancelPickedSOMOrders Class and cancelPickedOrders Method");
			printLogs("The exception is [ "+ e.getMessage() +" ]");
			//OMS-3084 Changes -- End
			e.printStackTrace();
		}
	}
	
	//OMS-3171 Changes -- Start
		private void sendRestockPushNotification(YFSEnvironment env, Document docInboxIn) throws Exception {
			
			printLogs("================Inside sendRestockPushNotification Method================");
			
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
			
			printLogs("================Exiting sendRestockPushNotification Method================");
		}
		//OMS-3171 Changes -- End
		
	public Document changeOrderLineCancellation(String orderHeaderKey, String ConditionVariable1, String orderLineKey, YFSEnvironment env, String strItemId, String strOrderedQty, String strShipNode, String strCustPONo, String strCustName)		//OMS-3171 Change
	{
		//OMS-3084 Changes -- Start
		printLogs("================Inside changeOrderLineCancellation Method================");
		//OMS-3084 Changes -- End
		
		//OMS-3171 Changes -- Start
		Document docInboxIn=null;
		//OMS-3171 Changes -- End
				
		try
		{ 
			//int totalNumberOfRecords = 0;		//OMS-3084 Changes
			printLogs("Invoking changeOrderLineCancellation => OH Key "+orderHeaderKey+" Con Var "+ConditionVariable1+" orderLineKey "+orderLineKey);		//OMS-2946 Changes
			
			if(ConditionVariable1.equalsIgnoreCase("Y")){
				Document getShipmentListInputDoc = SCXmlUtil.createDocument(ELE_SHIPMENT);
				Element eleShipment = getShipmentListInputDoc.getDocumentElement();
				eleShipment.setAttribute(ATTR_ORDER_HEADER_KEY, orderHeaderKey);
				
				//OMS-3084 Changes -- Start
				printLogs("Printing getShipmentList Input: "+SCXmlUtil.getString(getShipmentListInputDoc));
				//OMS-3084 Changes -- End
				Document getShipmentListOuputDoc = VSIUtils.invokeAPI(env, TEMPLATE_SHIPMENT_LIST, API_GET_SHIPMENT_LIST, getShipmentListInputDoc);
				//OMS-3084 Changes -- Start
				printLogs("Printing getShipmentList Output: "+SCXmlUtil.getString(getShipmentListOuputDoc));
				
				/*Element shipments = (Element) getShipmentListOuputDoc.getElementsByTagName(ELE_SHIPMENTS).item(0);
				Element shipEle = (Element) getShipmentListOuputDoc.getElementsByTagName(ELE_SHIPMENT).item(0);	
				String status = shipEle.getAttribute("Status");
				
				
				totalNumberOfRecords = Integer.parseInt(shipments.getAttribute(ATTR_TOTAL_NUMBER_OF_RECORDS));
				NodeList shipmentNode = getShipmentListOuputDoc.getElementsByTagName(ELE_SHIPMENT);				
				int shipmentNodeLength = shipmentNode.getLength();
				log.info("totalNumberOfRecords => "+totalNumberOfRecords);
				if ((totalNumberOfRecords > 0) && (!"9000".equalsIgnoreCase(status))) {				
				for(int l=0; l< shipmentNodeLength; l++)
				{
					Element shipmentEle = (Element) getShipmentListOuputDoc.getElementsByTagName(ELE_SHIPMENT).item(l);
					
					String shipmentkey = shipmentEle.getAttribute(ATTR_SHIPMENT_KEY);*/
				
					
					Element eleShpmntFrmIn=XMLUtil.getElementByXPath(getShipmentListOuputDoc, "/Shipments/Shipment[ShipmentLines/ShipmentLine/OrderLine[@OrderLineKey='"+orderLineKey+"']]");
					String shipmentkey=eleShpmntFrmIn.getAttribute(ATTR_SHIPMENT_KEY);
					String strShpmtSts=eleShpmntFrmIn.getAttribute(ATTR_STATUS);
					if(!STATUS_CANCEL.equals(strShpmtSts)){
						//OMS-3084 Changes -- End
						Document getChangeShipmentInputDoc = SCXmlUtil.createDocument(ELE_SHIPMENT);
						Element eleChngShipment = getChangeShipmentInputDoc.getDocumentElement();
						eleChngShipment.setAttribute(ATTR_SHIPMENT_KEY, shipmentkey);
						eleChngShipment.setAttribute(ATTR_ACTION, "Cancel");
						//OMS-2836 Changes -- Start
						eleChngShipment.setAttribute("RestockRequired", "N");
						//OMS-2836 Changes -- End
						
						printLogs("changeShipment API Input: "+SCXmlUtil.getString(getChangeShipmentInputDoc));
						
						Document docOutChangeShipment = VSIUtils.invokeAPI(env, VSIConstants.API_CHANGE_SHIPMENT,
								getChangeShipmentInputDoc);
						
						printLogs("changeShipment API Output: "+SCXmlUtil.getString(docOutChangeShipment));
						//OMS-3084 Changes -- Start
						//}
					}
					//OMS-3084 Changes -- End
				
				//SOM Restock changes -- Start
				String strQueueId="VSI_RESTOCK_"+strShipNode;
				docInboxIn=XMLUtil.createDocument(ELE_INBOX);		//OMS-3171 Change
				Element eleInboxIn=docInboxIn.getDocumentElement();
				eleInboxIn.setAttribute(ATTR_ACTIVE_FLAG, FLAG_Y);
				eleInboxIn.setAttribute(ATTR_ORDER_HEADER_KEY, orderHeaderKey);
				eleInboxIn.setAttribute(ATTR_ORDER_LINE_KEY, orderLineKey);
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
				eleQtyReference.setAttribute(ATTR_VALUE, strOrderedQty);
				//OMS-3002 Changes -- Start
				Element eleCustNameRef=SCXmlUtil.createChild(eleInbxRfrncsLst, ELE_INBOX_REFERANCES);
				eleCustNameRef.setAttribute(ATTR_NAME, "Name");
				eleCustNameRef.setAttribute(ATTR_REFERENCE_TYPE, "TEXT");
				eleCustNameRef.setAttribute(ATTR_VALUE, strCustName);
				//OMS-3002 Changes -- End
				
				printLogs("createException API Input: "+SCXmlUtil.getString(docInboxIn));				
				
				VSIUtils.invokeAPI(env, API_CREATE_EXCEPTION, docInboxIn);
				
				printLogs("createException API was invoked successfully");
				
				//SOM Restock changes -- End
		
		}
		
		
		Document changeOrderDoc = SCXmlUtil.createDocument(ELE_ORDER);
		Element eleOrder = changeOrderDoc.getDocumentElement();
		eleOrder.setAttribute(ATTR_OVERRIDE, VSIConstants.FLAG_Y);
		eleOrder.setAttribute(ATTR_ORDER_HEADER_KEY, orderHeaderKey);
		Element eleOrderLines = SCXmlUtil.createChild(eleOrder, ELE_ORDER_LINES);
		Element elemOrderLine = SCXmlUtil.createChild(eleOrderLines, ELE_ORDER_LINE);
		elemOrderLine.setAttribute(ATTR_ORDER_LINE_KEY, orderLineKey);
		elemOrderLine.setAttribute(ATTR_ACTION, ACTION_RELEASE_CANCEL);
		//Prod Issue Fix: Start
		eleOrder.setAttribute(ATTR_MODIFICATION_REASON_CODE, NO_CUSTOMER_PICK);
		//Prod Issue Fix: End
		
		printLogs("changeOrder API Input: "+SCXmlUtil.getString(changeOrderDoc));
		
		VSIUtils.invokeAPI(env, API_CHANGE_ORDER, changeOrderDoc);
		
		printLogs("changeOrder API was invoked successfully");
		}
		catch(Exception e)
		{
			//OMS-3084 Changes -- Start
			printLogs("Exception in VSICancelPickedSOMOrders Class and changeOrderLineCancellation Method");
			printLogs("The exception is [ "+ e.getMessage() +" ]");
			//OMS-3084 Changes -- End
			e.printStackTrace();
		}
		//OMS-3084 Changes -- Start
		printLogs("================Exiting changeOrderLineCancellation Method================");
		//OMS-3084 Changes -- End
		//OMS-3171 Changes -- Start
		return docInboxIn;
		//OMS-3171 Changes -- End
	}
	//OMS-2946 Changes -- Start
	private void printLogs(String mesg) {
		if(log.isDebugEnabled()){
			log.debug(TAG +" : "+mesg);
		}
	}
	//OMS-2946 Changes -- End
}