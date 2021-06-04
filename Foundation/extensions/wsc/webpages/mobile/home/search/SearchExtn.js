
scDefine(["scbase/loader!dojo/_base/declare","scbase/loader!extn/mobile/home/search/SearchExtnUI","scbase/loader!sc/plat/dojo/utils/BaseUtils","scbase/loader!sc/plat/dojo/utils/ModelUtils","scbase/loader!ias/utils/ContextUtils","scbase/loader!wsc/mobile/home/utils/MobileHomeUtils","scbase/loader!ias/utils/BaseTemplateUtils"]
,
function(			 
			    _dojodeclare
			 ,
			    _extnSearchExtnUI
			,
				_scBaseUtils
			,
				_scModelUtils
			,
				_iasContextUtils
			,
				_wscMobileHomeUtils
			,
				_iasBaseTemplateUtils
){ 
	return _dojodeclare("extn.mobile.home.search.SearchExtn", [_extnSearchExtnUI],{
	// custom code 
	searchOrders: function(
        event, bEvent, ctrl, args) {
            if (
            this.isSearchCriteriaValid()) {
                var shipmentSearchCriteriaModel = null;
                var includeOtherStoresModel = null;
                shipmentSearchCriteriaModel = _scBaseUtils.getTargetModel(
                this, "getShipmentSearch_input", null);
                if (
                _scBaseUtils.isVoid(
                shipmentSearchCriteriaModel)) {
                    shipmentSearchCriteriaModel = _scModelUtils.createModelObjectFromKey("Shipment", shipmentSearchCriteriaModel);
                }
				/*oms-2859*/
                if(!_scBaseUtils.isVoid(shipmentSearchCriteriaModel))
                {
                        if(!_scBaseUtils.isVoid(shipmentSearchCriteriaModel.Shipment.OrderNo))
                        {
                             shipmentSearchCriteriaModel.Shipment.OrderNo = shipmentSearchCriteriaModel.Shipment.OrderNo.toUpperCase();        
                        }
                }    
				/*oms-2859 end*/
				/*oms-2827*/
				if(!_scBaseUtils.isVoid(shipmentSearchCriteriaModel))
                {
                        if(!_scBaseUtils.isVoid(shipmentSearchCriteriaModel.Shipment.Extn))
                        {
                            if(!_scBaseUtils.isVoid(shipmentSearchCriteriaModel.Shipment.Extn.ExtnCaseInsensitiveFirstName))
								{
									 shipmentSearchCriteriaModel.Shipment.Extn.ExtnCaseInsensitiveFirstName = shipmentSearchCriteriaModel.Shipment.Extn.ExtnCaseInsensitiveFirstName.toLowerCase();        
								}
							if(!_scBaseUtils.isVoid(shipmentSearchCriteriaModel.Shipment.Extn.ExtnCaseInsensitiveLastName))
								{
									 shipmentSearchCriteriaModel.Shipment.Extn.ExtnCaseInsensitiveLastName = shipmentSearchCriteriaModel.Shipment.Extn.ExtnCaseInsensitiveLastName.toLowerCase();        
								}
							if(!_scBaseUtils.isVoid(shipmentSearchCriteriaModel.Shipment.Extn.ExtnCaseInsensitiveEmail))
								{
									 shipmentSearchCriteriaModel.Shipment.Extn.ExtnCaseInsensitiveEmail = shipmentSearchCriteriaModel.Shipment.Extn.ExtnCaseInsensitiveEmail.toLowerCase();        
								}    
                        }
                } 
                /*oms-2827 end*/  
                includeOtherStoresModel = _scBaseUtils.getTargetModel(
                this, "IncludeOrdersPickedInOtherStore", null);
                if (
                _scBaseUtils.equals(
                _scModelUtils.getStringValueFromPath("isChecked", includeOtherStoresModel), "Y")) {
                    _scModelUtils.setStringValueAtModelPath("Shipment.ShipNode", " ", shipmentSearchCriteriaModel);
                } else {
                    _scModelUtils.setStringValueAtModelPath("Shipment.ShipNode", _iasContextUtils.getFromContext("CurrentStore"), shipmentSearchCriteriaModel);
                }
                _scModelUtils.setStringValueAtModelPath("Shipment.OrderByAttribute", this.orderByAttribute, shipmentSearchCriteriaModel);
                _scBaseUtils.removeBlankAttributes(
                shipmentSearchCriteriaModel);
                _iasContextUtils.addToContext("SearchCriteria", shipmentSearchCriteriaModel);
                _wscMobileHomeUtils.openScreenWithInputData("wsc.mobile.home.search.SearchResult", shipmentSearchCriteriaModel, "wsc.mobile.editors.MobileEditor");
            } else {
                _iasBaseTemplateUtils.showMessage(
                this, "InvalidSearchCriteria", "error", null);
            }
        }
});
});

