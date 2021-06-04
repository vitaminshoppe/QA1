package com.vsi.oms.api.order;

import java.rmi.RemoteException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.vsi.oms.utils.VSIConstants;
import com.vsi.oms.utils.VSIUtils;
import com.vsi.oms.utils.XMLUtil;
import com.yantra.interop.japi.YIFApi;
import com.yantra.interop.japi.YIFClientCreationException;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;


/**
 * @author IBM .
 * 
 *	This class do the fix of updating ETS with Product availability date for Non POS STH orders.
 *
 */
public class VSIChangeETSForNonPOS implements VSIConstants{

	
	private YFCLogCategory log = YFCLogCategory.instance(VSIScheduleTO.class);
	YIFApi api;

	public void changeOrderForETS(YFSEnvironment env, Document inXML)
			throws Exception {
		if(log.isDebugEnabled()){
			log.info("================Inside changeOrderForETS================================");
			log.debug("VSIChangeETSForNonPOS : Printing Input XML :" +SCXmlUtil.getString(inXML));
		}
		try {
			//OMS-ETS Issue- Start
			Document docChangeSchedule= null;
			Element eleInOrder= inXML.getDocumentElement();
			String orderType= eleInOrder.getAttribute(ATTR_ORDER_TYPE);
			
			if (eleInOrder != null) {
			Element eleOrderLines = SCXmlUtil.getChildElement(eleInOrder, ELE_ORDER_LINES);
			
			if (eleOrderLines != null) {
			NodeList nlEleOrderLine= eleOrderLines.getElementsByTagName(ELE_ORDER_LINE);
			int totalNoOfOrderLine= nlEleOrderLine.getLength();
			
			if (totalNoOfOrderLine >0)
				for (int i = 0; i < totalNoOfOrderLine; i++) {	
					Element eleOrderLine=((Element) nlEleOrderLine.item(i));
					
					if(eleOrderLine!=null) {
					String strLineType = eleOrderLine.getAttribute(ATTR_LINE_TYPE);
					
					NodeList nlSchedules= eleOrderLine.getElementsByTagName("Schedule");
					int totalScheduleForodrline= nlSchedules.getLength();

					if(totalScheduleForodrline>0 && strLineType !="" && orderType != "" && (! orderType.equalsIgnoreCase("POS")) && strLineType.equalsIgnoreCase(LINETYPE_STH)){

						for (int j = 0; j < totalScheduleForodrline; j++) {
							String scheduleChangeReqFlag="N";
							Element eleSchedule = (Element) nlSchedules.item(j);
							String strShipDate = eleSchedule.getAttribute(ATTR_EXPECTED_SHIPMENT_DATE);
							
							if(dateGreaterThanCurrentDate(strShipDate)) {
								String productAvailabilitydate= eleSchedule.getAttribute("ProductAvailabilityDate");
								String expectedShipmentDate= eleSchedule.getAttribute("ExpectedShipmentDate");

								if((!dateGreaterThanCurrentDate(productAvailabilitydate)) && (dateGreaterThanCurrentDate(expectedShipmentDate))) {
									if(dateEqualsCurrentDate(productAvailabilitydate)) {
									docChangeSchedule = SCXmlUtil.createDocument("ChangeSchedule");
									Element eleChangeSchedule = docChangeSchedule.getDocumentElement();
									eleChangeSchedule.setAttribute(VSIConstants.ATTR_OVERRIDE, VSIConstants.FLAG_Y);
									eleChangeSchedule.setAttribute(ATTR_ORDER_HEADER_KEY,eleInOrder.getAttribute(ATTR_ORDER_HEADER_KEY) );
									eleChangeSchedule.setAttribute(ATTR_DOCUMENT_TYPE, eleInOrder.getAttribute(ATTR_DOCUMENT_TYPE));
									eleChangeSchedule.setAttribute("SelectMethod", "WAIT");
									
									Element eleFromSchedules = SCXmlUtil.createChild(eleChangeSchedule, "FromSchedules");
									Element eleFromSchedule = SCXmlUtil.createChild(eleFromSchedules, "FromSchedule");
									String strOrderLineSchKey= eleSchedule.getAttribute("OrderLineScheduleKey");
									eleFromSchedule.setAttribute(ATTR_PRIME_LINE_NO, eleOrderLine.getAttribute(ATTR_PRIME_LINE_NO));
									eleFromSchedule.setAttribute(ATTR_ORDER_HEADER_KEY, eleOrderLine.getAttribute(ATTR_ORDER_HEADER_KEY));
									eleFromSchedule.setAttribute(ATTR_ORDER_LINE_KEY, eleSchedule.getAttribute(ATTR_ORDER_LINE_KEY));
									eleFromSchedule.setAttribute("OrderLineScheduleKey", strOrderLineSchKey);
									eleFromSchedule.setAttribute("FromStatus", "1500");
									eleFromSchedule.setAttribute("FromExpectedShipDate", eleSchedule.getAttribute(ATTR_EXPECTED_SHIPMENT_DATE));
									Element eleToSchedules = SCXmlUtil.createChild(eleFromSchedule, "ToSchedules");
									Element eleToSchedule = SCXmlUtil.createChild(eleToSchedules, "ToSchedule");
									
									NodeList nlOrderStatus = eleOrderLine.getElementsByTagName("OrderStatus");									
									int eleOrderStatusCount= nlOrderStatus.getLength();
									
									int quantity= 0;		
									//Changes for Node Capacity- 12Sep2020- Start
									String strShipNode="";
									//Changes for Node Capacity- 12Sep2020- End
									if (eleOrderStatusCount>0) {									 
									for (int k = 0; k < eleOrderStatusCount; k++) {		
										Element eleOrderStatus = (Element) nlOrderStatus.item(k);										
										if ((eleOrderStatus.getAttribute("OrderLineScheduleKey").equalsIgnoreCase(strOrderLineSchKey)) && eleOrderStatus.getAttribute(ATTR_STATUS).equalsIgnoreCase("1500") ) {
											quantity= Integer.parseInt(eleOrderStatus.getAttribute("TotalQuantity"));
											//Changes for Node Capacity- 12Sep2020- Start
											Element orderStatusDtl= (Element) eleOrderStatus.getElementsByTagName("Details").item(0);
											if(orderStatusDtl!=null) {
											strShipNode= orderStatusDtl.getAttribute("ShipNode");
											}
											//Changes for Node Capacity- 12Sep2020- End
											break;
										}										
									}
									}
									//Changes for Node Capacity- 12Sep2020- Start
									Element eleShipNodePersoninfo= SCXmlUtil.getChildElement(eleOrderLine, "PersonInfoShipTo");
									String strCountry= eleShipNodePersoninfo.getAttribute("Country");
									if (strCountry.isEmpty()) {
										strCountry="US";
										log.info("Country for VSIChangeETSForNonPOS is null hence stamping US default");
									}
									String strCalculatedShipDate=productAvailabilitydate;
									
									if((!strShipNode.isEmpty()) && (!strShipNode.equalsIgnoreCase("9004")) && (!strShipNode.equalsIgnoreCase("9005"))) {
										scheduleChangeReqFlag="Y";
									log.info("Ship Node is not empty hence checking new ship date");
									strCalculatedShipDate= getShipmentDateAsPerNodeCapacity(env, strShipNode, strCountry);
									}								
									if((!strShipNode.isEmpty()) && (strShipNode.equalsIgnoreCase("9004") || strShipNode.equalsIgnoreCase("9005")))
										scheduleChangeReqFlag="Y";
									//Changes for Node Capacity- 12Sep2020- End
									
									if (strCalculatedShipDate.isEmpty())
										strCalculatedShipDate=productAvailabilitydate;
									
									if(quantity==0)
										continue;
									
									String StatusQuantity= Integer.toString(quantity);
									eleToSchedule.setAttribute("Quantity", StatusQuantity);
									//Changes for Node Capacity- 12Sep2020- Start
									eleToSchedule.setAttribute("ToExpectedShipDate", strCalculatedShipDate);	
									//Changes for Node Capacity- 12Sep2020- End
									
																		
								}
							}

							//creating input for change order schedule
							 						
							if(log.isDebugEnabled()){
								log.debug("Input of  change order for VSIChangeETSForNonPOS:  "+XMLUtil.getXMLString(docChangeSchedule));
							}
							
							if (docChangeSchedule != null && scheduleChangeReqFlag.equalsIgnoreCase("Y")) {
								log.info("Input for ChangeSchedule for VSIChangeETSForNonPOS is "+XMLUtil.getXMLString(docChangeSchedule));
								VSIUtils.invokeAPI(env, "changeOrderSchedule", docChangeSchedule);
							}
							
					
						}
									
					}
				}
			}

			}
				
			}
			}

		
		}
		catch (Exception e) {
			e.printStackTrace();
		}			
		}
	
