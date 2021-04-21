package com.vsi.oms.api;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.vsi.oms.utils.VSIConstants;
import com.vsi.oms.utils.XMLUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfs.japi.YFSEnvironment;

public class VSIATGRTAMUpdatesForCapacity implements VSIConstants
    {
		  private YFCLogCategory log = YFCLogCategory.instance(VSIATGRTAMUpdatesForCapacity.class);
		  public Document onhandAvailableDateChange(YFSEnvironment env,Document inXml)
		     {
			   if(log.isDebugEnabled())
				   log.debug("Input for VSIATGRTAMUpdatesForCapacity.onhandAvailableDateChange => "+XMLUtil.getXMLString(inXml));
			   try
			   {
				 Element availabilityChanges =  (Element) inXml.getDocumentElement().getElementsByTagName(ELE_AVAILABILITY_CHANGES).item(0);
				 NodeList availabilityChange = availabilityChanges.getElementsByTagName(ELE_AVAILABILITY_CHANGE);
				 for (int i = 0; i < availabilityChange.getLength(); i++) 
					{
						Element availabilityChangeEle = (Element) availabilityChange.item(i);
						String onhandAvailableDate = availabilityChangeEle.getAttribute(ATTR_ONHAND_AVAILABLE_DATE);
						if(dateGreaterThanCurrentDate(onhandAvailableDate))
							availabilityChangeEle.setAttribute(ATTR_ONHAND_AVAILABLE_QUANTITY,"0");
					}
			   }
			   catch(Exception e)
				{
					e.printStackTrace();
				}
	return inXml;
		     }
		  private boolean dateGreaterThanCurrentDate(String onhandAvailableDateValue)
		  {
				boolean bResult = false;
				onhandAvailableDateValue = onhandAvailableDateValue.substring(0, 10);
				DateFormat sdf = new SimpleDateFormat(VSIConstants.YYYY_MM_DD);				
				Date date = new Date();
				String strCurrentDate = sdf.format(date);				
				Date dPassedDate;
				try 
				{
					dPassedDate = sdf.parse(onhandAvailableDateValue);				
					Date dCurrentDate = sdf.parse(strCurrentDate);
					if(dPassedDate.after(dCurrentDate))
						bResult = true; 
				} catch (ParseException e) {
					e.printStackTrace();
				}
				return bResult;
			}
        }
