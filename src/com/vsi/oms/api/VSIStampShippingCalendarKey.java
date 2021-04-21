package com.vsi.oms.api;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.sterlingcommerce.tools.datavalidator.XmlUtils;
import com.vsi.oms.utils.VSIConstants;
import com.vsi.oms.utils.VSIUtils;
import com.vsi.oms.utils.XMLUtil;
import com.yantra.interop.japi.YIFApi;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfs.japi.YFSEnvironment;

public class VSIStampShippingCalendarKey {
	private YFCLogCategory log = YFCLogCategory.instance(VSIStampShippingCalendarKey.class);
	YIFApi api;

	/**
	 * @param env
	 * @param inXML
	 * 
	 * This class is invoked on Create and Confirm Shipment OnSuccess
	 * Event.
	 * @throws FactoryConfigurationError 
	 * @throws ParserConfigurationException 
	 */
	public Document stampShippingCalendarKey(YFSEnvironment env, Document inXML) throws ParserConfigurationException, FactoryConfigurationError {
		
		log.info("Input to stampShippingCalendarKey"+ XMLUtil.getXMLString(inXML));
		Element inputEle = inXML.getDocumentElement();
		Document docCommonCode = XmlUtils.createDocument(VSIConstants.ELEMENT_COMMON_CODE);
		Element eCommonCode = docCommonCode.getDocumentElement();
		eCommonCode.setAttribute(VSIConstants.ATTR_CODE_TYPE, "VSI_DUMMY_CALENDAR");
		Document getCommonCodeOutput = VSIUtils.getCommonCodeList(env, docCommonCode);
		Element commonCodeEle = getCommonCodeOutput.getDocumentElement();
		String organizationCode = inputEle.getAttribute(VSIConstants.ATTR_ORGANIZATION_CODE);
		Element nodeEle = SCXmlUtil.getChildElement(inputEle, VSIConstants.ELE_NODE);
		NodeList nlsCommonCode = commonCodeEle
				.getElementsByTagName(VSIConstants.ELEMENT_COMMON_CODE);
		for (int i = 0; i < nlsCommonCode.getLength(); i++) {
			Element eleCommonCode = (Element) nlsCommonCode.item(i);
			String strCodeShortDesc = eleCommonCode.getAttribute(VSIConstants.ATTR_CODE_SHORT_DESCRIPTION);
			
			if(organizationCode.equals(strCodeShortDesc)) {
				String strCodeLongDesc = eleCommonCode.getAttribute(VSIConstants.ATTR_CODE_LONG_DESCRIPTION);
				nodeEle.setAttribute("ShippingCalendarKey", strCodeLongDesc);
				break;
			}
			
		}
		log.info("Output from stampShippingCalendarKey"+ XMLUtil.getXMLString(inXML));
		return inXML;
		}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	}