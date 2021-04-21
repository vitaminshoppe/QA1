package com.vsi.som.shipment;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Calendar;

import javax.xml.parsers.ParserConfigurationException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.vsi.oms.utils.VSIConstants;
import com.vsi.oms.utils.VSIUtils;
import com.vsi.oms.utils.XMLUtil;
import com.yantra.interop.japi.YIFApi;
import com.yantra.interop.japi.YIFClientCreationException;
import com.yantra.interop.japi.YIFClientFactory;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.core.YFSSystem;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;

public class VSISOMFlexCustomerReceipt implements VSIConstants{
	
	private static YFCLogCategory log = YFCLogCategory.instance(VSISOMFlexCustomerReceipt.class);
	private static final String TAG = VSISOMFlexCustomerReceipt.class.getSimpleName();
	
	public Document sendCustomerReceipt(YFSEnvironment env, Document inXML){
		
		printLogs("================Inside VSISOMFlexCustomerReceipt class and sendCustomerReceipt Method================================");
		printLogs("Printing Input XML: "+SCXmlUtil.getString(inXML));
		
		try{
			//OMS-3289 Changes -- Start
			Element eleInvoiceDtl=inXML.getDocumentElement();
			Element eleInvoiceHdr=SCXmlUtil.getChildElement(eleInvoiceDtl, ELE_INVOICE_HEADER);
			Element eleShipment=SCXmlUtil.getChildElement(eleInvoiceHdr, ELE_SHIPMENT);
			//OMS-3289 Changes -- End
			String strShipmentKey=eleShipment.getAttribute(ATTR_SHIPMENT_KEY);			
			
			Document docGetShipmentListIn=XMLUtil.createDocument(ELE_SHIPMENT);
			Element eleGetShipmentList = docGetShipmentListIn.getDocumentElement();
			eleGetShipmentList.setAttribute(ATTR_SHIPMENT_KEY, strShipmentKey);
			
			printLogs("Input XML to API_GET_SHIPMENT_LIST: "+SCXmlUtil.getString(docGetShipmentListIn));			
			Document docGetShipmentListOut = VSIUtils.invokeAPI(env,SHIPMENT_LIST_PICK_PACK_TEMPLATE,API_GET_SHIPMENT_LIST, docGetShipmentListIn);
			printLogs("Output XML from API_GET_SHIPMENT_LIST: "+SCXmlUtil.getString(docGetShipmentListOut));
			
			Element eleGetShipmentListOut = (Element) docGetShipmentListOut.getElementsByTagName(ELE_SHIPMENTS).item(0);			
			int intTotalNoOfShipment= Integer.parseInt(eleGetShipmentListOut.getAttribute(ATTR_TOTAL_NUMBER_OF_RECORDS));
			Element eleShipmentOut=null;
			Element eleShpmntExtn= null;
			String strShipmentStatus = null;
			String strCustRcptFlag=null;
			String strUserId= "";
			if (intTotalNoOfShipment > 0) {
				eleShipmentOut= (Element) docGetShipmentListOut.getElementsByTagName(ELE_SHIPMENT).item(0);
				Element ShipmentStatusAudits = SCXmlUtil.getChildElement(eleShipmentOut, "ShipmentStatusAudits");
				int totalNoOfRecords= Integer.parseInt(ShipmentStatusAudits.getAttribute(ATTR_TOTAL_NUMBER_OF_RECORDS));

				if (totalNoOfRecords>0) {
					NodeList nlShipmentStatusAudit= ShipmentStatusAudits.getElementsByTagName("ShipmentStatusAudit");
					for (int i = 0; i < nlShipmentStatusAudit.getLength(); i++) {
						Element ShipmentStatusAudit = (Element) nlShipmentStatusAudit.item(i);
						if (ShipmentStatusAudit.getAttribute("NewStatus").equalsIgnoreCase("1400")) {
							strUserId= ShipmentStatusAudit.getAttribute("Createuserid");
							break;
						}								
					}
				}				
				eleShpmntExtn=SCXmlUtil.getChildElement(eleShipmentOut, ELE_EXTN);
				 
				if(!YFCCommon.isVoid(eleShpmntExtn)){
					strCustRcptFlag=eleShpmntExtn.getAttribute("ExtnCustomerReceiptFlag");
				}
				strShipmentStatus= eleShipmentOut.getAttribute(ATTR_STATUS);
			}
			
			printLogs("Printing Customer Flex Receipt Flag "+strCustRcptFlag);
			printLogs("Shipment Status "+strShipmentStatus);
			
			if(FLAG_Y.equals(strCustRcptFlag)){
				printLogs("Customer Flex Receipt has already been sent for this Shipment");
				printLogs("Exiting VSISOMFlexCustomerReceipt Class and sendCustomerReceipt Method");
				return inXML;
			}else{
				printLogs("Starting Customer Flex Receipt Process for this Shipment");
					
				Document docSortedShpmntIn = XMLUtil.createDocument(ELE_SHIPMENT);
				Element eleSortedShpmntIn = docSortedShpmntIn.getDocumentElement();
				eleSortedShpmntIn.setAttribute(ATTR_SHIPMENT_KEY, strShipmentKey);
					
				printLogs("Input to getSortedShipmentDetails API: "+SCXmlUtil.getString(docSortedShpmntIn));					
				Document docSortedShpmntOut = VSIUtils.invokeAPI(env,"global/template/api/getSortedShipmentDetails_VSISOMFlexCustomerReceipt.xml","getSortedShipmentDetails", docSortedShpmntIn);
				printLogs("Output from getSortedShipmentDetails API: "+SCXmlUtil.getString(docSortedShpmntOut));
					
				Element eleSortedShpmntOut=docSortedShpmntOut.getDocumentElement();
				String strStoreNo=eleSortedShpmntOut.getAttribute(ATTR_SHIP_NODE);
				String strStoreNumber=("00000" + strStoreNo).substring(strStoreNo.length());
					
				Document docGetOrgListIn=XMLUtil.createDocument(ELE_ORGANIZATION);
				Element eleGetOrgListIn=docGetOrgListIn.getDocumentElement();
				eleGetOrgListIn.setAttribute(ATTR_ORG_CODE, strStoreNo);
					
				printLogs("Input to getOrganizationList API: "+SCXmlUtil.getString(docGetOrgListIn));					
				Document docGetOrgListOut = VSIUtils.invokeAPI(env,"getOrganizationList",docGetOrgListIn);
				printLogs("Output from getOrganizationList API: "+SCXmlUtil.getString(docGetOrgListOut));
					
				Element eleGetOrgListOut=docGetOrgListOut.getDocumentElement();
				Element eleOrganization=SCXmlUtil.getChildElement(eleGetOrgListOut, ELE_ORGANIZATION);
				String strStoreName=null;
				String strAddLine1="";
				String strCty="";
				String strPostalCode="";
				String strTerritory="";
				//OMS-3247 Changes -- Start
				String strStorePhNo="";
				//OMS-3247 Changes -- End
				if(!YFCCommon.isVoid(eleOrganization)){
					String strOrgName=eleOrganization.getAttribute(ATTR_ORGANIZATION_NAME);						
					if(!YFCCommon.isVoid(strOrgName) && strOrgName.contains(",")){
						strStoreName=EMAIL_HEADER+strOrgName.substring(0, strOrgName.lastIndexOf(","));
					}else if(!YFCCommon.isVoid(strOrgName) && strOrgName.contains(" ")){
						strStoreName=EMAIL_HEADER+strOrgName.substring(0, strOrgName.lastIndexOf(" "));
					}else{
						strStoreName=EMAIL_HEADER+strOrgName;	
					}
						
					Element eleNode=SCXmlUtil.getChildElement(eleOrganization, ELE_NODE);
					if(!YFCCommon.isVoid(eleNode)){
						Element eleShpNdPrsnInfo=SCXmlUtil.getChildElement(eleNode, ELE_SHIP_NODE_PERSON);
						if(!YFCCommon.isVoid(eleShpNdPrsnInfo)){
							strAddLine1=eleShpNdPrsnInfo.getAttribute(ATTR_ADDRESS1);
							strCty=eleShpNdPrsnInfo.getAttribute(ATTR_CITY);
							strPostalCode=eleShpNdPrsnInfo.getAttribute(ATTR_ZIPCODE);
							strTerritory=eleShpNdPrsnInfo.getAttribute(ATTR_STATE);
							//OMS-3247 Changes -- Start
							strStorePhNo=eleShpNdPrsnInfo.getAttribute(ATTR_DAY_PHONE);
							//OMS-3247 Changes -- End
						}
					}						
				}					
				Element eleShipmentLines=SCXmlUtil.getChildElement(eleSortedShpmntOut, ELE_SHIPMENT_LINES);
				Element eleShipmentLine=SCXmlUtil.getChildElement(eleShipmentLines, ELE_SHIPMENT_LINE);					
				Element eleOrder=SCXmlUtil.getChildElement(eleShipmentLine, ELE_ORDER);
				String strOrderNo=eleOrder.getAttribute(ATTR_ORDER_NO);
				String strOrdHdrKey=eleOrder.getAttribute(ATTR_ORDER_HEADER_KEY);
				String strCustEmailID=eleOrder.getAttribute(ATTR_CUSTOMER_EMAIL_ID);
				String strCustPhoneNo=eleOrder.getAttribute("CustomerPhoneNo");
				String strBillToId=eleOrder.getAttribute(ATTR_BILL_TO_ID);
				String strCustFstName=eleOrder.getAttribute(ATTR_CUSTOMER_FIRST_NAME);
				String strCustLstName=eleOrder.getAttribute(ATTR_CUSTOMER_LAST_NAME);
				String strCustName=strCustFstName+" "+strCustLstName;
					
				Element elePriceInfo=SCXmlUtil.getChildElement(eleOrder, ELE_PRICE_INFO);
				String strGrandAmount=elePriceInfo.getAttribute(ATTR_TOTAL_AMOUNT);
					
				Element eleOrdExtn=SCXmlUtil.getChildElement(eleOrder, ELE_EXTN);
				String strCustTier=eleOrdExtn.getAttribute(ATTR_CUSTOMER_TIER);
				String strMembershipStatus="";
				if(!YFCCommon.isVoid(strCustTier)){
					strMembershipStatus=strCustTier;
				}
					
				String strAddrLine1="";
				String strAddrLine2="";
				String strAddrLine3="";
				String strPrsnInfoCity="";
				String strPrsnInfoState="";
				String strPrsnInfoZipCode="";
				Element eleOrdPrsnInfo=SCXmlUtil.getChildElement(eleOrder, ELE_PERSON_INFO_BILL_TO);		//OMS-3249 Change
				if(!YFCCommon.isVoid(eleOrdPrsnInfo)) {
					strAddrLine1=eleOrdPrsnInfo.getAttribute(ATTR_ADDRESS1);
					strAddrLine2=eleOrdPrsnInfo.getAttribute(ATTR_ADDRESS2);
					if(YFCCommon.isVoid(strAddrLine2)) {
						strAddrLine2="";
					}
					strAddrLine3=eleOrdPrsnInfo.getAttribute(ATTR_ADDRESS3);
					if(YFCCommon.isVoid(strAddrLine3)) {
						strAddrLine3="";
					}
					strPrsnInfoCity=eleOrdPrsnInfo.getAttribute(ATTR_CITY);
					strPrsnInfoState=eleOrdPrsnInfo.getAttribute(ATTR_STATE);
					strPrsnInfoZipCode=eleOrdPrsnInfo.getAttribute(ATTR_ZIPCODE);
				}
					
				JSONArray tenders = new JSONArray();
				
				//OMS-3289 Changes -- Start
				Element eleCollectionDetails=SCXmlUtil.getChildElement(eleInvoiceHdr, ELE_COLLECTION_DEATILS);
				NodeList nlCollectionDtl=eleCollectionDetails.getElementsByTagName(ELE_COLLECTION_DETAIL);
				for(int i=0; i<nlCollectionDtl.getLength(); i++) {
					Element eleCollectionDtl=(Element)nlCollectionDtl.item(i);
					String strAmount=eleCollectionDtl.getAttribute(ATTR_AMOUNT_COLLECTED);
					Element elePaymentMethod=SCXmlUtil.getChildElement(eleCollectionDtl, ELE_PAYMENT_METHOD);
					//OMS-3289 Changes -- End
					JSONObject tender = new JSONObject();
					String strPaymentType=elePaymentMethod.getAttribute(ATTR_PAYMENT_TYPE);					
					String strPaymentMethod="";
											
					if(PAYMENT_MODE_CC.equals(strPaymentType)){
						//OMS-3189 Changes -- Start
						Element eleExtnPayment=SCXmlUtil.getChildElement(elePaymentMethod, ELE_EXTN);							
						String strContactLess=eleExtnPayment.getAttribute("ExtnContactLess");
						if(!YFCCommon.isVoid(strContactLess) && "Apple".equals(strContactLess)) {
							strPaymentMethod="Apple Pay";
						}
						else {
							strPaymentMethod=elePaymentMethod.getAttribute(ATTR_CREDIT_CARD_TYPE);
						}
						//OMS-3189 Changes -- End							
						String strLast4Digits=elePaymentMethod.getAttribute("DisplayCreditCardNo");
						String strFstName=elePaymentMethod.getAttribute("FirstName");
						String strLstName=elePaymentMethod.getAttribute("LastName");
						String strCrdExpiry=elePaymentMethod.getAttribute("CreditCardExpDate");
						String strExpiryMonth=strCrdExpiry.substring(0, 2);
						String strExpiryYear=strCrdExpiry.substring(strCrdExpiry.length()-2);
						strCrdExpiry=strExpiryMonth+strExpiryYear;
							
						tender.put("tenderTypeCode", strPaymentMethod);
							
						JSONObject creditDebit = new JSONObject();
						creditDebit.put("last4Digits", strLast4Digits);
						creditDebit.put("typeCode", strPaymentMethod);
							
						JSONObject name = new JSONObject();
						name.put("firstName", strFstName);
						name.put("lastName", strLstName);
							
						creditDebit.put("name", name);
						creditDebit.put("expirationDate", strCrdExpiry);
							
						tender.put("creditDebit", creditDebit);
						tender.put("amount", strAmount);
							
					}else if(PAYMENT_MODE_GC.equals(strPaymentType)){
						strPaymentMethod="GiftCard";							
						String strDispSVCNo=elePaymentMethod.getAttribute("DisplaySvcNo");
							
						tender.put("tenderTypeCode", strPaymentMethod);
							
						JSONObject giftCard = new JSONObject();
						giftCard.put("accountNumber", strDispSVCNo);
							
						tender.put("giftCard", giftCard);
						tender.put("amount", strAmount);
							
					}else if(PAYMENT_MODE_VOUCHERS.equals(strPaymentType)){
						strPaymentMethod="HAV";							
							
						tender.put("tenderTypeCode", strPaymentMethod);
						tender.put("amount", strAmount);
					}else if(PAYMENT_MODE_PP.equals(strPaymentType)){
						strPaymentMethod="PayPal";							
							
						tender.put("tenderTypeCode", strPaymentMethod);
						tender.put("amount", strAmount);
					}else if(PAYMENT_MODE_CASH.equals(strPaymentType)){
						strPaymentMethod="cash";							
							
						tender.put("tenderTypeCode", strPaymentMethod);
						tender.put("amount", strAmount);
					}
						
					tenders.put(tender);
				}
					
				double dTotalExtendedPrice=0.0;
					
				double dTotalAmount=0.0;
				double dAmount=0.0;
					
				//OMS-3255 Changes -- Start
				double dOrderedQty=0.0;
				//OMS-3255 Changes -- End
					
				//OMS-3274 Changes -- Start
				double dTotalTax=0.0;
				double dLineTax=0.0;
				//OMS-3274 Changes -- End
				
				//OMS-2817 Changes -- Start
				double dTotalDiscount=0.0;
				double dLineDiscount=0.0;
				double dLnDiscount=0.0;
				//OMS-2817 Changes -- End
					
				JSONArray items = new JSONArray();
					
				NodeList nlShipmentLine=eleShipmentLines.getElementsByTagName(ELE_SHIPMENT_LINE);
				for(int i=0; i<nlShipmentLine.getLength(); i++){
					Element eleShipmentLn=(Element)nlShipmentLine.item(i);
					//OMS-3255 Changes -- Start
					Element eleOrderLine=SCXmlUtil.getChildElement(eleShipmentLn, ELE_ORDER_LINE);
					String strOrderedQty=eleOrderLine.getAttribute(ATTR_ORD_QTY);
					dOrderedQty=Double.parseDouble(strOrderedQty);
					if(dOrderedQty>0.0) {
					//OMS-3255 Changes -- End
						String strItemDesc=eleShipmentLn.getAttribute(ATTR_ITEM_DESC);
						String strItemId=eleShipmentLn.getAttribute(ATTR_ITEM_ID);
						String strQuantity=eleShipmentLn.getAttribute(ATTR_QUANTITY);
						String strUOM=eleShipmentLn.getAttribute(ATTR_UOM);						
						Element eleLnOverallTtls=SCXmlUtil.getChildElement(eleOrderLine, ELE_LINE_OVERALL_TOTALS);
						String strExtendedPrice=eleLnOverallTtls.getAttribute(ATTR_EXTENDED_PRICE);
						double dExtendedPrice=Double.parseDouble(strExtendedPrice);
						dTotalExtendedPrice=dTotalExtendedPrice+dExtendedPrice;
						//OMS-3257 Changes -- Start
						Element eleLnPriceInfo=SCXmlUtil.getChildElement(eleOrderLine, ELE_LINE_PRICE);
						String strUnitPrice=eleLnPriceInfo.getAttribute(ATTR_UNIT_PRICE);
						double dUnitPrice=Double.parseDouble(strUnitPrice);
						//OMS-3257 Changes -- End
						String strLnTtlWthoutTax=eleLnOverallTtls.getAttribute("LineTotalWithoutTax");
						String strLineTotal=eleLnOverallTtls.getAttribute(ATTR_LINE_TOTAL);
						if(!YFCCommon.isVoid(strLineTotal)) {
							dAmount=Double.parseDouble(strLineTotal);
						}
						dTotalAmount=dTotalAmount+dAmount;
							
						//OMS-3274 Changes -- Start
						String strLineTax=eleLnOverallTtls.getAttribute(ATTR_TAX);
						if(!YFCCommon.isVoid(strLineTax)) {
							dLineTax=Double.parseDouble(strLineTax);
						}
						dTotalTax=dTotalTax+dLineTax;
						//OMS-3274 Changes -- End
							
						Element eleItemDtls=SCXmlUtil.getChildElement(eleOrderLine, ELE_ITEM_DETAILS);
						Element eleItemAliasList=SCXmlUtil.getChildElement(eleItemDtls, ELE_ITEM_ALIAS_LIST);
						NodeList nlItemAlias=eleItemAliasList.getElementsByTagName(ELE_ITEM_ALIAS);
						String strUPC="";
						for(int m=0; m<nlItemAlias.getLength(); m++){
							Element eleItemAlias=(Element) nlItemAlias.item(m);
							String strAliasName=eleItemAlias.getAttribute(ATTR_ALIAS_NAME);
							if(ATTR_UPC.equals(strAliasName)){
								String strAliasValue=eleItemAlias.getAttribute(ATTR_ALIAS_VALUE);
								strUPC=strAliasValue;
								break;
							}
						}
							
						JSONObject item = new JSONObject();
						item.put("name", strItemDesc);
						item.put("description", strItemDesc);
						item.put("classCode", "");
						item.put("sku", strItemId);
						item.put("upc", strUPC);
						item.put("serialNumber", "");
						item.put("modelNumber", "");
						item.put("brand", "");
						item.put("industry", "");
						item.put("manufacturer", "");
						item.put("price", dUnitPrice);		//OMS-3257 Change
						item.put("total", strLnTtlWthoutTax);
						item.put("priceNotation", "");
							
						JSONObject quantity = new JSONObject();
						quantity.put("value", strQuantity);
						quantity.put("units", strQuantity);
						quantity.put("unitOfMeasureCode", strUOM);
							
						item.put("quantity", quantity);
							
						JSONArray discounts = new JSONArray();
							
						Element eleOrdLnChrgs=SCXmlUtil.getChildElement(eleOrderLine, ELE_LINE_CHARGES);
						NodeList nlOrdLnChrg=eleOrdLnChrgs.getElementsByTagName(ELE_LINE_CHARGE);
						for(int j=0; j<nlOrdLnChrg.getLength(); j++){							
							Element eleOrdLnChrg = (Element) nlOrdLnChrg.item(j);													
							String strChargeName=eleOrdLnChrg.getAttribute(ATTR_CHARGE_NAME);							
							String strChargeAmount=eleOrdLnChrg.getAttribute(ATTR_CHARGE_AMOUNT);
							//OMS-2817 Changes -- Start
							if(!YFCCommon.isVoid(strChargeAmount)) {								
								dLineDiscount=Double.parseDouble(strChargeAmount);
							}
							dTotalDiscount=dTotalDiscount+dLineDiscount;
							//OMS-2817 Changes -- End
								
							JSONObject discount = new JSONObject();
							//OMS-2817 Changes -- Start
							if(!YFCCommon.isVoid(strChargeAmount)) {
								strChargeAmount="-"+strChargeAmount;
								dLnDiscount=Double.parseDouble(strChargeAmount);
							}
							//OMS-2817 Changes -- End
							discount.put("amount", dLnDiscount);
							discount.put("name", strChargeName);
								
							discounts.put(discount);
						}
						
						if(nlOrdLnChrg.getLength()>0) {
							item.put("discounts", discounts);
						}
							
						item.put("miscInfo", "");
						item.put("category", "");
						item.put("customCategory1", "");
						item.put("customCategory2", "");
							
						items.put(item);
					//OMS-3255 Changes -- Start
					}
					//OMS-3255 Changes -- End
				}
				//OMS-2817 Changes -- Start
				String strTotalDiscount=Double.toString(dTotalDiscount);
				strTotalDiscount="-"+strTotalDiscount;
				double dSignedTotalDisc=Double.parseDouble(strTotalDiscount);
				//OMS-2817 Changes -- End
				String strTotalExtendedPrice=Double.toString(dTotalExtendedPrice);
					
				String strTenderApplied=Double.toString(dTotalAmount);
										
				JSONObject jsonRequest = new JSONObject();
					
				JSONObject customer = new JSONObject();
					
				customer.put("emailAddress", strCustEmailID);
				//OMS-3247 Changes -- Start
				strCustPhoneNo = formatPhoneNo(strCustPhoneNo);
				//OMS-3247 Changes -- End
				//OMS-3398 Changes -- Start
				int iPhnNoLength=strCustPhoneNo.length();
				if(!YFCCommon.isVoid(strCustPhoneNo) && iPhnNoLength>1) {
					JSONObject telephone = new JSONObject();
						
					telephone.put("areaCode", "");
					telephone.put("localNumber", strCustPhoneNo);
						
					customer.put("telephone", telephone);
				}
				//OMS-3398 Changes -- End
				jsonRequest.put("customer", customer);
					
				JSONObject workstation = new JSONObject();
				workstation.put("id", strUserId);
					
				JSONObject store = new JSONObject();
				store.put("id", strStoreNumber);
				store.put("name", strStoreName);
					
				JSONObject address = new JSONObject();
				address.put("addressLine1", strAddLine1);
				address.put("city", strCty);
					
				JSONObject country = new JSONObject();					
				country.put("code", "US");
				country.put("value", "US");
				address.put("country", country);
					
				address.put("postalCode", strPostalCode);
				address.put("territory", strTerritory);
				store.put("address", address);
				//OMS-3247 Changes -- Start
				strStorePhNo = formatPhoneNo(strStorePhNo);
				//OMS-3398 Changes -- Start
				int iStorePhnNo=strStorePhNo.length();
				if(!YFCCommon.isVoid(strStorePhNo) && iStorePhnNo>1) {
					JSONObject storePhone = new JSONObject();
					storePhone.put("areaCode", "");
					storePhone.put("localNumber", strStorePhNo);
						
					store.put("telephone", storePhone);
				}
				//OMS-3398 Changes -- End
				//OMS-3247 Changes -- End
					
				workstation.put("store", store);
					
				jsonRequest.put("workstation", workstation);
					
				Calendar calendar = Calendar.getInstance();
				String strReceiptDateTime = new SimpleDateFormat(YYYY_MM_DD_T_HH_MM_SS)		//OMS-3248 Change 
				.format(calendar.getTime());
					
				//OMS-3256 Changes -- Start
				String strSuperSplymnt="N";
					
				Document docCommonCodeIn=SCXmlUtil.createDocument(ELEMENT_COMMON_CODE);
				Element eleCommonCodeIn=docCommonCodeIn.getDocumentElement();				
				eleCommonCodeIn.setAttribute(ATTR_CODE_TYPE, "VSI_SPRSUPLMNT_STRS");
				eleCommonCodeIn.setAttribute(ATTR_CODE_VALUE, strStoreNo);
					
				printLogs("Input to getCommonCodeList API: "+SCXmlUtil.getString(docCommonCodeIn));
				Document docCommonCodeOut = VSIUtils.invokeAPI(env,API_COMMON_CODE_LIST, docCommonCodeIn);
				printLogs("Output from getCommonCodeList API: "+SCXmlUtil.getString(docCommonCodeOut));
					
				Element eleCommonCodeOut=docCommonCodeOut.getDocumentElement();
				NodeList nlCommonCode=eleCommonCodeOut.getElementsByTagName(ELEMENT_COMMON_CODE);
				if(nlCommonCode.getLength()>0) {
					strSuperSplymnt="Y";
				}
					
				jsonRequest.put("superSupplement", strSuperSplymnt);
				//OMS-3256 Changes -- End
					
				jsonRequest.put("receiptDateTime", strReceiptDateTime);
				jsonRequest.put("receiptType", "DigitalOnly");
				jsonRequest.put("transactionNumber", strOrderNo);
				jsonRequest.put("transactionType", "BOPUS");
				jsonRequest.put("grandAmount", dTotalAmount);		//OMS-3274 Change
				jsonRequest.put("netAmount", strTotalExtendedPrice);
				//OMS-2817 Changes -- Start
				JSONArray totalDiscounts = new JSONArray();
				
				JSONObject totalDiscount = new JSONObject();
				totalDiscount.put("amount", dSignedTotalDisc);
								
				totalDiscounts.put(totalDiscount);
				jsonRequest.put("discounts", totalDiscounts);
				//OMS-2817 Changes -- End
				jsonRequest.put("grossAmount", strTotalExtendedPrice);
				jsonRequest.put("tenderApplied", strTenderApplied);
				jsonRequest.put("membershipStatus", strMembershipStatus);
				jsonRequest.put("customerLoyaltyNumber", strBillToId);
				jsonRequest.put("currentBalance", "");
					
				JSONArray orders = new JSONArray();
					
				JSONObject order = new JSONObject();
				order.put("number", strOrderNo);
				order.put("action", "Pickup");
				order.put("totalAmount", strGrandAmount);
				order.put("description", "BOPUS order");
					
				JSONArray taxes = new JSONArray();
					
				JSONObject tax = new JSONObject();
				tax.put("amount", dTotalTax);		//OMS-3274 Change
				tax.put("taxableAmount", strTotalExtendedPrice);
				tax.put("taxTypeCode", "Sales");
				tax.put("description", "Sales Tax");		//OMS-3252 Change
					
				taxes.put(tax);
					
				order.put("taxes", taxes);
					
				JSONArray fees = new JSONArray();
					
				double dFeeAmount=0.0;
					
				JSONObject fee = new JSONObject();
				fee.put("amount", dFeeAmount);
				fee.put("description", "");
					
				fees.put(fee);
					
				order.put("fees", fees);
					
				order.put("items", items);
					
				JSONObject soldTo = new JSONObject();
										
				JSONObject soldToAddr = new JSONObject();					
				soldToAddr.put("addressLine1", strAddrLine1);
				soldToAddr.put("addressLine2", strAddrLine2);
				soldToAddr.put("addressLine3", strAddrLine3);
				soldToAddr.put("city", strPrsnInfoCity);
				soldToAddr.put("territory", strPrsnInfoState);
				soldToAddr.put("postalCode", strPrsnInfoZipCode);
					
				JSONObject soldToAddrCntry = new JSONObject();
				soldToAddrCntry.put("value", "US");
				soldToAddrCntry.put("code", "US");
					
				soldToAddr.put("country", soldToAddrCntry);
					
				soldTo.put("address", soldToAddr);
					
				soldTo.put("customerName", strCustName);
				soldTo.put("customerNumber", strBillToId);
					
				//OMS-3247 Changes -- Start
				strCustPhoneNo = formatPhoneNo(strCustPhoneNo);
				//OMS-3247 Changes -- End
					
				JSONObject telephone1 = new JSONObject();
				telephone1.put("areaCode", "");
				telephone1.put("localNumber", strCustPhoneNo);
					
				soldTo.put("telephone1", telephone1);
					
				order.put("soldTo", soldTo);
					
				JSONObject deliveryPickup = new JSONObject();
					
				JSONObject pickupAddress = new JSONObject();
				pickupAddress.put("addressLine1", strAddLine1);
				pickupAddress.put("city", strCty);
					
				JSONObject pickupCountry = new JSONObject();
				pickupCountry.put("code", "US");
				pickupCountry.put("value", "US");
					
				pickupAddress.put("country", pickupCountry);
					
				pickupAddress.put("postalCode", strPostalCode);
				pickupAddress.put("territory", strTerritory);
					
				deliveryPickup.put("address", pickupAddress);
					
				order.put("deliveryPickup", deliveryPickup);
					
				orders.put(order);
					
				jsonRequest.put("orders", orders);
				//OMS-3398 Changes -- Start
				if(nlCollectionDtl.getLength()>0) {
					jsonRequest.put("tenders", tenders);
				}else {
					JSONArray noTenders = new JSONArray();
					JSONObject noTender = new JSONObject();
					noTender.put("tenderTypeCode", "None");
					noTender.put("amount", 0.0);
					noTenders.put(noTender);
					jsonRequest.put("tenders", noTenders);
				}
				//OMS-3398 Changes -- End	
				String strJSONRequest=jsonRequest.toString();
					
				printLogs("Printing Flex Customer Receipt Request: "+strJSONRequest);
					
				invokeFlexReceiptWebservice(env, strJSONRequest, strOrdHdrKey, strOrderNo, strStoreNo);
					
				updateShipmentFlag(env, strShipmentKey);

								
			}
			
		}catch (Exception e) {
			printLogs("Exception in VSISOMFlexCustomerReceipt Class and sendCustomerReceipt Method");
			printLogs("The exception is [ "+ e.getMessage() +" ]");
			throw new YFSException();
		}
		
		printLogs("================Exiting VSISOMFlexCustomerReceipt class and sendCustomerReceipt Method================================");
		
		return inXML;
	}
	
