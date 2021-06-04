<%
// Licensed Materials - Property of IBM
// IBM Sterling Order Management Store (5725-D10)
// (C) Copyright IBM Corp. 2014 , 2015 All Rights Reserved. , 2015 All Rights Reserved.
// US Government Users Restricted Rights - Use, duplication or disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@page import="com.ibm.wsc.core.WSCConstants"%>
<%@page import="com.sterlingcommerce.security.dv.SCEncoder"%>
<%@page import="com.sterlingcommerce.ui.web.framework.utils.SCUIContextHelper"%>
<%@page import="com.sterlingcommerce.ui.web.framework.context.SCUIContext"%>
<%@page import="com.sterlingcommerce.ui.web.framework.utils.SCUIJSONUtils"%>
<%@page import="com.sterlingcommerce.ui.web.framework.helpers.SCUILocalizationHelper"%>
<%@page import="com.sterlingcommerce.ui.web.framework.utils.SCUIUtils"%>
<%@page import="org.w3c.dom.Element"%>
<%@page import="org.w3c.dom.Document"%>
<%@page import="com.ibm.sterling.afc.dojo.util.ProfileUtil" %>
<%@ taglib uri="/WEB-INF/scui.tld" prefix="scuitag" %>
<%@ taglib uri="/WEB-INF/scuiimpl.tld" prefix="scuiimpltag" %>
<%@ page import="java.util.Locale "%>

<%

	response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
	response.setHeader("Pragma", "no-cache"); // HTTP 1.0.
	response.setDateHeader("Expires", 0);
			
	String enterpriseCode = request.getParameter(WSCConstants.LOGIN_ENTERPRISE_CODE);
	String landingpage = request.getParameter(WSCConstants.LANDING_PAGE_REQUEST_PARAMETER_NAME);
	String storeId = request.getParameter(WSCConstants.LOGIN_STORE_ID);
	String storeLocaleCode = request.getParameter(WSCConstants.LOGIN_STORE_LOCALE_CODE);
	
	if(SCUIUtils.isVoid(enterpriseCode)){
		enterpriseCode="";
	}
	if(SCUIUtils.isVoid(landingpage)){
		landingpage="";
	}
	if(SCUIUtils.isVoid(storeId)){
		storeId="";
	}
	if(SCUIUtils.isVoid(storeLocaleCode)){
		storeLocaleCode="";
	}
	
	SCUIContext uiContext = SCUIContextHelper.getUIContext(request, response);
	String localeCode = "";
	if(!SCUIUtils.isVoid(uiContext)){
	 localeCode = uiContext.getUserPreferences().getLocale().getLocaleCode();
	}
	if(SCUIUtils.isVoid(localeCode) && !SCUIUtils.isVoid(request.getParameter("LocaleCode"))  ){
		localeCode = request.getParameter("LocaleCode");  
	}else if(SCUIUtils.isVoid(localeCode)){
		localeCode = request.getLocale().toString();
	}
	if(!SCUIUtils.isVoid(storeLocaleCode)){
		localeCode = storeLocaleCode;
	}
	
	
	localeCode = localeCode.toString().replace('_', '-').toLowerCase();

	//Strip the -Ext at the end of the locale.
	//If the locale string contains a - then take the first 5 letters xx-xx
	//Else leave it as is to avoid  
	if (localeCode.length()>5 && localeCode.contains("-"))
		localeCode=localeCode.substring(0,5);

	response.setContentType("text/html;charset=UTF-8");
	String errorMsg = request.getParameter("ErrorMsg");
	if(SCUIUtils.isVoid(errorMsg)){
		errorMsg = (String)uiContext.getAttribute("ERROR_MESSAGE");
	}
	
	if(SCUIUtils.isVoid(errorMsg)){
		errorMsg = "";
	}

	Cookie storeCookie = new Cookie (WSCConstants.LOGIN_STORE_ID,storeId);	
	Cookie localeCodeCookie = new Cookie (WSCConstants.LOGIN_STORE_LOCALE_CODE,storeLocaleCode);
	
	storeCookie.setPath(request.getContextPath());
	localeCodeCookie.setPath(request.getContextPath());
	
	response.addCookie(storeCookie);
	response.addCookie(localeCodeCookie);
	
	
	String storeID = request.getParameter("StoreID");
%>
<html lang="<%=SCEncoder.getEncoder().encodeForJavaScript(localeCode)%>"> 
<head>
<meta http-equiv="Content-type" content="text/html; charset=utf-8">
<meta name="apple-mobile-web-app-capable" content="yes" />
<meta name="viewport" content="initial-scale=1.0, user-scalable=yes" />

	<title><%= SCUILocalizationHelper.getString(uiContext, "WSC_Product_Name")%></title>
	<link rel="SHORTCUT ICON" href="<%=request.getContextPath()%>/wsc/resources/css/icons/images/IBMLogoWindow.ico"/>

	<style>
			.isccsLoginError{
				margin-top: 10px;
				color: #FF0000;
				font-weight: bold;
			}

	</style>

