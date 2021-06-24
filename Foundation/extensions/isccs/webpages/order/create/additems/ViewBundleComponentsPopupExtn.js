
scDefine(["scbase/loader!dojo/_base/declare","scbase/loader!extn/order/create/additems/ViewBundleComponentsPopupExtnUI","scbase/loader!isccs/utils/ContextUtils", "scbase/loader!isccs/utils/ItemUtils", "scbase/loader!isccs/utils/OrderUtils", "scbase/loader!isccs/utils/UIUtils", "scbase/loader!isccs/utils/UOMUtils", "scbase/loader!sc/plat/dojo/utils/BaseUtils", "scbase/loader!sc/plat/dojo/utils/EditorUtils", "scbase/loader!sc/plat/dojo/utils/GridxUtils", "scbase/loader!sc/plat/dojo/utils/ModelUtils", "scbase/loader!sc/plat/dojo/utils/ScreenUtils", "scbase/loader!sc/plat/dojo/utils/WidgetUtils"]
,
function(			 
			    _dojodeclare
			 ,
			    _extnViewBundleComponentsPopupExtnUI
			 ,  _isccsContextUtils
			 ,  _isccsItemUtils
			 ,  _isccsOrderUtils
			 ,  _isccsUIUtils
			 ,  _isccsUOMUtils
			 ,  _scBaseUtils
			 ,  _scEditorUtils
			 ,  _scGridxUtils
			 ,  _scModelUtils
			 ,  _scScreenUtils
			 ,  _scWidgetUtils	
){ 
	return _dojodeclare("extn.order.create.additems.ViewBundleComponentsPopupExtn", [_extnViewBundleComponentsPopupExtnUI],{
	// custom code here
	
	        initializeScreen: function(event, bEvent, ctrl, args) {
				var inputOrderModel = null;
				var orderLineModel = null;
				var vInitialInputData = _scScreenUtils.getInitialInputData(this);
				var whlResolveHoldsPopupFlag = _scModelUtils.getStringValueFromPath("Order.Extn.ExtnWholesaleResolveHoldPopup", vInitialInputData);
				if(_scBaseUtils.isVoid(whlResolveHoldsPopupFlag) || _scBaseUtils.equals(whlResolveHoldsPopupFlag,"N")){
					inputOrderModel = _scScreenUtils.getModel(
						this, "getCompleteOrderDetails_output");
					orderLineModel = _scScreenUtils.getModel(
						this, "orderLineModel_output");
					if (
						_scBaseUtils.isVoid(
						_scModelUtils.getStringValueFromPath("OrderLine.OrderLineKey", orderLineModel))) {
						this.showBundleComponentsFromItem();
						_scWidgetUtils.hideWidget(
						this, "lblAdjustment", false);
						_scWidgetUtils.hideWidget(
						this, "lblLineTotal", false);
					} else {
						this.showBundleComponentsFromOrderLine(
						orderLineModel, inputOrderModel);
						_scWidgetUtils.showWidget(
						this, "lblAdjustment", false, null);
						_scWidgetUtils.showWidget(
						this, "lblLineTotal", false, null);
					}
					if (!(
					_isccsItemUtils.isShipIndBundleParentItem(
					this, orderLineModel))) {
					_scGridxUtils.hideTableColumn(
					this, "ComponentsGrid", "AvailableOn", "Product");
					_scGridxUtils.refreshGridxLayout(
					this, "ComponentsGrid");
					}
					_scScreenUtils.setModel(
					this, "orderLineModel_output", orderLineModel, null);
				}
			},
		
		extn_afterScreenInit:function(event, bEvent, ctrl, args) {
			var inputOrderModel = null;
            var orderLineModel = null;
            var vInitialInputData = _scScreenUtils.getInitialInputData(this);
			 var whlResolveHoldsPopupFlag = _scModelUtils.getStringValueFromPath("Order.Extn.ExtnWholesaleResolveHoldPopup", vInitialInputData);
                if(_scBaseUtils.equals(whlResolveHoldsPopupFlag,"Y")){

					var holdTypes = _scModelUtils.getModelObjectFromPath("Order.Extn",vInitialInputData);
					var list = _scModelUtils.getModelObjectFromPath("OrderHoldTypes.OrderHoldType",holdTypes);
					for(var i=0; i< list.length;i++){
						var holdType = list[i];
						var status = _scModelUtils.getStringValueFromPath("Status",holdType);
						if(status == '1300'){
							isccs.utils.ModelUtils.removeItemFromArray(list,holdType);
							i--;
						}else if(status == '1100'){
							_scModelUtils.setStringValueAtModelPath("HoldTypeDescription","WHOLESA LE oholdsafsaf",holdType);
						}					                
					}       
					_scScreenUtils.setModel(this,"extn_getOrderHoldTypeList",holdTypes);         	
	                _scWidgetUtils.hideWidget(this, "pnlComponents", false);
	                _scWidgetUtils.hideWidget(this, "ComponentsGrid", false);
	                _scWidgetUtils.hideWidget(this, "parentDetailsPanel", false);
	                _scWidgetUtils.hideWidget(this, "systemMessagePanelPopup", false);
	                _scWidgetUtils.showWidget(this, "extn_contentpane_resolvedHolds", false);
	                _scWidgetUtils.showWidget(this, "extn_gridx_resolvedHolds", false);
					_scWidgetUtils.showWidget(this, "extn_button_apply", false);
				}
		},


		extn_resolveHoldOnApply: function(event, bEvent, ctrl, args) {
			var inputOrderModel = null;
            var orderLineModel = null;
			var vInitialInputData = _scScreenUtils.getInitialInputData(this);
			var popupOutput = _scGridxUtils.getSelectedTargetRecordsUsingUId(this,"extn_gridx_resolvedHolds");
			_scScreenUtils.setPopupOutput(
			this, popupOutput);
			_scWidgetUtils.closePopup(
            this, "APPLY", false);
		},
		
        getAppliedToDescription: function(
        gridReference, rowIndex, columnIndex, gridRowRecord, unformattedValue) {
            var orderLineKey = null;
            var retString = null;
            orderLineKey = _scModelUtils.getStringValueFromPath("OrderLine.OrderLineKey", gridRowRecord, true);
            if (
            _scBaseUtils.isVoid(
            orderLineKey)) {
                retString = _scScreenUtils.getString(
                this, "Order");
                return retString;
            } else {
                retString = _scScreenUtils.getString(
                this, "OrderLine");
                return retString;
            }
            return retString;
        }		
});
});

