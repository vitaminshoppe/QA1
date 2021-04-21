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

public class VSIMixedCartCondition implements YCPDynamicConditionEx {
	
	private YFCLogCategory log = YFCLogCategory.instance(VSIMixedCartCondition.class);
	private static final String TAG = VSIMixedCartCondition.class.getSimpleName();

	@Override
	public boolean evaluateCondition(YFSEnvironment env,
			String condName, Map mapData, Document inXML) {
		
		printLogs("================Inside VSIMixedCartCondition Class================================");
		printLogs("VSIMixedCartCondition.evaluateCondition Input XML: " + SCXmlUtil.getString(inXML));
		
		boolean bMixedCart = false;
		boolean bSTHLinePresent=false;
		boolean bBOPUSOrSTSPresent=false;
		boolean bMultiNodePresent=false;
		
		Element eleOrder=inXML.getDocumentElement();
		String strOrderType=eleOrder.getAttribute(VSIConstants.ATTR_ORDER_TYPE);
		
		if(VSIConstants.ATTR_ORDER_TYPE_POS.equals(strOrderType)){
			
			printLogs("POS Order, so returning False");
			
			bMixedCart=false;
			
			return bMixedCart;
		}
		
		String strPreviousNode="";
		Element eleOrderLines=SCXmlUtil.getChildElement(eleOrder, VSIConstants.ELE_ORDER_LINES);
		NodeList nlOrderLine=eleOrderLines.getElementsByTagName(VSIConstants.ELE_ORDER_LINE);
		int iOrdLneLnth=nlOrderLine.getLength();
		if(iOrdLneLnth<2){
			printLogs("There are not enough lines on the order for a Mixed Cart, so returning False");
			
			bMixedCart=false;
			
			return bMixedCart;
		}
		
		for(int i=0; i<nlOrderLine.getLength(); i++){			
			Element eleOrderLine=(Element)nlOrderLine.item(i);
			String strPrimeLineNo=eleOrderLine.getAttribute(VSIConstants.ATTR_PRIME_LINE_NO);
			String strLineType=eleOrderLine.getAttribute(VSIConstants.ATTR_LINE_TYPE);
			if(VSIConstants.LINETYPE_STH.equals(strLineType)){
				bSTHLinePresent=true;
			}else if(VSIConstants.LINETYPE_PUS.equals(strLineType) || VSIConstants.LINETYPE_STS.equals(strLineType)){
				bBOPUSOrSTSPresent=true;
				String strShipNode=eleOrderLine.getAttribute(VSIConstants.ATTR_SHIP_NODE);
				if(YFCCommon.isVoid(strPreviousNode)){
					printLogs("Setting the ShipNode value for the first time as "+strShipNode);
					strPreviousNode=strShipNode;
				}else if(!strPreviousNode.equals(strShipNode)){
					printLogs("OrderLine with PrimeLineNo "+strPrimeLineNo+" with ShipNode "+strShipNode+" differnt from previous ShipNode "+strPreviousNode+", so setting MultiNode flag as true");
					bMultiNodePresent=true;
				}
			}
		}
		
		printLogs("Value of bSTHLinePresent: "+bSTHLinePresent);
		printLogs("Value of bBOPUSOrSTSPresent: "+bBOPUSOrSTSPresent);
		printLogs("Value of bMultiNodePresent: "+bMultiNodePresent);
		
		if((bSTHLinePresent && bBOPUSOrSTSPresent) || bMultiNodePresent){
			printLogs("Mixed Cart Scenario");
			bMixedCart=true;
		}
		
		printLogs("VSIMixedCartCondition.evaluateCondition Output: "+bMixedCart);
		printLogs("================Exiting VSIMixedCartCondition Class================================");
		
		return bMixedCart;
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
