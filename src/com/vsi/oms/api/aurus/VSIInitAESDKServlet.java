package com.vsi.oms.api.aurus;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.aurus.aesdk.abstractfactory.formfactor.FormFactorHandler;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.vsi.oms.utils.XMLUtil;
import com.yantra.yfc.log.YFCLogCategory;

public class VSIInitAESDKServlet extends HttpServlet{

	private static YFCLogCategory log = YFCLogCategory.instance(VSIInitAESDKServlet.class);
	private static final String TAG = VSIInitAESDKServlet.class.getSimpleName();
	
	private static final long serialVersionUID = 8808616055836017631L;

	  public synchronized void init(ServletConfig config)
	    throws ServletException
	  {
		  printLogs("================Inside VSIInitAESDKServlet Class and init Method================================");
		  String outDoc=null;
		  try {
			  outDoc = initAESDKAPI();
			  printLogs("outDoc: "+outDoc);
		  } catch (ParserConfigurationException e) {
			  printLogs("Exception in VSIInitAESDKServlet Class and init Method");
			  printLogs("The exception is [ "+ e.getMessage() +" ]");
		  }
		  printLogs("================Exiting VSIInitAESDKServlet Class and init Method================================");		  
	  }

	private String initAESDKAPI() throws ParserConfigurationException  {
		
		printLogs("================Inside initAESDKAPI Method================================");
		
		Document docInitAESDKReq = XMLUtil.createDocument("InitAesdkRequest");
		Element eleInitAESDKReq = docInitAESDKReq.getDocumentElement();

		putElementValue(eleInitAESDKReq,"POSID","VSIDEV20");		
		putElementValue(eleInitAESDKReq,"ConfigFilePath","/Sterling/opt/aesdkprop/");
		
		String strRequest=XMLUtil.getXMLString(docInitAESDKReq);
		printLogs("InitAesdkRequest: "+strRequest);

		FormFactorHandler formFactor = new FormFactorHandler();		
		String strResponse = formFactor.initAESDK(strRequest);
		
		printLogs("InitAesdkResponse: "+strResponse);
		printLogs("================Exiting initAESDKAPI Method================================");
		
		return strResponse; 
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
