package com.vsi.som.shipment;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.vsi.oms.utils.VSIBaseCustomAPI;
import com.vsi.oms.utils.VSIConstants;
import com.vsi.oms.utils.VSIUtils;
import com.vsi.oms.utils.XMLUtil;
import com.yantra.interop.japi.YIFApi;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;

public class VSISFSChangeReturnNode extends VSIBaseCustomAPI implements VSIConstants {

	
	private YFCLogCategory log = YFCLogCategory.instance(VSISFSChangeReturnNode.class);
	private static final String TAG = VSISFSChangeReturnNode.class.getSimpleName();
	YIFApi api;
	
	public Document changeReturnNode(YFSEnvironment env, Document inXml)
	{

		printLogs("================Inside VSISFSChangeReturnNode Class and changeReturnNode Method================");
		printLogs("VSISFSChangeReturnNode: Printing Input XML to changeReturnNode from  :"+SCXmlUtil.getString(inXml));		
		
		
		Document docChangeOrderIn= SCXmlUtil.createDocument(ELE_ORDER);
		try {
					Element eleOrder= inXml.getDocumentElement();
					Element eleChangeOrderIn= docChangeOrderIn.getDocumentElement();
					Element eleOrderLines= SCXmlUtil.createChild(eleChangeOrderIn, ELE_ORDER_LINES);
					
					if (eleOrder !=null ) {
						
						String strOrderHeaderkey= eleOrder.getAttribute(ATTR_ORDER_HEADER_KEY);
						eleChangeOrderIn.setAttribute(ATTR_ORDER_HEADER_KEY, strOrderHeaderkey);
						eleChangeOrderIn.setAttribute(ATTR_OVERRIDE, "Y");
						NodeList nlOrderLines= eleOrder.getElementsByTagName(ELE_ORDER_LINE);						
						int totalOrderLine= nlOrderLines.getLength();
						
						for (int i = 0; i < totalOrderLine; i++) {
							Element eleOrderLineOut= (Element) nlOrderLines.item(i);
							String strOrderLineKey= eleOrderLineOut.getAttribute(ATTR_ORDER_LINE_KEY);
							String strShipNode= eleOrderLineOut.getAttribute(ATTR_SHIP_NODE);
							//Below condition checks if the order is for SFS or not.
							if(strShipNode.equalsIgnoreCase("9004")|| strShipNode.equalsIgnoreCase("9005")) {
								continue;
							}
							Element eleOrderLineIn= SCXmlUtil.createChild(eleOrderLines, ELE_ORDER_LINE);
							String strReturnToShipNode="";
							if (!strShipNode.isEmpty()) {
								Document docGetNodeTransferScheduleListIn = XMLUtil.createDocument(VSIConstants.ELE_NODE_TFR_SCHEDULE);
								Element eleGetNodeTransferScheduleListIn = docGetNodeTransferScheduleListIn.getDocumentElement();
								eleGetNodeTransferScheduleListIn.setAttribute(VSIConstants.ATTR_TO_NODE, strShipNode);

								Document docGetNodeTransferScheduleListOut = VSIUtils.invokeAPI(env,"global/template/api/VSISFS_getNodeTransferScheduleList.xml",VSIConstants.API_GET_NODE_TFR_SCHEDULE_LIST, docGetNodeTransferScheduleListIn);

								Element eleGetNodeTransferScheduleListOut = docGetNodeTransferScheduleListOut.getDocumentElement();
								Element eleNodeTransferSchedule=(Element)eleGetNodeTransferScheduleListOut.getElementsByTagName(VSIConstants.ELE_NODE_TFR_SCHEDULE).item(0);

								if (eleNodeTransferSchedule != null) {
									strReturnToShipNode= eleNodeTransferSchedule.getAttribute(VSIConstants.ATTR_FROM_NODE);
								}
								else {
									strReturnToShipNode ="9004";
									printLogs("VSISFSChangeReturnNode: No Transfer node found. Defaulting to 9004 ");		
									//log.info("VSISFSChangeReturnNode: No Transfer node found. Defaulting to 9004 ");	
									
								}
							}
							else{
								printLogs("VSISFSChangeReturnNode: No Ship Node found for the order Line. Defaulting to 9004 ");		
								//log.info("VSISFSChangeReturnNode: No Ship Node found for the order Line. Defaulting to 9004 ");
								
								strReturnToShipNode="9004";
							}
							eleOrderLineIn.setAttribute(ATTR_ORDER_HEADER_KEY, strOrderHeaderkey);
							eleOrderLineIn.setAttribute(ATTR_ORDER_LINE_KEY, strOrderLineKey);
							eleOrderLineIn.setAttribute(ATTR_SHIP_NODE, strReturnToShipNode);
							eleOrderLineIn.setAttribute(ATTR_OVERRIDE, "Y");
							if (!strReturnToShipNode.isEmpty()) {								
							eleOrderLineOut.setAttribute(ATTR_SHIP_NODE, strReturnToShipNode);
							}
							
						}

						printLogs("VSISFSChangeReturnNode: Printing Input XML to changeReturnNode   :"+SCXmlUtil.getString(docChangeOrderIn));		
						//log.info("VSISFSChangeReturnNode: Printing Input XML to changeReturnNode   :"+SCXmlUtil.getString(docChangeOrderIn));
						
						NodeList nlOrderLinesIn= docChangeOrderIn.getElementsByTagName(ELE_ORDER_LINE);						
						int totalOrderLineIn= nlOrderLinesIn.getLength();
						
						if (totalOrderLineIn>0) {
							Document docChangeOrderOut = VSIUtils.invokeAPI(env,VSIConstants.API_CHANGE_ORDER, docChangeOrderIn);
							printLogs("VSISFSChangeReturnNode: Output from Change order: "+SCXmlUtil.getString(docChangeOrderOut));		
							//log.info("VSISFSChangeReturnNode: Output from Change order: "+SCXmlUtil.getString(docChangeOrderOut));
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
		return inXml;
	}

	private void printLogs(String mesg) {
		if(log.isDebugEnabled()){
			log.debug(TAG +" : "+mesg);
		}
	}
}
