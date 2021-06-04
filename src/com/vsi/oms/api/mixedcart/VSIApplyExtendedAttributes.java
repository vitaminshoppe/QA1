package com.vsi.oms.api.mixedcart;

import java.rmi.RemoteException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.common.joda.time.Hours;
import org.elasticsearch.common.joda.time.Minutes;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.vsi.oms.utils.VSIConstants;
import com.vsi.oms.utils.VSIDBUtil;
import com.vsi.oms.utils.VSIGeneralUtils;
import com.vsi.oms.utils.VSIUtils;
import com.vsi.oms.utils.XMLUtil;
import com.yantra.interop.japi.YIFApi;
import com.yantra.interop.japi.YIFClientCreationException;
import com.yantra.yfc.core.YFCObject;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;

public class VSIApplyExtendedAttributes {
	
	private static YFCLogCategory log = YFCLogCategory.instance(VSIApplyExtendedAttributes.class);
	private static final String TAG = VSIApplyExtendedAttributes.class.getSimpleName();
	YIFApi api;
	
	HashSet<String> setShipNode = new HashSet<String>();
	
	public Document applyExtendedAttributes(YFSEnvironment env, Document inXML){
		
		printLogs("================Inside VSIApplyExtendedAttributes Class and applyExtendedAttributes Method================");
		printLogs("Printing Input XML :"+SCXmlUtil.getString(inXML));
		
		try{
			
			Element eleOrder=inXML.getDocumentElement();
			String strOrderHeaderKey=eleOrder.getAttribute(VSIConstants.ATTR_ORDER_HEADER_KEY);
			
			Document docGetOrdLstIn=XMLUtil.createDocument(VSIConstants.ELE_ORDER);
			Element eleGetOrdLstIn=docGetOrdLstIn.getDocumentElement();
			eleGetOrdLstIn.setAttribute(VSIConstants.ATTR_ORDER_HEADER_KEY, strOrderHeaderKey);
			
			printLogs("Input to getOrderList API: "+SCXmlUtil.getString(docGetOrdLstIn));
			Document docGetOrdLstOut = VSIUtils.invokeAPI(env,"global/template/api/getOrderList_VSIApplyExtendedAttributes.xml",
					VSIConstants.API_GET_ORDER_LIST, docGetOrdLstIn);
			printLogs("Output from getOrderList API: "+SCXmlUtil.getString(docGetOrdLstOut));
			
			Element eleGetOrdLstOut=docGetOrdLstOut.getDocumentElement();
			Element eleOrderOut=(Element)eleGetOrdLstOut.getElementsByTagName(VSIConstants.ELE_ORDER).item(0);
			String strDraftOrderFlag=eleOrderOut.getAttribute(VSIConstants.ATTR_DRAFT_ORDER_FLAG);
			
			if(VSIConstants.FLAG_N.equals(strDraftOrderFlag)){
				
				printLogs("Non-Draft Order scenario");
				
				Document docInputgetCommonCodeList=XMLUtil.createDocument(VSIConstants.ELE_COMMON_CODE);
				Element eleCommonCode = docInputgetCommonCodeList.getDocumentElement();
				eleCommonCode.setAttribute(VSIConstants.ATTR_CODE_TYPE, "POS_MIXEDCART");
								
				printLogs("Input for getCommonCodeList :" +XMLUtil.getXMLString(docInputgetCommonCodeList));
				Document docCommonCodeListOP=VSIUtils.invokeAPI(env,VSIConstants.API_COMMON_CODE_LIST, docInputgetCommonCodeList);
				printLogs("Output from getCommonCodeList :" +XMLUtil.getXMLString(docCommonCodeListOP));
				
				Element eleCCOut=(Element)docCommonCodeListOP
						.getElementsByTagName(VSIConstants.ELE_COMMON_CODE).item(0);
				
				String strPOSFlag=eleCCOut.getAttribute(VSIConstants.ATTR_CODE_SHORT_DESCRIPTION);
				
				if("NOT_ALLOWED".equals(strPOSFlag)){
					
					printLogs("Common Code Switch is set");
					
					generateCustomerPONo(env, eleOrderOut);
					
					getPTandLPTdetails(env,eleOrderOut);
					
					eleOrderOut.setAttribute(VSIConstants.ATTR_BYPASS_PRICING, VSIConstants.FLAG_Y);
					
					eleOrderOut.setAttribute(VSIConstants.ATTR_OVERRIDE, VSIConstants.FLAG_Y);
					Element eleOrderExtn = (Element) eleOrderOut.getElementsByTagName("Extn").item(0);
					eleOrderExtn.setAttribute("ExtnIsMixedCartOrder", "Y");
					Document docChangeOrderIn=XMLUtil.createDocumentFromElement(eleOrderOut);
					
					printLogs("Input to changeOrder API: "+SCXmlUtil.getString(docChangeOrderIn));
					VSIUtils.invokeAPI(env,VSIConstants.API_CHANGE_ORDER, docChangeOrderIn);
					printLogs("changeOrder API was invoked successfully");
					
				}else{
					printLogs("Common Code Switch not set, so exiting the class");
				}
			}else{
				printLogs("Draft Order scenario, so exiting the class");
			}
			
			
		}catch (YFSException e) {
			e.printStackTrace();
			throw new YFSException();
		} catch (Exception e){
			e.printStackTrace();
			throw new YFSException();
		}
		
		printLogs("================Exiting VSIApplyExtendedAttributes Class and applyExtendedAttributes Method================");
	
		return inXML;
		
	}

