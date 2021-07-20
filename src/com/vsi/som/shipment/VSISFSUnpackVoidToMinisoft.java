package com.vsi.som.shipment;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.vsi.oms.utils.VSIBaseCustomAPI;
import com.vsi.oms.utils.VSIConstants;
import com.vsi.oms.utils.VSIUtils;
import com.vsi.oms.utils.XMLUtil;
import com.yantra.interop.japi.YIFApi;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.core.YFSSystem;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;

public class VSISFSUnpackVoidToMinisoft extends VSIBaseCustomAPI implements VSIConstants {
	
	private YFCLogCategory log = YFCLogCategory.instance(VSICreateShipmentOnRelease.class);
	private static final String TAG = VSISOMPrintReceiptXML.class.getSimpleName();
	YIFApi api;
	
	public void callSFSUnpackVoidWS(YFSEnvironment env, Document inXml )
	{
		Document requestDoc= SCXmlUtil.createDocument("CartonData");
		printLogs("================Inside VSISFSUnpackVoidToMinisoft Class and callSFSUnpackVoidWS Method================");
		printLogs("VSISFSUnpackVoidToMinisoft: Printing Input XML to callSFSUnpackVoidWS from  :"+SCXmlUtil.getString(inXml));		
		//log.info("VSISFSUnpackVoidToMinisoft: Printing Input XML to callSFSUnpackVoidWS from  :"+SCXmlUtil.getString(inXml));
		//changes for re-packing scenarios-11Sep2020-Ashutosh-Start
		Document docChangeShipment = SCXmlUtil.createDocument("Shipment");
		//changes for re-packing scenarios-11Sep2020-Ashutosh-End
		
		
		try {

					Element eleShipmentIn= inXml.getDocumentElement();
					Element eleRequest= requestDoc.getDocumentElement();
					String strDocType= "VOID";
					String strShipmentKey=eleShipmentIn.getAttribute(ATTR_SHIPMENT_KEY);
					//changes for re-packing scenarios-11Sep2020-Ashutosh-Start
					Element eleExtn= SCXmlUtil.getChildElement(eleShipmentIn, "Extn");
					String strExtnPackShipFlag="";
					
					if (eleExtn!=null) {
						 strExtnPackShipFlag = eleExtn.getAttribute("ExtnPackShipFlag");
					}
					if (strExtnPackShipFlag.equalsIgnoreCase("Y")) {
						 Element eleChangeShipmentIn= docChangeShipment.getDocumentElement();
						 eleChangeShipmentIn.setAttribute(ATTR_SHIPMENT_KEY, strShipmentKey);
						 eleChangeShipmentIn.setAttribute(ATTR_OVERRIDE, "Y");
						 Element eleChangeShipmentExtn= SCXmlUtil.createChild(eleChangeShipmentIn, "Extn");
						 eleChangeShipmentExtn.setAttribute("ExtnPackShipFlag", "");
						 
						 printLogs("changeShipment api Input: "+XMLUtil.getXMLString(docChangeShipment));
						 VSIUtils.invokeAPI(env, VSIConstants.API_CHANGE_SHIPMENT, docChangeShipment);						 
					}					
					//changes for re-packing scenarios-11Sep2020-Ashutosh-End
				
					
					putElementValue(eleRequest,"DocumentType", strDocType);

					Element elePackages= SCXmlUtil.createChild(eleRequest, "Packages");
					Element eleContainers= SCXmlUtil.getChildElement(eleShipmentIn, "Containers");
					NodeList nlEleContainer= eleContainers.getElementsByTagName("Container");
					int totalEleContainer = nlEleContainer.getLength();
					String strTrackingNo="";
					Element elePackage=null;

					if(totalEleContainer>0) {
					for (int j = 0; j < totalEleContainer; j++) {		
						
						elePackage= SCXmlUtil.createChild(elePackages, "Package");
						//elePackage.setAttribute("BoxNumber", "1");
						Element eleContainer= (Element) nlEleContainer.item(j);
						Element eleExtnCntr = SCXmlUtil.getChildElement(eleContainer, "Extn");
						String strContainerScm= eleContainer.getAttribute("ContainerScm");
						strTrackingNo= eleContainer.getAttribute("TrackingNo");
						if(eleExtnCntr != null) {
							String extnBoxNumber = eleExtnCntr.getAttribute("ExtnBoxNumber");
							log.info("BoxNumber::::"+extnBoxNumber);
							elePackage.setAttribute("BoxNumber", extnBoxNumber);
						}
						putElementValue(elePackage,"ContainerScm", strContainerScm);
						putElementValue(elePackage,"TrackingNo", strTrackingNo);
						}
						}
					String strRaiseAlertFlag="Y";
					String cartonId="";

					if (!strTrackingNo.isEmpty()) {
						Document responseDoc= invokeMiniSoftWebService (env, requestDoc);
						//changes for new minisoft response-start
						printLogs("VSISFSUnpackVoidToMinisoft: Printing Response XML From WS :"+SCXmlUtil.getString(responseDoc));
						//log.info("VSISFSUnpackVoidToMinisoft: Printing Response XML From WS :"+SCXmlUtil.getString(responseDoc));

						Element eleResponse= responseDoc.getDocumentElement();						
						Element eleStatus=SCXmlUtil.getChildElement(eleResponse, "Status");
						if (eleStatus != null) {
						String strStatus=eleStatus.getTextContent();
						if((!strStatus.isEmpty()) && (strStatus.equalsIgnoreCase("SUCCESS"))){
							strRaiseAlertFlag="N";
						}
						}
						Element eleCartonID=SCXmlUtil.getChildElement(eleResponse, "CartonID");
						if (eleCartonID != null) {
						 cartonId=eleCartonID.getTextContent();
							
							}
						//changes for new minisoft response-end
					//Raise Alert on Failure
					if (strRaiseAlertFlag.contentEquals("Y")) {
						
						String exceptionType = "ALERT_VOID_UNPACK_INCORRECT_RESPONSE";
						String exceptionDescription = "ALERT_SFS_UNPACK_VOID_WS_INCOMPLETE_RESPONSE: Minisoft Response XML for Void Unpack either returned ERROR or has one or more elements missing ";
						String queueId = "VSI_ALERT_SFS_UNPACK_VOID_WS_FAIL_QUEUE";
						alertForUnpackVoidFailure(env, strShipmentKey, cartonId, strTrackingNo, exceptionType, exceptionDescription, queueId);
					}
					}

					}
				
					catch (YFSException e) {
						e.printStackTrace();
						throw new YFSException();
					} catch (Exception e){
						e.printStackTrace();
						throw new YFSException();
					}
		

			}
	
