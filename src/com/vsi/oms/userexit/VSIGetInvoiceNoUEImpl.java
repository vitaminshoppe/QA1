package com.vsi.oms.userexit;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.vsi.oms.utils.VSIConstants;
import com.vsi.oms.utils.VSIDBUtil;
import com.vsi.oms.utils.VSIUtils;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;
import com.yantra.yfs.japi.YFSUserExitException;
import com.yantra.yfs.japi.ue.YFSGetInvoiceNoUE;
import com.yantra.yfc.log.YFCLogCategory;

public class VSIGetInvoiceNoUEImpl implements YFSGetInvoiceNoUE{

	private YFCLogCategory log = YFCLogCategory.instance(VSIGetInvoiceNoUEImpl.class);
	@Override
	public String getInvoiceNo(YFSEnvironment env, String strEnterpriseCode, String strOrderNo, String strShipmentNo) throws YFSUserExitException {
		// TODO Auto-generated method stub
		try{

			//Start - changes for wholesale project

			String strInvoiceNum="";
			String strSeqName="";

			Document docGetCommonCodeInput = SCXmlUtil.createDocument(VSIConstants.ELEMENT_COMMON_CODE);
			Element eleCommonCodeElement = docGetCommonCodeInput.getDocumentElement();
			eleCommonCodeElement.setAttribute(VSIConstants.ATTR_CODE_TYPE, VSIConstants.SEQ_WHOLESALE_INVOICE);
			eleCommonCodeElement.setAttribute(VSIConstants.ATTR_CODE_VALUE, strEnterpriseCode);
			eleCommonCodeElement.setAttribute(VSIConstants.ATTR_ORG_CODE, VSIConstants.ATTR_DEFAULT);
			Document docgetCommonCodeOutput = VSIUtils.invokeAPI(env,VSIConstants.API_COMMON_CODE_LIST, docGetCommonCodeInput);
			Element commonCodeListElement = docgetCommonCodeOutput.getDocumentElement();


			if(commonCodeListElement.hasChildNodes()){
				Element eleCommonCode = SCXmlUtil.getChildElement(commonCodeListElement, VSIConstants.ELE_COMMON_CODE);
				strSeqName = SCXmlUtil.getAttribute(eleCommonCode, VSIConstants.ATTR_CODE_LONG_DESC);
				strInvoiceNum = VSIDBUtil.getNextSequence(env,strSeqName); 
				if(log.isDebugEnabled()){
					log.debug("strInvoiceNum :" + strInvoiceNum); }
				return strInvoiceNum; 
			}				

			// Second Check for OrderNo.
			/*
			 * if(VSIConstants.ENT_NAVY_EXCHANGE.contentEquals(strEnterpriseCode)){ String
			 * strInvoiceNum = VSIDBUtil.getNextSequence(env,
			 * VSIConstants.SEQ_NAVY_EXCHANGE_INVOICE); if(log.isDebugEnabled()){
			 * log.debug("strInvoiceNum :" + strInvoiceNum); } return strInvoiceNum; }
			 */		
			
			//End - changes for wholesale project
		}catch (Exception e) {
			e.printStackTrace();
			throw new YFSException(
					e.getMessage(),
					e.getMessage(),
					"YFS Exception");

		} 
		return null;
	}

}
