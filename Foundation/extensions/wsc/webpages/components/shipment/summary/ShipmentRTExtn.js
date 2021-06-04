scDefine(["scbase/loader!dojo/_base/declare","scbase/loader!sc/plat/dojo/utils/ScreenUtils","scbase/loader!ias/utils/UIUtils","scbase/loader!extn/components/shipment/summary/ShipmentRTExtnUI","scbase/loader!sc/plat/dojo/utils/BaseUtils","scbase/loader!sc/plat/dojo/utils/ModelUtils","scbase/loader!ias/utils/RelatedTaskUtils"]
,
function(            
                _dojodeclare,
                _scScreenUtils,
                _iasUIUtils,
                _extnShipmentRTExtnUI,
				_scBaseUtils,
				_scModelUtils,
				_iasRelatedTaskUtils
){
	 
    return _dojodeclare("extn.components.shipment.summary.ShipmentRTExtn", [_extnShipmentRTExtnUI],{
    printPickPackReceipt: function(
    event, bEvent, controlName, args) {
        var shipmentDetailsModel = null;
            shipmentDetailsModel = _scScreenUtils.getModel(
            this, "getShipmentDetails_output");
        var shipmentInput = {};
        shipmentInput.Shipment = {};
        shipmentInput.Shipment.ShipmentKey = shipmentDetailsModel.Shipment.ShipmentKey;
        _iasUIUtils.callApi(this, shipmentInput, "extn_printpickpackreceipt", null);
    },
    printCustomerReceipt: function(
    event, bEvent, controlName, args) {
        var shipmentDetailsModel = null;
            shipmentDetailsModel = _scScreenUtils.getModel(
            this, "getShipmentDetails_output");
        var shipmentInput = {};
        shipmentInput.Shipment = {};
        shipmentInput.Shipment.ShipmentKey = shipmentDetailsModel.Shipment.ShipmentKey;   
        _iasUIUtils.callApi(this, shipmentInput, "extn_Print_Customer_Receipt", null);
    },
    printPackReceipt: function(
    event, bEvent, controlName, args) {
        var shipmentDetailsModel = null;
            shipmentDetailsModel = _scScreenUtils.getModel(
            this, "getShipmentDetails_output");
        var shipmentInput = {};
        shipmentInput.Shipment = {};
        shipmentInput.Shipment.ShipmentKey = shipmentDetailsModel.Shipment.ShipmentKey;   
        _iasUIUtils.callApi(this, shipmentInput, "extn_Print_Pack_Receipt", null);
      },
    
	  disableAllTasks: function(
        event, bEvent, ctrl, args) {
            var shipmentDetailsModel = null;
            shipmentDetailsModel = _scBaseUtils.getAttributeValue("shipmentModel", false, args);
            var deliveryMethod = null;
            deliveryMethod = _scModelUtils.getStringValueFromPath("Shipment.DeliveryMethod", shipmentDetailsModel);
            if (
            _scBaseUtils.equals(
            deliveryMethod, "SHP")) {
                _iasRelatedTaskUtils.disableTaskInWebAndMobile(
                this, "lnk_RT_PrintPickTicket", false);
                _iasRelatedTaskUtils.disableTaskInWebAndMobile(
                this, "lnk_RT_RecordPickShipment", false);
                _iasRelatedTaskUtils.disableTaskInWebAndMobile(
                this, "lnk_RT_AssignToHold", false);
                _iasRelatedTaskUtils.disableTaskInWebAndMobile(
                this, "lnk_RT_PackShipment", false);
                _iasRelatedTaskUtils.disableTaskInWebAndMobile(
                this, "lnk_RT_PrintPackSlip", false);
                _iasRelatedTaskUtils.disableTaskInWebAndMobile(
                this, "lnk_RT_UnpackShipment", false);
				 _iasRelatedTaskUtils.disableTaskInWebAndMobile(
                this, "extn_printpickpackreceipt", false);
				 _iasRelatedTaskUtils.disableTaskInWebAndMobile(
                this, "extn_customerpickupreceipt", false);
            } else {}
        },
	hideOrShowRelatedTasks: function(
        event, bEvent, ctrl, args) {
            var shipmentDetailsModel = null;
            shipmentDetailsModel = _scBaseUtils.getAttributeValue("shipmentModel", false, args);
            var deliveryMethod = null;
            deliveryMethod = _scModelUtils.getStringValueFromPath("Shipment.DeliveryMethod", shipmentDetailsModel);
            var statusPath = null;
            statusPath = _scBaseUtils.getAttributeValue("statusPath", false, args);
            var status = null;
            status = _scModelUtils.getStringValueFromPath(
            statusPath, shipmentDetailsModel);
            if (
            _scBaseUtils.equals(
            deliveryMethod, "SHP")) {
                _iasRelatedTaskUtils.showTaskInWebAndMobile(
                this, "lnk_RT_PrintPickTicket", false);
                _iasRelatedTaskUtils.showTaskInWebAndMobile(
                this, "lnk_RT_RecordPickShipment", false);
                /*_iasRelatedTaskUtils.showTaskInWebAndMobile(
                this, "lnk_RT_AssignToHold", false); */
                _iasRelatedTaskUtils.showTaskInWebAndMobile(
                this, "lnk_RT_PackShipment", false);
                _iasRelatedTaskUtils.showTaskInWebAndMobile(
                this, "lnk_RT_PrintPackSlip", false);
		_iasRelatedTaskUtils.showTaskInWebAndMobile(
                this, "extn_printpackreceipt", false);
                _iasRelatedTaskUtils.showTaskInWebAndMobile(
                this, "lnk_RT_UnpackShipment", false);
				_iasRelatedTaskUtils.showTaskInWebAndMobile(
                this, "extn_printpickpackreceipt", false);
                if (
                _scBaseUtils.contains(
                status, "1100.70.06.10")) {
                    _iasRelatedTaskUtils.enableTaskInWebAndMobile(
                    this, "lnk_RT_PrintPickTicket", false);
                    _iasRelatedTaskUtils.enableTaskInWebAndMobile(
                    this, "lnk_RT_RecordPickShipment", false);
                } else if (
                _scBaseUtils.contains(
                status, "1100.70.06.20")) {
                    _iasRelatedTaskUtils.enableTaskInWebAndMobile(
                    this, "lnk_RT_PrintPickTicket", false);
                    _iasRelatedTaskUtils.enableTaskInWebAndMobile(
                    this, "lnk_RT_RecordPickShipment", false);
                   /* _iasRelatedTaskUtils.enableTaskInWebAndMobile(
                    this, "lnk_RT_AssignToHold", false); */
                } else if (
                _scBaseUtils.contains(
                status, "1100.70.06.50")) {
                    /*_iasRelatedTaskUtils.enableTaskInWebAndMobile(
                    this, "lnk_RT_AssignToHold", false); */
                    _iasRelatedTaskUtils.enableTaskInWebAndMobile(
                    this, "lnk_RT_PackShipment", false);
					_iasRelatedTaskUtils.enableTaskInWebAndMobile(
                    this, "extn_printpickpackreceipt", false);
                } else if (
                _scBaseUtils.contains(
                status, "1100.70.06.70")) {
                    _iasRelatedTaskUtils.enableTaskInWebAndMobile(
                    this, "lnk_RT_PackShipment", false);
                    _iasRelatedTaskUtils.enableTaskInWebAndMobile(
                    this, "lnk_RT_UnpackShipment", false);
                } else if (
                _scBaseUtils.contains(
                status, "1300")) {
                   _iasRelatedTaskUtils.enableTaskInWebAndMobile(
                    this, "extn_printpackreceipt", false);
                    _iasRelatedTaskUtils.enableTaskInWebAndMobile(
                    this, "lnk_RT_UnpackShipment", false);
                } else if ((
                _scBaseUtils.contains(
                status, "1400")) || (_scBaseUtils.contains(
                status, "1600.002"))) {
					_iasRelatedTaskUtils.enableTaskInWebAndMobile(
                    this, "extn_printpackreceipt", false);
				}
            } else if (
            _scBaseUtils.equals(
            deliveryMethod, "PICK")) {
                _iasRelatedTaskUtils.showTaskInWebAndMobile(
                this, "lnk_RT_PrintPickTicket", false);
                _iasRelatedTaskUtils.showTaskInWebAndMobile(
                this, "lnk_RT_RecordPickShipment", false);
                /*_iasRelatedTaskUtils.showTaskInWebAndMobile(
                this, "lnk_RT_AssignToHold", false); */
                _iasRelatedTaskUtils.showTaskInWebAndMobile(
                this, "lnk_RT_RecordCustomerPick", false);
                _iasRelatedTaskUtils.showTaskInWebAndMobile(
                this, "lnk_RT_PrintReceipt", false);
				_iasRelatedTaskUtils.showTaskInWebAndMobile(this, "extn_printpickpackreceipt", false);
				_iasRelatedTaskUtils.showTaskInWebAndMobile(this, "extn_customerpickupreceipt", false);
                if (
                _scBaseUtils.contains(
                status, "1100.70.06.10")) {
                    _iasRelatedTaskUtils.enableTaskInWebAndMobile(
                    this, "lnk_RT_PrintPickTicket", false);
                    _iasRelatedTaskUtils.enableTaskInWebAndMobile(
                    this, "lnk_RT_RecordPickShipment", false);
                } else if (
                _scBaseUtils.contains(
                status, "1100.70.06.20")) {
                    _iasRelatedTaskUtils.enableTaskInWebAndMobile(
                    this, "lnk_RT_PrintPickTicket", false);
                    _iasRelatedTaskUtils.enableTaskInWebAndMobile(
                    this, "lnk_RT_RecordPickShipment", false);
                   /* _iasRelatedTaskUtils.enableTaskInWebAndMobile(
                    this, "lnk_RT_AssignToHold", false); */
                } else if (
                _scBaseUtils.contains(
                status, "1100.70.06.30")) {
                    /*_iasRelatedTaskUtils.enableTaskInWebAndMobile(
                    this, "lnk_RT_AssignToHold", false); */
                    _iasRelatedTaskUtils.enableTaskInWebAndMobile(
                    this, "lnk_RT_RecordCustomerPick", false);
					_iasRelatedTaskUtils.enableTaskInWebAndMobile(
                    this, "extn_printpickpackreceipt", false);
                } else if ((
                _scBaseUtils.contains(
                status, "1400")) || (_scBaseUtils.contains(
                status, "1600.002"))) {
                   _iasRelatedTaskUtils.enableTaskInWebAndMobile(
                    this, "extn_customerpickupreceipt", false);
					
                }
            }
            if (!(
            this.hasPermissionForPrintPickTicket(
            shipmentDetailsModel))) {
                _iasRelatedTaskUtils.hideTaskInWebAndMobile(
                this, "lnk_RT_PrintPickTicket", false);
            }
            if (!(
            this.hasPermissionForBackroomPick(
            shipmentDetailsModel))) {
                _iasRelatedTaskUtils.hideTaskInWebAndMobile(
                this, "lnk_RT_RecordPickShipment", false);
            }
        }

});
});
