package com.vsi.oms.api.web;



import java.rmi.RemoteException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.TimeZone;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.common.joda.time.DateTimeZone;
import org.elasticsearch.common.joda.time.Hours;
import org.elasticsearch.common.joda.time.Minutes;
import org.elasticsearch.common.joda.time.format.DateTimeFormat;
import org.elasticsearch.common.joda.time.format.DateTimeFormatter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.vsi.oms.utils.VSIConstants;
import com.vsi.oms.utils.VSIGeneralUtils;
import com.vsi.oms.utils.VSIUtils;
import com.vsi.oms.utils.XMLUtil;
import com.yantra.interop.japi.YIFClientCreationException;
import com.yantra.yfc.core.YFCObject;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;

public class VSIGetPTandLPTdetails {

	//OMS-3027 Changes -- Start
	private static YFCLogCategory log = YFCLogCategory.instance(VSIGetPTandLPTdetails.class);	
	private static final String TAG = VSIGetPTandLPTdetails.class.getSimpleName();
	//OMS-3027 Changse -- End
	
	public static Document getPTandLPTdetails(YFSEnvironment env, Document inXML) throws Exception
    {
		Element eleOrder = inXML.getDocumentElement();
		
		if(eleOrder.getElementsByTagName(VSIConstants.ELE_ORDER_LINE).getLength() > 0){
		
		Element eleOrderExtn = (Element) eleOrder.getElementsByTagName("Extn").item(0);
		String strExtnOrdDte = null;
		String shipnode = "";
		if(eleOrderExtn != null){
			strExtnOrdDte = eleOrderExtn.getAttribute("ExtnOrderDate");
		}
		
		Element orderLineEle = (Element) eleOrder.getElementsByTagName(VSIConstants.ELE_ORDER_LINE).item(0);
		shipnode = orderLineEle.getAttribute(VSIConstants.ATTR_SHIP_NODE);
		
		if(strExtnOrdDte != null && !strExtnOrdDte.trim().equals("") && shipnode != null && !shipnode.trim().equals("")){
			
		
		
		String strDate = "";
		Date date;
		String bopusPT = "";
		String bopusLPT = "";
		String bostsPT = "";
		String bostsLPT = "";
		int iShip = 0;
		int iPick = 0;
		try {
			
			
				NodeList orderLineList = inXML
						.getElementsByTagName(VSIConstants.ELE_ORDER_LINE);
				int orderLineLength = orderLineList.getLength();
				for (int i = 0; i < orderLineLength; i++) {
					Element orderLineElement = (Element) inXML
							.getElementsByTagName(
									VSIConstants.ELE_ORDER_LINE).item(i);
					String sLineType = orderLineElement
							.getAttribute(VSIConstants.ATTR_LINE_TYPE);
					
				if (sLineType.equalsIgnoreCase("SHIP_TO_STORE") ) {

					iShip++;
					

				} else
					iPick++;
			}
			
			
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-mm-dd");
		date = formatter.parse(strExtnOrdDte);
		strDate = formatter.format(date);
		
		

		
		Document getCalOutDoc = getCalendarDetails(shipnode, env);
			
			int counter = 1;
			boolean isHoliday = false;
			if(getCalOutDoc != null){
				
				// This loop is to validate if the OrderDate is a Holiday.
				// If it is holiday, it will take the next day and exit the loop
				// If it is not holiday then it will exit the loop
				
				if(iPick > 0){
					while(counter > 0){
						
						isHoliday = checkIfDayIsHoliday(getCalOutDoc,strDate);
//System.out.println("Is it holiday" + isHoliday);
						if(isHoliday){
							
							//System.out.println("Is it holiday 1 " + isHoliday);
							DateTime dtOrg = new DateTime(strDate);
							DateTime dtPlusOne = dtOrg.plusDays(1);
							date = formatter.parse(dtPlusOne.toString());
							strDate = formatter.format(date);
							
							counter ++;
							
							
							
							
						}else{
							break;
						}
						
					}
					
					if(counter == 1){
						bopusPT = getPTForBOPUSLine(strDate,strExtnOrdDte,getCalOutDoc);
						

						
					}else{
						bopusPT = strDate;
						
					}
					 
					bopusLPT = getLPTForLine(bopusPT,getCalOutDoc,env);
				}
				
				if(iShip > 0){
					bostsPT = getPTForBOSTSLine(strExtnOrdDte,shipnode,env);
					isHoliday = checkIfDayIsHoliday(getCalOutDoc,bostsPT);
					if(isHoliday){
						DateTime dtOrg = new DateTime(bostsPT);
						DateTime dtPlusOne = dtOrg.plusDays(1);
						date = formatter.parse(dtPlusOne.toString());
						bostsPT = formatter.format(date);
					}
					bostsLPT = getLPTForLine(bostsPT,getCalOutDoc,env);
				}
				
				
			}
			
			
			for (int i = 0; i < orderLineLength; i++) {
				Element orderLineElement = (Element) orderLineList.item(i);
				String sLineType = orderLineElement
						.getAttribute(VSIConstants.ATTR_LINE_TYPE);
				
				
			if (sLineType.equalsIgnoreCase("SHIP_TO_STORE") ) {
				Element ordLneExtnEle = (Element) orderLineElement.getElementsByTagName("Extn").item(0);
				ordLneExtnEle.setAttribute("ExtnPickDate", bostsPT);
				ordLneExtnEle.setAttribute("ExtnLastPickDate", bostsLPT);
				Element orderDatesEle = inXML.createElement("OrderDates");
				orderLineElement.appendChild(orderDatesEle);
					Element orderDateEle = inXML.createElement("OrderDate");
					orderDateEle.setAttribute("DateTypeId", "ExtnPickDate");
					orderDateEle.setAttribute("ActualDate", bostsPT);
					orderDatesEle.appendChild(orderDateEle);
					
					orderDateEle = inXML.createElement("OrderDate");
					orderDateEle.setAttribute("DateTypeId", "ExtnLastPickDate");
					orderDateEle.setAttribute("ActualDate", bostsLPT);
					orderDatesEle.appendChild(orderDateEle);
					
					
				

			} else{
				Element ordLneExtnEle = (Element) orderLineElement.getElementsByTagName("Extn").item(0);
				ordLneExtnEle.setAttribute("ExtnPickDate", bopusPT);
				ordLneExtnEle.setAttribute("ExtnLastPickDate", bopusLPT);
				
				Element orderDatesEle = inXML.createElement("OrderDates");
				orderLineElement.appendChild(orderDatesEle);
					Element orderDateEle = inXML.createElement("OrderDate");
					orderDateEle.setAttribute("DateTypeId", "ExtnPickDate");
					orderDateEle.setAttribute("ActualDate", bopusPT);
					orderDatesEle.appendChild(orderDateEle);
					
					orderDateEle = inXML.createElement("OrderDate");
					orderDateEle.setAttribute("DateTypeId", "ExtnLastPickDate");
					orderDateEle.setAttribute("ActualDate", bopusLPT);
					orderDatesEle.appendChild(orderDateEle);
				
			}
				
		}
		
			
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (YIFClientCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		}
		}
		return inXML;
		
    }
	
	
	private static Document getCalendarDetails(String shipnode, YFSEnvironment env) throws YFSException, RemoteException, YIFClientCreationException, ParserConfigurationException {
		Document getCalInDoc;
		
		getCalInDoc = XMLUtil.createDocument("Calendar");
		Element rootEle = getCalInDoc.getDocumentElement();
		rootEle.setAttribute("CalendarId", shipnode);
		rootEle.setAttribute("OrganizationCode", "VSI");
		Document getCalOutDoc = VSIUtils.invokeAPI(env, "global/template/api/VSIGetCalendarDetails.xml", "getCalendarList", getCalInDoc);
		return getCalOutDoc;
	}


	public static Document stampPTAndLPTForPickedOrders(YFSEnvironment env, Document inXML) throws Exception
    {
		//OMS-3027 Changes -- Start
		printLogs("================Inside VSIGetPTandLPTdetails Class and stampPTAndLPTForPickedOrders Method================");
		printLogs("Printing Input XML: "+SCXmlUtil.getString(inXML));
		
		HashSet<String> setShipNode = new HashSet<String>();
		//OMS-3027 Changes -- End
		
		Calendar localCal=Calendar.getInstance(); 
		Date curDate=localCal.getTime();
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		Document getOrgListIpDoc;
		String extnZoneName = null;
		Element orderEle = inXML.getDocumentElement();
		//OMS-3027 Changes -- Start
		/*Element orderLineEle = (Element) orderEle.getElementsByTagName(VSIConstants.ELE_ORDER_LINE).item(0);
		String shipNode = orderLineEle.getAttribute(VSIConstants.ATTR_SHIP_NODE);*/
		//OMS-3027 Changes -- End
		int iShip =0;
		int iPick =0;
		String bopusPT = "";
		String bopusLPT = "", bopusCLPT = "";
		String bostsPT = "";
		String bostsLPT = "", bostsCLPT = ""; 
		try {
			//OMS-3027 Changes -- Start
			Element eleOrderLines=SCXmlUtil.getChildElement(orderEle, VSIConstants.ELE_ORDER_LINES);
			NodeList orderLineList = eleOrderLines.getElementsByTagName(VSIConstants.ELE_ORDER_LINE);
			int orderLineLength = orderLineList.getLength();
			for (int i = 0; i < orderLineLength; i++){				
				Element orderLineElement = (Element) orderLineList.item(i);
				String sLineType = orderLineElement.getAttribute(VSIConstants.ATTR_LINE_TYPE);
				printLogs("Line Type: "+sLineType);
				if(VSIConstants.LINETYPE_PUS.equals(sLineType) || VSIConstants.LINETYPE_STS.equals(sLineType)){
					String shipNode=orderLineElement.getAttribute(VSIConstants.ATTR_SHIP_NODE);
					setShipNode.add(shipNode);
					if(sLineType.equalsIgnoreCase(VSIConstants.LINETYPE_STS)){
						iShip++;
					} else if(sLineType.equalsIgnoreCase(VSIConstants.LINETYPE_PUS)){
						iPick++;
					}						
				}
			}
			printLogs("Ship Node HashSet Contents after processing all lines: "+Arrays.toString(setShipNode.toArray()));
			
			for(String shpNd:setShipNode){
				printLogs("Ship Node being processed: "+shpNd);
				//OMS-3027 Changes -- End
				getOrgListIpDoc = XMLUtil.createDocument(VSIConstants.ELE_ORGANIZATION);
				//OMS-3027 Changes -- Start
				getOrgListIpDoc.getDocumentElement().setAttribute(VSIConstants.ATTR_ORG_CODE, shpNd);
				printLogs("Input to getOrganizationList API: "+SCXmlUtil.getString(getOrgListIpDoc));
				//OMS-3027 Changes -- End
				Document getOrgListOpDoc = VSIUtils.invokeAPI(env, "global/template/api/VSIGetOrgListTemplate.xml", "getOrganizationList", getOrgListIpDoc);
				//OMS-3027 Changes -- Start
				printLogs("Output from getOrganizationList API: "+SCXmlUtil.getString(getOrgListOpDoc));
				//OMS-3027 Changes -- End
				if(getOrgListOpDoc != null){
					NodeList orgNL = getOrgListOpDoc.getElementsByTagName("Organization");
					if(orgNL.getLength() > 0){
						Element eleOrg = (Element) orgNL.item(0);
						Element extnEle = (Element) eleOrg.getElementsByTagName("Extn").item(0);
						if(extnEle != null){
							extnZoneName = extnEle.getAttribute("ExtnZoneName");
							if(extnZoneName != null && !extnZoneName.trim().equals("")){
								//OMS-3027 Changes -- Start
								printLogs("extnZoneName: "+extnZoneName);
								//OMS-3027 Changes -- End
								TimeZone toTimeZone = TimeZone.getTimeZone(extnZoneName);
								formatter.setTimeZone(toTimeZone);
								String convertedDate = formatter.format(curDate);
								//OMS-3027 Changes -- Start
								printLogs("convertedDate: "+convertedDate);
								//OMS-3027 Changes -- End
								Document getCalOutDoc = getCalendarDetails(shpNd, env);
								int counter = 1;
								boolean isHoliday = false;
								String strDate = "";
								if(getCalOutDoc != null){
									
									// This loop is to validate if the OrderDate is a Holiday.
									// If it is holiday, it will take the next day and exit the loop
									// If it is not holiday then it will exit the loop
									SimpleDateFormat sdfformatter = new SimpleDateFormat("yyyy-mm-dd");
									Date date = sdfformatter.parse(convertedDate);
									strDate = formatter.format(date);
									//OMS-3027 Changes -- Start
									printLogs("strDate: "+strDate);
									//OMS-3027 Changes -- End
									if(iPick > 0){
										//OMS-3027 Changes -- Start
										printLogs("Inside if condition when BOPUS lines are present");
										//OMS-3027 Changes -- End
										while(counter > 0){
											
											isHoliday = checkIfDayIsHoliday(getCalOutDoc,strDate);
											
											if(isHoliday){
												//OMS-3027 Changes -- Start
												printLogs("Inside if its a Holiday");
												//OMS-3027 Changes -- End
												DateTime dtOrg = new DateTime(strDate);
												DateTime dtPlusOne = dtOrg.plusDays(1);
												date = formatter.parse(dtPlusOne.toString());
												strDate = formatter.format(date);
												//OMS-3027 Changes -- Start
												printLogs("New value of strDate: "+strDate);
												//OMS-3027 Changes -- End
												counter ++;
											}else{
												//OMS-3027 Changes -- Start
												printLogs("Inside if its not a Holiday");
												//OMS-3027 Changes -- End
												break;
											}
											
										}
										
										if(counter == 1){
											//OMS-3027 Changes -- Start
											printLogs("counter value is 1");
											//OMS-3027 Changes -- End
											bopusPT = getPTForBOPUSLine(strDate,convertedDate,getCalOutDoc);
											
											
										}else{
											//OMS-3027 Changes -- Start
											printLogs("counter value not equals 1");
											//OMS-3027 Changes -- End
											bopusPT = strDate;
										}
										 
										bopusLPT = getLPTForLine(bopusPT,getCalOutDoc,env);
										//OMS-3027 Changes -- Start
										printLogs("bopusLPT: "+bopusLPT);
										//OMS-3027 Changes -- End
										bopusCLPT = getCLPTForLine(bopusPT,getCalOutDoc,env);
										//OMS-3027 Changes -- Start
										printLogs("bopusCLPT: "+bopusCLPT);
										//OMS-3027 Changes -- End
									}
									
									if(iShip > 0){
										//OMS-3027 Changes -- Start
										printLogs("Inside if condition when STS lines are present");
										//OMS-3027 Changes -- End
										bostsPT = getPTForBOPUSLine(strDate,convertedDate,getCalOutDoc);
										//OMS-3027 Changes -- Start
										printLogs("bostsPT: "+bostsPT);
										//OMS-3027 Changes -- End
										bostsLPT = getLPTForLine(bostsPT,getCalOutDoc,env);
										//OMS-3027 Changes -- Start
										printLogs("bostsLPT: "+bostsLPT);
										//OMS-3027 Changes -- End
										bostsCLPT = getCLPTForLine(bostsPT,getCalOutDoc,env);
										//OMS-3027 Changes -- Start
										printLogs("bostsCLPT: "+bostsCLPT);
										//OMS-3027 Changes -- End
									}
									
									for(int i = 0; i < orderLineLength; i++) {
										Element orderLineElement = (Element) orderLineList.item(i);
										//OMS-3027 Changes -- Start
										String strPrimeLineNo=orderLineElement.getAttribute(VSIConstants.ATTR_PRIME_LINE_NO);
										String strOrderLnKey=orderLineElement.getAttribute(VSIConstants.ATTR_ORDER_LINE_KEY);
										printLogs("Order Line with Prime Line number "+strPrimeLineNo+" and order line key "+strOrderLnKey+" is being processed");								
										String shipNode=orderLineElement.getAttribute(VSIConstants.ATTR_SHIP_NODE);
										printLogs("Ship Node is: "+shipNode);
										if(shpNd.equals(shipNode)){
											printLogs("Ship Node Matches");
											//OMS-3027 Changes -- End
											String sLineType = orderLineElement.getAttribute(VSIConstants.ATTR_LINE_TYPE);
											//OMS-3027 Changes -- Start
											printLogs("Line Type is: "+sLineType);
											//OMS-3027 Changes -- End
											if(sLineType.equalsIgnoreCase("SHIP_TO_STORE")){
												Element ordLneExtnEle = (Element) orderLineElement.getElementsByTagName("Extn").item(0);
												ordLneExtnEle.setAttribute("ExtnPickDate", bostsPT);
												ordLneExtnEle.setAttribute("ExtnLastPickDate", bostsLPT);
												ordLneExtnEle.setAttribute("ExtnLastPickDateForCancel", bostsCLPT);
												
												Element orderDatesEle = inXML.createElement("OrderDates");
												orderLineElement.appendChild(orderDatesEle);
												
												Element orderDateEle = inXML.createElement("OrderDate");
												orderDateEle.setAttribute("DateTypeId", "ExtnPickDate");
												orderDateEle.setAttribute("ActualDate", bostsPT);
												orderDatesEle.appendChild(orderDateEle);
													
												orderDateEle = inXML.createElement("OrderDate");
												orderDateEle.setAttribute("DateTypeId", "ExtnLastPickDate");
												orderDateEle.setAttribute("ActualDate", bostsLPT);
												orderDatesEle.appendChild(orderDateEle);
													
												orderDateEle = inXML.createElement("OrderDate");
												orderDateEle.setAttribute("DateTypeId", "ExtnLastPickDateForCancel");
												orderDateEle.setAttribute("ActualDate", bostsCLPT);
												orderDatesEle.appendChild(orderDateEle);									
			
											}else if(sLineType.equalsIgnoreCase("PICK_IN_STORE")){
												Element ordLneExtnEle = (Element) orderLineElement.getElementsByTagName("Extn").item(0);
												ordLneExtnEle.setAttribute("ExtnPickDate", bopusPT);
												ordLneExtnEle.setAttribute("ExtnLastPickDate", bopusLPT);
												ordLneExtnEle.setAttribute("ExtnLastPickDateForCancel", bopusCLPT);
												
												Element orderDatesEle = inXML.createElement("OrderDates");
												orderLineElement.appendChild(orderDatesEle);
												
												Element orderDateEle = inXML.createElement("OrderDate");
												orderDateEle.setAttribute("DateTypeId", "ExtnPickDate");
												orderDateEle.setAttribute("ActualDate", bopusPT);
												orderDatesEle.appendChild(orderDateEle);
													
												orderDateEle = inXML.createElement("OrderDate");
												orderDateEle.setAttribute("DateTypeId", "ExtnLastPickDate");
												orderDateEle.setAttribute("ActualDate", bopusLPT);
												orderDatesEle.appendChild(orderDateEle);
													
												orderDateEle = inXML.createElement("OrderDate");
												orderDateEle.setAttribute("DateTypeId", "ExtnLastPickDateForCancel");
												orderDateEle.setAttribute("ActualDate", bopusCLPT);
												orderDatesEle.appendChild(orderDateEle);
												
											}
									//OMS-3027 Changes -- Start
									}
									printLogs("Order Line after stamping PT and LPT dates: "+SCXmlUtil.getString(orderLineElement));
									printLogs("Order Line with Prime Line number "+strPrimeLineNo+" and order line key "+strOrderLnKey+" processing completed");
									//OMS-3027 Changes -- End										
								}
								//OMS-3027 Changes -- Start
								printLogs("Ship Node "+shpNd+" processing completed");
								//OMS-3027 Changes -- End									
								}								
							}
						}
					}
				}
			//OMS-3027 Changes -- Start
			printLogs("Ship Node "+shpNd+" processing completed");
			}
			orderEle.setAttribute("Action", "MODIFY");
			orderEle.setAttribute("Override", "Y");
			//OMS-3027 Changes -- End
		}catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (YIFClientCreationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ParserConfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (TransformerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		//OMS-3027 Changes -- Start
		printLogs("Output while exiting stampPTAndLPTForPickedOrders method: "+SCXmlUtil.getString(inXML));
		printLogs("Exiting VSIGetPTandLPTdetails Class and stampPTAndLPTForPickedOrders Method");
		//OMS-3027 Changes -- End
		//System.out.println("Printing changeOrder Input XML: "+XMLUtil.getXMLString(inXML));
		return inXML;
		
		
    }
	
	//OMS-3027 Changes -- Start
	private static void printLogs(String mesg) {
		if(log.isDebugEnabled()){
			log.debug(TAG +" : "+mesg);
		}
	}	
	//OMS-3027 Changes -- End
	
	public static Document stampExtnOrderDate(YFSEnvironment env, Document inXML) throws YFSException
    {
		Element orderEle = inXML.getDocumentElement();
		
		if(orderEle.getElementsByTagName(VSIConstants.ELE_ORDER_LINE).getLength() > 0){
			
			Element orderLineEle = (Element) orderEle.getElementsByTagName(VSIConstants.ELE_ORDER_LINE).item(0);	
		
		String shipNode = orderLineEle.getAttribute(VSIConstants.ATTR_SHIP_NODE);
		String dateInString = orderEle.getAttribute(VSIConstants.ATTR_ORDER_DATE);
		
		if(shipNode != null && !shipNode.trim().equals("") && dateInString != null && !dateInString.trim().equals("")){
			
		
		
		
		Document getOrgListIpDoc;
		String extnZoneName = null;
		try {
			getOrgListIpDoc = XMLUtil.createDocument(VSIConstants.ELE_ORGANIZATION);
			getOrgListIpDoc.getDocumentElement().setAttribute(VSIConstants.ATTR_ORG_CODE, shipNode);
			Document getOrgListOpDoc = VSIUtils.invokeAPI(env, "global/template/api/VSIGetOrgListTemplate.xml", "getOrganizationList", getOrgListIpDoc);
			if(getOrgListOpDoc != null){
				NodeList orgNL = getOrgListOpDoc.getElementsByTagName("Organization");
				if(orgNL.getLength() > 0){
					Element eleOrg = (Element) orgNL.item(0);
					Element extnEle = (Element) eleOrg.getElementsByTagName("Extn").item(0);
					if(extnEle != null){
						extnZoneName = extnEle.getAttribute("ExtnZoneName");
						if(extnZoneName != null && !extnZoneName.trim().equals("")){
							TimeZone toTimeZone = TimeZone.getTimeZone(extnZoneName);
							SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
							
							Date date = sdf.parse(dateInString); 
							DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
							formatter.setTimeZone(toTimeZone);
						
							Element extnOrdEle = (Element) orderEle.getElementsByTagName("Extn").item(0);
							if(extnOrdEle == null){
								extnOrdEle = inXML.createElement("Extn");
								orderEle.appendChild(extnOrdEle);								
								
							}
							extnOrdEle.setAttribute("ExtnOrderDate",formatter.format(date));
							Element orderDatesEle = inXML.createElement("OrderDates");
							orderEle.appendChild(orderDatesEle);
							Element orderDateEle = inXML.createElement("OrderDate");
							orderDateEle.setAttribute("DateTypeId", "ExtnOrderDate");
							orderDateEle.setAttribute("ActualDate", formatter.format(date));
							orderDatesEle.appendChild(orderDateEle);
						
					}
				}
			}
		}
		}catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (YIFClientCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		}
		}
		return inXML;
		
    }

	private static String getPTForBOSTSLine(String strOrdDte, String shipnode,YFSEnvironment env) throws ParserConfigurationException, YFSException, RemoteException, YIFClientCreationException, ParseException {
		String extnLeadTime = "";
		String ordCpyDte = strOrdDte.substring(0,strOrdDte.indexOf("T")+1)+"23:59:59";
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		Date date ;
		DateTime ordDT = new DateTime(ordCpyDte);
		Document getOrgListIpDoc = XMLUtil.createDocument(VSIConstants.ELE_ORGANIZATION);
		getOrgListIpDoc.getDocumentElement().setAttribute(VSIConstants.ATTR_ORG_CODE, shipnode);
		Document getOrgListOpDoc = VSIUtils.invokeAPI(env, "global/template/api/VSIGetOrgListTemplate.xml", "getOrganizationList", getOrgListIpDoc);
		if(getOrgListOpDoc != null){
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
		return strPTDate;
		
		
	}

	

	private static String getBufferPeriod(YFSEnvironment env) {
		String bufferPrd = null;
		try{
		Document docForGetCommonCodeList = XMLUtil.createDocument(VSIConstants.ELEMENT_COMMON_CODE);

		Element eleCommonCode = docForGetCommonCodeList.getDocumentElement();
		eleCommonCode.setAttribute(VSIConstants.ATTR_ENTERPRISE_CODE,"VSI.com");
		eleCommonCode.setAttribute(VSIConstants.ATTR_CODE_TYPE,"BUFFER_DAYS_BOSTS");

		Document docAfterGetCommonCodeList = VSIUtils.invokeAPI(env,VSIConstants.API_COMMON_CODE_LIST,docForGetCommonCodeList);
		
		
		if (docAfterGetCommonCodeList != null) {
			Element eleOutCommonCode=(Element) docAfterGetCommonCodeList.getElementsByTagName(VSIConstants.ELEMENT_COMMON_CODE).item(0);
			if(eleOutCommonCode != null){
				bufferPrd = eleOutCommonCode.getAttribute(VSIConstants.ATTR_CODE_VALUE);
			}
		
			
		}}
		catch(Exception ex) {
			ex.printStackTrace();
		}
		
		return bufferPrd;
	}
	

	private static String getLPTForLine(String sPT, Document getCalOutDoc, YFSEnvironment env) throws Exception {
	
		String ordCpyDte=null;
		String ordCpyDteMod = sPT.substring(0,sPT.indexOf("T")+1);
		if(!YFCObject.isVoid(ordCpyDteMod)){
			ordCpyDte = ordCpyDteMod+"23:59:59";

		}
		else{
			ordCpyDte=sPT +"T" + "23:59:59";
		}


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
				 nExtnPickDate = Integer.parseInt(strExtnPickDate);
			}
		}
		//OMS-2329 END
		while (counter < nExtnPickDate){
			counter ++;
			nextOrddateTime = nextOrddateTime.plusDays(1);
			boolean isHoliday = checkIfDayIsHoliday(getCalOutDoc,nextOrddateTime.toString());
			if(isHoliday){
				counter --;
			}
		}
		
		date = formatter.parse(nextOrddateTime.toString());
		String strLPTDate = formatter.format(date);
		return strLPTDate;
	}
	
	private static String getCLPTForLine(String sPT, Document getCalOutDoc, YFSEnvironment env) throws Exception 
	{		
		String ordCpyDte=null;
		String ordCpyDteMod = sPT.substring(0,sPT.indexOf("T")+1);
		if(!YFCObject.isVoid(ordCpyDteMod)){
			ordCpyDte = ordCpyDteMod+"23:59:59";
		}
		else{
			ordCpyDte=sPT +"T" + "23:59:59";
		}
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		DateTime nextOrddateTime = new DateTime(ordCpyDte);
		int counter = 0, nExtnPickDate=0;
		String strExtnPickDate=null;
		ArrayList<Element> listExtnCancelLastPickDate = VSIUtils.getCommonCodeList(env, VSIConstants.BOPUS_CANCEL_COMMON_CODE, VSIConstants.EXTN_CANCEL_LPT_COMMON_CODE, VSIConstants.ATTR_DEFAULT);
		if(!listExtnCancelLastPickDate.isEmpty())
		{
			Element eleCommonCode=listExtnCancelLastPickDate.get(0);
			strExtnPickDate=eleCommonCode.getAttribute(VSIConstants.ATTR_CODE_SHORT_DESCRIPTION);
			if (!YFCObject.isVoid(strExtnPickDate))
				 nExtnPickDate = Integer.parseInt(strExtnPickDate);
		}
		while (counter < nExtnPickDate)
		{
			counter ++;
			nextOrddateTime = nextOrddateTime.plusDays(1);
			boolean isHoliday = checkIfDayIsHoliday(getCalOutDoc,nextOrddateTime.toString());
			if(isHoliday)
				counter --;
		}		
		Date date = formatter.parse(nextOrddateTime.toString());
		String strLPTDate = formatter.format(date);
		return strLPTDate;
	}

	private static String getPTForBOPUSLine(String strDate, String strOrdDte, Document getCalOutDoc) throws ParseException, ParserConfigurationException, TransformerException {
		//This condition is to check if OD ~ ShiftEndTime is more than 2 hours.
		//If it is true, then PT = OD
		//If it is false, then PT = OD next day
		
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			Date date;
			DateTime orddateTime = new DateTime(strOrdDte);
			String dayOfWeek = orddateTime.dayOfWeek().getAsText();
			dayOfWeek = dayOfWeek+"Valid";
			String shiftETStr = "";
			NodeList shiftNL  = getCalOutDoc.getElementsByTagName("Shift");
			for(int i=0; i < shiftNL.getLength(); i++){
				Element shiftEle = (Element) shiftNL.item(i);
				if(shiftEle.getAttribute(dayOfWeek) != null && shiftEle.getAttribute(dayOfWeek).equals("Y")){
					shiftETStr = shiftEle.getAttribute("ShiftEndTime");
					strOrdDte = strOrdDte.substring(0,strOrdDte.indexOf("T")+1);
					String shiftEndTime = strOrdDte+shiftETStr;
					DateTime shiftEnddateTime = new DateTime(shiftEndTime);
					int diffHours = Hours.hoursBetween(orddateTime, shiftEnddateTime).getHours() % 24;
					int diffMins = Minutes.minutesBetween(orddateTime, shiftEnddateTime).getMinutes() % 60;
					if(( diffHours < 2) || (diffHours == 2 && diffMins == 0 ) ){
						
						while(true){
							String ordCpyDte = orddateTime.toString();
							ordCpyDte = ordCpyDte.substring(0,ordCpyDte.indexOf("T")+1)+"23:59:59";
							orddateTime = new DateTime(ordCpyDte);
							orddateTime = orddateTime.plusDays(1);
							String tempDate = orddateTime.toString();
							tempDate = tempDate.substring(0,tempDate.indexOf("T"));
							//System.out.println("tempDate :"+tempDate);
							boolean isHoliday = checkIfDayIsHoliday(getCalOutDoc,tempDate);
							//System.out.println("isHoliday :"+isHoliday);
							if(isHoliday){
								continue;
							}else{
								break;
							}
						}
						
					}else{
						orddateTime = orddateTime.plusHours(2);
					}
				}
			}
			date = formatter.parse(orddateTime.toString());
			strDate = formatter.format(date);
		
		return strDate;
	}

	private static boolean checkIfDayIsHoliday(Document getCalOutDoc, String strDate) throws ParserConfigurationException, TransformerException {
		//System.out.println("strDate in method" + strDate);
		boolean isHoliday = false;
		String strXPATHDate = VSIGeneralUtils
				.formXPATHWithOneCondition(
						"/Calendars/Calendar/CalendarDayExceptions/CalendarDayException",
						"Date", strDate);
		//System.out.println("calendar date  in method" + strXPATHDate);

		NodeList nlCalExpList = XMLUtil.getNodeListByXpath(
				getCalOutDoc, strXPATHDate);
		if(nlCalExpList != null && nlCalExpList.getLength() > 0){
			isHoliday = true;
		//System.out.println("Inside Holiday");
		}
		
		return isHoliday;
	}
}
