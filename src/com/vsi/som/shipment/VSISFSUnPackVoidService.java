package com.vsi.som.shipment;

import org.w3c.dom.Document;

import com.vsi.oms.utils.VSIBaseCustomAPI;
import com.vsi.oms.utils.VSIConstants;
import com.vsi.oms.utils.VSIUtils;
import com.yantra.yfs.japi.YFSEnvironment;

public class VSISFSUnPackVoidService extends VSIBaseCustomAPI implements VSIConstants
{
	public void invokeUnPackVoidService(YFSEnvironment env, Document inXml)
	{
		try
		{
			VSIUtils.invokeService(env, SERVICE_SFS_UNPACK_VOID_MSG, inXml);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}