	private void generateCustomerPONo(YFSEnvironment env, Element eleOrderOut)
			throws Exception {
		
		printLogs("Inside generateCustomerPONo Method");
		
		String tranDate=eleOrderOut.getAttribute(VSIConstants.ATTR_ORDER_DATE);
		printLogs("Order Date before Formatting: "+tranDate);
		String ordNo=eleOrderOut.getAttribute(VSIConstants.ATTR_ORDER_NO);					
		String ordType=eleOrderOut.getAttribute(VSIConstants.ATTR_ORDER_TYPE);
		String entryType=eleOrderOut.getAttribute(VSIConstants.ATTR_ENTRY_TYPE);
		printLogs("Order No: "+ordNo+" Order Type: "+ordType+" Entry Type: "+entryType);
		
		tranDate = tranDate.replaceAll("\\-", "");
		tranDate = tranDate.substring(2, 6);
		printLogs("Order Date after Formatting: "+tranDate);
		
		String regNumber = VSIConstants.REG_NUMBER;
		String grpNumber = VSIConstants.GROUP_NUMBER;
		String itemStatusNumber = VSIConstants.LINE_ITEM_STATUS_NUMBER;
		printLogs("Reg No: "+regNumber+" Group Number: "+grpNumber+" Item Status Number: "+itemStatusNumber);
		
		Element eleOrderLines=SCXmlUtil.getChildElement(eleOrderOut, VSIConstants.ELE_ORDER_LINES);
		NodeList nlOrderLine=eleOrderLines.getElementsByTagName(VSIConstants.ELE_ORDER_LINE);
		for(int i=0; i<nlOrderLine.getLength(); i++){
			Element eleOrderLine = (Element)nlOrderLine.item(i);
			String strLineType=eleOrderLine.getAttribute(VSIConstants.ATTR_LINE_TYPE);
			printLogs("Line Type is: "+strLineType);
			if(VSIConstants.LINETYPE_PUS.equals(strLineType) || VSIConstants.LINETYPE_STS.equals(strLineType)){
				String shipNode=eleOrderLine.getAttribute(VSIConstants.ATTR_SHIP_NODE);
				setShipNode.add(shipNode);
			}
			printLogs("Ship Node HashSet Contents: "+Arrays.toString(setShipNode.toArray()));
		}
		printLogs("Ship Node HashSet Contents after processing all lines: "+Arrays.toString(setShipNode.toArray()));
		
		for(String shpNd:setShipNode){
			printLogs("Ship Node being processed: "+shpNd);
			String shipNodePadded = ("00000" + shpNd).substring(shpNd
					.length());
			printLogs("shipNodePadded: "+shipNodePadded);
			String tranNumberBOPUS = "";
			String tranNumberSTS = "";
			String seqNum = "VSI_SEQ_" + shpNd;
			printLogs("seqNum: "+seqNum);
			tranNumberBOPUS = VSIDBUtil.getNextSequence(env, seqNum);
			printLogs("tranNumberBOPUS: "+tranNumberBOPUS);
			tranNumberSTS = VSIDBUtil.getNextSequence(env, seqNum);			
			printLogs("tranNumberSTS: "+tranNumberSTS);
			String tranNumberPaddedBOPUS = ("00000" + tranNumberBOPUS).substring(tranNumberBOPUS
					.length()); 
			printLogs("tranNumberPaddedBOPUS: "+tranNumberPaddedBOPUS);
			String tranNumberPaddedSTS = ("00000" + tranNumberSTS).substring(tranNumberSTS
					.length());
			printLogs("tranNumberPaddedSTS: "+tranNumberPaddedSTS);
			for(int j=0; j<nlOrderLine.getLength(); j++){
				Element eleOrderLine = (Element)nlOrderLine.item(j);
				String strPrimeLineNo=eleOrderLine.getAttribute(VSIConstants.ATTR_PRIME_LINE_NO);
				String strOrderLnKey=eleOrderLine.getAttribute(VSIConstants.ATTR_ORDER_LINE_KEY);
				String strLineType=eleOrderLine.getAttribute(VSIConstants.ATTR_LINE_TYPE);
				printLogs("Order Line with Prime Line number "+strPrimeLineNo+" and order line key "+strOrderLnKey+" is being processed");
				printLogs("Line Type is: "+strLineType);
				if(VSIConstants.LINETYPE_PUS.equals(strLineType) || VSIConstants.LINETYPE_STS.equals(strLineType)){
					String shipNode=eleOrderLine.getAttribute(VSIConstants.ATTR_SHIP_NODE);
					printLogs("Ship Node of current Line: "+shipNode);
					if(shipNode.equals(shpNd)){
						printLogs("Ship Node Matches");
						String customerPoNo=null;
						if(VSIConstants.LINETYPE_PUS.equals(strLineType)){
							customerPoNo = shipNodePadded + tranDate + regNumber
								+ tranNumberPaddedBOPUS + grpNumber + itemStatusNumber;
							printLogs("customerPoNo: "+customerPoNo);
						}else if(VSIConstants.LINETYPE_STS.equals(strLineType)){
							customerPoNo = shipNodePadded + tranDate + regNumber
								+ tranNumberPaddedSTS + grpNumber + itemStatusNumber;
							printLogs("customerPoNo: "+customerPoNo);
						}
						if ((ordType != null && ordType.equalsIgnoreCase("WEB"))
								&& (entryType != null && entryType.equalsIgnoreCase("WEB"))) {
							
							printLogs("Web Order");

							if (customerPoNo != null && customerPoNo.length() > 10) {
								printLogs("customerPoNo will be trimmed");
								customerPoNo = customerPoNo
										.substring(customerPoNo.length() - 10);
								printLogs("customerPoNo after trimming: "+customerPoNo);
							}

							customerPoNo = ordNo + customerPoNo;
							printLogs("customerPoNo after appending Web Order Number: "+customerPoNo);
						}
						eleOrderLine.setAttribute(VSIConstants.ATTR_CUST_PO_NO, customerPoNo);
						printLogs("Order Line after adding customerPoNo: "+SCXmlUtil.getString(eleOrderLine));
					}
				}
				printLogs("Order Line with Prime Line number "+strPrimeLineNo+" and order line key "+strOrderLnKey+" processing completed");
			}
			printLogs("Ship Node "+shpNd+" processing completed");
		}
		Document docOrder=XMLUtil.createDocumentFromElement(eleOrderOut);
		printLogs("Order Details while exiting generateCustomerPONo method: "+SCXmlUtil.getString(docOrder));
		printLogs("Exiting generateCustomerPONo Method");
	}
	
