package com.vsi.oms.api;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


import org.w3c.dom.NodeList;

import com.sterlingcommerce.baseutil.SCUtil;
import com.sterlingcommerce.tools.datavalidator.XmlUtils;
import com.vsi.oms.utils.VSIConstants;
import com.vsi.oms.utils.VSIUtils;
import com.vsi.oms.utils.XMLUtil;
import com.yantra.interop.japi.YIFApi;
import com.yantra.interop.japi.YIFClientFactory;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;

//this class is used for scheduling STH order
public class VSIScheduleSTHOrder implements VSIConstants {
	private YFCLogCategory log = YFCLogCategory.instance(VSIScheduleBOPUSOrder.class);
	YIFApi api;
	public void vsiScheduleSTH(YFSEnvironment env, Document XML)
			throws Exception {
		log.info("================Inside VSIScheduleSTHOrder================================");
		if (!SCUtil.isVoid(XML)) {
			log.debug("Printing Input XML:" + XmlUtils.getString(XML));		
			try {

				Element rootElement = XML.getDocumentElement();
				String orderHeaderKey = rootElement.getAttribute("OrderHeaderKey");
				
				Document getOrderListInput = XMLUtil.createDocument("Order");
				Element eleOrder = getOrderListInput.getDocumentElement();
				eleOrder.setAttribute("OrderHeaderKey", orderHeaderKey);

				//Calling the getOrderList API 
				log.debug("getOrderList API Input: "+XmlUtils.getString(getOrderListInput));
				api = YIFClientFactory.getInstance().getApi();
				env.setApiTemplate(VSIConstants.API_GET_ORDER_LIST, "global/template/api/VSIGetOrderListSOForHold.xml");
				Document getOrderListOp = api.invoke(env, VSIConstants.API_GET_ORDER_LIST,getOrderListInput);
				if(!YFCCommon.isVoid(getOrderListOp)){
					log.debug("getOrderList API Output: "+XmlUtils.getString(getOrderListOp));
					NodeList nlOrderHoldTypeList = XMLUtil.getNodeListByXpath(
							getOrderListOp, "/OrderList/Order/OrderHoldTypes/OrderHoldType");

					boolean shouldInvokeApi = true;
					for(int i1=0;i1<nlOrderHoldTypeList.getLength();i1++){
						Element eleOrderHoldType = (Element)nlOrderHoldTypeList.item(i1);	
						String status = eleOrderHoldType.getAttribute(VSIConstants.ATTR_STATUS);
						String holdType = eleOrderHoldType.getAttribute(VSIConstants.ATTR_HOLD_TYPE);

						if(("1100".equals(status) && ("VSI_SCHEDULE_HOLD".equals(holdType)))){
							shouldInvokeApi = false;
							break;
						}
					}
					log.debug("Schedule Order API to be called: "+shouldInvokeApi);
					if(shouldInvokeApi){
						Document scheduleOrderInput = XMLUtil.createDocument("ScheduleOrder");
						Element sOrderElement = scheduleOrderInput.getDocumentElement();
						sOrderElement.setAttribute(VSIConstants.ATTR_ORDER_HEADER_KEY, orderHeaderKey);
						sOrderElement.setAttribute("CheckInventory", "Y");
						log.debug("*** NOW PRINTING scheduleOrderInput "+XMLUtil.getXMLString(scheduleOrderInput));
						api = YIFClientFactory.getInstance().getApi();
						api.invoke(env, VSIConstants.API_SCHEDULE_ORDER,scheduleOrderInput);
						log.debug("Schedule Order API invoked successfully");
						//OMS-3068 Changes -- Start
						boolean bIsMixCart=false;
						NodeList nlOrderLine = XMLUtil.getNodeListByXpath(
								getOrderListOp, "/OrderList/Order/OrderLines/OrderLine");
						for(int i=0; i<nlOrderLine.getLength(); i++){
							Element eleOrderLine=(Element)nlOrderLine.item(i);
							String strLineType=eleOrderLine.getAttribute(ATTR_LINE_TYPE);
							log.info("Line Type: "+strLineType);
							if(LINETYPE_PUS.equals(strLineType) || LINETYPE_STS.equals(strLineType)){
								log.info("There is a "+strLineType+" line present, It's a Mixcart scenario");
								bIsMixCart=true;
								break;
							}
						}
						if(!bIsMixCart){
							log.info("Task Queue Entry will be removed");
							Document taskQueueInputDoc = XMLUtil.createDocument("TaskQueue");
	                        Element taskQueueElement = taskQueueInputDoc.getDocumentElement();
	                        taskQueueElement.setAttribute("DataKey", orderHeaderKey);
	                        taskQueueElement.setAttribute("TransactionKey", "SCHEDULE.0001");
	                        taskQueueElement.setAttribute("Operation", "Delete");
	                        taskQueueElement.setAttribute("DataType", "OrderHeaderKey");
	                        log.info("taskQueueInputDoc => "+XMLUtil.getXMLString(taskQueueInputDoc));
	                        VSIUtils.invokeAPI(env, "manageTaskQueue",taskQueueInputDoc);
						}
                        //OMS-3068 Changes -- End
					}
				}
			}
			catch (YFSException e) {
				e.printStackTrace();
			}catch(Exception e){
				e.printStackTrace();

			}
		}
		log.info("================Exiting VSIScheduleSTHOrder================================");
	}
}
