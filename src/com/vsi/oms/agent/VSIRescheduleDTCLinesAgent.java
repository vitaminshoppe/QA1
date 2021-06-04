package com.vsi.oms.agent;

import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.vsi.oms.utils.VSIConstants;
import com.vsi.oms.utils.VSIUtils;
import com.vsi.oms.utils.XMLUtil;
import com.yantra.interop.japi.YIFClientCreationException;
import com.yantra.ycp.japi.util.YCPBaseAgent;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;

public class VSIRescheduleDTCLinesAgent extends YCPBaseAgent implements VSIConstants {
	
	private static YFCLogCategory log = YFCLogCategory.instance(VSIRescheduleDTCLinesAgent.class.getName());
	
	private static final String TAG = VSIRescheduleDTCLinesAgent.class.getSimpleName();
	
	public final List<Document> getJobs(YFSEnvironment env, Document inDoc){
		
		printLogs("================Inside VSIRescheduleDTCLinesAgent class and getJobs Method================================");
		printLogs("Printing Input XML: "+SCXmlUtil.getString(inDoc));
		
		List<Document> orderList = new ArrayList<Document>();
		
		try{
			Document docGetOrdLnLstIn=XMLUtil.createDocument(ELE_ORDER_LINE);
			Element eleGetOrdLnLstIn=docGetOrdLnLstIn.getDocumentElement();
			eleGetOrdLnLstIn.setAttribute(ATTR_LINE_TYPE, LINETYPE_STH);
			eleGetOrdLnLstIn.setAttribute(ATTR_DELIVERY_METHOD, ATTR_DEL_METHOD_SHP);
			eleGetOrdLnLstIn.setAttribute(ATTR_STATUS, STATUS_NO_ACTION);
			eleGetOrdLnLstIn.setAttribute(ATTR_READ_FROM_HISTORY, FLAG_N);
			
			Element eleOrderIn=SCXmlUtil.createChild(eleGetOrdLnLstIn, ELE_ORDER);
			eleOrderIn.setAttribute(ATTR_DRAFT_ORDER_FLAG, FLAG_N);
			
			Document docOrderHoldList = callGetCommonCodeList(env, "RESCHED_ORD_HLD_LST");
			
			String strCodeShortDesc=null;
			int iNoOfDays=0;
			
			Element eleNoOfDays=XMLUtil.getElementByXPath(docOrderHoldList, "/CommonCodeList/CommonCode[@CodeValue='Number Of Days']");
			if(!YFCCommon.isVoid(eleNoOfDays)) {
				strCodeShortDesc=eleNoOfDays.getAttribute(ATTR_CODE_SHORT_DESCRIPTION);
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
			
			eleOrderIn.setAttribute(ATTR_FROM_ORDER_DATE, strFromDate);
			eleOrderIn.setAttribute(ATTR_TO_ORDER_DATE, strToDate);
			eleOrderIn.setAttribute(ATTR_ORDER_DATE_QRY_TYPE, BETWEEN_QUERY);
			
			eleOrderIn.setAttribute(ATTR_ORDER_TYPE, WHOLESALE);
			eleOrderIn.setAttribute("OrderTypeQryType", "NE");
			
			printLogs("Input to getOrderLineList API: "+SCXmlUtil.getString(docGetOrdLnLstIn));
			Document docGetOrdLnLstOut = VSIUtils.invokeAPI(env, "global/template/api/getOrderLineList_DTCReschedule.xml", API_GET_ORDER_LINE_LIST, docGetOrdLnLstIn);
			printLogs("Output from getOrderLineList API: "+SCXmlUtil.getString(docGetOrdLnLstOut));
			
			Element eleGetOrdLnLstOut=docGetOrdLnLstOut.getDocumentElement();
			ArrayList<Element> alOrderLineList = SCXmlUtil.getChildren(eleGetOrdLnLstOut, ELE_ORDER_LINE);			
			for(int i=0; i<alOrderLineList.size(); i++) {
				
				boolean bOrderHldPrsnt=false;
				boolean bOrdLnHldPrsnt=false;
				
				Element eleOrderLine=alOrderLineList.get(i);				
				String strOrderLineKey=eleOrderLine.getAttribute(ATTR_ORDER_LINE_KEY);
				printLogs("OrderLine with orderlinekey "+strOrderLineKey+" is being processed");
				Element eleOrderAPI=SCXmlUtil.getChildElement(eleOrderLine, ELE_ORDER);
				Element eleOrderHoldTypesAPI=SCXmlUtil.getChildElement(eleOrderAPI, ELE_ORDER_HOLD_TYPES);
				ArrayList<Element> alOrderHoldList = SCXmlUtil.getChildren(eleOrderHoldTypesAPI, ELE_ORDER_HOLD_TYPE);				
				for(int k=0; k<alOrderHoldList.size(); k++) {
					Element eleOrderHoldTypeAPI=alOrderHoldList.get(k);
					String strOrderHoldType=eleOrderHoldTypeAPI.getAttribute(ATTR_HOLD_TYPE);
					Element eleOrdHldTypOut=XMLUtil.getElementByXPath(docOrderHoldList, "/CommonCodeList/CommonCode[@CodeValue='"+strOrderHoldType+"']");
					if(!YFCCommon.isVoid(eleOrdHldTypOut)) {
						String strHldSts=eleOrderHoldTypeAPI.getAttribute(ATTR_STATUS);
						if(STATUS_CREATE.equals(strHldSts)) {
							bOrderHldPrsnt=true;
							printLogs(strOrderHoldType+" is active on the order, orderline will be skipped");
							break;
						}
					}
				}
				if(bOrderHldPrsnt) {
					continue;
				}
				Element eleOrderLineHldTyps=SCXmlUtil.getChildElement(eleOrderLine, ELE_ORDER_HOLD_TYPES);
				ArrayList<Element> alOrderLineHoldList = SCXmlUtil.getChildren(eleOrderLineHldTyps, ELE_ORDER_HOLD_TYPE);				
				for(int m=0; m<alOrderLineHoldList.size(); m++) {
					Element eleOrderLnHldTyp=alOrderLineHoldList.get(m);
					String strOrderHoldType=eleOrderLnHldTyp.getAttribute(ATTR_HOLD_TYPE);
					Element eleOrdLnHldTypOut=XMLUtil.getElementByXPath(docOrderHoldList, "/CommonCodeList/CommonCode[@CodeValue='"+strOrderHoldType+"']");
					if(!YFCCommon.isVoid(eleOrdLnHldTypOut)) {
						String strHldSts=eleOrderLnHldTyp.getAttribute(ATTR_STATUS);
						if(STATUS_CREATE.equals(strHldSts)) {
							bOrdLnHldPrsnt=true;
							printLogs(strOrderHoldType+" is active on the orderline, hence it will be skipped");
							break;
						}
					}
				}
				if(bOrdLnHldPrsnt) {
					continue;
				}
				Element eleOrderStatusesAPI=SCXmlUtil.getChildElement(eleOrderLine, ELE_ORDER_STATUSES);
				ArrayList<Element> alOrderLineStatusAPI = SCXmlUtil.getChildren(eleOrderStatusesAPI, ELE_ORDER_STATUS);				
				for(int j=0; j<alOrderLineStatusAPI.size(); j++) {
					Element eleOrderLineStatusAPI=alOrderLineStatusAPI.get(j);
					String strStatus=eleOrderLineStatusAPI.getAttribute(ATTR_STATUS);					
					if(STATUS_NO_ACTION.equals(strStatus)) {
						String strShipNode=eleOrderLineStatusAPI.getAttribute(ATTR_SHIP_NODE);
						Element eleDetails=SCXmlUtil.getChildElement(eleOrderLineStatusAPI, ELE_DETAILS);						
						if(!YFCCommon.isVoid(eleDetails)) {							
							String strETS=eleDetails.getAttribute(ATTR_EXPECTED_SHIPMENT_DATE);
							Date dETSFormatted=new SimpleDateFormat(YYYY_MM_DD_T_HH_MM_SS).parse(strETS);
							Calendar cETSFormatted = Calendar.getInstance();
							cETSFormatted.setTime(dETSFormatted);		  
							Calendar currentTime = Calendar.getInstance();
							String strCurrentTime = new SimpleDateFormat(VSIConstants.YYYY_MM_DD_T_HH_MM_SS).format(currentTime.getTime());
							Date dCurrentTime=new SimpleDateFormat(YYYY_MM_DD_T_HH_MM_SS).parse(strCurrentTime);
							currentTime.setTime(dCurrentTime);
							if((VSI_VADC.equals(strShipNode) || VSI_AZDC.equals(strShipNode)) && (cETSFormatted.compareTo(currentTime) > 0)) {
								Document docOrderLine = SCXmlUtil.createFromString(SCXmlUtil.getString(eleOrderLine));
				               	orderList.add(docOrderLine);
				               	printLogs("OrderLine with orderlinekey "+strOrderLineKey+" is selected for backorder");
							}
						}
					}
				}
				printLogs("OrderLine with orderlinekey "+strOrderLineKey+" processing completed");
			}
			printLogs("All the orderlines from getOrderLineList API output is processed");
			
		}catch(Exception e){			
			printLogs("Exception in VSIRescheduleDTCLinesAgent Class and getJobs Method");
			printLogs("The exception is [ "+ e.getMessage() +" ]");			
		}
		
		printLogs("Total number of orderlines available for backorder: "+Integer.toString(orderList.size()));
		printLogs("================Exiting VSIRescheduleDTCLinesAgent class and getJobs Method================================");
		return orderList;
		
	}

	private Document callGetCommonCodeList(YFSEnvironment env, String strCodeType) throws YIFClientCreationException, RemoteException {
		
		printLogs("================Inside VSIRescheduleDTCLinesAgent class and callGetCommonCodeList Method================================");
		printLogs("CodeType is: "+strCodeType);
		
		Document docCommonCodeIn=SCXmlUtil.createDocument(ELEMENT_COMMON_CODE);
		Element eleCommonCodeIn=docCommonCodeIn.getDocumentElement();				
		eleCommonCodeIn.setAttribute(ATTR_CODE_TYPE, strCodeType);
		
		printLogs("Input to getCommonCodeList API: "+SCXmlUtil.getString(docCommonCodeIn));
		Document docCommonCodeOut = VSIUtils.invokeAPI(env,API_COMMON_CODE_LIST, docCommonCodeIn);
		printLogs("Output from getCommonCodeList API: "+SCXmlUtil.getString(docCommonCodeOut));
		
		printLogs("================Exiting VSIRescheduleDTCLinesAgent class and callGetCommonCodeList Method================================");
		return docCommonCodeOut;
	}
	
	@Override
	public void executeJob(YFSEnvironment env, Document docInput) throws Exception {
		
		printLogs("================Inside VSIRescheduleDTCLinesAgent class and executeJob Method================================");
		printLogs("Printing Input XML: "+SCXmlUtil.getString(docInput));
		
		try {
			
			Element eleOrderLine=docInput.getDocumentElement();
			String strOrderHeaderKey=eleOrderLine.getAttribute(ATTR_ORDER_HEADER_KEY);
			String strOrderLineKey=eleOrderLine.getAttribute(ATTR_ORDER_LINE_KEY);
			Document docChngOrdStsIn=XMLUtil.createDocument(ELE_ORDER_STATUS_CHANGE);
			Element eleChngOrdStsIn=docChngOrdStsIn.getDocumentElement();
			eleChngOrdStsIn.setAttribute(ATTR_BASE_DROP_STATUS, STATUS_BACKORDERED);
			eleChngOrdStsIn.setAttribute(ATTR_CHANGE_FOR_ALL_AVAILABLE_QTY, FLAG_Y);
			eleChngOrdStsIn.setAttribute(ELE_IGNORE_TRANSACTION_DEPENDENCIES, FLAG_Y);
			eleChngOrdStsIn.setAttribute(ATTR_ORDER_HEADER_KEY, strOrderHeaderKey);
			eleChngOrdStsIn.setAttribute(ATTR_TRANSACTION_ID, TXN_SCHEDULE);
			Element eleOrderLines=SCXmlUtil.createChild(eleChngOrdStsIn, ELE_ORDER_LINES);
			Element eleOrderLineAPI=SCXmlUtil.createChild(eleOrderLines, ELE_ORDER_LINE);
			eleOrderLineAPI.setAttribute(ATTR_BASE_DROP_STATUS, STATUS_BACKORDERED);
			eleOrderLineAPI.setAttribute(ATTR_CHANGE_FOR_ALL_AVAILABLE_QTY, FLAG_Y);
			eleOrderLineAPI.setAttribute(ATTR_ORDER_LINE_KEY, strOrderLineKey);
			
			printLogs("Input to changeOrderStatus API: "+SCXmlUtil.getString(docChngOrdStsIn));
			VSIUtils.invokeAPI(env, API_CHANGE_ORDER_STATUS, docChngOrdStsIn);
			printLogs("changeOrderStatus API was invoked successfully");			
			
		}catch(Exception e){			
			printLogs("Exception in VSIRescheduleDTCLinesAgent Class and executeJob Method");
			printLogs("The exception is [ "+ e.getMessage() +" ]");			
		}
		
		printLogs("================Exiting VSIRescheduleDTCLinesAgent class and executeJob Method================================");
	}
	
	private void printLogs(String mesg) {
		if(log.isDebugEnabled()){
			log.debug(TAG +" : "+mesg);
		}
	}

}
