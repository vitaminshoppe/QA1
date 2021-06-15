package com.vsi.oms.api.order;

import java.util.ArrayList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.vsi.oms.utils.VSIConstants;
import com.vsi.oms.utils.VSIUtils;
import com.vsi.oms.utils.XMLUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;

public class VSIWholesaleOrderUpdateNotification implements VSIConstants{
	
	private static YFCLogCategory log = YFCLogCategory.instance(VSIWholesaleOrderUpdateNotification.class);
	private static final String TAG = VSIWholesaleOrderUpdateNotification.class.getSimpleName();
	
	public Document prepareUpdateNotification(YFSEnvironment env, Document inXML){
		
		printLogs("================Inside VSIWholesaleOrderUpdateNotification class and prepareUpdateNotification Method================================");
		printLogs("Printing Input XML: "+SCXmlUtil.getString(inXML));
		
		try {
			
			Element eleOrderIn=inXML.getDocumentElement();
			Document docNotificationIn=SCXmlUtil.createDocument(ELE_ORDER);
			Element eleNotificationIn=docNotificationIn.getDocumentElement();
			Element eleNtfcnOrdLns=SCXmlUtil.createChild(eleNotificationIn, ELE_ORDER_LINES);
			Element eleOrderAuditIn=SCXmlUtil.getChildElement(eleOrderIn, ELE_ORDER_AUDIT);
			String strItemId=null;
			String strStoreNo=null;
			String strOrderedQty=null;
			if(!YFCCommon.isVoid(eleOrderAuditIn)) {
				printLogs("OrderAudit details are available in the input");				
				String strCreatUsrId=eleOrderAuditIn.getAttribute("Createuserid");
				String strMdfyUsrId=eleOrderAuditIn.getAttribute("Modifyuserid");
				if("VSIWholesaleOrderCreate".equals(strCreatUsrId) && "VSIWholesaleOrderCreate".equals(strMdfyUsrId)) {
					Element eleOrdAudtLvls=SCXmlUtil.getChildElement(eleOrderAuditIn, "OrderAuditLevels");
					ArrayList<Element> alOrdAudtLvl = SCXmlUtil.getChildren(eleOrdAudtLvls, "OrderAuditLevel");
					for(int i=0; i<alOrdAudtLvl.size(); i++) {
						Element eleOrdAudtLvl=alOrdAudtLvl.get(i);
						String strMdfycnLvl=eleOrdAudtLvl.getAttribute("ModificationLevel");
						if("ORDER_LINE".equals(strMdfycnLvl)) {
							Document docOrdAdtLvl=XMLUtil.getDocumentForElement(eleOrdAudtLvl);
							Element eleOrdAdtLvl=XMLUtil.getElementByXPath(docOrdAdtLvl, "/OrderAuditLevel/OrderAuditDetails/OrderAuditDetail/Attributes/Attribute[@Name='OrderedQty' or @Name='ExtnShipNotBeforeDate' or @Name='ExtnReqCancelDate' or @Name='ExtnShipNotAfterDate' or @Name='ExtnReqShipDate' "
									+ "or @Name='PrimeLineNo']");
							if(!YFCCommon.isVoid(eleOrdAdtLvl)) {
								String strOrdLnKey=eleOrdAudtLvl.getAttribute(ATTR_ORDER_LINE_KEY);
								Element eleOrderLineIn=SCXmlUtil.getChildElement(eleOrdAudtLvl, ELE_ORDER_LINE);
								String strPrimeLineNo=eleOrderLineIn.getAttribute(ATTR_PRIME_LINE_NO);
								Element eleOrdLnIn=XMLUtil.getElementByXPath(inXML, "/Order/OrderLines/OrderLine[@OrderLineKey='"+strOrdLnKey+"']");
								if(!YFCCommon.isVoid(eleOrdLnIn)) {
									strOrderedQty=eleOrdLnIn.getAttribute(ATTR_ORD_QTY);
									Element eleItemIn=SCXmlUtil.getChildElement(eleOrdLnIn, ELE_ITEM);
									strItemId=eleItemIn.getAttribute(ATTR_ITEM_ID);
									Element elePrsnInfoShipToLn=SCXmlUtil.getChildElement(eleOrdLnIn, ELE_PERSON_INFO_SHIP_TO);
									Element elePrsnInfoExtn=SCXmlUtil.getChildElement(elePrsnInfoShipToLn, ELE_EXTN);
									strStoreNo=elePrsnInfoExtn.getAttribute(ATTR_EXTN_MARK_FOR_STORE_NO);
								}
								Element eleNtfcnOrdLn=SCXmlUtil.createChild(eleNtfcnOrdLns, ELE_ORDER_LINE);
								eleNtfcnOrdLn.setAttribute(ATTR_PRIME_LINE_NO, strPrimeLineNo);
								if(!YFCCommon.isVoid(strItemId)) {
									eleNtfcnOrdLn.setAttribute(ATTR_ITEM_ID, strItemId);
								}
								if(!YFCCommon.isVoid(strStoreNo)) {
									eleNtfcnOrdLn.setAttribute(ATTR_EXTN_MARK_FOR_STORE_NO, strStoreNo);
								}
								Element eleNtfcnLnChngs=SCXmlUtil.createChild(eleNtfcnOrdLn, "LineChanges");
								Element eleOrdAdtDtls=SCXmlUtil.getChildElement(eleOrdAudtLvl, "OrderAuditDetails");
								Element eleOrdAdtDtl=SCXmlUtil.getChildElement(eleOrdAdtDtls, "OrderAuditDetail");
								Element eleAttributes=SCXmlUtil.getChildElement(eleOrdAdtDtl, "Attributes");
								ArrayList<Element> alAttribute = SCXmlUtil.getChildren(eleAttributes, "Attribute");
								for(int j=0; j<alAttribute.size(); j++) {
									Element eleAttribute=alAttribute.get(j);
									String strAttrName=eleAttribute.getAttribute(ATTR_NAME);
									if("OrderedQty".equals(strAttrName)) {
										String strOldQty=eleAttribute.getAttribute(ATTR_OLD_VALUE);
										String strNewQty=eleAttribute.getAttribute(ATTR_NEW_VALUE);
										Element eleNtfcnLnChng=SCXmlUtil.createChild(eleNtfcnLnChngs, "LineChange");
										eleNtfcnLnChng.setAttribute("ChangeType", "QuantityChange");
										eleNtfcnLnChng.setAttribute("OldValue", strOldQty);
										eleNtfcnLnChng.setAttribute("NewValue", strNewQty);
									}else if("ExtnShipNotBeforeDate".equals(strAttrName)) {
										String strOldDate=eleAttribute.getAttribute(ATTR_OLD_VALUE);
										String strNewDate=eleAttribute.getAttribute(ATTR_NEW_VALUE);
										Element eleNtfcnLnChng=SCXmlUtil.createChild(eleNtfcnLnChngs, "LineChange");
										eleNtfcnLnChng.setAttribute("ChangeType", "DateChange");
										eleNtfcnLnChng.setAttribute("DateType", "ExtnShipNotBeforeDate");
										eleNtfcnLnChng.setAttribute("OldValue", strOldDate);
										eleNtfcnLnChng.setAttribute("NewValue", strNewDate);
									}else if("ExtnReqCancelDate".equals(strAttrName)) {
										String strOldDate=eleAttribute.getAttribute(ATTR_OLD_VALUE);
										String strNewDate=eleAttribute.getAttribute(ATTR_NEW_VALUE);
										Element eleNtfcnLnChng=SCXmlUtil.createChild(eleNtfcnLnChngs, "LineChange");
										eleNtfcnLnChng.setAttribute("ChangeType", "DateChange");
										eleNtfcnLnChng.setAttribute("DateType", "ExtnReqCancelDate");
										eleNtfcnLnChng.setAttribute("OldValue", strOldDate);
										eleNtfcnLnChng.setAttribute("NewValue", strNewDate);
									}else if("ExtnReqShipDate".equals(strAttrName)) {
										String strOldDate=eleAttribute.getAttribute(ATTR_OLD_VALUE);
										String strNewDate=eleAttribute.getAttribute(ATTR_NEW_VALUE);
										Element eleNtfcnLnChng=SCXmlUtil.createChild(eleNtfcnLnChngs, "LineChange");
										eleNtfcnLnChng.setAttribute("ChangeType", "DateChange");
										eleNtfcnLnChng.setAttribute("DateType", "ExtnReqShipDate");
										eleNtfcnLnChng.setAttribute("OldValue", strOldDate);
										eleNtfcnLnChng.setAttribute("NewValue", strNewDate);
									}else if("ExtnShipNotAfterDate".equals(strAttrName)) {
										String strOldDate=eleAttribute.getAttribute(ATTR_OLD_VALUE);
										String strNewDate=eleAttribute.getAttribute(ATTR_NEW_VALUE);
										Element eleNtfcnLnChng=SCXmlUtil.createChild(eleNtfcnLnChngs, "LineChange");
										eleNtfcnLnChng.setAttribute("ChangeType", "DateChange");
										eleNtfcnLnChng.setAttribute("DateType", "ExtnShipNotAfterDate");
										eleNtfcnLnChng.setAttribute("OldValue", strOldDate);
										eleNtfcnLnChng.setAttribute("NewValue", strNewDate);
									}else if("PrimeLineNo".equals(strAttrName)) {
										String strNewLineNo=eleAttribute.getAttribute(ATTR_NEW_VALUE);
										Element eleNtfcnLnChng=SCXmlUtil.createChild(eleNtfcnLnChngs, "LineChange");
										eleNtfcnLnChng.setAttribute("ChangeType", "AddLine");
										eleNtfcnLnChng.setAttribute("NewValue", strNewLineNo);
										if(!YFCCommon.isVoid(strOrderedQty)) {
											eleNtfcnLnChng.setAttribute("Quantity", strOrderedQty);
										}
									}
								}
							}
						}
					}
					NodeList nlOrderLine=eleNtfcnOrdLns.getElementsByTagName(VSIConstants.ELE_ORDER_LINE);
					if(nlOrderLine.getLength()>0) {
						printLogs("OrderLines with attribute changes are available");
						String strEntCode=eleOrderIn.getAttribute(ATTR_ENTERPRISE_CODE);
						String strOrderNo=eleOrderIn.getAttribute(ATTR_ORDER_NO);
						String strCustPONo=eleOrderIn.getAttribute(ATTR_CUST_PO_NO);
						String strStatus=eleOrderIn.getAttribute(ATTR_STATUS);
						eleNotificationIn.setAttribute(ATTR_ENTERPRISE_CODE, strEntCode);
						eleNotificationIn.setAttribute(ATTR_ORDER_NO, strOrderNo);
						eleNotificationIn.setAttribute(ATTR_CUST_PO_NO, strCustPONo);
						eleNotificationIn.setAttribute(ATTR_STATUS, strStatus);
						eleNotificationIn.setAttribute("ChangeStatus", "Accepted");
						
						printLogs("Input to VSIWHOrderChangeNotificationEmail service "+SCXmlUtil.getString(docNotificationIn));
						VSIUtils.invokeService(env, "VSIWHOrderChangeNotificationEmail", docNotificationIn);
						printLogs("VSIWHOrderChangeNotificationEmail service invoked successfully");
					}
				}				
			}
		}catch (Exception e) {
			printLogs("Exception in VSIWholesaleOrderUpdateNotification Class and prepareUpdateNotification Method");
			printLogs("The exception is [ "+ e.getMessage() +" ]");
			throw new YFSException();
		}
		
		
		printLogs("================Exiting VSIWholesaleOrderUpdateNotification class and prepareUpdateNotification Method================================");
		
		return inXML;
		
	}
	
	private void printLogs(String mesg) {
		if(log.isDebugEnabled()){
			log.debug(TAG +" : "+mesg);
		}
	}

}
