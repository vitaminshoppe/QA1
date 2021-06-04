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

public class VSIWebCancelUpdate {
	private YFCLogCategory log = YFCLogCategory.instance(VSIWebCancelUpdate.class);
	YIFApi api;
	private Properties props;
	//OMS-2958 Changes -- Start
	private static final String TAG = VSIWebCancelUpdate.class.getSimpleName();
	//OMS-2958 Changes -- End
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
	public void cancelUpdateToWEB(YFSEnvironment env, Document inXML)
	throws Exception {
		
		//OMS-2958 Changes -- Start
		printLogs("================Inside VSIWebCancelUpdate Class and cancelUpdateToWEB Method================");
		printLogs("Printing Input XML :"+SCXmlUtil.getString(inXML));
		//OMS-2958 Changes -- End

					
			Element rootElement = inXML.getDocumentElement();
			
			
			String strStatus=rootElement.getAttribute("Status");
			String strOrderNo=rootElement.getAttribute("OrderNo");
			api = YIFClientFactory.getInstance().getApi();
			
			//OMS-2958 Changes -- Start			
			String strATGCancelFlag=null;
			ArrayList<Element> listATGFlag;
			listATGFlag = VSIUtils.getCommonCodeList(env, "MC_ATG_CANCEL_UPDATE", "Cancel_Update_Required", VSIConstants.ATTR_DEFAULT);
			if(!listATGFlag.isEmpty()){
				Element eleCommonCode=listATGFlag.get(0);
				strATGCancelFlag=eleCommonCode.getAttribute(VSIConstants.ATTR_CODE_SHORT_DESCRIPTION);
			}
			if(!YFCCommon.isVoid(strATGCancelFlag) && VSIConstants.FLAG_Y.equals(strATGCancelFlag)){
				printLogs("ATG Cancel Update Flag is Y, hence new flow will be invoked");
				String strMinOrdSts=rootElement.getAttribute(VSIConstants.ATTR_MIN_ORDER_STATUS);
				String strMaxOrdSts=rootElement.getAttribute(VSIConstants.ATTR_MAX_ORDER_STATUS);
				if(VSIConstants.STATUS_CANCEL.equals(strMinOrdSts) && VSIConstants.STATUS_CANCEL.equals(strMaxOrdSts)){
					printLogs("Order is fully Cancelled, ATG call will be invoked");
					Calendar calendar = Calendar.getInstance();
					String statusDate = new SimpleDateFormat("yyyyMMddHHmmss").format(calendar.getTime());
						
					Document OrderStatusToWEB = XMLUtil.createDocument("Order");
					Element OrderStatusToWEBEle = OrderStatusToWEB.getDocumentElement();
					OrderStatusToWEBEle.setAttribute("OrderNo",strOrderNo);
					OrderStatusToWEBEle.setAttribute("TimeStamp",statusDate);
					OrderStatusToWEBEle.setAttribute("Status","Cancelled");
						
					printLogs("Input to VSIWEBStatusUpdate service: "+SCXmlUtil.getString(OrderStatusToWEB));
					api.executeFlow(env,"VSIWEBStatusUpdate",OrderStatusToWEB);
					printLogs("VSIWEBStatusUpdate service was invoked successfully");
						
				}else{
					printLogs("Order is not fully cancelled, ATG call will not be invoked");
				}
			}else{
			//OMS-2958 Changes -- End			
			
				Calendar calendar = Calendar.getInstance();
				String statusDate = new SimpleDateFormat("yyyyMMddHHmmss").format(calendar.getTime());
				
				Document OrderStatusToWEB = XMLUtil.createDocument("Order");
				Element OrderStatusToWEBEle = OrderStatusToWEB.getDocumentElement();
				OrderStatusToWEBEle.setAttribute("OrderNo",strOrderNo);
				OrderStatusToWEBEle.setAttribute("TimeStamp",statusDate);
				if(strStatus.equalsIgnoreCase("Cancelled")){
					OrderStatusToWEBEle.setAttribute("Status",strStatus);
				   api.executeFlow(env,"VSIWEBStatusUpdate",OrderStatusToWEB);
				
				}
				else{
					String webStatus=null;
					String strMaxStatus=rootElement.getAttribute("MaxOrderStatus");
					String strMinStatus=rootElement.getAttribute("MinOrderStatus");
					//double dbMinStatus=Double.parseDouble(strMinStatus);
					if(strMaxStatus.equalsIgnoreCase("3200.500")){
						strStatus="Ready For Pickup";
					}
					else if(strMaxStatus.equalsIgnoreCase("2160.200")||strMaxStatus.equalsIgnoreCase("2160.400")
							||strMaxStatus.equalsIgnoreCase("3200.401")||strMaxStatus.equalsIgnoreCase("3200.411")){
						strStatus="In Transit to Store";	
					}
					else if (strMaxStatus.equalsIgnoreCase("1500")||strMaxStatus.equalsIgnoreCase("1100")||
							strMaxStatus.equalsIgnoreCase("2160.100")||strMaxStatus.equalsIgnoreCase("2160")
							||strMaxStatus.equalsIgnoreCase("3200")||strMaxStatus.equalsIgnoreCase("1500.100")
							||strMaxStatus.equalsIgnoreCase("1500.200")){
						strStatus="Processing";
					}
					//OrderStatusToWEBEle.setAttribute("Status",strStatus);
					if(strMinStatus.equalsIgnoreCase(strMaxStatus)){
						webStatus=strStatus;
					}
					else if (strStatus.equalsIgnoreCase("Processing")){
						
						webStatus=strStatus;
					}
					else{
						webStatus="Partially "+strStatus;
					}
					OrderStatusToWEBEle.setAttribute("Status",webStatus);
	
					api.executeFlow(env,"VSIWEBStatusUpdate",OrderStatusToWEB);
	
				}
			//OMS-2958 Changes -- Start
			}
			printLogs("================Exiting VSIWebCancelUpdate Class and cancelUpdateToWEB Method================");
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