<script type="text/javascript">
			var contextPath = '<%=request.getContextPath()%>';
			var dojoConfig = {
			    async:true,
			    locale:'<%=SCEncoder.getEncoder().encodeForJavaScript(localeCode)%>',
			    waitSeconds:5,
			    packages : [ {
						name : 'sc/plat',
						location :contextPath + "/platform/scripts/sc/plat"
					},
					{
						name : 'idx',
						location : contextPath + "/ibmjs/idx"
					},
					{
						name : 'scbase',
						location : contextPath + "/platform/scripts/sc/base"
					},					
					{
						name : 'wsc',
						location : contextPath + "/wsc"
					},
					{
						name : 'ias',
						location : contextPath + "/ias"
					},
					{
						name : 'gridx',
						location : contextPath + '/dojo/gridx'
					},
					{
						name : 'dojox',
						location : contextPath + '/dojo/dojox'
					},
					{
						name : 'dijit',
						location : contextPath + '/dojo/dijit'
					}

				]
			};
			dojoConfig.paths = {"sc/plat":contextPath+"/platform/scripts/sc/plat"};
</script>


<script src="<%=request.getContextPath()%>/ibmjs/idx/mobile/deviceTheme.js"></script>
<script src="<%=request.getContextPath()%>/dojo/dojo/dojo.js"></script>
<script src="<%=request.getContextPath()%>/platform/scripts/sc/plat/dojo/base/loader.js"></script>

	<style>
	
		.wscLoginForm{
			display : none;
		}
		.oneui.mobile body,
		.mblSimpleDialogDecoration {
			background-image: url('<%=request.getContextPath()%>/wsc/resources/css/icons/images/Mobile_Login_Background.png');
		}
		.dj_phone .mblIdxLoginDialog {
			left: 0 !important;
			bottom: 0px;
			font-family: "HelvNeueRomanforIBM", "Helvetica Neue", Helvetica, Arial, Tahoma, Verdana, sans-serif;
		}

		.dj_tablet .mblIdxLoginDialog {
			box-shadow: none;
		}		
				
		.comapps .mblIdxLoginPane {
			text-align: center;
			padding: 15px;
			display: inline-block;
			vertical-align: middle;
		}
		.comapps .mblIdxLoginDialog .mblIdxIbmLogo {
			display: inline-block;
			background-image: url('<%=request.getContextPath()%>/wsc/resources/css/icons/images/vendorLogoWhite.png');
		}

		.comapps .mblIdxLoginDialog .mblIdxProductName {		
			font-family: "HelvNeueRomanforIBM", "Helvetica Neue", Helvetica, sans-serif, Arial, Tahoma, Verdana;
			font-size: 28px;
			color: #FFF;
		}
		
		.comapps .mblIdxLoginDialog .mblIdxEdit {
			margin: 5px 0px;
		}
		
		
		.mobile .comapps .mblIdxLoginDialog {
			border: none;			
		}
		
		.comapps .mblIdxLoginDialog .mblIdxEdit .mblTextBox {
			height: 35px;
			width: 285px;
			background-color: #009dc2;
			border: none;
			padding-left: 35px;
			color: #FFF;
			font-size: 18px;
			text-align: left;
		}
		

		.comapps .mblIdxLoginDialog .mblIdxButtons .mblButton {
			height: 35px;
			width: 285px;
			background: transparent;
			border: 1px solid #FFF;	
			text-align: center;
			margin: 0px;
			border-radius: 0px;
		}

		::-webkit-input-placeholder { /* WebKit browsers */
			color: #C2BBBB;		    
		}
		:-moz-placeholder { /* Mozilla Firefox 4 to 18 */
			color: #C2BBBB;		    
		}
		::-moz-placeholder { /* Mozilla Firefox 19+ */
			color: #C2BBBB;	    
		}
		:-ms-input-placeholder { /* Internet Explorer 10+ */
			color: #C2BBBB; 
		}
		
		input[type="password"] {
			background-image: url('<%=request.getContextPath()%>/wsc/resources/css/icons/images/password_white.png');
			background-repeat: no-repeat;		
		}

		input[type="text"] {
			background-image: url('<%=request.getContextPath()%>/wsc/resources/css/icons/images/user_white.png');
			background-repeat: no-repeat;		
		}
		
		.comapps .mblIdxLoginDialog .mblIdxDivider {
			display: none;
		}
		
		.comapps .mblIdxLoginDialog .mblIdxLegal {
			color: #FFF;
			font-size: 10px;
			font-family: "HelvNeueRomanforIBM", "Helvetica Neue", Helvetica, sans-serif, Arial, Tahoma, Verdana;
			margin-top:60px;
		}
		
		
		.dijitDialogUnderlayWrapperLogin .dijitDialogUnderlay {
			background-color: #4c4d53;
			//opacity: 0;
		}

		.scMask {
			background-repeat: no-repeat;
			background-position: center;	
			opacity: 0.5;
		}

		.scLoadingIconAndTextWrapper {
			position: absolute;
		}

		.scLoadingIcon {
			background-image: url('<%=request.getContextPath()%>/platform/scripts/sc/plat/dojo/themes/claro/images/loading_big.gif');
			background-repeat: no-repeat;
			background-position: 0px;
			height: 50px;
			width: 50px;
		}

		.scLoadingText {
			/*Make it so small that it is not visible.*/
			font-size: 0px;
		}
	

	</style>


