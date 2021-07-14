package com.vsi.som.shipment;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.vsi.oms.shipment.api.VSIProcessShipConfirmFromWMS;
import com.vsi.oms.utils.VSIConstants;
import com.vsi.oms.utils.VSIUtils;
import com.vsi.oms.utils.XMLUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfs.japi.YFSEnvironment;

public class VSITransferInventoryForShipment implements VSIConstants
{
	private YFCLogCategory log = YFCLogCategory.instance(VSITransferInventoryForShipment.class);
	public void transferInventoryOwnership(YFSEnvironment env, Document docShipment)
	{
		//log.info("Input for VSITransferInventoryForShipment.transferInventoryOwnership => "+XMLUtil.getXMLString(docShipment));
		if(log.isDebugEnabled())
			log.debug("Input for VSITransferInventoryForShipment.transferInventoryOwnership => "+XMLUtil.getXMLString(docShipment));
		try
		{
			Element eleShipment = docShipment.getDocumentElement();
			String strEnterprise = eleShipment.getAttribute(ATTR_ENTERPRISE_CODE);
			String shipNode = eleShipment.getAttribute(ATTR_SHIP_NODE);
			String shipmentKey = eleShipment.getAttribute(ATTR_SHIPMENT_KEY);
			if(log.isDebugEnabled())
			log.debug("strEnterprise => "+strEnterprise+"shipNode => "+shipNode+"shipmentKey => "+shipmentKey);
			Document getContainerListInXml=XMLUtil.createDocument(ELE_CONTAINER);
			Element containerEle = getContainerListInXml.getDocumentElement();
			containerEle.setAttribute(ATTR_SHIPMENT_KEY,shipmentKey);
			Document getContainerListOutXML = VSIUtils.invokeAPI(env, TEMPLATE_GET_SHIPMENT_CONTAINER_LIST, API_GET_SHIPMENT_CONTAINER_LIST, getContainerListInXml);
			if(log.isDebugEnabled())
			log.debug("getContainerListOutXML => "+XMLUtil.getXMLString(getContainerListOutXML));
			NodeList containerNode = getContainerListOutXML.getElementsByTagName(ELE_CONTAINER);
			for (int i = 0; i < containerNode.getLength(); i++)
			{
				Element containerElem = (Element) containerNode.item(i);
			NodeList containerDtlNode = containerElem.getElementsByTagName(ELE_CONTAINER_DETAIL);
			for (int j = 0; j < containerDtlNode.getLength(); j++) 
			{
				Element containerDtlElem = (Element) containerDtlNode.item(j);
				String strItemId = containerDtlElem.getAttribute(ATTR_ITEM_ID);
				Double dblRequiredQty = SCXmlUtil.getDoubleAttribute(containerDtlElem, ATTR_QUANTITY);
				if(log.isDebugEnabled())
				log.debug("strItemId => "+strItemId+"dblRequiredQty" + dblRequiredQty);
				String strFromOrganizationCode = VSI_INV;
				
			Document docGetInventorySupply = SCXmlUtil.createDocument(ELE_INVENTORY_SUPPLY);
			Element eleGetInventorySupply = docGetInventorySupply.getDocumentElement();
			eleGetInventorySupply.setAttribute(ATTR_PRODUCT_CLASS, GOOD);
			eleGetInventorySupply.setAttribute(ATTR_UOM, UOM_EACH);
			eleGetInventorySupply.setAttribute(ATTR_SUPPLY_TYPE, ONHAND_SUPPLY_TYPE);
			eleGetInventorySupply.setAttribute(ATTR_SHIP_NODE, shipNode);
			eleGetInventorySupply.setAttribute(ATTR_ITEM_ID, strItemId);
			Document docTransferInventory = SCXmlUtil.createDocument(ELE_ITEMS);
			Element eleTransferInventory = docTransferInventory.getDocumentElement();

			VSIProcessShipConfirmFromWMS shipObj = new VSIProcessShipConfirmFromWMS();
			dblRequiredQty = shipObj.invokeGetInventorySupplyNewLogic(env, docGetInventorySupply, dblRequiredQty, eleTransferInventory,
					strFromOrganizationCode, strEnterprise);
			if (dblRequiredQty > 0)
			{
				if (ENT_ADP.equalsIgnoreCase(strEnterprise))
					strFromOrganizationCode = ADP_INV;
				else if (ENT_MCL.equalsIgnoreCase(strEnterprise))
					strFromOrganizationCode = MCL_INV;
				else
					strFromOrganizationCode = DTC_INV;
				dblRequiredQty = shipObj.invokeGetInventorySupplyNewLogic(env, docGetInventorySupply, dblRequiredQty,
						eleTransferInventory, strFromOrganizationCode, strEnterprise);
			}
			if (dblRequiredQty > 0) 
			{
				if (ENT_ADP.equalsIgnoreCase(strEnterprise)) {
					strFromOrganizationCode = DTC_INV;
					dblRequiredQty = shipObj.invokeGetInventorySupplyNewLogic(env, docGetInventorySupply, dblRequiredQty,
							eleTransferInventory, strFromOrganizationCode, strEnterprise);
				} 
				//Start - changes for wholesale project
				Document docGetCommonCodeInput = SCXmlUtil.createDocument(VSIConstants.ELEMENT_COMMON_CODE);
				Element eleCommonCodeElement = docGetCommonCodeInput.getDocumentElement();
				eleCommonCodeElement.setAttribute(VSIConstants.ATTR_CODE_TYPE, VSIConstants.ATTR_ALL_WH_ORG);
				eleCommonCodeElement.setAttribute(VSIConstants.ATTR_ORG_CODE, VSIConstants.ATTR_DEFAULT);
				Document docgetCommonCodeOutput = VSIUtils.invokeAPI(env,VSIConstants.API_COMMON_CODE_LIST, docGetCommonCodeInput);
				
				if(docgetCommonCodeOutput.getDocumentElement().hasChildNodes()){
					NodeList nCommonCodeList = docgetCommonCodeOutput.getElementsByTagName(VSIConstants.ELE_COMMON_CODE);
					for (int k = 0; k < nCommonCodeList.getLength(); k++) {
						Element eleCommonCode = (Element) nCommonCodeList.item(k);
						String strWHEnterpriseCode = eleCommonCode.getAttribute(VSIConstants.ATTR_CODE_LONG_DESC);
				
				 if (strWHEnterpriseCode.equalsIgnoreCase(strEnterprise)) {
					strFromOrganizationCode = MCL_INV;
					dblRequiredQty = shipObj.invokeGetInventorySupplyNewLogic(env, docGetInventorySupply, dblRequiredQty,
							eleTransferInventory, strFromOrganizationCode, strEnterprise);
					
				}
					}				
				
				//End - changes for wholesale project
				}
			}
			if (dblRequiredQty > 0) 
			{
				if (ENT_ADP.equalsIgnoreCase(strEnterprise)) {
					strFromOrganizationCode = MCL_INV;
					dblRequiredQty = shipObj.invokeGetInventorySupplyNewLogic(env, docGetInventorySupply, dblRequiredQty,
							eleTransferInventory, strFromOrganizationCode, strEnterprise);
				}
				else
				{
					eleTransferInventory = addItemToTransferInv(eleTransferInventory,shipNode,strEnterprise,strItemId,dblRequiredQty);
					dblRequiredQty = 0.0;
				}
			}
			if (dblRequiredQty > 0)
			{
				eleTransferInventory = addItemToTransferInv(eleTransferInventory,shipNode,strEnterprise,strItemId,dblRequiredQty);
				dblRequiredQty = 0.0;
			}
		// invoke transferInventoryOwnership API
			if(log.isDebugEnabled())
			log.debug("docTransferInventory Before API call => "+XMLUtil.getXMLString(docTransferInventory));
		VSIUtils.invokeAPI(env, API_TRANSFER_INV_OWNERSHIP, docTransferInventory);
			}
			}
		if(log.isDebugEnabled())
			log.debug("docShipment Final return => "+XMLUtil.getXMLString(docShipment));
		VSIUtils.invokeAPI(env, API_CONFIRM_SHIPMENT, docShipment);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	public Element addItemToTransferInv(Element eleTransferInventory, String shipNode, String strEnterprise,
			String strItemId, Double dblRequiredQty) 
	{
		Element eleTransferItems = SCXmlUtil.createChild(eleTransferInventory, ELE_ITEM);
		eleTransferItems.setAttribute(ATTR_PRODUCT_CLASS, GOOD);
		eleTransferItems.setAttribute(ATTR_UOM, UOM_EACH);
		eleTransferItems.setAttribute(ATTR_SUPPLY_TYPE, ONHAND_SUPPLY_TYPE);
		eleTransferItems.setAttribute(ATTR_SHIP_NODE, shipNode);
		eleTransferItems.setAttribute(ATTR_FROM_ORG_CODE, VSI_INV);
		eleTransferItems.setAttribute(ATTR_TO_ORG_CODE, strEnterprise);
		eleTransferItems.setAttribute(ATTR_ITEM_ID, strItemId);
		eleTransferItems.setAttribute(ATTR_QUANTITY, Double.toString(dblRequiredQty));
		return eleTransferInventory;
	}
}
