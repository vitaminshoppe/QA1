package com.vsi.oms.api;

import java.rmi.RemoteException;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.vsi.oms.utils.VSIConstants;
import com.vsi.oms.utils.VSIUtils;
import com.vsi.oms.utils.XMLUtil;
import com.yantra.interop.japi.YIFClientCreationException;
import com.yantra.yfc.core.YFCObject;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;

public class VSIUpdateAvailabilityForNonSFSItems implements VSIConstants{
	private YFCLogCategory log = YFCLogCategory.instance(VSIUpdateAvailabilityForNonSFSItems.class);
	public Document updateOnhandAvailability(YFSEnvironment env,Document inXml) {
		
		   if(log.isDebugEnabled())
			   log.debug("Input for VSIUpdateAvailabilityForNonSFSItems.updateOnhandAvailability => "+XMLUtil.getXMLString(inXml));
		   try
		   {					
			Element availabilityChanges =  (Element) inXml.getDocumentElement().getElementsByTagName(ELE_AVAILABILITY_CHANGES).item(0);
			NodeList availabilityChange = availabilityChanges.getElementsByTagName(ELE_AVAILABILITY_CHANGE);

			 Element eleItem =  (Element) inXml.getDocumentElement().getElementsByTagName("Item").item(0);
			 Element eleExtn = SCXmlUtil.getChildElement(eleItem, ELE_EXTN);
			 String isSfsEligible = eleExtn.getAttribute(ATTR_EXTN_SFS_ELIGIBLE);
			 String strItemID = inXml.getDocumentElement().getAttribute("ItemID");
			 if(isSfsEligible.equalsIgnoreCase("N")) {
				 for (int i = 0; i < availabilityChange.getLength(); i++) 
					{
						Element availabilityChangeEle = (Element) availabilityChange.item(i);
						String availOnHandQty=availabilityChangeEle.getAttribute(ATTR_ONHAND_AVAILABLE_QUANTITY);
						Double dblavailOnHandQty = 
								SCXmlUtil.getDoubleAttribute(availabilityChangeEle, ATTR_ONHAND_AVAILABLE_QUANTITY);
						if(dblavailOnHandQty != 0) {
							Document getAvlInvOutputXML = getAvailableInventoryAPI(env,inXml);
							Element eleAvailability = XMLUtil.getElementByXPath(getAvlInvOutputXML, 
									"//PromiseLine[@ItemID='" + strItemID + "']/Availability");
							
							if(!YFCObject.isVoid(eleAvailability) && eleAvailability.hasChildNodes()){
								
								Element eleAvailableInv =SCXmlUtil.getChildElement(eleAvailability, "AvailableInventory");
								String availableOnhandQuantity= eleAvailableInv.getAttribute(ATTR_AVAILABLE_ONHAND_QTY);
								log.debug("setting DC onhand availableqty :"+availableOnhandQuantity);
								availabilityChangeEle.setAttribute(ATTR_ONHAND_AVAILABLE_QUANTITY,availableOnhandQuantity);
							
							}
						}
					}
			 }	
		   }
		   catch(Exception e)
			{
				e.printStackTrace();
			}
		return inXml;
	}
	private Document getAvailableInventoryAPI(YFSEnvironment env, Document inXml) throws ParserConfigurationException, YFSException, RemoteException, YIFClientCreationException {
	  	
		String strItemID = inXml.getDocumentElement().getAttribute("ItemID");
	  	String strOrgcode = inXml.getDocumentElement().getAttribute("InventoryOrganizationCode");
		
	  	Document createGetAvailInv = XMLUtil.createDocument("Promise");
		Element createGetAvailInvElement = createGetAvailInv.getDocumentElement();
		
		createGetAvailInvElement.setAttribute(VSIConstants.ATTR_ORG_CODE, strOrgcode);
		createGetAvailInvElement.setAttribute("CheckInventory", "Y");
		createGetAvailInvElement.setAttribute("DistributionRuleId","VSI_Region4_DC");
		
		Element promiseLinesEle = XMLUtil.appendChild(createGetAvailInv, createGetAvailInvElement, "PromiseLines", "");
		Element promiseLineEle = XMLUtil.appendChild(createGetAvailInv, promiseLinesEle, "PromiseLine", "");

		promiseLineEle.setAttribute("ItemID", strItemID);
		promiseLineEle.setAttribute("UnitOfMeasure", "EACH");
		promiseLineEle.setAttribute("ProductClass", "GOOD");
		promiseLineEle.setAttribute("LineId", "1");
		
		Document getAvlInvOutputXML = VSIUtils.invokeAPI(env,
				"global/template/api/getAvailableInventory.xml",
				"getAvailableInventory", createGetAvailInv);
		
		return getAvlInvOutputXML;
	}
}