	private void getPTandLPTdetails(YFSEnvironment env, Element eleOrder) throws Exception{
		
		printLogs("Inside getPTandLPTdetails Method");
		
		try {
			
			if(eleOrder.getElementsByTagName(VSIConstants.ELE_ORDER_LINE).getLength() > 0){
			
				Element eleOrderExtn = (Element) eleOrder.getElementsByTagName("Extn").item(0);
				String strExtnOrdDte = null;
				
				if(eleOrderExtn != null){
					strExtnOrdDte = eleOrderExtn.getAttribute("ExtnOrderDate");
				}
				
				printLogs("strExtnOrdDte: "+strExtnOrdDte);
				
				String strDate = "";
				Date date;
				String bopusPT = "";
				String bopusLPT = "";
				String bostsPT = "";
				String bostsLPT = "";
				int iShip = 0;
				int iPick = 0;			
				
				Element eleOrderLines =SCXmlUtil.getChildElement(eleOrder, VSIConstants.ELE_ORDER_LINES);
				NodeList orderLineList = eleOrderLines.getElementsByTagName(VSIConstants.ELE_ORDER_LINE);		
				int orderLineLength = orderLineList.getLength();
				for (int i = 0; i < orderLineLength; i++) {
					Element orderLineElement = (Element) orderLineList.item(i);
					String sLineType = orderLineElement.getAttribute(VSIConstants.ATTR_LINE_TYPE);
					printLogs("Line Type: "+sLineType);
					if (sLineType.equalsIgnoreCase("SHIP_TO_STORE") ) {	
						iShip++;	
					} else if(sLineType.equalsIgnoreCase("PICK_IN_STORE")){
						iPick++;
					}
				}
				printLogs("After processing all lines value of iShip "+iShip+" and iPick "+iPick);
				
				for(String shipnode:setShipNode){
					
					printLogs("Ship Node being processed: "+shipnode);
						
					if(strExtnOrdDte != null && !strExtnOrdDte.trim().equals("") && shipnode != null && !shipnode.trim().equals("")){
						
						SimpleDateFormat formatter = new SimpleDateFormat("yyyy-mm-dd");
						date = formatter.parse(strExtnOrdDte);
						strDate = formatter.format(date);
						printLogs("strDate: "+strDate);
						
						Document getCalOutDoc = getCalendarDetails(shipnode, env);
							
							int counter = 1;
							boolean isHoliday = false;
							if(getCalOutDoc != null){
								
								// This loop is to validate if the OrderDate is a Holiday.
								// If it is holiday, it will take the next day and exit the loop
								// If it is not holiday then it will exit the loop
								
								if(iPick > 0){
									printLogs("Inside if condition when BOPUS lines are present");
									while(counter > 0){
										
										isHoliday = checkIfDayIsHoliday(getCalOutDoc,strDate);
				
										if(isHoliday){
											
											printLogs("Inside if its a Holiday");
											DateTime dtOrg = new DateTime(strDate);
											DateTime dtPlusOne = dtOrg.plusDays(1);
											date = formatter.parse(dtPlusOne.toString());
											strDate = formatter.format(date);
											printLogs("New value of strDate: "+strDate);
											
											counter ++;											
										}else{
											printLogs("Inside if its not a Holiday");
											break;
										}										
									}
									
									if(counter == 1){
										printLogs("counter value is 1");
										bopusPT = getPTForBOPUSLine(strDate,strExtnOrdDte,getCalOutDoc);										
									}else{
										printLogs("counter value not equals 1");
										bopusPT = strDate;										
									}									 
									bopusLPT = getLPTForLine(bopusPT,getCalOutDoc,env);
									printLogs("bopusLPT: "+bopusLPT);
								}
								
								if(iShip > 0){
									printLogs("Inside if condition when STS lines are present");
									bostsPT = getPTForBOSTSLine(strExtnOrdDte,shipnode,env);
									isHoliday = checkIfDayIsHoliday(getCalOutDoc,bostsPT);
									if(isHoliday){
										printLogs("Inside If its a holiday");
										DateTime dtOrg = new DateTime(bostsPT);
										DateTime dtPlusOne = dtOrg.plusDays(1);
										date = formatter.parse(dtPlusOne.toString());
										bostsPT = formatter.format(date);
										printLogs("Updated value of bostsPT: "+bostsPT);
									}
									bostsLPT = getLPTForLine(bostsPT,getCalOutDoc,env);
									printLogs("bostsLPT: "+bostsLPT);
								}								
							}
							
							
							for (int i = 0; i < orderLineLength; i++) {
								Element orderLineElement = (Element) orderLineList.item(i);
								String strPrimeLineNo=orderLineElement.getAttribute(VSIConstants.ATTR_PRIME_LINE_NO);
								String strOrderLnKey=orderLineElement.getAttribute(VSIConstants.ATTR_ORDER_LINE_KEY);
								printLogs("Order Line with Prime Line number "+strPrimeLineNo+" and order line key "+strOrderLnKey+" is being processed");								
								String shipNode=orderLineElement.getAttribute(VSIConstants.ATTR_SHIP_NODE);
								printLogs("Ship Node is: "+shipNode);
								if(shipNode.equals(shipnode)){
									printLogs("Ship Node Matches");
									String sLineType = orderLineElement.getAttribute(VSIConstants.ATTR_LINE_TYPE);									
									printLogs("Line Type is: "+sLineType);
									if (sLineType.equalsIgnoreCase("SHIP_TO_STORE")){
										
										Element ordLneExtnEle = (Element) orderLineElement.getElementsByTagName("Extn").item(0);
										ordLneExtnEle.setAttribute("ExtnPickDate", bostsPT);
										ordLneExtnEle.setAttribute("ExtnLastPickDate", bostsLPT);
										
										Element orderDatesEle = SCXmlUtil.createChild(orderLineElement, "OrderDates");
										
										Element orderDateEle = SCXmlUtil.createChild(orderDatesEle, "OrderDate");
										orderDateEle.setAttribute("DateTypeId", "ExtnPickDate");
										orderDateEle.setAttribute("ActualDate", bostsPT);
																			
										Element eleOrderDate = SCXmlUtil.createChild(orderDatesEle, "OrderDate");;
										eleOrderDate.setAttribute("DateTypeId", "ExtnLastPickDate");
										eleOrderDate.setAttribute("ActualDate", bostsLPT);								
						
									} else if(sLineType.equalsIgnoreCase("PICK_IN_STORE")){
										
										Element ordLneExtnEle = (Element) orderLineElement.getElementsByTagName("Extn").item(0);
										ordLneExtnEle.setAttribute("ExtnPickDate", bopusPT);
										ordLneExtnEle.setAttribute("ExtnLastPickDate", bopusLPT);
										
										Element orderDatesEle = SCXmlUtil.createChild(orderLineElement, "OrderDates");
										
										Element orderDateEle = SCXmlUtil.createChild(orderDatesEle, "OrderDate");
										orderDateEle.setAttribute("DateTypeId", "ExtnPickDate");
										orderDateEle.setAttribute("ActualDate", bopusPT);
																					
										Element eleOrderDate = SCXmlUtil.createChild(orderDatesEle, "OrderDate");
										eleOrderDate.setAttribute("DateTypeId", "ExtnLastPickDate");
										eleOrderDate.setAttribute("ActualDate", bopusLPT);									
									}							
								}
								printLogs("Order Line after stamping PT and LPT dates: "+SCXmlUtil.getString(orderLineElement));
								printLogs("Order Line with Prime Line number "+strPrimeLineNo+" and order line key "+strOrderLnKey+" processing completed");
							}						
						}
					printLogs("Ship Node "+shipnode+" processing completed");
					}
				}
		}catch (ParserConfigurationException e) {			
			e.printStackTrace();
		} catch (RemoteException e) {			
			e.printStackTrace();
		} catch (YIFClientCreationException e) {			
			e.printStackTrace();
		} catch (ParseException e) {			
			e.printStackTrace();
		} catch (TransformerException e) {			
			e.printStackTrace();
		}
		
		Document docOrder=XMLUtil.createDocumentFromElement(eleOrder);
		printLogs("Order Details while exiting getPTandLPTdetails method: "+SCXmlUtil.getString(docOrder));
		printLogs("Exiting getPTandLPTdetails Method");
    }
	