	//OMS-3247 Changes -- Start
	private String formatPhoneNo(String strStorePhNo) {
		
		printLogs("================Inside formatPhoneNo Method================");
		
		int iPhnNoLength=strStorePhNo.length();
		if(iPhnNoLength>0) {
			if(strStorePhNo.contains("(")) {
				String strPhNo1=strStorePhNo.substring(strStorePhNo.indexOf("(")+1, strStorePhNo.lastIndexOf(")"));							
				String strPhNo2=null;
				if(iPhnNoLength==13) {
					strPhNo2=strStorePhNo.substring(strStorePhNo.lastIndexOf(")")+1, strStorePhNo.indexOf("-"));								
				}else if(iPhnNoLength==14) {
					strPhNo2=strStorePhNo.substring(strStorePhNo.lastIndexOf(")")+2, strStorePhNo.indexOf("-"));								
				}
				String strPhNo3=strStorePhNo.substring(strStorePhNo.lastIndexOf("-")+1);							
				strStorePhNo=strPhNo1+strPhNo2+strPhNo3;				
			}
		}
		
		printLogs("Phone Number being returned: "+strStorePhNo);
		printLogs("================Exiting formatPhoneNo Method================");
		return strStorePhNo;
	}
	//OMS-3247 Changes -- End

	/**
	 * @param env
	 * @param strShipmentKey
	 * @throws ParserConfigurationException
	 * @throws YIFClientCreationException
	 * @throws RemoteException
	 */
	private void updateShipmentFlag(YFSEnvironment env, String strShipmentKey)
			throws ParserConfigurationException, YIFClientCreationException,
			RemoteException {
		
		printLogs("================Inside updateShipmentFlag Method================");
		
		Document docChangeShipmentIn=XMLUtil.createDocument(ELE_SHIPMENT);
		Element eleChangeShipmentIn=docChangeShipmentIn.getDocumentElement();
		
		eleChangeShipmentIn.setAttribute(ATTR_SHIPMENT_KEY, strShipmentKey);
		Element eleShpExtn=SCXmlUtil.createChild(eleChangeShipmentIn, ELE_EXTN);
		eleShpExtn.setAttribute("ExtnCustomerReceiptFlag", FLAG_Y);
		
		printLogs("changeShipment api Input: "+XMLUtil.getXMLString(docChangeShipmentIn));
		VSIUtils.invokeAPI(env, API_CHANGE_SHIPMENT, docChangeShipmentIn);
		printLogs("changeShipment api was invoked Successfully");
		
		printLogs("================Exiting updateShipmentFlag Method================");
	}

