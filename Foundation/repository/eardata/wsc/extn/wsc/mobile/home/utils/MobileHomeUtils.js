scDefine([
	"scbase/loader!dojo/_base/lang",	
	"scbase/loader!wsc",
	"scbase/loader!sc/plat/dojo/utils/ControllerUtils",
	"scbase/loader!ias/utils/UIUtils",
	"scbase/loader!sc/plat/dojo/utils/WidgetUtils",
	"scbase/loader!sc/plat/dojo/utils/BaseUtils",
	"scbase/loader!sc/plat/dojo/utils/ScreenUtils",
	"scbase/loader!ias/utils/ContextUtils",
	"scbase/loader!sc/plat/dojo/utils/EventUtils",
	"scbase/loader!sc/plat/dojo/utils/ModelUtils",
	"scbase/loader!sc/plat/dojo/utils/ResourcePermissionUtils",
	"scbase/loader!wsc/components/shipment/common/utils/ShipmentUtils"
	],
	function(dLang,wsc,scControllerUtils,iasUIUtils,scWidgetUtils,scBaseUtils,scScreenUtils,iasContextUtils,scEventUtils,scModelUtils,scResourcePermissionUtils, wscShipmentUtils){
		var mobileHomeUtils = dLang.getObject("mobile.home.utils.MobileHomeUtils", true,wsc);
		
		
		mobileHomeUtils.getDescription = function(descriptionField,dataValue,screenObject,widgetObject){
			if(!scBaseUtils.isVoid(descriptionField)){
				return scScreenUtils.getFormattedString(screenObject, descriptionField, null);
			}
		};
		
		mobileHomeUtils.getShipmentStatusListModel = function(screen){
			
			var orderSearchConfig = iasContextUtils.getFromContext("orderSearchConfig");
			var existingLength = orderSearchConfig.OrderSearch.OrderStatus.StatusList.Status.length;
			var statusCode = orderSearchConfig.OrderSearch.OrderStatus.StatusList.Status[existingLength-1].StatusCode;
			if(statusCode!="1600.002")
			{
				orderSearchConfig.OrderSearch.OrderStatus.StatusList.Status[existingLength] = {};
				orderSearchConfig.OrderSearch.OrderStatus.StatusList.Status[existingLength].StatusCode = {};
				orderSearchConfig.OrderSearch.OrderStatus.StatusList.Status[existingLength].StatusCode = "1600.002";
				orderSearchConfig.OrderSearch.OrderStatus.StatusList.Status[existingLength].StatusKey = {};
				orderSearchConfig.OrderSearch.OrderStatus.StatusList.Status[existingLength].StatusKey = "Shipment Invoiced";
			}
			
			if(!scBaseUtils.isVoid(orderSearchConfig)) {
				return orderSearchConfig.OrderSearch.OrderStatus;
			} else {
				return "";
			}
			
		};
		
		mobileHomeUtils.getDeliveryMethodListModel = function(screen){
			
			var orderSearchConfig = iasContextUtils.getFromContext("orderSearchConfig");
			if(!scBaseUtils.isVoid(orderSearchConfig)){
				return orderSearchConfig.OrderSearch.OrderFulfillment;
			} else {
				return "";
			}
			
		};
		
		mobileHomeUtils.generateSortByWidget = function(screen, parentUId, uId, sortByListModel){
			
			if(!scBaseUtils.isVoid(sortByListModel) && !scBaseUtils.isVoid(sortByListModel.OrderByList) 
					&& !scBaseUtils.isVoid(sortByListModel.OrderByList.OrderBy) 
					&& !scBaseUtils.isVoid(sortByListModel.OrderByList.OrderBy.length)) {
				
				for(var i=0;i<sortByListModel.OrderByList.OrderBy.length;i++) {
					
					var orderByModel = sortByListModel.OrderByList.OrderBy[i];
					
					var cpUId = uId + orderByModel.Attribute + orderByModel.Descending + "cp";
					
					var configParamscp = {
							"CONTENTPANE": {
								"uId": cpUId,
								"class":""
							}
						};
					
					var subscriberDefn = {
							sequence : 30,
							eventId: cpUId + "_" + "onClick",
							handler :{
								methodName : "onSelection"
							}
						};	
					
					var sortByLabelContainer = scWidgetUtils.createWidgetInScreen("CONTENTPANE", screen, cpUId, configParamscp);
					scWidgetUtils.placeAt(screen, parentUId, sortByLabelContainer, null);
					scEventUtils.subscribeEvent(screen, subscriberDefn);
					
					var labelUId =   uId + orderByModel.Attribute + orderByModel.Descending + "label";
					
					 var configParamsLabel = {
								"Label": {
									
									"uId": labelUId,
									"value": orderByModel.Description,
									"class":"autoLabelZeroWidth shortageQty"
								}
							};
					 
					scWidgetUtils.createWidgetInScreen("Label", screen, labelUId, configParamsLabel);
					
					scWidgetUtils.placeAt(screen, cpUId, labelUId, null);
					
					
				}
			}
			
		};
		
		
		
		mobileHomeUtils.openScreen = function(screenPath, editorPath){
			/*EXAMPLE VALUES
			 *scControllerUtils.openScreenInEditor("wsc.mobile.home.subscreens.PickupInStore", {}, null, {}, {}, "wsc.editors.MobileEditor");
			 */
			scControllerUtils.openScreenInEditor(screenPath, {}, null, {}, {}, editorPath);

		};
		
		mobileHomeUtils.openScreenWithInputData = function(screenPath, inputData, editorPath){
			/*EXAMPLE VALUES
			 *scControllerUtils.openScreenInEditor("wsc.mobile.home.subscreens.PickupInStore", inputData, null, {}, {}, "wsc.editors.MobileEditor");
			 */
			scControllerUtils.openScreenInEditor(screenPath, inputData , null, {}, {}, editorPath);

		};
		
		mobileHomeUtils.buildInputForGetShipmentDetails = function(screen, shipmentModel, mashupRefId, action) {
			
			//var shipmentKey = scBaseUtils.getAttributeValue("Shipment.ShipmentKey", false, shipmentModel);
			var shipmentKey = shipmentModel.Shipment.ShipmentKey
			var inputModel = {};
			inputModel.Shipment={};
			inputModel.Shipment.ShipmentKey = shipmentKey;
			inputModel.Shipment.Action = action;
			var mashupContext = {};
			mashupContext.Action = action;
			//inputModel.Shipment = shipmentModel.Shipment;
			//inputModel.landingScreenForWizard = landingScreen;
			iasUIUtils.callApi(screen, inputModel, mashupRefId, null);
			
		};
		
		mobileHomeUtils.buildCompleteInputGetShipmentList = function(shipmentModel, complexQryModel, orderByModel) {
			var completeModel = {};
			completeModel.Shipment = {};
			if(!scBaseUtils.isVoid(shipmentModel.Shipment)){
				completeModel.Shipment = shipmentModel.Shipment;
			}
			if(!scBaseUtils.isVoid(complexQryModel.ComplexQuery)){
				completeModel.Shipment.ComplexQuery = complexQryModel.ComplexQuery;
			}
			if(!scBaseUtils.isVoid(orderByModel.OrderBy)){
				completeModel.Shipment.OrderBy = orderByModel.OrderBy;
			}
			
			return completeModel;
		};
		
		mobileHomeUtils.buildQryForShipmentList = function(filterOptionsModel) {
			
			var complexQryModel = {};
			complexQryModel.ComplexQuery={};
			complexQryModel.ComplexQuery.Operator="AND";
			complexQryModel.ComplexQuery.Or={};
			complexQryModel.ComplexQuery.Or.Exp=[];
			
			var pos = 0;
			var backroomPick;
			if(filterOptionsModel.Filter.BackroomPickup == "Y"){
				complexQryModel.ComplexQuery.Or.Exp[pos]=mobileHomeUtils.buildQueryElement("Status", "1100.70.06.10","FLIKE");
				pos++;
			}
			if(filterOptionsModel.Filter.PicksInProgress == "Y"){
				complexQryModel.ComplexQuery.Or.Exp[pos]=mobileHomeUtils.buildQueryElement("Status", "1100.70.06.20","FLIKE");
				pos++;
			}
			if(filterOptionsModel.Filter.CustomerPickup == "Y"){
				complexQryModel.ComplexQuery.Or.Exp[pos]=mobileHomeUtils.buildQueryElement("Status", "1100.70.06.30","FLIKE");
				pos++;
			}
			if(filterOptionsModel.Filter.ShipmentComplete == "Y"){
				complexQryModel.ComplexQuery.Or.Exp[pos]=mobileHomeUtils.buildQueryElement("Status", "1400","FLIKE");
				pos++;
			}
			
			if(filterOptionsModel.Filter.ReadyForPack == "Y"){
				complexQryModel.ComplexQuery.Or.Exp[pos]=mobileHomeUtils.buildQueryElement("Status", "1100.70.06.50","FLIKE");
				pos++;
			}
			if(filterOptionsModel.Filter.PackInProgress == "Y"){
				complexQryModel.ComplexQuery.Or.Exp[pos]=mobileHomeUtils.buildQueryElement("Status", "1100.70.06.70","FLIKE");
				pos++;
			}
			if(filterOptionsModel.Filter.Packed == "Y"){
				complexQryModel.ComplexQuery.Or.Exp[pos]=mobileHomeUtils.buildQueryElement("Status", "1300","FLIKE");
				pos++;
			}
			if(filterOptionsModel.Filter.Shipped == "Y"){
				complexQryModel.ComplexQuery.Or.Exp[pos]=mobileHomeUtils.buildQueryElement("Status", "1400","FLIKE");
				pos++;
			}
			//If no elements were added, blank out the return model
			if(pos == 0){
				complexQryModel={};
			}
			
			return complexQryModel;
		};
		
		mobileHomeUtils.buildOrderByModel = function(sortValue) {
			
			var orderByModel = {};
			if(!scBaseUtils.isVoid(sortValue)){
				orderByModel.OrderBy={};
				if(scBaseUtils.contains(sortValue,"Extn")){
					orderByModel.OrderBy.Extn={};
					orderByModel.OrderBy.Extn.Attribute={};
					var splitStr = sortValue.split("-");
					if(!scBaseUtils.isVoid(splitStr[0])){
						orderByModel.OrderBy.Extn.Attribute.Name = splitStr[0];
					}
					if(!scBaseUtils.isVoid(splitStr[1])){
						orderByModel.OrderBy.Extn.Attribute.Desc = splitStr[1];
					}
				}
				else{
					orderByModel.OrderBy.Attribute={};
					var splitStr = sortValue.split("-");
					if(!scBaseUtils.isVoid(splitStr[0])){
						orderByModel.OrderBy.Attribute.Name = splitStr[0];
					}
					if(!scBaseUtils.isVoid(splitStr[1])){
						orderByModel.OrderBy.Attribute.Desc = splitStr[1];
					}
				}
			}
			
			return orderByModel;
		};
		
		mobileHomeUtils.buildQueryElement = function(attributeName, attributeValue, qryType) {

			var qryElement = {};
			qryElement.Name = attributeName;
			qryElement.Value = attributeValue;
			qryElement.QryType = qryType;
			return qryElement;
		};
		
		mobileHomeUtils.getItemImageLocation = function(screen,widget,nameSpace,itemModel,options,dataValue){
			/*
			 * EXAMPLE VALUES
			 * url = "wsc/resources/css/icons/images/timeLeftOverdue.png"
			 * imgLoc = "wsc/resources/css/icons/images"
			 * fullImgLoc = "wscdev/wsc/resources/css/icons/images"
			*/
			var url = scModelUtils.getStringValueFromPath("CodeLongDescription", dataValue);
			var lastSlash = url.lastIndexOf("/");
			var imgLoc = url.slice(0,lastSlash);
			var fullImgLoc = iasUIUtils.getFullURLForImage(imgLoc);
			return fullImgLoc;
				
		};
		
		mobileHomeUtils.getItemImageId = function(screen,widget,nameSpace,itemModel,options,dataValue){
			/*
			 * EXAMPLE VALUES
			 * url = "wsc/resources/css/icons/images/timeLeftOverdue.png"
			 * imgId = "timeLeftOverdue.png"
			*/
			var url = scModelUtils.getStringValueFromPath("CodeLongDescription", dataValue);
			var lastSlash = url.lastIndexOf("/");
			var urlLength = url.length;
			var imgId = url.slice(lastSlash+1,urlLength);
			return imgId;	
		};
		mobileHomeUtils.applyOverdueStyling = function(unformattedValue,screen,widgetId,namespace,shipment) {
			
			var dueInDate = "";
			if(!scBaseUtils.isVoid(shipment) && !scBaseUtils.isVoid(shipment.Shipment.TimeRemaining) && wscShipmentUtils.showSLA(shipment.Shipment.Status.Status)) {
				dueInDate=shipment.Shipment.TimeRemaining;
				/**
				 * 	var isOverdue = "";
				 *	isOverdue = shipment.Shipment.IsOverdue;
				 *	if(scBaseUtils.equals(isOverdue,"true")){
				 *		scWidgetUtils.addClass(screen, widgetId, "pastDue");
				 *    }
				 */
				
				/**
				 * Defect 445014: Due In Label will appear in colored text based on image file name. 
				 * CSS class are added based image file names.
				 */				
				var imageUrl =  shipment.Shipment.ImageUrl;
				var cssClass = "";
				if(!scBaseUtils.isVoid(imageUrl) && imageUrl.lastIndexOf("/") != -1) {
					cssClass = imageUrl.substring(imageUrl.lastIndexOf("/") + 1, imageUrl.indexOf("."));
					if(!scBaseUtils.isVoid(cssClass)) {
						scWidgetUtils.addClass(screen, widgetId, cssClass);
						//scWidgetUtils.addClass(screen, "lbl_Status", cssClass);
					}
				}
			}
			return dueInDate;
		};
		mobileHomeUtils.buildNameFromShipment = function(screen, shipmentModel) {
			var fullName = "";
			if(!scBaseUtils.isVoid(shipmentModel) && !scBaseUtils.isVoid(shipmentModel.Shipment.BillToAddress)){
				var addressModel = shipmentModel.Shipment.BillToAddress;
				//Format the name using the country
				fullName = mobileHomeUtils.getFormattedNameDisplay(screen, addressModel);
			}
			
			return fullName;
		};
		mobileHomeUtils.getFormattedNameDisplay = function(screen, modelObject){
            var returnValue = "";
            if (!scBaseUtils.isVoid(modelObject)) {
                var sCountry = "";
                sCountry = scModelUtils.getStringValueFromPath("Country", modelObject);
                var sAddressKey = "";
                sAddressKey = scBaseUtils.stringConcat(sCountry, "_AddressNameFormat");
                if (scScreenUtils.hasBundleKey(screen, sAddressKey)) {
                    returnValue = scScreenUtils.getFormattedString(screen, sAddressKey, modelObject);
                } else {
                    returnValue = scScreenUtils.getFormattedString(screen, "AddressNameFormat", modelObject);
                }
            }
            return returnValue.trim();
		};
		
		mobileHomeUtils.showNextTask = function(screen, shipmentModel, identifierId, nextActionLink,statusLabel,relatedWidgetUIdBean) {
			
			var buttonTextKey = null, statusLabelKey = null;
			var bpPickPermission = "WSC000006", bpShipPermission = "WSC000017", packPermission = "WSC000019";
			
			var shipmentStatus = scModelUtils.getStringValueFromPath("Shipment.Status.Status", shipmentModel);
			var shipmentShipNode = scModelUtils.getStringValueFromPath("Shipment.ShipNode", shipmentModel);
			var currentStore = iasContextUtils.getFromContext("CurrentStore");
			
			if(scBaseUtils.equals(identifierId, "Pick")) {
				
				var holdLocation = scModelUtils.getStringValueFromPath("Shipment.HoldLocation", shipmentModel);
				var shipNode = scModelUtils.getStringValueFromPath("Shipment.ShipNode", shipmentModel);
				
				if(scBaseUtils.contains(shipmentStatus, "1100.70.06.10")) {
					if(scResourcePermissionUtils.hasPermission(bpPickPermission)) {
						buttonTextKey = "Action_Pick";
					}
					//statusLabelKey = "Label_ReadyForPicking";
		        } else if(scBaseUtils.contains(shipmentStatus, "1100.70.06.20")) {
		        	if(scResourcePermissionUtils.hasPermission(bpPickPermission)) {
		        		buttonTextKey = "Action_Continue";
					}
					//statusLabelKey = "Label_PickingInProgress";
		        } else if(scBaseUtils.contains(shipmentStatus, "1100.70.06.30")) {		        	
					
					scWidgetUtils.showWidget(screen, scBaseUtils.getStringValueFromBean("RecordCustomerPickupWidgetUId",relatedWidgetUIdBean), true, null);
					
		        	scWidgetUtils.hideWidget(screen, nextActionLink, false);
		        	//commenting hold location as customer pick available in mobile 466754
		        	/*if(scBaseUtils.isVoid(holdLocation)) {
		        		//statusLabelKey = "Label_AssignHold";
		        		if(scBaseUtils.equals(currentStore, shipmentShipNode)) {
		        			scWidgetUtils.showWidget(screen, scBaseUtils.getStringValueFromBean("HoldLocationWidgetUId",relatedWidgetUIdBean), true, null);
		        		}
		        	}*/ 
					/*else {
		        		//statusLabelKey = "Label_ReadyForCustomer";
		        		scWidgetUtils.hideWidget(screen, scBaseUtils.getStringValueFromBean("DueInLabelWidgetUId",relatedWidgetUIdBean), false);
		        		scWidgetUtils.hideWidget(screen, scBaseUtils.getStringValueFromBean("DueInIconWidgetUId",relatedWidgetUIdBean), false);
		        	}*/
					
		        } else if(scBaseUtils.contains(shipmentStatus, "1400")) {
		        	//statusLabelKey = "Label_PickedUpByCustomer";
		        	scWidgetUtils.hideWidget(screen, nextActionLink, false);
		        	scWidgetUtils.hideWidget(screen, scBaseUtils.getStringValueFromBean("DueInLabelWidgetUId",relatedWidgetUIdBean), false);
	        		scWidgetUtils.hideWidget(screen, scBaseUtils.getStringValueFromBean("DueInIconWidgetUId",relatedWidgetUIdBean), false);
		        	
		        } 
			} else if(scBaseUtils.equals(identifierId, "Ship")) {
				
				if(scBaseUtils.contains(shipmentStatus, "1100.70.06.10")) {
					if(scResourcePermissionUtils.hasPermission(bpShipPermission)) {
						buttonTextKey = "Action_Pick";
					}
					//statusLabelKey = "Label_ReadyForPicking";
		        } else if(scBaseUtils.contains(shipmentStatus, "1100.70.06.20")) {
		        	if(scResourcePermissionUtils.hasPermission(bpShipPermission)) {
		        		buttonTextKey = "Action_Continue";
					}
					//statusLabelKey = "Label_PickingInProgress";
		        } else if(scBaseUtils.contains(shipmentStatus, "1100.70.06.50")) {
		        	scWidgetUtils.hideWidget(screen, nextActionLink, false);
					//statusLabelKey = "Label_ReadyForPacking";
		        } else if(scBaseUtils.contains(shipmentStatus, "1300")) {
		        	//statusLabelKey = "Label_Packed";
			        scWidgetUtils.hideWidget(screen, nextActionLink, false);
			        scWidgetUtils.hideWidget(screen, scBaseUtils.getStringValueFromBean("PackWidgetUId",relatedWidgetUIdBean), false);
			        	
			    } else if(scBaseUtils.contains(shipmentStatus, "1400")) {
		    		//statusLabelKey = "Label_ShippedFromStore";
			        scWidgetUtils.hideWidget(screen, nextActionLink, false);
			        scWidgetUtils.hideWidget(screen, scBaseUtils.getStringValueFromBean("DueInLabelWidgetUId",relatedWidgetUIdBean), false);
	        		scWidgetUtils.hideWidget(screen, scBaseUtils.getStringValueFromBean("DueInIconWidgetUId",relatedWidgetUIdBean), false);
			        //scWidgetUtils.hideWidget(screen, scBaseUtils.getStringValueFromBean("PackWidgetUId",relatedWidgetUIdBean), false);
				}  
				
				
			} else if(scBaseUtils.equals(identifierId, "Pack")) {
				
				if(scBaseUtils.contains(shipmentStatus, "1100.70.06.50")) {
					if(scResourcePermissionUtils.hasPermission(packPermission)) {
						buttonTextKey = "Action_Pack";
					}
					//statusLabelKey = "Label_ReadyForPacking";
		        } else if(scBaseUtils.contains(shipmentStatus, "1100.70.06.70")) {
		        	if(scResourcePermissionUtils.hasPermission(packPermission)) {
		        		buttonTextKey = "Action_Continue";
					}
		        	//statusLabelKey = "Label_PackingInProgress";
			    } else if(scBaseUtils.contains(shipmentStatus, "1300")) {
			    	//statusLabelKey = "Label_Packed";
			        scWidgetUtils.hideWidget(screen, nextActionLink, false);
			        scWidgetUtils.hideWidget(screen, scBaseUtils.getStringValueFromBean("ShipWidgetUId",relatedWidgetUIdBean), false);
			    } else if(scBaseUtils.contains(shipmentStatus, "1400")) {
		    		//statusLabelKey = "Label_ShippedFromStore";
			        scWidgetUtils.hideWidget(screen, nextActionLink, false);
			        scWidgetUtils.hideWidget(screen, scBaseUtils.getStringValueFromBean("DueInLabelWidgetUId",relatedWidgetUIdBean), false);
	        		scWidgetUtils.hideWidget(screen, scBaseUtils.getStringValueFromBean("DueInIconWidgetUId",relatedWidgetUIdBean), false);
			       // scWidgetUtils.hideWidget(screen, scBaseUtils.getStringValueFromBean("ShipWidgetUId",relatedWidgetUIdBean), false);
				} 
				
				
			}
				
			
			if(buttonTextKey != null) {
				if(scBaseUtils.equals(currentStore, shipmentShipNode)) {
					scWidgetUtils.setValue(screen,nextActionLink,scScreenUtils.getString(screen,buttonTextKey));
					scWidgetUtils.showWidget(screen, nextActionLink, true, null);
				} else {
					 scWidgetUtils.hideWidget(screen, nextActionLink, false);
				}
			}
			
			if(statusLabelKey != null) {
				scWidgetUtils.setValue(screen,statusLabel,scScreenUtils.getString(screen,statusLabelKey), true);
			} /*else {
				scWidgetUtils.setValue(screen,statusLabel,scModelUtils.getStringValueFromPath("Shipment.Status.Description", shipmentModel), true);
			}*/
			
			
		};
    
    
		
		return mobileHomeUtils;
});