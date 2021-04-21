package com.vsi.oms.api.mixedcart;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

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
import com.yantra.interop.japi.YIFApi;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfs.core.YFSSystem;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;

public class VSIMixedCartJDAAllocation {
	
	private static YFCLogCategory log = YFCLogCategory.instance(VSIMixedCartJDAAllocation.class);
	private static final String TAG = VSIMixedCartJDAAllocation.class.getSimpleName();
	YIFApi api;
	
	public Document mixedCartJDAAllocation(YFSEnvironment env, Document inXML){
		
		printLogs("================Inside VSIMixedCartJDAAllocation Class and mixedCartJDAAllocation Method================");
		printLogs("Printing Input XML :"+SCXmlUtil.getString(inXML));
		
		try{
			
			Element eleRelease = inXML.getDocumentElement();
			Element eleOrderLn=(Element)eleRelease.getElementsByTagName(VSIConstants.ELE_ORDER_LINE).item(0);
			String strLineType=eleOrderLn.getAttribute(VSIConstants.ATTR_LINE_TYPE);
						
			if(VSIConstants.LINETYPE_PUS.equals(strLineType)){
				
				printLogs("Release contains PICK_IN_STORE lines and hence will be processed");
			
				Document docJDARequest=XMLUtil.createDocument(VSIConstants.ELE_ORDER);
				Element eleJDARequest=docJDARequest.getDocumentElement();
				
				Element eleOrder=SCXmlUtil.getChildElement(eleRelease, VSIConstants.ELE_ORDER);
				String strOrderNo=eleOrder.getAttribute(VSIConstants.ATTR_ORDER_NO);
				String strOrderDate=eleRelease.getAttribute(VSIConstants.ATTR_ORDER_DATE);
				String strOrderHeaderKey=eleRelease.getAttribute(VSIConstants.ATTR_ORDER_HEADER_KEY);
				
				String strCustPONo=eleOrderLn.getAttribute(VSIConstants.ATTR_CUST_PO_NO);
				Element eleOrdStss=SCXmlUtil.getChildElement(eleOrderLn, VSIConstants.ELE_ORDER_STATUSES);
				Element eleOrdSts=(Element)eleOrdStss.getElementsByTagName(VSIConstants.ELE_ORDER_STATUS).item(0);
				String strStatusDate=eleOrdSts.getAttribute("StatusDate");
				String strShipNode=eleRelease.getAttribute(VSIConstants.ATTR_SHIP_NODE);
				
				putElementValue(eleJDARequest,"OrderDate", strOrderDate);
				putElementValue(eleJDARequest,"OrderNo", strCustPONo);
				putElementValue(eleJDARequest,"OrderType", "BOPUS");
				putElementValue(eleJDARequest,"StatusDate", strStatusDate);
				putElementValue(eleJDARequest,"Status", "CREATE");
				putElementValue(eleJDARequest,"FullfillingStore", strShipNode);
				
				Element eleOrderLines=SCXmlUtil.createChild(eleJDARequest, VSIConstants.ELE_ORDER_LINES);
				NodeList nlOrderLine=eleRelease.getElementsByTagName(VSIConstants.ELE_ORDER_LINE);
				for(int i=0; i<nlOrderLine.getLength(); i++){
					Element eleOrderLine = (Element)nlOrderLine.item(i);
					Element eleOrderStss=SCXmlUtil.getChildElement(eleOrderLine, VSIConstants.ELE_ORDER_STATUSES);
					Element eleOrderSts=(Element)eleOrderStss.getElementsByTagName(VSIConstants.ELE_ORDER_STATUS).item(0);
					String strStsQty=eleOrderSts.getAttribute(VSIConstants.ATTR_STATUS_QUANTITY);
					Element eleItem=SCXmlUtil.getChildElement(eleOrderLine, VSIConstants.ELE_ITEM);
					String strItemId=eleItem.getAttribute(VSIConstants.ATTR_ITEM_ID);
					Element eleOrdLnOut=SCXmlUtil.createChild(eleOrderLines, VSIConstants.ELE_ORDER_LINE);
					putElementValue(eleOrdLnOut,"OrderedQty", strStsQty);
					Element eleItemOut=SCXmlUtil.createChild(eleOrdLnOut, VSIConstants.ELE_ITEM);
					putElementValue(eleItemOut,"ItemID", strItemId);
				}
				
				printLogs("JDA Allocation Request Prepared is: "+SCXmlUtil.getString(docJDARequest));
				
				VSIUtils.invokeService(env, "VSIMixedCartJDAAllocation_DB", docJDARequest);
				printLogs("JDA Allocation Request is posted to DB successfully");
				
				DataOutputStream wr=null;
				
				String strJDAURL= YFSSystem.getProperty("SFS_FORCED_ALLOCATION_URL");
				printLogs("JDA URL: "+strJDAURL);
				
				String reqString=XMLUtil.getXMLString(docJDARequest);
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
				printLogs("Invoked the JDA service");
				
				if(connection.getResponseCode() == -1 || connection.getResponseMessage() == null){
					printLogs("Inside If condition when ResponseCode is -1 or ResponseMessage is null");
					alertForJDAFailure(env,strOrderNo,strOrderHeaderKey,"VSI_MC_JDA_ALLOCATION_FAILURE_ALERT","MIXEDCART JDA Allocation Failure Alert","VSI_MC_JDA_ALLOCATION_FAILURE_ALERT");
				}
					 
				if(connection.getResponseCode() != 200)
				{
					printLogs("Inside If condition when ResponseCode not equals 200");
					printLogs(connection.getResponseCode()+" - "+connection.getResponseMessage());	
					alertForJDAFailure(env,strOrderNo,strOrderHeaderKey,"VSI_MC_JDA_ALLOCATION_FAILURE_ALERT",connection.getResponseCode()+" - "+connection.getResponseMessage(),"VSI_MC_JDA_ALLOCATION_FAILURE_ALERT");
				}
				
				if(connection.getResponseCode() == 200)
				{
					printLogs("Inside If condition when ResponseCode equals 200");
					responseDoc=parseDoc(connection.getInputStream());
					printLogs("Response from JDA WebService:"+XMLUtil.getXMLString(responseDoc));
					
					VSIUtils.invokeService(env, "VSIMixedCartJDAAllocation_DB", responseDoc);
					printLogs("JDA Allocation Response is posted to DB successfully");
				}
			}else{
				printLogs("Release contains Non PICK_IN_STORE lines and hence will not be processed");
			}
			
		}catch (YFSException e) {
			e.printStackTrace();
			throw new YFSException();
		} catch (Exception e){
			e.printStackTrace();
			throw new YFSException();
		}
		printLogs("================Exiting VSIMixedCartJDAAllocation Class and mixedCartJDAAllocation Method================");
		
		return inXML;
		
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

}