	public void alertForUnpackVoidFailure(YFSEnvironment env, String shipmentKey, String containerNo, String trackingNo, String exceptionType, String exceptionDescription, String queueId)
	{
		try
		{
		//Creation of Alert
		Document createExceptionDoc = SCXmlUtil.createDocument(ELE_INBOX);
		Element eleInbox = createExceptionDoc.getDocumentElement();
		eleInbox.setAttribute(ATTR_SHIPMENT_KEY, shipmentKey);
		eleInbox.setAttribute(ATTR_CONTAINER_NO, containerNo);
		eleInbox.setAttribute(ATTR_TRACKING_NO, trackingNo);
		eleInbox.setAttribute(ATTR_EXCEPTION_TYPE, exceptionType);
		eleInbox.setAttribute(ATTR_DETAIL_DESCRIPTION, exceptionDescription);
		eleInbox.setAttribute(ATTR_QUEUE_ID, queueId);
		Element eleConsolidationTemplate = createExceptionDoc.createElement(ELE_CONSOLIDATE_TEMPLATE);
		eleInbox.appendChild(eleConsolidationTemplate);
		Element eleInboxCpy = (Element) eleInbox.cloneNode(true);
		eleConsolidationTemplate.appendChild(eleInboxCpy);
		VSIUtils.invokeAPI(env, API_CREATE_EXCEPTION,createExceptionDoc);
		}
		catch(Exception e)
		{
			if(log.isDebugEnabled())
			{
				log.info("alert For Unpack Void WS Failure - in catch block => ");
				e.printStackTrace();
			}
		}
	}
	public void alertForUnpackVoidWebserviceFailure(YFSEnvironment env, String exceptionType, String exceptionDescription, String queueId)
	{
		try
		{
		//Creation of Alert
		Document createExceptionDoc = SCXmlUtil.createDocument(ELE_INBOX);
		Element eleInbox = createExceptionDoc.getDocumentElement();
		eleInbox.setAttribute(ATTR_EXCEPTION_TYPE, exceptionType);
		eleInbox.setAttribute(ATTR_DETAIL_DESCRIPTION, exceptionDescription);
		eleInbox.setAttribute(ATTR_QUEUE_ID, queueId);
		Element eleConsolidationTemplate = createExceptionDoc.createElement(ELE_CONSOLIDATE_TEMPLATE);
		eleInbox.appendChild(eleConsolidationTemplate);
		Element eleInboxCpy = (Element) eleInbox.cloneNode(true);
		eleConsolidationTemplate.appendChild(eleInboxCpy);
		VSIUtils.invokeAPI(env, API_CREATE_EXCEPTION,createExceptionDoc);
		}
		catch(Exception e)
		{
			if(log.isDebugEnabled())
			{
				printLogs("alert For Unpack Void WS Failure - in catch block => ");
				e.printStackTrace();
			}
		}
	}
	