	/**
	 * @param env
	 * @param strJSONRequest
	 * @param strOrdHdrKey 
	 * @param strStoreNo 
	 * @param strOrderNo 
	 * @throws MalformedURLException
	 * @throws IOException
	 * @throws ProtocolException
	 * @throws Exception
	 */
	private void invokeFlexReceiptWebservice(YFSEnvironment env,
			String strJSONRequest, String strOrdHdrKey, String strOrderNo, String strStoreNo){
		
		printLogs("================Inside invokeFlexReceiptWebservice Method================");
		
		String strResponseCode=null;
		String responseStatus=null;
		
		try{
		
		String strFlexReceiptsURL=YFSSystem.getProperty("FLEXRECEIPT_BOPUS_WEBSERVICE_URL");
		
		printLogs("URL obtained from COP file: "+strFlexReceiptsURL);		
		
		printLogs("inDocString: "+strJSONRequest);
		
		String strUserName=YFSSystem.getProperty("FLEXRECEIPT_BOPUS_WEBSERVICE_USERNAME");
		String strPassword=YFSSystem.getProperty("FLEXRECEIPT_BOPUS_WEBSERVICE_PASSWORD");
		
		DataOutputStream wr=null;
		
		URL url = new URL (strFlexReceiptsURL);
		
		HttpURLConnection connection =  (HttpURLConnection) url.openConnection();
		
		String encoded = Base64.getEncoder().encodeToString((strUserName+":"+strPassword).getBytes());  
				
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Authorization", "Basic "+encoded);
		byte[] bContent = strJSONRequest.getBytes();
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setRequestProperty("Content-Length", String.valueOf(bContent.length));
		connection.setDefaultUseCaches(false);
		connection.setDoOutput(true);		
		
		printLogs("Invoking the FlexReceipt web service");
		
		wr = new DataOutputStream (connection.getOutputStream());
		wr.write(bContent);
		
		printLogs("Invoked the FlexReceipt web service");	
		
		wr.flush();
		wr.close();
		
		printLogs("FlexReceipt Request will be stored in DB");
		
		String strChargeType="Flex Customer Receipt Request";
		
		storeRequestAndResponse(env, strJSONRequest, strOrdHdrKey, strChargeType);
		
		printLogs("FlexReceipt Request is stored in DB");
		
		printLogs("Before reading ResponseMessage");
		
		int iResponseCode=connection.getResponseCode();
		strResponseCode=Integer.toString(iResponseCode);
		printLogs("responseCode: "+strResponseCode);
		
		responseStatus = connection.getResponseMessage();
		printLogs("responseStatus: "+responseStatus);		
		
		BufferedReader in = new BufferedReader(new InputStreamReader(
				connection.getInputStream()));
		printLogs("After reading InputStream");
		String inputLine;
		StringBuffer response = new StringBuffer();
		while ((inputLine = in.readLine()) != null) {
		    response.append(inputLine);
		}
		in.close();
		
		printLogs("response: " + response.toString());
		
		String strResponse=response.toString();
		
		printLogs("strResponse: " + strResponse);
		
		printLogs("Response from FlexReceipt WebService: "+strResponse);		
		
		printLogs("FlexReceipt Response will be stored in DB");
		
		strChargeType="Flex Customer Receipt Response";
		
		storeRequestAndResponse(env, strResponse, strOrdHdrKey, strChargeType);
		
		printLogs("FlexReceipt Response is stored in DB");
		
		}catch (Exception e){
			printLogs("Exception occurred in VSISOMFlexCustomerReceipt.invokeFlexReceiptWebservice method");
			printLogs("The exception xml is [ "+ e.getMessage() +" ]");
			printLogs("Flex Receipt Exception Email Triggering service will be Invoked");
			try {
				Document docEmailTrigger=XMLUtil.createDocument("Order");
				Element eleEmailTrigger=docEmailTrigger.getDocumentElement();
				eleEmailTrigger.setAttribute("OrderNo", strOrderNo);
				eleEmailTrigger.setAttribute("StoreNo", strStoreNo);
				eleEmailTrigger.setAttribute("ResponceCode", strResponseCode);
				eleEmailTrigger.setAttribute("ResponseMessage", responseStatus);
				eleEmailTrigger.setAttribute("ExceptionMessage", e.getMessage());
				
				printLogs("Invoking VSIFlexReceiptExceptionEmailTrigger Service with input: "+SCXmlUtil.getString(docEmailTrigger));
				VSIUtils.invokeService(env, "VSIFlexReceiptExceptionEmailTrigger", docEmailTrigger);
				printLogs("VSIFlexReceiptExceptionEmailTrigger service was invoked successfully");
			} catch (ParserConfigurationException e1) {			
				printLogs("ParserConfigurationException thrown in VSISOMFlexCustomerReceipt.invokeFlexReceiptWebservice method's catch block ");
				printLogs("The exception xml is [ "+ e1.getMessage() +" ]");;
			} catch (Exception e1) {	
				printLogs("Exception thrown in VSISOMFlexCustomerReceipt.invokeFlexReceiptWebservice method's catch block ");
				printLogs("The exception xml is [ "+ e1.getMessage() +" ]");;
			}
		}
		
		printLogs("================Exiting invokeFlexReceiptWebservice Method================");
	}