	private String getPTForBOSTSLine(String strOrdDte, String shipnode,YFSEnvironment env) throws ParserConfigurationException, YFSException, RemoteException, YIFClientCreationException, ParseException {
		
		printLogs("Inside getPTForBOSTSLine method");
		
		String extnLeadTime = "";
		String ordCpyDte = strOrdDte.substring(0,strOrdDte.indexOf("T")+1)+"23:59:59";
		printLogs("ordCpyDte: "+ordCpyDte);
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		Date date ;
		DateTime ordDT = new DateTime(ordCpyDte);
		Document getOrgListIpDoc = XMLUtil.createDocument(VSIConstants.ELE_ORGANIZATION);
		getOrgListIpDoc.getDocumentElement().setAttribute(VSIConstants.ATTR_ORG_CODE, shipnode);
		
		printLogs("Input to getOrganizationList api: "+SCXmlUtil.getString(getOrgListIpDoc));
		Document getOrgListOpDoc = VSIUtils.invokeAPI(env, "global/template/api/VSIGetOrgListTemplate.xml", "getOrganizationList", getOrgListIpDoc);
				
		if(getOrgListOpDoc != null){
			printLogs("Output from getOrganizationList api: "+SCXmlUtil.getString(getOrgListOpDoc));
			NodeList orgNL = getOrgListOpDoc.getElementsByTagName("Organization");
			if(orgNL.getLength() > 0){
				Element eleOrg = (Element) orgNL.item(0);
				Element extnEle = (Element) eleOrg.getElementsByTagName("Extn").item(0);
				if(extnEle != null){
					extnLeadTime = extnEle.getAttribute("ExtnLeadTime");
					if(extnLeadTime != null && !extnLeadTime.trim().equals("")){
						ordDT = ordDT.plusDays(Integer.parseInt(extnLeadTime));
					}
				}
			}
		}
		
		String bufferPrd = getBufferPeriod(env);
		if(bufferPrd != null && !bufferPrd.trim().equals("")){
			ordDT = ordDT.plusDays(Integer.parseInt(bufferPrd));
		}
		date = formatter.parse(ordDT.toString());
		String strPTDate = formatter.format(date);
		
		printLogs("Date being returned: "+strPTDate);
		
		printLogs("Exiting getPTForBOSTSLine method");
		
		return strPTDate;		
	}
	
