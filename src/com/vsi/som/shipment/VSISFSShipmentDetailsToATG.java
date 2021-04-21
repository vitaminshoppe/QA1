package com.vsi.som.shipment;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.vsi.oms.utils.VSIBaseCustomAPI;
import com.vsi.oms.utils.VSIConstants;
import com.vsi.oms.utils.VSIUtils;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfs.japi.YFSEnvironment;

public class VSISFSShipmentDetailsToATG extends VSIBaseCustomAPI implements VSIConstants
{
	private YFCLogCategory log = YFCLogCategory.instance(VSISFSShipmentDetailsToATG.class);	
	
	public void splitShipConfirmMsgToATG(YFSEnvironment env, Document inXml)
	{
         log.info("VSISFSShipmentDetailsToATG.splitShipConfirmMsgToATG() i/p xml => "+SCXmlUtil.getString(inXml));		
		try {
			Element eleShipment = (Element) inXml.getElementsByTagName(ELE_SHIPMENT).item(0);
			Element eleOrder = (Element) inXml.getElementsByTagName(ELE_ORDER).item(0);			
			Element eleContainers= (Element) inXml.getElementsByTagName(ELE_CONTAINERS).item(0);
			NodeList nlContainer= eleContainers.getElementsByTagName(ELE_CONTAINER);			
			int containerLength = nlContainer.getLength();
			if(containerLength > 1)
			{
			for (int k = 0; k < containerLength; k++)
			{
				Element eleContainer= (Element) nlContainer.item(k);
				Document splitShipDoc= SCXmlUtil.createDocument(ELE_SHIPMENT_LIST);
				Element splitShipEle = splitShipDoc.getDocumentElement();
				Element shipment= SCXmlUtil.createChild(splitShipEle, ELE_SHIPMENT);
				shipment = copyAttributes(eleShipment,shipment);
				Element splitShipLines= SCXmlUtil.createChild(shipment, ELE_SHIPMENT_LINES);
				Element order = SCXmlUtil.createChild(splitShipEle, ELE_ORDER);
				order = copyAttributes(eleOrder,order);
				Element splitOrderLines= SCXmlUtil.createChild(order, ELE_ORDER_LINES);
				order.appendChild(splitOrderLines);
				Element splitContainers= SCXmlUtil.createChild(shipment, ELE_CONTAINERS);
				Node containerNode = splitShipDoc.importNode(eleContainer, true);
				splitContainers.appendChild(containerNode);
				Element containerDtls = (Element) eleContainer.getElementsByTagName(ELE_CONTAINER_DETAILS).item(0);
				NodeList containerDtl= containerDtls.getElementsByTagName(ELE_CONTAINER_DETAIL);
				for (int i = 0; i < containerDtl.getLength(); i++)
				{
					Element eleContainerDtl = (Element) containerDtl.item(i);
					Element shipmentLine = (Element) eleContainerDtl.getElementsByTagName(ELE_SHIPMENT_LINE).item(0);
					String itemId = shipmentLine.getAttribute(ATTR_ITEM_ID);
					Element orderLines = (Element) eleOrder.getElementsByTagName(ELE_ORDER_LINES).item(0);
					NodeList orderLine= orderLines.getElementsByTagName(ELE_ORDER_LINE);
					for (int j = 0; j < orderLine.getLength(); j++)
					{
						Element orderLineEle = (Element) orderLine.item(j);
						orderLineEle.setAttribute(ATTR_SHIP_QTY, eleContainerDtl.getAttribute(ATTR_QUANTITY));
						if(k > 0)
							orderLineEle.setAttribute(ATTR_USER_CANCELED_QTY, "0");
						else
						{
							Double cancelledQtyValue = Double.valueOf(orderLineEle.getAttribute(ATTR_USER_CANCELED_QTY));
							Integer cancelledQty = cancelledQtyValue.intValue();
							orderLineEle.setAttribute(ATTR_USER_CANCELED_QTY, cancelledQty.toString());
							log.info("cancelledQtyValue => "+cancelledQtyValue +"cancelledQty => "+cancelledQty+"Value => "+orderLineEle.getAttribute(ATTR_USER_CANCELED_QTY));
						}
						if(orderLineEle.getAttribute(ATTR_ITEM_ID).equalsIgnoreCase(itemId))
						{
							Node orderLineNode = splitShipDoc.importNode(orderLineEle, true);
							splitOrderLines.appendChild(orderLineNode);
						}
					}
					shipmentLine.setAttribute(ATTR_QUANTITY, eleContainerDtl.getAttribute(ATTR_QUANTITY));
					Node shipmentLineNode = splitShipDoc.importNode(shipmentLine, true);
					splitShipLines.appendChild(shipmentLineNode);					
				}				
				log.info("VSISFSShipmentDetailsToATG.splitShipConfirmMsgToATG() splitShipDoc => "+SCXmlUtil.getString(splitShipDoc));
				VSIUtils.invokeService(env, "VSISFSShipConfirmATG", splitShipDoc);
			}
			}
			else
			{
				Element orderLines = (Element) eleOrder.getElementsByTagName(ELE_ORDER_LINES).item(0);
				NodeList orderLine= orderLines.getElementsByTagName(ELE_ORDER_LINE);
				for (int j = 0; j < orderLine.getLength(); j++)
				{
					Element orderLineEle = (Element) orderLine.item(j);
					Double cancelledQtyValue = Double.valueOf(orderLineEle.getAttribute(ATTR_USER_CANCELED_QTY));
					Integer cancelledQty = cancelledQtyValue.intValue();
					orderLineEle.setAttribute(ATTR_USER_CANCELED_QTY, cancelledQty.toString());
					log.info("In else - cancelledQtyValue => "+cancelledQtyValue +"cancelledQty => "+cancelledQty+"Value => "+orderLineEle.getAttribute(ATTR_USER_CANCELED_QTY));
				}
				log.info("VSISFSShipmentDetailsToATG.splitShipConfirmMsgToATG() splitShipDoc => "+SCXmlUtil.getString(inXml));
				VSIUtils.invokeService(env, "VSISFSShipConfirmATG", inXml);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}	
	
	public Element copyAttributes(Element fromNode, Element toNode)
	{
		NamedNodeMap attributes = fromNode.getAttributes();
	    for (int i = 0; i < attributes.getLength(); i++) 
	    {
	    	Attr attrNode = (Attr) attributes.item(i);
	    	toNode.setAttributeNS(attrNode.getNamespaceURI(), attrNode.getName(), attrNode.getValue());
	    }
		return toNode;
	}
}