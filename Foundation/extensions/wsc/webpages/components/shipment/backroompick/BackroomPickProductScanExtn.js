
scDefine(["scbase/loader!dojo/_base/declare","scbase/loader!extn/components/shipment/backroompick/BackroomPickProductScanExtnUI", "scbase/loader!sc/plat/dojo/utils/ScreenUtils", "scbase/loader!ias/utils/UIUtils", "scbase/loader!ias/utils/WizardUtils", "scbase/loader!sc/plat/dojo/utils/EventUtils", "scbase/loader!ias/utils/ScreenUtils", "scbase/loader!ias/utils/ContextUtils", "scbase/loader!sc/plat/dojo/utils/WidgetUtils", "scbase/loader!sc/plat/dojo/utils/BaseUtils", "scbase/loader!sc/plat/dojo/utils/ModelUtils", "scbase/loader!wsc/components/common/utils/CommonUtils"]
,
function(			 
			    _dojodeclare
			 ,
			    _extnBackroomPickProductScanExtnUI
			,
				_scScreenUtils
			,
				_iasUIUtils
			,
				_iasWizardUtils
			,
				_scEventUtils
			,
				_iasScreenUtils
			,
				_iasContextUtils
			,
				_scWidgetUtils
			,
				_scBaseUtils
			,
				_scModelUtils
			,
				_wscCommonUtils
){ 
	return _dojodeclare("extn.components.shipment.backroompick.BackroomPickProductScanExtn", [_extnBackroomPickProductScanExtnUI],{
	// custom code here
	
	// oms-2949 - Below code is modified to skip staging location screen
	gotoNextScreen: function() {
            _scScreenUtils.clearScreen(
            this, "translateBarCode_input");
            var parentScreen = null;
            parentScreen = _iasUIUtils.getParentScreen(
            this, true);
			/*
            if (_scWizardUtils.isCurrentPageLastEntity(
            parentScreen)) {
                _iasWizardUtils.setActionPerformedOnWizard(
                parentScreen, "CONFIRM");
            } else {
                _iasWizardUtils.setActionPerformedOnWizard(
                parentScreen, "NEXT");
            }*/
			_iasWizardUtils.setActionPerformedOnWizard(
                parentScreen, "CONFIRM");
            _scEventUtils.fireEventToParent(
            this, "onSaveSuccess", null);
		},
	
	initializeScreen: function(
        event, bEvent, ctrl, args) {
			/* Code to modify the Next Button label to Finish Pick */
            var parentScreen = null;
            parentScreen = _iasUIUtils.getParentScreen(
            this, true);
            _iasWizardUtils.setLabelOnNavigationalWidget(
                 parentScreen, "nextBttn", _scScreenUtils.getString(parentScreen, "Label_Finish_Pick"));
            _iasWizardUtils.setLabelOnNavigationalWidget(
                 parentScreen, "confirmBttn", _scScreenUtils.getString(parentScreen, "Label_Finish_Pick"));
            var inputShipmentModel = null;
            var eventBean = null;
            eventBean = {};
            inputShipmentModel = _scScreenUtils.getModel(
            this, "backroomPickShipmentDetails_output");
            _iasScreenUtils.hideSystemMessage(
            this);
            if (
            _iasContextUtils.isMobileContainer()) {
                _scWidgetUtils.hideWidget(
                this, "shipmentDetails", false);
                var blankModel = null;
                blankModel = {};
                _scBaseUtils.setAttributeValue("argumentList", blankModel, eventBean);
                _scBaseUtils.setAttributeValue("argumentList.Shipment", inputShipmentModel, eventBean);
                _scEventUtils.fireEventToParent(
                this, "setWizardDescription", eventBean);
            }
            var wizardScreen = null;
            var pickupOrderNo = null;
            var deliveryMethod = null;
            var wizardTitleKey = null;
            var shipmentTypeModel = null;
            shipmentTypeModel = {};
            shipmentTypeModel = _scModelUtils.createModelObjectFromKey("Shipment", shipmentTypeModel);
            pickupOrderNo = _wscCommonUtils.getDisplayOrderNumber(
            _scModelUtils.getStringValueFromPath("Shipment.DisplayOrderNo", inputShipmentModel), "LITERAL");
            _iasScreenUtils.updateEditorTitle(
            this, pickupOrderNo, "Title_Backroom_Pickup_Order");
            deliveryMethod = _scModelUtils.getStringValueFromPath("Shipment.DeliveryMethod", inputShipmentModel);
            _scModelUtils.setStringValueAtModelPath("Shipment.DeliveryMethod", deliveryMethod, shipmentTypeModel);
            _iasUIUtils.setWizardModel(
            this, "ShipmentType", shipmentTypeModel, null);
            if (
            _scBaseUtils.equals(
            deliveryMethod, "SHP")) {
                wizardTitleKey = "Title_BPForShipOrder";
            } else if (
            _scBaseUtils.equals(
            deliveryMethod, "PICK")) {
                wizardTitleKey = "Title_BPForPickOrder";
            }
            eventBean = {};
            _scBaseUtils.setAttributeValue("argumentList.titleKey", wizardTitleKey, eventBean);
            _scEventUtils.fireEventToParent(
            this, "setWizardTitle", eventBean);
            var urlString = null;
            urlString = _scModelUtils.getStringValueFromPath("Shipment.ImageUrl", inputShipmentModel);
            if (!(
            _scBaseUtils.isVoid(
            urlString))) {
                var imageUrlModel = null;
                imageUrlModel = _scModelUtils.createNewModelObjectWithRootKey("CommonCode");
                _scModelUtils.setStringValueAtModelPath("CommonCode.CodeLongDescription", urlString, imageUrlModel);
                _scModelUtils.setStringValueAtModelPath("CommonCode.CodeShortDescription", _scModelUtils.getStringValueFromPath("Shipment.ImageAltText", inputShipmentModel), imageUrlModel);
                _scScreenUtils.setModel(
                this, "clockImageBindingValues", imageUrlModel, null);
            }
        }
});
});

