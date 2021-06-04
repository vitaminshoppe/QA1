package com.vsi.oms.api.mixedcart;

import java.io.IOException;
import java.rmi.RemoteException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.riskified.RiskifiedClient;
import com.riskified.RiskifiedError;
import com.riskified.models.Address;
import com.riskified.models.ChargeFreePaymentDetails;
import com.riskified.models.ClientDetails;
import com.riskified.models.CreditCardPaymentDetails;
import com.riskified.models.Customer;
import com.riskified.models.DiscountCode;
import com.riskified.models.LineItem;
import com.riskified.models.Order;
import com.riskified.models.PaypalPaymentDetails;
import com.riskified.models.Response;
import com.riskified.models.ShippingLine;
import com.riskified.validations.FieldBadFormatException;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.vsi.oms.utils.VSIConstants;
import com.vsi.oms.utils.VSIUtils;
import com.vsi.oms.utils.XMLUtil;
import com.yantra.interop.japi.YIFApi;
import com.yantra.interop.japi.YIFClientCreationException;
import com.yantra.interop.japi.YIFClientFactory;
import com.yantra.yfc.core.YFCObject;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;

public class VSIApplyFraudMixedOrder {
	
	private static YFCLogCategory log = YFCLogCategory.instance(VSIApplyFraudMixedOrder.class);
	private static final String TAG = VSIApplyFraudMixedOrder.class.getSimpleName();
	YIFApi api;
	
	public void mixedOrderFraudRequest(YFSEnvironment env, Document inXML){
		
		printLogs("================Inside VSIApplyFraudMixedOrder Class and mixedOrderFraudRequest Method================");
		printLogs("Printing Input XML :"+SCXmlUtil.getString(inXML));
		
		String strOrderHeaderKey=null;
		String strOrderNo=null;
		
		try{			
			Element eleInput=inXML.getDocumentElement();
			strOrderHeaderKey=eleInput.getAttribute(VSIConstants.ATTR_ORDER_HEADER_KEY);
			
			Document getOrderListIp = XMLUtil.createDocument(VSIConstants.ELE_ORDER);
			getOrderListIp.getDocumentElement().setAttribute(VSIConstants.ATTR_ORDER_HEADER_KEY, strOrderHeaderKey);
			
			printLogs("Input to getOrderList API: "+SCXmlUtil.getString(getOrderListIp));
			Document getOrderListOp = VSIUtils.invokeAPI(env, "global/template/api/VSIGetOrderListSOForHold.xml", VSIConstants.API_GET_ORDER_LIST, getOrderListIp);
			printLogs("Output from getOrderList API: "+SCXmlUtil.getString(getOrderListOp));
			
			if(getOrderListOp != null){
				
				Element eleOrderOutput = (Element) getOrderListOp.getDocumentElement().getElementsByTagName(VSIConstants.ELE_ORDER).item(0);
				if(!YFCObject.isVoid(eleOrderOutput)){					
					strOrderNo=eleOrderOutput.getAttribute(VSIConstants.ATTR_ORDER_NO);
					
					//OMS-2046 START		
					//OMS-2419 -- Start
					int iRiskifiedCount = 0;
					int iRiskifiedCountCC = 0;
					boolean isRiskifiedCallReqd = false;
					Element eleOrderExtn = SCXmlUtil.getChildElement(eleOrderOutput, VSIConstants.ELE_EXTN);
					String strRiskifiedCount = eleOrderExtn.getAttribute("ExtnRiskifiedCount");
					if(YFCCommon.isVoid(strRiskifiedCount)){
						isRiskifiedCallReqd=true;									
					}
					else{									
						iRiskifiedCount=Integer.parseInt(strRiskifiedCount);								
						ArrayList<Element> listRiskifiedCount;
						listRiskifiedCount = VSIUtils.getCommonCodeListWithCodeType(env, "RISKIFIED_CALL_COUNT", null, VSIConstants.ATTR_DEFAULT);
						if(!listRiskifiedCount.isEmpty()){
							Element eleRiskifiedCount=listRiskifiedCount.get(0);
							String strRiskifiedCountCC=eleRiskifiedCount.getAttribute(VSIConstants.ATTR_CODE_SHORT_DESCRIPTION);
							if(!YFCCommon.isVoid(strRiskifiedCountCC)){
								iRiskifiedCountCC = Integer.parseInt(strRiskifiedCountCC);
							}
						}
					
						if(iRiskifiedCount<iRiskifiedCountCC){
							isRiskifiedCallReqd=true;
						}
					}
					if(isRiskifiedCallReqd){
						printLogs("Number of Riskified Requests is within the allowed limit, Riskified Call will be triggerred");
						//OMS-2419 -- End
						
						Order order=constructRiskifiedRequest(getOrderListOp,env);
								
						String chargeType="Fraud Request";
						
						String strOrder = com.riskified.JSONFormater.toJson(order);
			
						printLogs("Riskified Request: "+strOrder);
						
						storeFraudRequestandResponse(env,strOrderHeaderKey,strOrder,chargeType);
								
						//riskified response						
						RiskifiedClient client = new RiskifiedClient();
			
						Response res = client.analyzeOrder(order);
						String strResponse = com.riskified.JSONFormater.toJson(res);
						printLogs("Riskified Response: "+strResponse);
						
						String strStatus=res.getOrder().getStatus();
						chargeType="Fraud Response";
						if(!YFCObject.isVoid(strResponse)){							
							//OMS-2419 -- Start
							updateRiskifiedCount(env,strOrderHeaderKey,iRiskifiedCount);
							//OMS-2419 -- End
									
							storeFraudRequestandResponse(env,strOrderHeaderKey,strResponse,chargeType);
						}
								
						if(!YFCObject.isVoid(strStatus)){							
							processRiskifiedResponse(env, strStatus, strOrderHeaderKey, strOrderNo);
						}else{
							resolveAndApplyHold(env,strOrderHeaderKey);
						}
						//OMS-2046 END
						//OMS-2419 -- Start
					}
					else{
						printLogs("Number of Riskified Requests has exceeded the allowed limit, hence will not be triggerred");
					}
					//OMS-2419 -- End
				}				
			}			
		}catch (YFSException | RemoteException| YIFClientCreationException |ParserConfigurationException e){
			printLogs("Inside YFSException | RemoteException| YIFClientCreationException |ParserConfigurationException");
			resolveAndApplyHold(env,strOrderHeaderKey);
		}catch (ParseException e){
			printLogs("Inside ParseException");
			resolveAndApplyHold(env,strOrderHeaderKey);					
		}catch (RiskifiedError e){
			printLogs("Inside RiskifiedError");
			resolveAndApplyHold(env,strOrderHeaderKey);					
		}catch (IOException e){
			printLogs("Inside IOException");
			resolveAndApplyHold(env,strOrderHeaderKey);					
		}catch (FieldBadFormatException e){
			printLogs("Inside FieldBadFormatException");
			resolveAndApplyHold(env,strOrderHeaderKey);					
		}catch (Exception e){
			printLogs("Inside Exception");
			resolveAndApplyHold(env,strOrderHeaderKey);					
		}
		printLogs("================Exiting VSIApplyFraudMixedOrder Class and mixedOrderFraudRequest Method================");
	}
	
