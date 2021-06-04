package com.vsi.oms.api.order;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.HashSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.vsi.oms.utils.VSIConstants;
import com.vsi.oms.utils.VSIUtils;
import com.vsi.oms.utils.XMLUtil;
import com.yantra.interop.japi.YIFClientCreationException;
import com.yantra.yfc.core.YFCObject;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.core.YFSSystem;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;

public class VSISendJDACnclMsg implements VSIConstants {
	
	private YFCLogCategory log = YFCLogCategory.instance(VSISendJDACnclMsg.class);
	//MixedCart Changes -- Start
	private static final String TAG = VSISendJDACnclMsg.class.getSimpleName();
	//MixedCart Changes -- End
		
	public void sendJDACnclMsg(YFSEnvironment env, Document docInput) throws YFSException, RemoteException, ParserConfigurationException, YIFClientCreationException{
		log.beginTimer("VSISendJDACnclMsg.sendJDACnclMsg() : START");
		//MixedCart Changes -- Start
		printLogs("================Inside VSISendJDACnclMsg Class and sendJDACnclMsg Method================");
		//MixedCart Changes -- End
		if(log.isDebugEnabled()){
			log.debug("Input document:\n" + SCXmlUtil.getString(docInput));
		}
		Document docJDACnclMsg = (Document) env.getTxnObject(JDA_CNCL_MSG);
		if (!YFCObject.isVoid(docJDACnclMsg)) {
			if(log.isDebugEnabled()){
				log.debug("\nIncoming JDA Cancel Message: " + SCXmlUtil.getString(docJDACnclMsg));
			}
			
			Element eleOrder = docInput.getDocumentElement();
			Element elePriceInfo = SCXmlUtil.getChildElement(eleOrder, VSIConstants.ELE_PRICE_INFO);
			double dCustCredit = Math.abs(SCXmlUtil.getDoubleAttribute(elePriceInfo, VSIConstants.ATTR_CHANGE_IN_TOTAL_AMOUNT));
			double dNewOrderTotal = SCXmlUtil.getDoubleAttribute(elePriceInfo, VSIConstants.ATTR_TOTAL_AMOUNT);
			double dOriginalPrice = dCustCredit + dNewOrderTotal;
			
			Element eleJDACnclMsg = SCXmlUtil.getChildElement(docJDACnclMsg.getDocumentElement(), ELE_ORDER);
			Element eleJDAPriceInfo = SCXmlUtil.getChildElement(eleJDACnclMsg, ELE_PRICE_INFO);
			SCXmlUtil.setAttribute(eleJDAPriceInfo, VSIConstants.ATTR_PREVIOUS_ORDER_TOTAL, dOriginalPrice);
			SCXmlUtil.setAttribute(eleJDAPriceInfo, VSIConstants.ATTR_NEW_ORDER_TOTAL, dNewOrderTotal);
			SCXmlUtil.setAttribute(eleJDAPriceInfo, VSIConstants.ATTR_CUSTOMER_CREDIT, dCustCredit);
			
			try {
				VSIUtils.invokeService(env, VSI_CNCL_PUBLISH_TO_JDA, docJDACnclMsg);
			} catch (Exception e) {
				log.error("Error while invoking VSICancelPublishToJDA from VSISendJDACnclMsg.sendJDACnclMsg()", e);
				throw VSIUtils.getYFSException(e);
			}
		}
		
		//MixedCart Changes -- Start
		printLogs("JDA BOPUS Cancel Allocation Webservice Call Input will be prepared");
		HashSet<String> setShipNode = new HashSet<String>();
		try {
			Element eleInput=docInput.getDocumentElement();
			String strOrderHeaderKey=eleInput.getAttribute(VSIConstants.ATTR_ORDER_HEADER_KEY);
			String strOrderNo=eleInput.getAttribute(VSIConstants.ATTR_ORDER_NO);
			String strOrderDate=eleInput.getAttribute(VSIConstants.ATTR_ORDER_DATE);
			Element eleOrdLines=SCXmlUtil.getChildElement(eleInput, VSIConstants.ELE_ORDER_LINES);
			NodeList nlOrdLine=eleOrdLines.getElementsByTagName(VSIConstants.ELE_ORDER_LINE);
			for(int i=0; i<nlOrdLine.getLength(); i++){
				Element eleOrdLine=(Element)nlOrdLine.item(i);
				String strLineType=eleOrdLine.getAttribute(VSIConstants.ATTR_LINE_TYPE);
				printLogs("Line Type is: "+strLineType);
				if(VSIConstants.LINETYPE_PUS.equals(strLineType) || VSIConstants.LINETYPE_STS.equals(strLineType)){
					String shipNode=eleOrdLine.getAttribute(VSIConstants.ATTR_SHIP_NODE);
					setShipNode.add(shipNode);
				}						
			}
			printLogs("Ship Node HashSet Contents after processing all lines: "+Arrays.toString(setShipNode.toArray()));
			
			for(String shpNd:setShipNode){
				printLogs("Ship Node being processed: "+shpNd);
				
				String strBOPUSOrderNo=null;
				String strBOPUSStatusDate=null;
				String strSTSOrderNo=null;
				String strSTSStatusDate=null;
				
				Document docBOPUSJDARequest=XMLUtil.createDocument(VSIConstants.ELE_ORDER);
				Element eleBOPUSJDARequest=docBOPUSJDARequest.getDocumentElement();
				
				putElementValue(eleBOPUSJDARequest,"OrderDate", strOrderDate);
				Element eleBOPUSOrderLines=XMLUtil.createElement(docBOPUSJDARequest, VSIConstants.ELE_ORDER_LINES, "");
				
				Document docSTSJDARequest=XMLUtil.createDocument(VSIConstants.ELE_ORDER);
				Element eleSTSJDARequest=docSTSJDARequest.getDocumentElement();
				
				putElementValue(eleSTSJDARequest,"OrderDate", strOrderDate);
				Element eleSTSOrderLines=XMLUtil.createElement(docSTSJDARequest, VSIConstants.ELE_ORDER_LINES, "");
				
				for(int j=0; j<nlOrdLine.getLength(); j++){
					Element eleOrderLn=(Element)nlOrdLine.item(j);
					String strLineType=eleOrderLn.getAttribute(VSIConstants.ATTR_LINE_TYPE);
					String strPrimeLineNo=eleOrderLn.getAttribute(VSIConstants.ATTR_PRIME_LINE_NO);
					printLogs("Line Type is: "+strLineType);
					if(VSIConstants.LINETYPE_PUS.equals(strLineType) || VSIConstants.LINETYPE_STS.equals(strLineType)){
						String shipNode=eleOrderLn.getAttribute(VSIConstants.ATTR_SHIP_NODE);
						printLogs("Ship Node of current Line: "+shipNode);
						if(shipNode.equals(shpNd)){
							printLogs("Ship Node Matches");		
							if(VSIConstants.LINETYPE_PUS.equals(strLineType)){
								printLogs("OrderLine with PrimeLineNo "+strPrimeLineNo+" is a BOPUS line, hence will be added to BOPUS JDA Request");
								
								if(YFCCommon.isVoid(strBOPUSOrderNo)){
									printLogs("OrderNo is not set yet");
									String strCustPONo=eleOrderLn.getAttribute(VSIConstants.ATTR_CUST_PO_NO);
									strBOPUSOrderNo=strCustPONo;
									putElementValue(eleBOPUSJDARequest,"OrderNo", strBOPUSOrderNo);
									putElementValue(eleBOPUSJDARequest,"OrderType", "BOPUS");
									printLogs("OrderNo and OrderType is set");
								}
								if(YFCCommon.isVoid(strBOPUSStatusDate)){
									printLogs("StatusDate is not set yet");
									Element eleStsBrkp=SCXmlUtil.getChildElement(eleOrderLn, VSIConstants.ELE_STATUS_BREAKUP_FOR_CANCELED_QTY);
									Element eleCncldFrm=SCXmlUtil.getChildElement(eleStsBrkp, VSIConstants.ATTR_CANCELED_FROM);;
									String strStsDt=eleCncldFrm.getAttribute("StatusDate");			
									strBOPUSStatusDate=strStsDt;
									putElementValue(eleBOPUSJDARequest,"StatusDate", strBOPUSStatusDate);
									putElementValue(eleBOPUSJDARequest,"Status", "CANCEL");
									putElementValue(eleBOPUSJDARequest,"FullfillingStore", shpNd);
									printLogs("StatusDate, Status and FullfillingStore are set");
								}
								Element eleBOPUSOrdLnOut=SCXmlUtil.createChild(eleBOPUSOrderLines, VSIConstants.ELE_ORDER_LINE);
								String strChngInOrdQty=eleOrderLn.getAttribute("ChangeInOrderedQty");								
								int iChngInOrdQty=Integer.parseInt(strChngInOrdQty);
								iChngInOrdQty=Math.abs(iChngInOrdQty);
								strChngInOrdQty=Integer.toString(iChngInOrdQty);		
								putElementValue(eleBOPUSOrdLnOut,"OrderedQty", strChngInOrdQty);
								Element eleBOPUSItemOut=SCXmlUtil.createChild(eleBOPUSOrdLnOut, VSIConstants.ELE_ITEM);
								Element eleItem=SCXmlUtil.getChildElement(eleOrderLn, VSIConstants.ELE_ITEM);
								String strItemId=eleItem.getAttribute(VSIConstants.ATTR_ITEM_ID);
								putElementValue(eleBOPUSItemOut,"ItemID", strItemId);
								
							}else if(VSIConstants.LINETYPE_STS.equals(strLineType)){
								printLogs("OrderLine with PrimeLineNo "+strPrimeLineNo+" is a STS line, hence will be added to STS JDA Request");
								
								if(YFCCommon.isVoid(strSTSOrderNo)){
									printLogs("OrderNo is not set yet");
									String strCustPONo=eleOrderLn.getAttribute(VSIConstants.ATTR_CUST_PO_NO);
									strSTSOrderNo=strCustPONo;
									putElementValue(eleSTSJDARequest,"OrderNo", strSTSOrderNo);
									putElementValue(eleSTSJDARequest,"OrderType", "BOPUS");
									printLogs("OrderNo and OrderType is set");
								}
								if(YFCCommon.isVoid(strSTSStatusDate)){
									printLogs("StatusDate is not set yet");
									Element eleStsBrkp=SCXmlUtil.getChildElement(eleOrderLn, VSIConstants.ELE_STATUS_BREAKUP_FOR_CANCELED_QTY);
									Element eleCncldFrm=SCXmlUtil.getChildElement(eleStsBrkp, VSIConstants.ATTR_CANCELED_FROM);;
									String strStsDt=eleCncldFrm.getAttribute("StatusDate");			
									strSTSStatusDate=strStsDt;
									putElementValue(eleSTSJDARequest,"StatusDate", strSTSStatusDate);
									putElementValue(eleSTSJDARequest,"Status", "CANCEL");
									putElementValue(eleSTSJDARequest,"FullfillingStore", shpNd);
									printLogs("StatusDate, Status and FullfillingStore are set");									
								}
								Element eleSTSOrdLnOut=SCXmlUtil.createChild(eleSTSOrderLines, VSIConstants.ELE_ORDER_LINE);
								String strChngInOrdQty=eleOrderLn.getAttribute("ChangeInOrderedQty");								
								int iChngInOrdQty=Integer.parseInt(strChngInOrdQty);
								iChngInOrdQty=Math.abs(iChngInOrdQty);
								strChngInOrdQty=Integer.toString(iChngInOrdQty);		
								putElementValue(eleSTSOrdLnOut,"OrderedQty", strChngInOrdQty);
								Element eleSTSItemOut=SCXmlUtil.createChild(eleSTSOrdLnOut, VSIConstants.ELE_ITEM);
								Element eleItem=SCXmlUtil.getChildElement(eleOrderLn, VSIConstants.ELE_ITEM);
								String strItemId=eleItem.getAttribute(VSIConstants.ATTR_ITEM_ID);
								putElementValue(eleSTSItemOut,"ItemID", strItemId);
								
							}
						}					
					}
				}
				
				eleBOPUSJDARequest.appendChild(eleBOPUSOrderLines);
				eleSTSJDARequest.appendChild(eleSTSOrderLines);
				
				printLogs("JDA Cancel Allocation Request Prepared for BOPUS lines is: "+SCXmlUtil.getString(docBOPUSJDARequest));
				
				printLogs("JDA Cancel Allocation Request Prepared for STS lines is: "+SCXmlUtil.getString(docSTSJDARequest));
				
				NodeList nlBOPUSOrdLn=eleBOPUSOrderLines.getElementsByTagName(VSIConstants.ELE_ORDER_LINE);
				if(nlBOPUSOrdLn.getLength()>0){
					
					printLogs("BOPUS Lines are available for shipnode "+shpNd+" hence JDA call will be invoked for BOPUS lines");
					invokeJDA(env, strOrderHeaderKey, strOrderNo,
							docBOPUSJDARequest);
				}
				
				NodeList nlSTSOrdLn=eleSTSOrderLines.getElementsByTagName(VSIConstants.ELE_ORDER_LINE);
				if(nlSTSOrdLn.getLength()>0){
					
					printLogs("STS Lines are available for shipnode "+shpNd+" hence JDA call will be invoked for STS lines");
					invokeJDA(env, strOrderHeaderKey, strOrderNo,
							docSTSJDARequest);
				}
			}

		} catch (YFSException e) {
			e.printStackTrace();
			throw new YFSException();
		} catch (Exception e){
			e.printStackTrace();
			throw new YFSException();
		}
		
		printLogs("================Exiting VSISendJDACnclMsg Class and sendJDACnclMsg Method================");
		
		//MixedCart Changes -- End
		
		log.endTimer("VSISendJDACnclMsg.sendJDACnclMsg() : END");
	}
	
