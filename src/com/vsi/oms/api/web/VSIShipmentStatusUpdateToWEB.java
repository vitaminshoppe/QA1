package com.vsi.oms.api.web;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Properties;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.vsi.oms.utils.VSIConstants;
import com.vsi.oms.utils.VSIUtils;
import com.vsi.oms.utils.XMLUtil;
import com.yantra.interop.japi.YIFApi;
import com.yantra.interop.japi.YIFClientFactory;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;

public class VSIShipmentStatusUpdateToWEB {
	private YFCLogCategory log = YFCLogCategory.instance(VSIShipmentStatusUpdateToWEB.class);
	//OMS-2958 Changes -- Start
	private static final String TAG = VSIShipmentStatusUpdateToWEB.class.getSimpleName();
	//OMS-2958 Changes -- End
	YIFApi api;
	private Properties props;
	//YIFApi newApi;
	//YIFApi shipApi;
	/**
	 * 
	 * @param env
	 * 	Required. Environment handle returned by the createEnvironment
	 * 	API. This is required for the user exit to either raise errors
	 * 	or access environment information.
	 * @param inXML
	 * 	Required. This contains the input message from ESB.
	 * @return
	 * 
	 * @throws Exception
	 *   This exception is thrown whenever system errors that the
	 *   application cannot handle are encountered. The transaction is
	 *   aborted and changes are rolled back.
	 */
	public void shipUpdateToWEB(YFSEnvironment env, Document inXML)
	throws Exception {
		
		//OMS-2958 Changes -- Start
		printLogs("================Inside VSIShipmentStatusUpdateToWEB Class and shipUpdateToWEB Method================");
		printLogs("Printing Input XML :"+SCXmlUtil.getString(inXML));
		//OMS-2958 Changes -- End
					
			Element rootElement = inXML.getDocumentElement();
            String orderHeaderKeyStr=rootElement.getAttribute("OrderHeaderKey");
			
			
			Document getOrderListDoc=XMLUtil.createDocument("Order");
			Element getOrderListDocEle = getOrderListDoc.getDocumentElement();
			getOrderListDocEle.setAttribute("OrderHeaderKey",orderHeaderKeyStr);
			//OMS-2958 Changes -- Start
			printLogs("Input to getOrderList API: "+SCXmlUtil.getString(getOrderListDoc));
			//OMS-2958 Changes -- End
			
			api = YIFClientFactory.getInstance().getApi();
			env.setApiTemplate(VSIConstants.API_GET_ORDER_LIST, "global/template/api/VSIGetOrderListWEB.xml");
			Document outDoc  = api.invoke(env, VSIConstants.API_GET_ORDER_LIST,getOrderListDoc);
			//OMS-2958 Changes -- Start
			printLogs("Output from getOrderList API: "+SCXmlUtil.getString(outDoc));
			//OMS-2958 Changes -- End
			env.clearApiTemplate(VSIConstants.API_GET_ORDER_LIST);
			Element outDocOrderElement = (Element)outDoc.getElementsByTagName("Order").item(0);
			
			//OMS-2958 Changes -- Start
			String strOrderNo=outDocOrderElement.getAttribute("OrderNo");
			String strEntryType=outDocOrderElement.getAttribute("EntryType");
			String strATGShipFlag=null;
			ArrayList<Element> listATGFlag;
			listATGFlag = VSIUtils.getCommonCodeList(env, "MC_ATG_SHIP_UPDATE", "Ship_Update_Required", VSIConstants.ATTR_DEFAULT);
			if(!listATGFlag.isEmpty()){
				Element eleCommonCode=listATGFlag.get(0);
				strATGShipFlag=eleCommonCode.getAttribute(VSIConstants.ATTR_CODE_SHORT_DESCRIPTION);
			}
			if(!YFCCommon.isVoid(strATGShipFlag) && VSIConstants.FLAG_Y.equals(strATGShipFlag)){
				printLogs("ATG Ship Update Flag is Y, hence new flow will be invoked");
				if(strEntryType.equalsIgnoreCase("WEB")){
					String strMinOrdSts=outDocOrderElement.getAttribute(VSIConstants.ATTR_MIN_ORDER_STATUS);
					if(("3700").equalsIgnoreCase(strMinOrdSts)||Double.parseDouble(strMinOrdSts)>3700){
						printLogs("Order is fully shipped, ATG call will be invoked");
						Calendar calendar = Calendar.getInstance();
						String statusDate = new SimpleDateFormat("yyyyMMddHHmmss").format(calendar.getTime());
						
						Document OrderStatusToWEB = XMLUtil.createDocument("Order");
						Element OrderStatusToWEBEle = OrderStatusToWEB.getDocumentElement();
						OrderStatusToWEBEle.setAttribute("OrderNo",strOrderNo);
						OrderStatusToWEBEle.setAttribute("TimeStamp",statusDate);
						OrderStatusToWEBEle.setAttribute("Status","Complete");
						
						printLogs("Input to VSIWEBStatusUpdate service: "+SCXmlUtil.getString(OrderStatusToWEB));
						api.executeFlow(env,"VSIWEBStatusUpdate",OrderStatusToWEB);
						printLogs("VSIWEBStatusUpdate service was invoked successfully");
						
					}else{
						printLogs("Order is not fully shipped yet, ATG call will not be invoked");
					}
				}else{
					printLogs("Its not a Web order, hence exiting the class");
				}
			}else{
			//OMS-2958 Changes -- End
							
				String strStatus=outDocOrderElement.getAttribute("Status");				
					     
				if(strEntryType.equalsIgnoreCase("WEB")){
				Calendar calendar = Calendar.getInstance();
				String statusDate = new SimpleDateFormat("yyyyMMddHHmmss").format(calendar.getTime());
				
				Document OrderStatusToWEB = XMLUtil.createDocument("Order");
				Element OrderStatusToWEBEle = OrderStatusToWEB.getDocumentElement();
				OrderStatusToWEBEle.setAttribute("OrderNo",strOrderNo);
				OrderStatusToWEBEle.setAttribute("TimeStamp",statusDate);
				if(strStatus.equalsIgnoreCase("Shipped")){
					OrderStatusToWEBEle.setAttribute("Status","Complete");
	
				api.executeFlow(env,"VSIWEBStatusUpdate",OrderStatusToWEB);
				
				}
				else{
					String strMinStatus=outDocOrderElement.getAttribute("MinOrderStatus");
					//double dbMinStatus=Double.parseDouble(strMinStatus);
					if(strMinStatus.equalsIgnoreCase("3200.500") || (strMinStatus.equalsIgnoreCase(VSIConstants.STATUS_SOM_READY_FOR_PICKUP))){
						strStatus="Ready For Pickup";
					}
					else if(strMinStatus.equalsIgnoreCase("2160.200")||strMinStatus.equalsIgnoreCase("2160.400")
							||strMinStatus.equalsIgnoreCase("3200.401")||strMinStatus.equalsIgnoreCase("3200.411")){
						strStatus="In Transit to Store";	
					}
					else if (strMinStatus.equalsIgnoreCase("1500")||strMinStatus.equalsIgnoreCase("1100")||
							strMinStatus.equalsIgnoreCase("2160.100")||strMinStatus.equalsIgnoreCase("2160")
							||strMinStatus.equalsIgnoreCase("3200")||strMinStatus.equalsIgnoreCase("1500.100")
							||strMinStatus.equalsIgnoreCase("1500.200") || (strMinStatus.equalsIgnoreCase(VSIConstants.STATUS_SOM_READY_FOR_PICKUP_PROCESS))){
						strStatus="Processing";
					}
					OrderStatusToWEBEle.setAttribute("Status",strStatus);
					api.executeFlow(env,"VSIWEBStatusUpdate",OrderStatusToWEB);
	
				}
				
		}
	//OMS-2958 Changes -- Start
	}
	printLogs("================Exiting VSIShipmentStatusUpdateToWEB Class and shipUpdateToWEB Method================");
	//OMS-2958 Changes -- End
	}
	//OMS-2958 Changes -- Start
	private void printLogs(String mesg) {
		if(log.isDebugEnabled()){
			log.debug(TAG +" : "+mesg);
		}
	}
	//OMS-2958 Changes -- End
}
