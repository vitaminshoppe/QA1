package com.vsi.som.api;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.vsi.oms.utils.VSIConstants;
import com.vsi.oms.utils.VSIUtils;
import com.vsi.oms.utils.XMLUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfs.japi.YFSEnvironment;

public class VSICreateUsers implements VSIConstants
{
	private YFCLogCategory log = YFCLogCategory.instance(VSICreateUsers.class);
	public void createUsers(YFSEnvironment env, Document inXml)
	{
		log.info("Input for VSICreateUsers.createUsers => "+XMLUtil.getXMLString(inXml));
		if(log.isDebugEnabled())
			log.debug("Input for VSICreateUsers.createUsers => "+XMLUtil.getXMLString(inXml));
		try
		{
			Document getUserInXml=XMLUtil.createDocument(ELE_USER);
			Element userElem = getUserInXml.getDocumentElement();
			Element employeDtlEle = (Element) inXml.getElementsByTagName(ELE_EMPLOYEE_DETAIL).item(0);
			String storeNo = employeDtlEle.getElementsByTagName(ELE_STORE_NUM).item(0).getTextContent().toString();
			int i=0;
			while(i < storeNo.length() && storeNo.charAt(i) == '0')
				i++;
			StringBuffer storeNum = new StringBuffer(storeNo);
			storeNum.replace(0, i, "");
			log.info("storeNo => "+storeNo+"storeNum => "+storeNum);
			//OMS-2968 Changes -- Start
			//userElem.setAttribute(ATTR_ORG_KEY, storeNum.toString());
			//OMS-2968 Changes -- End
			userElem.setAttribute(ATTR_LOGIN_ID, employeDtlEle.getElementsByTagName(ELE_EMPLOYEE_ID).item(0).getTextContent().toString());
			//OMS-2968 Changes -- Start
			if(log.isDebugEnabled())
				log.debug("Input for getUserList API: "+XMLUtil.getXMLString(getUserInXml));
			//OMS-2968 Changes -- End
			Document getUserListOutXml = VSIUtils.invokeAPI(env, API_GET_USER_LIST, getUserInXml);
			log.info("getUserListOutXml => "+XMLUtil.getXMLString(getUserListOutXml));
			Element userList = getUserListOutXml.getDocumentElement();
			Document userHierarchyInXml;
			if(!userList.hasChildNodes())
			{	
				//OMS-2968 Changes -- Start
				if(log.isDebugEnabled())
					log.debug("New user");
				//OMS-2968 Changes -- End
				userHierarchyInXml = userHierarchyXml(env, employeDtlEle, storeNum);
				log.info("createUserHierarchy => "+XMLUtil.getXMLString(userHierarchyInXml));
				VSIUtils.invokeAPI(env, API_CREATE_USER_HIERARCHY, userHierarchyInXml);
				//OMS-2968 Changes -- Start
				if(log.isDebugEnabled())
					log.debug("createUserHierarchy API Invoked successfully");
				//OMS-2968 Changes -- End
			}
			else
			{
				//OMS-2968 Changes -- Start				
				if(log.isDebugEnabled())
					log.debug("Existing user");				
				Element eleUser=SCXmlUtil.getChildElement(userList, ELE_USER);
				String strOrgKey=eleUser.getAttribute(ATTR_ORG_KEY);  
				String strStrIn=storeNum.toString();
				if(strStrIn.equals(strOrgKey)){
					if(log.isDebugEnabled())
						log.debug("User's current Organization and new Organization from Input are same, modifyUserHierarchy API will be invoked");
					userHierarchyInXml = userHierarchyXml(env, employeDtlEle, storeNum);
					log.info("modifyUserHierarchy => "+XMLUtil.getXMLString(userHierarchyInXml));
					VSIUtils.invokeAPI(env, API_MOD_USER_HIERARCHY, userHierarchyInXml);
					if(log.isDebugEnabled())
						log.debug("User modified successfully");
				}else{
					if(log.isDebugEnabled()){
						log.debug("User's current Organization and new Organization from Input are different, user will be deleted and recreated");
						log.debug("deleteUserHierarchy API Input is being prepared");
					}
					Document docDelUserIn=XMLUtil.createDocument(ELE_USER);
					Element eleDelUserIn=docDelUserIn.getDocumentElement();
					String strLoginId=employeDtlEle.getElementsByTagName(ELE_EMPLOYEE_ID).item(0).getTextContent().toString();
					eleDelUserIn.setAttribute(ATTR_LOGIN_ID, strLoginId);
					if(log.isDebugEnabled())
						log.debug("deleteUserHierarchy API Input prepared is: "+XMLUtil.getXMLString(docDelUserIn));	
					VSIUtils.invokeAPI(env, "deleteUserHierarchy", docDelUserIn);
					if(log.isDebugEnabled()){
						log.debug("deleteUserHierarchy API is invoked successfully");
						log.debug("User will be created now");
					}
					userHierarchyInXml = userHierarchyXml(env, employeDtlEle, storeNum);
					log.info("createUserHierarchy => "+XMLUtil.getXMLString(userHierarchyInXml));
					VSIUtils.invokeAPI(env, API_CREATE_USER_HIERARCHY, userHierarchyInXml);
					if(log.isDebugEnabled())
						log.debug("User created successfully");
				}
				//OMS-2968 Changes -- End				
			}		
		}
		catch(Exception e)
		{
			log.info("Exception within VSICreateUsers.createUsers() are as below => ");
			e.printStackTrace();
		}
		//OMS-2968 Changes -- Start
		if(log.isDebugEnabled())
			log.debug("Exiting VSICreateUsers class and createUsers method");
		//OMS-2968 Changes -- End
	}
	
