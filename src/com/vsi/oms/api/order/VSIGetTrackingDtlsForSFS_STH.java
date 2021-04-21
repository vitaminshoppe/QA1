package com.vsi.oms.api.order;

import java.rmi.RemoteException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.sterlingcommerce.tools.datavalidator.XmlUtils;
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

public class VSIGetTrackingDtlsForSFS_STH implements VSIConstants {

	private static YFCLogCategory log = YFCLogCategory
			.instance(GetotherDetails.class);
	YIFApi api;

	/**
	 * @param env
	 * @param inXML
	 * @return
	 * @throws Exception
	 */
	public Document shipmentDetails(YFSEnvironment env, Document inXML)
			throws Exception {
		if(log.isDebugEnabled()){
			log.info("================Inside ================================");
			log.debug("Printing Input XML :" + XmlUtils.getString(inXML));
		}
		/**** Stamping TrackingNo ,CarrierServiceCode and TrackingURL ****/
		
		log.info("GetotherDetails.shipmentDetails() i/p xml  => "+SCXmlUtil.getString(inXML));
		Element orderEle = (Element) inXML.getElementsByTagName(ELE_ORDER).item(0);
		Element shipNodeElem = (Element) inXML.getElementsByTagName(ATTR_SHIP_NODE).item(0);
		String shipNodeKey = "";
		if(!YFCCommon.isVoid(shipNodeElem))
			shipNodeKey = shipNodeElem.getAttribute(ATTR_SHIPNODE_KEY);
		String documentType = orderEle.getAttribute(ATTR_DOCUMENT_TYPE);
		log.info("shipNodeKey => "+shipNodeKey+"documentType => "+documentType);
		if((!shipNodeKey.equalsIgnoreCase("")) && (!shipNodeKey.equalsIgnoreCase(SHIP_NODE_9004_VALUE) && !shipNodeKey.equalsIgnoreCase(SHIP_NODE_9005_VALUE)) && (documentType.equalsIgnoreCase(DOCUMENT_TYPE)))
		{
			Document getContainerInXml=XMLUtil.createDocument(ELE_CONTAINER);
			Element containerEle = getContainerInXml.getDocumentElement();
			String shipmentKey = (String) env.getTxnObject(TRANS_OBJ_SHIPMENTKEY);
			log.info("shipmentKey => "+shipmentKey);
			containerEle.setAttribute(ATTR_SHIPMENT_KEY,shipmentKey);
			Document getContainerDtlsOutXML = VSIUtils.invokeAPI(env, TEMPLATE_GET_SHIPMENT_CONTAINER_LIST, API_GET_SHIPMENT_CONTAINER_LIST, getContainerInXml);
			NodeList containerNode = getContainerDtlsOutXML.getElementsByTagName(ELE_CONTAINER);
			Element shipmentsEle = inXML.createElement(ELE_SHIPMENTS);
			for (int i = 0; i < containerNode.getLength(); i++) 
			{
				Element containerElem = (Element) containerNode.item(i);
				String trackingNo = containerElem.getAttribute(ATTR_TRACKING_NO);
				log.info("shipmentKey => "+shipmentKey);
				Element shipmentEle = SCXmlUtil.createChild(shipmentsEle, ELE_SHIPMENT);
				String carrierServiceCode = (String) env.getTxnObject(TRANS_OBJ_SCAC);
				log.info("carrierServiceCode => "+carrierServiceCode);
				shipmentEle.setAttribute(ATTR_CARRIER_SERVICE_CODE, carrierServiceCode);
				shipmentEle.setAttribute(ATTR_TRACKING_NO, trackingNo);
				String scacCommonCodeValue = getScacCommonCodeValue(env);
				log.info("scacCommonCodeValue => "+scacCommonCodeValue);
				shipmentEle.setAttribute(ATTR_TRACKING_URL, scacCommonCodeValue+trackingNo);
				shipmentsEle.appendChild(shipmentEle);				
			}
			orderEle.appendChild(shipmentsEle);			
		}
		else
		{		
		Element shipmentsEle = inXML.createElement(ELE_SHIPMENTS);
		Element ShipsEle = SCXmlUtil.createChild(shipmentsEle, ELE_SHIPMENT);
		String nTrackingValue = (String) env.getTxnObject("eTrackingValue");
		String nSCACandServiceDesc = (String) env.getTxnObject(TRANS_OBJ_SCAC);		
		if (!YFCObject.isVoid(nTrackingValue))
			ShipsEle.setAttribute("TrackingNo", nTrackingValue);
		String strCodeLongDesc = getScacCommonCodeValue(env);
		if (!YFCObject.isVoid(strCodeLongDesc)) 
		{
			String nTrackingURL = strCodeLongDesc + nTrackingValue; // Creating Tracking URL
			ShipsEle.setAttribute("TrackingURL", nTrackingURL);
		}		
		/*
		 * Tracking URL will not get created if carrier service code is null or
		 * empty
		 */
		if (!YFCObject.isVoid(nSCACandServiceDesc))
			ShipsEle.setAttribute("CarrierServiceCode", nSCACandServiceDesc);
		
		shipmentsEle.appendChild(ShipsEle);
		orderEle.appendChild(shipmentsEle);
		}		
		
		if (log.isVerboseEnabled())
			log.debug("inXML", XmlUtils.getString(inXML));
		
		log.info("GetotherDetails.shipmentDetails() final xml  => "+SCXmlUtil.getString(inXML));
		return inXML;
	}

