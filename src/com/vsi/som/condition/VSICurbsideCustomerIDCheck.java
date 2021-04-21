package com.vsi.som.condition;

import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.vsi.oms.utils.VSIConstants;
import com.yantra.ycp.japi.YCPDynamicConditionEx;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;

public class VSICurbsideCustomerIDCheck implements YCPDynamicConditionEx, VSIConstants {
	
	private static YFCLogCategory log = YFCLogCategory.instance(VSICurbsideCustomerIDCheck.class);
	private static final String TAG = VSICurbsideCustomerIDCheck.class.getSimpleName();

	@Override
	public boolean evaluateCondition(YFSEnvironment env, String condName, Map mapData, Document inXML) {
		
		printLogs("================Inside VSICurbsideCustomerIDCheck class and evaluateCondition Method================================");
		printLogs("Printing Input XML: "+SCXmlUtil.getString(inXML));
		
		boolean bCustomerIDExist = false;
		
		Element eleInput = inXML.getDocumentElement();
		
		if(!YFCCommon.isVoid(eleInput)) {
			String strCustomerID=eleInput.getAttribute(ATTR_BILL_TO_ID);
			if(!YFCCommon.isVoid(strCustomerID)) {
				bCustomerIDExist=true;
				printLogs("Customer ID is present in the Input");
			}else {
				printLogs("Customer ID is not present in the Input");
				YFSException exception = new YFSException();
				exception.setErrorCode("YFS10319");
				exception.setErrorDescription("YFS:Invalid Customer Id");
				exception.setAttribute(ATTR_ERROR_CODE, "YFS10319");
				exception.setAttribute(ATTR_ERROR_DESC, "YFS:Invalid Customer Id");
				throw exception;
			}
		}
		printLogs("Is Customer ID present: "+bCustomerIDExist);
		printLogs("================Exiting VSICurbsideCustomerIDCheck class and evaluateCondition Method================================");
		return bCustomerIDExist;
	}
	
	private void printLogs(String mesg) {
		if(log.isDebugEnabled()){
			log.debug(TAG +" : "+mesg);
		}
	}

	@Override
	public void setProperties(Map arg0) {		
	}

}
