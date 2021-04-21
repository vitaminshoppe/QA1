package com.vsi.oms.api;

import java.util.ArrayList;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfs.japi.YFSEnvironment;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.vsi.oms.utils.VSIBaseCustomAPI;
import com.vsi.oms.utils.VSIConstants;
import com.vsi.oms.utils.VSIUtils;
import com.vsi.oms.utils.XMLUtil;

public class VSISTSCancellationForWeb extends VSIBaseCustomAPI implements VSIConstants {
	private YFCLogCategory log = YFCLogCategory.instance(VSISTSCancellationForWeb.class);	
	public Document stsOrderLineCancellation(YFSEnvironment env, Document inXml) {
		if(log.isDebugEnabled())
			log.debug("Input for VSISTSCancellationForWeb.stsOrderLineCancellation() => "+XMLUtil.getXMLString(inXml));
		try {
		Element orderElement = inXml.getDocumentElement();
		Document docGetCommonCodeList = XMLUtil.createDocument(ELEMENT_COMMON_CODE);
		Element eleCommonCode = docGetCommonCodeList.getDocumentElement();
		eleCommonCode.setAttribute(ATTR_CODE_TYPE, IS_STS_CANCEL_ENABLED);
		Document docGetCommonCodeListOutput = VSIUtils.invokeAPI(env, API_COMMON_CODE_LIST, docGetCommonCodeList);
		if(docGetCommonCodeListOutput != null) {
			Element elemCommonCode = (Element) docGetCommonCodeListOutput.getElementsByTagName(ELEMENT_COMMON_CODE).item(0);
			String stsCancelEnabled = elemCommonCode.getAttribute(ATTR_CODE_VALUE);
			if(stsCancelEnabled.equalsIgnoreCase(FLAG_Y)) {
				if(orderElement.getAttribute(ATTR_ORDER_TYPE).equalsIgnoreCase(WEB)) {
					ArrayList<String> orderLineKeyList = new ArrayList<>();
					Element nOrderLines = (Element)inXml.getElementsByTagName(ELE_ORDER_LINES).item(0);
					NodeList nOrderLine = nOrderLines.getElementsByTagName(ELE_ORDER_LINE);
					for(int i=0;i<nOrderLine.getLength();i++) {
						Element orderLine = (Element) nOrderLine.item(i);
						if(orderLine.getAttribute(ATTR_LINE_TYPE).equalsIgnoreCase(LINETYPE_STS))
							orderLineKeyList.add(orderLine.getAttribute(ATTR_ORDER_LINE_KEY));
					}
					invokeChangeOrder(env,orderLineKeyList,orderElement.getAttribute(ATTR_ORDER_HEADER_KEY));
				}
			}
		} 
	}
		catch(Exception e)
		{
			e.printStackTrace();
		} 
		return inXml;
		}
	private void invokeChangeOrder(YFSEnvironment env, ArrayList<String> orderLineKeyList,String orderHeaderKey) {
	try {
		Document changeOrderDoc = SCXmlUtil.createDocument(ELE_ORDER);
		Element eleOrder = changeOrderDoc.getDocumentElement();
		eleOrder.setAttribute(ATTR_OVERRIDE, VSIConstants.FLAG_Y);
		eleOrder.setAttribute(ATTR_ORDER_HEADER_KEY, orderHeaderKey);
		Element eleOrderLines = SCXmlUtil.createChild(eleOrder, ELE_ORDER_LINES);
		for (int i=0; i <orderLineKeyList.size();i++)
		{
		Element elemOrderLine = SCXmlUtil.createChild(eleOrderLines, ELE_ORDER_LINE);
		elemOrderLine.setAttribute(ATTR_ORDER_LINE_KEY, orderLineKeyList.get(i));
		elemOrderLine.setAttribute(ATTR_ACTION, ACTION_CAPS_MODIFY);
		Element eleOrderHoldTypes = SCXmlUtil.createChild(elemOrderLine, ELE_ORDER_HOLD_TYPES);
		Element elemOrderHoldType = SCXmlUtil.createChild(eleOrderHoldTypes, ELE_ORDER_HOLD_TYPE);
		elemOrderHoldType.setAttribute(ATTR_HOLD_TYPE, STS_CANCEL_HOLD);
		elemOrderHoldType.setAttribute(ATTR_REASON_TEXT, ATTR_MODIFY_REASON_TEXT_STS);
		elemOrderHoldType.setAttribute(ATTR_STATUS, STATUS_CREATE);
		}
		VSIUtils.invokeAPI(env, API_CHANGE_ORDER, changeOrderDoc);
			}
	catch(Exception e)
		{
		e.printStackTrace();
		} 
	}
}

