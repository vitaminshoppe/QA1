package com.vsi.oms.api;

import java.io.PrintStream;
import java.math.RoundingMode;
import java.net.SocketTimeoutException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.text.DecimalFormat;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.vsi.oms.api.order.VSITobTaxJurisdiction;
import com.vsi.oms.utils.InvokeWebservice;
import com.vsi.oms.utils.VSIConstants;
import com.vsi.oms.utils.VSIUtils;
import com.vsi.oms.utils.XMLUtil;
import com.yantra.interop.japi.YIFApi;
import com.yantra.interop.japi.YIFClientFactory;
import com.yantra.interop.japi.YIFCustomApi;
import com.yantra.yfc.core.YFCIterable;
import com.yantra.yfc.core.YFCObject;
import com.yantra.yfc.dom.YFCDocument;
import com.yantra.yfc.dom.YFCElement;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.core.YFSSystem;
import com.yantra.yfs.japi.YFSEnvironment;

public class VSIVertexTaxWebService implements YIFCustomApi {

	static HashMap<String, Integer> CCPI_Timeout = null; // Time in seconds to
	// wait for pricing
	// call, by brand
	static final String NAMESPACE = "urn";
	/**
	 * get instance of logger
	 */
	private YFCLogCategory log = YFCLogCategory.instance(VSIVertexTaxWebService.class);

	YIFApi api;
	Boolean Flag = false;
	Boolean hasTaxType = false;

	private String getItemDetails(YFSEnvironment env, String itemID, String organizationCode) {
		String itemDetail = null;
		Document getItemListInput;
		try {

			/*
			 * <Item ItemID="1000" OrganizationCode="DEFAULT" > </Item>
			 */
			String API_GET_ITEM_LIST = "getItemList";
			getItemListInput = XMLUtil.createDocument("Item");
			Element eleOrder = getItemListInput.getDocumentElement();
			eleOrder.setAttribute("ItemID", itemID);
			eleOrder.setAttribute("OrganizationCode", organizationCode);
			api = YIFClientFactory.getInstance().getApi();
			env.setApiTemplate("API_GET_ITEM_LIST", "global/template/api/getItemList");
			Document outDoc = api.invoke(env, API_GET_ITEM_LIST, getItemListInput);

			itemDetail = YFCDocument.getDocumentFor(outDoc).getDocumentElement().getChildElement("Item")
					.getChildElement("Extn").getAttribute("ExtnTaxProductCode");
		} catch (Exception e) {
			// //System.out.println("Exception in getItemDetails:NAA:\n"+e.getMessage());
			e.printStackTrace(new PrintStream(System.out));
		}
		// //System.out.println("itemDetail= "+itemDetail);
		return itemDetail;
	}
	


