/*
JIRA: OMS-3420
 */
package com.vsi.oms.api.web;

import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.vsi.oms.utils.VSIBaseCustomAPI;
import com.vsi.oms.utils.VSIConstants;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class VSIBlankPersonInfoShipTo extends VSIBaseCustomAPI implements VSIConstants {
    public Document createExceptionForBlankPersonInfo(YFSEnvironment env, Document inXml) {

        Element orderElement = inXml.getDocumentElement();
        String strDocumentType = orderElement.getAttribute(ATTR_DOCUMENT_TYPE);
        String entryType = orderElement.getAttribute(ATTR_ENTRY_TYPE);
        int counter = 0;
        boolean flag = false;
        try{
            if (strDocumentType.equalsIgnoreCase(ATTR_DOCUMENT_TYPE_SALES) && entryType.equalsIgnoreCase(ENTRYTYPE_WEB)) {

                flag = checkDTCOrder(orderElement);//Checking the order Type
                if(flag){//if the order is having a STH line
                    Element personInfoShipToHdr = SCXmlUtil.getChildElement(orderElement, ATTR_PERSON_INFO_SHIP_TO);
                    counter = personInfoShipToValidation(personInfoShipToHdr);
                    //Checking the line level only if header level has correct state and zip code.
                    if (counter == 0) {
                        NodeList orderLineList = orderElement
                                .getElementsByTagName(VSIConstants.ELE_ORDER_LINE);
                        int orderLineLength = orderLineList.getLength();
                        for (int i = 0; i < orderLineLength; i++) {
                            Element eleOrderLine = (Element) orderLineList.item(i);
                            String lineType = eleOrderLine.getAttribute(ATTR_LINE_TYPE);
                            //Verifying only for DTC orders
                            if (LINETYPE_STH.equals(lineType)) {
                                Element personInfoShipToLine = SCXmlUtil.getChildElement(eleOrderLine, ATTR_PERSON_INFO_SHIP_TO);
                                counter = personInfoShipToValidation(personInfoShipToLine);
                                if (counter == 1) {
                                    break;
                                }
                            }
                        }
                    }
                }
                if (counter == 1) {
                    //Throwing the exception
                    throw new YFSException(BLANK_PERSONSHIPTO_ALERT_DETAIL_DESCRIPTION, BLANK_PERSONSHIPTO_ALERT_DETAIL_DESCRIPTION, BLANK_PERSONSHIPTO_ALERT_DETAIL_DESCRIPTION);
                }

            }
        }
        catch(Exception e){
            e.printStackTrace();
            throw new YFSException(BLANK_PERSONSHIPTO_ALERT_DETAIL_DESCRIPTION, BLANK_PERSONSHIPTO_ALERT_DETAIL_DESCRIPTION, BLANK_PERSONSHIPTO_ALERT_DETAIL_DESCRIPTION);
        }

        return inXml;
    }

    public int personInfoShipToValidation(Element personInfoShipTo) {
        int flag = 0;
        String shipToState = personInfoShipTo.getAttribute(ATTR_STATE);
        String shipToZip = personInfoShipTo.getAttribute(ATTR_ZIPCODE);
        //Checking whether zipcode or state is blank
        if (YFCCommon.isVoid(shipToState) || YFCCommon.isVoid(shipToZip)) {
            flag = 1;
        }
        return flag;
    }

    public boolean checkDTCOrder(Element orderElement) {
        NodeList orderLineList = orderElement
                .getElementsByTagName(VSIConstants.ELE_ORDER_LINE);
        int orderLineLength = orderLineList.getLength();
        for (int i = 0; i < orderLineLength; i++) {
            Element eleOrderLine = (Element) orderLineList.item(i);
            String lineType = eleOrderLine.getAttribute(ATTR_LINE_TYPE);
            //Verifying only for DTC orders
            if (LINETYPE_STH.equals(lineType)) {
                return true;
            }
        }
        return false;
    }
}