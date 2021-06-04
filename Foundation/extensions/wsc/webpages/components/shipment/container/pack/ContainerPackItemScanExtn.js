
scDefine(["scbase/loader!dojo/_base/declare","scbase/loader!extn/components/shipment/container/pack/ContainerPackItemScanExtnUI","scbase/loader!wsc/components/shipment/container/pack/ContainerPackUtils", "scbase/loader!sc/plat/dojo/utils/WidgetUtils", "scbase/loader!sc/plat/dojo/utils/BaseUtils", "scbase/loader!ias/utils/ContextUtils"]
,
function(			 
			    _dojodeclare
			 ,
			    _extnContainerPackItemScanExtnUI
			,
				_wscContainerPackUtils
			,
				_scWidgetUtils
			,
				_scBaseUtils
			,
				_iasContextUtils
){ 
	return _dojodeclare("extn.components.shipment.container.pack.ContainerPackItemScanExtn", [_extnContainerPackItemScanExtnUI],{
	// custom code here
	refreshItemScanScreenData: function(
        event, bEvent, ctrl, args) {
            this.clearItemFilter();
            var containerList = null;
            var activeContainerModel = null;
            _wscContainerPackUtils.refreshCommonData(
            this, args);
            _scWidgetUtils.hideWidget(
            this, "tpLastProductScan", false);
            if (
            _scWidgetUtils.isWidgetVisible(
            this, "errorMsgPnl")) {
                _scWidgetUtils.hideWidget(
                this, "errorMsgPnl", false);
            }
            _scWidgetUtils.showWidget(
            this, "noProductScanPnl", false, "");
            activeContainerModel = _scBaseUtils.getValueFromPath("activeContainerInfo", args);
            if (!(
            _iasContextUtils.isMobileContainer())) {
                _scWidgetUtils.setFocusOnWidgetUsingUid(
                this, "txtScanField");
            }
            this.addOrRemoveCssToLPpanel();
            this.showProducts();
            if (
            _scBaseUtils.equals(
            this.isProductsPainted, "Y")) {
                this.showProducts();
            }
        }
});
});