	public Document invoke(YFSEnvironment env, Document inXML) throws Exception {
	
		log.info("VSIVertexTaxWebService: Input XML to invoke method "+SCXmlUtil.getString(inXML));
		Document responseDoc = null;
		Element eleResponse = null;
		
		Element eleOrderIn = (Element) inXML.getElementsByTagName("Order").item(0);
		Element eleOrderLinesIn = SCXmlUtil.getChildElement(eleOrderIn, VSIConstants.ELE_ORDER_LINES);
		ArrayList<Element> alVertexResponse = new ArrayList<Element>();

		// Header nodes input for vertex call
		Document invokeVertex = SCXmlUtil.createDocument("Order");
		Element eleInvokeVertex = invokeVertex.getDocumentElement();
		if (eleOrderIn != null) {
			String strBillToID = eleOrderIn.getAttribute("BillToID");
			eleInvokeVertex.setAttribute("BillToID", strBillToID);
			String strCustomerEmailID = eleOrderIn.getAttribute("CustomerEMailID");
			eleInvokeVertex.setAttribute("CustomerEmailID", strCustomerEmailID);
			String strDocumentType = eleOrderIn.getAttribute("DocumentType");
			eleInvokeVertex.setAttribute("DocumentType", strDocumentType);
			String strEnterpriseCode = eleOrderIn.getAttribute("EnterpriseCode");
			eleInvokeVertex.setAttribute("EnterpriseCode", strEnterpriseCode);
			String strEntryType = eleOrderIn.getAttribute("EntryType");
			eleInvokeVertex.setAttribute("EntryType", strEntryType);
			String strOrderDate = eleOrderIn.getAttribute("OrderDate");
			eleInvokeVertex.setAttribute("OrderDate", strOrderDate);
			String strOrderHeaderKey = eleOrderIn.getAttribute("OrderHeaderKey");
			eleInvokeVertex.setAttribute("OrderHeaderKey", strOrderHeaderKey);
			String strOrderNo = eleOrderIn.getAttribute("OrderNo");
			eleInvokeVertex.setAttribute("OrderNo", strOrderNo);
			String strOrderType = eleOrderIn.getAttribute("OrderType");
			eleInvokeVertex.setAttribute("OrderType", strOrderType);
			String strSellerOrganizationCode = eleOrderIn.getAttribute("SellerOrganizationCode");
			eleInvokeVertex.setAttribute("SellerOrganizationCode", strSellerOrganizationCode);
			String strTaxExemptFlag = eleOrderIn.getAttribute("TaxExemptFlag");
			eleInvokeVertex.setAttribute("TaxExemptFlag", strTaxExemptFlag);
			String strTaxExemptionCertificate = eleOrderIn.getAttribute("TaxExemptionCertificate");
			eleInvokeVertex.setAttribute("TaxExemptionCertificate", strTaxExemptionCertificate);

			// Adding Extn Tag
			Node nodeExtnOrderIn = (Node) eleOrderIn.getElementsByTagName("Extn").item(0);
			Node nodeExtnCpyNode = invokeVertex.importNode(nodeExtnOrderIn, true);
			Element eleExtn = (Element) nodeExtnCpyNode;
			eleInvokeVertex.appendChild(eleExtn);

			// Header Charges
			Element inHdrChgsEle = (Element) eleOrderIn.getElementsByTagName("HeaderCharges").item(0);
			if (inHdrChgsEle != null) {
				Element hdrChargesEle = invokeVertex.createElement("HeaderCharges");
				eleInvokeVertex.appendChild(hdrChargesEle);
				NodeList inHdrChargeNL = inHdrChgsEle.getElementsByTagName("HeaderCharge");
				for (int m = 0; m < inHdrChargeNL.getLength(); m++) {
					Node inHdrChargeNode = inHdrChargeNL.item(m);
					Node hdrChargeCpyNde = invokeVertex.importNode(inHdrChargeNode, true);
					Element hdrChargeEle = (Element) hdrChargeCpyNde;
					hdrChargesEle.appendChild(hdrChargeEle);

				}
			}

			// Header Taxes
			Element inHdrTaxsEle = (Element) eleOrderIn.getElementsByTagName("HeaderTaxes").item(0);
			if (inHdrTaxsEle != null) {
				Element hdrTaxesEle = invokeVertex.createElement("HeaderTaxes");
				eleInvokeVertex.appendChild(hdrTaxesEle);

				NodeList inHdrTaxNL = inHdrTaxsEle.getElementsByTagName("HeaderTax");
				for (int m = 0; m < inHdrTaxNL.getLength(); m++) {
					//Element inHdrTaxEle = (Element) inHdrTaxNL.item(m);
					Node inHdrTaxNode = inHdrTaxNL.item(m);
					Node hdrtaxCpyNde = invokeVertex.importNode(inHdrTaxNode, true);
					//Element inExtnHTEle = (Element) inHdrTaxEle.getElementsByTagName("Extn").item(0);
					Element hdrTaxEle = (Element) hdrtaxCpyNde;
					hdrTaxesEle.appendChild(hdrTaxEle);
				}
			}

			// Adding Price info
			Node nodePriceInfoOrderIn = (Node) eleOrderIn.getElementsByTagName("PriceInfo").item(0);
			Node nodePriceInfoCpyNode = invokeVertex.importNode(nodePriceInfoOrderIn, true);
			Element elePriceInfo = (Element) nodePriceInfoCpyNode;
			eleInvokeVertex.appendChild(elePriceInfo);

			// Adding Promotions
			Node nodePromotionsOrderIn = (Node) eleOrderIn.getElementsByTagName("Promotions").item(0);
			Node nodePromotionsCpyNode = invokeVertex.importNode(nodePromotionsOrderIn, true);
			Element elePromotions = (Element) nodePromotionsCpyNode;
			eleInvokeVertex.appendChild(elePromotions);

			// Adding PersonInfoBillTo
			Node nodeBillToOrderIn = (Node) eleOrderIn.getElementsByTagName("PersonInfoBillTo").item(0);
			Node nodeBillToCpyNode = invokeVertex.importNode(nodeBillToOrderIn, true);
			Element eleBillTo = (Element) nodeBillToCpyNode;
			eleInvokeVertex.appendChild(eleBillTo);

			// Adding PersonInfoShipTo
			Node nodeShipToOrderIn = (Node) eleOrderIn.getElementsByTagName("PersonInfoShipTo").item(0);
			Node nodeShipToCpyNode = invokeVertex.importNode(nodeShipToOrderIn, true);
			Element eleShipTo = (Element) nodeShipToCpyNode;
			eleInvokeVertex.appendChild(eleShipTo);
		}

		// Adding Order Lines
		NodeList nlEleOrderLineIn = eleOrderLinesIn.getElementsByTagName(VSIConstants.ELE_ORDER_LINE);
		ArrayList<Element> alOrderLineSHP = new ArrayList<Element>();
		ArrayList<Element> alOrderLinePICK = new ArrayList<Element>();
		Set<String> uniqueNodesPICK = new HashSet<String>();
		int LengthOfnlEleOrderLineIn = nlEleOrderLineIn.getLength();
		if (LengthOfnlEleOrderLineIn > 0) {
			for (int i = 0; i < LengthOfnlEleOrderLineIn; i++) {
				Element eleOrderLineIn = (Element) nlEleOrderLineIn.item(i);
				String strDelMethod = eleOrderLineIn.getAttribute(VSIConstants.ATTR_DELIVERY_METHOD);
				String strShipNode = eleOrderLineIn.getAttribute(VSIConstants.ATTR_SHIP_NODE);

				if (strDelMethod.equalsIgnoreCase("SHP")) {
					alOrderLineSHP.add(eleOrderLineIn);
				} else {
					alOrderLinePICK.add(eleOrderLineIn);
					if (!strShipNode.isEmpty()) {
						uniqueNodesPICK.add(strShipNode);
					}
				}

			}
		}
		int countSHP = alOrderLineSHP.size();
		int countPICK = alOrderLinePICK.size();

		int countUniqueNodesPICK = uniqueNodesPICK.size();
		//System.out.println("countUniqueNodesPICK " + countUniqueNodesPICK);
		//System.out.println("countSHP" + countSHP);
		// String strShipNodeFromSet="";

		//Checking if its a mix cart order
		if (countSHP > 0 && countPICK > 0) {
			//For PICK type of order lines
			if (countUniqueNodesPICK > 0) {
				Iterator<String> itr = uniqueNodesPICK.iterator();
				while (itr.hasNext()) {
					Document docInvokeVertexPick = (Document) invokeVertex.cloneNode(true);
					Element nodeHeaderCharges = (Element) docInvokeVertexPick.getElementsByTagName("HeaderCharges")
							.item(0);
					Element nodeHeaderTaxes = (Element) docInvokeVertexPick.getElementsByTagName("HeaderTaxes").item(0);
					if (nodeHeaderCharges != null) {
						nodeHeaderCharges.getParentNode().removeChild(nodeHeaderCharges);
					}
					if (nodeHeaderCharges != null) {
						nodeHeaderTaxes.getParentNode().removeChild(nodeHeaderTaxes);
					}

					Element eleInvokeVertexPick = docInvokeVertexPick.getDocumentElement();
					Element eleInvokeVertexOrderLns = SCXmlUtil.createChild(eleInvokeVertexPick, "OrderLines");
					// System.out.println("invokeVertexPick
					// "+SCXmlUtil.getString(docInvokeVertexPick));
					String strShipNodeFromSet = itr.next();
					// System.out.println("Value from Set"+strShipNodeFromSet);
					for (int k = 0; k < countPICK; k++) {
						String strPICKShipNode = alOrderLinePICK.get(k).getAttribute(VSIConstants.ATTR_SHIP_NODE);
						// System.out.println("Value form strSHPShipNode"+strPICKShipNode);
						if (strShipNodeFromSet.equals(strPICKShipNode)) {
							Node eleOrderLnFromPickArray = (Node) alOrderLinePICK.get(k);
							Node nodeOrderLnCpyNode = docInvokeVertexPick.importNode(eleOrderLnFromPickArray, true);
							Element eleOrderLnToAppend = (Element) nodeOrderLnCpyNode;
							eleInvokeVertexOrderLns.appendChild(eleOrderLnToAppend);

						}
					}
					log.info("RequestDoc for PICK from VSIVertexTaxWebService.java "+SCXmlUtil.getString(docInvokeVertexPick));
					Document docResponsePICK = this.invokeWebservice(env, docInvokeVertexPick);
					log.info("ResponseDoc for PICK from VSIVertexTaxWebService.java "+SCXmlUtil.getString(docResponsePICK));
					Element eleResponsePICK = docResponsePICK.getDocumentElement();
					alVertexResponse.add(eleResponsePICK);
					// System.out.println("printing invokeVertex"+
					// SCXmlUtil.getString(docInvokeVertexPick));

				}
			} 
			//For SHP type of order lines
			if (countSHP>0) {
				Document docInvokeVertexSHP = (Document) invokeVertex.cloneNode(true);
				Element eleInvokeVertexSHP = docInvokeVertexSHP.getDocumentElement();
				Element eleInvokeVertexOrderLns = SCXmlUtil.createChild(eleInvokeVertexSHP, "OrderLines");
				for (int k = 0; k < countSHP; k++) {
					Node eleOrderLnFromShpArray = (Node) alOrderLineSHP.get(k);
					Node nodeOrderLnCpyNode = docInvokeVertexSHP.importNode(eleOrderLnFromShpArray, true);
					Element eleOrderLnToAppend = (Element) nodeOrderLnCpyNode;
					eleInvokeVertexOrderLns.appendChild(eleOrderLnToAppend);
				}

				log.info("RequestDoc for SHIP from VSIVertexTaxWebService.java "+SCXmlUtil.getString(docInvokeVertexSHP));
				Document docResponseSHP = this.invokeWebservice(env, docInvokeVertexSHP);
				log.info("ResponseDoc for SHIP from VSIVertexTaxWebService.java "+SCXmlUtil.getString(docResponseSHP));
				Element eleResponseSHP = docResponseSHP.getDocumentElement();
				alVertexResponse.add(eleResponseSHP);
				// System.out.println("docInvokeVertexSHP " +
				// SCXmlUtil.getString(docInvokeVertexSHP));

			}
			
			//Preparing Final Response for the UE.
			int totalNoOfResponse = alVertexResponse.size();
			if (totalNoOfResponse > 0) {
				// Preparing Header Response

				Element headerResponse = alVertexResponse.get(0);
				Element headerResponseSHP=null;
				responseDoc = SCXmlUtil.createDocument("Order");
				eleResponse = responseDoc.getDocumentElement();
				
				for (int i = 0; i < totalNoOfResponse; i++) {
					Element eleHeaderChrgs= (Element) alVertexResponse.get(i).getElementsByTagName("HeaderCharges").item(0);
					if (eleHeaderChrgs != null) {
						NodeList outHdrTaxNL = eleHeaderChrgs.getElementsByTagName("HeaderCharge");
						if(outHdrTaxNL.getLength()>0) {
							headerResponseSHP=(Element) alVertexResponse.get(i);
							break;
						}
						
					}
				}
				
				if (headerResponse != null) {

					String strBillToID = headerResponse.getAttribute("BillToID");
					eleResponse.setAttribute("BillToID", strBillToID);
					String strCustomerEmailID = headerResponse.getAttribute("CustomerEMailID");
					eleResponse.setAttribute("CustomerEmailID", strCustomerEmailID);
					String strDocumentType = headerResponse.getAttribute("DocumentType");
					eleResponse.setAttribute("DocumentType", strDocumentType);
					String strEnterpriseCode = headerResponse.getAttribute("EnterpriseCode");
					eleResponse.setAttribute("EnterpriseCode", strEnterpriseCode);
					String strEntryType = headerResponse.getAttribute("EntryType");
					eleResponse.setAttribute("EntryType", strEntryType);

					String strMaxOrderStatus = headerResponse.getAttribute("MaxOrderStatus");
					eleResponse.setAttribute("MaxOrderStatus", strMaxOrderStatus);
					String strMinOrderStatus = headerResponse.getAttribute("MinOrderStatus");
					eleResponse.setAttribute("MinOrderStatus", strMinOrderStatus);
					String strStatus = headerResponse.getAttribute("Status");
					eleResponse.setAttribute("Status", strStatus);

					String strOrderDate = headerResponse.getAttribute("OrderDate");
					eleResponse.setAttribute("OrderDate", strOrderDate);
					String strOrderHeaderKey = headerResponse.getAttribute("OrderHeaderKey");
					eleResponse.setAttribute("OrderHeaderKey", strOrderHeaderKey);
					String strOrderNo = headerResponse.getAttribute("OrderNo");
					eleResponse.setAttribute("OrderNo", strOrderNo);
					String strOrderType = headerResponse.getAttribute("OrderType");
					eleResponse.setAttribute("OrderType", strOrderType);
					String strSellerOrganizationCode = headerResponse.getAttribute("SellerOrganizationCode");
					eleResponse.setAttribute("SellerOrganizationCode", strSellerOrganizationCode);
					String strTaxExemptFlag = headerResponse.getAttribute("TaxExemptFlag");
					eleResponse.setAttribute("TaxExemptFlag", strTaxExemptFlag);
					String strTaxExemptionCertificate = headerResponse.getAttribute("TaxExemptionCertificate");
					eleResponse.setAttribute("TaxExemptionCertificate", strTaxExemptionCertificate);

					// Adding Extn Tag
					Node nodeExtnOrderOut = (Node) headerResponse.getElementsByTagName("Extn").item(0);
					Node nodeExtnCpyNode = responseDoc.importNode(nodeExtnOrderOut, true);
					Element eleExtn = (Element) nodeExtnCpyNode;
					eleResponse.appendChild(eleExtn);

					// Header Charges
					if(headerResponseSHP !=null){
					log.info("headerResponseSHP for header charges is "+SCXmlUtil.getString(headerResponseSHP));
					Element outHdrChgsEle = (Element) headerResponseSHP.getElementsByTagName("HeaderCharges").item(0);
					if (outHdrChgsEle != null) {
						Element hdrChargesEle = responseDoc.createElement("HeaderCharges");
						eleResponse.appendChild(hdrChargesEle);
						NodeList outHdrChargeNL = outHdrChgsEle.getElementsByTagName("HeaderCharge");
						for (int m = 0; m < outHdrChargeNL.getLength(); m++) {
							Node outHdrChargeNode = outHdrChargeNL.item(m);
							Node hdrChargeCpyNde = responseDoc.importNode(outHdrChargeNode, true);
							Element hdrChargeEle = (Element) hdrChargeCpyNde;
							hdrChargesEle.appendChild(hdrChargeEle);

						}
					}
					}

					// Header Taxes
					if(headerResponseSHP !=null){
					log.info("headerResponseSHP for header taxes is "+SCXmlUtil.getString(headerResponseSHP));
					Element outHdrTaxsEle = (Element) headerResponseSHP.getElementsByTagName("HeaderTaxes").item(0);
					if (outHdrTaxsEle != null) {
						Element hdrTaxesEle = responseDoc.createElement("HeaderTaxes");
						eleResponse.appendChild(hdrTaxesEle);

						NodeList outHdrTaxNL = outHdrTaxsEle.getElementsByTagName("HeaderTax");
						for (int m = 0; m < outHdrTaxNL.getLength(); m++) {
							//Element outHdrTaxEle = (Element) outHdrTaxNL.item(m);
							Node outHdrTaxNode = outHdrTaxNL.item(m);
							Node hdrtaxCpyNde = responseDoc.importNode(outHdrTaxNode, true);
							//Element outExtnHTEle = (Element) outHdrTaxEle.getElementsByTagName("Extn").item(0);
							Element hdrTaxEle = (Element) hdrtaxCpyNde;
							hdrTaxesEle.appendChild(hdrTaxEle);
						}
					}
					}

					// Adding Price info
					Node nodePriceInfoOrderOut = (Node) headerResponse.getElementsByTagName("PriceInfo").item(0);
					Node nodePriceInfoCpyNode = responseDoc.importNode(nodePriceInfoOrderOut, true);
					Element elePriceInfo = (Element) nodePriceInfoCpyNode;
					eleResponse.appendChild(elePriceInfo);

					// Adding Promotions
					Node nodePromotionsOrderOut = (Node) headerResponse.getElementsByTagName("Promotions").item(0);
					Node nodePromotionsCpyNode = responseDoc.importNode(nodePromotionsOrderOut, true);
					Element elePromotions = (Element) nodePromotionsCpyNode;
					eleResponse.appendChild(elePromotions);

					// Adding PersonInfoBillTo
					Node nodeBillToOrderOut = (Node) headerResponse.getElementsByTagName("PersonInfoBillTo").item(0);
					Node nodeBillToCpyNode = responseDoc.importNode(nodeBillToOrderOut, true);
					Element eleBillTo = (Element) nodeBillToCpyNode;
					eleResponse.appendChild(eleBillTo);

					// Adding PersonInfoShipTo
					Node nodeShipToOrderOut = (Node) headerResponse.getElementsByTagName("PersonInfoShipTo").item(0);
					Node nodeShipToCpyNode = responseDoc.importNode(nodeShipToOrderOut, true);
					Element eleShipTo = (Element) nodeShipToCpyNode;
					eleResponse.appendChild(eleShipTo);

				}

			}

			// Preparing Order Line Data Response
			Element eleOrderLinesRespOut = SCXmlUtil.createChild(eleResponse, VSIConstants.ELE_ORDER_LINES);
			Iterator<Element> itrResp = alVertexResponse.iterator();
			while (itrResp.hasNext()) {
				Element itrResponse = itrResp.next();

				// Adding Order Lines
				NodeList nlEleOrderLineOut = itrResponse.getElementsByTagName(VSIConstants.ELE_ORDER_LINE);
				int lengthOfnlEleOrderLineOut = nlEleOrderLineOut.getLength();
				if (lengthOfnlEleOrderLineOut > 0) {
					for (int m = 0; m < lengthOfnlEleOrderLineOut; m++) {
						Node eleOrderLineOut = (Node) nlEleOrderLineOut.item(m);
						Node nodeOrderLnCpyNode = responseDoc.importNode(eleOrderLineOut, true);
						Element eleOrderLnToAppend = (Element) nodeOrderLnCpyNode;
						eleOrderLinesRespOut.appendChild(eleOrderLnToAppend);
					}
				}

			}

		}
		// Calling normal flow for Non-Mix Cart orders
		else {
			responseDoc = this.invokeWebservice(env, inXML);
		}
		log.info("Final responseDoc from VSIVertexTaxWebService.java "+SCXmlUtil.getString(responseDoc));
		return responseDoc;

	}

