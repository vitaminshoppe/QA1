package com.vsi.oms.api.web;

import com.vsi.oms.utils.VSIBaseCustomAPI;
import com.vsi.oms.utils.VSIConstants;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class VSIBlankShipNode extends VSIBaseCustomAPI implements VSIConstants
{
    YFCLogCategory log=YFCLogCategory.instance(VSIBlankPersonInfoShipTo.class);
    public Document createExceptionForBlankShipNode(YFSEnvironment env, Document inXml)
    {
        if(log.isDebugEnabled()){
            log.verbose("Inside the VSI BLANK SHIP NODE CLASS");
        }
        Element orderElement = inXml.getDocumentElement();
        String strDocumentType = orderElement.getAttribute(ATTR_DOCUMENT_TYPE);
        String entryType=orderElement.getAttribute(ATTR_ENTRY_TYPE);
        int counter=0;
        try{
            if(strDocumentType.equalsIgnoreCase(ATTR_DOCUMENT_TYPE_SALES) &&entryType.equalsIgnoreCase(ENTRYTYPE_WEB) )
            {
                NodeList orderLineList = orderElement
                        .getElementsByTagName(VSIConstants.ELE_ORDER_LINE);
                int orderLineLength = orderLineList.getLength();
                for(int i=0;i<orderLineLength;i++) {
                    Element eleOrderLine = (Element) orderLineList.item(i);
                    String lineType = eleOrderLine.getAttribute(ATTR_LINE_TYPE);
                    if (LINETYPE_PUS.equals(lineType)) {
                        String shipNode = eleOrderLine.getAttribute(ELE_SHIP_NODE);
                        if(log.isDebugEnabled()){
                            log.verbose("VSIBLANKSHIPNODE: Shipnode is "+shipNode);
                        }
                        if (YFCCommon.isVoid(shipNode)) {

                            counter=1;
                            if(log.isDebugEnabled()){
                                log.verbose("VSIBLANKSHIPNODE: Counter is "+counter);
                            }
                            break;
                        }
                    }
                }
                if(log.isDebugEnabled()){
                    log.verbose("VSIBLANKSHIPNODE: Counter before exception "+counter);
                }
                if(counter==1) {
                    if(log.isDebugEnabled()){
                        log.verbose("VSIBLANKSHIPNODE: inside the exception create");
                    }
                    //Creation of Exception
                    throw new YFSException(BLANK_SHIPNODE_EXCEPTION_DETAIL_DESCRIPTION,BLANK_SHIPNODE_EXCEPTION_DETAIL_DESCRIPTION,BLANK_SHIPNODE_EXCEPTION_DETAIL_DESCRIPTION);
                }
            }

        }
        catch(YFSException e){
            e.printStackTrace();
            throw new YFSException(BLANK_SHIPNODE_EXCEPTION_DETAIL_DESCRIPTION,BLANK_SHIPNODE_EXCEPTION_DETAIL_DESCRIPTION,BLANK_SHIPNODE_EXCEPTION_DETAIL_DESCRIPTION);
        }

        return inXml;
    }
}
