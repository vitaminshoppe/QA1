package com.vsi.oms.api;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.vsi.oms.utils.VSIConstants;
import com.vsi.oms.utils.VSIUtils;
import com.vsi.oms.utils.XMLUtil;
import com.yantra.yfc.core.YFCObject;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfs.japi.YFSEnvironment;

public class VSIATGRTAMUpdatesForCapacity implements VSIConstants
    {
		  private YFCLogCategory log = YFCLogCategory.instance(VSIATGRTAMUpdatesForCapacity.class);
		  public Document onhandAvailableDateChange(YFSEnvironment env,Document inXml)
		     {
			   if(log.isDebugEnabled())
				   log.debug("Input for VSIATGRTAMUpdatesForCapacity.onhandAvailableDateChange => "+XMLUtil.getXMLString(inXml));
			   try
			   {
				 Element availabilityChanges =  (Element) inXml.getDocumentElement().getElementsByTagName(ELE_AVAILABILITY_CHANGES).item(0);
				 NodeList availabilityChange = availabilityChanges.getElementsByTagName(ELE_AVAILABILITY_CHANGE);
				 for (int i = 0; i < availabilityChange.getLength(); i++) 
					{
						Element availabilityChangeEle = (Element) availabilityChange.item(i);
						String onhandAvailableDate = availabilityChangeEle.getAttribute(ATTR_ONHAND_AVAILABLE_DATE);
						if(dateGreaterThanCurrentDate(onhandAvailableDate))
							availabilityChangeEle.setAttribute(ATTR_ONHAND_AVAILABLE_QUANTITY,"0");
					}
				 Element eleItem =  (Element) inXml.getDocumentElement().getElementsByTagName("Item").item(0);
				 Element eleExtn = SCXmlUtil.getChildElement(eleItem, ELE_EXTN);
				 String isSfsEligible = eleExtn.getAttribute(ATTR_EXTN_SFS_ELIGIBLE);
				 if(isSfsEligible.equalsIgnoreCase("N")) {
					// getAvailabilityForNonSFSItem(env,inXml);
				 }	
			   }
			   catch(Exception e)
				{
					e.printStackTrace();
				}
	return inXml;
		     }
		  private boolean dateGreaterThanCurrentDate(String onhandAvailableDateValue)
		  {
				boolean bResult = false;
				onhandAvailableDateValue = onhandAvailableDateValue.substring(0, 10);
				DateFormat sdf = new SimpleDateFormat(VSIConstants.YYYY_MM_DD);				
				Date date = new Date();
				String strCurrentDate = sdf.format(date);				
				Date dPassedDate;
				try 
				{
					dPassedDate = sdf.parse(onhandAvailableDateValue);				
					Date dCurrentDate = sdf.parse(strCurrentDate);
					if(dPassedDate.after(dCurrentDate))
						bResult = true; 
				} catch (ParseException e) {
					e.printStackTrace();
				}
				return bResult;
			}
			
		  private void getAvailabilityForNonSFSItem(YFSEnvironment env,Document input) throws Exception {
			  	String strItemID = input.getDocumentElement().getAttribute("ItemID");
			  	String strOrgcode = input.getDocumentElement().getAttribute("InventoryOrganizationCode");
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
				
				Element eleAvailability = XMLUtil.getElementByXPath(getAvlInvOutputXML, 
						"//PromiseLine[@ItemID='" + strItemID + "']/Availability");
				
				if(!YFCObject.isVoid(eleAvailability) && eleAvailability.hasChildNodes()){
					
					Element eleAvailableInv =SCXmlUtil.getChildElement(eleAvailability, "AvailableInventory");
					Element availabilityChanges =  (Element) input.getDocumentElement().getElementsByTagName(ELE_AVAILABILITY_CHANGES).item(0);
					NodeList availabilityChange = availabilityChanges.getElementsByTagName(ELE_AVAILABILITY_CHANGE);
					 for (int i = 0; i < availabilityChange.getLength(); i++) 
						{
							Element availabilityChangeEle = (Element) availabilityChange.item(i);
							String availOnHandQty=availabilityChangeEle.getAttribute("AvailableOnhandQuantity");
							if(availOnHandQty != "0") {
								String availableOnhandQuantity= eleAvailableInv.getAttribute(ATTR_AVAILABLE_ONHAND_QTY);
								availabilityChangeEle.setAttribute(ATTR_ONHAND_AVAILABLE_QUANTITY,availableOnhandQuantity);
							}
						}
					
				}
		  }			
        }
