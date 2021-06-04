package com.vsi.som.shipment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

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

public class VSICreateShipmentOnReleaseUpdated extends VSIBaseCustomAPI implements VSIConstants
{
	private YFCLogCategory log = YFCLogCategory.instance(VSICreateShipmentOnReleaseUpdated.class);
	//OMS-2956 Changes -- Start
	private static final String TAG = VSICreateShipmentOnReleaseUpdated.class.getSimpleName();
	//OMS-2956 Changes -- End
	public void createShipmentOnRelease(YFSEnvironment env, Document inXml)
	{
		printLogs("VSICreateShipmentOnReleaseUpdated.createShipmentOnRelease() => "+SCXmlUtil.getString(inXml));		//OMS-2956 Change
		try
		{
		Element orderRelease = inXml.getDocumentElement();
		Element orderElement = (Element) orderRelease.getElementsByTagName(ELE_ORDER).item(0);
		Element personInfoBillTo = (Element) orderElement.getElementsByTagName(ELE_PERSON_INFO_BILL_TO).item(0);
		String name = personInfoBillTo.getAttribute(ATTR_FIRST_NAME);
		String lastName = personInfoBillTo.getAttribute(ATTR_LAST_NAME);
		name = name.concat(" ").concat(lastName);
		
		String sFirstName = personInfoBillTo.getAttribute(VSIConstants.ATTR_FIRST_NAME);
		sFirstName = sFirstName.toLowerCase();
		String sLastName = personInfoBillTo.getAttribute(VSIConstants.ATTR_LAST_NAME);
		sLastName = sLastName.toLowerCase();
		String sEmailID = personInfoBillTo.getAttribute("EMailID");
		sEmailID = sEmailID.toLowerCase();
		
		String orderHeaderKey = orderElement.getAttribute(ATTR_ORDER_HEADER_KEY);
		Element orderLine = (Element) orderRelease.getElementsByTagName(ELE_ORDER_LINE).item(0);
		
		//changes to get customer po no from order line key from release xml
		String orderLineKey=orderLine.getAttribute("OrderLineKey");
		Document createOrderLineListInput = XMLUtil.createDocument(ELE_ORDER_LINE);
		Element elementOrderLine = createOrderLineListInput.getDocumentElement();
		elementOrderLine.setAttribute(ATTR_ORDER_LINE_KEY,orderLineKey);
		env.setApiTemplate(VSIConstants.API_GET_ORDER_LINE_LIST, "global/template/api/getOrderLineList_CreateShipment.xml");
		Document outDoc  = VSIUtils.invokeAPI(env, VSIConstants.API_GET_ORDER_LINE_LIST,createOrderLineListInput);
		env.clearApiTemplates();
		Element orderLineList = outDoc.getDocumentElement();
		Element orderLineElement = (Element) orderLineList.getElementsByTagName(ELE_ORDER_LINE).item(0);
		String customerPoNo=orderLineElement.getAttribute("CustomerPONo");
           // changes done 
		NodeList orderLineNode = orderRelease.getElementsByTagName(ELE_ORDER_LINE);
		Document createShipmentInput = XMLUtil.createDocument(ELE_SHIPMENT);
		Element eleShipment = createShipmentInput.getDocumentElement();
		eleShipment.setAttribute(ATTR_DOCUMENT_TYPE, orderElement.getAttribute(ATTR_DOCUMENT_TYPE));
		eleShipment.setAttribute(ATTR_ORDER_NO, orderElement.getAttribute(ATTR_ORDER_NO));
		eleShipment.setAttribute(ATTR_ORDER_HEADER_KEY, orderHeaderKey);
		eleShipment.setAttribute(ATTR_SCAC, orderRelease.getAttribute(ATTR_SCAC));
		eleShipment.setAttribute(ATTR_SHIP_NODE, orderRelease.getAttribute(ATTR_SHIP_NODE));
		eleShipment.setAttribute(ATTR_SHIPMENT_TYPE, ATTR_SOM_SHIPMENT_TYPE);
		eleShipment.setAttribute(ATTR_SHIP_CUST_PO_NO, customerPoNo);
		//OMS-2881 Changes -- Start
		String strExpctdShpmntDt=null;
		String strReqShpDt=orderRelease.getAttribute(ATTR_REQ_SHIP_DATE);
		if(log.isDebugEnabled()){
			log.debug("ReqShipDate: "+strReqShpDt);
		}
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");  
		ArrayList<Element> listBOPUSBufferHours;
		listBOPUSBufferHours = VSIUtils.getCommonCodeList(env, "BOPUS_BUFFER_HRS", "Buffer Hours", VSIConstants.ATTR_DEFAULT);
		if(!listBOPUSBufferHours.isEmpty()){
			Element eleCommonCode=listBOPUSBufferHours.get(0);
			String strBufferHrs=eleCommonCode.getAttribute(VSIConstants.ATTR_CODE_SHORT_DESCRIPTION);
			if(log.isDebugEnabled()){
				log.debug("strBufferHrs: "+strBufferHrs);
			}
			int iBufferHrs=Integer.parseInt(strBufferHrs);
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(formatter.parse(strReqShpDt));
			calendar.add(Calendar.HOUR,iBufferHrs);
			strExpctdShpmntDt=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
				.format(calendar.getTime());
			if(log.isDebugEnabled()){
				log.debug("strExpctdShpmntDt: "+strExpctdShpmntDt);
			}
		}
		if(!YFCCommon.isVoid(strExpctdShpmntDt)){
			eleShipment.setAttribute(ATTR_EXPECTED_SHIPMENT_DATE, strExpctdShpmntDt);
			if(log.isDebugEnabled()){
				log.debug("ExpectedShipmentDate attribute is set on createShipment API Input");
			}
		}
		//OMS-2881 Changes -- End
		//OMS-2956 Changes -- Start
		boolean bBOSTSLnPrsnt=false;
		Document docGetOrdLstIn=XMLUtil.createDocument(ELE_ORDER);
		Element eleGetOrdLstIn=docGetOrdLstIn.getDocumentElement();
		eleGetOrdLstIn.setAttribute(ATTR_ORDER_HEADER_KEY, orderHeaderKey);
		
		printLogs("getOrderList API input: "+SCXmlUtil.getString(docGetOrdLstIn));
		Document docGetOrdLstOut = VSIUtils.invokeAPI(env, TEMPLATE_ORDER_LIST_ORDER_MONITOR,VSIConstants.API_GET_ORDER_LIST, docGetOrdLstIn);			
		printLogs("getOrderList API output: "+SCXmlUtil.getString(docGetOrdLstOut));
		//OMS-2956 Changes -- End
		Element eleShipmentLines = createShipmentInput.createElement(ELE_SHIPMENT_LINES);
		for(int i=0; i< orderLineNode.getLength(); i++)
		{
			Element orderLineElem = (Element) orderRelease.getElementsByTagName(ELE_ORDER_LINE).item(i);
			//OMS-2956 Changes -- Start
			String strLineType=orderLineElem.getAttribute(ATTR_LINE_TYPE);
			printLogs("Line Type is: "+strLineType);
			if(LINETYPE_PUS.equals(strLineType)){
				String strShipNode=orderLineElem.getAttribute(ATTR_SHIP_NODE);
				if(!YFCCommon.isVoid(strShipNode)){
					printLogs("Ship Node is: "+strShipNode);
					Element eleOrdLnFrmIn=XMLUtil.getElementByXPath(docGetOrdLstOut, "/OrderList/Order/OrderLines/OrderLine[@LineType='SHIP_TO_STORE' and @ShipNode='"+strShipNode+"']");
					if(!YFCCommon.isVoid(eleOrdLnFrmIn)){
						printLogs("BOSTS line for the same store is available");
						bBOSTSLnPrsnt=true;
					}
				}				
			}
			//OMS-2956 Changes -- End
			Element itemEle = (Element) orderLineElem.getElementsByTagName(ELE_ITEM).item(0);
			Element orderStatusesEle = (Element) orderLineElem.getElementsByTagName(ELE_ORDER_STATUSES).item(0);
			Element orderStatusEle = (Element) orderStatusesEle.getElementsByTagName(ELE_ORDER_STATUS).item(0);
			Element eleShipmentLine = SCXmlUtil.createChild(eleShipmentLines, ELE_SHIPMENT_LINE);
			eleShipmentLine.setAttribute(ATTR_ITEM_ID, itemEle.getAttribute(ATTR_ITEM_ID));
			eleShipmentLine.setAttribute(ATTR_ORDER_LINE_KEY, orderLineElem.getAttribute(ATTR_ORDER_LINE_KEY));
			eleShipmentLine.setAttribute(ATTR_PRIME_LINE_NO, orderLineElem.getAttribute(ATTR_PRIME_LINE_NO));
			eleShipmentLine.setAttribute(ATTR_SUB_LINE_NO, orderLineElem.getAttribute(ATTR_SUB_LINE_NO));
			eleShipmentLine.setAttribute(ATTR_QUANTITY, orderStatusEle.getAttribute(ATTR_STATUS_QUANTITY));			
			eleShipmentLine.setAttribute(ATTR_PRODUCT_CLASS, itemEle.getAttribute(ATTR_PRODUCT_CLASS));
			eleShipmentLine.setAttribute(ATTR_UOM, itemEle.getAttribute(ATTR_UOM));		
			eleShipmentLine.setAttribute(ATTR_ORDER_RELEASE_KEY, orderStatusEle.getAttribute(ATTR_ORDER_RELEASE_KEY));			
			eleShipmentLine.setAttribute(ATTR_RELEASE_NO, orderRelease.getAttribute(ATTR_RELEASE_NO));
			eleShipmentLines.appendChild(eleShipmentLine);
		}
		eleShipment.appendChild(eleShipmentLines);
		//OMS-2956 Changes -- Start
		Element extnEle = createShipmentInput.createElement(ELE_EXTN);
		extnEle.setAttribute(ATTR_EXTN_NAME, name);
		extnEle.setAttribute("ExtnCaseInsensitiveFirstName", sFirstName);
		extnEle.setAttribute("ExtnCaseInsensitiveLastName", sLastName);
		extnEle.setAttribute("ExtnCaseInsensitiveEmail", sEmailID);
		if(bBOSTSLnPrsnt){
			printLogs("BOSTS line available, hence setting ExtnBOSTSFlag as Y");
			extnEle.setAttribute("ExtnBOSTSFlag", FLAG_Y);
		}
		eleShipment.appendChild(extnEle);
		//OMS-2956 Changes -- End
		printLogs("createShipmentInput => "+SCXmlUtil.getString(createShipmentInput));		//OMS-2956 Change
		VSIUtils.invokeAPI(env, API_CREATE_SHIPMENT,createShipmentInput);
				
		//Mixed Cart Changes -- Start
		if(log.isDebugEnabled()){
			log.debug("Create Shipment API was invoked successfully");
			log.debug("Now posting the input to JDA Allocation Queue");
			log.debug("Input XML: "+SCXmlUtil.getString(inXml));
		}
		VSIUtils.invokeService(env, "VSIMixedCartJDAAllocationSync", inXml);
		if(log.isDebugEnabled()){
			log.debug("Input was posted to JDA Allocation Queue successfully");
		}
		//Mixed Cart Changes -- End
				
		}
		catch(Exception e)
		{
			//OMS-2956 Changes -- Start
			log.info("Exception in VSICreateShipmentOnReleaseUpdated.createShipmentOnRelease() as below => ");
			printLogs("The exception is [ "+ e.getMessage() +" ]");
			//OMS-2956 Changes -- End			
		}
	}
	//OMS-2956 Changes -- Start
	private void printLogs(String mesg) {
		if(log.isDebugEnabled()){
			log.debug(TAG +" : "+mesg);
		}
	}
	//OMS-2956 Changes -- End
}