	private String getScacCommonCodeValue(YFSEnvironment env) throws YFSException, RemoteException, YIFClientCreationException
	{
		String nCarrierServiceCode = (String) env.getTxnObject("eCarrierServiceCode");
		String nSCAC = (String) env.getTxnObject("eSCAC");		
		//Start preparing document for getCommonCodeList API
		Document docCommonCodeInput = SCXmlUtil.createDocument(ELE_COMMON_CODE);
		Element eleCommonCodeInput = docCommonCodeInput.getDocumentElement();
		
		//Check for Surepost and MI first and then go on  to carriers
		if(!(YFCCommon.isVoid(nCarrierServiceCode)) && nCarrierServiceCode.equals(CARRIER_SERVICE_CODE_SUREPOST)){
			eleCommonCodeInput.setAttribute(ATTR_CODE_TYPE, TRACKING_COMMON_CODE_SURE);
		}else if( !(YFCCommon.isVoid(nCarrierServiceCode)) && nCarrierServiceCode.equalsIgnoreCase(CARRIER_SERVICE_CODE_MI)){
			eleCommonCodeInput.setAttribute(ATTR_CODE_TYPE, TRACKING_COMMON_CODE_MI);
		}else if(!(YFCCommon.isVoid(nSCAC)) && nSCAC.equalsIgnoreCase(SCAC_UPSN)){
			eleCommonCodeInput.setAttribute(ATTR_CODE_TYPE, TRACKING_COMMON_CODE_UPS);
		}else if( !(YFCCommon.isVoid(nSCAC)) && nSCAC.equalsIgnoreCase(SCAC_LSHIP)){
			eleCommonCodeInput.setAttribute(ATTR_CODE_TYPE, TRACKING_COMMON_CODE_LSHIP);
		}else if(!(YFCCommon.isVoid(nSCAC)) && nSCAC.equalsIgnoreCase(SCAC_ONTRAC)){
			eleCommonCodeInput.setAttribute(ATTR_CODE_TYPE, TRACKING_COMMON_CODE_ONTRAC);
		}else if(!(YFCCommon.isVoid(nSCAC)) && nSCAC.equalsIgnoreCase(SCAC_USPS)){
			eleCommonCodeInput.setAttribute(ATTR_CODE_TYPE, TRACKING_COMMON_CODE_USPS);
		}
		//OMS-2719: Start
		else if(!(YFCCommon.isVoid(nSCAC)) && nSCAC.equalsIgnoreCase(SCAC_FEDEX)){
			eleCommonCodeInput.setAttribute(ATTR_CODE_TYPE, TRACKING_COMMON_CODE_FEDEX);
		}
		//OMS-2719: END
		//OMS-3149 Changes -- Start
		else if(!(YFCCommon.isVoid(nSCAC)) && nSCAC.equalsIgnoreCase(SCAC_DHL)){
			eleCommonCodeInput.setAttribute(ATTR_CODE_TYPE, DHL_TRACKING_COMMON_CODE);
		}
		//OMS-3149 Changes -- End
		//Invoke common code list based on the carrier or carrier service
		String strCodeLongDesc ="";
		if(!YFCCommon.isVoid(eleCommonCodeInput.getAttribute(ATTR_CODE_TYPE)))
		{
			Document docCommonCodeListOutput = VSIUtils.invokeAPI(env, API_COMMON_CODE_LIST, docCommonCodeInput);
			Element eleCommonCodeListOutput = docCommonCodeListOutput.getDocumentElement();
			Element eleCommonCodeOutput = SCXmlUtil.getChildElement(eleCommonCodeListOutput, ELE_COMMON_CODE);
			strCodeLongDesc = eleCommonCodeOutput.getAttribute(ATTR_CODE_LONG_DESC);
		}
		return strCodeLongDesc;
	}