	//MixedCart Changes -- Start
	private void invokeJDA(YFSEnvironment env, String strOrderHeaderKey,
			String strBOPUSOrderNo, Document docBOPUSJDARequest)
			throws Exception, UnsupportedEncodingException,
			MalformedURLException, IOException, ProtocolException,
			ParserConfigurationException, SAXException {
		
		printLogs("Inside invokeJDA Method");
		
		VSIUtils.invokeService(env, "VSIMixedCartJDACancelAllocation_DB", docBOPUSJDARequest);
		printLogs("JDA Cancel Allocation Request is posted to DB successfully");
		
		DataOutputStream wr=null;
		
		String strJDAURL= YFSSystem.getProperty("SFS_FORCED_ALLOCATION_URL");
		printLogs("JDA URL: "+strJDAURL);
		
		String reqString=XMLUtil.getXMLString(docBOPUSJDARequest);
		printLogs("JDA Request in String format: "+reqString);
		
		String strSplit[]=strJDAURL.split("\\?");
		String endPointURL=strSplit[0].concat("?");
		String strArg1=strSplit[1];
		printLogs("endPointURL is: "+endPointURL);
		printLogs("strArg1: "+strArg1);
		
		String xmlencoded = URLEncoder.encode(reqString,VSIConstants.UTF8_ENCODING);
		String strContent=strArg1+"&request="+xmlencoded;
		printLogs("xmlencoded => "+xmlencoded);
		printLogs("strContent => "+strContent);
		
		URL url = new URL(endPointURL);	
		 
		//open connection using url
		HttpURLConnection connection =  (HttpURLConnection) url.openConnection();
		
		//get web service response
		connection.setRequestMethod(VSIConstants.JDA_POST_METHOD);
		byte[] bContent = strContent.getBytes();	
		connection.setRequestProperty(VSIConstants.JDA_REQUEST_PROP, String.valueOf(bContent.length));
		connection.setDefaultUseCaches(false);
		connection.setDoOutput(true);

		Document responseDoc=null;
		printLogs("Invoking the JDA web service");
			 
		wr = new DataOutputStream (connection.getOutputStream());
		wr.write(bContent);
		printLogs("Invoked the JDA web service");
		
		if(connection.getResponseCode() == -1 || connection.getResponseMessage() == null){
			printLogs("Inside If condition when ResponseCode is -1 or ResponseMessage is null");
			alertForJDAFailure(env,strBOPUSOrderNo,strOrderHeaderKey,"VSI_MC_JDA_CANCEL_ALLOCATION_FAILURE","MixedCart JDA Cancel Allocation Failure Alert","VSI_MC_JDA_CANCEL_ALLOCATION_FAILURE");
		}
			 
		if(connection.getResponseCode() != 200)
		{
			printLogs("Inside If condition when ResponseCode not equals 200");
			printLogs(connection.getResponseCode()+" - "+connection.getResponseMessage());	
			alertForJDAFailure(env,strBOPUSOrderNo,strOrderHeaderKey,"VSI_MC_JDA_CANCEL_ALLOCATION_FAILURE",connection.getResponseCode()+" - "+connection.getResponseMessage(),"VSI_MC_JDA_CANCEL_ALLOCATION_FAILURE");
		}
		
		if(connection.getResponseCode() == 200)
		{
			printLogs("Inside If condition when ResponseCode equals 200");
			responseDoc=parseDoc(connection.getInputStream());
			printLogs("Response from JDA WebService:"+XMLUtil.getXMLString(responseDoc));
			
			VSIUtils.invokeService(env, "VSIMixedCartJDACancelAllocation_DB", responseDoc);
			printLogs("JDA BOPUS Cancel Allocation Response is posted to DB successfully");
		}
		
		printLogs("Exiting invokeJDA Method");
	}
	