		//Changes for Node Capacity- 12Sep2020- Start
		private String getShipmentDateAsPerNodeCapacity(YFSEnvironment env, String strShipNode, String strCountryCode) throws ParseException, RemoteException, YIFClientCreationException{
			String finalDate = "";
			try {

			Document docCheckNodeCapacityIn= SCXmlUtil.createDocument(ELE_RESOURCE_POOL);
			Element eleCheckNodeCapacity= docCheckNodeCapacityIn.getDocumentElement();
			eleCheckNodeCapacity.setAttribute(ATTR_PROVIDER_ORG_CODE, ENT_VSI);
			eleCheckNodeCapacity.setAttribute(ELE_NODE, strShipNode);
			eleCheckNodeCapacity.setAttribute(ATTR_ITEM_GROUP_CODE, ITEM_GROUP_CODE_PROD);
			eleCheckNodeCapacity.setAttribute(ATTR_CAPACITY_ORG_CODE, ENT_VSI_CAT);
			eleCheckNodeCapacity.setAttribute(ATTR_CAPACITY_UOM, CAPACITY_UOM_VALUE);
			Element elePersonInfo= SCXmlUtil.createChild(eleCheckNodeCapacity, ELE_PERSON_INFO);			
			if (strCountryCode.isEmpty())
				elePersonInfo.setAttribute(ATTR_COUNTRY, US);
			else
				elePersonInfo.setAttribute(ATTR_COUNTRY, strCountryCode);
					
			log.info("Input of  Get Resource Pool Capacity for VSIChangeETSForNonPOS:  "+XMLUtil.getXMLString(docCheckNodeCapacityIn));		
			Document docCheckNodeCapacityOut= VSIUtils.invokeAPI(env,API_GET_RESOURCE_POOL_CAPACITY, docCheckNodeCapacityIn);
			log.info("Output from  Get Resource Pool Capacity for VSIChangeETSForNonPOS:  "+XMLUtil.getXMLString(docCheckNodeCapacityOut));	
			
			Element eleCheckNodeCapacityOut= docCheckNodeCapacityOut.getDocumentElement();			
			if (eleCheckNodeCapacityOut!=null) 
			{
			NodeList nlResourcePool= eleCheckNodeCapacityOut.getElementsByTagName(ELE_RESOURCE_POOL);
			SimpleDateFormat dateFormat = new SimpleDateFormat(YYYY_MM_DD);						
			for (int i = 0; i < nlResourcePool.getLength(); i++) 
			 {
				Element eleResourcePool= (Element) nlResourcePool.item(i);				
				if(eleResourcePool != null) 
				{					
				  String strResourcePoolId= eleResourcePool.getAttribute(ATTR_RESOURCE_POOL_ID);				
				  if ((!strResourcePoolId.isEmpty()) && strResourcePoolId.equalsIgnoreCase(BOSS_VALUE)) 
				  {
					  NodeList nlCapacityDates= eleResourcePool.getElementsByTagName(ELE_DATE);
						int totalDate= nlCapacityDates.getLength();	
						log.info("totalDate => "+totalDate);
						Map<Date, String> capacity = new HashMap<Date, String>();
						for (int k = 0; k < totalDate; k++) 
						{			
							Element eleDate = (Element) nlCapacityDates.item(k);
							String strAvailability= eleDate.getAttribute(ELE_AVAILABILITY);
							log.info("strAvailability => "+strAvailability);
		                	Date date = dateFormat.parse(eleDate.getAttribute(ELE_DATE));
		                	capacity.put(date, strAvailability);
						}
						ArrayList<Date> arrayOfDates =new ArrayList<Date>(capacity.keySet());
						Collections.sort(arrayOfDates);						
						for (Date dateList : arrayOfDates) 
						{
							log.info("dateList => "+dateList+"Availability => "+capacity.get(dateList));
								Double availableQty= Double.parseDouble(capacity.get(dateList));
								 if (availableQty > 0)
								 {
									finalDate = dateFormat.format(dateList);
									 break;
								 }					                						
						}
				  }
				}
			 }
			}	
			log.info("finalDate => "+finalDate);
			} catch (YFSException e) 
			{
				e.printStackTrace();
			}
			log.info("finalDate before return => "+finalDate);
			return finalDate;
		}
		//Changes for Node Capacity- 12Sep2020- End
		