	private Document invokeMiniSoftWebService(YFSEnvironment env, Document docUnpackXML) throws Exception {
		
		printLogs("================Inside VSISFSUnpackVoidToMinisoft: invokeMiniSoftWebService Method================");
		
		String strMinisoftURL=YFSSystem.getProperty("MINISOFT_SFS_WEBSERVICE_URL");
		
		printLogs("URL obtained from COP file: "+strMinisoftURL);		
		
		String inDocString=XMLUtil.getXMLString(docUnpackXML);
		printLogs("inDocString: "+inDocString);
		
		DataOutputStream wr=null;		
		
		URL url = new URL (strMinisoftURL);
		
		HttpURLConnection connection =  (HttpURLConnection) url.openConnection();
		
		connection.setRequestMethod("POST");
		byte[] bContent = inDocString.getBytes();	
		connection.setRequestProperty("Content-Type", "application/xml;charset=utf-8");
		connection.setRequestProperty("Content-Length", String.valueOf(bContent.length));
		connection.setDefaultUseCaches(false);
		connection.setDoOutput(true);		
		
		printLogs("Invoking the MiniSoft web service");
		
		wr = new DataOutputStream (connection.getOutputStream());
		wr.write(bContent);
		
		printLogs("Invoked the MiniSoft web service");
		
		wr.flush();
        wr.close();
        
        printLogs("Before reading ResponseMessage");
        
        String responseStatus = connection.getResponseMessage();
        printLogs("responseStatus: "+responseStatus);
        
        BufferedReader in = new BufferedReader(new InputStreamReader(
        		connection.getInputStream()));
        printLogs("After reading InputStream");
        String inputLine;
        StringBuffer response = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        
        printLogs("response: " + response.toString());
        
        String strResponse=response.toString();
        
        printLogs("strResponse: " + strResponse);
		
		Document responseDoc=null;	
		
		 if(connection.getResponseCode() == -1 || connection.getResponseMessage() == null) {
			 alertForUnpackVoidWebserviceFailure(env, "ALERT_SFS_UNPACK_VOID_WS_FAIL", "ALERT_SFS_UNPACK_VOID_WS_CONNECTION_FAILURE", "VSI_ALERT_SFS_UNPACK_VOID_WS_FAIL_QUEUE");
		 } 
		 if(connection.getResponseCode() != 200)
		 {
			 printLogs(connection.getResponseCode()+" - "+connection.getResponseMessage());
			 alertForUnpackVoidWebserviceFailure(env, "ALERT_SFS_UNPACK_VOID_WS_FAIL", "ALERT_SFS_UNPACK_VOID_WS_CONNECTION_FAILURE", "VSI_ALERT_SFS_UNPACK_VOID_WS_FAIL_QUEUE");
		 }				 
		 if(connection.getResponseCode() == 200)
		 {		
			 responseDoc=SCXmlUtil.createFromString(strResponse);
		 }
		
		VSIUtils.invokeService(env, "VSISFSVoidUnpack_DB", docUnpackXML);
		
		printLogs("MiniSoft Request is stored in DB");
		
		printLogs("MiniSoft Response will be stored in DB");
		
		VSIUtils.invokeService(env, "VSISFSVoidUnpack_DB", responseDoc);
		
		printLogs("MiniSoft Response is stored in DB");
		
		printLogs("Response from MiniSoft WebService: "+XMLUtil.getXMLString(responseDoc));
		
		printLogs("================Exiting VSISFSUnpackVoidToMinisoft: invokeMiniSoftWebService Method================");
		
		return responseDoc;
	}



private void printLogs(String mesg) {
	if(log.isDebugEnabled()){
		log.debug(TAG +" : "+mesg);
	}
}

private void putElementValue(Element childEle, String key, Object value) {
	Element ele = SCXmlUtil.createChild(childEle, key);
	if(value instanceof String ) {
		ele.setTextContent((String)value);
	}else if(value instanceof Element ) {
		ele.appendChild((Element)value);
	}
}
	
}
