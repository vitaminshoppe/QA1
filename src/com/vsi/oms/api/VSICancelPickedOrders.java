package com.vsi.oms.api;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.TimeZone;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.vsi.oms.utils.VSIBaseCustomAPI;
import com.vsi.oms.utils.VSIConstants;
import com.vsi.oms.utils.VSIUtils;
import com.vsi.oms.utils.XMLUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfs.japi.YFSEnvironment;

public class VSICancelPickedOrders extends VSIBaseCustomAPI implements VSIConstants
{
	private YFCLogCategory log = YFCLogCategory.instance(VSICancelPickedOrders.class);
	public void cancelPickedOrders(YFSEnvironment env, Document inXml)
	{
		try
		{
				Element orderStatusEle = (Element) inXml.getElementsByTagName(ELE_ORDER_STATUS).item(0);
				String originalOrderLineKey = orderStatusEle.getAttribute(ATTR_ORDER_LINE_KEY);
				Document getOrderLineListDoc = SCXmlUtil.createDocument(ELE_ORDER_LINE);
				Element eleOrderLine = getOrderLineListDoc.getDocumentElement();
				eleOrderLine.setAttribute(ATTR_ORDER_LINE_KEY, originalOrderLineKey);
				eleOrderLine.setAttribute(ATTR_DOCUMENT_TYPE, ATTR_DOCUMENT_TYPE_SALES);		
				Document getOrderLineListOutputDoc = VSIUtils.invokeAPI(env, TEMPLATE_ORDER_LINE_LIST,API_GET_ORDER_LINE_LIST, getOrderLineListDoc);
				Element eleExtn = (Element) getOrderLineListOutputDoc.getElementsByTagName(ELE_EXTN).item(0);
				String lastPickDate = null;
				if(eleExtn.getAttribute(ATTR_EXTN_LAST_PICK_DATE_FOR_CANCEL) != "")
					lastPickDate = eleExtn.getAttribute(ATTR_EXTN_LAST_PICK_DATE_FOR_CANCEL);
				else
					lastPickDate = eleExtn.getAttribute(ATTR_EXTN_LAST_PICK_DATE);
				String orderHeaderKey = orderStatusEle.getAttribute(ATTR_ORDER_HEADER_KEY);
				
				SimpleDateFormat formatter = new SimpleDateFormat(YYYY_MM_DD_T_HH_MM_SS);
                Calendar calculatedOrderTime = Calendar.getInstance();
                calculatedOrderTime.setTime(formatter.parse(lastPickDate));
                if(log.isDebugEnabled())
                	log.info("calculatedOrderTime(lastPickDate) => "+calculatedOrderTime.getTime());
                Date localTime = new Date();
                SimpleDateFormat converter = new SimpleDateFormat(YYYY_MM_DD_T_HH_MM_SS);
                converter.setTimeZone(TimeZone.getTimeZone(EST_TIME_ZONE));
                String currTime = converter.format(localTime);
                Calendar currentTime = Calendar.getInstance();
                currentTime.setTime(formatter.parse(currTime));
                if(log.isDebugEnabled())
                	log.info("currTime => "+ currTime + "currentTime => "+currentTime.getTime());   

                if (currentTime.compareTo(calculatedOrderTime) > 0)
                {
                	Document getOrderListInXML=XMLUtil.createDocument(ELE_ORDER);
    				Element getOrderListEle = getOrderListInXML.getDocumentElement();
    				getOrderListEle.setAttribute(ATTR_ORDER_HEADER_KEY,orderHeaderKey);
    				getOrderListEle.setAttribute(ATTR_DOCUMENT_TYPE, ATTR_DOCUMENT_TYPE_SALES);
    				Document getOrderListOutXML = VSIUtils.invokeAPI(env, TEMPLATE_GET_ORDER_LIST_FOR_ORDER_MONITOR, API_GET_ORDER_LIST, getOrderListInXML);
    				NodeList orderLineNode = getOrderListOutXML.getElementsByTagName(ELE_ORDER_LINE);
    				int orderLineNodeLength = orderLineNode.getLength();
    				if(orderLineNodeLength == 1)
    				{
    					Element orderLineEle = (Element) getOrderListOutXML.getElementsByTagName(ELE_ORDER_LINE).item(0);
    					String lineType = orderLineEle.getAttribute(ATTR_LINE_TYPE);
    					if(lineType.equalsIgnoreCase(LINETYPE_PUS) || lineType.equalsIgnoreCase(LINETYPE_STS))
    							changeOrderLineCancellation(orderHeaderKey, originalOrderLineKey, env);
    				}
    				else
    				{
    					int shipToStore = 0, pickInStore = 0;
    					String lastPickDateVal = null;
    					ArrayList<String> orderLineKeys = new ArrayList<String>();
    					for(int l=0; l< orderLineNodeLength; l++)
    					{
    						Element orderLineEle = (Element) getOrderListOutXML.getElementsByTagName(ELE_ORDER_LINE).item(l);
        					String lineTypeValue = orderLineEle.getAttribute(ATTR_LINE_TYPE);
        					if(lineTypeValue.equalsIgnoreCase(LINETYPE_STS))
        						shipToStore++;
        					else if(lineTypeValue.equalsIgnoreCase(LINETYPE_PUS))
        						pickInStore++;
    					}
    					if(orderLineNodeLength == shipToStore || orderLineNodeLength == pickInStore)
    					{
    						for(int l=0; l< orderLineNodeLength; l++)
    						{
    							Element orderLineEle = (Element) getOrderListOutXML.getElementsByTagName(ELE_ORDER_LINE).item(l);
            					double maxLineStatus = Double.parseDouble(orderLineEle.getAttribute(ATTR_MAX_LINE_STATUS));
            					Element elemExtn = (Element) orderLineEle.getElementsByTagName(ELE_EXTN).item(0);
            					log.info("elemExtn.getAttribute(ATTR_EXTN_LAST_PICK_DATE_FOR_CANCEL) => "+elemExtn.getAttribute(ATTR_EXTN_LAST_PICK_DATE_FOR_CANCEL));
            					if(elemExtn.getAttribute(ATTR_EXTN_LAST_PICK_DATE_FOR_CANCEL) != "")
            					{
            						lastPickDateVal = elemExtn.getAttribute(ATTR_EXTN_LAST_PICK_DATE_FOR_CANCEL);
            						log.info("In If - lastPickDateVal => "+lastPickDateVal);
            					}
            					else
            					{
            						lastPickDateVal = elemExtn.getAttribute(ATTR_EXTN_LAST_PICK_DATE);
            						log.info("In Else - lastPickDateVal => "+lastPickDateVal);
            					}
            					log.info("lastPickDateVal => "+lastPickDateVal);
        						SimpleDateFormat formater = new SimpleDateFormat(YYYY_MM_DD_T_HH_MM_SS);
        		                Calendar calculatedLastPickDate = Calendar.getInstance();
        		                calculatedLastPickDate.setTime(formater.parse(lastPickDateVal));
        		                if((maxLineStatus == 3200.500) && (currentTime.compareTo(calculatedLastPickDate) > 0))
        								changeOrderLineCancellation(orderHeaderKey, orderLineEle.getAttribute(ATTR_ORDER_LINE_KEY), env);
    						}
    					}
    					else if(shipToStore > 0 && pickInStore > 0)
    					{
    						boolean bosts = false;
    						for(int j=0; j< orderLineNodeLength; j++)
    						{
    							Element orderLineEle = (Element) getOrderListOutXML.getElementsByTagName(ELE_ORDER_LINE).item(j);
            					String lineTypeValue = orderLineEle.getAttribute(ATTR_LINE_TYPE);
            					double maxLineStatus = Double.parseDouble(orderLineEle.getAttribute(ATTR_MAX_LINE_STATUS));
            					Element elemExtn = (Element) orderLineEle.getElementsByTagName(ELE_EXTN).item(0);
            					log.info("Combo - elemExtn.getAttribute(ATTR_EXTN_LAST_PICK_DATE_FOR_CANCEL) => "+elemExtn.getAttribute(ATTR_EXTN_LAST_PICK_DATE_FOR_CANCEL));
            					if(elemExtn.getAttribute(ATTR_EXTN_LAST_PICK_DATE_FOR_CANCEL) != "")
            					{
            						lastPickDateVal = elemExtn.getAttribute(ATTR_EXTN_LAST_PICK_DATE_FOR_CANCEL);
            						log.info("Combo In If - lastPickDateVal => "+lastPickDateVal);
            					}
            					else
            					{
            						lastPickDateVal = elemExtn.getAttribute(ATTR_EXTN_LAST_PICK_DATE);
            						log.info("Combo In Else - lastPickDateVal => "+lastPickDateVal);
            					}
            					log.info("lastPickDateVal in Combo => "+lastPickDateVal);
            					SimpleDateFormat formater = new SimpleDateFormat(YYYY_MM_DD_T_HH_MM_SS);
        		                Calendar calculatedLastPickDate = Calendar.getInstance();
        		                calculatedLastPickDate.setTime(formater.parse(lastPickDateVal));
            					if(lineTypeValue.equalsIgnoreCase(LINETYPE_PUS) && (maxLineStatus == 3200.500) && (currentTime.compareTo(calculatedLastPickDate) > 0))
            						orderLineKeys.add(orderLineEle.getAttribute(ATTR_ORDER_LINE_KEY));
            					else if(lineTypeValue.equalsIgnoreCase(LINETYPE_STS) && (maxLineStatus < 3200.500) && (currentTime.compareTo(calculatedLastPickDate) > 0))
            					{
            						bosts = true;
            						break;
            					}
            					else if (lineTypeValue.equalsIgnoreCase(LINETYPE_STS) && (maxLineStatus == 3200.500) && (currentTime.compareTo(calculatedLastPickDate) > 0))
            					{
            						orderLineKeys.add(orderLineEle.getAttribute(ATTR_ORDER_LINE_KEY));
            					}
    						}
    						if(!bosts)
    						{
    							Iterator<String> iter = orderLineKeys.iterator();
    						      while (iter.hasNext())
    						    	  changeOrderLineCancellation(orderHeaderKey, iter.next().toString(), env);
    						}
    					}
    				}
                }                
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	public void changeOrderLineCancellation(String orderHeaderKey, String orderLineKey, YFSEnvironment env)
	{
		try
		{
		Document changeOrderDoc = SCXmlUtil.createDocument(ELE_ORDER);
		Element eleOrder = changeOrderDoc.getDocumentElement();
		eleOrder.setAttribute(ATTR_OVERRIDE, VSIConstants.FLAG_Y);
		eleOrder.setAttribute(ATTR_ORDER_HEADER_KEY, orderHeaderKey);
		Element eleOrderLines = SCXmlUtil.createChild(eleOrder, ELE_ORDER_LINES);
		Element elemOrderLine = SCXmlUtil.createChild(eleOrderLines, ELE_ORDER_LINE);
		elemOrderLine.setAttribute(ATTR_ORDER_LINE_KEY, orderLineKey);
		elemOrderLine.setAttribute(ATTR_ACTION, ACTION_RELEASE_CANCEL);
		//Prod Issue Fix: Start
		eleOrder.setAttribute(ATTR_MODIFICATION_REASON_CODE, NO_CUSTOMER_PICK);
		//Prod Issue Fix: End
		VSIUtils.invokeAPI(env, API_CHANGE_ORDER, changeOrderDoc);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}