	public Document invokeWebservice(YFSEnvironment env, Document inXML) throws Exception {
	log.info("VSIVertexTaxWebService: Input XML to invokeWebservice method "+SCXmlUtil.getString(inXML));
		float calQty = 1.00f;

		YFCElement docEInXML = YFCDocument.getDocumentFor(inXML).getDocumentElement();
		YFCElement eOrderLines = YFCDocument.getDocumentFor(inXML).getDocumentElement().getChildElement("OrderLines");
		YFCElement eOrderLine = null;
		String deliveryMethod = "";
		if (null != eOrderLines) {
			eOrderLine = eOrderLines.getChildElement("OrderLine");
			deliveryMethod = eOrderLine.getAttribute("DeliveryMethod");

		}

		if (!checkElements(inXML)) {
			YFCElement extn1 = YFCDocument.getDocumentFor(inXML).getDocumentElement().getChildElement("Extn", true);
			/*
			 * String taxExempt = inXML.getDocumentElement().getAttribute("TaxExemptFlag");
			 * extn1.setAttribute("ExtnVertexEngine", "null");
			 * if(taxExempt.equalsIgnoreCase("Y")){
			 * extn1.setAttribute("ExtnIsTaxCalculated", "Y"); }else{
			 * extn1.setAttribute("ExtnIsTaxCalculated", "N"); }
			 */
			YFCIterable<YFCElement> orderLineItr = YFCDocument.getDocumentFor(inXML).getDocumentElement()
					.getChildElement("OrderLines").getChildren("OrderLine");
			while (orderLineItr.hasNext()) {
				YFCElement orderLine = orderLineItr.next();

				YFCElement eleLineTaxes = orderLine.getChildElement("LineTaxes", true);
				YFCElement eleLineTax = eleLineTaxes.getChildElement("LineTax", true);

				String strTax = eleLineTax.getAttribute("TaxName");
				if (YFCObject.isVoid(strTax)) {
					eleLineTax.setAttribute("TaxName", "Sales Tax");
					eleLineTax.setAttribute("Tax", "0.0");
					eleLineTax.setAttribute("TaxPercentage", "0.00");
					eleLineTax.setAttribute("TaxableFlag", "N");
					YFCElement eleExtn = eleLineTax.getChildElement("Extn", true);
				}
			}

			// System.out.print("This is a null Check:NEHA");
			return inXML;
		}

		String xml = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">  "
				+ " <soapenv:Header/> " + " <soapenv:Body> "
				+ " <urn:VertexEnvelope xmlns:urn=\"urn:vertexinc:o-series:tps:6:0\"> " + " <urn:Login> "
				+ " <urn:UserName>ecommerce</urn:UserName> " + " <urn:Password>password</urn:Password> "
				+ " </urn:Login> "
				+ " <urn:QuotationRequest documentDate=\"2012-08-20\" returnAssistedParametersIndicator=\"true\" transactionType=\"SALE\"> "
				+ "  <urn:Currency/> " + " <urn:Seller>" + " <urn:Company>VSP</urn:Company>"
				+ " <urn:Division>VSI01</urn:Division>" + " <urn:PhysicalOrigin locationCode=\"\" />"
				+ " </urn:Seller> " + " <urn:Customer> " + "  <urn:Destination> "
				+ "  <urn:StreetAddress1>3890 Harbor Blvd </urn:StreetAddress1> "
				+ " <urn:StreetAddress2> back entrance</urn:StreetAddress2> " + "  <urn:City>WANTAGH</urn:City> "
				+ "<urn:MainDivision>NY</urn:MainDivision> " + "<urn:PostalCode>11793</urn:PostalCode> "
				+ " </urn:Destination> " + " </urn:Customer> " +

				" </urn:QuotationRequest> " + "</urn:VertexEnvelope> " + " </soapenv:Body> " + "</soapenv:Envelope>";
		Document taxOut = null;

		// current Date
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Calendar cal = Calendar.getInstance();
		// //System.out.println(dateFormat.format(cal.getTime()));
		YFCElement pShipTo = YFCDocument.getDocumentFor(inXML).getDocumentElement().getChildElement("PersonInfoShipTo");
		YFCElement orgCode = YFCDocument.getDocumentFor(inXML).getDocumentElement();
		String taxExemptFlag = inXML.getDocumentElement().getAttribute("TaxExemptFlag");

		// YFCElement
		// address=YFCDocument.getDocumentFor(inXML).getDocumentElement().getChildElement("PersonInfoShipTo");
		// YFCElement el=address.getChildElement("PersonInfoShip");

		String strAddressLine1 = pShipTo.getAttribute("AddressLine1");
		String strAddressLine2 = pShipTo.getAttribute("AddressLine2");
		String strCity = pShipTo.getAttribute("City");
		String strState = pShipTo.getAttribute("State");
		String strZipCode = pShipTo.getAttribute("ZipCode");
		// String strOrgCode=orgCode.getAttribute("EnterpriseCode");

		YFCDocument vertexTaxInput = YFCDocument.getDocumentFor(xml);
		YFCElement vertexTaxInputEle = vertexTaxInput.getDocumentElement();
		YFCElement vEnvelope = vertexTaxInputEle.getChildElement("soapenv:Body", true)
				.getChildElement("urn:VertexEnvelope", true);
		YFCElement tmp = vEnvelope.getChildElement("urn:QuotationRequest");
		tmp.setAttribute("documentDate", dateFormat.format(cal.getTime())); // documentDate
		// current
		// system
		// date

		if ("SHP".equals(deliveryMethod)) {
			String strCodeLongDesc = null;
			Document docCommonCodeInput = SCXmlUtil.createDocument(VSIConstants.ELE_COMMON_CODE);
			Element eleCommonCodeInput = docCommonCodeInput.getDocumentElement();
			eleCommonCodeInput.setAttribute(VSIConstants.ATTR_CODE_TYPE, "SHIPFROM_TAX_LOC");
			Document docCommonCodeListOutput = VSIUtils.invokeAPI(env, VSIConstants.API_COMMON_CODE_LIST,
					docCommonCodeInput);
			Element eleCommonCodeListOutput = docCommonCodeListOutput.getDocumentElement();
			Element eleCommonCodeOutput = SCXmlUtil.getChildElement(eleCommonCodeListOutput,
					VSIConstants.ELE_COMMON_CODE);
			strCodeLongDesc = eleCommonCodeOutput.getAttribute(VSIConstants.ATTR_CODE_LONG_DESC);
			if (YFCCommon.isVoid(strCodeLongDesc)) {
				strCodeLongDesc = "9004";
			}
			vEnvelope.getChildElement("urn:QuotationRequest").getChildElement("urn:Seller")
					.getChildElement("urn:PhysicalOrigin").setAttribute("locationCode", strCodeLongDesc);
		} else {
			vEnvelope.getChildElement("urn:QuotationRequest").getChildElement("urn:Seller")
					.removeChild(vEnvelope.getChildElement("urn:QuotationRequest").getChildElement("urn:Seller")
							.getChildElement("urn:PhysicalOrigin"));
		}

		Element eleHeaderChargeSC = XMLUtil.getElementByXPath(inXML,
				"//HeaderCharges/HeaderCharge[@ChargeCategory='Shipping']");
		double attrChargeAmountSC = 0.0;
		if (!YFCObject.isVoid(eleHeaderChargeSC)) {
			attrChargeAmountSC = SCXmlUtil.getDoubleAttribute(eleHeaderChargeSC, "ChargeAmount");
		}

		Element eleHeaderChargeDC = XMLUtil.getElementByXPath(inXML,
				"//HeaderCharges/HeaderCharge[@ChargeCategory='Shipping Discount']");
		double attrChargeAmountDC = 0.0;
		if (!YFCObject.isVoid(eleHeaderChargeDC)) {
			attrChargeAmountDC = SCXmlUtil.getDoubleAttribute(eleHeaderChargeDC, "ChargeAmount");
		}

		Element eleHeaderChargeAppeasement = XMLUtil.getElementByXPath(inXML,
				"//HeaderCharges/HeaderCharge[@ChargeName='Shipping Appeasement']");
		double attrChargeAmountAppeasement = 0.0;
		if (!YFCObject.isVoid(eleHeaderChargeAppeasement)) {
			attrChargeAmountAppeasement = SCXmlUtil.getDoubleAttribute(eleHeaderChargeAppeasement, "ChargeAmount");
		}

		double shippingAmount = attrChargeAmountSC - attrChargeAmountDC - attrChargeAmountAppeasement;

		// set user name and password
		final String strVertexUserName = YFSSystem.getProperty("VERTEX_O_USER_NAME");
		final String strVertexPassword = YFSSystem.getProperty("VERTEX_O_USER_PASS");

		if (!YFCObject.isVoid(strVertexUserName) && !YFCObject.isVoid(strVertexPassword)) {
			YFCElement eleLogin = vEnvelope.getChildElement("urn:Login", true);
			YFCElement eleUsername = eleLogin.getChildElement("urn:UserName", true);
			YFCElement elePass = eleLogin.getChildElement("urn:Password", true);
			eleUsername.setNodeValue(strVertexUserName);
			elePass.setNodeValue(strVertexPassword);

		}

		YFCElement temp = vEnvelope.getChildElement("urn:QuotationRequest").getChildElement("urn:Customer")
				.getChildElement("urn:Destination");
		temp.getChildElement("urn:StreetAddress1").setNodeValue(strAddressLine1);
		temp.getChildElement("urn:StreetAddress2").setNodeValue(strAddressLine2);
		temp.getChildElement("urn:City").setNodeValue(strCity);
		temp.getChildElement("urn:MainDivision").setNodeValue(strState);
		temp.getChildElement("urn:PostalCode").setNodeValue(strZipCode);
		if (taxExemptFlag.equalsIgnoreCase("Y")) {
			vEnvelope.getChildElement("urn:QuotationRequest").getChildElement("urn:Customer")
					.setAttribute("isTaxExempt", "true");
		}
		YFCIterable<YFCElement> orderLineItr = YFCDocument.getDocumentFor(inXML).getDocumentElement()
				.getChildElement("OrderLines").getChildren("OrderLine");
		String slineItem = "<urn:LineItem lineItemNumber=\"1\"> "
				+ "<urn:Product productClass=\"0027\">1767706</urn:Product> " + "<urn:Quantity>1</urn:Quantity> "
				+ "<urn:ExtendedPrice>10</urn:ExtendedPrice> " + "<urn:Customer>"
				+ "  <urn:Destination locationCode=\"\"> "
				+ "  <urn:StreetAddress1>3890 Harbor Blvd </urn:StreetAddress1> "
				+ " <urn:StreetAddress2> back entrance</urn:StreetAddress2> " + "  <urn:City>WANTAGH</urn:City> "
				+ "<urn:MainDivision>NY</urn:MainDivision> " + "<urn:PostalCode>11793</urn:PostalCode> "
				+ " </urn:Destination> " + " </urn:Customer> " + "</urn:LineItem> ";

		String strAddressL1 = "";
		String strAddressL2 = "";
		String strCty = "";
		String strStat = "";
		String strZipCde = "";
		String strShipNode = "";

		while (orderLineItr.hasNext()) {
			YFCElement orderLine = orderLineItr.next();
			YFCElement ePersonInfoShipTo = orderLine.getChildElement("PersonInfoShipTo");
			if ("SHP".equals(deliveryMethod)) {
				ePersonInfoShipTo = orderLine.getChildElement("PersonInfoShipTo");
				if (YFCObject.isVoid(ePersonInfoShipTo)) {
					ePersonInfoShipTo = YFCDocument.getDocumentFor(inXML).getDocumentElement()
							.getChildElement("PersonInfoShipTo");
				}
			} else {
				YFCElement pshpToLn = orderLine.getChildElement("Shipnode");
				YFCElement shipNodePersonInfo = pshpToLn.getElementsByTagName("ShipNodePersonInfo").item(0);
				String addressLine1Value = shipNodePersonInfo.getAttribute("AddressLine1");
				if(!YFCCommon.isVoid(addressLine1Value))
					ePersonInfoShipTo = pshpToLn.getChildElement("ShipNodePersonInfo");				
			}
			YFCElement eItem = orderLine.getChildElement("Item");
			YFCElement eLineOverallTotals = orderLine.getChildElement("LineOverallTotals");

			String strPrimeLineNo = orderLine.getAttribute("PrimeLineNo");
			strShipNode = orderLine.getAttribute("ShipNode");

			String strOrderedQty = orderLine.getAttribute("OrderedQty");

			String strProduct = eItem.getAttribute("ItemID");
			// String strProductClass=el1.getAttribute("ProductClass");
			String strProductClass = getItemDetails(env, strProduct, "VSI-Cat"); // sending
			// to
			// getItem
			// details
			// System.out.print("Product Class:NAA " + strProductClass);
			// For now sending "DEFAULT" as orgCode, change it to valid orgCode
			// TODO:NehaAAmin

			strAddressL1 = ePersonInfoShipTo.getAttribute("AddressLine1");
			strAddressL2 = ePersonInfoShipTo.getAttribute("AddressLine2");
			strCty = ePersonInfoShipTo.getAttribute("City");
			strStat = ePersonInfoShipTo.getAttribute("State");
			strZipCde = ePersonInfoShipTo.getAttribute("ZipCode");
			String strLinetotal = eLineOverallTotals.getAttribute("ExtendedPrice");
			String strDisc = eLineOverallTotals.getAttribute("Discount");
			// START - Fix for SU-76
			// String strCharges = eLineOverallTotals.getAttribute("Charges");
			// END - Fix for SU-76
			float tPrice = Float.parseFloat(strLinetotal);
			float tDisc = Float.parseFloat(strDisc);

			// START - Fix for SU-76
			// float tCharges = Float.parseFloat(strCharges);

			YFCElement lineChargeEle = orderLine.getChildElement("LineCharges");
			YFCIterable<YFCElement> lineChargeItr = lineChargeEle.getChildren();

			float totalCharges = 0;

			if (Double.parseDouble(strOrderedQty) > 0) {
				while (lineChargeItr.hasNext()) {

					YFCElement lineCharge = lineChargeItr.next();

					String chargePerUnit = lineCharge.getAttribute("ChargePerUnit");
					String chargePerLine = lineCharge.getAttribute("ChargePerLine");
					String strChargeCategory = lineCharge.getAttribute("ChargeCategory");
					if (strChargeCategory.equalsIgnoreCase("Replacement charge")) {
						continue;
					}
					float fChargeperUnit = 0;
					float fChargeperLine = 0;
					if (!YFCCommon.isVoid(chargePerUnit)) {
						fChargeperUnit = Float.parseFloat(chargePerUnit);

					}
					if (!YFCCommon.isVoid(chargePerLine)) {
						fChargeperLine = Float.parseFloat(chargePerLine);
					}

					String isDiscount = lineCharge.getAttribute("IsDiscount");
					if ("Y".equals(isDiscount)) {
						totalCharges = totalCharges
								+ (((fChargeperUnit * Float.valueOf(strOrderedQty)) + fChargeperLine) * -1);
					} else {
						totalCharges = totalCharges
								+ ((fChargeperUnit * Float.valueOf(strOrderedQty)) + fChargeperLine);
					}

				}
			}
			// System.out.println("I am here"+totalCharges);

			float exPrice = tPrice + totalCharges;
			// END - Fix for SU-76

			vEnvelope.getChildElement("urn:QuotationRequest").addXMLToNode(slineItem);

			YFCIterable<YFCElement> temp2Itr = vEnvelope.getChildElement("urn:QuotationRequest")
					.getChildren("urn:LineItem"); // YFCDocument.getDocumentFor(slineItem).getDocumentElement();
			YFCElement temp2 = temp2Itr.next();
			while (temp2Itr.hasNext()) {
				temp2 = temp2Itr.next();
			}
			temp2.setAttribute("lineItemNumber", strPrimeLineNo);

			temp2.getChildElement("urn:Product").setNodeValue(strProduct);
			// System.out.print("Product Class NAA1:" + strProductClass);
			temp2.getChildElement("urn:Product").setAttribute("productClass", strProductClass);
			// temp2.setAttribute("productClass",strProductClass);
			temp2.getChildElement("urn:Quantity").setNodeValue(strOrderedQty);
			DecimalFormat df = new DecimalFormat(VSIConstants.DEC_FORMAT);
			df.setRoundingMode(RoundingMode.HALF_UP);
			// System.out.println("I am here"+df.format(exPrice));
			temp2.getChildElement("urn:ExtendedPrice").setNodeValue(df.format(exPrice));
			YFCElement temp0 = temp2.getChildElement("urn:Customer").getChildElement("urn:Destination");
			if (!YFCCommon.isVoid(strShipNode)) {
				temp0.setAttribute("locationCode", strShipNode);
			} else {
				temp0.removeAttribute("locationCode");
			}

			temp0.getChildElement("urn:StreetAddress1").setNodeValue(strAddressL1);
			temp0.getChildElement("urn:StreetAddress2").setNodeValue(strAddressL2);
			temp0.getChildElement("urn:City").setNodeValue(strCty);
			temp0.getChildElement("urn:MainDivision").setNodeValue(strStat);
			temp0.getChildElement("urn:PostalCode").setNodeValue(strZipCde);
			// not
			// showing
			// in
			// outPut
			// vEnvelope.getChildElement("urn:QuotationRequest").appendChild(temp2);
		}
		if (shippingAmount > 0) {
			String strShippingLineItem = "<urn:LineItem lineItemNumber=\"999\"> "
					+ "<urn:Product productClass=\"10000\"></urn:Product> " + "<urn:Quantity>1</urn:Quantity> "
					+ "<urn:ExtendedPrice>" + String.valueOf(shippingAmount) + "</urn:ExtendedPrice> "
					+ "<urn:Customer> " + "  <urn:Destination> " + "  <urn:StreetAddress1>" + strAddressL1
					+ "</urn:StreetAddress1> " + " <urn:StreetAddress2>" + strAddressL2 + "</urn:StreetAddress2> "
					+ "  <urn:City>" + strCty + "</urn:City> " + "<urn:MainDivision>" + strStat + "</urn:MainDivision> "
					+ "<urn:PostalCode>" + strZipCde + "</urn:PostalCode> " + " </urn:Destination> "
					+ " </urn:Customer> " + "</urn:LineItem> ";
			vEnvelope.getChildElement("urn:QuotationRequest").addXMLToNode(strShippingLineItem);
		}

		final int NUMBER_OF_RETRIES = 3;
		final long timeToWait = 500;
		boolean isFirstCallMade = true;
		boolean isSuccess = false;

		for (int i = 0; i < NUMBER_OF_RETRIES; i++) {

			if (log.isDebugEnabled()) {
				log.debug(XMLUtil.getXMLString(vertexTaxInput.getDocument()));
			}
			try {
				// System.out.println("vertex
				// input"+SCXmlUtil.getString(vertexTaxInput.getDocument()));
				//log.info("InvokeTaxWebService Request will be stored in DB");
				//VSIUtils.invokeService(env, "VSIVertaxWebService_DB", vertexTaxInput.getDocument());
				taxOut = InvokeWebservice.invokeTaxWebService(1, vertexTaxInput.getDocument());
				log.info("InvokeTaxWebService Request is stored in DB");
				//log.info("InvokeTaxWebService Response will be stored in DB");
				//VSIUtils.invokeService(env, "VSIVertaxWebService_DB", taxOut);				
				//log.info("InvokeTaxWebService Response is stored in DB");
				if (log.isDebugEnabled()) {
					log.debug(XMLUtil.getXMLString(taxOut));
					log.debug("vertex response" + SCXmlUtil.getString(taxOut));
				}
				if (taxOut != null)
					isSuccess = true;
				break;
			} catch (SocketTimeoutException e) {
				e.printStackTrace();
				log.error(e);
				isFirstCallMade = false;
			} catch (Exception e) {
				e.printStackTrace();
				log.error(e);
				isFirstCallMade = false;
			}

			if (isFirstCallMade == false) {
				try {
					//log.info("InvokeTaxWebService Request will be stored in DB");
					//VSIUtils.invokeService(env, "VSIVertaxWebService_DB", vertexTaxInput.getDocument());
					taxOut = InvokeWebservice.invokeTaxWebService(2, vertexTaxInput.getDocument());
					//log.info("InvokeTaxWebService Request is stored in DB");
					//log.info("InvokeTaxWebService Response will be stored in DB");
					//VSIUtils.invokeService(env, "VSIVertaxWebService_DB", taxOut);				
					//log.info("InvokeTaxWebService Response is stored in DB");
					if (log.isDebugEnabled()) {
						log.debug(XMLUtil.getXMLString(taxOut));
					}
					if (taxOut != null)
						isSuccess = true;
				} catch (Exception e) {
					e.printStackTrace();
					log.error(e);
					try {
						Thread.sleep(timeToWait);
					} catch (InterruptedException ie) {
					}
				}
			}

		}
		YFCElement extn1 = YFCDocument.getDocumentFor(inXML).getDocumentElement().getChildElement("Extn");
		if (isSuccess == false) {
			extn1.setAttribute("ExtnVertexEngine", "null");
			extn1.setAttribute("ExtnIsTaxCalculated", "N");
			return inXML;

		}
		if (isSuccess == true) {
			extn1.setAttribute("ExtnIsTaxCalculated", "Y");
			if (isFirstCallMade == true)
				extn1.setAttribute("ExtnVertexEngine", "vertex-O");
			if (isFirstCallMade == false)
				extn1.setAttribute("ExtnVertexEngine", "vertex-lite");
		}
		/*
		 * if(isSuccess==false) return inXML;
		 */

		YFCIterable<YFCElement> orderLineItr2 = YFCDocument.getDocumentFor(inXML).getDocumentElement()
				.getChildElement("OrderLines").getChildren("OrderLine");

		// under each orderline, there is multiple "tax" elements.
		// OMS-1201:updating the namespace from S to soapenv
		YFCIterable<YFCElement> lineItemItr = YFCDocument.getDocumentFor(taxOut).getDocumentElement()
				.getChildElement("soapenv:Body").getChildElement("VertexEnvelope").getChildElement("QuotationResponse")
				.getChildren("LineItem");
		String shippingTax = "";
		String shippingTaxPercentage = "";
		float shipTaxPer = 0;
		while (lineItemItr.hasNext()) {
			YFCElement lineItem = lineItemItr.next();
			String strlineItemNo = lineItem.getAttribute("lineItemNumber");
			if (strlineItemNo.equals("999")) {
				// Modified to account for multiple shipping taxes : BEGIN
				shippingTax = lineItem.getChildElement(VSIConstants.ATTR_TOTAL_TAX).getNodeValue();
				YFCIterable<YFCElement> itrShippingTaxes = lineItem.getChildren(VSIConstants.ELE_TAXES);
				while (itrShippingTaxes.hasNext()) {
					YFCElement eleShippingTaxes = itrShippingTaxes.next();
					double dCalcTax = Double.parseDouble(
							eleShippingTaxes.getChildElement(VSIConstants.ELE_CALCULATED_TAX).getNodeValue());
					if (!YFCObject.isVoid(dCalcTax)) {
						shippingTaxPercentage = eleShippingTaxes.getChildElement(VSIConstants.ELE_EFFECTIVE_RATE)
								.getNodeValue();
						float taxP = Float.parseFloat(shippingTaxPercentage);
						shipTaxPer = shipTaxPer + (taxP * 100);
					}
				}
				/*
				 * YFCElement shippingTaxes = lineItem.getChildElement("Taxes");
				 * if(!YFCObject.isVoid(shippingTaxes)){ YFCElement eleCalculatedTax =
				 * shippingTaxes.getChildElement("CalculatedTax");
				 * if(!YFCObject.isVoid(eleCalculatedTax)){ shippingTax =
				 * eleCalculatedTax.getNodeValue(); shippingTaxPercentage =
				 * shippingTaxes.getChildElement("EffectiveRate") .getNodeValue(); float taxP =
				 * Float.parseFloat(shippingTaxPercentage); shipTaxPer = (taxP * 100);
				 * System.out.println("shipping tax percentage"+shipTaxPer); } }
				 */
				// Modified to account for multiple shipping taxes : END
				lineItem.getParentElement().removeChild(lineItem);
			}
		}

		// System.out.println(XMLUtil.getXMLString(taxOut));
		// OMS-1201:updating the namespace from S to soapenv
		lineItemItr = YFCDocument.getDocumentFor(taxOut).getDocumentElement().getChildElement("soapenv:Body")
				.getChildElement("VertexEnvelope").getChildElement("QuotationResponse").getChildren("LineItem");

		String strZoneID = "";
		String strRuleID = "";
		String strProductClass = "";
		while (orderLineItr2.hasNext() && lineItemItr.hasNext()) {
			hasTaxType = false;
			YFCElement orderLine = orderLineItr2.next();
			YFCElement lineItem = lineItemItr.next();
			String orderQty = orderLine.getAttribute("OrderedQty");

			if (orderQty.equalsIgnoreCase("0.00") || orderQty.equalsIgnoreCase("0")) {

				YFCElement eleLineTaxes = orderLine.getChildElement("LineTaxes", true);
				YFCElement eleLineTax = eleLineTaxes.getChildElement("LineTax", true);

				String strTax = eleLineTax.getAttribute("TaxName");
				if (YFCObject.isVoid(strTax)) {
					eleLineTax.setAttribute("TaxName", "Sales Tax");
					eleLineTax.setAttribute("Tax", "0.0");
					eleLineTax.setAttribute("TaxPercentage", "0.00");
					eleLineTax.setAttribute("TaxableFlag", "N");
				}
			}

			else {

				String taxID = null;
				if (lineItem != null && lineItem.getParentElement().getChildElement("Customer") != null
						&& lineItem.getParentElement().getChildElement("Customer")
								.getChildElement("Destination") != null
						&& lineItem.getParentElement().getChildElement("Customer").getChildElement("Destination")
								.hasAttribute("taxAreaId")) {
					taxID = lineItem.getParentElement().getChildElement("Customer").getChildElement("Destination")
							.getAttribute("taxAreaId");
				}else if(lineItem != null && lineItem.getChildElement("Customer") != null && lineItem.getChildElement("Customer")
						.getChildElement("Destination") != null && lineItem.getChildElement("Customer").getChildElement("Destination")
								.hasAttribute("taxAreaId")) {
					taxID = lineItem.getChildElement("Customer").getChildElement("Destination")
							.getAttribute("taxAreaId");	
				}
				String taxType = lineItem.getChildElement("Taxes").getAttribute("taxType");
				strProductClass = lineItem.getChildElement("Product").getAttribute("productClass");
				if (deliveryMethod.equals("SHP")) {
					if (YFCObject.isVoid(strZoneID) || YFCObject.isVoid(strRuleID)) {
						String strTaxRateCode = "0";
						if (!YFCCommon.isVoid(taxType)) {

							if (taxType.equals("Standard")) {
								strTaxRateCode = "1";
							} else if (taxType.equals("Alternate")) {
								strTaxRateCode = "2";
							}
						}
						VSITobTaxJurisdiction tob = new VSITobTaxJurisdiction();
						Document tobDetailsXML = tob.fetchTobDetails(env, "", strTaxRateCode, strZipCode,
								"SHIP_TO_HOME", api);
						if (!YFCCommon.isVoid(tobDetailsXML)) {

							Element eleTobTaxJurisdictionList = tobDetailsXML.getDocumentElement();
							if (!YFCCommon.isVoid(eleTobTaxJurisdictionList)) {
								NodeList nlTobTaxJurisdiction = eleTobTaxJurisdictionList
										.getElementsByTagName("TobTaxJurisdiction");
								if (nlTobTaxJurisdiction.getLength() > 0) {
									Element eleTobTaxJurisdiction = (Element) nlTobTaxJurisdiction.item(0);
									strZoneID = eleTobTaxJurisdiction.getAttribute("TaxZoneId");
									strRuleID = eleTobTaxJurisdiction.getAttribute("TaxRuleId");
									if (log.isDebugEnabled()) {
										log.debug("TaxZoneId: " + strZoneID);
										log.debug("TaxRuleID: " + strRuleID);
									}
								}
							}
						}
					}
				}
				// System.out.print("TaxAreadID is:" + taxID);
				// System.out.print("TaxType is:" + taxType);

				String lineqty = lineItem.getChildElement("Quantity", true).getNodeValue();

				String tottax = lineItem.getChildElement("TotalTax").getNodeValue();
				orderLine.getChildElement("LineOverallTotals").setAttribute("Tax", tottax);

				YFCIterable<YFCElement> taxesItr = lineItem.getChildren("Taxes");
				YFCIterable<YFCElement> lineTaxItr = orderLine.getChildElement("LineTaxes").getChildren("LineTax");

				HashMap<String, YFCElement> map = new HashMap<String, YFCElement>();

				while (taxesItr.hasNext()/* && lineTaxItr.hasNext() */) {
					YFCElement taxes = taxesItr.next();
					YFCElement eleJurisdiction = taxes.getChildElement("Jurisdiction");
					String strCounty = "";
					if (eleJurisdiction.getAttribute("jurisdictionLevel").equals("COUNTY")) {
						strCounty = eleJurisdiction.getNodeValue();
					}

					if (taxes.getAttribute("taxResult").equals("TAXABLE")
							|| taxes.getAttribute("taxResult").equals("EXEMPT")
							|| taxes.getAttribute("taxResult").equals("NONTAXABLE")) {
						String tax = taxes.getChildElement("CalculatedTax").getNodeValue();
						String taxPercentage = taxes.getChildElement("EffectiveRate").getNodeValue();
						// String taxName =
						// taxes.getChildElement("Imposition")
						// .getAttribute("impositionType");
						String taxName = "Sales Tax";
						String invoiceSummanyTxt = null;
						if (!(null == taxes.getChildElement("SummaryInvoiceText"))) {
							String invTax = taxes.getChildElement("SummaryInvoiceText").getNodeValue();
							// System.out.print("SummaryInvoiceText" + invoiceSummanyTxt);
							if (null != taxes.getChildElement("SummaryInvoiceText").getNodeValue()) {
								Flag = true;
								invoiceSummanyTxt = taxes.getChildElement("SummaryInvoiceText").getNodeValue();
								/*
								 * System.out.print("SummaryInvoiceText in IF" + invoiceSummanyTxt);
								 */

							}

						} else
							Flag = false;

						// System.out.print("SummaryInvoiceText 1 " +
						// invoiceSummanyTxt);
						boolean isDuplicate = false;
						{
							if (map.containsKey(taxName))
								isDuplicate = true;

						}
						float taxP = Float.parseFloat(taxPercentage);
						float calTax = Float.parseFloat(tax);

						try {
							calQty = Float.parseFloat(lineqty);
						} catch (NumberFormatException e) {
						}

						float taxPer = (taxP * 100);
						float taxperunit = (calTax / calQty);
						DecimalFormat df = new DecimalFormat(VSIConstants.DEC_FORMAT);
						df.setRoundingMode(RoundingMode.DOWN);
						DecimalFormat dfRMUp = new DecimalFormat(VSIConstants.DEC_FORMAT);
						// System.out.print("Taxperunit:" + taxperunit);
						String taxpunit = df.format(taxperunit);

						Double dbTaxPerLine = calTax - ((Double.valueOf(taxpunit)) * calQty);

						if (dbTaxPerLine > 0) {
							dbTaxPerLine = Double.parseDouble(dfRMUp.format(dbTaxPerLine));
						} else {
							dbTaxPerLine = 0.00;
						}

						// System.out.print(" Taxperunit Upto 2 decimal:" + taxpunit);
						/**
						 * Start Block to merge lineTax items based on name
						 */

						YFCElement lineTax1 = null;
						if (!isDuplicate) {

							if (lineTaxItr.hasNext()) {

								lineTax1 = lineTaxItr.next();
								YFCElement extn = lineTax1.getChildElement("Extn");
								lineTax1.setAttribute("Tax", calTax);
								lineTax1.setAttribute("TaxName", taxName);
								lineTax1.setAttribute("TaxPercentage", taxPer);
								extn.setAttribute("ExtnTaxPerUnit", taxpunit);
								extn.setAttribute("ExtnRemTaxPerLine", dbTaxPerLine);
								extn.setAttribute("ExtnTaxAreaID", taxID);
								extn.setAttribute("ExtnCounty", strCounty);
								extn.setAttribute("ZoneId", strZoneID);
								extn.setAttribute("RuleId", strRuleID);
								extn.setAttribute("GroupId", strProductClass);
								setFlag(Flag, extn, lineTax1, orderLine, invoiceSummanyTxt, false);
							}

							else {
								lineTax1 = orderLine.getChildElement("LineTaxes").createChild("LineTax");
								YFCElement extn = lineTax1.createChild("Extn");
								lineTax1.setAttribute("Tax", tax);
								lineTax1.setAttribute("TaxName", taxName);
								lineTax1.setAttribute("TaxPercentage", taxPer);
								extn.setAttribute("ExtnTaxPerUnit", taxpunit);
								extn.setAttribute("ExtnRemTaxPerLine", dbTaxPerLine);
								extn.setAttribute("ExtnTaxAreaID", taxID);
								extn.setAttribute("ExtnCounty", strCounty);
								extn.setAttribute("ZoneId", strZoneID);
								extn.setAttribute("RuleId", strRuleID);
								extn.setAttribute("GroupId", strProductClass);
								setFlag(Flag, extn, lineTax1, orderLine, invoiceSummanyTxt, false);

							}
							map.put(taxName, lineTax1);
						} else {
							lineTax1 = map.get(taxName);

							calTax = (Float.parseFloat(tax) + Float.parseFloat(lineTax1.getAttribute("Tax")));
							taxPer = (taxP * 100 + Float.parseFloat(lineTax1.getAttribute("TaxPercentage")));
							taxperunit = (calTax / calQty);

							DecimalFormat df1 = new DecimalFormat(VSIConstants.DEC_FORMAT);
							df.setRoundingMode(RoundingMode.DOWN);
							DecimalFormat dfRMUp1 = new DecimalFormat(VSIConstants.DEC_FORMAT);
							// System.out.print("Taxperunit:" + taxperunit);
							String taxpunit1 = df.format(taxperunit);

							Double dbTaxPerLine1 = calTax - ((Double.valueOf(taxpunit1)) * calQty);

							if (dbTaxPerLine1 > 0) {
								dbTaxPerLine1 = Double.parseDouble(dfRMUp.format(dbTaxPerLine1));
							} else {
								dbTaxPerLine1 = 0.00;
							}

							YFCElement extn = lineTax1.getChildElement("Extn");
							lineTax1.setAttribute("Tax", calTax);
							lineTax1.setAttribute("TaxName", taxName);
							lineTax1.setAttribute("TaxPercentage", taxPer);
							extn.setAttribute("ExtnTaxPerUnit", taxpunit1);
							extn.setAttribute("ExtnRemTaxPerLine", dbTaxPerLine1);
							extn.setAttribute("ExtnTaxAreaID", taxID);
							extn.setAttribute("ZoneId", strZoneID);
							extn.setAttribute("RuleId", strRuleID);
							extn.setAttribute("GroupId", strProductClass);
							setFlag(Flag, extn, lineTax1, orderLine, invoiceSummanyTxt, true);
						}

						// //System.out.println("lineItemItr: " + lineTaxItr.hasNext());
						// //System.out.println("dumping transit inXML: \n"+
						// inXML.getDocumentElement().toString());
						// //System.out.println(orderLine.toString());
					}
				}

				while (lineTaxItr.hasNext()) {
					orderLine.getChildElement("LineTaxes").removeChild(lineTaxItr.next());
				}

				lineTaxItr = null;
				orderLine = null;

			}

			/*
			 * Now handle the repeated data extract from
			 */

			// return inXML;

		}

		if (!YFCObject.isVoid(shippingTax)) {
			YFCElement eleHeaderTaxes = docEInXML.getChildElement("HeaderTaxes");
			if (!YFCObject.isVoid(eleHeaderTaxes)) {
				docEInXML.removeChild(eleHeaderTaxes);
			}
			docEInXML.createChild("HeaderTaxes");
			eleHeaderTaxes = docEInXML.getChildElement("HeaderTaxes");
			eleHeaderTaxes.createChild("HeaderTax");
			YFCElement eleHeaderTaxe = eleHeaderTaxes.getChildElement("HeaderTax");
			eleHeaderTaxe.setAttribute("ChargeCategory", "Shipping");
			eleHeaderTaxe.setAttribute("ChargeName", "Shipping");
			eleHeaderTaxe.setAttribute("Tax", shippingTax);
			eleHeaderTaxe.setAttribute("TaxPercentage", shipTaxPer);
			eleHeaderTaxe.setAttribute("TaxName", "Shipping Tax");
		}
		// reset header tax in case it's not applicable anymore
		else {
			YFCElement eleHeaderTaxes = docEInXML.getChildElement("HeaderTaxes");
			if (!YFCCommon.isVoid(eleHeaderTaxes)) {
			YFCElement eleHeaderTax = eleHeaderTaxes.getChildElement("HeaderTax");
			if (!YFCCommon.isVoid(eleHeaderTax)) {
				eleHeaderTax.setAttribute("ChargeCategory", "Shipping");
				eleHeaderTax.setAttribute("ChargeName", "Shipping");
				eleHeaderTax.setAttribute("Tax", 0);
				eleHeaderTax.setAttribute("TaxName", "Shipping Tax");
			}
			}
		}

		return inXML;
	}

