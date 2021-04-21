package com.vsi.som.shipment;

import java.util.Arrays;
import java.util.HashSet;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.vsi.oms.utils.VSIConstants;
import com.vsi.oms.utils.VSIUtils;
import com.vsi.oms.utils.XMLUtil;
import com.yantra.yfc.core.YFCObject;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;

public class VSIMarketplaceSFSFulfillment {
	
	private static YFCLogCategory log = YFCLogCategory.instance(VSIMarketplaceSFSFulfillment.class);
	
	private static final String TAG = VSIMarketplaceSFSFulfillment.class.getSimpleName();
	
	public Document splitShipmentContainers(YFSEnvironment env, Document inXML){
		
		printLogs("================Inside VSIMarketplaceSFSFulfillment Class and splitShipmentContainers Method================");
		printLogs("Printing Input XML: "+SCXmlUtil.getString(inXML));
		
		try{			
			Element eleShipment = inXML.getDocumentElement();
	        String strSCAC= eleShipment.getAttribute(VSIConstants.ATTR_SCAC);
	        Document docGetScacList = SCXmlUtil.createDocument("Scac");
	        Element eleGetScacList= docGetScacList.getDocumentElement();
	        eleGetScacList.setAttribute("Scac", strSCAC);
	        
	        printLogs("Input to getScacList API: "+SCXmlUtil.getString(docGetScacList));
	        Document outDoc = VSIUtils.invokeAPI(env, VSIConstants.API_GET_SCAC_LIST, docGetScacList);
	        printLogs("Output from getScacList API: "+SCXmlUtil.getString(outDoc));
	        
	        String strCarrierName=null;
	        if(!YFCObject.isNull(outDoc)){
	        	Element eleScacList= outDoc.getDocumentElement();
		        Element eleSCAC = SCXmlUtil.getChildElement(eleScacList, "Scac");
		        strCarrierName=eleSCAC.getAttribute("ScacDesc");
	        }
	        
	        String sOrderHeaderKey= eleShipment.getAttribute(VSIConstants.ATTR_ORDER_HEADER_KEY);
	        Document docGetOrderList = SCXmlUtil.createDocument(VSIConstants.ELE_ORDER);
	        Element eleOrder= docGetOrderList.getDocumentElement();
	        eleOrder.setAttribute(VSIConstants.ATTR_ORDER_HEADER_KEY, sOrderHeaderKey);
	        
	        printLogs("Input to getOrderList API: "+SCXmlUtil.getString(docGetOrderList));
	        Document getOrderlistOutput = VSIUtils.invokeAPI(env, VSIConstants.TEMPLATE_VSI_GET_ORDER_LIST_MARKETPLACE,VSIConstants.API_GET_ORDER_LIST, docGetOrderList);
	        printLogs("Output from getOrderList API: "+SCXmlUtil.getString(getOrderlistOutput));
	        
	        Element eleOrderList=null;
	        Element OrderEle=null;
	        if(!YFCObject.isNull(getOrderlistOutput)){
	        	eleOrderList= getOrderlistOutput.getDocumentElement();
	        	OrderEle = SCXmlUtil.getChildElement(eleOrderList, VSIConstants.ELE_ORDER);
	        }
	        
	        String strEnterpriseCode=eleShipment.getAttribute(VSIConstants.ATTR_ENTERPRISE_CODE);
	        boolean bIsMCLShipment=false;
	        if(VSIConstants.ENT_MCL.equals(strEnterpriseCode)){
	        	printLogs("Shipment is for MCL Enterprise");
	        	bIsMCLShipment=true;
	        }
	        
	        Element eleShipmentExtn=SCXmlUtil.getChildElement(eleShipment, VSIConstants.ELE_EXTN);
	        Element eleFrmAddr=SCXmlUtil.getChildElement(eleShipment, "FromAddress");
	        Element eleToAddr=SCXmlUtil.getChildElement(eleShipment, "ToAddress");
	        Element eleInstrcns=SCXmlUtil.getChildElement(eleShipment, "Instructions");	        
	        Element eleShpmntChrgs=SCXmlUtil.getChildElement(eleShipment, "ShipmentCharges");
	        Element eleContainers= SCXmlUtil.getChildElement(eleShipment, VSIConstants.ELE_CONTAINERS);
			NodeList nlContainer= eleContainers.getElementsByTagName(VSIConstants.ELE_CONTAINER);
			for(int k = 0; k < nlContainer.getLength(); k++){
				HashSet<String> setShpmntLnKey = new HashSet<String>();
				Element eleContainer= (Element) nlContainer.item(k);
				String strContainerNo=eleContainer.getAttribute(VSIConstants.ATTR_CONTAINER_NO);
				printLogs("Container with ContainerNo "+strContainerNo+" is being processed");
				String strTrackingNo=eleContainer.getAttribute(VSIConstants.ATTR_TRACKING_NO);
				printLogs("Tracking Number is "+strTrackingNo);
				Document docSplitShipment= SCXmlUtil.createDocument(VSIConstants.ELE_SHIPMENT);
				Element eleSplitShipment = docSplitShipment.getDocumentElement();
				eleSplitShipment=copyAttributes(eleShipment,eleSplitShipment);
				if(!YFCCommon.isVoid(strCarrierName)){
					eleSplitShipment.setAttribute("CarrierName", strCarrierName);
				}
				eleSplitShipment.setAttribute(VSIConstants.ATTR_TRACKING_NO, strTrackingNo);
				printLogs("Shipment Element after stamping attributes "+SCXmlUtil.getString(eleSplitShipment));
				Element eleShpmntExtn=SCXmlUtil.createChild(eleSplitShipment, VSIConstants.ELE_EXTN);
				eleShpmntExtn=copyAttributes(eleShipmentExtn,eleShpmntExtn);
				printLogs("Shipment/Extn is added to document "+SCXmlUtil.getString(eleSplitShipment));
				Element eleTOAddr=SCXmlUtil.createChild(eleSplitShipment, "ToAddress");
				eleTOAddr=copyAttributes(eleToAddr,eleTOAddr);
				printLogs("Shipment/ToAddress is added to document "+SCXmlUtil.getString(eleSplitShipment));
				Element eleFromAddr=SCXmlUtil.createChild(eleSplitShipment, "FromAddress");
				eleFromAddr=copyAttributes(eleFrmAddr,eleFromAddr);
				printLogs("Shipment/FromAddress is added to document "+SCXmlUtil.getString(eleSplitShipment));
				Element eleOutContainers=SCXmlUtil.createChild(eleSplitShipment, VSIConstants.ELE_CONTAINERS);
				printLogs("Shipment/Containers is added to document "+SCXmlUtil.getString(eleSplitShipment));
				Node ndContainer = docSplitShipment.importNode(eleContainer, true);
				eleOutContainers.appendChild(ndContainer);
				printLogs("Shipment/Containers/Container is added to document "+SCXmlUtil.getString(eleSplitShipment));
				Node ndInstructions = docSplitShipment.importNode(eleInstrcns, true);
				eleSplitShipment.appendChild(ndInstructions);
				printLogs("Shipment/Instructions is added to document "+SCXmlUtil.getString(eleSplitShipment));
				Element eleShipmentLines=SCXmlUtil.createChild(eleSplitShipment, VSIConstants.ELE_SHIPMENT_LINES);
				Element eleContainerDetails=SCXmlUtil.getChildElement(eleContainer, VSIConstants.ELE_CONTAINER_DETAILS);
				NodeList nlContainerDetail=eleContainerDetails.getElementsByTagName(VSIConstants.ELE_CONTAINER_DETAIL);
				for(int j=0; j<nlContainerDetail.getLength(); j++){
					Element eleContainerDetail=(Element)nlContainerDetail.item(j);
					String strCntnrDtlKey=eleContainerDetail.getAttribute("ContainerDetailsKey");
					printLogs("Container Detail with ContainerDetailsKey "+strCntnrDtlKey+" is being processed");
					String strShpmntLnKey=eleContainerDetail.getAttribute(VSIConstants.ATTR_SHIPMENT_LINE_KEY);
					printLogs("ShipmentLineKey is "+strShpmntLnKey);
					setShpmntLnKey.add(strShpmntLnKey);					
				}
				printLogs("ShipmentLineKey HashSet Contents after processing all container details: "+Arrays.toString(setShpmntLnKey.toArray()));
				for(String shpmntLnKey:setShpmntLnKey){
					printLogs("ShipmentLineKey being processed "+shpmntLnKey);
					Element eleShipmentLn=XMLUtil.getElementByXPath(inXML, "/Shipment/ShipmentLines/ShipmentLine[@ShipmentLineKey='"+shpmntLnKey+"']");
					Node ndShipmentLine=docSplitShipment.importNode(eleShipmentLn, true);
					eleShipmentLines.appendChild(ndShipmentLine);
					printLogs("ShipmentLines/ShipmentLine is added to document "+SCXmlUtil.getString(eleShipmentLines));
				}				
				printLogs("Shipment/ShipmentLines is added to document "+SCXmlUtil.getString(eleSplitShipment));
				Node ndShipmentCharges=docSplitShipment.importNode(eleShpmntChrgs, true);
				eleSplitShipment.appendChild(ndShipmentCharges);
				printLogs("Shipment/ShipmentCharges is added to document "+SCXmlUtil.getString(eleSplitShipment));
	        	Node ndOrder=docSplitShipment.importNode(OrderEle, true);    		
	        	eleSplitShipment.appendChild(ndOrder);
	        	printLogs("Shipment/Order is added to document "+SCXmlUtil.getString(eleSplitShipment));
	        	if(bIsMCLShipment){	        		
	        		eleSplitShipment.setAttribute("MessageType", "SHIP");
	        		printLogs("Shipment Message will be posted to MCL Queue "+SCXmlUtil.getString(docSplitShipment));
	        		VSIUtils.invokeService(env, "VSIMCLFulfillment_Q", docSplitShipment);
	        		printLogs("Shipment Message is posted to MCL Queue");
	        	}else{
	        		printLogs("Shipment Message will be posted to Marketplace DB Component "+SCXmlUtil.getString(docSplitShipment));
	        		VSIUtils.invokeService(env, "VSIMarketplaceFulfillment_DB", docSplitShipment);
	        		printLogs("Shipment Message is posted to Marketplace DB Component");
	        		
	        		printLogs("Shipment Message will be posted to Marketplace Queue "+SCXmlUtil.getString(docSplitShipment));
	        		VSIUtils.invokeService(env, "VSIMarketplaceFulfillment_Q", docSplitShipment);
	        		printLogs("Shipment Message is posted to Marketplace Queue");
	        	}
	        	printLogs("Container with ContainerNo "+strContainerNo+" processing completed");
			}			
		}catch(Exception e){			
			printLogs("Exception in VSIMarketplaceSFSFulfillment Class and splitShipmentContainers Method");
			printLogs("The exception is [ "+ e.getMessage() +" ]");			
		}		
		printLogs("================Exiting VSIMarketplaceSFSFulfillment Class and splitShipmentContainers Method================");
		return inXML;		
	}
	
	private Element copyAttributes(Element fromNode, Element toNode){
		
		NamedNodeMap attributes = fromNode.getAttributes();
	    for (int i = 0; i < attributes.getLength(); i++) 
	    {
	    	Attr attrNode = (Attr) attributes.item(i);
	    	toNode.setAttributeNS(attrNode.getNamespaceURI(), attrNode.getName(), attrNode.getValue());
	    }
		return toNode;
	}
	
	private void printLogs(String mesg) {
		if(log.isDebugEnabled()){
			log.debug(TAG +" : "+mesg);
		}
	}
}
