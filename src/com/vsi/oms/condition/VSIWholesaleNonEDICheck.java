package com.vsi.oms.condition;

import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.vsi.oms.utils.VSIConstants;
import com.vsi.oms.utils.VSIUtils;
import com.vsi.oms.utils.XMLUtil;
import com.yantra.ycp.japi.YCPDynamicConditionEx;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;

public class VSIWholesaleNonEDICheck implements YCPDynamicConditionEx, VSIConstants {
	
	private YFCLogCategory log = YFCLogCategory.instance(VSIWholesaleNonEDICheck.class);
	private static final String TAG = VSIWholesaleNonEDICheck.class.getSimpleName();

	@SuppressWarnings("rawtypes")
	@Override
	public boolean evaluateCondition(YFSEnvironment env,
			String condName, Map mapData, Document inXML) {
		
		printLogs("================Inside VSIWholesaleNonEDICheck Class and evaluateCondition Method================================");
		printLogs("Input XML: " + SCXmlUtil.getString(inXML));
		
		boolean bIsNonEDIPartner = false;
		
		try {
			
			Element eleOrder=XMLUtil.getElementByXPath(inXML, "/InvoiceDetailList/InvoiceDetail/InvoiceHeader/Order");
		
			if(!YFCCommon.isVoid(eleOrder)) {
			
			String strEnterpriseCode=eleOrder.getAttribute(ATTR_ENTERPRISE_CODE);
			
			if(!YFCCommon.isVoid(strEnterpriseCode)) {
				
				printLogs("Enterprise Code is: "+strEnterpriseCode);
				
				Document docCommonCodeIn=SCXmlUtil.createDocument(ELEMENT_COMMON_CODE);
				Element eleCommonCodeIn=docCommonCodeIn.getDocumentElement();
				eleCommonCodeIn.setAttribute(ATTR_CODE_TYPE, "VSI_WH_NONEDI_PRTNRS");
				eleCommonCodeIn.setAttribute(ATTR_ORG_CODE, ATTR_DEFAULT);
				eleCommonCodeIn.setAttribute(ATTR_CODE_VALUE, strEnterpriseCode);
				
				printLogs("Input to getCommonCodeList API: "+SCXmlUtil.getString(docCommonCodeIn));
				Document docCommonCodeOut=VSIUtils.invokeAPI(env, API_COMMON_CODE_LIST, docCommonCodeIn);
				printLogs("Output from getCommonCodeList API: "+SCXmlUtil.getString(docCommonCodeOut));
				
				Element eleCommonCodeOut=docCommonCodeOut.getDocumentElement();
				if(eleCommonCodeOut.hasChildNodes()) {
					bIsNonEDIPartner=true;
					printLogs("Wholesale Partner belongs to Non-EDI category");
				}				
			}			
		}
		
		}catch(Exception ex) {
			printLogs("Exception in VSIWholesaleNonEDICheck Class and evaluateCondition Method");
			printLogs("The exception is [ "+ ex.getMessage() +" ]");
			throw new YFSException();			
		}
		
		printLogs("Condition Output: "+bIsNonEDIPartner);
		printLogs("================Exiting VSIWholesaleNonEDICheck Class and evaluateCondition Method================================");
		
		return bIsNonEDIPartner;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void setProperties(Map arg0) {
		//This method is added because this class implements YCPDynamicConditionEx class
	}
	
	private void printLogs(String mesg) {
		if(log.isDebugEnabled()){
			log.debug(TAG +" : "+mesg);
		}
	}

}
