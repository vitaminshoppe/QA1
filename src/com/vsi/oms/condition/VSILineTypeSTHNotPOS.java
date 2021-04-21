package com.vsi.oms.condition;

import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.vsi.oms.utils.VSIConstants;
import com.yantra.ycp.japi.YCPDynamicConditionEx;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfs.japi.YFSEnvironment;

public class VSILineTypeSTHNotPOS implements YCPDynamicConditionEx {
	
	private YFCLogCategory log = YFCLogCategory.instance(VSILineTypeSTHNotPOS.class);
	private static final String TAG = VSILineTypeSTHNotPOS.class.getSimpleName();
	
	@Override
	public boolean evaluateCondition(YFSEnvironment env,
			String condName, Map mapData, Document inXML) {
		
		printLogs("================Inside VSILineTypeSTHNotPOS Class================================");
		printLogs("VSILineTypeSTHNotPOS.evaluateCondition Input XML: " + SCXmlUtil.getString(inXML));
		
		boolean bLineTypeSTHNotPOS = true;
		
		Element eleOrder=inXML.getDocumentElement();
		String strOrderType=eleOrder.getAttribute(VSIConstants.ATTR_ORDER_TYPE);
		
		if(VSIConstants.ATTR_ORDER_TYPE_POS.equals(strOrderType)){
			
			printLogs("POS Order, so returning False");
			
			bLineTypeSTHNotPOS=false;
			
			return bLineTypeSTHNotPOS;
		}
		
		Element eleOrderLines=SCXmlUtil.getChildElement(eleOrder, VSIConstants.ELE_ORDER_LINES);
		NodeList nlOrderLine=eleOrderLines.getElementsByTagName(VSIConstants.ELE_ORDER_LINE);
		for(int i=0; i<nlOrderLine.getLength(); i++){
			Element eleOrderLine=(Element)nlOrderLine.item(i);
			String strPrimeLineNo=eleOrderLine.getAttribute(VSIConstants.ATTR_PRIME_LINE_NO);
			String strLineType=eleOrderLine.getAttribute(VSIConstants.ATTR_LINE_TYPE);
			if(!VSIConstants.LINETYPE_STH.equals(strLineType)){
				printLogs("OrderLine with PrimeLineNo "+strPrimeLineNo+" is not a STH line, hence returning false");
				bLineTypeSTHNotPOS=false;
				return bLineTypeSTHNotPOS;
			}
		}
		
		printLogs("VSILineTypeSTHNotPOS.evaluateCondition Output: "+bLineTypeSTHNotPOS);
		printLogs("================Exiting VSILineTypeSTHNotPOS Class================================");
		
		return bLineTypeSTHNotPOS;
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
