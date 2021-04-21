package com.vsi.som.shipment;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.vsi.oms.utils.VSIBaseCustomAPI;
import com.vsi.oms.utils.VSIConstants;
import com.yantra.interop.japi.YIFApi;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;

public class VSISFSGenerateShipConfirmForATG extends VSIBaseCustomAPI implements VSIConstants {
	
	private YFCLogCategory log = YFCLogCategory.instance(VSISFSGenerateShipConfirmForATG.class);
	private static final String TAG = VSISFSGenerateShipConfirmForATG.class.getSimpleName();
	YIFApi api;
	
	public Document createShipConfirmForATG(YFSEnvironment env, Document inXml)
	{
		Document responseDoc= SCXmlUtil.createDocument(ELE_SHIPMENT_LIST);
		printLogs("================Inside VSISFSGenerateShipConfirmForATG Class and createShipConfirmForATG Method================");
		printLogs("VSISFSGenerateShipConfirmForATG: Printing Input XML to createShipConfirmForATG from  :"+SCXmlUtil.getString(inXml));		
		log.info("VSISFSGenerateShipConfirmForATG: Printing Input XML to createShipConfirmForATG from  :"+SCXmlUtil.getString(inXml));
		
		
		try {

					Element eleShipmentIn= inXml.getDocumentElement();
					Element eleResponse= responseDoc.getDocumentElement();
					Element eleShipmentListResp = responseDoc.getDocumentElement();
					Element eleShipmentResp= SCXmlUtil.createChild(eleShipmentListResp, ELE_SHIPMENT);
					

					String strActualShipDate= eleShipmentIn.getAttribute("ActualShipmentDate");
					String strCarrierServiceCode= eleShipmentIn.getAttribute("CarrierServiceCode");
					String strDocumentType= eleShipmentIn.getAttribute("DocumentType");
					String strOrderNo= eleShipmentIn.getAttribute("OrderNo");
					String strPartialShipConfirmStatus= "UG";
					String strSCAC= eleShipmentIn.getAttribute("SCAC");
					String strShipNode= eleShipmentIn.getAttribute("ShipNode");
					String strShipmentDate= eleShipmentIn.getAttribute("ShipDate");
					String strStatus= eleShipmentIn.getAttribute("Status");
					if (strStatus.equalsIgnoreCase("1400")) {
						strStatus="SHIPPED";
					}
					//String strReleaseNo=eleShipmentIn.getAttribute("ReleaseNo");
					String strDistributionOrderId= strOrderNo;
					
					eleShipmentResp.setAttribute("ActualShipDate", strActualShipDate);
					eleShipmentResp.setAttribute("CarrierServiceCode", strCarrierServiceCode);
					eleShipmentResp.setAttribute("DistributionOrderId", strDistributionOrderId);
					eleShipmentResp.setAttribute("DocumentType", strDocumentType);
					eleShipmentResp.setAttribute("OrderNo", strOrderNo);
					eleShipmentResp.setAttribute("PartialShipConfirmStatus", strPartialShipConfirmStatus);
					eleShipmentResp.setAttribute("SCAC", strSCAC);
					eleShipmentResp.setAttribute("ShipNode", strShipNode);
					eleShipmentResp.setAttribute("ShipmentDate", strShipmentDate);
					eleShipmentResp.setAttribute("Status", strStatus);
					
					Element eleOrderResp= SCXmlUtil.createChild(eleResponse, "Order");
					Element eleOrderLinesResp= SCXmlUtil.createChild(eleOrderResp, "OrderLines");
					
					eleOrderResp.setAttribute("DistributionOrderId", strDistributionOrderId);
					eleOrderResp.setAttribute("DocumentType", strDocumentType);
					eleOrderResp.setAttribute("OrderNo", strOrderNo);
					
			
					
					Element eleContainersResp= SCXmlUtil.createChild(eleShipmentResp, "Containers");
					Element eleContainers= SCXmlUtil.getChildElement(eleShipmentIn, "Containers");
					
					NodeList nlEleContainer= eleContainers.getElementsByTagName("Container");
					int totalEleContainer = nlEleContainer.getLength();
					if(totalEleContainer>0) {
					for (int j = 0; j < totalEleContainer; j++) {						
						Element eleContainerResp= SCXmlUtil.createChild(eleContainersResp, "Container");
						Element eleContainer= (Element) nlEleContainer.item(j);
						String strContainerGrossWeight= eleContainer.getAttribute("ContainerGrossWeight");
						String strContainerGrossWeightUOM= eleContainer.getAttribute("ContainerGrossWeightUOM");
						String strContainerNo= eleContainer.getAttribute("ContainerNo");
						String strTrackingNo= eleContainer.getAttribute("TrackingNo");
						eleContainerResp.setAttribute("ContainerGrossWeight", strContainerGrossWeight);
						eleContainerResp.setAttribute("ContainerGrossWeightUOM", strContainerGrossWeightUOM);
						eleContainerResp.setAttribute("ContainerNo", strContainerNo);
						eleContainerResp.setAttribute("TrackingNo", strTrackingNo);

						Element eleContainerDetailsResp= SCXmlUtil.createChild(eleContainerResp, "ContainerDetails");					
						Element eleContainerDetails= SCXmlUtil.getChildElement(eleContainer, "ContainerDetails");
						NodeList nlEleContainerDetail= eleContainerDetails.getElementsByTagName("ContainerDetail");
						
						int totalEleContainerDetail = nlEleContainerDetail.getLength();
						if(totalEleContainerDetail>0) {
						for (int k = 0; k < totalEleContainerDetail; k++) {
							Element eleContainerDetailIn= (Element) nlEleContainerDetail.item(k);
							Element eleContainerDetailResp= SCXmlUtil.createChild(eleContainerDetailsResp, "ContainerDetail");
							String strQuantity= eleContainerDetailIn.getAttribute("Quantity");
							eleContainerDetailResp.setAttribute("Quantity", strQuantity);
							
							Element eleShipmentLineResp= SCXmlUtil.createChild(eleContainerDetailResp, "ShipmentLine");
							Element eleShipmentLineIn= SCXmlUtil.getChildElement(eleContainerDetailIn, "ShipmentLine");
							if(eleShipmentLineIn!=null) {
							String strOrderNumber= eleShipmentLineIn.getAttribute("OrderNo");
							String strDocType= eleShipmentLineIn.getAttribute("DocumentType");
							String strUnitOfMeasure= eleShipmentLineIn.getAttribute("UnitOfMeasure");
							String strSubLineNo= eleShipmentLineIn.getAttribute("SubLineNo");
							String strProductClass= eleShipmentLineIn.getAttribute("ProductClass");
							String strPrimeLineNo= eleShipmentLineIn.getAttribute("PrimeLineNo");
							String strItemID= eleShipmentLineIn.getAttribute("ItemID");
							eleShipmentLineResp.setAttribute("OrderNo", strOrderNumber);
							eleShipmentLineResp.setAttribute("DocumentType", strDocType);
							eleShipmentLineResp.setAttribute("UnitOfMeasure", strUnitOfMeasure);
							eleShipmentLineResp.setAttribute("SubLineNo", strSubLineNo);
							//OMS-2756-start
							if(strProductClass.isEmpty()) {
							strProductClass="GOOD";
							}
							//OMS-2756-end
							eleShipmentLineResp.setAttribute("ProductClass", strProductClass);
							eleShipmentLineResp.setAttribute("PrimeLineNo", strPrimeLineNo);
							eleShipmentLineResp.setAttribute("ItemID", strItemID);
							}

						}
						}
					}
					}
					Element eleShipmentLinesResp= SCXmlUtil.createChild(eleShipmentResp, "ShipmentLines");
					Element eleShipmentLinesIn= SCXmlUtil.getChildElement(eleShipmentIn, "ShipmentLines");
					NodeList nlShipmentLinesIn= eleShipmentLinesIn.getElementsByTagName("ShipmentLine");
					
					
					int totalShipmentLines = nlShipmentLinesIn.getLength();
					if(totalShipmentLines>0) {
						for (int l = 0; l < nlShipmentLinesIn.getLength(); l++) {
						
						Element eleShipmentLineResp= SCXmlUtil.createChild(eleShipmentLinesResp, "ShipmentLine");
						Element eleShipmentLineIn= (Element) nlShipmentLinesIn.item(l);					

						String strOrderNumber= eleShipmentLineIn.getAttribute("OrderNo");
						String strDocType= eleShipmentLineIn.getAttribute("DocumentType");
						String strUnitOfMeasure= eleShipmentLineIn.getAttribute("UnitOfMeasure");
						String strSubLineNo= eleShipmentLineIn.getAttribute("SubLineNo");
						String strProductClass= eleShipmentLineIn.getAttribute("ProductClass");
						String strPrimeLineNo= eleShipmentLineIn.getAttribute("PrimeLineNo");
						String strItemID= eleShipmentLineIn.getAttribute("ItemID");
						String strQuantity= eleShipmentLineIn.getAttribute("Quantity");
						
						eleShipmentLineResp.setAttribute("OrderNo", strOrderNumber);
						eleShipmentLineResp.setAttribute("DocumentType", strDocType);
						eleShipmentLineResp.setAttribute("UnitOfMeasure", strUnitOfMeasure);
						eleShipmentLineResp.setAttribute("SubLineNo", strSubLineNo);
						//OMS-2756-start
						if(strProductClass.isEmpty()) {
							strProductClass="GOOD";
						}
						//OMS-2756-end
						eleShipmentLineResp.setAttribute("ProductClass", strProductClass);
						eleShipmentLineResp.setAttribute("PrimeLineNo", strPrimeLineNo);
						eleShipmentLineResp.setAttribute("ItemID", strItemID);
						eleShipmentLineResp.setAttribute("Quantity", strQuantity);
						

						Element eleOrderLineResp= SCXmlUtil.createChild(eleOrderLinesResp, "OrderLine");
						String strOriginalReleasedQty= "0";
						String strShippedQty= "0";
						String strUserCanceledQuantity= "0";
						//OMS-2756- START
						strOriginalReleasedQty=eleShipmentLineIn.getAttribute("OriginalQuantity");
						strShippedQty=eleShipmentLineIn.getAttribute("Quantity");
						//OMS-2756- END
						
						
						
						int ShippedQty= Integer.parseInt(strOriginalReleasedQty);
						int  OriginalReleasedQty= Integer.parseInt(strShippedQty);
						int UserCanceledQuantity = ShippedQty-OriginalReleasedQty;						
						strUserCanceledQuantity= String.valueOf(UserCanceledQuantity);
						
						eleOrderLineResp.setAttribute("ItemID", strItemID);
						eleOrderLineResp.setAttribute("OriginalReleasedQty", strOriginalReleasedQty);
						eleOrderLineResp.setAttribute("PrimeLineNo", strPrimeLineNo);
						eleOrderLineResp.setAttribute("ShippedQty", strShippedQty);
						eleOrderLineResp.setAttribute("UserCanceledQuantity", strUserCanceledQuantity);

					}
					}

		}
	catch (YFSException e) {
		e.printStackTrace();
		throw new YFSException();
	} catch (Exception e){
		e.printStackTrace();
		throw new YFSException();
	}
		

		printLogs("VSISFSGenerateShipConfirmForATG: Printing Response Document XML :"+SCXmlUtil.getString(responseDoc));		
		log.info("VSISFSGenerateShipConfirmForATG: Printing Response Document XML :"+SCXmlUtil.getString(responseDoc));
		
		return responseDoc;

	}

	private void printLogs(String mesg) {
		if(log.isDebugEnabled()){
			log.debug(TAG +" : "+mesg);
		}
	}
	
}