	private void processRiskifiedResponse(YFSEnvironment env,String strResponse, String ohk, String orderNo) 
			throws YFSException, RemoteException, ParserConfigurationException, YIFClientCreationException {
		
		printLogs("Inside processRiskifiedResponse method");
		printLogs("Printing RiskifiedRespnse: "+strResponse);		

		if (strResponse != null ) {
			if (strResponse.contains(VSIConstants.ATTR_STATUS_APPROVE)) {				
				printLogs("Approval Scenario");
				Document orderInput = XMLUtil.createDocument(VSIConstants.ELE_ORDER);
				Element sOrderElement = orderInput.getDocumentElement();
				sOrderElement.setAttribute(VSIConstants.ATTR_ORDER_HEADER_KEY, ohk);
				resolveHold(env,ohk);
				api = YIFClientFactory.getInstance().getApi();
				printLogs("Order will be sent to Schedule Q");
				api.executeFlow(env, "VSIScheduleSTH_Q", orderInput);
				printLogs("Order was sent to Schedule Q");
				//OMS-2088
				sOrderElement.setAttribute(VSIConstants.ATTR_ORDER_NO,orderNo);
				sOrderElement.setAttribute(VSIConstants.FRAUD_RESULT,"Approved");
				try {
					printLogs("Before Invoking VSISendAcceptOrDeclinedToATG service");
					VSIUtils.invokeService(env, "VSISendAcceptOrDeclinedToATG", orderInput);
					printLogs("After Invoking VSISendAcceptOrDeclinedToATG service");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			else if(strResponse.contains(VSIConstants.ATTR_STATUS_DECLINED)) {
				// Cancel Order
				printLogs("Decline Scenario");
				Document changeOrderInput = XMLUtil.createDocument(VSIConstants.ELE_ORDER);
				Element orderElement = changeOrderInput.getDocumentElement();
				orderElement.setAttribute(VSIConstants.ATTR_ACTION, VSIConstants.ACTION_CAPS_CANCEL);
				orderElement.setAttribute(VSIConstants.ATTR_OVERRIDE, VSIConstants.FLAG_Y);
				orderElement.setAttribute(VSIConstants.ATTR_MODIFICATION_REASON_TEXT,
						"Fraud Check Declined");
				orderElement.setAttribute(VSIConstants.ATTR_MODIFICATION_REASON_CODE,
						"Fraud Check Declined");
				orderElement.setAttribute(VSIConstants.ATTR_ORDER_HEADER_KEY, ohk);
				printLogs("changeOrderInput: "+XMLUtil.getXMLString(changeOrderInput));

				VSIUtils.invokeAPI(env, VSIConstants.API_CHANGE_ORDER, changeOrderInput);
				printLogs("Order was cancelled successfully");
				//OMS-2088
				Document orderInput = XMLUtil.createDocument(VSIConstants.ELE_ORDER);
				Element sOrderElement = orderInput.getDocumentElement();
				sOrderElement.setAttribute(VSIConstants.ATTR_ORDER_HEADER_KEY, ohk);
				sOrderElement.setAttribute(VSIConstants.ATTR_ORDER_NO,orderNo);
				sOrderElement.setAttribute(VSIConstants.FRAUD_RESULT,"Declined");
				try {
					printLogs("Before Invoking VSISendAcceptOrDeclinedToATG service");
					VSIUtils.invokeService(env, "VSISendAcceptOrDeclinedToATG", orderInput);
					printLogs("After Invoking VSISendAcceptOrDeclinedToATG service");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			else{
				resolveAndApplyHold(env,ohk);
			}
		}
		printLogs("Exiting processRiskifiedResponse method");
	}
	
	private void resolveHold(YFSEnvironment env, String ohk) {
		
		printLogs("Inside resolveHold method");
		Document docProcessedHold=null;
		Element orderEle=null;
		Element eleOrderHoldTypes=null;
		
		try {
		    docProcessedHold =  XMLUtil.createDocument(VSIConstants.ELE_ORDER);
			orderEle =docProcessedHold.getDocumentElement();
			orderEle.setAttribute("Override", "Y");
			orderEle.setAttribute(VSIConstants.ATTR_ORDER_HEADER_KEY,ohk);
			eleOrderHoldTypes = SCXmlUtil.createChild(orderEle, VSIConstants.ELE_ORDER_HOLD_TYPES);
			Element eleOrderHoldType1 = SCXmlUtil.createChild(eleOrderHoldTypes, VSIConstants.ELE_ORDER_HOLD_TYPE);
			eleOrderHoldType1.setAttribute(VSIConstants.ATTR_HOLD_TYPE,
					"VSI_KOUNT_STH_HOLD");
			eleOrderHoldType1.setAttribute(VSIConstants.ATTR_STATUS, "1300");
			eleOrderHoldType1.setAttribute(VSIConstants.ATTR_REASON_TEXT,
					"Kount Call STH Hold");
			printLogs("Printing processedHoldDoc: "+XMLUtil.getXMLString(docProcessedHold));
			VSIUtils.invokeAPI(env, VSIConstants.API_CHANGE_ORDER, docProcessedHold);	
			printLogs("VSI_KOUNT_STH_HOLD resolved successfully");
		} catch (ParserConfigurationException | YFSException | RemoteException | YIFClientCreationException e) {
			printLogs("Inside ParserConfigurationException | YFSException | RemoteException | YIFClientCreationException in resolveHold method");
			e.printStackTrace();
		}
		printLogs("Exiting resolveHold method");
	}
	
	private void resolveAndApplyHold(YFSEnvironment env, String ohk) {
		
		printLogs("Inside resolveAndApplyHold method");
		Document processedHoldDoc=null;
		Element eleOrder=null;
		Element eleOrderHoldTypes=null;
		Element eleOrderHoldType=null;
		Element eleOrderHoldType1=null;
		try {			
			processedHoldDoc =  XMLUtil.createDocument(VSIConstants.ELE_ORDER);
			eleOrder =processedHoldDoc.getDocumentElement();
			eleOrder.setAttribute("Override", "Y");
			eleOrder.setAttribute(VSIConstants.ATTR_ORDER_HEADER_KEY,ohk);
			eleOrderHoldTypes = SCXmlUtil.createChild(eleOrder, VSIConstants.ELE_ORDER_HOLD_TYPES);
			eleOrderHoldType = SCXmlUtil.createChild(eleOrderHoldTypes, VSIConstants.ELE_ORDER_HOLD_TYPE);
			eleOrderHoldType.setAttribute(VSIConstants.ATTR_HOLD_TYPE,
					"VSI_FRAUD_HOLD");
			eleOrderHoldType.setAttribute(VSIConstants.ATTR_STATUS, "1100");
			eleOrderHoldType.setAttribute(VSIConstants.ATTR_REASON_TEXT,
					"Fraud Verification Hold");
			eleOrderHoldType1 = SCXmlUtil.createChild(eleOrderHoldTypes, VSIConstants.ELE_ORDER_HOLD_TYPE);
			eleOrderHoldType1.setAttribute(VSIConstants.ATTR_HOLD_TYPE,
					"VSI_KOUNT_STH_HOLD");
			eleOrderHoldType1.setAttribute(VSIConstants.ATTR_STATUS, "1300");
			eleOrderHoldType1.setAttribute(VSIConstants.ATTR_REASON_TEXT,
					"Kount Call STH Hold");
			printLogs("Printing processedHoldDoc: "+XMLUtil.getXMLString(processedHoldDoc));
			VSIUtils.invokeAPI(env, VSIConstants.API_CHANGE_ORDER, processedHoldDoc);
			printLogs("Hold was applied successfully");
		}catch (ParserConfigurationException | YFSException | RemoteException | YIFClientCreationException e) {
			e.printStackTrace();
		}
		printLogs("Exiting resolveAndApplyHold method");
	}
	
	private void updateRiskifiedCount(YFSEnvironment env,String strOrderHeaderKey, int iRiskifiedCount)
			throws ParserConfigurationException, YIFClientCreationException,RemoteException {
		
		printLogs("Inside updateRiskifiedCount method");
		printLogs("Current value of RiskifiedCount: "+Integer.toString(iRiskifiedCount));
		
		int iUpdtRiskifiedCount = iRiskifiedCount+1;
		String strUpdtRiskifiedCount = Integer.toString(iUpdtRiskifiedCount);
		printLogs("Updated value of RiskifiedCount: "+strUpdtRiskifiedCount);
		
		Document updateRiskifiedCountDoc =  XMLUtil.createDocument(VSIConstants.ELE_ORDER);
		Element eleRiskifiedCount =updateRiskifiedCountDoc.getDocumentElement();
		eleRiskifiedCount.setAttribute(VSIConstants.ATTR_ORDER_HEADER_KEY,strOrderHeaderKey);
		eleRiskifiedCount.setAttribute("Override", "Y");												
		Element eleRiskifiedExtn = SCXmlUtil.createChild(eleRiskifiedCount, VSIConstants.ELE_EXTN);												
		eleRiskifiedExtn.setAttribute("ExtnRiskifiedCount",strUpdtRiskifiedCount);
		
		printLogs("Change Order Input to update ExtnRiskifiedCount: "
				+ XMLUtil.getXMLString(updateRiskifiedCountDoc));
      
		VSIUtils.invokeAPI(env, VSIConstants.API_CHANGE_ORDER, updateRiskifiedCountDoc);
		
		printLogs("ExtnRiskifiedCount updated successfully");
		
		printLogs("Exiting updateRiskifiedCount method");
	}
	
	public void storeFraudRequestandResponse(YFSEnvironment env,String ohk,String request, String chargeType) 
			throws ParserConfigurationException, YIFClientCreationException, YFSException, RemoteException{
		
		printLogs("Inside storeFraudRequestandResponse method");
		
		Document doc = XMLUtil.createDocument("PaymentRecords");
  		Element elePayRecrds = doc.getDocumentElement();
  		elePayRecrds.setAttribute(VSIConstants.ATTR_ORDER_HEADER_KEY,
  				ohk);
  		elePayRecrds.setAttribute("Record", request);
  		elePayRecrds.setAttribute("ChargeType", chargeType);
  		YIFApi api;
  		api = YIFClientFactory.getInstance().getApi();
  		api.executeFlow(env,"VSIPaymentRecords", doc);
		
  		printLogs("Exiting storeFraudRequestandResponse method");
	}
	
	public Order constructRiskifiedRequest(Document getOrderListOp,
			YFSEnvironment env) throws ParseException, RiskifiedError, IOException, FieldBadFormatException, TransformerException {
		
		printLogs("Inside constructRiskifiedRequest method");
		
		Element eleOrderList=getOrderListOp.getDocumentElement();
		Element eleOrder=SCXmlUtil.getChildElement(eleOrderList, VSIConstants.ELE_ORDER);
		String strEntryType=eleOrder.getAttribute(VSIConstants.ATTR_ENTRY_TYPE);
		Element eleOrderExtn=SCXmlUtil.getChildElement(eleOrder, VSIConstants.ELE_EXTN);
		String strExtnSubscriptionOrder = eleOrderExtn.getAttribute(VSIConstants.ATTR_EXTN_SUBSCRIPTION_ORDER);
		String strExtnOriginalADPOrder = eleOrderExtn.getAttribute(VSIConstants.ATTR_EXTN_ORIGINAL_ADP_ORDER);
		String strExtnBrowserIP = eleOrderExtn.getAttribute(VSIConstants.ATTR_EXTN_BROWSER_IP);
		String strExtnCustomerCreatets = eleOrderExtn.getAttribute(VSIConstants.ATTR_EXTN_CUSTOMER_CREATETS);
		String strExtnAcceptUser = eleOrderExtn.getAttribute(VSIConstants.ATTR_EXTN_ACCEPT_USER);
		String strExtnUserAgent = eleOrderExtn.getAttribute(VSIConstants.ATTR_USER_AGENT);
		String strExtnCheckoutId = eleOrderExtn.getAttribute(VSIConstants.ATTR_EXTN_CHECKOUTID);
		String strExtnAurusToken = eleOrderExtn.getAttribute(VSIConstants.EXTN_AURUS_TOKEN);
		
		String strCustomerFirstName=eleOrder.getAttribute(VSIConstants.ATTR_CUSTOMER_FIRST_NAME);
		String strCustomerLastName=eleOrder.getAttribute(VSIConstants.ATTR_CUSTOMER_LAST_NAME);
		String strBillToID=eleOrder.getAttribute(VSIConstants.ATTR_BILL_TO_ID);
		String createts=eleOrder.getAttribute(VSIConstants.ATTR_CREATETS);
		//OMS-2405 -- Start
		String modifyts=eleOrder.getAttribute(VSIConstants.ATTR_MODIFYTS);
		//OMS-2405 -- End		
		SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss-hh",Locale.US); 

		//order
		Order order = new Order();
		List<LineItem> lineItems = new ArrayList<LineItem>();
		List<DiscountCode> Discount = new ArrayList<DiscountCode>();
		order.setId(eleOrder.getAttribute(VSIConstants.ATTR_ORDER_NO));
		order.setEmail(eleOrder.getAttribute(VSIConstants.ATTR_CUSTOMER_EMAIL_ID));
		
		//OMS-2405 -- Start
		if(!YFCCommon.isVoid(strEntryType) && VSIConstants.ENTRYTYPE_CC.equals(strEntryType)){
			order.setCreatedAt(dt.parse(modifyts));
			order.setUpdatedAt(dt.parse(modifyts));
		}else{
			order.setCreatedAt(dt.parse(createts));
			order.setUpdatedAt(dt.parse(createts));
		}
		//OMS-2405 -- End		
		
		order.setCurrency(VSIConstants.ATTR_CURRENCY);
		//OMS-3078 Changes -- Start
		/*if(!YFCObject.isVoid(strEntryType)&&!YFCObject.isVoid(strExtnCheckoutId)&&!((VSIConstants.ENTRYTYPE_CC).equalsIgnoreCase(strEntryType))){
			order.setCheckoutId(strExtnCheckoutId);
		}*/
		//OMS-3078 Changes -- End
		//Changes for OMS-2177 -- Start
		String strExtnMobileOrder = eleOrderExtn.getAttribute(VSIConstants.ATTR_EXTN_MOBILE_ORDER);
		//Changes for OMS-2177 -- End
		
		//Changes for OMS-2192 -- Start
		String strExtnReorder = eleOrderExtn.getAttribute(VSIConstants.ATTR_EXTN_RE_ORDER);		

		if(!YFCCommon.isVoid(strExtnReorder) && VSIConstants.FLAG_Y.equals(strExtnReorder)){
			order.setSource(VSIConstants.ATTR_REORDER);
		}
		//Changes for OMS-2192 -- End
		else if(!YFCObject.isVoid(strEntryType)&&(VSIConstants.ENTRYTYPE_CC).equalsIgnoreCase(strEntryType)){
			order.setSource(VSIConstants.ATTR_PHONE);
		}
		else if(!YFCObject.isVoid(strExtnSubscriptionOrder)&&VSIConstants.FLAG_Y.equalsIgnoreCase(strExtnSubscriptionOrder)){
			order.setSource(VSIConstants.ATTR_SUBSCRIPTION);
		}		
		//Changes for OMS-2177 -- Start
		else if(!YFCCommon.isVoid(strExtnMobileOrder) && VSIConstants.FLAG_Y.equals(strExtnMobileOrder)){
			String strExtnMobileType = eleOrderExtn.getAttribute(VSIConstants.ATTR_EXTN_MOBILE_TYPE);
			order.setSource(strExtnMobileType);			
		}
		//Changes for OMS-2177 -- End
		else{
			order.setSource(VSIConstants.ATTR_WEB);
		}
		String strAcceptUser=null;
		String strUserAgent=null;
		String strCustomerCreatets=null;
		if(!YFCObject.isVoid(strExtnAcceptUser)){
			 strAcceptUser=strExtnAcceptUser;
		}
		else{
			 strAcceptUser="en-CA";
		}
		
		if(!YFCObject.isVoid(strExtnUserAgent)){
			 strUserAgent=strExtnUserAgent;
		}
		else{
			strUserAgent="Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0)";
		}
		if(!YFCObject.isVoid(strExtnCustomerCreatets)){
			strCustomerCreatets=strExtnCustomerCreatets;
		}
		else{
			strCustomerCreatets=createts;
		}
			
		Element eleOverallTotals=SCXmlUtil.getChildElement(eleOrder, VSIConstants.ELE_OVERALL_TOTALS);
		String strGrandTotal = eleOverallTotals.getAttribute(VSIConstants.ATTR_GRAND_TOTAL);
		String strGrandDiscount = eleOverallTotals.getAttribute(VSIConstants.ATTR_GRAND_DISCOUNT);
		String strGrandShippingCharges=eleOverallTotals.getAttribute(VSIConstants.ATTR_GRAND_SHIPPING_CHARGE);
		double dGrandTotal=0.00;
		double dGrandDiscount=0.00;
		double dGrandShippingCharges=0.00;
		if(!YFCObject.isVoid(strGrandTotal)){
		 dGrandTotal = Double.parseDouble(strGrandTotal);
		}
		if(!YFCObject.isVoid(strGrandDiscount)){
		 dGrandDiscount = Double.parseDouble(strGrandDiscount);
		}
		if(!YFCObject.isVoid(strGrandShippingCharges)){
		 dGrandShippingCharges = Double.parseDouble(strGrandShippingCharges);
		}

		order.setTotalPrice(dGrandTotal);
		order.setTotalDiscounts(dGrandDiscount);
		
		if(eleOrderExtn!=null &&!VSIConstants.ENTRYTYPE_CC.equalsIgnoreCase(strEntryType)){
			String extnSessionId=eleOrderExtn.getAttribute(VSIConstants.ATTR_EXTN_RISKIFIED_SESSION_ID);
			if(!YFCCommon.isVoid(strExtnBrowserIP)&&!VSIConstants.FLAG_Y.equalsIgnoreCase(strExtnSubscriptionOrder)){
				order.setBrowserIp(strExtnBrowserIP);
			}
			else if(!VSIConstants.FLAG_Y.equalsIgnoreCase(strExtnSubscriptionOrder)){
				order.setBrowserIp("50.49.141.248");
			}
			if(!YFCCommon.isVoid(extnSessionId)){
				order.setCartToken(extnSessionId);
			}
		}
		
		order.setReferringSite("null");

		// LineItems
		NodeList nlOrderLines = eleOrder
				.getElementsByTagName(VSIConstants.ELE_ORDER_LINE);
		Element orderLineElement = (Element) getOrderListOp.getElementsByTagName(
				VSIConstants.ELE_ORDER_LINE).item(0);
		int noOfLines = nlOrderLines.getLength();
		String strSCAC=null;
		String strLineType=null;
		int k=1;
		for (int i = 0; i < noOfLines; i++) {
			Element orderLineEle = (Element) nlOrderLines.item(i);
			strLineType = orderLineEle.getAttribute(VSIConstants.ATTR_LINE_TYPE);			
			strSCAC = orderLineEle.getAttribute(VSIConstants.ATTR_CARRIER_SERVICE_CODE);

			Element eleItem = (Element) orderLineEle.getElementsByTagName(
					VSIConstants.ELE_ITEM).item(0);

			Element eleItemDetails = (Element) orderLineEle.getElementsByTagName(
					VSIConstants.ELE_ITEM_DETAILS).item(0);
			Element eleItemExtn = (Element) eleItemDetails.getElementsByTagName(
					VSIConstants.ELE_EXTN).item(0);
			//OMS-968 : End
			Element elelinePrice = (Element) orderLineEle.getElementsByTagName(
					VSIConstants.ELE_LINE_PRICE).item(0);


			String strItemDesc= eleItem.getAttribute(VSIConstants.ATTR_ITEM_DESC);
			String strSkuId= eleItemExtn.getAttribute(VSIConstants.EXTN_ACT_SKU_ID);
			String strItemId= eleItem.getAttribute(VSIConstants.ATTR_ITEM_ID);
			String strBrandTitle= eleItemExtn.getAttribute(VSIConstants.ATTR_EXTN_BRAND_TITLE);
			String strLineTotal=elelinePrice.getAttribute(VSIConstants.ATTR_UNIT_PRICE);
			String strQty=orderLineEle.getAttribute(VSIConstants.ATTR_ORD_QTY);
			String strOrderLineKey = orderLineEle.getAttribute(VSIConstants.ATTR_ORDER_LINE_KEY);
			Element eleFirstDelayDate = XMLUtil.getElementByXPath(getOrderListOp,
					"OrderList/Order/OrderLines/OrderLine[@OrderLineKey='"+strOrderLineKey+"']/OrderDates/OrderDate[@DateTypeId='YCD_FTC_FIRST_PROMISE_DATE']");
			


			int dQty=Integer.parseInt(strQty);

			if(dQty!=0.00){

				double dUnitPrice=Double.parseDouble(strLineTotal);


				LineItem item = new LineItem(dUnitPrice, dQty, strItemDesc, strSkuId);
				item.setBrand(strBrandTitle);
				item.setSku(strItemId);
				item.setProductType("physical");
				item.setProperties(null);
				item.setTaxLines(null);
				String isGiftCardItem = eleItemExtn.getAttribute(VSIConstants.ATTR_EXTN_ITEM_TYPE);

				if (VSIConstants.GIFT_CARD.equals(isGiftCardItem)|| VSIConstants.GIFT_CARD_VAR.equals(isGiftCardItem)) 
				{
					String strsku="giftcard"+k;
					item.setSku(strsku);
					item.setRequiresShipping(true);
					k = k+1;
				}
				boolean bbackorder=false;
				if(!YFCCommon.isVoid(eleFirstDelayDate)){
					String strActualDate=eleFirstDelayDate.getAttribute(VSIConstants.ATTR_ACTUAL_DATE);
					String strExpectedDate=eleFirstDelayDate.getAttribute(VSIConstants.ATTR_EXPECTED_DATE);
					String strShipmentDateFormat=null;
					if(!YFCCommon.isVoid(strActualDate)||!YFCCommon.isVoid(strExpectedDate)){
						if(!YFCCommon.isVoid(strActualDate)){
							 strShipmentDateFormat=strActualDate.substring(0, strActualDate.indexOf("T")).replaceAll("-", "");
						}
						else if(!YFCCommon.isVoid(strExpectedDate)){
							 strShipmentDateFormat=strExpectedDate.substring(0, strExpectedDate.indexOf("T")).replaceAll("-", "");
						}

						DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
						Date date = new Date();	
						String currentdate = dateFormat.format(date);
						Date dcurrentdate = dateFormat.parse(currentdate);
						Calendar c = Calendar.getInstance();
						c.setTime(dcurrentdate);
						c.add(Calendar.DAY_OF_MONTH, 1);
						Date currentDatePlusTwo = c.getTime();
						Date dstrShipmentDateFormat = dateFormat.parse(strShipmentDateFormat);
						if(dstrShipmentDateFormat.after(currentDatePlusTwo)){
							bbackorder=true;							
						}
					}
				}
				if(bbackorder&&!YFCObject.isVoid(strExtnOriginalADPOrder)&&VSIConstants.FLAG_Y.equalsIgnoreCase(strExtnOriginalADPOrder)){
					item.setSubCategory(VSIConstants.ATTR_INITIAL_SUBSCRIPTION +" and backorder");
				}
				else if(bbackorder){
					item.setSubCategory("backorder");
				}
				else if(!YFCObject.isVoid(strExtnOriginalADPOrder)&&VSIConstants.FLAG_Y.equalsIgnoreCase(strExtnOriginalADPOrder)){
					item.setSubCategory(VSIConstants.ATTR_INITIAL_SUBSCRIPTION);
				}
				lineItems.add(item);

				//discount

				NodeList ndlLineCharges  = orderLineEle.getElementsByTagName(VSIConstants.ELE_LINE_CHARGE);

				for(int j= 0; j < ndlLineCharges.getLength(); j++){
					Element eleLineCharge = (Element)ndlLineCharges.item(j);

					if(!YFCObject.isVoid(eleLineCharge)){
						String strChargeCategory = eleLineCharge.getAttribute(VSIConstants.ATTR_CHARGE_CATEGORY);
						if(VSIConstants.DISCOUNT.equalsIgnoreCase(strChargeCategory) || VSIConstants.DISCOUNT_CATEGORY_ADJ.equalsIgnoreCase(strChargeCategory)){
							String strChargeAmount = eleLineCharge.getAttribute(VSIConstants.ATTR_CHARGE_AMOUNT);
							String strChargeName = eleLineCharge.getAttribute(VSIConstants.ATTR_CHARGE_NAME);
							double dstrChargeAmount=Double.parseDouble(strChargeAmount);
							Discount.add(new DiscountCode(dstrChargeAmount,strChargeName));
						}
					}
				}
			}
		}
		
		//Changes for OMS-2193 -- Start
		Element eleHeaderCharges = SCXmlUtil.getChildElement(eleOrder, VSIConstants.ELE_HEADER_CHARGES);
		NodeList ndlHeaderCharge = eleHeaderCharges.getElementsByTagName(VSIConstants.ELE_HEADER_CHARGE);
		for(int m= 0; m < ndlHeaderCharge.getLength(); m++){
			
			Element eleHeaderCharge = (Element)ndlHeaderCharge.item(m);
			if(!YFCCommon.isVoid(eleHeaderCharge)){
				String strHdrChrgCategory = eleHeaderCharge.getAttribute(VSIConstants.ATTR_CHARGE_CATEGORY);
				if(VSIConstants.DISCOUNT_CATEGORY_ADJ.equals(strHdrChrgCategory)){
					String strHdrChrgName = eleHeaderCharge.getAttribute(VSIConstants.ATTR_CHARGE_NAME);
					String strHdrChrgAmnt = eleHeaderCharge.getAttribute(VSIConstants.ATTR_CHARGE_AMOUNT);
					double dHdrChrgAmnt = Double.parseDouble(strHdrChrgAmnt);
					Discount.add(new DiscountCode(dHdrChrgAmnt,strHdrChrgName));
				}
			}			
		}
		//Changes for OMS-2193 -- End
		order.setLineItems(lineItems);
		order.setDiscountCodes(Discount);


		// ShippingLines
		/*if(VSIConstants.LINETYPE_PUS.equalsIgnoreCase(strLineType) || VSIConstants.LINETYPE_STS.equalsIgnoreCase(strLineType)){
			//order.setSource(VSIConstants.ATTR_WEB);
			order.setShippingLines(Arrays.asList(new ShippingLine(dGrandShippingCharges, VSIConstants.ATTR_STORE_PICK_UP)));

		}
		else{*/
			if(YFCCommon.isVoid(strSCAC)){
				strSCAC="STANDARD";
			}
			order.setShippingLines(Arrays.asList(new ShippingLine(dGrandShippingCharges, "mixed")));
		//}

		//PaymentDetails
		boolean bvoucher=false;
		boolean bcreditCard=false;
		boolean bpaypal=false;
		boolean bgcCard=false;
		//OMS-3187 Changes -- Start
		 boolean bApplePay=false;
		//OMS-3187 Changes -- End
		//OMS-3368 Changes -- Start
		 boolean bGooglePay=false;
		//OMS-3368 Changes -- End
		NodeList nlPaymtMthd = eleOrder
				.getElementsByTagName(VSIConstants.ELE_PAYMENT_METHOD);
		int pymthdCount = nlPaymtMthd.getLength();
		//String strPaymentTechCode=null;
		List<ChargeFreePaymentDetails> chargefreePaymentDetails= new ArrayList<ChargeFreePaymentDetails>();
		for (int n = 0; n < pymthdCount; n++) {
			Element paymtMthdEle = (Element) nlPaymtMthd.item(n);
			String strAvsResp=SCXmlUtil.getXpathAttribute(eleOrder, "Extn/TNSAuthRecordsList/TNSAuthRecords/@AvsGatewayCode");
			String strCVVResp=SCXmlUtil.getXpathAttribute(eleOrder, "Extn/TNSAuthRecordsList/TNSAuthRecords/@CVVResp");
			if(YFCCommon.isStringVoid(strCVVResp)){
				strCVVResp="null";
			}
			//log.debug("AvsResp : "+ strAvsResp);
			/*if(!YFCCommon.isStringVoid(strAvsResp)){
				//Sample value for strAvsResp AvsResp-I8, fetch the value after -
				String[] arrAvsResp= strAvsResp.split("-");
				if(arrAvsResp.length==2){
					 strPaymentTechCode=arrAvsResp[1];
					
				}
			}*/
			if (paymtMthdEle != null) {

				String strPaymentType = paymtMthdEle
						.getAttribute(VSIConstants.ATTR_PAYMENT_TYPE);
				String strCreditCardNumber=null;
				if (strPaymentType != null
						&& strPaymentType.equalsIgnoreCase(VSIConstants.PAYMENT_MODE_CC)) {
					String strCreditCarType = paymtMthdEle
							.getAttribute(VSIConstants.ATTR_CREDIT_CARD_TYPE);
					Element eleExtnPayment = SCXmlUtil.getChildElement(paymtMthdEle, VSIConstants.ELE_EXTN);
					if(!YFCCommon.isVoid(strExtnAurusToken) && VSIConstants.FLAG_Y.equals(strExtnAurusToken)&&!YFCCommon.isVoid(eleExtnPayment)&&!YFCCommon.isStringVoid(eleExtnPayment.getAttribute("ExtnAurusReferralNUM"))){
						strCreditCardNumber=eleExtnPayment.getAttribute("ExtnAurusReferralNUM");							
					}
					else{
						strCreditCardNumber = paymtMthdEle.getAttribute(VSIConstants.ATTR_PAYMENT_REFERENCE_2);
					}
					//OMS-3331 Changes -- Start
					//OMS-3187 Changes -- Start
					if(!YFCCommon.isVoid(eleExtnPayment)) {
						String strContactLess=eleExtnPayment.getAttribute("ExtnContactLess");
						if(!YFCCommon.isVoid(strContactLess) && "Apple".equals(strContactLess)) {
							order.setGateway("aurus_applepay");
							bApplePay=true;
						}
						//OMS-3368 Changes -- Start
						else if(!YFCCommon.isVoid(strContactLess) && "Google".equals(strContactLess)) {
							order.setGateway("aurus_googlepay");
							bGooglePay=true;
						}
						//OMS-3368 Changes -- End
					}					
					else if(!YFCCommon.isVoid(strExtnAurusToken) && VSIConstants.FLAG_Y.equals(strExtnAurusToken)){
					//OMS-3187 Changes -- End
						order.setGateway("Aurus");
					}
					else{
						order.setGateway("TNS");
					}
					//OMS-3331 Changes -- End
	                String creditCardBin=null;
	                String strCardNo=null;
	                //OMS-3331 Changes -- Start
                    if(bApplePay) {
                    	creditCardBin="null";
                    	strCardNo=paymtMthdEle.getAttribute("DisplayCreditCardNo");
                    }
                  //OMS-3368 Changes -- Start
                    else if(bGooglePay) {
                    	creditCardBin="null";
                    	strCardNo=paymtMthdEle.getAttribute("DisplayCreditCardNo");
                    	strAvsResp="null";
                    	strCVVResp="null";
                    }
                  //OMS-3368 Changes -- End 
                    else if(!YFCCommon.isVoid(strCreditCardNumber) ){
                    //OMS-3331 Changes -- End
	                	creditCardBin=strCreditCardNumber.substring(0, 6);
						strCardNo=strCreditCardNumber.substring(strCreditCardNumber.length()-4);
	                }
					String strTotalAuthorized = paymtMthdEle
							.getAttribute(VSIConstants.ATTR_TOTAL_AUTHORIZED);
					if(!YFCCommon.isVoid(strTotalAuthorized)&& Double.parseDouble(strTotalAuthorized)!=0.0){
						 bcreditCard=true;
						 order.setPaymentDetails(Arrays.asList(new CreditCardPaymentDetails(creditCardBin, strAvsResp, strCVVResp, strCardNo, strCreditCarType)));
					}

				}
				if (strPaymentType != null
						&& strPaymentType.equalsIgnoreCase(VSIConstants.PAYMENT_MODE_PP)) {
					
					order.setGateway("Paypal");
					bpaypal=true;
					String strPayerEmail = paymtMthdEle.getAttribute(VSIConstants.ATTR_PAYMENT_REFERENCE_3);
					order.setPaymentDetails(Arrays.asList(new PaypalPaymentDetails(strPayerEmail, "verified", "unconfirmed", "Eligible")));
				}
				if (strPaymentType != null
						&& strPaymentType.equalsIgnoreCase(VSIConstants.PAYMENT_MODE_GC)) {

					String strTotalAuthorized = paymtMthdEle
							.getAttribute(VSIConstants.ATTR_MAX_CHARGE_LIMIT);
					order.setGateway("giftcard");
				
					double dTotalAuthorized=Double.parseDouble(strTotalAuthorized);
					if(!YFCCommon.isVoid(dTotalAuthorized)&& dTotalAuthorized!=0.0){
						bgcCard=true;
					if(order.getChargeFreePaymentDetails() != null && VSIConstants.PAYMENT_MODE_GC == order.getChargeFreePaymentDetails().getGateway()){
						order.getChargeFreePaymentDetails().setAmount(order.getChargeFreePaymentDetails().getAmount()+ dTotalAuthorized);
					}else{
						ChargeFreePaymentDetails chargeFreePaymentDetails=new ChargeFreePaymentDetails(VSIConstants.PAYMENT_MODE_GC, dTotalAuthorized);
						chargefreePaymentDetails.add(chargeFreePaymentDetails);						
					}
				}
				}
				if (strPaymentType != null
						&& strPaymentType.equalsIgnoreCase(VSIConstants.PAYMENT_MODE_VOUCHERS)) {

					String strTotalAuthorized = paymtMthdEle
							.getAttribute(VSIConstants.ATTR_TOTAL_CHARGED);
					order.setGateway("healthyawards");
					
					double dTotalAuthorized=Double.parseDouble(strTotalAuthorized);
					if(!YFCCommon.isVoid(dTotalAuthorized)&& dTotalAuthorized!=0.0){
						bvoucher=true;
					if(order.getChargeFreePaymentDetails() != null && VSIConstants.PAYMENT_MODE_VOUCHERS == order.getChargeFreePaymentDetails().getGateway()){
						order.getChargeFreePaymentDetails().setAmount(order.getChargeFreePaymentDetails().getAmount()+ dTotalAuthorized);
					}else{
						ChargeFreePaymentDetails chargeFreePaymentDetails=new ChargeFreePaymentDetails(VSIConstants.PAYMENT_MODE_VOUCHERS, dTotalAuthorized);
						chargefreePaymentDetails.add(chargeFreePaymentDetails);						
					}
				}
				}
			}
		}

		if(chargefreePaymentDetails.size()>0) {
			ChargeFreePaymentDetails cfg=createChargeFreePaymentDetailsObj(chargefreePaymentDetails);
			order.setChargeFreePaymentDetails(cfg);
		}
		//OMS--3187 Changes -- Start
		if(bApplePay&&bcreditCard&&(bvoucher||bgcCard)) {
			order.setGateway("aurus_applepay");
		}
		//OMS-3368 Changes -- Start
		else if(bGooglePay&&bcreditCard&&(bvoucher||bgcCard)) {
			order.setGateway("aurus_googlepay");
		}
		//OMS-3368 Changes -- End
		else if(bcreditCard&&(bvoucher||bgcCard)){
		//OMS--3187 Changes -- End
			if(!YFCCommon.isVoid(strExtnAurusToken) && VSIConstants.FLAG_Y.equals(strExtnAurusToken)){
				order.setGateway("Aurus");
			}
			else{
				order.setGateway("TNS");
			}
		}
		else if(bpaypal&&(bvoucher||bgcCard))
		{
			order.setGateway("paypal");
		}
		else if(bvoucher&&bgcCard){
			order.setGateway("giftcard");
		}
		else if(bvoucher){
			order.setGateway("healthyawards");
		}
		else if(bvoucher||bgcCard){
			order.setGateway("giftcard");
		}
		//BillingAddress
		Element eleBillToAdd = (Element) eleOrder.getElementsByTagName(
				VSIConstants.ELE_PERSON_INFO_BILL_TO).item(0);
		if (eleBillToAdd != null) {
			String strFirstName = eleBillToAdd.getAttribute(VSIConstants.ATTR_FIRST_NAME);
			String strLastname= eleBillToAdd.getAttribute(VSIConstants.ATTR_LAST_NAME);
			String address1 = eleBillToAdd.getAttribute(VSIConstants.ATTR_ADDRESS1);
			String address2 = eleBillToAdd.getAttribute(VSIConstants.ATTR_ADDRESS2);
			String city = eleBillToAdd.getAttribute(VSIConstants.ATTR_CITY);
			String state = eleBillToAdd.getAttribute(VSIConstants.ATTR_STATE);
			String country = eleBillToAdd.getAttribute(VSIConstants.ATTR_COUNTRY);
			String postalCode = eleBillToAdd.getAttribute(VSIConstants.ATTR_ZIPCODE);
			String phone = eleBillToAdd.getAttribute(VSIConstants.ATTR_DAY_PHONE);
			if(YFCCommon.isVoid(strFirstName)){
				strFirstName="Dummy";
			}
			if(YFCCommon.isVoid(strLastname)){
				strLastname="Dummy";
			}
			if(YFCCommon.isVoid(address1)){
				address1="Dummy";
			}
			if(YFCCommon.isVoid(address2)){
				address2="null";
			}
			if(YFCCommon.isVoid(phone)){
				phone="1234567891";
			}

			Address address = new Address(strFirstName, strLastname, address1, city, phone, null);
			address.setAddress2(address2);

			address.setCountryCode(country);

			address.setProvinceCode(state);
			address.setZip(postalCode);
			order.setBillingAddress(address);
		}

		//ShippingAddress
		
		//OMS-3075 Changes -- Start
		String strFirstName=null;
		String strLastname=null;
		
		Element eleOrdLnIn=XMLUtil.getElementByXPath(getOrderListOp, "/OrderList/Order/OrderLines/OrderLine[@LineType='PICK_IN_STORE' or @LineType='SHIP_TO_STORE']");
		if(!YFCCommon.isVoid(eleOrdLnIn)){
			Element elePrsnShpTo=SCXmlUtil.getChildElement(eleOrdLnIn, VSIConstants.ELE_PERSON_INFO_SHIP_TO);
			if(!YFCCommon.isVoid(elePrsnShpTo)){
				//OMS-3093 Changes -- Start
				strFirstName = eleOrder.getAttribute(VSIConstants.ATTR_CUSTOMER_FIRST_NAME);
				strLastname= eleOrder.getAttribute(VSIConstants.ATTR_CUSTOMER_LAST_NAME);				
				//OMS-3093 Changes -- End				
						
				String address1 = elePrsnShpTo.getAttribute(VSIConstants.ATTR_ADDRESS1);
				String address2 = elePrsnShpTo.getAttribute(VSIConstants.ATTR_ADDRESS2);
				String city = elePrsnShpTo.getAttribute(VSIConstants.ATTR_CITY);
				String state = elePrsnShpTo.getAttribute(VSIConstants.ATTR_STATE);
				String country = elePrsnShpTo.getAttribute(VSIConstants.ATTR_COUNTRY);
				String postalCode = elePrsnShpTo.getAttribute(VSIConstants.ATTR_ZIPCODE);
				String phone = elePrsnShpTo.getAttribute(VSIConstants.ATTR_DAY_PHONE);
				
			
				/*Element eleShipToAdd = (Element) orderLineElement.getElementsByTagName(VSIConstants.ELE_PERSON_INFO_SHIP_TO).item(0);
				if (eleShipToAdd != null) {
				 */			
				
				if(YFCCommon.isVoid(strFirstName)){
					strFirstName="Dummy";
				}
				if(YFCCommon.isVoid(strLastname)){
					strLastname="Dummy";
				}
				if(YFCCommon.isVoid(address1)){
					address1="Dummy";
				}
				if(YFCCommon.isVoid(address2)){
					address2="null";
				}
				if(YFCCommon.isVoid(phone)){
					phone="1234567891";
				}
				
				Address address = new Address(strFirstName, strLastname, address1, city, phone, null);
				address.setAddress2(address2);
	
				address.setCountryCode(country);
	
				address.setProvinceCode(state);
				address.setZip(postalCode);
				order.setShippingAddress(address);
			  /*}
				else{
				
					Element eleShipToAddress = SCXmlUtil.getChildElement(eleOrder, VSIConstants.ELE_PERSON_INFO_SHIP_TO);
					if (eleShipToAddress != null) {
						
						strFirstName = eleShipToAddress.getAttribute(VSIConstants.ATTR_FIRST_NAME);
						strLastname= eleShipToAddress.getAttribute(VSIConstants.ATTR_LAST_NAME);
						
						String address1 = eleShipToAddress.getAttribute(VSIConstants.ATTR_ADDRESS1);
						String address2 = eleShipToAddress.getAttribute(VSIConstants.ATTR_ADDRESS2);
						String city = eleShipToAddress.getAttribute(VSIConstants.ATTR_CITY);
						String state = eleShipToAddress.getAttribute(VSIConstants.ATTR_STATE);
						String country = eleShipToAddress.getAttribute(VSIConstants.ATTR_COUNTRY);
						String postalCode = eleShipToAddress.getAttribute(VSIConstants.ATTR_ZIPCODE);
						String phone = eleShipToAddress.getAttribute(VSIConstants.ATTR_DAY_PHONE);
						if(YFCCommon.isVoid(strFirstName)){
							strFirstName="Dummy";
						}
						if(YFCCommon.isVoid(strLastname)){
							strLastname="Dummy";
						}
						if(YFCCommon.isVoid(address1)){
							address1="Dummy";
						}
						if(YFCCommon.isVoid(address2)){
							address2="null";
						}
						if(YFCCommon.isVoid(phone)){
							phone="1234567891";
						}
						
						Address address = new Address(strFirstName, strLastname, address1, city, phone, null);
						address.setAddress2(address2);
		
						address.setCountryCode(country);
		
						address.setProvinceCode(state);
						address.setZip(postalCode);
						order.setShippingAddress(address);
					}			
			}*/
			}
		}		
		//OMS-3075 Changes -- End
		
		boolean btrue=false;
		// Customer
		if(YFCCommon.isVoid(strCustomerFirstName)){
			strCustomerFirstName="Dummy";
		}
		if(YFCCommon.isVoid(strCustomerLastName)){
			strCustomerLastName="Dummy";
		}
		Customer customer = new Customer(
				eleOrder.getAttribute(VSIConstants.ATTR_CUSTOMER_EMAIL_ID),
				strCustomerFirstName,
				strCustomerLastName,
				strBillToID,
				dt.parse(strCustomerCreatets),
				btrue,
				null);
		customer.setSocial(null);
		
		//OMS-2375
		String strGuestCheckOut = eleOrderExtn.getAttribute(VSIConstants.ATTR_CUSTOMER_TIER);
		if(!YFCCommon.isVoid(strGuestCheckOut) && VSIConstants.ATTR_ORDER_TYPE_VALUE.equals(strEntryType)){
			customer.setAccountType(strGuestCheckOut);
		} else{
			customer.setAccountType(VSIConstants.ATTR_CHECKOUT_TYPE_REGISTERED);
		}
		//OMS-2375 END
		
		order.setCustomer(customer);
		order.setName(strCustomerFirstName);
		//client details
		if((!YFCObject.isVoid(strEntryType)&&!(VSIConstants.ENTRYTYPE_CC).equalsIgnoreCase(strEntryType))&&(!VSIConstants.FLAG_Y.equalsIgnoreCase(strExtnSubscriptionOrder))){
			ClientDetails clientDetails = new ClientDetails();
			clientDetails.setAcceptLanguage(strAcceptUser);
			clientDetails.setUserAgent(strUserAgent);
			order.setClientDetails(clientDetails);
		}
		
		printLogs("Exiting constructRiskifiedRequest method");
		
		return order;		
	}
	
	private ChargeFreePaymentDetails createChargeFreePaymentDetailsObj (List<ChargeFreePaymentDetails> cfp){
		
		printLogs("Inside createChargeFreePaymentDetailsObj method");
		
		ChargeFreePaymentDetails cfpd = null;
		List<String> tempList = new ArrayList<String>();
		Double amount =0.0 ;
		for(int i=0 ; i<cfp.size() ;i++) {
			if(!tempList.contains(cfp.get(i).getGateway())) {
				tempList.add(cfp.get(i).getGateway());
			}
			amount=amount+cfp.get(i).getAmount();
		}
		StringBuffer sb = new StringBuffer();
		for(int i=0; i<tempList.size();i++) {
			sb.append(tempList.get(i));
			if(i<tempList.size()-1)
				sb.append(" and ");
		}

		cfpd= new ChargeFreePaymentDetails(sb.toString(), amount);
		
		printLogs("Exiting createChargeFreePaymentDetailsObj method");
		
		return cfpd;
	}
	
	private void printLogs(String mesg) {
		if(log.isDebugEnabled()){
			log.debug(TAG +" : "+mesg);
		}
	}

}
