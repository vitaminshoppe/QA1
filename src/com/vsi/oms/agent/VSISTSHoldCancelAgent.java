package com.vsi.oms.agent;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.vsi.oms.utils.VSIConstants;
import com.vsi.oms.utils.VSIUtils;
import com.yantra.ycp.japi.util.YCPBaseAgent;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfs.japi.YFSEnvironment;

public class VSISTSHoldCancelAgent extends YCPBaseAgent implements VSIConstants 
{
	private static YFCLogCategory log = YFCLogCategory.instance(VSISTSHoldCancelAgent.class.getName());
	//OMS-3406 Changes -- Start
	private static final String TAG = VSISTSHoldCancelAgent.class.getSimpleName();
	//OMS-3406 Changes -- End	
	public final List<Document> getJobs(YFSEnvironment env, Document inDoc)
	{
		//OMS-3406 Changes -- Start			
		printLogs("================Inside VSISTSHoldCancelAgent class and getJobs Method================================");
		printLogs("Printing Input XML: "+SCXmlUtil.getString(inDoc));
		//OMS-3406 Changes -- End
			List<Document> orderList = new ArrayList<Document>();			
			try
			{
				Document getOrderHoldTypeListDoc = SCXmlUtil.createDocument(ELE_ORDER_HOLD_TYPE);
				Element getOrderHoldTypeListEle = getOrderHoldTypeListDoc.getDocumentElement();
				getOrderHoldTypeListEle.setAttribute(ATTR_HOLD_TYPE , STS_CANCEL_HOLD);
				getOrderHoldTypeListEle.setAttribute(ATTR_STATUS , STATUS_CREATE);
				//OMS-3406 Changes -- Start
				printLogs("Input to getOrderHoldTypeList API: "+SCXmlUtil.getString(getOrderHoldTypeListDoc));
				//OMS-3406 Changes -- End
				Document getOrderHoldTypeListOutput = VSIUtils.invokeAPI(env, TEMPLATE_GET_ORDER_HOLD_TYPE_LIST_STS, API_GET_ORDER_HOLD_TYPE_LIST,getOrderHoldTypeListDoc);
				//OMS-3406 Changes -- Start
				printLogs("Output from getOrderHoldTypeList API: "+SCXmlUtil.getString(getOrderHoldTypeListOutput));
				//OMS-3406 Changes -- End
				NodeList orderHoldType = getOrderHoldTypeListOutput.getElementsByTagName(ELE_ORDER_HOLD_TYPE);
				//OMS-3406 Changes -- Start
				int iNoOfOrders=orderHoldType.getLength();
				printLogs("Total number of orders/orderlines with STS hold is "+Integer.toString(iNoOfOrders));
				//OMS-3406 Changes -- End
				for (int i = 0; i < orderHoldType.getLength(); i++) 
				{
					Element eleOrderHoldType = (Element) orderHoldType.item(i);
					//OMS-3406 Changes -- Start
					Document docOrderHoldType = SCXmlUtil.createFromString(SCXmlUtil.getString(eleOrderHoldType));
	            	orderList.add(docOrderHoldType);
	            	//OMS-3406 Changes -- End
				}
				//OMS-3406 Changes -- Start
				printLogs("All the orders/orderlines from getOrderHoldTypeList API output is processed");
				//OMS-3406 Changes -- End
			}			
			catch(Exception e)
			{
				//OMS-3406 Changes -- Start
				printLogs("Exception in VSISTSHoldCancelAgent Class and getJobs Method");
				printLogs("The exception is [ "+ e.getMessage() +" ]");
				//OMS-3406 Changes -- End
			}
			//OMS-3406 Changes -- Start
			printLogs("================Exiting VSISTSHoldCancelAgent class and getJobs Method================================");
			//OMS-3406 Changes -- End
			return orderList;
	}
	@Override
	public void executeJob(YFSEnvironment env, Document docInput) {		//OMS-3406 Change
		
		//OMS-3406 Changes -- Start			
		printLogs("================Inside VSISTSHoldCancelAgent class and executeJob Method================================");
		printLogs("Printing Input XML: "+SCXmlUtil.getString(docInput));
		
		try{
			
			Element eleOrderHoldType = docInput.getDocumentElement();		
		
			Element elemOrder = (Element) eleOrderHoldType.getElementsByTagName(ELE_ORDER).item(0);
			Document changeOrderDoc = SCXmlUtil.createDocument(ELE_ORDER);
			Element eleOrder = changeOrderDoc.getDocumentElement();
			eleOrder.setAttribute(ATTR_ORDER_HEADER_KEY, elemOrder.getAttribute(ATTR_ORDER_HEADER_KEY));
			eleOrder.setAttribute(ATTR_OVERRIDE, FLAG_Y);
			eleOrder.setAttribute(ATTR_MODIFICATION_REASON_CODE, CANCEL_MODIFICATION_CODE);
			eleOrder.setAttribute(ATTR_MODIFICATION_REASON_TEXT, CANCEL_MODIFICATION_CODE);		
			eleOrder.setAttribute(ATTR_ENTERPRISE_CODE, elemOrder.getAttribute(ATTR_ENTERPRISE_CODE));
			eleOrder.setAttribute(ATTR_DOCUMENT_TYPE, elemOrder.getAttribute(ATTR_DOCUMENT_TYPE));
			eleOrder.setAttribute(ATTR_SELECT_METHOD, SELECT_METHOD_WAIT);
			
			Element eleOrderLines = SCXmlUtil.createChild(eleOrder, ELE_ORDER_LINES);
			Element elemOrderLine = SCXmlUtil.createChild(eleOrderLines, ELE_ORDER_LINE);
			elemOrderLine.setAttribute(ATTR_ORDER_LINE_KEY, eleOrderHoldType.getAttribute(ATTR_ORDER_LINE_KEY));
			elemOrderLine.setAttribute(ATTR_ACTION, ACTION_RELEASE_CANCEL);
			
			printLogs("Input to changeOrder API: "+SCXmlUtil.getString(changeOrderDoc));
			VSIUtils.invokeAPI(env, API_CHANGE_ORDER, changeOrderDoc);
			printLogs("changeOrder API was invoked successfully");
			
		}catch(Exception e)
		{
			printLogs("Exception in VSISTSHoldCancelAgent Class and executeJob Method");
			printLogs("The exception is [ "+ e.getMessage() +" ]");			
		}
		
		printLogs("================Exiting VSISTSHoldCancelAgent class and executeJob Method================================");
		//OMS-3406 Changes -- End
	}
	//OMS-3406 Changes -- Start
	private void printLogs(String mesg) {
		if(log.isDebugEnabled()){
			log.debug(TAG +" : "+mesg);
		}
	}
	//OMS-3406 Changes -- End
}
