package com.vsi.som.shipment;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;		//OMS-3729 Change

import javax.xml.XMLConstants;		//OMS-3729 Change
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.vsi.oms.utils.XMLUtil;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.vsi.oms.utils.VSIConstants;
import com.vsi.oms.utils.VSIUtils;
import com.yantra.interop.japi.YIFApi;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfs.core.YFSSystem;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;

public class VSISOMJDAReverseAllocation implements VSIConstants {
	
	private static YFCLogCategory log = YFCLogCategory.instance(VSISOMJDAReverseAllocation.class);
	private static final String TAG = VSISOMJDAReverseAllocation.class.getSimpleName();
	YIFApi api;
	
	public Document processJDAReverseAllocation(YFSEnvironment env, Document inXML){
		
		printLogs("================Inside VSISOMJDAReverseAllocation Class and processJDAReverseAllocation Method================");
		printLogs("Printing Input XML :"+SCXmlUtil.getString(inXML));
		
		try{
			
			Element eleShipment = inXML.getDocumentElement();
			String strShipNode=eleShipment.getAttribute(VSIConstants.ATTR_SHIP_NODE);
			
			Document docJDARequest=XMLUtil.createDocument("VSIEnvelope");
			Element eleJDARequest=docJDARequest.getDocumentElement();
			
			putElementValue(eleJDARequest,"MessageType", "AllocationRequest");
			
			Element eleMessage = SCXmlUtil.createChild(eleJDARequest, "Message");
			
			Element eleShipmentLines=SCXmlUtil.getChildElement(eleShipment, VSIConstants.ELE_SHIPMENT_LINES);
			
			Element eleShipmentLine=SCXmlUtil.getChildElement(eleShipmentLines, VSIConstants.ELE_SHIPMENT_LINE);
			String strReleaseNo=eleShipmentLine.getAttribute(VSIConstants.ATTR_RELEASE_NO);
			
			Element eleOrder=SCXmlUtil.getChildElement(eleShipmentLine, VSIConstants.ELE_ORDER);
			
			String strCreatets=eleOrder.getAttribute(VSIConstants.ATTR_CREATETS);
			String strOrdNo=eleOrder.getAttribute(VSIConstants.ATTR_ORDER_NO);
			String strOrderDate=eleOrder.getAttribute(VSIConstants.ATTR_ORDER_DATE);
			String strCustNo=eleOrder.getAttribute(VSIConstants.ATTR_BILL_TO_ID);
			//OMS-3729 Changes -- Start
			String strOrderType=eleOrder.getAttribute(VSIConstants.ATTR_ORDER_TYPE);
			String strEnteredBy=eleOrder.getAttribute(VSIConstants.ATTR_ENTERED_BY);
			String strStore=null;
			//OMS-3729 Changes -- End
			String strOrderNo=strOrdNo+"*"+strReleaseNo;
			
			putElementValue(eleMessage,"DateTimeStamp", strCreatets);
			putElementValue(eleMessage,"OrderNo", strOrderNo);
			putElementValue(eleMessage,"OrderType", "Ship_to_Home");
			putElementValue(eleMessage,"IntOrderDate", strOrderDate);
			//OMS-3729 Changes -- Start
			if(MARKETPLACE.equals(strOrderType)) {
				strStore=strEnteredBy;
			}else {
				strStore=SHIP_NODE_6102_VALUE;
			}
			putElementValue(eleMessage,"Store", strStore);			//OMS-3011 Change
			//OMS-3729 Changes -- End
			putElementValue(eleMessage,"WhseNo", strShipNode);
			putElementValue(eleMessage,"CustNo", strCustNo);
			
			NodeList nlShipmentLine=eleShipmentLines.getElementsByTagName(VSIConstants.ELE_SHIPMENT_LINE);
			for(int i=0; i<nlShipmentLine.getLength(); i++){
				Element eleShipmentLne=(Element)nlShipmentLine.item(i);
				Element eleItem=SCXmlUtil.createChild(eleMessage, VSIConstants.ELE_ITEM);
				String strJdaSku=eleShipmentLne.getAttribute(VSIConstants.ATTR_ITEM_ID);
				
				String strOrigQty=eleShipmentLne.getAttribute("QuantityReduced");				
				strOrigQty=strOrigQty.substring(0,strOrigQty.indexOf("."));
				
				putElementValue(eleItem,"JdaSku", strJdaSku);
				putElementValue(eleItem,"OrigQty", strOrigQty);
				putElementValue(eleItem,"QtyOrdered", "0");
			}
			
			printLogs("JDA Reverse Allocation Request Prepared is: "+SCXmlUtil.getString(docJDARequest));
			
			VSIUtils.invokeService(env, "VSISOMJDAReverseAllocation_DB", docJDARequest);
			printLogs("JDA Reverse Allocation Request is posted to DB successfully");
			
			DataOutputStream wr=null;
			
			String strJDAURL= YFSSystem.getProperty(VSIConstants.PROP_FORCED_ALLOCATION_URL);
			
			String reqString=XMLUtil.getXMLString(docJDARequest);
			printLogs("JDA Request in String format: "+reqString);
			
			String[] strSplit=strJDAURL.split("\\?");		//OMS-3729 Change
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
			}
				 
			if(connection.getResponseCode() != 200)
			{
				printLogs("Inside If condition when ResponseCode not equals 200");
				printLogs(connection.getResponseCode()+" - "+connection.getResponseMessage());				
			}
			
			if(connection.getResponseCode() == 200)
			{
				printLogs("Inside If condition when ResponseCode equals 200");
				responseDoc=parseDoc(connection.getInputStream());
				printLogs("Response from JDA WebService:"+XMLUtil.getXMLString(responseDoc));
				
				VSIUtils.invokeService(env, "VSISOMJDAReverseAllocation_DB", responseDoc);
				printLogs("JDA Reverse Allocation Response is posted to DB successfully");
			}
			
		}catch (Exception e){
			//OMS-3729 Changes -- Start
			printLogs("Exception in VSISOMJDAReverseAllocation Class and processJDAReverseAllocation Method");
			printLogs("The exception is [ "+ e.getMessage() +" ]");
			//OMS-3729 Changes -- End
			throw new YFSException();
		}
		
		printLogs("================Exiting VSISOMJDAReverseAllocation Class and processJDAReverseAllocation Method================");
		return inXML;		
	}
	
	private Document parseDoc(InputStream instream) throws ParserConfigurationException, SAXException, IOException{
		
		printLogs("================Inside parseDoc Method================");
		DocumentBuilderFactory factory =DocumentBuilderFactory.newInstance();
		//OMS-3729 Changes -- Start
		factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, ""); 
		factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
		//OMS-3729 Changes -- End
		factory.isIgnoringElementContentWhitespace();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc=null;
	 	if(instream.markSupported()){
	 		instream.mark(Integer.MAX_VALUE);
	 		BufferedReader buffread = new BufferedReader(new InputStreamReader(instream,StandardCharsets.UTF_8));		//OMS-3729 Change
		    String  inputLine = null;
	
		 while((inputLine = buffread.readLine())!= null){		//OMS-3729 Change
			 printLogs(inputLine);			 
		    }  
		    instream.reset();
	  }
	 	doc=builder.parse(instream);
	 	printLogs("JDA WebService Response being Parsed"+instream);
	 	
	 	printLogs("================Exiting parseDoc Method================");
	 	return doc;
  }
	
	public void putElementValue(Element childEle, String key, Object value) {
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
