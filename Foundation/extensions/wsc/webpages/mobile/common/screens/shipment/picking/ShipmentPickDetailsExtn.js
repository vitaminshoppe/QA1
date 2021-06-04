
scDefine(["scbase/loader!dojo/_base/declare","scbase/loader!extn/mobile/common/screens/shipment/picking/ShipmentPickDetailsExtnUI","scbase/loader!sc/plat/dojo/utils/ScreenUtils","scbase/loader!ias/utils/UIUtils","scbase/loader!sc/plat/dojo/utils/BaseUtils","scbase/loader!ias/utils/BaseTemplateUtils","scbase/loader!sc/plat/dojo/utils/WidgetUtils","scbase/loader!sc/plat/dojo/utils/ModelUtils"]
,
function(			 
			    _dojodeclare
			 ,
			    _extnShipmentPickDetailsExtnUI
			,
				_scScreenUtils
			,
				_iasUIUtils
			,
				_scBaseUtils
			,
				_iasBaseTemplateUtils
			,
				_scWidgetUtils
			,
				_scModelUtils
){ 
	return _dojodeclare("extn.mobile.common.screens.shipment.picking.ShipmentPickDetailsExtn", [_extnShipmentPickDetailsExtnUI],{
	// custom code here
	
	extnCheckSTS:function(event, bEvent, ctrl, args)
	{
		var shipmentModel = null;
        shipmentModel = _scScreenUtils.getModel(this, "Shipment");	
		var orderNo = shipmentModel.Shipment.OrderNo;
		var orderInput = {};
        orderInput.Order = {};
        orderInput.Order.OrderNo = orderNo;   
        _iasUIUtils.callApi(this, orderInput, "extn_getOrderDetails", null);
	},
	
	handleMashupCompletion: function(mashupContext, mashupRefObj, mashupRefList, inputData, hasError, data) {
		
		if (_scBaseUtils.equals(mashupRefList[0].mashupRefId, "extn_getOrderDetails")) 
		{
           var modelOutput = mashupRefList[0].mashupRefOutput;
		   var bindings = null;
			var popupParams = null;
			bindings = {};
			popupParams = {};
			popupParams["screenInput"] = modelOutput;
			var dialogParams = null;
			dialogParams = {};
			dialogParams["closeCallBackHandler"] = "onOrderDetailsPopupClose";
			_iasUIUtils.openSimplePopup("extn.orderDetails.STSOrderDetails", "Check STS Order Details", this, popupParams, dialogParams);
        }
			_iasBaseTemplateUtils.handleMashupCompletion(
			mashupContext, mashupRefObj, mashupRefList, inputData, hasError, data, this);
	},
	
	onOrderDetailsPopupClose: function(actionPerformed, model, popupParams) {

	},
	
	initializeScreen: function(
        event, bEvent, ctrl, args) {
            var identifierMode = null;
            identifierMode = _scBaseUtils.getAttributeValue("identifierId", false, this);
            var shipmentModel = null;
            shipmentModel = _scScreenUtils.getModel(
            this, "Shipment");
            var stsFlag = shipmentModel.Shipment.Extn.ExtnBOSTSFlag;
            if (!_scBaseUtils.isVoid(stsFlag))
            {
                  if (_scBaseUtils.equals(stsFlag, "Y"))
                    {
                           _scWidgetUtils.showWidget(this, "extn_checkSTS", true, null);     
                    }  
            }
            var userId = null;
            userId = _scModelUtils.getStringValueFromPath("Shipment.AssignedToUserId", shipmentModel);
            if (
            _scBaseUtils.isVoid(
            userId)) {
                _scWidgetUtils.hideWidget(
                this, "img_User", false);
                _scWidgetUtils.hideWidget(
                this, "lbl_AssociateName", false);
            }
            var screenType = null;
            screenType = _scModelUtils.getStringValueFromPath("Shipment.ScreenType", shipmentModel);
            if (
            _scBaseUtils.equals(
            identifierMode, "Pick")) {
                this.initializeScreenPick(
                shipmentModel);
            } else if (
            _scBaseUtils.equals(
            identifierMode, "Ship")) {
                this.initializeScreenShip(
                shipmentModel);
            } else if (
            _scBaseUtils.equals(
            identifierMode, "Pack")) {
                this.initializeScreenPack(
                shipmentModel);
            }
            var urlString = null;
            urlString = _scModelUtils.getStringValueFromPath("Shipment.ImageUrl", shipmentModel);
            if (!(
            _scBaseUtils.isVoid(
            urlString))) {
                var imageUrlModel = null;
                imageUrlModel = _scModelUtils.createNewModelObjectWithRootKey("CommonCode");
                _scModelUtils.setStringValueAtModelPath("CommonCode.CodeLongDescription", urlString, imageUrlModel);
                if(window.navigator.appVersion.indexOf("iPhone OS 8_1") > -1) {
					return;
				}else{
					_scScreenUtils.setModel(this, "clockImageBindingValues", imageUrlModel, null);
				}
            }
        }
});
});

