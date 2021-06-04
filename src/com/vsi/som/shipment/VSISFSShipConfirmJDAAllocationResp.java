package com.vsi.som.shipment;

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
import com.vsi.oms.api.mixedcart.VSIMixedCartJDAAllocation;
import com.vsi.oms.api.web.VSIInvokeJDAWebservice;
import com.vsi.oms.utils.VSIConstants;
import com.vsi.oms.utils.VSIUtils;
import com.vsi.oms.utils.XMLUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfs.core.YFSSystem;
import com.yantra.yfs.japi.YFSEnvironment;

public class VSISFSShipConfirmJDAAllocationResp implements VSIConstants
{
	private YFCLogCategory log = YFCLogCategory.instance(VSISFSShipConfirmJDAAllocationResp.class);
	//Mixed Cart Changes -- Start
	private static final String TAG = VSIMixedCartJDAAllocation.class.getSimpleName();
	//Mixed Cart Changes -- End
	public void shipConfirmJDAAllocationResp(YFSEnvironment env, Document inXml) throws Exception
	{
		log.info("Input for VSISFSShipConfirmJDAAllocationResp.shipConfirmJDAAllocationResp => "+XMLUtil.getXMLString(inXml));
		if(log.isDebugEnabled())
			log.debug("Input for VSISFSShipConfirmJDAAllocationResp.shipConfirmJDAAllocationResp => "+XMLUtil.getXMLString(inXml));
		DataOutputStream wr = null;
		try
		{
			//Mixed Cart Changes -- Start
			String strOrderHeaderKey=null;
			String strOrderType=inXml.getElementsByTagName(ATTR_ORDER_TYPE).item(0).getTextContent().toString();
			if("BOPUS".equals(strOrderType)){
				Element eleJDARequest=inXml.getDocumentElement();
				strOrderHeaderKey=eleJDARequest.getAttribute(VSIConstants.ATTR_ORDER_HEADER_KEY);
				eleJDARequest.removeAttribute(VSIConstants.ATTR_ORDER_HEADER_KEY);
			}
			//Mixed Cart Changes -- End
			String orderNo=inXml.getElementsByTagName(ATTR_ORDER_NO).item(0).getTextContent().toString();
			NodeList orderLineList = inXml.getElementsByTagName(ELE_ORDER_LINE);
			String itemId="";
			for (int i = 0; i < orderLineList.getLength(); i++) 
			{
				Element orderLineElem = (Element) orderLineList.item(i);
				Element itemEle = (Element) orderLineElem.getElementsByTagName(ELE_ITEM).item(0);
				if(orderLineList.getLength() == 1)
				{
					itemId=itemEle.getElementsByTagName(ATTR_ITEM_ID).item(0).getTextContent().toString();
					break;
				}
				else
					itemId=itemEle.getElementsByTagName(ATTR_ITEM_ID).item(0).getTextContent().toString()+" "+itemId;
			}
			log.info("Entire itemId - SFS =>  "+ itemId);
			if(itemId.length() >= 31)
				itemId = itemId.substring(0,31).concat("...");
			log.info("Truncated/Entire itemId - SFS =>  "+ itemId);
			
			final String forcedAllocUrl= YFSSystem.getProperty(PROP_SFS_FORCED_ALLOCATION_URL);
			String inDocString=XMLUtil.getXMLString(inXml);
			log.info("inDocString "+ inDocString+"forcedAllocUrl => "+ forcedAllocUrl);
			if(log.isDebugEnabled())
				log.debug("inDocString "+ inDocString+"forcedAllocUrl => "+ forcedAllocUrl);
			String strSplit[]=forcedAllocUrl.split("\\?");
			 String endPointUrl=strSplit[0].concat("?");
			 String strArg1=strSplit[1];
			 log.info("endPointUrl => "+endPointUrl+ "   strArg1 :"+strArg1);
			 if(log.isDebugEnabled())
				 log.debug("endPointUrl => "+endPointUrl+ "   strArg1 :"+strArg1);
			 String xmlencoded = URLEncoder.encode(inDocString,UTF8_ENCODING);
			 String strContent=strArg1+"&request="+xmlencoded;
			 log.info("xmlencoded => "+xmlencoded+"strContent => "+strContent);
			 URL url = new URL (endPointUrl);	
			 HttpURLConnection connection =  (HttpURLConnection) url.openConnection();
			 connection.setRequestMethod(JDA_POST_METHOD);
			 byte[] bContent = strContent.getBytes();	
			 connection.setRequestProperty( JDA_REQUEST_PROP, String.valueOf(bContent.length));
			 connection.setDefaultUseCaches(false);
			 connection.setDoOutput(true);
			 Document responseDoc=null;
			 log.info("Invoking SFS - JDA web service");
			 if(log.isDebugEnabled())
				 log.debug("Invoking SFS - JDA web service");
			 wr = new DataOutputStream (connection.getOutputStream());
			 wr.write(bContent);
			 log.info("Invoked SFS - JDA service");
			//Mixed Cart Changes -- Start
			 if(ORDER_TYPE_VALUE.equals(strOrderType)){
			//Mixed Cart Changes -- End
				 VSIInvokeJDAWebservice obj = new VSIInvokeJDAWebservice();
				 if(connection.getResponseCode() == -1 || connection.getResponseMessage() == null)
					 obj.alertForJDASendReleaseFailure(env, itemId, orderNo, "", ALERT_SFS_SHIP_JDA_SERVICE_CALL_FAILURE_EXCEPTION_TYPE, ALERT_SFS_SHIP_JDA_SERVICE_CALL_FAILURE_DETAIL_DESCRIPTION, ALERT_SFS_SHIP_JDA_SERVICE_CALL_FAILURE_QUEUE);
				 if(connection.getResponseCode() != 200)
				 {
					 log.info(connection.getResponseCode()+" - "+connection.getResponseMessage());
					 obj.alertForJDASendReleaseFailure(env, itemId, orderNo, "", ALERT_SFS_SHIP_JDA_SERVICE_CALL_FAILURE_EXCEPTION_TYPE, connection.getResponseCode()+" - "+connection.getResponseMessage(), ALERT_SFS_SHIP_JDA_SERVICE_CALL_FAILURE_QUEUE);
				 }				 
				 if(connection.getResponseCode() == 200)
				 {
					 responseDoc=obj.parseDoc(connection.getInputStream());
					 log.info("Response from SFS - JDA WebService => "+XMLUtil.getXMLString(responseDoc));
					 if(log.isDebugEnabled())
						 log.debug("Response from SFS - JDA WebService => "+XMLUtil.getXMLString(responseDoc));
					 if((responseDoc.getElementsByTagName(ELE_ERROR_CODE).item(0) != null))
					 {
						 Element eleResp = (Element) responseDoc.getElementsByTagName(ELE_REQUEST_RESPONSE).item(0);
						 Element eleOrderNo = SCXmlUtil.createChild(eleResp,ATTR_ORDER_NO);
						 eleOrderNo.setTextContent(orderNo);
						 VSIUtils.invokeService(env, SERVICE_SFS_SHIP_JDA_ALLOCATION_RESPONSE, responseDoc);
					 }				
				 }
			//Mixed Cart Changes -- Start
			 }else if("BOPUS".equals(strOrderType)){
					if(connection.getResponseCode() == -1 || connection.getResponseMessage() == null){
						printLogs("Inside If condition when ResponseCode is -1 or ResponseMessage is null");
						alertForJDAFailure(env,orderNo,strOrderHeaderKey,"VSI_MC_JDA_PICK_ALLOCATION_FAILURE","MIXEDCART JDA PICK Allocation Failure Alert","VSI_MC_JDA_PICK_ALLOCATION_FAILURE");
					}
						 
					if(connection.getResponseCode() != 200)
					{
						printLogs("Inside If condition when ResponseCode not equals 200");
						printLogs(connection.getResponseCode()+" - "+connection.getResponseMessage());	
						alertForJDAFailure(env,orderNo,strOrderHeaderKey,"VSI_MC_JDA_PICK_ALLOCATION_FAILURE",connection.getResponseCode()+" - "+connection.getResponseMessage(),"VSI_MC_JDA_PICK_ALLOCATION_FAILURE");
					}
					
					if(connection.getResponseCode() == 200)
					{
						printLogs("Inside If condition when ResponseCode equals 200");
						responseDoc=parseDoc(connection.getInputStream());
						printLogs("Response from JDA WebService:"+XMLUtil.getXMLString(responseDoc));
						
						printLogs("JDA Allocation Request will be stored in DB: "+XMLUtil.getXMLString(inXml));
						VSIUtils.invokeService(env, "VSIMixedCartJDAPickAllocation_DB", inXml);
						printLogs("JDA Allocation Request is posted to DB successfully");
						
						printLogs("JDA Allocation Response will be stored in DB: "+XMLUtil.getXMLString(responseDoc));
						VSIUtils.invokeService(env, "VSIMixedCartJDAPickAllocation_DB", responseDoc);
						printLogs("JDA Allocation Response is posted to DB successfully");
					}
				 
			 }
			//Mixed Cart Changes -- End
		}
		catch(Exception e)
		{
			 log.info("Exception thrown within VSISFSShipConfirmJDAAllocationResp.shipConfirmJDAAllocationResp() as below => ");
			 e.printStackTrace();
		 }finally {
			 if(wr!=null)
				 wr.close();
		 }
		//Mixed Cart Changes -- Start
		printLogs("Exiting VSISFSShipConfirmJDAAllocationResp class and shipConfirmJDAAllocationResp method");
		//Mixed Cart Changes -- End
	}
	//Mixed Cart Changes -- Start
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
	
	private void printLogs(String mesg) {
		if(log.isDebugEnabled()){
			log.debug(TAG +" : "+mesg);
		}
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
	//Mixed Cart Changes -- End
}
