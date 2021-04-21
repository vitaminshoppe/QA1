package com.vsi.oms.condition;

import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.vsi.oms.utils.VSIConstants;
import com.yantra.ycp.japi.YCPDynamicConditionEx;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;

public class VSISingleNodeBOPUSOrSTS implements YCPDynamicConditionEx {
	
	private YFCLogCategory log = YFCLogCategory.instance(VSISingleNodeBOPUSOrSTS.class);
	private static final String TAG = VSISingleNodeBOPUSOrSTS.class.getSimpleName();

	@Override
	public boolean evaluateCondition(YFSEnvironment env,
			String condName, Map mapData, Document inXML) {
		
		printLogs("================Inside VSISingleNodeBOPUSOrSTS Class================================");
		printLogs("VSISingleNodeBOPUSOrSTS.evaluateCondition Input XML: " + SCXmlUtil.getString(inXML));
		
		boolean bSingleNodeBOPUSOrSTS = true;
		
		Element eleOrder=inXML.getDocumentElement();
		String strOrderType=eleOrder.getAttribute(VSIConstants.ATTR_ORDER_TYPE);
		
		if(VSIConstants.ATTR_ORDER_TYPE_POS.equals(strOrderType)){
			
			printLogs("POS Order, so returning False");
			
			bSingleNodeBOPUSOrSTS=false;
			
			return bSingleNodeBOPUSOrSTS;
		}
		
		String strPreviousNode="";
		Element eleOrderLines=SCXmlUtil.getChildElement(eleOrder, VSIConstants.ELE_ORDER_LINES);
		NodeList nlOrderLine=eleOrderLines.getElementsByTagName(VSIConstants.ELE_ORDER_LINE);
		for(int i=0; i<nlOrderLine.getLength(); i++){
			Element eleOrderLine=(Element)nlOrderLine.item(i);
			String strPrimeLineNo=eleOrderLine.getAttribute(VSIConstants.ATTR_PRIME_LINE_NO);
			String strLineType=eleOrderLine.getAttribute(VSIConstants.ATTR_LINE_TYPE);
			if(VSIConstants.LINETYPE_STH.equals(strLineType)){
				printLogs("OrderLine with PrimeLineNo "+strPrimeLineNo+" is a STH line, hence returning false");
				bSingleNodeBOPUSOrSTS=false;
				return bSingleNodeBOPUSOrSTS;
			}
			String strShipNode=eleOrderLine.getAttribute(VSIConstants.ATTR_SHIP_NODE);
			if(YFCCommon.isVoid(strPreviousNode)){
				printLogs("Setting the ShipNode value for the first time as "+strShipNode);
				strPreviousNode=strShipNode;
			}else if(!strPreviousNode.equals(strShipNode)){
				printLogs("OrderLine with PrimeLineNo "+strPrimeLineNo+" with ShipNode "+strShipNode+" differnt from previous ShipNode "+strPreviousNode+", so returning false");
				bSingleNodeBOPUSOrSTS=false;
				return bSingleNodeBOPUSOrSTS;
			}
		}
		
		printLogs("VSISingleNodeBOPUSOrSTS.evaluateCondition Output: "+bSingleNodeBOPUSOrSTS);
		printLogs("================Exiting VSISingleNodeBOPUSOrSTS Class================================");
		
		return bSingleNodeBOPUSOrSTS;
	}
	
	private void printLogs(String mesg) {
		if(log.isDebugEnabled()){
			log.debug(TAG +" : "+mesg);
		}
	}

	@Override
	public void setProperties(Map paramMap) {		
	}

}