</head>
<body class="comapps">

<div role="main" aria-label="Login Frame" aria-live="assertive">
	<div id="loginFrame"/>
	<% /* change processLogin.do to a new action after platform change is made */ %>
	<form id="fieldsForm"  class="wscLoginForm" method="POST" action="processLogin.do" aria-live="assertive">
		<input id="displayUserId" name="DisplayUserID" type="hidden" />
		<input id="password" name="Password" type="password" />
		<input id="enterpriseCode" name="EnterpriseCode" type="hidden" value='<%=SCEncoder.getEncoder().encodeForJavaScript(enterpriseCode)%>'/>
		<input id="landingpage" name="landingpage" type="hidden" value='<%=SCEncoder.getEncoder().encodeForJavaScript(landingpage)%>'/>
		<input id="StoreId" name="StoreId" type="hidden" value='<%=SCEncoder.getEncoder().encodeForJavaScript(storeId)%>'/>
		<input id="StoreLocaleCode" name="StoreLocaleCode" type="hidden" value='<%=SCEncoder.getEncoder().encodeForJavaScript(storeLocaleCode)%>'/>
		<input id="logintype" name="logintype" type="hidden" value='mobile'/>
	</form>
</div>
<div class="dijitDialogUnderlayWrapperLogin" role="alert" 
	id="sc_plat_dojo_widgets_ScreenDialogUnderlay_Initial" 
	widgetid="sc_plat_dojo_widgets_ScreenDialogUnderlay_Initial" 
	style="display: block; z-index: 999999; top: 0px; left: 0px; position: fixed; width: 100%; height: 100%;">
	<div class="dijitDialogUnderlay scMask" dojoattachpoint="node" role="alert" 
		id="dialogId_underlay" style="width: 100%; height: 100%; top: -50px; left: -50px; text-align: center; opacity: 0.4">
	</div>
	<div style="width: 100%; height: 100%; top: -25px; left: -25px; position:absolute">
		<div class="scLoadingIconAndTextWrapper" dojoattachpoint="imageAndTextHolderNode" role="alert" style="top: 50%; left: 50%;">    
			<div class="scLoadingIcon" dojoattachpoint="loadingIconNode"> </div> 
			<div class="scLoadingText" dojoattachpoint="loadingTextNode" role="alert">&nbsp;<%= SCUILocalizationHelper.getString(uiContext, "Processing")%></div>    </div> 
		<iframe src='javascript:""' class="dijitBackgroundIframe" role="presentation" tabindex="-1" style="opacity: 0.1; width: 100%; height: 100%; border: none;"></iframe>
	</div>
</div>
</body>
<script>

require(["scbase/loader!dojo/dom",
"scbase/loader!extn/ias/mobile/login/LoginExtn", 
"scbase/loader!sc/plat/dojo/utils/BundleUtils", 
"dojox/mobile",
"dojox/mobile/compat"],
	function(dDom, iasMobLogin, scBundleUtils, mobile, compat){
		scBundleUtils.registerGlobalBundles("ias.resources.bundle","ias_login_bundle");
		scBundleUtils.registerGlobalBundles("wsc.resources.bundle","wsc_login_bundle");
		dDom.byId("sc_plat_dojo_widgets_ScreenDialogUnderlay_Initial").style.display="none";
		dDom.byId("dialogId_underlay").style.opacity ="0";
		iasMobLogin.init('<%=SCEncoder.getEncoder().encodeForJavaScript(errorMsg)%>');
		
		<% if(!SCUIUtils.isVoid(request.getParameter("EnterpriseCode"))) { %>
			dDom.byId("enterpriseCode").value = '<%=SCEncoder.getEncoder().encodeForJavaScript(request.getParameter("EnterpriseCode"))%>';
		<% } %>
	}
);

</script>
</html>