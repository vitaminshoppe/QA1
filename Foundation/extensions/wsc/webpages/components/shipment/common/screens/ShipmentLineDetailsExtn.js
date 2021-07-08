
scDefine(["scbase/loader!dojo/_base/declare","scbase/loader!sc/plat/dojo/utils/ScreenUtils","scbase/loader!sc/plat/dojo/utils/BaseUtils","scbase/loader!sc/plat/dojo/utils/ModelUtils","scbase/loader!extn/components/shipment/common/screens/ShipmentLineDetailsExtnUI","scbase/loader!ias/utils/UIUtils","scbase/loader!wsc/components/shipment/common/utils/ShipmentUtils"]
,
function(			 
			    _dojodeclare,
			_scScreenUtils,
			_scBaseUtils,
			_scModelUtils,
			_extnShipmentLineDetailsExtnUI,
			_iasUIUtils,
			_wscShipmentUtils
){ 
	return _dojodeclare("extn.components.shipment.common.screens.ShipmentLineDetailsExtn", [_extnShipmentLineDetailsExtnUI],{
		AliasValue: function(event, bEvent, ctrl, args)
		{
			var shipmentModel = null;
            shipmentModel = _scScreenUtils.getModel(this, "ShipmentLine");
            var itemAlias = null;
			itemAlias = _scModelUtils.getModelObjectFromPath("ShipmentLine.OrderLine.ItemDetails.ItemAliasList.ItemAlias", shipmentModel); 
           for (var length = 0; length < _scBaseUtils.getAttributeCount(itemAlias); length++) 
                {
					var name =_scModelUtils.getStringValueFromPath("AliasName", itemAlias[length]);
					if(_scBaseUtils.equals(name,"UPC"))
                    {
                        var AliasValue = _scModelUtils.getStringValueFromPath("AliasValue", itemAlias[length]);
                        return AliasValue;
                    }
                }
            },
			
        openShortageResolutionPopup: function(event, bEvent, ctrl, args) {
            var screenInputModel = null;
            var backroomPickedQuantity = null;
            var shortageQuantity = null;
			      var shipmentDetails = _scScreenUtils.getModel(this,"Shipment");
            var zero = 0;
            var codeType = null;
            screenInputModel = _scScreenUtils.getTargetModel(
            this, "Shortage_Output", null);
            if (
            _scBaseUtils.equals("CustomerPick", this.flowName)) {
                codeType = "YCD_CUST_VERFN_TYP";
            } else if (
            _scBaseUtils.equals("BackroomPick", this.flowName)) {
                var deliveryMethod = _scModelUtils.getStringValueFromPath("Shipment.DeliveryMethod",shipmentDetails);
				        if(_scBaseUtils.equals("SHP", deliveryMethod)){
					         codeType = "VSI_BOSS_PICK_SHORT";
				         }else{
					        codeType = "YCD_PICK_SHORT_RESOL";
				       }
            } else if (
            _scBaseUtils.equals("ContainerPack", this.flowName)) {
                codeType = "YCD_PACK_SHORT_RESOL";
            }
            _scModelUtils.setStringValueAtModelPath("CommonCode.CodeType", codeType, screenInputModel);
            var shipmentLineModel = null;
            shipmentLineModel = _scScreenUtils.getModel(
            this, "ShipmentLine");
            backroomPickedQuantity = _scModelUtils.getNumberValueFromPath("ShipmentLine.BackroomPickedQuantity", shipmentLineModel);
            if (!(
            _iasUIUtils.isValueNumber(
            backroomPickedQuantity))) {
                backroomPickedQuantity = zero;
            }
            _scModelUtils.setNumberValueAtModelPath("ShipmentLine.DisplayQty", backroomPickedQuantity, shipmentLineModel);
            _scModelUtils.setStringValueAtModelPath("ShipmentLine.DisplayShortQty", _wscShipmentUtils.subtract(
            _scModelUtils.getNumberValueFromPath("ShipmentLine.Quantity", shipmentLineModel), backroomPickedQuantity), shipmentLineModel);
            var bindings = null;
            bindings = {};
            var screenConstructorParams = null;
            screenConstructorParams = {};
            _scModelUtils.addStringValueToModelObject("flowName", this.flowName, screenConstructorParams);
            bindings["ShipmentLine"] = shipmentLineModel;
            var popupParams = null;
            popupParams = {};
            popupParams["screenInput"] = screenInputModel;
            popupParams["outputNamespace"] = "ShortedShipmentLineModel";
            popupParams["binding"] = bindings;
            popupParams["screenConstructorParams"] = screenConstructorParams;
            var dialogParams = null;
            dialogParams = {};
            dialogParams["closeCallBackHandler"] = "onShortageReasonSelection";
            dialogParams["class"] = "popupTitleBorder fixedActionBarDialog";
            _iasUIUtils.openSimplePopup("wsc.components.shipment.common.screens.ShortageReasonPopup", "Title_ShortageReason", this, popupParams, dialogParams);
        }			
			
	});
});