	public void setFlag(boolean flag, YFCElement extn, YFCElement lineTax1, YFCElement orderLine,
			String invoiceSummanyTxt, boolean skipFalse) {
		if (flag == true && hasTaxType == false) {
			if (invoiceSummanyTxt.equalsIgnoreCase("N")) {
				extn.setAttribute("TaxType", "NonTaxable");
				lineTax1.setAttribute("TaxableFlag", "N");
				orderLine.getChildElement("LinePriceInfo").setAttribute("TaxableFlag", "N");
			} else if (invoiceSummanyTxt.equalsIgnoreCase("A")) {
				extn.setAttribute("TaxType", "Alternate");
				lineTax1.setAttribute("TaxableFlag", "Y");
				orderLine.getChildElement("LinePriceInfo").setAttribute("TaxableFlag", "Y");
				hasTaxType = true;
			} else if (invoiceSummanyTxt.equalsIgnoreCase("S")) {
				extn.setAttribute("TaxType", "Standard");
				lineTax1.setAttribute("TaxableFlag", "Y");
				orderLine.getChildElement("LinePriceInfo").setAttribute("TaxableFlag", "Y");
				hasTaxType = true;
			}
		}
		if (flag == false && skipFalse == false && hasTaxType == false) {
			extn.setAttribute("TaxType", "Alternate");
			lineTax1.setAttribute("TaxableFlag", "Y");
			orderLine.getChildElement("LinePriceInfo").setAttribute("TaxableFlag", "Y");
			hasTaxType = true;
			// System.out.print("In case of SummaryInvoice not sent:NEHA");
		}

	}