	public Document userHierarchyXml(YFSEnvironment env, Element employeDtlEle, StringBuffer storeNum)
	{
		Document createUserInXml= null;
		try
		{
		createUserInXml=XMLUtil.createDocument(ELE_USER);
		Element userEle = createUserInXml.getDocumentElement();
		String empStatus = "";
		if (employeDtlEle.getElementsByTagName(ELE_EMPLOYEE_STATUS).item(0) != null)
		{
			empStatus = employeDtlEle.getElementsByTagName(ELE_EMPLOYEE_STATUS).item(0).getTextContent().toString();
			log.info("empStatus => "+empStatus);
			if(empStatus.equalsIgnoreCase("A") || empStatus.equalsIgnoreCase("L"))
				userEle.setAttribute(ATTR_ACTIVATE_FLAG, FLAG_Y);
			else if(empStatus.equalsIgnoreCase("T"))
				userEle.setAttribute(ATTR_ACTIVATE_FLAG, FLAG_N);
		}			
		userEle.setAttribute(ATTR_CALL_ORG_CODE, storeNum.toString());
		String displayUserId = "";
		if(employeDtlEle.getElementsByTagName(ELE_FIRST_NAME).item(0) != null && employeDtlEle.getElementsByTagName(ELE_MIDDLE_NAME).item(0) != null && employeDtlEle.getElementsByTagName(ELE_LAST_NAME).item(0).getTextContent().toString() != null)
			displayUserId = employeDtlEle.getElementsByTagName(ELE_FIRST_NAME).item(0).getTextContent().toString().concat(employeDtlEle.getElementsByTagName(ELE_MIDDLE_NAME).item(0).getTextContent().toString()).concat(employeDtlEle.getElementsByTagName(ELE_LAST_NAME).item(0).getTextContent().toString());
		else
		{
			if (employeDtlEle.getElementsByTagName(ELE_FIRST_NAME).item(0) != null)
				displayUserId = employeDtlEle.getElementsByTagName(ELE_FIRST_NAME).item(0).getTextContent().toString();
			if(employeDtlEle.getElementsByTagName(ELE_MIDDLE_NAME).item(0) != null)
				displayUserId = displayUserId.concat(employeDtlEle.getElementsByTagName(ELE_MIDDLE_NAME).item(0).getTextContent().toString());
			if(employeDtlEle.getElementsByTagName(ELE_LAST_NAME).item(0) != null)
				displayUserId = displayUserId.concat(employeDtlEle.getElementsByTagName(ELE_LAST_NAME).item(0).getTextContent().toString());
		}
		if(displayUserId.length() > 40)
		{
			displayUserId = employeDtlEle.getElementsByTagName(ELE_FIRST_NAME).item(0).getTextContent().toString().concat(employeDtlEle.getElementsByTagName(ELE_LAST_NAME).item(0).getTextContent().toString());
			if(displayUserId.length() > 40)
				displayUserId = displayUserId.substring(0, 39);
		}
		log.info("displayUserId => "+displayUserId);
		userEle.setAttribute(ATTR_DISPLAY_USER_ID, employeDtlEle.getElementsByTagName(ELE_EMPLOYEE_ID).item(0).getTextContent().toString());
		userEle.setAttribute(ATTR_LOCALE_CODE, ATTR_EN_US_EST);
		userEle.setAttribute(ATTR_LOGIN_ID, employeDtlEle.getElementsByTagName(ELE_EMPLOYEE_ID).item(0).getTextContent().toString());
		Document docGetCommonCodeList = XMLUtil.createDocument(ELEMENT_COMMON_CODE);
		Element eleCommonCode = docGetCommonCodeList.getDocumentElement();
		eleCommonCode.setAttribute(ATTR_ORG_CODE, ATTR_DEFAULT);
		eleCommonCode.setAttribute(ATTR_CODE_TYPE, LOAD_USER_COMMON_CODE_TYPE);
		docGetCommonCodeList = VSIUtils.invokeAPI(env, API_COMMON_CODE_LIST, docGetCommonCodeList);
		NodeList commonCode = docGetCommonCodeList.getElementsByTagName(ELEMENT_COMMON_CODE);			
		userEle.setAttribute(ATTR_PASSWORD, getLoadUserCommonCodeValues(commonCode, COMMON_CODE_PASSWORD));
		userEle.setAttribute(ATTR_MENU_ID, getLoadUserCommonCodeValues(commonCode, ATTR_MENU_ID));
		userEle.setAttribute(ATTR_ORG_KEY, storeNum.toString());
		userEle.setAttribute(ATTR_SESSION_TIMEOUT, getLoadUserCommonCodeValues(commonCode, ATTR_SESSION_TIMEOUT));
		userEle.setAttribute(ATTR_THEME, getLoadUserCommonCodeValues(commonCode, ATTR_THEME));
		userEle.setAttribute(ATTR_USERNAME, displayUserId);
		Element queueSubList = SCXmlUtil.createChild(userEle, ELE_QUEUE_SUB_LIST);
		queueSubList.setAttribute(ATTR_RESET, FLAG_Y);
		Element queueSub = SCXmlUtil.createChild(queueSubList, ELE_QUEUE_SUBSCRIPTION);
		queueSub.setAttribute(ATTR_QUEUE_KEY, ATTR_QUEUE_KEY_VALUE.concat(storeNum.toString()));
		Element userGpLists = SCXmlUtil.createChild(userEle, ELE_USER_GROUP_LISTS);
		userGpLists.setAttribute(ATTR_RESET, FLAG_Y);
		Element userGpList = SCXmlUtil.createChild(userGpLists, ELE_USER_GROUP_LIST);
		userGpList.setAttribute(ATTR_USER_GROUP_DESC, getLoadUserCommonCodeValues(commonCode, ATTR_USER_GROUP_NAME));
		userGpList.setAttribute(ATTR_USER_GROUP_ID, getLoadUserCommonCodeValues(commonCode, ATTR_USER_GROUP_ID));
		userGpList.setAttribute(ATTR_USER_GROUP_KEY, getLoadUserCommonCodeValues(commonCode, ATTR_USER_GROUP_KEY));
		userGpList.setAttribute(ATTR_USER_GROUP_NAME, getLoadUserCommonCodeValues(commonCode, ATTR_USER_GROUP_NAME));
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return createUserInXml;
	}
	
	public String getLoadUserCommonCodeValues(NodeList commonCode,String commonCodeValue)
	{
		String commonCodeVal = null;
		for (int j = 0; j < commonCode.getLength(); j++) 
		{
			Element commonCodeElem = (Element) commonCode.item(j);
			if(commonCodeElem.getAttribute(ATTR_CODE_VALUE).equalsIgnoreCase(commonCodeValue))
				commonCodeVal = commonCodeElem.getAttribute(ATTR_CODE_SHORT_DESCRIPTION);
		}
		return commonCodeVal;
	}
}