	private String getBufferPeriod(YFSEnvironment env) {
		
		printLogs("Inside getBufferPeriod method");
		
		String bufferPrd = null;
		try{
			Document docForGetCommonCodeList = XMLUtil.createDocument(VSIConstants.ELEMENT_COMMON_CODE);
	
			Element eleCommonCode = docForGetCommonCodeList.getDocumentElement();
			eleCommonCode.setAttribute(VSIConstants.ATTR_ENTERPRISE_CODE,"VSI.com");
			eleCommonCode.setAttribute(VSIConstants.ATTR_CODE_TYPE,"BUFFER_DAYS_BOSTS");
			
			printLogs("Input to getCommonCodeList api: "+SCXmlUtil.getString(docForGetCommonCodeList));
			Document docAfterGetCommonCodeList = VSIUtils.invokeAPI(env,VSIConstants.API_COMMON_CODE_LIST,docForGetCommonCodeList);
			
			
			if (docAfterGetCommonCodeList != null) {
				printLogs("Output from getCommonCodeList api: "+SCXmlUtil.getString(docAfterGetCommonCodeList));
				Element eleOutCommonCode=(Element) docAfterGetCommonCodeList.getElementsByTagName(VSIConstants.ELEMENT_COMMON_CODE).item(0);
				if(eleOutCommonCode != null){
					bufferPrd = eleOutCommonCode.getAttribute(VSIConstants.ATTR_CODE_VALUE);
				}			
			}
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
		
		printLogs("bufferPrd: "+bufferPrd);
		
		printLogs("Exiting getBufferPeriod method");
		
		return bufferPrd;
	}
	
	private String getLPTForLine(String sPT, Document getCalOutDoc, YFSEnvironment env) throws Exception {
		
		printLogs("Inside getLPTForLine method");
		
		String ordCpyDte=null;
		String ordCpyDteMod = sPT.substring(0,sPT.indexOf("T")+1);
		if(!YFCObject.isVoid(ordCpyDteMod)){
			ordCpyDte = ordCpyDteMod+"23:59:59";
		}
		else{
			ordCpyDte=sPT +"T" + "23:59:59";
		}
		
		printLogs("ordCpyDte: "+ordCpyDte);
		
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		Date date ;
		DateTime nextOrddateTime = new DateTime(ordCpyDte);
		int counter = 0;
		//OMS-2329 START
		ArrayList<Element> listExtnPickDate;
		String strExtnPickDate=null;
		int nExtnPickDate=0;
		listExtnPickDate = VSIUtils.getCommonCodeList(env, "BOPUS_PICKDATE", "ExtnPickDate", "DEFAULT");
		if(!listExtnPickDate.isEmpty()){
			Element eleCommonCode=listExtnPickDate.get(0);
			strExtnPickDate=eleCommonCode.getAttribute(VSIConstants.ATTR_CODE_SHORT_DESCRIPTION);

			if (!YFCObject.isVoid(strExtnPickDate)) {
				printLogs("strExtnPickDate: "+strExtnPickDate);
				nExtnPickDate = Integer.parseInt(strExtnPickDate);
			}
		}
		//OMS-2329 END
		while (counter < nExtnPickDate){
			counter ++;
			nextOrddateTime = nextOrddateTime.plusDays(1);
			boolean isHoliday = checkIfDayIsHoliday(getCalOutDoc,nextOrddateTime.toString());
			if(isHoliday){
				printLogs("Inside If its a Holiday");
				counter --;
			}
		}
		
		date = formatter.parse(nextOrddateTime.toString());
		String strLPTDate = formatter.format(date);
		
		printLogs("Date being returned: "+strLPTDate);
		
		printLogs("Exiting getLPTForLine method");
		
		return strLPTDate;
	}
	
	private String getPTForBOPUSLine(String strDate, String strOrdDte, Document getCalOutDoc) throws ParseException, ParserConfigurationException, TransformerException {
		
		printLogs("Inside getPTForBOPUSLine method");
		
		//This condition is to check if OD ~ ShiftEndTime is more than 2 hours.
		//If it is true, then PT = OD
		//If it is false, then PT = OD next day
		
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			Date date;
			DateTime orddateTime = new DateTime(strOrdDte);
			String dayOfWeek = orddateTime.dayOfWeek().getAsText();
			printLogs("dayOfWeek: "+dayOfWeek);
			dayOfWeek = dayOfWeek+"Valid";
			printLogs("Updated dayOfWeek: "+dayOfWeek);
			String shiftETStr = "";
			NodeList shiftNL  = getCalOutDoc.getElementsByTagName("Shift");
			for(int i=0; i < shiftNL.getLength(); i++){
				Element shiftEle = (Element) shiftNL.item(i);
				if(shiftEle.getAttribute(dayOfWeek) != null && shiftEle.getAttribute(dayOfWeek).equals("Y")){
					printLogs("Inside if Condition is Y");
					shiftETStr = shiftEle.getAttribute("ShiftEndTime");
					printLogs("shiftETStr: "+shiftETStr);
					strOrdDte = strOrdDte.substring(0,strOrdDte.indexOf("T")+1);
					printLogs("strOrdDte: "+strOrdDte);
					String shiftEndTime = strOrdDte+shiftETStr;
					printLogs("shiftEndTime: "+shiftEndTime);
					DateTime shiftEnddateTime = new DateTime(shiftEndTime);
					int diffHours = Hours.hoursBetween(orddateTime, shiftEnddateTime).getHours() % 24;
					int diffMins = Minutes.minutesBetween(orddateTime, shiftEnddateTime).getMinutes() % 60;
					printLogs("diffHours: "+diffHours+" diffMins: "+diffMins);
					if(( diffHours < 2) || (diffHours == 2 && diffMins == 0 ) ){
						printLogs("Inside if block");
						while(true){
							String ordCpyDte = orddateTime.toString();
							printLogs("ordCpyDte: "+ordCpyDte);
							ordCpyDte = ordCpyDte.substring(0,ordCpyDte.indexOf("T")+1)+"23:59:59";
							printLogs("Updated ordCpyDte: "+ordCpyDte);
							orddateTime = new DateTime(ordCpyDte);
							orddateTime = orddateTime.plusDays(1);
							String tempDate = orddateTime.toString();
							printLogs("tempDate: "+tempDate);
							tempDate = tempDate.substring(0,tempDate.indexOf("T"));
							printLogs("Updated tempDate: "+tempDate);
							
							boolean isHoliday = checkIfDayIsHoliday(getCalOutDoc,tempDate);
							
							if(isHoliday){
								printLogs("Inside if its a Holiday");
								continue;
							}else{
								printLogs("Inside else its not a Holiday");
								break;
							}
						}
						
					}else{
						printLogs("Inside else block");
						orddateTime = orddateTime.plusHours(2);
					}
				}
			}
			date = formatter.parse(orddateTime.toString());
			strDate = formatter.format(date);
			
			printLogs("Date being returned: "+strDate);
		
			printLogs("Exiting getPTForBOPUSLine method");
			
			return strDate;
	}
	
	private boolean checkIfDayIsHoliday(Document getCalOutDoc, String strDate) throws ParserConfigurationException, TransformerException {
		
		printLogs("Inside checkIfDayIsHoliday method");
		
		boolean isHoliday = false;
		String strXPATHDate = VSIGeneralUtils
				.formXPATHWithOneCondition(
						"/Calendars/Calendar/CalendarDayExceptions/CalendarDayException",
						"Date", strDate);
		printLogs("XPATH generated: "+strXPATHDate);
		NodeList nlCalExpList = XMLUtil.getNodeListByXpath(
				getCalOutDoc, strXPATHDate);
		if(nlCalExpList != null && nlCalExpList.getLength() > 0){
			isHoliday = true;		
		}
		
		printLogs("isHoliday: "+isHoliday);
		
		printLogs("Exiting checkIfDayIsHoliday method");
		
		return isHoliday;
	}
	
	private Document getCalendarDetails(String shipnode, YFSEnvironment env) throws YFSException, RemoteException, YIFClientCreationException, 
		ParserConfigurationException {
		
		printLogs("Inside getCalendarDetails method");
		
		Document getCalInDoc;
		
		getCalInDoc = XMLUtil.createDocument("Calendar");
		Element rootEle = getCalInDoc.getDocumentElement();
		rootEle.setAttribute("CalendarId", shipnode);
		rootEle.setAttribute("OrganizationCode", "VSI");
		
		printLogs("Input to getCalendarList api: "+SCXmlUtil.getString(getCalInDoc));
		Document getCalOutDoc = VSIUtils.invokeAPI(env, "global/template/api/VSIGetCalendarDetails.xml", "getCalendarList", getCalInDoc);
		printLogs("Output from getCalendarList api: "+SCXmlUtil.getString(getCalOutDoc));
		
		printLogs("Exiting getCalendarDetails method");
		
		return getCalOutDoc;		
	}
	
	private void printLogs(String mesg) {
		if(log.isDebugEnabled()){
			log.debug(TAG +" : "+mesg);
		}
	}
}
