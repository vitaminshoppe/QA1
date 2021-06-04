/*
 * Licensed Materials - Property of IBM
 * IBM Sterling Order Management Store (5725-D10)
 *(C) Copyright IBM Corp. 2014 , 2015 All Rights Reserved. , 2015 All Rights Reserved.
 * US Government Users Restricted Rights - Use, duplication or disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 */
scDefine([
	"scbase/loader!dojo/_base/lang",	
	"scbase/loader!ias",		
	"scbase/loader!dojo/dom-class",
	"scbase/loader!dojo/dom-attr",
	"scbase/loader!dijit/registry",
	"scbase/loader!dojo/window",
	"scbase/loader!sc/plat/dojo/widgets/Screen",
	"scbase/loader!sc/plat/dojo/utils/BaseUtils",
	"scbase/loader!sc/plat/dojo/utils/BundleUtils",
	"scbase/loader!sc/plat/dojo/utils/ScreenUtils",
	"scbase/loader!sc/plat/dojo/utils/ModelUtils",	
	"scbase/loader!ias/utils/UIUtils",	
	"scbase/loader!ias/utils/ContextUtils",
	"scbase/loader!sc/plat/dojo/utils/WidgetUtils",
	"scbase/loader!sc/plat/dojo/utils/EventUtils",	
	"scbase/loader!sc/plat/dojo/utils/ControllerUtils",	
	"scbase/loader!ias/utils/BaseTemplateUtils",
	"scbase/loader!sc/plat/dojo/utils/Util",
	"scbase/loader!sc/plat/dojo/info/ApplicationInfo",
	"scbase/loader!sc/plat/dojo/utils/RepeatingPanelUtils",
	"scbase/loader!dojo/NodeList-manipulate"
	],
	function(dLang,ias,dDomClass,dDomAttr,dregistry,dWindow,scScreen,scBaseUtils,scBundleUtils,scScreenUtils,scModelUtils,iasUIUtils,iasContextUtils,scWidgetUtils,scEventUtils,scControllerUtils,iasBaseTemplateUtils,scUtil,scApplicationInfo,scRepeatingPanelUtils){
		var screenUtils = dLang.getObject("utils.ScreenUtils", true,ias);
		
		
		screenUtils.showErrorMessageBoxWithOk = function(screen,errorBundleKey){
			var errorMsg = scScreenUtils.getString(screen,errorBundleKey);
			scScreenUtils.showErrorMessageBox(screen,errorMsg,null,iasUIUtils.getTextOkObjectForMessageBox(screen),null);
				
		};
		
		screenUtils.isPageResultLessThanPageSize = function(args){
			
			var isFirstPage = null, isLastPage = null, result = null, hidePaginationPanel = false;
			
			result = scBaseUtils.getModelValueFromBean("result", args);
			isFirstPage = scModelUtils.getStringValueFromPath("Page.IsFirstPage", result);
			isLastPage = scModelUtils.getStringValueFromPath("Page.IsLastPage", result);
			
			if(!scBaseUtils.isVoid(isFirstPage) && !scBaseUtils.isVoid(isLastPage) && scBaseUtils.equals(isFirstPage,"Y") && scBaseUtils.equals(isLastPage,"Y")) {
				hidePaginationPanel = true;
			}
			
			return hidePaginationPanel;
			
		};

		screenUtils.scrollTopWithFixedHeader = function(screen, uId, extraOffset, fixedHeaderHeight){
			
			var widget = screen.getWidgetByUId(uId);
			if(widget!=null || widget != undefined) {
				scScreenUtils.scrollToWidget(screen, widget.uId);
				
				var node = widget.get("domNode");
				screenUtils.scrollTopWithOffest(node, extraOffset, fixedHeaderHeight);				
			}
		};
				
		screenUtils.scrollTop = function(screen, uId, extraOffset){
			
			var widget = screen.getWidgetByUId(uId);
			if(widget!=null && widget != undefined) {
				scScreenUtils.scrollToWidget(screen, widget.uId);
				
				var node = widget.get("domNode");
				screenUtils.scrollTopWithOffest(node,extraOffset, null);				
			}		
		};
		
		screenUtils.scrollTopWithOffest = function(node,extraOffset, fixedHeaderHeight){
			var viewportOffset = node.getBoundingClientRect();
			var top = viewportOffset.top;
			
			var fixedHeight;
			if( fixedHeaderHeight!= null && fixedHeaderHeight!= undefined ) {
				fixedHeight = fixedHeaderHeight;
			} else {
				fixedHeight =  iasContextUtils.getFixedHeaderHeight();
			}
			
			if(extraOffset){
				fixedHeight+=extraOffset;
			}
			
			var scrolledY = window.scrollY;
			if(fixedHeight - top > 0) {
				if(scrolledY){
					window.scroll(0, scrolledY - (fixedHeight - top));
				}
			} else {
			  window.scroll(0, scrolledY+(top -fixedHeight));
			}
		};
		
		screenUtils.scrollToWidgetTop = function(event,bEvent,ctrl,args){
			args.screen.scrollToWidgetUId = ctrl;
			var scrollTimeOut = args.scrollTimeOut!=undefined ? args.scrollTimeOut : 200;
			setTimeout(function () {screenUtils.scrollTop(args.screen,ctrl);}, scrollTimeOut);
		};
		
		screenUtils.scrollToWidgetTopForTabChange = function(event,bEvent,ctrl,args){
			if(args.showType!=="InitView" && args.showType!=="ReloadView" && !screen[screen.currentView+"_loaded"]){
				var tabWidget = args.screen.getWidgetByUId(ctrl);
				var widgetParentNode = dDomAttr.getNodeProp(tabWidget.domNode, "parentNode");
				var parentWidget = dregistry.getEnclosingWidget(widgetParentNode);
				widgetParentNode = dDomAttr.getNodeProp(parentWidget.domNode, "parentNode");
				parentWidget = dregistry.getEnclosingWidget(widgetParentNode);
				args.screen.scrollToWidgetUId = parentWidget.uId;
				screenUtils.scrollTop(args.screen,parentWidget.uId);
			}
			else{
				args.screen.scrollToWidgetUId = null;
			}
		};
		
		screenUtils.scrollToWidgetAfterPageLoad = function(event,bEvent,ctrl,args){
			var isFirstPage = null, result = null;
			
			result = scBaseUtils.getModelValueFromBean("result", args);
			isFirstPage = scModelUtils.getStringValueFromPath("Page.IsFirstPage", result);
			
			if(scRepeatingPanelUtils.getLastPageAction(args.screen,ctrl)=="start" && args.screen.scrollToWidgetUId) {
				var scrollToWidget = args.screen.scrollToWidgetUId;// ? args.screen.scrollToWidgetUId : crtl;
				if(args.screen.hasExtraOffset){
					screenUtils.scrollTop(args.screen,scrollToWidget,50);
				}
				else{
					screenUtils.scrollTop(args.screen,scrollToWidget);
				}
				
			}
			else if (scRepeatingPanelUtils.getLastPageAction(args.screen,ctrl)!=="start"){
				//var scrollToWidget = scRepeatingPanelUtils.getLastPageActionWidget(args.screen,ctrl);
				//dWindow.scrollIntoView(scrollToWidget.domNode);
				//screenUtils.scrollTopWithOffest(scrollToWidget.domNode);
				screenUtils.scrollTop(args.screen,ctrl,50);
				
			}
			
			args.screen.scrollToWidgetUId = null;
			
		};
		
		screenUtils.afterDynamicChildScreenStartup = function(event, bEvent, ctrl, args){
			var controllerWidget = args.controllerWidget;
			var widgetParentNode = dDomAttr.getNodeProp(controllerWidget.domNode, "parentNode");
			var parentWidget = dregistry.getEnclosingWidget(widgetParentNode);
			args.scrollTimeOut = 0;
			if(args.screen.scrollToWidgetUId){// === parentWidget.uId){				
				screenUtils.scrollTop(args.screen,args.screen.scrollToWidgetUId);
				args.screen.scrollToWidgetUId = null;
			}
			/*else{
				screenUtils.scrollTop(args.screen,ctrl);
			}*/
			
		},
		
		screenUtils.getItemImageLocation = function(screen,widget,nameSpace,shipmentLineModel,options,dataValue){
			if(iasContextUtils.getDisplayItemImageRule()==="N"){
				//var pContainer =scWidgetUtils.getParentContainer(screen.getWidgetByUId(widget));
				//scWidgetUtils.hideWidget(screen, pContainer.uId, false);
				if(!dDomClass.contains(screen.domNode,"noItemImageRule")){
					dDomClass.add(screen.domNode,"noItemImageRule");
				}
				return screen._blankGif;
			}	
			var imgLoc = "https://s7.vitaminshoppe.com/is/image/VitaminShoppe";
			return imgLoc;
				
		};
		
		 screenUtils.getItemImageId = function(screen,widget,nameSpace,shipmentLineModel,options,dataValue){
            if(iasContextUtils.getDisplayItemImageRule()==="N")
                return "";   
            var imgID = scModelUtils.getStringValueFromPath("ExtendedDisplayDescription", dataValue);
            if(imgID != null)
            {
				var imgID1 = imgID.split("(");
				var imgID2 = imgID1[1].split(")");
				imgID = imgID2[0]+"_01";
            }
            return imgID;  
        };


		screenUtils.resetProductAspectRatio = function(event,bEvent,ctrl,args){
			if(iasContextUtils.getDisplayItemImageRule()==="N")
				return;
			var screen = args.screen;
			
			var imageObj= scScreenUtils.getWidgetByUId(screen,ctrl);
			if(!scBaseUtils.isVoid(imageObj) && !scBaseUtils.isVoid(imageObj.domNode)) {
				var parentWidget = dregistry.getEnclosingWidget(imageObj.domNode.parentNode);
				
				if(!scBaseUtils.isVoid(parentWidget) && !scBaseUtils.isVoid(parentWidget.domNode)) {
					var naturalHeight=imageObj.domNode.naturalHeight;
					var naturalWidth=imageObj.domNode.naturalWidth;
					var clientWidth=parentWidget.domNode.clientWidth+"px";
					
					var style="";			
					if(naturalHeight > naturalWidth) {
						style = "width:auto;height:"+clientWidth;
					} else {
						style = "height:auto;width:"+clientWidth;
					}
					
					dDomAttr.set(imageObj, {"style": style});
				}
			} 			
		};
		
		
		
		screenUtils.getDueInImageLocation = function(screen,widget,nameSpace,itemModel,options,dataValue){
			/*
			 * EXAMPLE VALUES
			 * url = "wsc/resources/css/icons/images/timeOverdue.png"
			 * imgLoc = "wsc/resources/css/icons/images"
			 * fullImgLoc = "wscdev/wsc/resources/css/icons/images"
			*/
			var url = scModelUtils.getStringValueFromPath("CodeLongDescription", dataValue);
			var lastSlash = url.lastIndexOf("/");
			var imgLoc = url.slice(0,lastSlash);
			var fullImgLoc = iasUIUtils.getFullURLForImage(imgLoc);
			return fullImgLoc;
				
		};
		
		screenUtils.getRepeatingPanelScreenWidget = function(screen, repeatingpanelUId) {
			
			var repeatingPanelScreenWidget = "";
			
			if(!scBaseUtils.isVoid(repeatingpanelUId)) {
				repeatingPanelScreenWidget = scScreenUtils.getWidgetByUId(screen,repeatingpanelUId);
			}
			
			return repeatingPanelScreenWidget;
			
			
		};
		
		screenUtils.getDueInImageId = function(screen,widget,nameSpace,itemModel,options,dataValue){
			/*
			 * EXAMPLE VALUES
			 * url = "wsc/resources/css/icons/images/timeOverdue.png"
			 * imgId = "timeOverdue.png"
			*/
			var url = scModelUtils.getStringValueFromPath("CodeLongDescription", dataValue);
			var lastSlash = url.lastIndexOf("/");
			var urlLength = url.length;
			var imgId = url.slice(lastSlash+1,urlLength);
			return imgId;	
		};
		
		
		
		
		
		screenUtils.appendChildModelAsArrayToParentModelPath = function(parentModel, path, childModel, index) {
		
			var arrayModel = scModelUtils.getModelObjectFromPath(path,parentModel);
			arrayModel = [];
			arrayModel[index] = childModel;
			scModelUtils.addModelToModelPath(path,arrayModel,parentModel);
			
			return parentModel;
		};
		
		screenUtils.decrementNumber = function(numberToDecrement , decrementValue) {
			numberToDecrement = numberToDecrement-decrementValue;
				return numberToDecrement;
		};
		
		screenUtils.hideSystemMessage = function(screen) {
			var mashupContext = scControllerUtils.getMashupContext(screen);
			if(iasUIUtils.shouldHideSystemMessage(mashupContext)) {
				iasBaseTemplateUtils.hideMessage(screen);
			}
		};

		
		screenUtils.updateEditorTitle = function(screen,titleParam,titleKey){
			
			var titleArgs = scBaseUtils.getNewBeanInstance();
			var eventArgs = scBaseUtils.getNewBeanInstance();
			
			scBaseUtils.setAttributeValue("title",titleParam,titleArgs);
			scBaseUtils.setAttributeValue("titleKey",titleKey,titleArgs);			
			scBaseUtils.setAttributeValue("title",titleArgs,eventArgs);			
			
			var editorInstance = iasUIUtils.getEditorFromScreen(screen);
			scEventUtils.fireEventInsideScreen(editorInstance,"updateEditorTitle",null,eventArgs);
			
			var inputArray = scBaseUtils.getNewArrayInstance();
			scBaseUtils.appendToArray(inputArray,titleParam);
			var editorTitle = scScreenUtils.getFormattedString(screen,titleKey,inputArray);	
			var editorTitleArgs = scBaseUtils.getNewBeanInstance();	
			scBaseUtils.setAttributeValue("title",editorTitle,editorTitleArgs);			
			scEventUtils.fireEventGlobally(screen, "changeEditorTitle", null, editorTitleArgs);
		};
		
		screenUtils.getForwardBindingNamespaceMapBean = function(parentnamespace,parentpath,childnamespace,childpath){
			var namespaceMapBean = scBaseUtils.getNewBeanInstance();
			scBaseUtils.addStringValueToBean("parentnamespace",parentnamespace,namespaceMapBean);
			scBaseUtils.addStringValueToBean("parentpath",parentpath,namespaceMapBean);
			scBaseUtils.addStringValueToBean("childnamespace",childnamespace,namespaceMapBean);
			scBaseUtils.addStringValueToBean("childpath",childpath,namespaceMapBean);
			return namespaceMapBean;
		};
		
		screenUtils.getRepeatingScreenBindingData = function(screen,widget,repeatingScreenId,sourceNamespaceMapArray,targetNamespaceMapArray,additionalParamsBean){
			var returnValue = scBaseUtils.getNewBeanInstance();
			var constructorData = scBaseUtils.getNewBeanInstance();
			scBaseUtils.addStringValueToBean("repeatingscreenID", repeatingScreenId, returnValue);			
			if(!scBaseUtils.isVoid(additionalParamsBean)){
				constructorData = additionalParamsBean;
			}
			var bindingData = scBaseUtils.getNewBeanInstance();
			var fwdbindingData = scBaseUtils.getNewBeanInstance();
			if(!scBaseUtils.isVoid(sourceNamespaceMapArray)){				
				scBaseUtils.addArrayValueToBean("sourceMapping", sourceNamespaceMapArray, fwdbindingData);			
			}
			if(!scBaseUtils.isVoid(targetNamespaceMapArray)){				
				scBaseUtils.addArrayValueToBean("targetMapping", targetNamespaceMapArray, fwdbindingData);			
			}
			scBaseUtils.addBeanValueToBean("fwdbindingData", fwdbindingData, bindingData);
			scBaseUtils.addBeanValueToBean("bindingData", bindingData, constructorData);
			scBaseUtils.addBeanValueToBean("constructorArguments", constructorData, returnValue);
			return returnValue;
		};
		
		screenUtils.getRepeatingScreenData = function(repeatingScreenId,targetNamespaceMapBean,additionalParamsBean) {
			var returnValue = null;
			var constructorData = null;

			returnValue = scBaseUtils.getNewBeanInstance();
			scBaseUtils.addStringValueToBean("repeatingscreenID", repeatingScreenId, returnValue);

			constructorData = scBaseUtils.getNewBeanInstance();

			if(!scBaseUtils.isVoid(additionalParamsBean)){
				constructorData = additionalParamsBean;
			}

			if(!scBaseUtils.isVoid(targetNamespaceMapBean)){
				var namespaceMapArray = null;
				var mainTargetMappingBean = null;
				var targetMappingBean = null;

				namespaceMapArray = scBaseUtils.getNewArrayInstance();
				scBaseUtils.appendBeanToArray(namespaceMapArray, targetNamespaceMapBean);

				targetMappingBean = scBaseUtils.getNewBeanInstance();
				scBaseUtils.addArrayValueToBean("targetMapping", namespaceMapArray, targetMappingBean);

				mainTargetMappingBean = scBaseUtils.getNewBeanInstance();
				scBaseUtils.addBeanValueToBean("fwdbindingData", targetMappingBean, mainTargetMappingBean);
				scBaseUtils.addBeanValueToBean("bindingData", mainTargetMappingBean, constructorData);
			}

			scBaseUtils.addBeanValueToBean("constructorArguments", constructorData, returnValue);

			//console.log("returnValue : ",returnValue);
			return returnValue;
		};
		
		screenUtils.showError = function(screen, uId, errorMsgBundleKey){
			if (iasContextUtils.isMobileContainer()) {				
					iasBaseTemplateUtils.showMessage(screen, errorMsgBundleKey, "error", null);				
				}else{			
					scWidgetUtils.markFieldinError(screen, uId, scScreenUtils.getString(screen, errorMsgBundleKey), true);
				}
		};
		
		/*
		 * action can be increase,decrease,validate
		 * This method will return N if the field is in error, Y if qty is in range, S if qty is same as oldQty
		 */
		screenUtils.ifQtyIsInRange = function(screen, action, qtyTxtUId, validationConfig, repeatingPanelUId){
			var qty = scBaseUtils.getNumberValueFromBean("currentQty",validationConfig);
			var maxQty = scBaseUtils.getNumberValueFromBean("maxQty",validationConfig);
			var minQty = scBaseUtils.getNumberValueFromBean("minQty",validationConfig);
			var oldQty = scBaseUtils.getNumberValueFromBean("oldQty",validationConfig);
			minQty = minQty ? minQty : 0;
			var minErrorMsg = scBaseUtils.getStringValueFromBean("minErrorMsg",validationConfig);
			minErrorMsg = minErrorMsg ? minErrorMsg : "Message_NegativeQtyError";
			var maxErrorMsg = scBaseUtils.getStringValueFromBean("maxErrorMsg",validationConfig);			
			maxErrorMsg = maxErrorMsg ? maxErrorMsg : "Message_MaxQtyError";
			
			var lastUpdatedRepPanelUId = scBaseUtils.getStringValueFromBean("lastUpdatedRepPanelUId",validationConfig);
			
			var isQtyInRange = true;
			if (!iasUIUtils.isValueNumber(qty)) {
				isQtyInRange = false;
				errorMsgBundleKey = "Message_NotValidNumber";
	        } else{
	        	
	        	var newQty = qty;
				if(scBaseUtils.equals(action, "increase")){
					newQty = qty + 1;
				}else if(scBaseUtils.equals(action, "decrease")){
					newQty = qty - 1;
				}
				
				var bundleArray = scBaseUtils.getNewArrayInstance();
				 if (scBaseUtils.lessThan(newQty, minQty)) {
					 isQtyInRange = false;
					 scBaseUtils.appendToArray(bundleArray, minQty);
					 errorMsgBundleKey = minErrorMsg;
				 } else if (scBaseUtils.greaterThan(newQty, maxQty)) {
					 isQtyInRange = false;
					 scBaseUtils.appendToArray(bundleArray, maxQty);
					 errorMsgBundleKey = maxErrorMsg;
				 }
	        }
			
			 if(!isQtyInRange){
				 var messageConfig = scBaseUtils.getNewBeanInstance();
				 if(!scBaseUtils.equals(action, "validate")){
					scBaseUtils.addStringValueToBean("showAsDialog", "true", messageConfig);
				 }
				 scBaseUtils.addStringValueToBean("callBackHandler", "setFocusOnQty", messageConfig);
				 iasBaseTemplateUtils.showMessage(screen, scScreenUtils.getFormattedString(screen, errorMsgBundleKey, bundleArray), "error", messageConfig);
				 if(repeatingPanelUId)
					 scScreenUtils.addClass(screen, "errorRepeatingPanel");
			 }else{
					 if(!scBaseUtils.equals(action, "validate")){
						scWidgetUtils.setValue(screen, qtyTxtUId,newQty, true);  
	           	  }else{
					   if(lastUpdatedRepPanelUId)
	        			 scWidgetUtils.removeClass(iasUIUtils.getParentScreen(screen, true), lastUpdatedRepPanelUId, "highlightRepeatingPanel");
					
					 if(repeatingPanelUId){
						 scScreenUtils.removeClass(screen, "errorRepeatingPanel");
						 scScreenUtils.addClass(screen, "highlightRepeatingPanel");
					 }
					  if(iasUIUtils.isValueNumber(oldQty)){
						 if (scBaseUtils.equals(newQty, oldQty)) {
							 iasBaseTemplateUtils.showMessage(screen, "Message_NoSave", "information", null);							
							 return "S";
						 }

					}
					
				  }
	                //scWidgetUtils.clearFieldinError(screen, qtyTxtUId);
					
						 
			 }
			 var returnIsQtyInRange = "N";
			 if(isQtyInRange)
				returnIsQtyInRange = "Y";
			 return returnIsQtyInRange;
		};
		
		screenUtils.getMsgPanelScreen = function(screen, msgPanelId){
			var msgPanelWidget = screen.getWidgetByUId(msgPanelId);
			if(msgPanelWidget)
				return screen;
			else{
				var parentScreen = iasUIUtils.getParentScreen(screen, true);
			}
		};
		
		/**
		 * screen - repeatingPanelScreen or screen which contains qty text box
		 * msgPanelScreen -  screen containing msgPanel - could be repeating screen or last scanned product screen
		 * action - increase (on +), decrease (on -), validate (on onBlur), update (on Enter and Update button)
		 * qtyTxtUid - To set the increased/decreased value. Should be direct child of screen
		 * validationConfig
		 **** currentQty - current qty in the field
		 **** maxQty - max limit of the qty
		 **** maxErrorMsg - (Default value: Message_MaxQtyError)
		 **** minQty - (Default value: 0)
		 **** minErrorMsg - (Default value: Message_NegativeQtyError)
		 **** oldQty - Old qty if no changes to be updated validation is to be done
		 **** messagePanelId (Default value: lspErrorMsgPnl) - The messagePanel uId where error message needs to be displayed
		 **** lastUpdatedRepPanelPropertyName - property name to get/set LastUpdatedRepeatingPanelUId from the parent screen
		 * isRepeatingPanel - If yes, it will set error and add on highlighting classes, otherwise will just show messgae for LPS
		 */
		screenUtils.ifQtyIsInRangeNew = function(screen, msgPanelScreen, action, qtyTxtUId, validationConfig, isRepeatingPanel){
			var qty = scBaseUtils.getNumberValueFromBean("currentQty",validationConfig);
			var maxQty = scBaseUtils.getNumberValueFromBean("maxQty",validationConfig);
			var minQty = scBaseUtils.getNumberValueFromBean("minQty",validationConfig);
			var oldQty = scBaseUtils.getNumberValueFromBean("oldQty",validationConfig);
			var msgPanelId = scBaseUtils.getStringValueFromBean("messagePanelId",validationConfig);
			msgPanelId = msgPanelId ? msgPanelId : "lspErrorMsgPnl";
		
			minQty = minQty ? minQty : 0;
			var minErrorMsg = scBaseUtils.getStringValueFromBean("minErrorMsg",validationConfig);
			minErrorMsg = minErrorMsg ? minErrorMsg : "Message_NegativeQtyError";
			var maxErrorMsg = scBaseUtils.getStringValueFromBean("maxErrorMsg",validationConfig);			
			maxErrorMsg = maxErrorMsg ? maxErrorMsg : "Message_MaxQtyError";
			
			var lastUpdatedRepPanelPropertyName = scBaseUtils.getStringValueFromBean("lastUpdatedRepPanelPropertyName",validationConfig);
				
			var messageConfig = scBaseUtils.getNewBeanInstance();
			
			var ifQtyCanBeUpdated = true;
			var errorMsgBundleKey = "";
			var doSetValue = false;
			if (!iasUIUtils.isValueNumber(qty)) {
				ifQtyCanBeUpdated = false;
				errorMsgBundleKey = "Message_NotValidNumber";
	        } else{	        	
	        	var newQty = qty;
				if(scBaseUtils.equals(action, "increase")){
					newQty = qty + 1;
				}else if(scBaseUtils.equals(action, "decrease")){
					newQty = qty - 1;
				}				
				var bundleArray = scBaseUtils.getNewArrayInstance();
				 if (/*scBaseUtils.lessThan(qty, minQty) || */scBaseUtils.lessThan(newQty, minQty)) {
					 ifQtyCanBeUpdated = false;
					 scBaseUtils.appendToArray(bundleArray, minQty);
					 errorMsgBundleKey = minErrorMsg;
					 if(scBaseUtils.equals(action, "increase")){//increase is a corrective action, value should be changed but not updated
						 doSetValue = true;
					 }
				 } else if (iasUIUtils.isValueNumber(maxQty) && (/*scBaseUtils.greaterThan(qty, maxQty) || */scBaseUtils.greaterThan(newQty, maxQty))) {
					 ifQtyCanBeUpdated = false;
					 scBaseUtils.appendToArray(bundleArray, maxQty);
					 errorMsgBundleKey = maxErrorMsg;
					 if(scBaseUtils.equals(action, "decrease")){//decrease is a corrective action, value should be changed but not updated
						 doSetValue = true;
					 }
				 }
	        }
			
			var errorMsgType = "";
			var msg = "";
			 if(!ifQtyCanBeUpdated){
				errorMsgType = "error";
				if(scBaseUtils.equals(action, "increase") || scBaseUtils.equals(action, "decrease")){
					if(scBaseUtils.lessThanOrEqual(qty, maxQty) && scBaseUtils.greaterThanOrEqual(qty, minQty)){//no action is required
						errorMsgType = "information";
					}
					if(doSetValue)
						scWidgetUtils.setValue(screen, qtyTxtUId,newQty, true);
					
				}else if(scBaseUtils.equals(action, "validate")){
					if(!iasUIUtils.isValueNumber(qty)){
						errorMsgType = "information";
						errorMsgBundleKey = "";
					}
						
				}	
				msg = scScreenUtils.getFormattedString(screen, errorMsgBundleKey, bundleArray);			 
				 				
			 }else{
					scWidgetUtils.hideWidget(msgPanelScreen, msgPanelId);
					scWidgetUtils.clearFieldinError(screen, qtyTxtUId);
					
					 if(scBaseUtils.equals(action, "increase") || scBaseUtils.equals(action, "decrease")){
						scWidgetUtils.setValue(screen, qtyTxtUId,newQty, true); 

					}else if(scBaseUtils.equals(action, "update")){							 						  
						 if(iasUIUtils.isValueNumber(oldQty) && scBaseUtils.equals(newQty, oldQty)){								 
								errorMsgType = "information";
								msg = "Message_NoSave";
								ifQtyCanBeUpdated = false;
						 }					
				  }			 
			 }
			 if(isRepeatingPanel){
				 	var parentScreen = iasUIUtils.getParentScreen(msgPanelScreen, true);
					screenUtils.toggleHighlight(parentScreen, msgPanelScreen,lastUpdatedRepPanelPropertyName,msgPanelId,errorMsgType,msg);
			}else if(errorMsgType && msg){//simply show the message for LPS
				iasBaseTemplateUtils.displaySingleMessage(msgPanelScreen, msg, errorMsgType, null, msgPanelId);
				
			}
			 if(scBaseUtils.equals(errorMsgType, "error"))
				scWidgetUtils.markFieldinError(screen, qtyTxtUId, msg, false);
			 return ifQtyCanBeUpdated;
		};
		
		/**
		 * repeatingScreen - repeatingScreen
		 * lastUpdatedRepPanelPropertyName - property name to get/set LastUpdatedRepeatingPanelUId from the parent screen 
		 * msgPanelId - The messagePanel uId where error message needs to be displayed
		 * errorMsgType - information/success/error
		 * msg
		 */
		screenUtils.toggleHighlight = function(parentScreen, repeatingScreen,lastUpdatedRepPanelPropertyName,msgPanelId,errorMsgType,msg){
			var classToAdd = "highlightRepeatingPanel";
			var classToRemove = "errorRepeatingPanel";
			if(scBaseUtils.equals(errorMsgType, "error")){
				classToAdd = "errorRepeatingPanel";
				classToRemove = "highlightRepeatingPanel glowAndFadeout";
			}else if(scBaseUtils.equals(errorMsgType, "success")){
				classToAdd = "highlightRepeatingPanel glowAndFadeout";
			}
			//var parentScreen = iasUIUtils.getParentScreen(repeatingScreen, true);
			if(lastUpdatedRepPanelPropertyName){
			var lastUpdatedRepPanelUId = screenUtils.getScreenProperty(parentScreen,lastUpdatedRepPanelPropertyName);
			if(lastUpdatedRepPanelUId){
				var lastUpdatedRepPanelScreen = parentScreen.getWidgetByUId(lastUpdatedRepPanelUId);
				if(lastUpdatedRepPanelScreen){
					scScreenUtils.removeClass(lastUpdatedRepPanelScreen, "highlightRepeatingPanel glowAndFadeout");
					 var messageWidget = lastUpdatedRepPanelScreen.getWidgetByUId('singlemessagelabel');
					 if(messageWidget && !(messageWidget.type == 'error'))
						scWidgetUtils.hideWidget(lastUpdatedRepPanelScreen, msgPanelId);
				}
			}
			
			if(scBaseUtils.isVoid(repeatingScreen)) {
				
				screenUtils.setScreenProperty(parentScreen,lastUpdatedRepPanelPropertyName,null);
				
			} else {
				screenUtils.setScreenProperty(parentScreen,lastUpdatedRepPanelPropertyName,scWidgetUtils.getWidgetUId(repeatingScreen));
				
				scScreenUtils.removeClass(repeatingScreen, classToRemove);
				scScreenUtils.addClass(repeatingScreen, classToAdd);
				
				if(errorMsgType && msg)
					iasBaseTemplateUtils.displaySingleMessage(repeatingScreen, msg, errorMsgType, null, msgPanelId);				 
				}
			}
			
			
			
		};
		
		screenUtils.getScreenProperty = function(screen,propertyName){
			return screen.get(propertyName);
		};
		
		screenUtils.setScreenProperty = function(screen,propertyName,propertyValue){
			screen.set(propertyName, propertyValue);
		};
		
		
		screenUtils.changeView = function(event,bEvent,ctrl,args){
			var screen = args.screen;
			if(!scBaseUtils.isVoid(screen)){
				var currentView = screen.currentView;
				if(!scBaseUtils.isVoid(currentView) && !scBaseUtils.equals(currentView,ctrl)){
					screen.nextView = ctrl;
					scEventUtils.fireEventInsideScreen(screen,screen.currentView+"_beforeHide",{},{"view":ctrl});
				}
			}		
		};
		
		screenUtils.loadSelectedView = function(event,bEvent,ctrl,args){
			var screen = args.screen;
			var nextView = args.view;			
			if(scBaseUtils.isVoid(nextView)){
				if(!scBaseUtils.isVoid(screen) && !scBaseUtils.isVoid(screen.nextView)){
					nextView = screen.nextView;
				}
				else{
					nextView = ctrl;
				}
			}
			if(!scBaseUtils.isVoid(screen)){
				/*var currentView = screen.currentView;
				if(args.reload || !scBaseUtils.isVoid(currentView) && !scBaseUtils.equals(currentView,nextView)){
					scWidgetUtils.hideWidget(screen,currentView+"_ContentPane",false);					
					var cssClass = scBaseUtils.getNewArrayInstance();
					scBaseUtils.appendToArray(cssClass,"active");
					scWidgetUtils.removeClass(screen,currentView,cssClass);
					scWidgetUtils.addClass(screen,nextView,cssClass);					
					scWidgetUtils.showWidget(screen,nextView+"_ContentPane",true);
					//if(!scBaseUtils.isVoid(screen[nextView+"_onShow"])){
						//screen[ctrl+"_onShow"](event,bEvent,nextView,args);
						scEventUtils.fireEventInsideScreen(screen,nextView+"_onShow",{},{});
					//}
					screen.currentView = nextView;
					screen.nextView = "";
				}*/
				screenUtils.showView(screen,"LoadView",nextView);
			}		
		};
		
		screenUtils.reloadSelectedView = function(event,bEvent,ctrl,args){
			var screen = args.screen;
			var nextView = null;			
			if(scBaseUtils.isVoid(nextView)){
				if(!scBaseUtils.isVoid(screen) && !scBaseUtils.isVoid(screen.nextView)){
					nextView = screen.nextView;
				}
				else{
					nextView = ctrl;
				}
			}
			if(!scBaseUtils.isVoid(screen)){
				screenUtils.showView(screen,"ReloadView",nextView);
			}		
		};
		
		screenUtils.initSelectedView = function(event,bEvent,ctrl,args){
			var screen = args.screen;
			if(!scBaseUtils.isVoid(screen)){
				/*var currentView = screen.currentView;
				if(!scBaseUtils.isVoid(currentView)){
					var cssClass = scBaseUtils.getNewArrayInstance();
					scBaseUtils.appendToArray(cssClass,"active");
					scWidgetUtils.addClass(screen,currentView,cssClass);
					scWidgetUtils.showWidget(screen,currentView+"_ContentPane",true);
					//if(!scBaseUtils.isVoid(screen[currentView+"_onShow"])){
						//screen[currentView+"_onShow"](event,bEvent,currentView,args);
						scEventUtils.fireEventInsideScreen(screen,currentView+"_onShow",{},{});
					//}					
				}*/			
				screenUtils.showView(screen,"InitView","");
			}		
		};
		
		screenUtils.showView = function(screen,showType,nextView){
			var currentView = screen.currentView;
			var args = {};
			args.showType = showType;
			if(!scBaseUtils.isVoid(currentView)){
				if(scBaseUtils.equals(showType,"InitView")){
					var cssClass = scBaseUtils.getNewArrayInstance();
					scBaseUtils.appendToArray(cssClass,"active");
					scWidgetUtils.addClass(screen,currentView,cssClass);
					scWidgetUtils.showWidget(screen,currentView+"_ContentPane",true);					
					scEventUtils.fireEventInsideScreen(screen,currentView+"_onShow",{},args);	
				}					
				else if((scBaseUtils.equals(showType,"LoadView")|| scBaseUtils.equals(showType,"ReloadView")) && !scBaseUtils.isVoid(currentView) && !scBaseUtils.equals(currentView,nextView)){
					scWidgetUtils.hideWidget(screen,currentView+"_ContentPane",false);					
					var cssClass = scBaseUtils.getNewArrayInstance();
					scBaseUtils.appendToArray(cssClass,"active");
					scWidgetUtils.removeClass(screen,currentView,cssClass);
					scWidgetUtils.addClass(screen,nextView,cssClass);					
					scWidgetUtils.showWidget(screen,nextView+"_ContentPane",true);						
					scEventUtils.fireEventInsideScreen(screen,nextView+"_onShow",{},args);						
					screen.currentView = nextView;						
				}
				else if(scBaseUtils.equals(showType,"ReloadView")){
					scEventUtils.fireEventInsideScreen(screen,nextView+"_onShow",{},args);
				}
				screen.nextView = "";
			}
			if(showType!=="InitView" && showType!=="ReloadView"){// && !screen[screen.currentView+"_loaded"]){
				var tabWidget = screen.getWidgetByUId(screen.currentView);
				var widgetParentNode = dDomAttr.getNodeProp(tabWidget.domNode, "parentNode");
				var parentWidget = dregistry.getEnclosingWidget(widgetParentNode);
				widgetParentNode = dDomAttr.getNodeProp(parentWidget.domNode, "parentNode");
				parentWidget = dregistry.getEnclosingWidget(widgetParentNode);
				screen.scrollToWidgetUId = parentWidget.uId;
				screenUtils.scrollTop(screen,parentWidget.uId);
			}
			else{
				screen.scrollToWidgetUId = null;
			}
		};
		
		screenUtils.hideView = function(screen,viewId){
			if(!scBaseUtils.isVoid(screen)){				
				if(!scBaseUtils.isVoid(viewId)){	
					scWidgetUtils.hideWidget(screen,viewId,true);
					scWidgetUtils.hideWidget(screen,viewId+"_ContentPane",true);										
				}
			}		
		};
		
		screenUtils.hideTabForSingleView = function(screen,tabContainerUId){
				var tabPnl = scScreenUtils.getWidgetByUId(screen,tabContainerUId);
				var tabs = tabPnl.getChildren();
				var visibleTabs = 0;
				for(var i=0;i<tabs.length;i++){
					if(scWidgetUtils.isWidgetVisible(screen,tabs[i].uId)){
						visibleTabs++;						
					}					
				}
				if(visibleTabs===1){
					scWidgetUtils.addClass(screen,tabContainerUId,"singleTab");
				}
			
		};
		
		screenUtils.showSelectedViewOnChange = function(event,bEvent,ctrl,args){			
			if(!scBaseUtils.isVoid(event)){
				screenUtils.loadSelectedView(event,bEvent,event,args);				
			}
		};
		
		screenUtils.displayFieldIfHasValue = function(unformattedValue,screen,widget,namespace,shipmentLine) {
			if(!scBaseUtils.isVoid(unformattedValue)){
				scWidgetUtils.showWidget(screen,widget,true);
			}
			else{
				scWidgetUtils.hideWidget(screen,widget,true);
			}
			return unformattedValue;
		};
		
		/*screenUtils.printHtmlInIframe = function(outputModel) {
			
			var ua = window.navigator.userAgent;
			var msie = ua.indexOf("MSIE ");//Detect if the browser is IE other than IE11
			var trident = ua.indexOf('Trident/'); //Detect if the browser IE11
			var isIEMobile = ua.indexOf("IEMobile/") > 0;
			
			if (msie > 0 || trident > 0 || iasContextUtils.isMobileContainer()) {
				screenUtils.printHtmlInPopup(outputModel,isIEMobile);
			}else{
				var iframe = document.getElementById('print_iframe');
				if (iframe == null) {
					iframe = document.createElement('iframe');
					iframe.setAttribute('id', 'print_iframe');
					iframe.setAttribute('name', 'print_iframe');
					iframe.setAttribute('width', '0');
					iframe.setAttribute('height', '0');
					iframe.setAttribute('frameBorder', 'no');
					iframe.setAttribute('src', 'about:blank');
					iframe.setAttribute('sandbox', 'allow-same-origin');
					document.body.appendChild(iframe);
				}

				iframe.contentWindow.onload = iframe.onload = function() {
					scUtil.setReloadConfirmationPopupMode("NEVER");
					iframe.contentWindow.print();
					scUtil.setReloadConfirmationPopupMode("ALWAYS");
				};
				
				if (outputModel.Output.hasOwnProperty('out')) {
					iframe.contentWindow.document.open();
					iframe.contentWindow.document.write(outputModel.Output.out);
					iframe.contentWindow.document.close();			
				}
			}
			
		};
		
		screenUtils.printHtmlInPopup = function(outputModel,isIEMobile){
			if (outputModel.Output.hasOwnProperty('out')) {
				// If you want the dimensions of the current window
				//var height = "innerHeight" in window 
				//   ? window.innerHeight
				//   : document.documentElement.offsetHeight;
				//var width = "innerWidth" in window 
				//   ? window.innerWidth
				//   : document.documentElement.offsetWidth;
				var dimensions = "width=" + width + ", height=" + height;
								
				var parameters = "menubar=yes, resizable=yes, scrollbars=yes";
				var printWindow = null;
				if(isIEMobile){
					//printWindow = window.open("", "_parent", parameters);	
					var iframe = document.getElementById('print_mobileiframe');
					if (iframe == null) {
						iframe = document.createElement('iframe');
						iframe.setAttribute('id', 'print_mobileiframe');
						iframe.setAttribute('name', 'print_mobileiframe');
						iframe.setAttribute('width', '100%');
						iframe.setAttribute('height', '100%');
						iframe.setAttribute('frameBorder', 'no');
						//iframe.setAttribute('src', 'about:blank');
						//iframe.setAttribute('sandbox', 'allow-same-origin');
						document.getElementById("printHolder").appendChild(iframe);
					}
					
					iframe.contentWindow.document.open();
					iframe.contentWindow.document.write(outputModel.Output.out);
					iframe.contentWindow.document.close();
					screenUtils.showPrintIframe();				
				}
				else{
					printWindow = window.open("about:blank", "_blank", parameters);
					printWindow.document.write(outputModel.Output.out);		
				}
			}
		};		
		
		screenUtils.showPrintIframe = function(){
			document.getElementById("printHolder").style.display="block";
			document.getElementById("mainContentHolder").style.display="none";
			window.history.pushState({"page":"print"}, "Print", null);
			window.addEventListener("popstate", screenUtils.hidePrintIframe );			
		};
		
		screenUtils.hidePrintIframe = function(){
			document.getElementById("printHolder").style.display="none";
			document.getElementById("mainContentHolder").style.display="block";
			document.getElementById("printHolder").removeChild(document.getElementById('print_mobileiframe'));
			window.removeEventListener("popstate",screenUtils.hidePrintIframe);			
		};
		*/
		
		screenUtils.showInfoMessageBoxWithOk = function(screen,message,screenMethodName,args){		
			var textObj = {};
			textObj.OK = scBundleUtils.getString("Action_Ok");
			var messageObj = {text:scScreenUtils.getString(screen,"textInformation"),info:message};
			scScreenUtils.showInfoMessageBox(screen, messageObj, screenMethodName, textObj, args);
			//scScreenUtils.showInfoMessageBox(screen,message,screenMethodName,textObj,args)
		};
		screenUtils.showConfirmMessageBoxForSuccess = function(screen,message,screenMethodName,args){		
			var messageObj = {};
			messageObj.info = message;
			messageObj.iconClass = "idxSignIcon idxInformIcon";
			messageObj.type = "information";
			scScreenUtils.showConfirmMessageBox(screen,messageObj,screenMethodName,null,args)
		};
		screenUtils.encodeContentID = function(contentID){
			if(scApplicationInfo.getShouldEncodeImageId()) {
				return encodeURIComponent(contentID);
			}
			return contentID;
		};
		screenUtils.encodeContentLocation = function(contentLocation){	
			return encodeURI(contentLocation);
		};
		
		return screenUtils;
	});