	private boolean dateGreaterThanCurrentDate(String strDate) throws ParseException{
		boolean bResult = false;

		strDate = strDate.substring(0, 10);
		// Input date will be passed to sterling in below format
		DateFormat sdf = new SimpleDateFormat(VSIConstants.YYYY_MM_DD);
		
		Date date = new Date();
		String strCurrentDate = sdf.format(date);
		
		
		Date dPassedDate = sdf.parse(strDate);
		Date dCurrentDate = sdf.parse(strCurrentDate);
		
		
		if(dPassedDate.after(dCurrentDate)){
			bResult = true; 
		}
		
		return bResult;
	}
	
	private boolean dateEqualsCurrentDate(String strDate) throws ParseException{
		boolean bResult = false;

		strDate = strDate.substring(0, 10);
		// Input date will be passed to sterling in below format
		DateFormat sdf = new SimpleDateFormat(VSIConstants.YYYY_MM_DD);
		
		Date date = new Date();
		String strCurrentDate = sdf.format(date);
		
		
		Date dPassedDate = sdf.parse(strDate);
		Date dCurrentDate = sdf.parse(strCurrentDate);
		
		int comparedInt= dPassedDate.compareTo(dCurrentDate);
		
		if (comparedInt==0) {
			bResult= true;
		}
		
		return bResult;
	}

}