	public boolean checkElements(Document inXML) {

		if (inXML == null)
			return false;
		YFCElement docEInXML = YFCDocument.getDocumentFor(inXML).getDocumentElement();

		String taxExemptFlag = docEInXML.getAttribute("TaxExemptFlag");

		YFCElement eleOrderShipTo = docEInXML.getChildElement("PersonInfoShipTo");

		YFCElement eOrderLines = YFCDocument.getDocumentFor(inXML).getDocumentElement().getChildElement("OrderLines");
		if (eOrderLines == null)
			return false;
		YFCElement eOrderLine = null;
		YFCIterable<YFCElement> itrOrderlines = eOrderLines.getChildren("OrderLine");

		while (itrOrderlines.hasNext()) {
			eOrderLine = itrOrderlines.next();
			if (null == eOrderLine
					|| (null == eOrderLine.getChildElement("PersonInfoShipTo") && YFCObject.isVoid(eleOrderShipTo))
					|| null == eOrderLine.getChildElement("LineOverallTotals")
					|| null == eOrderLine.getChildElement("Item") || null == eOrderLine.getAttribute("OrderedQty")// ||
																													// taxExemptFlag.equals("Y")
//					|| eOrderLine.getAttribute("OrderedQty").equalsIgnoreCase("0.00") 
//					|| eOrderLine.getAttribute("OrderedQty").equalsIgnoreCase("0")
//					|| eOrderLine.getAttribute("OrderedQty").equalsIgnoreCase("")
			) {
				return false;
			}

		}
		return true;
	}

	@Override
	public void setProperties(Properties arg0) throws Exception {
		// TODO Auto-generated method stub

	}

}