	private Document parseDoc(InputStream instream) throws ParserConfigurationException, SAXException, IOException{
		
		printLogs("================Inside parseDoc Method================");
		DocumentBuilderFactory factory =DocumentBuilderFactory.newInstance();
		factory.isIgnoringElementContentWhitespace();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc=null;
	 	if(instream.markSupported()){
	 		instream.mark(Integer.MAX_VALUE);
	 		BufferedReader buff_read = new BufferedReader(new InputStreamReader(instream,"UTF-8"));
		    String  inputLine = null;
	
		 while((inputLine = buff_read.readLine())!= null){
			 printLogs(inputLine);			 
		    }  
		    instream.reset();
	 	}
	 	doc=builder.parse(instream);
	 	printLogs("JDA WebService Response being Parsed"+instream);
	 	
	 	printLogs("================Exiting parseDoc Method================");
	 	return doc;
  }
	
	public void alertForJDAFailure(YFSEnvironment env, String orderNo, String orderHeaderKey, String exceptionType, String exceptionDescription, String queueId)
	{
		printLogs("================Inside alertForJDAFailure Method================");
		
		try
		{
			//Creation of Alert
			Document createExceptionDoc = SCXmlUtil.createDocument(VSIConstants.ELE_INBOX);
			Element eleInbox = createExceptionDoc.getDocumentElement();
			eleInbox.setAttribute(VSIConstants.ATTR_ORDER_HEADER_KEY, orderHeaderKey);		
			eleInbox.setAttribute(VSIConstants.ATTR_ORDER_NO, orderNo);
			eleInbox.setAttribute(VSIConstants.ATTR_EXCEPTION_TYPE, exceptionType);
			eleInbox.setAttribute(VSIConstants.ATTR_DETAIL_DESCRIPTION, exceptionDescription);
			eleInbox.setAttribute(VSIConstants.ATTR_QUEUE_ID, queueId);
			eleInbox.setAttribute(VSIConstants.ATTR_CONSOLIDATE,VSIConstants.FLAG_Y);
			eleInbox.setAttribute(VSIConstants.ATTR_CONS_WINDOW,VSIConstants.VAL_FOREVER);
			Element eleConsolidationTemplate = createExceptionDoc.createElement(VSIConstants.ELE_CONSOLIDATE_TEMPLATE);
			eleInbox.appendChild(eleConsolidationTemplate);
			Element eleInboxCpy = (Element) eleInbox.cloneNode(true);
			eleConsolidationTemplate.appendChild(eleInboxCpy);
			
			printLogs("createException API Input XML :"+SCXmlUtil.getString(createExceptionDoc));
			VSIUtils.invokeAPI(env, VSIConstants.API_CREATE_EXCEPTION,createExceptionDoc);
			printLogs("createException API invoked successfully");
		
		}
		catch(Exception e)
		{
			printLogs("alertForJDAFailure - in catch block => ");
			e.printStackTrace();
		}
		
		printLogs("================Exiting alertForJDAFailure Method================");
	}
	
/*	private void prepareJDARequest(String shpNd, String strBOPUSOrderNo,
			String strBOPUSStatusDate, Element eleBOPUSJDARequest,
			Element eleBOPUSOrderLines, Element eleOrderLn) {
		
		printLogs("Inside prepareJDARequest Method");
		
		if(YFCCommon.isVoid(strBOPUSOrderNo)){
			printLogs("OrderNo is not set yet");
			String strCustPONo=eleOrderLn.getAttribute(VSIConstants.ATTR_CUST_PO_NO);
			strBOPUSOrderNo=strCustPONo;
			putElementValue(eleBOPUSJDARequest,"OrderNo", strBOPUSOrderNo);
			putElementValue(eleBOPUSJDARequest,"OrderType", "BOPUS");
			printLogs("OrderNo and OrderType is set");
		}
		if(YFCCommon.isVoid(strBOPUSStatusDate)){
			printLogs("StatusDate is not set yet");			
			Element eleStsBrkp=SCXmlUtil.getChildElement(eleOrderLn, VSIConstants.ELE_STATUS_BREAKUP_FOR_CANCELED_QTY);
			Element eleCncldFrm=(Element)eleStsBrkp.getElementsByTagName(VSIConstants.ATTR_CANCELED_FROM).item(0);
			String strStsDt=eleCncldFrm.getAttribute("StatusDate");			
			strBOPUSStatusDate=strStsDt;
			putElementValue(eleBOPUSJDARequest,"StatusDate", strBOPUSStatusDate);
			putElementValue(eleBOPUSJDARequest,"Status", "CANCEL");
			putElementValue(eleBOPUSJDARequest,"FullfillingStore", shpNd);
			printLogs("StatusDate, Status and FullfillingStore are set");
		}
		Element eleBOPUSOrdLnOut=SCXmlUtil.createChild(eleBOPUSOrderLines, VSIConstants.ELE_ORDER_LINE);
		String strChngInOrdQty=eleOrderLn.getAttribute("ChangeInOrderedQty");
		int iChngInOrdQty=Integer.parseInt(strChngInOrdQty);
		iChngInOrdQty=Math.abs(iChngInOrdQty);
		strChngInOrdQty=Integer.toString(iChngInOrdQty);		
		putElementValue(eleBOPUSOrdLnOut,"OrderedQty", strChngInOrdQty);
		Element eleBOPUSItemOut=SCXmlUtil.createChild(eleBOPUSOrdLnOut, VSIConstants.ELE_ITEM);
		Element eleItem=SCXmlUtil.getChildElement(eleOrderLn, VSIConstants.ELE_ITEM);
		String strItemId=eleItem.getAttribute(VSIConstants.ATTR_ITEM_ID);
		putElementValue(eleBOPUSItemOut,"ItemID", strItemId);
		
		printLogs("Exiting prepareJDARequest Method");
	}*/
	
