package com.vsi.oms.shipment.api;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.vsi.oms.utils.VSIConstants;
import com.vsi.oms.utils.VSIUtils;
import com.vsi.oms.utils.XMLUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;

public class VSIUpdateOrderInInvoice {

	private YFCLogCategory log = YFCLogCategory.instance(VSIUpdateOrderInInvoice.class);
	
	public Document updateOrderInfrm(YFSEnvironment env, Document inXML) throws Exception {

		if(log.isDebugEnabled()){
			log.debug("Input XML :" + SCXmlUtil.getString(inXML));
			log.info("================Inside updateOrderInfrm================================");
		}
		
		Element eleOrderInvoice = inXML.getDocumentElement();

		
		String strOrderInvoiceKey = eleOrderInvoice.getAttribute(VSIConstants.ATTR_ORDER_INVOICE_KEY);
		
		if (!YFCCommon.isVoid(strOrderInvoiceKey)) {
			
			Document docOrderInvoiceIn = SCXmlUtil.createDocument(VSIConstants.ELE_ORDER_INVOICE);
			Element orderInvoiceEle = docOrderInvoiceIn.getDocumentElement();
			orderInvoiceEle.setAttribute(VSIConstants.ATTR_ORDER_INVOICE_KEY, strOrderInvoiceKey);
			
			Element extnEle = docOrderInvoiceIn.createElement(VSIConstants.ELE_EXTN);
			orderInvoiceEle.appendChild(extnEle);
			SCXmlUtil.setAttribute(extnEle, VSIConstants.ATTR_EXTN_IS_LAST_INVOICE_FOR_ORDER, VSIConstants.FLAG_Y);
			
			Document docChangeOrderInvoice = VSIUtils.invokeService(env, VSIConstants.SERVICE_CHANGE_ORDER_INVOICE,
					docOrderInvoiceIn);

			if (log.isDebugEnabled()) {
				log.debug("Output to chnageOrderInvoice in class VSIUpdateOrderInInvoice"
						+ XMLUtil.getXMLString(docChangeOrderInvoice));
			}
		}
		
		return inXML;
	}
}
