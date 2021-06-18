package com.vsi.som.api;

import java.rmi.RemoteException;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.vsi.oms.utils.VSIConstants;
import com.vsi.oms.utils.VSIUtils;
import com.vsi.oms.utils.XMLUtil;
import com.yantra.interop.japi.YIFClientCreationException;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;

public class VSICreateUsers implements VSIConstants
{
	private YFCLogCategory log = YFCLogCategory.instance(VSICreateUsers.class);
	//OMS-3600 Changes -- Start
	private static final String TAG = VSICreateUsers.class.getSimpleName();
	//OMS-3600 Changes -- End
	public void createUsers(YFSEnvironment env, Document inXml)
	{
		//OMS-3600 Changes -- Start
		printLogs("================Inside VSICreateUsers Class and createUsers Method================================");
		printLogs("Input for VSICreateUsers.createUsers => "+XMLUtil.getXMLString(inXml));
		//OMS-3600 Changes -- End
		try
		{
			Document getUserInXml=XMLUtil.createDocument(ELE_USER);
			Element userElem = getUserInXml.getDocumentElement();
			Element employeDtlEle = (Element) inXml.getElementsByTagName(ELE_EMPLOYEE_DETAIL).item(0);
			String storeNo = employeDtlEle.getElementsByTagName(ELE_STORE_NUM).item(0).getTextContent();		//OMS-3600 Change
			int i=0;
			while(i < storeNo.length() && storeNo.charAt(i) == '0')
				i++;
			StringBuilder storeNum = new StringBuilder(storeNo);		//OMS-3600 Change
			storeNum.replace(0, i, "");
			printLogs("storeNo => "+storeNo+" storeNum => "+storeNum);		//OMS-3600 Change			
			userElem.setAttribute(ATTR_LOGIN_ID, employeDtlEle.getElementsByTagName(ELE_EMPLOYEE_ID).item(0).getTextContent());		//OMS-3600 Change
			//OMS-2968 Changes -- Start
			printLogs("Input for getUserList API: "+XMLUtil.getXMLString(getUserInXml));		//OMS-3600 Change		
			//OMS-2968 Changes -- End
			Document getUserListOutXml = VSIUtils.invokeAPI(env, API_GET_USER_LIST, getUserInXml);
			printLogs("getUserListOutXml => "+XMLUtil.getXMLString(getUserListOutXml));		//OMS-3600 Change
			Element userList = getUserListOutXml.getDocumentElement();
			Document userHierarchyInXml;
			if(!userList.hasChildNodes())
			{	
				//OMS-2968 Changes -- Start
				printLogs("New user");		//OMS-3600 Change
				//OMS-2968 Changes -- End
				userHierarchyInXml = userHierarchyXml(env, employeDtlEle, storeNum);
				printLogs("createUserHierarchy Input XML=> "+XMLUtil.getXMLString(userHierarchyInXml));		//OMS-3600 Change
				VSIUtils.invokeAPI(env, API_CREATE_USER_HIERARCHY, userHierarchyInXml);
				//OMS-2968 Changes -- Start
				printLogs("createUserHierarchy API Invoked successfully");		//OMS-3600 Change
				//OMS-2968 Changes -- End
			}
			else
			{
				//OMS-2968 Changes -- Start				
				printLogs("Existing user");		//OMS-3600 Change
				//OMS-3600 Changes -- Start
				String empStatus = "";
				if (employeDtlEle.getElementsByTagName(ELE_EMPLOYEE_STATUS).item(0) != null)
				{
					empStatus = employeDtlEle.getElementsByTagName(ELE_EMPLOYEE_STATUS).item(0).getTextContent();
				}
				if(!YFCCommon.isVoid(empStatus) && "T".equals(empStatus)) {
					printLogs("Employee is terminated and hence the user will be deleted from OMS");
					deleteUser(env, employeDtlEle);
				}						
				else {
					printLogs("Non-Termination scenario");
					//OMS-3600 Changes -- End
					Element eleUser=SCXmlUtil.getChildElement(userList, ELE_USER);
					String strOrgKey=eleUser.getAttribute(ATTR_ORG_KEY);  
					String strStrIn=storeNum.toString();
					if(strStrIn.equals(strOrgKey)){
						printLogs("User's current Organization and new Organization from Input are same, modifyUserHierarchy API will be invoked");		//OMS-3600 Change
						userHierarchyInXml = userHierarchyXml(env, employeDtlEle, storeNum);
						printLogs("modifyUserHierarchy Input XML=> "+XMLUtil.getXMLString(userHierarchyInXml));		//OMS-3600 Change
						VSIUtils.invokeAPI(env, API_MOD_USER_HIERARCHY, userHierarchyInXml);
						printLogs("User modified successfully");		//OMS-3600 Change
					}else{
						//OMS-3600 Changes -- Start
						printLogs("User's current Organization and new Organization from Input are different, user will be deleted and recreated");
						printLogs("deleteUserHierarchy API Input is being prepared");						
						deleteUser(env, employeDtlEle);
						//OMS-3600 Changes -- End
						userHierarchyInXml = userHierarchyXml(env, employeDtlEle, storeNum);
						printLogs("createUserHierarchy Input XML=> "+XMLUtil.getXMLString(userHierarchyInXml));		//OMS-3600 Change
						VSIUtils.invokeAPI(env, API_CREATE_USER_HIERARCHY, userHierarchyInXml);
						printLogs("User created successfully");		//OMS-3600 Change
					}
					//OMS-2968 Changes -- End
				//OMS-3600 Changes -- Start
				}
				//OMS-3600 Changes -- End
			}		
		}
		catch(Exception e)
		{
			//OMS-3600 Changes -- Start
			printLogs("Exception in VSICreateUsers.createUsers() are as below => ");
			printLogs("The exception is [ "+ e.getMessage() +" ]");
			//OMS-3600 Changes -- End
		}
		//OMS-2968 Changes -- Start
		printLogs("Exiting VSICreateUsers class and createUsers method");		//OMS-3600 Change
		//OMS-2968 Changes -- End
	}
	//OMS-3600 Changes -- Start
	private void deleteUser(YFSEnvironment env, Element employeDtlEle)
			throws ParserConfigurationException, YIFClientCreationException, RemoteException {
		
		printLogs("Inside deleteUser method");
		Document docDelUserIn=XMLUtil.createDocument(ELE_USER);
		Element eleDelUserIn=docDelUserIn.getDocumentElement();
		String strLoginId=employeDtlEle.getElementsByTagName(ELE_EMPLOYEE_ID).item(0).getTextContent();
		eleDelUserIn.setAttribute(ATTR_LOGIN_ID, strLoginId);
		
		printLogs("deleteUserHierarchy API Input prepared is: "+XMLUtil.getXMLString(docDelUserIn));	
		VSIUtils.invokeAPI(env, "deleteUserHierarchy", docDelUserIn);
		printLogs("deleteUserHierarchy API is invoked successfully");
		
		printLogs("Exiting deleteUser method");
		
	}
	//OMS-3600 Changes -- End
	public Document userHierarchyXml(YFSEnvironment env, Element employeDtlEle, StringBuilder storeNum)		//OMS-3600 Change
	{
		//OMS-3600 Changes -- Start
		printLogs("Inside userHierarchyXml method");
		//OMS-3600 Changes -- End
		Document createUserInXml= null;
		try
		{
		createUserInXml=XMLUtil.createDocument(ELE_USER);
		Element userEle = createUserInXml.getDocumentElement();
		String empStatus = "";
		if (employeDtlEle.getElementsByTagName(ELE_EMPLOYEE_STATUS).item(0) != null)
		{
			empStatus = employeDtlEle.getElementsByTagName(ELE_EMPLOYEE_STATUS).item(0).getTextContent();		//OMS-3600 Change
			printLogs("empStatus => "+empStatus);		//OMS-3600 Change
			if(empStatus.equalsIgnoreCase("A") || empStatus.equalsIgnoreCase("L"))
				userEle.setAttribute(ATTR_ACTIVATE_FLAG, FLAG_Y);			
		}			
		userEle.setAttribute(ATTR_CALL_ORG_CODE, storeNum.toString());
		String displayUserId = "";
		//OMS-3600 Changes -- Start
		displayUserId = setDisplayUserId(employeDtlEle, displayUserId);
		//OMS-3600 Changes -- End
		printLogs("displayUserId => "+displayUserId);		//OMS-3600 Change
		userEle.setAttribute(ATTR_DISPLAY_USER_ID, employeDtlEle.getElementsByTagName(ELE_EMPLOYEE_ID).item(0).getTextContent());		//OMS-3600 Change
		userEle.setAttribute(ATTR_LOCALE_CODE, ATTR_EN_US_EST);
		userEle.setAttribute(ATTR_LOGIN_ID, employeDtlEle.getElementsByTagName(ELE_EMPLOYEE_ID).item(0).getTextContent());		//OMS-3600 Change
		Document docGetCommonCodeList = XMLUtil.createDocument(ELEMENT_COMMON_CODE);
		Element eleCommonCode = docGetCommonCodeList.getDocumentElement();
		eleCommonCode.setAttribute(ATTR_ORG_CODE, ATTR_DEFAULT);
		eleCommonCode.setAttribute(ATTR_CODE_TYPE, LOAD_USER_COMMON_CODE_TYPE);
		//OMS-3600 Changes -- Start
		printLogs("getCommonCodeList API Input prepared is: "+XMLUtil.getXMLString(docGetCommonCodeList));
		//OMS-3600 Changes -- End
		Document docGetCommonCodeListOut = VSIUtils.invokeAPI(env, API_COMMON_CODE_LIST, docGetCommonCodeList);
		//OMS-3600 Changes -- Start
		printLogs("getCommonCodeList API output is: "+XMLUtil.getXMLString(docGetCommonCodeListOut));
		//OMS-3600 Changes -- End
		NodeList commonCode = docGetCommonCodeListOut.getElementsByTagName(ELEMENT_COMMON_CODE);		//OMS-3600 Change			
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
			//OMS-3600 Changes -- Start
			printLogs("Exception in VSICreateUsers.userHierarchyXml() are as below => ");
			printLogs("The exception is [ "+ e.getMessage() +" ]");
			//OMS-3600 Changes -- End
		}
		//OMS-3600 Changes -- Start
		printLogs("Exiting userHierarchyXml method");
		//OMS-3600 Changes -- End
		return createUserInXml;
	}
	//OMS-3600 Changes -- Start
	private String setDisplayUserId(Element employeDtlEle, String displayUserId) {
		
		printLogs("Inside setDisplayUserId method");
		
		if(employeDtlEle.getElementsByTagName(ELE_FIRST_NAME).item(0) != null && employeDtlEle.getElementsByTagName(ELE_MIDDLE_NAME).item(0) != null && employeDtlEle.getElementsByTagName(ELE_LAST_NAME).item(0).getTextContent() != null)		//OMS-3600 Change
			displayUserId = employeDtlEle.getElementsByTagName(ELE_FIRST_NAME).item(0).getTextContent().concat(employeDtlEle.getElementsByTagName(ELE_MIDDLE_NAME).item(0).getTextContent()).concat(employeDtlEle.getElementsByTagName(ELE_LAST_NAME).item(0).getTextContent());		//OMS-3600 Change
		else
		{
			if (employeDtlEle.getElementsByTagName(ELE_FIRST_NAME).item(0) != null)
				displayUserId = employeDtlEle.getElementsByTagName(ELE_FIRST_NAME).item(0).getTextContent();		//OMS-3600 Change
			if(employeDtlEle.getElementsByTagName(ELE_MIDDLE_NAME).item(0) != null)
				displayUserId = displayUserId.concat(employeDtlEle.getElementsByTagName(ELE_MIDDLE_NAME).item(0).getTextContent());		//OMS-3600 Change
			if(employeDtlEle.getElementsByTagName(ELE_LAST_NAME).item(0) != null)
				displayUserId = displayUserId.concat(employeDtlEle.getElementsByTagName(ELE_LAST_NAME).item(0).getTextContent());		//OMS-3600 Change
		}
		if(displayUserId.length() > 40)
		{
			displayUserId = employeDtlEle.getElementsByTagName(ELE_FIRST_NAME).item(0).getTextContent().concat(employeDtlEle.getElementsByTagName(ELE_LAST_NAME).item(0).getTextContent());		//OMS-3600 Change
			if(displayUserId.length() > 40)
				displayUserId = displayUserId.substring(0, 39);
		}
		
		printLogs("Exiting setDisplayUserId method");
		
		return displayUserId;
	}
	//OMS-3600 Changes -- End
	public String getLoadUserCommonCodeValues(NodeList commonCode,String commonCodeValue)
	{
		//OMS-3600 Changes -- Start
		printLogs("Inside getLoadUserCommonCodeValues method");
		printLogs("commonCodeValue from Input: "+commonCodeValue);
		//OMS-3600 Changes -- End
		String commonCodeVal = null;
		for (int j = 0; j < commonCode.getLength(); j++) 
		{
			Element commonCodeElem = (Element) commonCode.item(j);
			if(commonCodeElem.getAttribute(ATTR_CODE_VALUE).equalsIgnoreCase(commonCodeValue))
				commonCodeVal = commonCodeElem.getAttribute(ATTR_CODE_SHORT_DESCRIPTION);
		}
		//OMS-3600 Changes -- Start
		printLogs("commonCodeVal being returned: "+commonCodeVal);
		printLogs("Exiting getLoadUserCommonCodeValues method");
		//OMS-3600 Changes -- End
		return commonCodeVal;
	}
	//OMS-3600 Changes -- Start
	private void printLogs(String mesg) {
		if(log.isDebugEnabled()){
			log.debug(TAG +" : "+mesg);
		}
	}
	//OMS-3600 Changes -- End
}