	private void putElementValue(Element childEle, String key, Object value) {
		Element ele = SCXmlUtil.createChild(childEle, key);
		if(value instanceof String ) {
			ele.setTextContent((String)value);
		}else if(value instanceof Element ) {
			ele.appendChild((Element)value);
		}
	}
	
	private void printLogs(String mesg) {
		if(log.isDebugEnabled()){
			log.debug(TAG +" : "+mesg);
		}
	}
	//MixedCart Changes -- End
	
	public Document sendJDACnclMsgForPartialCancellation(YFSEnvironment env, Document docInput) throws YFSException, RemoteException, ParserConfigurationException, YIFClientCreationException{
		log.beginTimer("VSISendJDACnclMsg.sendJDACnclMsgForPartialCancellation() : START");
		//MixedCart Changes -- Start
		printLogs("================Inside sendJDACnclMsgForPartialCancellation Class and sendJDACnclMsgForPartialCancellation Method================");
		//MixedCart Changes -- End
		if(log.isDebugEnabled()){
			log.debug("Input document:\n" + SCXmlUtil.getString(docInput));
		}
		Document docJDACnclMsg = (Document) env.getTxnObject(JDA_CNCL_MSG);
		log.debug("\nIncoming JDA Cancel Message: " + SCXmlUtil.getString(docJDACnclMsg));

		String isComCancellation =  (String) env.getTxnObject("IsCOMPartialCancellation");
		log.debug("isComCancellation ::::" + isComCancellation);
		if (!YFCObject.isVoid(docJDACnclMsg) && !YFCObject.isVoid(isComCancellation) && isComCancellation.equalsIgnoreCase("Y")) {
			if(log.isDebugEnabled()){
				log.debug("\nIncoming JDA Cancel Message: " + SCXmlUtil.getString(docJDACnclMsg));
			}
			
			Element eleOrder = docInput.getDocumentElement();
			Element elePriceInfo = SCXmlUtil.getChildElement(eleOrder, VSIConstants.ELE_PRICE_INFO);
			double dCustCredit = Math.abs(SCXmlUtil.getDoubleAttribute(elePriceInfo, VSIConstants.ATTR_CHANGE_IN_TOTAL_AMOUNT));
			double dNewOrderTotal = SCXmlUtil.getDoubleAttribute(elePriceInfo, VSIConstants.ATTR_TOTAL_AMOUNT);
			double dOriginalPrice = dCustCredit + dNewOrderTotal;
			
			Element eleJDACnclMsg = SCXmlUtil.getChildElement(docJDACnclMsg.getDocumentElement(), ELE_ORDER);
			Element eleJDAPriceInfo = SCXmlUtil.getChildElement(eleJDACnclMsg, ELE_PRICE_INFO);
			SCXmlUtil.setAttribute(eleJDAPriceInfo, VSIConstants.ATTR_PREVIOUS_ORDER_TOTAL, dOriginalPrice);
			SCXmlUtil.setAttribute(eleJDAPriceInfo, VSIConstants.ATTR_NEW_ORDER_TOTAL, dNewOrderTotal);
			SCXmlUtil.setAttribute(eleJDAPriceInfo, VSIConstants.ATTR_CUSTOMER_CREDIT, dCustCredit);
			
			try {
				VSIUtils.invokeService(env, VSI_CNCL_PUBLISH_TO_JDA, docJDACnclMsg);
			} catch (Exception e) {
				log.error("Error while invoking VSICancelPublishToJDA from VSISendJDACnclMsg.sendJDACnclMsg()", e);
				throw VSIUtils.getYFSException(e);
			}
		}
		return docInput;
	
	}
}
