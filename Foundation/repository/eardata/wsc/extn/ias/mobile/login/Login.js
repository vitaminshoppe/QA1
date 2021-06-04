scDefine(["scbase/loader!dojo/_base/lang",
          "scbase/loader!dojo/_base/array",
          "scbase/loader!dojo/_base/connect",
          "scbase/loader!dojo/keys",
          "scbase/loader!dojo/aspect",
          "scbase/loader!dojo/dom",
          "scbase/loader!dojox/html/entities",
          "scbase/loader!dijit/registry",
          "scbase/loader!idx/mobile/Login",
          "scbase/loader!sc/plat/dojo/utils/BundleUtils",
		  "scbase/loader!ias/utils/ContextUtils",
		  "scbase/loader!sc/plat/dojo/utils/BaseUtils",
          "scbase/loader!ias",
          "scbase/loader!idx/resources",
		  "scbase/loader!idx/app/nls/LoginFrame",
          ],
function(dLang, dArray, dConnect, dKeys, dAspect, dDom, dHtmlEntities, dRegistry, idxMobileLoginDiag, scBundleUtils,_iasContextUtils, _scBaseUtils, ias, iResources){
	
	var dLogin = dLang.getObject("login.Login", true, ias);
	
	var loginFrameId = "login";
	var userNameInputFieldId = loginFrameId+"UserName";
	var passwordInputFieldId = loginFrameId+"Password";
	/*sessionStorage.setItem("LoginID", "ds");
	sessionStorage.setItem("Password", "password");
	_iasContextUtils.addToContext("LoginID", "ds");
	_iasContextUtils.addToContext("Password", "password"); */
	//add different values to the resource bundle later - extended
	dLogin.init = function(errorMessage){		
		/*var loginID = _iasContextUtils.getFromContext("LoginID");
		var loginPassword = _iasContextUtils.getFromContext("Password"); */
		var loginIDSession = sessionStorage.getItem("LoginID");
		var loginPasswordSession = sessionStorage.getItem("Password");
		if(!_scBaseUtils.isVoid(loginIDSession) && !_scBaseUtils.isVoid(loginPasswordSession))
		{
			dDom.byId("sc_plat_dojo_widgets_ScreenDialogUnderlay_Initial").style.display="block";
			dDom.byId("displayUserId").value = loginIDSession;
			dDom.byId("password").value = loginPasswordSession;
			/*_iasContextUtils.addToContext("LoginID", " ");
			_iasContextUtils.addToContext("Password", " ");*/
			sessionStorage.setItem("LoginID", "");
			sessionStorage.setItem("Password", "");
			dDom.byId("fieldsForm").submit();
		}
		
		var loginFrame = new idxMobileLoginDiag({
			//id: loginFrameId,
			legal: scBundleUtils.getString("copyright_mobile"),
			//inactivityMessage: scBundleUtils.getString("inactivity_message"),
			name: scBundleUtils.getString("login_title"),
			//loginSubTitle: scBundleUtils.getString("login_sub_title"),
			//labelUserName: scBundleUtils.getString("login_User_ID"),
			//labelPassword: scBundleUtils.getString("login_Password"),
			//labelSubmitButton: scBundleUtils.getString("login_Submit"),
			onLogin: function(args){
				if (!args.name || !args.password){
					var emptyFieldsMessage = iResources.getResources("idx/app/LoginFrame", this.lang).invalidMessage;
				    loginFrame.showMessage(emptyFieldsMessage);
				}
				else{
					dDom.byId("sc_plat_dojo_widgets_ScreenDialogUnderlay_Initial").style.display="block";
					dDom.byId("displayUserId").value = args.name;
					dDom.byId("password").value = args.password;
					dDom.byId("fieldsForm").submit();
				}
			},
			modal:false,
			cancelable:false
		
		}, "loginFrame");

		loginFrame._nameEditor.textbox.setAttribute("autocapitalize", "off");
		loginFrame._nameEditor.textbox.setAttribute("autocorrect", "off");
		
//		dConnect.connect(dRegistry.byId(""));
		
		dArray.forEach([dRegistry.byId(userNameInputFieldId), dRegistry.byId(passwordInputFieldId)], function(node, index, array){
			dConnect.connect(node, "onKeyUp", function(event){
				if(event.keyCode === dojo.keys.ENTER){
					dRegistry.byId("loginButton").focus();
					loginFrame._onSubmitClick(event);
				}
			});
		}, null);

		if(scBundleUtils.hasBundleKey(errorMessage)){
			var localizederrorMessage = scBundleUtils.getString(errorMessage);
			loginFrame.showMessage(localizederrorMessage === errorMessage ? dHtmlEntities.encode(errorMessage) : localizederrorMessage);
			
			
			// wait for dialog to close to revalidate the form and focus the first bad field
			//var self = loginFrame;
			//dialogHandle = dAspect.after(loginFrame.invalidLoginDialog, "onHide", function() {
			//   self.invalidMessageNode.innerHTML = self.invalidMessage;
			//   self.loginUserName.focus();
			//   if (dialogHandle) dialogHandle.remove();
			//});
		}
		
		//loginFrame.loginUserName.focus();
		loginFrame.show();
	};
	
	return dLogin;
});
