
scDefine(["scbase/loader!dojo/_base/declare","scbase/loader!extn/components/shipment/customerpickup/CustomerIdentificationExtnUI","scbase/loader!sc/plat/dojo/utils/ScreenUtils", "scbase/loader!sc/plat/dojo/utils/ModelUtils", "scbase/loader!ias/utils/ScreenUtils", "scbase/loader!wsc/components/common/utils/CommonUtils", "scbase/loader!sc/plat/dojo/utils/BaseUtils", "scbase/loader!ias/utils/ContextUtils", "scbase/loader!sc/plat/dojo/utils/WidgetUtils","scbase/loader!sc/plat/dojo/utils/EventUtils"]
,
function(			 
			    _dojodeclare
			 ,
			    _extnCustomerIdentificationExtnUI
			,
				_scScreenUtils
			,
				_scModelUtils
			,
				_iasScreenUtils
			,
				_wscCommonUtils
			,
				_scBaseUtils
			,
				_iasContextUtils
			,
				_scWidgetUtils
			,
				_scEventUtils
){ 
	return _dojodeclare("extn.components.shipment.customerpickup.CustomerIdentificationExtn", [_extnCustomerIdentificationExtnUI],{
	// custom code here
	initialize: function(
        event, bEvent, ctrl, args) {
            var pickupOrderNo = null;
            var shipmentDetailModel = null;
            shipmentDetailModel = _scScreenUtils.getModel(
            this, "ShipmentDetails");
            var custVerificationModel = _scScreenUtils.getModel(
            this, "CustomerVerficationMethodList");
            var custVerificationModelCommonCode = custVerificationModel.CommonCodeList.CommonCode;
            var custVerificationModelCommonCodeLength = custVerificationModelCommonCode.length;
            if(custVerificationModelCommonCodeLength==1)
            {
            	var custVerificationModelCommonCodeShortDesc = custVerificationModelCommonCode[0].CodeShortDescription;
            	var custVerificationNewModel = null;
				custVerificationNewModel = {};
				custVerificationNewModel.CustomerVerification = {};
				custVerificationNewModel.CustomerVerification.Method = {};
				custVerificationNewModel.CustomerVerification.Method = custVerificationModelCommonCodeShortDesc;
				_scScreenUtils.setModel(this, "extn_custMethod", custVerificationNewModel, null);
            }            
            pickupOrderNo = _scModelUtils.getStringValueFromPath("Shipment.DisplayOrderNo", shipmentDetailModel);
            _iasScreenUtils.updateEditorTitle(
            this, pickupOrderNo, _wscCommonUtils.getBundleKeyForTabHeader(
            pickupOrderNo));
            var originalShipmentKey = null;
            originalShipmentKey = _scModelUtils.getStringValueFromPath("Shipment.OriginalShipmentKey", shipmentDetailModel);
            if (
            _scBaseUtils.isVoid(
            originalShipmentKey)) {
                _iasScreenUtils.hideView(
                this, "previouslyPicked");
            }
			_iasScreenUtils.hideTabForSingleView(this,'tabPnl');
			if (_iasContextUtils.isMobileContainer()) {
                _scWidgetUtils.hideWidget(
                this, "lblOrderNo", false);
				var blankModel = {};
				var eventBean = {};
				_scBaseUtils.setAttributeValue("argumentList", blankModel, eventBean);
				_scBaseUtils.setAttributeValue("argumentList.Shipment", shipmentDetailModel, eventBean);
				_scEventUtils.fireEventToParent(this, "setWizardDescription", eventBean);
			}
        }
});
});

