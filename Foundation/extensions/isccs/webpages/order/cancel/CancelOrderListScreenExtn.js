
scDefine(["scbase/loader!dojo/_base/declare",
		  "scbase/loader!extn/order/cancel/CancelOrderListScreenExtnUI",
		  "scbase/loader!sc/plat/dojo/utils/ScreenUtils",
		  "scbase/loader!sc/plat/dojo/utils/BaseUtils",
		   "scbase/loader!sc/plat/dojo/utils/ModelUtils",
		   "scbase/loader!isccs/utils/OrderUtils",
		   "scbase/loader!sc/plat/dojo/utils/EventUtils",
		   "scbase/loader!isccs/utils/UIUtils"]
,
function(			 
		_dojodeclare,
		_extnCancelOrderListScreenExtnUI,
		_scScreenUtils,
		_scBaseUtils,
		_scModelUtils,
		_isccsOrderUtils,
		_scEventUtils,
		_isccsUIUtils
){ 
	return _dojodeclare("extn.order.cancel.CancelOrderListScreenExtn", [_extnCancelOrderListScreenExtnUI],{
	initializeChildScreen: function(
        event, bEvent, ctrl, args) {
            var getCompleteOrderLineList_output = null;
            getCompleteOrderLineList_output = _scScreenUtils.getModel(
            this, "getCompleteOrderLineList_output");
            var orderLinesList = null;
            orderLinesList = _scModelUtils.getModelListFromPath("Page.Output.OrderLineList.OrderLine", getCompleteOrderLineList_output);
            this.txQtyRuleSetValue = "";
            this.txQtyRuleSetValue = _scBaseUtils.getStringValueFromBean("txQtyRuleSetValue", args);
            this.checkLineModificationAllowed(
            orderLinesList);
        },
        checkLineModificationAllowed: function(
        orderLinesList) {
            var isLinesModificationAllowed = false;
            var rowsCount = 0;
            rowsCount = _scBaseUtils.getAttributeCount(
            orderLinesList);
            if (
            _scBaseUtils.equals(
            rowsCount, 0)) {
                isLinesModificationAllowed = true;
            }
            for (
            var i = 0;
            i < rowsCount;
            i = i + 1) {
                var indexObj = null;
                indexObj = orderLinesList[
                i];
                var availableCancelQty = 0;
                availableCancelQty = _isccsOrderUtils.calculateOrder_CancelQuantityWOFormat(
                this.txQtyRuleSetValue, indexObj, false);                
                var maxLineStatus = indexObj.MaxLineStatus;
                if (
                availableCancelQty > 0 && maxLineStatus.indexOf("3350.60")<0) {
                    isLinesModificationAllowed = true;
                    break;
                }
            }
            var eventDefn = null;
            var blankModel = null;
            eventDefn = {};
            blankModel = {};
            eventDefn["argumentList"] = blankModel;
            _scBaseUtils.setAttributeValue("argumentList.isLinesModificationAllowed", isLinesModificationAllowed, eventDefn);
            _scEventUtils.fireEventToParent(
            this, "initializeLayout", eventDefn);
        },
		handleGetCompleteOrdLineList: function(
        mashupRefOutput, mashupContext) {
            _scWidgetUtils.hideWidget(
            this, "gridXParentMessagePanel", false, null);
            var orderLinesList = null;
            orderLinesList = _scModelUtils.getModelListFromPath("Page.Output.OrderLineList.OrderLine", mashupRefOutput);
            this.checkLineModificationAllowed(
            orderLinesList);
            var eventDefn = null;
            eventDefn = {};
            blankModel = {};
            _scBaseUtils.setAttributeValue("argumentList", blankModel, eventDefn);
            _scEventUtils.fireEventToParent(
            this, "cancelType_onChange", eventDefn);
            this.selectCancelLines();
            _scScreenUtils.clearScreen(
            _isccsUIUtils.getWizardForScreen(
            this), null);
        },
		isGridRowDisabled: function(
        rowData, screen) {
            var availableCancelQty = 0;
            var maxLineStatus = rowData.MaxLineStatus;
            availableCancelQty = _isccsOrderUtils.calculateOrder_CancelQuantityWOFormat(
            this.txQtyRuleSetValue, rowData, false);
            if (
            availableCancelQty <= 0 || maxLineStatus.indexOf("3350.60")>=0) {
                return true;
            } else {
                return false;
            }
        }		
});
});