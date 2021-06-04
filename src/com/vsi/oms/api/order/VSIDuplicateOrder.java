package com.vsi.oms.api.order;

import java.rmi.RemoteException;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.vsi.oms.utils.VSIConstants;
import com.vsi.oms.utils.VSIUtils;
import com.yantra.interop.japi.YIFClientCreationException;

import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;

public class VSIDuplicateOrder {

	
	private YFCLogCategory log = YFCLogCategory.instance(VSIDuplicateOrder.class);
	
/**invoking the VSIOrderCreate
 * 
 * @param env
 * @param inXML
 * @return
 * @throws YFSException
 * @throws RemoteException
 * @throws ParserConfigurationException
 * @throws YIFClientCreationException
 */
	
	
	public Document checkDuplicateOrder(YFSEnvironment env, Document inXML) throws YFSException, RemoteException, ParserConfigurationException,
			YIFClientCreationException{
		
		if(log.isDebugEnabled()){
			log.verbose("Printing Input XML :" + SCXmlUtil.getString(inXML));
			log.info("================Inside checkDuplicateOrder================================");
		}
		
		Document outdoc = null;
		String strIsChangeReq="";
		Element orderElement = inXML.getDocumentElement();
		Document getOrderListInput = null;
		getOrderListInput=SCXmlUtil.createDocument("Order");
		Element eleOrderElement = getOrderListInput.getDocumentElement();
		eleOrderElement.setAttribute(VSIConstants.ATTR_CUST_PO_NO, orderElement.getAttribute(VSIConstants.ATTR_CUST_PO_NO));
		if("MCL".equals(orderElement.getAttribute(VSIConstants.ATTR_ENTERPRISE_CODE)))
		{
			Element eleExtnIp = SCXmlUtil.getChildElement(orderElement, "Extn");
			if(!YFCCommon.isVoid(eleExtnIp))
			{
				String extnMCLOrderShipmentID = eleExtnIp.getAttribute("ExtnMCLOderShipmentID");
				if(!YFCCommon.isVoid(extnMCLOrderShipmentID))
				{
					Element extnEle = SCXmlUtil.createChild(eleOrderElement, "Extn");
					extnEle.setAttribute("ExtnMCLOderShipmentID", extnMCLOrderShipmentID);
				}	
			}
		}
		eleOrderElement.setAttribute(VSIConstants.ATTR_ENTERPRISE_CODE, orderElement.getAttribute(VSIConstants.ATTR_ENTERPRISE_CODE));
				
		 outdoc = VSIUtils.invokeAPI(env, VSIConstants.TEMPLATE_GET_ORDER_LIST, VSIConstants.API_GET_ORDER_LIST, getOrderListInput);
		 Element eleOrderList = outdoc.getDocumentElement();
		 
		 // check if order has same customerPoNo and enterprise code
		 if(eleOrderList.hasChildNodes()) {
			 
		 //setting the duplicate variable to Y
		 orderElement.setAttribute(VSIConstants.ATTR_DUPLICATE_ORDER, "Y");
		
		 
		 Element eleOrder = SCXmlUtil.getChildElement(eleOrderList,"Order");
		  String strOrderNo=eleOrder.getAttribute(VSIConstants.ATTR_ORDER_NO);
		  orderElement.setAttribute("OrderNo", strOrderNo);
		  orderElement.setAttribute("Status", eleOrder.getAttribute("Status"));
		  orderElement.setAttribute(VSIConstants.ATTR_ORDER_HEADER_KEY, eleOrder.getAttribute(VSIConstants.ATTR_ORDER_HEADER_KEY));
		  
		  String strEntryType=orderElement.getAttribute(VSIConstants.ATTR_ENTRY_TYPE);
			 
			 if(VSIConstants.WHOLESALE.equals(strEntryType))
			 {
				    boolean bIsLineReleased = false;
				    Document docGetCommonCodeInput = SCXmlUtil.createDocument(VSIConstants.ELEMENT_COMMON_CODE);
					Element eleCommonCodeElement = docGetCommonCodeInput.getDocumentElement();
					eleCommonCodeElement.setAttribute(VSIConstants.ATTR_CODE_TYPE, VSIConstants.ATTR_VSI_WH_CHANGE_ORDER);
					eleCommonCodeElement.setAttribute(VSIConstants.ATTR_CODE_VALUE, (orderElement.getAttribute(VSIConstants.ATTR_ENTERPRISE_CODE)));
					eleCommonCodeElement.setAttribute(VSIConstants.ATTR_ORG_CODE, VSIConstants.ATTR_DEFAULT);
					Document docgetCommonCodeOutput = VSIUtils.invokeAPI(env,VSIConstants.API_COMMON_CODE_LIST, docGetCommonCodeInput);
					Element commonCodeListElement = docgetCommonCodeOutput.getDocumentElement();
					
					if(commonCodeListElement.hasChildNodes()){
						Element eleCommonCode = SCXmlUtil.getChildElement(commonCodeListElement, VSIConstants.ELE_COMMON_CODE);						
						strIsChangeReq = SCXmlUtil.getAttribute(eleCommonCode, VSIConstants.ATTR_CODE_SHORT_DESCRIPTION);	
					}
						
						if((!YFCCommon.isVoid(strIsChangeReq)) && VSIConstants.FLAG_Y.equals(strIsChangeReq))
						{
							
							try {
								orderElement.setAttribute(VSIConstants.ATTR_DUPLICATE_ORDER, "N");
								orderElement.setAttribute("IsChangeOrder", "Y");
								orderElement.setAttribute("Override", "Y");
								orderElement.setAttribute(VSIConstants.ATTR_SELECT_METHOD, VSIConstants.SELECT_METHOD_WAIT);
								NodeList nlOrderLineOut = eleOrder.getElementsByTagName(VSIConstants.ELE_ORDER_LINE);
			            		for(int i = 0; i < nlOrderLineOut.getLength(); i++){
			            			Element eleOrderLine = (Element) nlOrderLineOut.item(i);
			            			String minLineStatus = eleOrderLine.getAttribute(VSIConstants.ATTR_MIN_LINE_STATUS);			        				     				
			        				
			            			
			            			String strOrderLineKey = SCXmlUtil.getAttribute(eleOrderLine, VSIConstants.ATTR_ORDER_LINE_KEY);
			            			 Element eleItemout = SCXmlUtil.getChildElement(eleOrderLine,VSIConstants.ELE_ITEM);
			            			 String strItemIdOut=eleItemout.getAttribute(VSIConstants.ITEM_ID);
			            			
			            			 Element elePersonInfoOut= SCXmlUtil.getChildElement(eleOrderLine,VSIConstants.ELE_PERSON_INFO_SHIP_TO);
			            			 Element elePersonInfoExtnOut= SCXmlUtil.getChildElement(elePersonInfoOut,VSIConstants.ELE_EXTN);
			            			 String strExtnMarkForStoreNo=elePersonInfoExtnOut.getAttribute(VSIConstants.ATTR_EXTN_MARK_FOR_STORE_NO);	       				         				            				
			                        			            				
			            				NodeList nlOrderLineIn = orderElement.getElementsByTagName(VSIConstants.ELE_ORDER_LINE);
					            		for(int j = 0; j < nlOrderLineIn.getLength(); j++){
					            			Element eleOrderLineIn = (Element) nlOrderLineIn.item(j);
					            			
					            			 Element eleItemIn = SCXmlUtil.getChildElement(eleOrderLineIn,VSIConstants.ELE_ITEM);
					            			 String strItemIdIn=eleItemIn.getAttribute(VSIConstants.ITEM_ID);
					            			
					            			 Element elePersonInfoIn= SCXmlUtil.getChildElement(eleOrderLineIn,VSIConstants.ELE_PERSON_INFO_SHIP_TO);
					            			 Element elePersonInfoExtnIN= SCXmlUtil.getChildElement(elePersonInfoIn,VSIConstants.ELE_EXTN);
					            			 String strExtnMarkForStoreNoIn=elePersonInfoExtnIN.getAttribute(VSIConstants.ATTR_EXTN_MARK_FOR_STORE_NO);	 
					            			 
					            			 if((strItemIdOut.equals(strItemIdIn)) && (strExtnMarkForStoreNo.equals(strExtnMarkForStoreNoIn)))
					            			 {
					            				 eleOrderLineIn.setAttribute(VSIConstants.ATTR_ORDER_LINE_KEY, strOrderLineKey);
					            				 eleOrderLineIn.setAttribute(VSIConstants.ATTR_ORDER_HEADER_KEY, eleOrder.getAttribute(VSIConstants.ATTR_ORDER_HEADER_KEY));
					            				 
					            				 if((Double.parseDouble(minLineStatus)>3200) || Double.parseDouble(minLineStatus)==3200 ){
					            					
														
					            					 bIsLineReleased=true;
														
					            					 
					            				 }
					            				 
					            			 }          			 
					            			 
					            		
			            				
			            			}
			            		}
			            		
			            		if(bIsLineReleased)
			            		{
			            			orderElement.setAttribute("ChangeStatus", "Rejected");
			            			VSIUtils.invokeService(env, "VSIWHOrderChangeNotificationEmail", inXML);
			            		}
			            		else {
			            			VSIUtils.invokeAPI(env, VSIConstants.API_CHANGE_ORDER, inXML);
			            			//OMS-3512 OMS-3513 OMS-3514 OMS-3515 Changes -- Start
									/*
									 * orderElement.setAttribute("ChangeStatus", "Accepted");
									 * VSIUtils.invokeService(env, "VSIWHOrderChangeNotificationEmail", inXML);
									 */
			            			//OMS-3512 OMS-3513 OMS-3514 OMS-3515 Changes -- End
			            		}
			            		
			            		
			                	
			                }                
			               
						
							catch(Exception e)
							{
								e.printStackTrace();
							}
						
						
						
						}
						
						
						 
					
					
			 }
		  
		  }//end of if
		 
		 	 
		  return inXML;
		}// end of method checkDuplicateOrder()
	
}// end of class
