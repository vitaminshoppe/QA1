package com.vsi.oms.api;

import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.sterlingcommerce.baseutil.SCUtil;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.vsi.oms.utils.VSIConstants;
import com.vsi.oms.utils.VSIUtils;
import com.vsi.oms.utils.XMLUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.interop.japi.YIFClientCreationException;
import com.yantra.yfs.japi.YFSException;
/*
 * this class is used for creating release ack delay alert
 */
public class VSIReleaseAckDelayAlert {
	private YFCLogCategory log = YFCLogCategory
			.instance(VSIReleaseAckDelayAlert.class);
	public void vsiReleaseAckDelayAlert(YFSEnvironment env, Document XML) throws RemoteException, YIFClientCreationException, TransformerException{
		if(log.isDebugEnabled()){
			log.debug("Printing inputdoc \n"+XMLUtil.getXMLString(XML));
		}
		if (!SCUtil.isVoid(XML)) {
			
			try {
				Element eleOrder = (Element) XML.getDocumentElement().getElementsByTagName("Order").item(0);
				String strOrderNo = eleOrder.getAttribute(VSIConstants.ATTR_ORDER_NO);
				String strOrderHeaderKey = eleOrder.getAttribute(VSIConstants.ATTR_ORDER_HEADER_KEY);
				String strEnterpriseCode = eleOrder.getAttribute(VSIConstants.ATTR_ENTERPRISE_CODE);
				String strBillToID = eleOrder.getAttribute(VSIConstants.ATTR_BILL_TO_ID);

				NodeList nOrderStatusList = XMLUtil.getNodeListByXpath(XML, "/MonitorConsolidation/Order/OrderStatuses/OrderStatus");
				Set<String> strOrderReleaseKeySet = new HashSet<String>(); 
				for(int i1=0;i1<nOrderStatusList.getLength();i1++){
					Element eleOrderStatusType = (Element)nOrderStatusList.item(i1);
					String sorderReleaseKey = eleOrderStatusType.getAttribute(VSIConstants.ATTR_ORDER_RELEASE_KEY);

					Document docCreateExInput = XMLUtil.createDocument("Inbox");
					Element eleInbox = docCreateExInput.getDocumentElement();

					eleInbox.setAttribute(VSIConstants.ATTR_ORDER_NO, strOrderNo);
					eleInbox.setAttribute(VSIConstants.ATTR_ORDER_HEADER_KEY, strOrderHeaderKey);
					eleInbox.setAttribute(VSIConstants.ATTR_CONSOLIDATE,
							"Y");
					eleInbox.setAttribute("ConsolidationWindow",
							"FOREVER");
					eleInbox.setAttribute(VSIConstants.ATTR_BILL_TO_ID,
							strBillToID);
					eleInbox.setAttribute(VSIConstants.ATTR_DESCRIPTION,
							"Release Ack Delays");
					eleInbox.setAttribute(VSIConstants.ATTR_EXCEPTION_TYPE,
							"WMS_ALERTS");
					eleInbox.setAttribute("FlowName", "VSIRaiseDelayedAckAlert");
					if(VSIConstants.ATTR_GOOGLE_EXPRESS.equalsIgnoreCase(strEnterpriseCode)){
						eleInbox.setAttribute(VSIConstants.ATTR_QUEUE_ID,
								"VSI_RELEASE_ACK_DELAY_GOOGLE_EXPRESS");
					}
					else{
					eleInbox.setAttribute(VSIConstants.ATTR_QUEUE_ID,
							"VSI_RELEASE_ACK_DELAY");
					}
					eleInbox.setAttribute("EnterpriseKey",strEnterpriseCode);
					Element eleCreateExtnInput = docCreateExInput.createElement("Extn");
					eleCreateExtnInput.setAttribute(VSIConstants.ATTR_ORDER_RELEASE_KEY, sorderReleaseKey);
					Element eleInboxReferencesListElement = docCreateExInput
							.createElement("InboxReferencesList");
					eleInbox.appendChild(eleCreateExtnInput);
					eleInbox.appendChild(eleInboxReferencesListElement);
					Element eleInboxReferencesElement = docCreateExInput
							.createElement("InboxReferences");

					eleInboxReferencesElement.setAttribute(VSIConstants.ATTR_NAME,
							"OrderReleaseKey");
					eleInboxReferencesElement.setAttribute(VSIConstants.ATTR_REFERENCE_TYPE,
							"TEXT");
					eleInboxReferencesElement.setAttribute(VSIConstants.ATTR_VALUE, sorderReleaseKey);

					eleInboxReferencesListElement.appendChild(eleInboxReferencesElement);

					Element docConsolidationTemplate = docCreateExInput
							.createElement("ConsolidationTemplate");
					eleInbox.appendChild(docConsolidationTemplate);
					Element eleInboxCpy = docCreateExInput.createElement("Inbox");
					eleInboxCpy.setAttribute(VSIConstants.ATTR_ORDER_HEADER_KEY, strOrderHeaderKey);
					eleInboxCpy.setAttribute(VSIConstants.ATTR_ORDER_NO, strOrderNo);
					eleInboxCpy.setAttribute(VSIConstants.ATTR_DESCRIPTION,
							"Release Ack Delays");
					eleInboxCpy.setAttribute("FlowName", "VSIRaiseDelayedAckAlert");
					if(VSIConstants.ATTR_GOOGLE_EXPRESS.equalsIgnoreCase(strEnterpriseCode)){
						eleInbox.setAttribute(VSIConstants.ATTR_QUEUE_ID,
								"VSI_RELEASE_ACK_DELAY_GOOGLE_EXPRESS");
					}
					else{
					eleInboxCpy.setAttribute(VSIConstants.ATTR_QUEUE_ID,
							"VSI_RELEASE_ACK_DELAY");
					}
					eleInboxCpy.setAttribute("EnterpriseKey",strEnterpriseCode);
					eleInboxCpy.setAttribute(VSIConstants.ATTR_EXCEPTION_TYPE,
							"WMS_ALERTS");
					Element eleInboxCpy1 = docCreateExInput.createElement("Extn");
					eleInboxCpy1.setAttribute(VSIConstants.ATTR_ORDER_RELEASE_KEY, sorderReleaseKey);
					
					eleInboxCpy.appendChild(eleInboxCpy1);

					docConsolidationTemplate.appendChild(eleInboxCpy);
					
					Document docInboxListTemplate = SCXmlUtil
							.createDocument("Inbox");
					Element eleInboxListTemplate = docInboxListTemplate
							.getDocumentElement();
					eleInboxListTemplate.setAttribute("OrderNo", "");

					if(log.isDebugEnabled()){
						log.debug("Printing inputdoc \n"+XMLUtil.getXMLString(docCreateExInput));
					}
					if(!strOrderReleaseKeySet.contains(sorderReleaseKey)){
					VSIUtils.invokeAPI(env,docInboxListTemplate, VSIConstants.API_CREATE_EXCEPTION,docCreateExInput);
					strOrderReleaseKeySet.add(sorderReleaseKey);
					}
					
				}

			}

			catch (YFSException | ParserConfigurationException e) {
				e.printStackTrace();
				// YFSException e1 = new YFSException();

			} 
		}
	}

}