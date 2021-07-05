package com.vsi.oms.condition;

import java.util.ArrayList;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.vsi.oms.utils.VSIConstants;
import com.vsi.oms.utils.VSIUtils;
import com.vsi.oms.utils.XMLUtil;
import com.yantra.ycp.japi.YCPDynamicConditionEx;
import com.yantra.yfc.core.YFCObject;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;

public class VSIApplyFraudVerificationHoldSTH implements YCPDynamicConditionEx {
	private YFCLogCategory log = YFCLogCategory.instance(VSIApplyFraudVerificationHoldSTH.class);
	@Override
	/**
	 * Condition to apply FraudVerificationHold
	 */
	public boolean evaluateCondition(YFSEnvironment env, String condName,
			@SuppressWarnings("rawtypes") Map mapData, Document inXML) {

		boolean bApplyFraudVerHold = true;
		boolean bCreditCardOrder = false;
		//OMS-2924 Changes -- Start
		boolean bPaypalOrder = false;
		//OMS-2924 Changes -- End
		//OMS-3691 Changes -- Start
		boolean bKlarnaOrder=false;
		//OMS-3691 Changes -- End
		double dAmount=0;
		String strDTCOFlag=null;
		Element eleOrder = inXML.getDocumentElement();
		if(log.isDebugEnabled()){
		log.verbose("VSIApplyFraudVerificationHoldSTH.evaluateCondition Input XML :" + SCXmlUtil.getString(inXML));
		log.info("==============Inside VSIApplyFraudVerificationHoldSTH================================");
		}
		if(!YFCObject.isNull(eleOrder)){

			try {
				//OMS-2332 Start
				Element elePaymentMethods = SCXmlUtil.getChildElement(eleOrder, VSIConstants.ELE_PAYMENT_METHODS);
				NodeList nlPaymentMethod = elePaymentMethods.getElementsByTagName(VSIConstants.ELE_PAYMENT_METHOD);
				for(int i=0; i<nlPaymentMethod.getLength(); i++){
					Element elePaymentMethod = (Element) nlPaymentMethod.item(i);
					String strPaymentType = elePaymentMethod.getAttribute(VSIConstants.ATTR_PAYMENT_TYPE);
					//OMS-2924 Changes -- Start
					if(VSIConstants.PAYMENT_MODE_CC.equals(strPaymentType) || VSIConstants.PAYMENT_MODE_PAYPAL.equals(strPaymentType)){
						if(VSIConstants.PAYMENT_MODE_CC.equals(strPaymentType)){
							bCreditCardOrder=true;
							log.info("Credit Card is used in this Order");
							//OMS-3691 Changes -- Start
							Element elePmntMthdExtn=SCXmlUtil.getChildElement(elePaymentMethod, VSIConstants.ELE_EXTN);
							if(!YFCCommon.isVoid(elePmntMthdExtn)) {
								String strContactLess=elePmntMthdExtn.getAttribute("ExtnContactLess");
								if(!YFCCommon.isVoid(strContactLess) && "klarna".equals(strContactLess)) {
									bKlarnaOrder=true;
									log.info("It's a Klarna Order");
								}
							}
							//OMS-3691 Changes -- End
						}else if(VSIConstants.PAYMENT_MODE_PAYPAL.equals(strPaymentType)){
							bPaypalOrder=true;
							log.info("Paypal is used in this Order");
						}
						//OMS-2924 Changes -- End
						String strAmount = elePaymentMethod.getAttribute(VSIConstants.ATTR_MAX_CHARGE_LIMIT);
						if(!YFCCommon.isVoid(strAmount)){
							dAmount = Double.parseDouble(strAmount);
						}
						if((dAmount==0) || (dAmount==0.0) || (dAmount==0.00)){
							//OMS-2924 Changes -- Start
							if(VSIConstants.PAYMENT_MODE_CC.equals(strPaymentType)){
								bCreditCardOrder=false;
								log.info("Credit Card Request Amount is zero");
							}else if(VSIConstants.PAYMENT_MODE_PAYPAL.equals(strPaymentType)){
								bPaypalOrder=false;
								log.info("Paypal Request Amount is zero");
							}
							//OMS-2924 Changes -- End
						}						
					}
				}
				if((!bCreditCardOrder && !bPaypalOrder) || bKlarnaOrder){		//OMS-3691 Change
					log.info("Non Credit Card/Paypal Order or its a Klarna order, hold will not be applied");		//OMS-3691 Change
					bApplyFraudVerHold=false;
					log.verbose("VSIApplyFraudVerificationHoldSTH.evaluateCondition output: " +bApplyFraudVerHold);
					return bApplyFraudVerHold;
				}
				//OMS-2332 End
				//OMS-1707 start
				ArrayList<Element> listDTCOrderFlag;
				listDTCOrderFlag = VSIUtils.getCommonCodeList(env, "VSI_DTC_FLAG", "DTC", "DEFAULT");
				if(!listDTCOrderFlag.isEmpty()){
					Element eleCommonCode=listDTCOrderFlag.get(0);
					strDTCOFlag=eleCommonCode.getAttribute(VSIConstants.ATTR_CODE_SHORT_DESCRIPTION);
					if(!YFCCommon.isStringVoid(strDTCOFlag)&&"Y".equalsIgnoreCase(strDTCOFlag)){

						bApplyFraudVerHold=false;
					}
				}
				//OMS-1707 End
				String strOrderType = eleOrder.getAttribute(VSIConstants.ATTR_ORDER_TYPE);
				String strEntryType=eleOrder.getAttribute(VSIConstants.ATTR_ENTRY_TYPE);
				NodeList orderLineList = inXML.getElementsByTagName(VSIConstants.ELE_ORDER_LINE);
				Element eleOrderLine = (Element)orderLineList.item(0);	
				String lineType = eleOrderLine.getAttribute(VSIConstants.ATTR_LINE_TYPE);	
				Element elePriceInfo=XMLUtil.getFirstElementByName(eleOrder, VSIConstants.ELE_PRICE_INFO);
				if(!YFCCommon.isStringVoid(strOrderType) && ((VSIConstants.ATTR_ORDER_TYPE_POS).equalsIgnoreCase(strOrderType) 
						||(VSIConstants.MARKETPLACE).equalsIgnoreCase(strOrderType)|| (VSIConstants.WHOLESALE).equalsIgnoreCase(strOrderType)) ){
					//order is a POS or Marketplace order
					bApplyFraudVerHold=false;
				}else if(elePriceInfo!=null && SCXmlUtil.getDoubleAttribute(elePriceInfo, VSIConstants.ATTR_TOTAL_AMOUNT)<=0.00
						&& !YFCCommon.isStringVoid(lineType) && (("SHIP_TO_HOME").equals(lineType) || ("PICK_IN_STORE").equals(lineType)|| ("SHIP_TO_STORE").equals(lineType)) ){
							bApplyFraudVerHold=false;
					
				}
				else if (!YFCCommon.isStringVoid(strOrderType)&&!YFCCommon.isStringVoid(lineType)&&(lineType.equals("PICK_IN_STORE")||lineType.equals("SHIP_TO_STORE"))){

					bApplyFraudVerHold=false;
				}// end of if callcenter
				// end of if for eleOrder
			}
			catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new YFSException();
			}
		}
		if(log.isDebugEnabled()){
		log.verbose("VSIApplyFraudVerificationHoldSTH.evaluateCondition output: " +bApplyFraudVerHold );
		}
		return bApplyFraudVerHold;
	}

	@Override
	public void setProperties(Map arg0) {
		// TODO Auto-generated method stub

	}

}
