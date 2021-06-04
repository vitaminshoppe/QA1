package com.vsi.som.shipment;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.vsi.oms.utils.VSIConstants;
import com.vsi.oms.utils.VSIUtils;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;

public class VSICurbsideWebserviceInput implements VSIConstants{
	
	private static YFCLogCategory log = YFCLogCategory.instance(VSICurbsideWebserviceInput.class);
	private static final String TAG = VSICurbsideWebserviceInput.class.getSimpleName();
	
	public Document formCurbsideWebserviceInput(YFSEnvironment env, Document inXML){
		
		printLogs("================Inside VSICurbsideWebserviceInput class and formCurbsideWebserviceInput Method================================");
		printLogs("Printing Input XML: "+SCXmlUtil.getString(inXML));
		
		Document docGetOrderListIn=SCXmlUtil.createDocument(ELE_ORDER);
		
		try{
			
			Element eleInput=inXML.getDocumentElement();
			String strBillToId=eleInput.getAttribute(ATTR_BILL_TO_ID);	
			
			Element eleGetOrderListIn=docGetOrderListIn.getDocumentElement();
						
			if(!YFCCommon.isVoid(strBillToId)) {
				eleGetOrderListIn.setAttribute(ATTR_BILL_TO_ID, strBillToId);
			}
			
			eleGetOrderListIn.setAttribute(ATTR_READ_FROM_HISTORY, FLAG_N);
			eleGetOrderListIn.setAttribute(ATTR_DRAFT_ORDER_FLAG, FLAG_N);
			eleGetOrderListIn.setAttribute(ATTR_DOCUMENT_TYPE, DOCUMENT_TYPE);
			
			Document docCommonCodeIn=SCXmlUtil.createDocument(ELEMENT_COMMON_CODE);
			Element eleCommonCodeIn=docCommonCodeIn.getDocumentElement();				
			eleCommonCodeIn.setAttribute(ATTR_CODE_TYPE, "VSI_CURBSIDE_PICKUP");
			
			printLogs("Input to getCommonCodeList API: "+SCXmlUtil.getString(docCommonCodeIn));
			Document docCommonCodeOut = VSIUtils.invokeAPI(env,API_COMMON_CODE_LIST, docCommonCodeIn);
			printLogs("Output from getCommonCodeList API: "+SCXmlUtil.getString(docCommonCodeOut));
			
			String strCodeShortDesc=null;
			int iNoOfDays=0;
			
			Element eleCommonCodeOut=docCommonCodeOut.getDocumentElement();
			Element eleCommonCode=(Element)eleCommonCodeOut.getElementsByTagName(ELEMENT_COMMON_CODE).item(0);
			if(!YFCCommon.isVoid(eleCommonCode)) {
				strCodeShortDesc=eleCommonCode.getAttribute(ATTR_CODE_SHORT_DESCRIPTION);
			}
			
			if(!YFCCommon.isVoid(strCodeShortDesc)) {
				iNoOfDays=Integer.parseInt(strCodeShortDesc);
			}
			
			Calendar calendar = Calendar.getInstance();
			String strToDate = new SimpleDateFormat("yyyy-MM-dd").format(calendar.getTime());
			strToDate=strToDate+"T23:59:59";
			
			calendar.add(Calendar.DAY_OF_MONTH, -iNoOfDays);
			String strFromDate=new SimpleDateFormat("yyyy-MM-dd").format(calendar.getTime());
			strFromDate=strFromDate+"T00:00:01";
			
			eleGetOrderListIn.setAttribute("FromOrderDate", strFromDate);
			eleGetOrderListIn.setAttribute("ToOrderDate", strToDate);
			eleGetOrderListIn.setAttribute("OrderDateQryType", BETWEEN_QUERY);
			
			Element eleOrderStatus=SCXmlUtil.createChild(eleGetOrderListIn, ELE_ORDER_STATUS);
			Element eleComplexQry=SCXmlUtil.createChild(eleOrderStatus, ELE_COMPLEX_QUERY);
			eleComplexQry.setAttribute(ATTR_OPERATOR, AND);
			Element eleExpSts1=SCXmlUtil.createChild(eleComplexQry, ELE_EXP);
			eleExpSts1.setAttribute(ATTR_NAME, ATTR_STATUS);
			eleExpSts1.setAttribute(ATTR_VALUE, STATUS_SOM_READY_FOR_PICKUP);
			eleExpSts1.setAttribute(ATTR_STATUS_QRY_TYPE, "GE");
			Element eleExpSts2=SCXmlUtil.createChild(eleComplexQry, ELE_EXP);
			eleExpSts2.setAttribute(ATTR_NAME, ATTR_STATUS);
			eleExpSts2.setAttribute(ATTR_VALUE, STATUS_CANCEL);
			eleExpSts2.setAttribute(ATTR_STATUS_QRY_TYPE, "NE");
			Element eleExpSts3=SCXmlUtil.createChild(eleComplexQry, ELE_EXP);
			eleExpSts3.setAttribute(ATTR_NAME, ATTR_STATUS);
			eleExpSts3.setAttribute(ATTR_VALUE, STATUS_CODE_SHIPPED);
			eleExpSts3.setAttribute(ATTR_STATUS_QRY_TYPE, "NE");
			
			Element eleOrderLine=SCXmlUtil.createChild(eleGetOrderListIn, ELE_ORDER_LINE);
			eleOrderLine.setAttribute(ATTR_LINE_TYPE, LINETYPE_STH);
			eleOrderLine.setAttribute("LineTypeQryType", "NE");
			
		}catch (Exception e) {
			printLogs("Exception in VSICurbsideWebserviceInput Class and formCurbsideWebserviceInput Method");
			printLogs("The exception is [ "+ e.getMessage() +" ]");
			throw new YFSException();
		}
				
		printLogs("GetOrderList API Input prepared is: "+SCXmlUtil.getString(docGetOrderListIn));
		
		printLogs("================Exiting VSICurbsideWebserviceInput class and formCurbsideWebserviceInput Method================================");
		
		return docGetOrderListIn;
		
	}
	
	private void printLogs(String mesg) {
		if(log.isDebugEnabled()){
			log.debug(TAG +" : "+mesg);
		}
	}

}
