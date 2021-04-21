package com.vsi.oms.agent;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.vsi.oms.utils.VSIConstants;
import com.vsi.oms.utils.VSIUtils;
import com.vsi.oms.utils.XMLUtil;
import com.yantra.ycp.japi.util.YCPBaseAgent;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfs.japi.YFSEnvironment;

public class VSISTSHoldCancelAgent extends YCPBaseAgent implements VSIConstants 
{
	private static YFCLogCategory log = YFCLogCategory.instance(VSISTSHoldCancelAgent.class.getName());
	public final List<Document> getJobs(YFSEnvironment env, Document inDoc)
	{
			if(log.isDebugEnabled()){
				log.debug("VSISTSHoldCancelAgent.getJobs() method Begins => ");
			}
			List<Document> orderList = new ArrayList<Document>();			
			try
			{	
				Document getOrderHoldTypeListDoc = SCXmlUtil.createDocument(ELE_ORDER_HOLD_TYPE);
				Element getOrderHoldTypeListEle = getOrderHoldTypeListDoc.getDocumentElement();
				getOrderHoldTypeListEle.setAttribute(ATTR_HOLD_TYPE , STS_CANCEL_HOLD);
				getOrderHoldTypeListEle.setAttribute(ATTR_STATUS , STATUS_CREATE);
				Document getOrderHoldTypeListOutput = VSIUtils.invokeAPI(env, TEMPLATE_GET_ORDER_HOLD_TYPE_LIST_STS, API_GET_ORDER_HOLD_TYPE_LIST,getOrderHoldTypeListDoc);
			NodeList orderHoldType = getOrderHoldTypeListOutput.getElementsByTagName(ELE_ORDER_HOLD_TYPE);
			for (int i = 0; i < orderHoldType.getLength(); i++) 
			{
				Element eleOrderHoldType = (Element) orderHoldType.item(i);
					Element elemOrder = (Element) eleOrderHoldType.getElementsByTagName(ELE_ORDER).item(0);
					Document changeOrderDoc = SCXmlUtil.createDocument(ELE_ORDER);
					Element eleOrder = changeOrderDoc.getDocumentElement();
					eleOrder.setAttribute(ATTR_OVERRIDE, FLAG_Y);
					eleOrder.setAttribute(ATTR_ORDER_HEADER_KEY, elemOrder.getAttribute(ATTR_ORDER_HEADER_KEY));
					eleOrder.setAttribute(ATTR_ENTERPRISE_CODE, elemOrder.getAttribute(ATTR_ENTERPRISE_CODE));
					eleOrder.setAttribute(ATTR_DOCUMENT_TYPE, elemOrder.getAttribute(ATTR_DOCUMENT_TYPE));
					Element eleOrderLines = SCXmlUtil.createChild(eleOrder, ELE_ORDER_LINES);
					Element elemOrderLine = SCXmlUtil.createChild(eleOrderLines, ELE_ORDER_LINE);
					elemOrderLine.setAttribute(ATTR_ORDER_LINE_KEY, eleOrderHoldType.getAttribute(ATTR_ORDER_LINE_KEY));
					elemOrderLine.setAttribute(ATTR_ACTION, ACTION_RELEASE_CANCEL);
					eleOrder.setAttribute(ATTR_MODIFICATION_REASON_CODE, CANCEL_MODIFICATION_CODE);
					eleOrder.setAttribute(ATTR_MODIFICATION_REASON_TEXT, CANCEL_MODIFICATION_CODE);
				    if(log.isDebugEnabled())
				    	log.debug("VSISTSHoldCancelAgent - Input for changeOrderDoc => "+XMLUtil.getXMLString(changeOrderDoc));
					VSIUtils.invokeAPI(env, API_CHANGE_ORDER, changeOrderDoc);
				}
}			
			catch(Exception e)
			{
				e.printStackTrace();
			}
			return orderList;
	}
	@Override
	public void executeJob(YFSEnvironment arg0, Document arg1) throws Exception {
	}
}