	/**
	 * @param env
	 * @param strCodeType
	 * @param strCodeValue
	 * @param strOrganizationCode
	 * @param isShortDescRequired
	 * @return
	 * @throws Exception
	 */
	public static String getCommonCodeValue(YFSEnvironment env,
			String strCodeType, String strCodeValue,
			String strOrganizationCode, boolean isShortDescRequired)
			throws Exception {

		Element eleCommonCodeOutputXml = getCommonCodeElement(env, strCodeType,
				strCodeValue, strOrganizationCode, "");

		String strDescription = "";
		if (!XmlUtils.isVoid(eleCommonCodeOutputXml)) {
			if (log.isVerboseEnabled()) {
				log.debug("eleCommonCodeOutputXml",
						XmlUtils.getString(eleCommonCodeOutputXml));
			}

			Element eleCommonCode = XmlUtils.getChildElement(
					eleCommonCodeOutputXml, "CommonCode");
			if (log.isVerboseEnabled()) {
				log.debug("eleCommonCode", XmlUtils.getString(eleCommonCode));

			}
			if (!XmlUtils.isVoid(eleCommonCode)) {
				{
					strDescription = eleCommonCode
							.getAttribute("CodeLongDescription");
				}
			}
		}

		return strDescription;
	}

	/**
	 * @param env
	 * @param strCodeType
	 * @param strCodeValue
	 * @param strOrganizationCode
	 * @param shortDescription
	 * @return
	 * @throws Exception
	 */
	private static Element getCommonCodeElement(YFSEnvironment env,
			String strCodeType, String strCodeValue,
			String strOrganizationCode, String shortDescription)
			throws Exception {

		Document docCommonCode = XmlUtils.createDocument("CommonCode");
		Element eCommonCode = docCommonCode.getDocumentElement();
		eCommonCode.setAttribute("CodeType", strCodeType);
		if (!XmlUtils.isVoid(strCodeValue)) {
			eCommonCode.setAttribute("CodeValue", strCodeValue);
		}
		if (!XmlUtils.isVoid(strCodeValue)) {
			eCommonCode.setAttribute("OrganizationCode", strOrganizationCode);
		}
		if (!XmlUtils.isVoid(shortDescription)) {
			eCommonCode.setAttribute("CodeShortDescription", shortDescription);
		}
		
		try {
		
			YIFApi api2 = YIFClientFactory.getInstance().getApi();
			if (log.isVerboseEnabled()) {
				log.verbose("docCommonCode : \n"
						+ XMLUtil.getXMLString(docCommonCode));
			}
			Document commonCodeOutDoc = api2.invoke(env, "getCommonCodeList",
					docCommonCode);
			return commonCodeOutDoc.getDocumentElement();
		} 
		catch (Exception e) {
			return null;
		}

	}
}

