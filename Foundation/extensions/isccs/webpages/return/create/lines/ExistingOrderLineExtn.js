
scDefine(["scbase/loader!dojo/_base/declare","scbase/loader!extn/return/create/lines/ExistingOrderLineExtnUI","scbase/loader!sc/plat/dojo/utils/ModelUtils", "scbase/loader!sc/plat/dojo/utils/ScreenUtils","scbase/loader!sc/plat/dojo/utils/BaseUtils","scbase/loader!sc/plat/dojo/utils/WidgetUtils"]
,
function(			 
			    _dojodeclare
			 ,
			    _extnExistingOrderLineExtnUI,
				_scModelUtils,
				_scScreenUtils,
				_scBaseUtils,
				_scWidgetUtils
				
){ 
	return _dojodeclare("extn.return.create.lines.ExistingOrderLineExtn", [_extnExistingOrderLineExtnUI],{
	// custom code here
	
	    getReturnQuantityMessage: function(unformattedValue, widget, screen, namespace, orderLine) {
            var sMessage = null;
            var extnReshipedlineFlag= _scModelUtils.getStringValueFromPath("OrderLine.Extn.ExtnReshippedLineFlag",orderLine);
			
            if(!_scBaseUtils.isVoid(extnReshipedlineFlag) && _scBaseUtils.equals(extnReshipedlineFlag,'Y')){
                sMessage = _scScreenUtils.getString(this, "extn_Restrict_Return_Reship");
            }else{
               sMessage = _scScreenUtils.getString(this, "TheSelectedLineIsNotReturnable");
            } 
            if (
            _scBaseUtils.equals(
            this.screenMode, "ReturnDetails")) {
                _scWidgetUtils.hideWidget(
                this, "pnlNoReturnableQuantity", true);
            } else if (
            _scModelUtils.getNumberValueFromPath("OrderLine.ReturnableQty", orderLine) <= 0) {
                _scWidgetUtils.hideWidget(
                this, "btnOverrideReturn", true);
                _scWidgetUtils.showWidget(
                this, "pnlNoReturnableQuantity", true, null);
                this.setScreenEnabled(
                false);
            } else {
                _scWidgetUtils.hideWidget(
                this, "pnlNoReturnableQuantity", true);
            }
            if (
            _scBaseUtils.equals(
            this.screenMode, "SalesOrderLineDetails")) {
                if (
                _scModelUtils.getNumberValueFromPath("OrderLine.OrderedQty", orderLine) > 0) {
                    _scWidgetUtils.setWidgetMandatory(
                    this, "cmbReturnReason");
                } else {
                    _scWidgetUtils.setWidgetNonMandatory(
                    this, "cmbReturnReason");
                }
            }
            return sMessage;
        }
});
});

