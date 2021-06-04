package com.vsi.som.shipment;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.vsi.oms.api.web.VSIInvokeJDAWebservice;
import com.vsi.oms.utils.VSIConstants;
import com.vsi.oms.utils.VSIUtils;
import com.vsi.oms.utils.XMLUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfs.japi.YFSEnvironment;

public class VSISFSShipConfirmJDAAllocation implements VSIConstants
{
	private YFCLogCategory log = YFCLogCategory.instance(VSISFSShipConfirmJDAAllocation.class);
	public void shipConfirmJDAAllocation(YFSEnvironment env, Document inXml)
	{
		log.info("Input for VSISFSShipConfirmJDAAllocation.shipConfirmJDAAllocation => "+XMLUtil.getXMLString(inXml));
		if(log.isDebugEnabled())
			log.debug("Input for VSISFSShipConfirmJDAAllocation.shipConfirmJDAAllocation => "+XMLUtil.getXMLString(inXml));
		try
		{
			//Mixed Cart Changes -- Start
			Element eleShipment = inXml.getDocumentElement();
			String strDeliveryMethod=eleShipment.getAttribute(VSIConstants.ATTR_DELIVERY_METHOD);
			String strOrderHeaderKey=eleShipment.getAttribute(VSIConstants.ATTR_ORDER_HEADER_KEY);
			if(VSIConstants.ATTR_DEL_METHOD_SHP.equals(strDeliveryMethod)){
			//Mixed Cart Changes -- End
				String transferNo = null;
				Document jdaAllocationInXml=XMLUtil.createDocument(ELE_ORDER);
				Element orderEle = jdaAllocationInXml.getDocumentElement();
				VSISOMJDAReverseAllocation obj = new VSISOMJDAReverseAllocation();
				Element shipmentLines = (Element) inXml.getElementsByTagName(ELE_SHIPMENT_LINES).item(0);
				NodeList shipmentLineNode = shipmentLines.getElementsByTagName(ELE_SHIPMENT_LINE);
				Element shipmentLineElement = (Element) shipmentLineNode.item(0);
				obj.putElementValue(orderEle,ATTR_ORDER_NO, shipmentLineElement.getAttribute(ATTR_ORDER_NO)+"*"+shipmentLineElement.getAttribute(ATTR_RELEASE_NO));
				Document docOrderReleaseListInput  = XMLUtil.createDocument(ELE_ORDER_RELEASE);
				Element eleOrderReleaseListInput = docOrderReleaseListInput.getDocumentElement();
				eleOrderReleaseListInput.setAttribute(ATTR_ORDER_RELEASE_KEY, shipmentLineElement.getAttribute(ATTR_ORDER_RELEASE_KEY));
				Document docOrderReleaseList = VSIUtils.invokeAPI(env, TEMPLATE_GET_ORD_REL_LIST_SEND_RELEASE, API_GET_ORDER_RELEASE_LIST, docOrderReleaseListInput);
				Element orderRelease = (Element) docOrderReleaseList.getElementsByTagName(ELE_ORDER_RELEASE).item(0);
				Element extn = (Element) orderRelease.getElementsByTagName(ELE_EXTN).item(0);
				transferNo = extn.getAttribute(ATTR_EXTN_TRANSFER_NO);
				log.info("transferNo => "+transferNo);
				obj.putElementValue(orderEle,ATTR_TRANSFER_NO, transferNo);
				obj.putElementValue(orderEle,ATTR_ORDER_TYPE, ORDER_TYPE_VALUE);
				obj.putElementValue(orderEle,ATTR_STATUS, SHIP_STATUS_VALUE);
				Element orderLines = SCXmlUtil.createChild(orderEle, ELE_ORDER_LINES);
				for (int i = 0; i < shipmentLineNode.getLength(); i++)
				{
					Element shipmentLineElem = (Element) shipmentLineNode.item(i);
					Element orderLine = SCXmlUtil.createChild(orderLines, ELE_ORDER_LINE);
					obj.putElementValue(orderLine,ATTR_ORD_QTY, shipmentLineElem.getAttribute(ATTR_ACTUAL_QUANTITY));
					Element item = SCXmlUtil.createChild(orderLine, ELE_ITEM);
					obj.putElementValue(item,ATTR_ITEM_ID, shipmentLineElem.getAttribute(ATTR_ITEM_ID));
				}
				log.info("jdaAllocationInXml => "+XMLUtil.getXMLString(jdaAllocationInXml));
				VSIUtils.invokeService(env, SERVICE_SFS_SHIP_JDA_ALLOCATION_REQUEST_DB, jdaAllocationInXml);
				//Mixed Cart Changes -- Start
				postJDARequest(env, jdaAllocationInXml);
				
			}else if(VSIConstants.ATTR_DEL_METHOD_PICK.equals(strDeliveryMethod)){
				
				if(log.isDebugEnabled()){
					log.debug("Shipment contains PICK lines and hence will be processed");
				}
				
				Document docJDARequest=XMLUtil.createDocument(VSIConstants.ELE_ORDER);
				Element eleJDARequest=docJDARequest.getDocumentElement();
				eleJDARequest.setAttribute(VSIConstants.ATTR_ORDER_HEADER_KEY, strOrderHeaderKey);
				
				Element eleShipmentLines=SCXmlUtil.getChildElement(eleShipment, VSIConstants.ELE_SHIPMENT_LINES);
				Element eleShipmentLine=(Element)eleShipmentLines.getElementsByTagName(VSIConstants.ELE_SHIPMENT_LINE).item(0);
				Element eleOrder=SCXmlUtil.getChildElement(eleShipmentLine, VSIConstants.ELE_ORDER);
				String strOrderDate=eleOrder.getAttribute(VSIConstants.ATTR_ORDER_DATE);
				String strCustPONo=eleShipment.getAttribute(VSIConstants.ATTR_SHIP_CUST_PO_NO);
				String strStsDate=eleShipment.getAttribute("StatusDate");
				String strShipNode=eleShipment.getAttribute(VSIConstants.ATTR_SHIP_NODE);
				
				putElementValue(eleJDARequest,"OrderDate", strOrderDate);
				putElementValue(eleJDARequest,"OrderNo", strCustPONo);
				putElementValue(eleJDARequest,"OrderType", "BOPUS");
				putElementValue(eleJDARequest,"StatusDate", strStsDate);
				putElementValue(eleJDARequest,"Status", "PICKUP");
				putElementValue(eleJDARequest,"FullfillingStore", strShipNode);
				
				Element eleOrderLines=SCXmlUtil.createChild(eleJDARequest, VSIConstants.ELE_ORDER_LINES);
				NodeList nlShpmntLine=eleShipmentLines.getElementsByTagName(VSIConstants.ELE_SHIPMENT_LINE);
				for(int i=0; i<nlShpmntLine.getLength(); i++){
					Element eleShpmntLine = (Element)nlShpmntLine.item(i);
					String strOrdQty=eleShpmntLine.getAttribute(VSIConstants.ATTR_ACTUAL_QUANTITY);					
					String strItemId=eleShpmntLine.getAttribute(VSIConstants.ATTR_ITEM_ID);
					Element eleOrdLnOut=SCXmlUtil.createChild(eleOrderLines, VSIConstants.ELE_ORDER_LINE);
					putElementValue(eleOrdLnOut,"OrderedQty", strOrdQty);
					Element eleItemOut=SCXmlUtil.createChild(eleOrdLnOut, VSIConstants.ELE_ITEM);
					putElementValue(eleItemOut,"ItemID", strItemId);
				}
				
				if(log.isDebugEnabled()){
					log.debug("JDA Allocation Request Prepared is: "+SCXmlUtil.getString(docJDARequest));
				}
				
				if(log.isDebugEnabled()){
					log.debug("JDA Allocation Request will be posted to Q");
				}
				
				postJDARequest(env, docJDARequest);
				
				if(log.isDebugEnabled()){
					log.debug("JDA Allocation Request is posted to Q successfully");					
				}
			}
			//Mixed Cart Changes -- End
		}
		catch(Exception e)
		{
			log.info("VSISFSShipConfirmJDAAllocation.shipConfirmJDAAllocation() exception as below => ");
			e.printStackTrace();
		}
	}
	//Mixed Cart Changes -- Start
	private void postJDARequest(YFSEnvironment env, Document jdaAllocationInXml) {
		
		try
		{
			VSIUtils.invokeService(env, SERVICE_SFS_SHIP_JDA_ALLOCATION_REQUEST_Q, jdaAllocationInXml);
		}
		catch(Exception e)
		{
			log.info("Posting to SFS - Ship Confirm JDA Request to MQ - Error as below");
			e.printStackTrace();
			String orderNo=jdaAllocationInXml.getElementsByTagName(ATTR_ORDER_NO).item(0).getTextContent().toString();
			NodeList orderLineList = jdaAllocationInXml.getElementsByTagName(ELE_ORDER_LINE);
			String itemId="";
			for (int i = 0; i < orderLineList.getLength(); i++) 
			{
				Element orderLineElem = (Element) orderLineList.item(i);
				Element itemEle = (Element) orderLineElem.getElementsByTagName(ELE_ITEM).item(0);
				if(orderLineList.getLength() == 1)
				{
					itemId=itemEle.getElementsByTagName(ATTR_ITEM_ID).item(0).getTextContent().toString();
					break;
				}
				else
					itemId=itemEle.getElementsByTagName(ATTR_ITEM_ID).item(0).getTextContent().toString()+" "+itemId;
			}
			log.info("Entire itemId - SFS JDA Request =>  "+ itemId);
			if(itemId.length() >= 31)
				itemId = itemId.substring(0,31).concat("...");
			log.info("Truncated/Entire itemId - SFS JDA Request =>  "+ itemId);
			VSIInvokeJDAWebservice object = new VSIInvokeJDAWebservice();
			object.alertForJDASendReleaseFailure(env, itemId, orderNo, "", ALERT_SFS_SHIP_JDA_REQ_POST_Q_FAILURE_EXCEPTION_TYPE, ALERT_SFS_SHIP_JDA_REQ_POST_Q_FAILURE_DETAIL_DESCRIPTION, ALERT_SFS_SHIP_JDA_REQ_POST_Q_FAILURE_QUEUE);
		}
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
}
