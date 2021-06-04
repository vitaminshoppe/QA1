package com.vsi.oms.api;


import java.util.ArrayList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.vsi.oms.utils.VSIConstants;
import com.yantra.yfc.core.YFCObject;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfs.core.YFSSystem;
import com.yantra.yfs.japi.YFSEnvironment;

public class VSINonEDIWholesaleOrderOnCreate {
	/**
	 * This custom implementation is used to create the NON EDI wholesale Orders
	 * We will manually update the Elements/Atrributes we require in OMS
	 * 
	 * @param env
	 * @param inXML
	 * @throws Exception
	 */
	private YFCLogCategory log = YFCLogCategory.instance(VSINonEDIWholesaleOrderOnCreate.class);

	public Document createNonEDIOrder(YFSEnvironment env, Document inXML) throws Exception {
		log.beginTimer("VSINonEDIWholesaleOrderOnCreate.createNonEDIOrder");
		if(log.isDebugEnabled()){
			log.verbose("Printing Input XML :" + SCXmlUtil.getString(inXML));
			log.info("================Inside VSINonEDIWholesaleOrderOnCreate================================");
		}

		try {
			
			String strFirstName=YFSSystem.getProperty("PERSON_INFO_FIRSTNAME");
			String strAddressLine1=YFSSystem.getProperty("PERSON_INFO_ADDR_LINE_1");
			String strAddressLine2=YFSSystem.getProperty("PERSON_INFO_ADDR_LINE_2");
			String strCity=YFSSystem.getProperty("PERSON_INFO_CITY");
			String strState=YFSSystem.getProperty("PERSON_INFO_STATE");
			String strCountry=YFSSystem.getProperty("PERSON_INFO_COUNTRY");
			String strZipCode=YFSSystem.getProperty("PERSON_INFO_ZIPCODE");

			Element orderElement = inXML.getDocumentElement();
			Element eleOrderPersonInfoShipTo = SCXmlUtil.createChild(orderElement,  VSIConstants.ELE_PERSON_INFO_SHIP_TO);
			eleOrderPersonInfoShipTo.setAttribute(VSIConstants.ATTR_FIRST_NAME, strFirstName);
			eleOrderPersonInfoShipTo.setAttribute(VSIConstants.ATTR_ADDR_LINE_1, strAddressLine1);
			eleOrderPersonInfoShipTo.setAttribute(VSIConstants.ATTR_ADDR_LINE_2, strAddressLine2);
			eleOrderPersonInfoShipTo.setAttribute(VSIConstants.ATTR_CITY, strCity);
			eleOrderPersonInfoShipTo.setAttribute(VSIConstants.ATTR_STATE, strState);
			eleOrderPersonInfoShipTo.setAttribute(VSIConstants.ATTR_COUNTRY, strCountry);
			eleOrderPersonInfoShipTo.setAttribute(VSIConstants.ATTR_ZIPCODE, strZipCode);
			

			Element eleOrderPersonInfoBillTo= SCXmlUtil.createChild(orderElement,  VSIConstants.ELE_PERSON_INFO_BILL_TO);
			eleOrderPersonInfoBillTo.setAttribute(VSIConstants.ATTR_FIRST_NAME, strFirstName);
			eleOrderPersonInfoBillTo.setAttribute(VSIConstants.ATTR_ADDR_LINE_1, strAddressLine1);
			eleOrderPersonInfoBillTo.setAttribute(VSIConstants.ATTR_ADDR_LINE_2, strAddressLine2);
			eleOrderPersonInfoBillTo.setAttribute(VSIConstants.ATTR_CITY, strCity);
			eleOrderPersonInfoBillTo.setAttribute(VSIConstants.ATTR_STATE, strState);
			eleOrderPersonInfoBillTo.setAttribute(VSIConstants.ATTR_COUNTRY, strCountry);
			eleOrderPersonInfoBillTo.setAttribute(VSIConstants.ATTR_ZIPCODE, strZipCode);
			
			Element eleOrderLines = SCXmlUtil.getChildElement(orderElement, VSIConstants.ELE_ORDER_LINES);
			ArrayList<Element> alOrderLines = SCXmlUtil.getChildren(eleOrderLines, VSIConstants.ELE_ORDER_LINE);
			
						

			for (Element eleOrderLine : alOrderLines) {
				Element elePersonInfoShipTo = SCXmlUtil.getChildElement(eleOrderLine, VSIConstants.ELE_PERSON_INFO_SHIP_TO);	

				if(!YFCObject.isVoid(elePersonInfoShipTo)){

					Element elePersonInfoExtn = SCXmlUtil.getChildElement(elePersonInfoShipTo, VSIConstants.ELE_EXTN);
					if(!YFCObject.isVoid(elePersonInfoExtn)){		

							
							elePersonInfoShipTo.setAttribute(VSIConstants.ATTR_FIRST_NAME, strFirstName);
							elePersonInfoShipTo.setAttribute(VSIConstants.ATTR_ADDR_LINE_1, strAddressLine1);
							elePersonInfoShipTo.setAttribute(VSIConstants.ATTR_ADDR_LINE_2, strAddressLine2);
							elePersonInfoShipTo.setAttribute(VSIConstants.ATTR_CITY, strCity);
							elePersonInfoShipTo.setAttribute(VSIConstants.ATTR_STATE, strState);
							elePersonInfoShipTo.setAttribute(VSIConstants.ATTR_COUNTRY, strCountry);
							elePersonInfoShipTo.setAttribute(VSIConstants.ATTR_ZIPCODE, strZipCode);

							//Element eleItem = SCXmlUtil.getChildElement(eleOrderLine, VSIConstants.ELE_ITEM, true);				

					}
				}
				
			}
		}
			catch(Exception ex) {
				ex.printStackTrace();
			}

				return inXML;
			}
		}