	/**
	 * @param env
	 * @param strJSONRequest
	 * @param strOrdHdrKey 
	 * @param strChargeType 
	 * @throws ParserConfigurationException
	 * @throws YIFClientCreationException
	 * @throws RemoteException
	 */
	private void storeRequestAndResponse(YFSEnvironment env,
			String strJSONRequest, String strOrdHdrKey, String strChargeType) throws ParserConfigurationException,
			YIFClientCreationException, RemoteException {
		
		printLogs("================Inside storeRequestAndResponse Method================");
		
		Document doc = XMLUtil.createDocument("PaymentRecords");
  		Element elePayRecrds = doc.getDocumentElement();
  		elePayRecrds.setAttribute(VSIConstants.ATTR_ORDER_HEADER_KEY, strOrdHdrKey);
  		elePayRecrds.setAttribute("Record", strJSONRequest);
  		elePayRecrds.setAttribute("ChargeType", strChargeType);
  		
  		printLogs("VSIPaymentRecords service Input: "+XMLUtil.getXMLString(doc));
  		YIFApi api;
  		api = YIFClientFactory.getInstance().getApi();
  		api.executeFlow(env,"VSIPaymentRecords", doc);
  		printLogs("VSIPaymentRecords service executed successfully");
  		
  		printLogs("================Exiting storeRequestAndResponse Method================");
	}
	
	private void printLogs(String mesg) {
		if(log.isDebugEnabled()){
			log.debug(TAG +" : "+mesg);
		}
	}

}
