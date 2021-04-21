package com.vsi.oms.userexit;

import java.util.ArrayList;
import java.util.Properties;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.vsi.oms.utils.VSIConstants;
import com.vsi.oms.utils.VSIUtils;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.core.YFSObject;
import com.yantra.yfs.japi.YFSEnvironment;

public class VSIBeforeChangeOrderFinal implements VSIConstants{
	
	public Properties prop = null;
	private YFCLogCategory log = YFCLogCategory.instance(VSIBeforeChangeOrderFinal.class);
	
	public Document updateSOMPipeline(YFSEnvironment env, Document inXml){
		
		try{
			
			//Mixed Cart Changes -- Start
			log.info("Inside VSIBeforeChangeOrderFinal Class and updateSOMPipeline method");
			log.info("Printing Input XML: "+SCXmlUtil.getString(inXml));
			Element eleInXML=inXml.getDocumentElement();
			String strCOMDraftOrder=eleInXML.getAttribute("IsCOMDraftOrder");
			if(!YFCCommon.isVoid(strCOMDraftOrder) && "Y".equals(strCOMDraftOrder)){
				eleInXML.removeAttribute("IsCOMDraftOrder");
				log.info("COM Draft Order Scenario, Pipeline details will be updated");
				log.info("Order Details before updating Pipeline Details: "+SCXmlUtil.getString(inXml));
				//Mixed Cart Changes -- End
				Element orderLinesEle = (Element) inXml.getDocumentElement().getElementsByTagName(VSIConstants.ELE_ORDER_LINES).item(0);
				if(orderLinesEle != null){
					
					NodeList nlOrderLines = inXml.getDocumentElement().getElementsByTagName(VSIConstants.ELE_ORDER_LINE);
					if(!YFSObject.isVoid(nlOrderLines)){
						//Mixed cart Call Center Changes -- Start
						ArrayList<Element> somEnabledValue = VSIUtils.getCommonCodeList(env, SOM_ENABLED_COMMON_CODE_TYPE , SOM_ENABLED_COMMON_CODE, ATTR_DEFAULT);
						String SOMEnabledCodeValue=null;
						if(!somEnabledValue.isEmpty()){
							
							Element eleCommonCode=somEnabledValue.get(0);
							SOMEnabledCodeValue=eleCommonCode.getAttribute(ATTR_CODE_SHORT_DESCRIPTION);
							log.info("B4ChangeOrderFinal - SOMEnabledCodeValue => "+SOMEnabledCodeValue);
						}
						//Mixed cart Call Center Changes -- End
						for(int i=0;i<nlOrderLines.getLength();i++){
							
							Element eleOrderLine = (Element) nlOrderLines.item(i);
							//Mixed cart Call Center Changes -- Start
							String strLineType=eleOrderLine.getAttribute(ATTR_LINE_TYPE);
							if(strLineType.equalsIgnoreCase(LINETYPE_STS) || strLineType.equalsIgnoreCase(LINETYPE_PUS)){
							//Mixed cart Call Center Changes -- End
								if(SOMEnabledCodeValue.equalsIgnoreCase(SOM_ENABLED_COMPLETE)){
									
										eleOrderLine.setAttribute(ATTR_CONDITION_VARIBALE1,FLAG_Y);
										eleOrderLine.setAttribute(ATTR_PIPELINE_ID, prop.getProperty(ARG_SOM_PIPELINE_ID));
								}
								else if(SOMEnabledCodeValue.equalsIgnoreCase(SOM_ENABLED_PARTIAL)){
									
								    ArrayList<Element> storeIds = VSIUtils.getCommonCodeList(env, SOM_CUTOVER_COMMON_CODE_TYPE, eleOrderLine.getAttribute(ELE_SHIP_NODE), ATTR_DEFAULT);
								    log.info("storeIds.isEmpty() => "+storeIds.isEmpty());
									if(!storeIds.isEmpty()){
										log.info("Its a SOM Enabled Ship Node");
								    	eleOrderLine.setAttribute(ATTR_CONDITION_VARIBALE1,FLAG_Y);
								    	eleOrderLine.setAttribute(ATTR_PIPELINE_ID, prop.getProperty(ARG_SOM_PIPELINE_ID));
								    }
									else{
										log.info("SOM is not enabled for this Ship Node");
										eleOrderLine.setAttribute(ATTR_CONDITION_VARIBALE1,FLAG_N);
										//OMS-2666 Changes -- Start
										eleOrderLine.setAttribute(ATTR_PIPELINE_ID, prop.getProperty("NON_SOM_STORE_PIPELINE_ID"));
										//OMS-2666 Changes -- End
									}
								}
								log.info("B4ChangeOrderFinal - ConditionVariable1 => "+eleOrderLine.getAttribute(ATTR_CONDITION_VARIBALE1)+" pipelineId => "+eleOrderLine.getAttribute(ATTR_PIPELINE_ID));
							//Mixed cart Call Center Changes -- Start	
							}else if(strLineType.equalsIgnoreCase(LINETYPE_STH)) {
								eleOrderLine.setAttribute(VSIConstants.ATTR_PIPELINE_ID, prop.getProperty("SHP_PIPELINE_ID"));
							}
							//Mixed cart Call Center Changes -- End
						}
					}
				}
			//Mixed Cart Changes -- Start
			}else{
				log.info("Not a COM Draft Order Scenario");
			}
			//Mixed Cart Changes -- End
		}
		catch(Exception e){
			e.printStackTrace();
		}
		//Mixed Cart Changes -- Start
		log.info("Printing Output XML: "+SCXmlUtil.getString(inXml));
		log.info("Exiting VSIBeforeChangeOrderFinal Class and updateSOMPipeline method");
		//Mixed Cart Changes -- End
		return inXml;
	}
	public void setProperties(Properties properties){
		prop = properties;
		
	}
}