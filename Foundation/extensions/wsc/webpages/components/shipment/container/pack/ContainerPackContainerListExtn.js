
scDefine(["scbase/loader!dojo/_base/declare","scbase/loader!extn/components/shipment/container/pack/ContainerPackContainerListExtnUI",
"scbase/loader!sc/plat/dojo/utils/ScreenUtils", "scbase/loader!sc/plat/dojo/utils/WidgetUtils", "scbase/loader!ias/utils/ContextUtils", "scbase/loader!ias/utils/UIUtils", "scbase/loader!sc/plat/dojo/utils/BaseUtils", "scbase/loader!wsc/components/shipment/container/pack/ContainerPackUtils", "scbase/loader!sc/plat/dojo/utils/ModelUtils", "scbase/loader!sc/plat/dojo/utils/EventUtils"]
,
function(			 
			    _dojodeclare
			 ,  
				_extnContainerPackContainerListExtnUI
			 ,
				_scScreenUtils
			,
				_scWidgetUtils
			,
				_iasContextUtils
			,
				_iasUIUtils
			,
				_scBaseUtils
			,
				_wscContainerPackUtils
			,
				_scModelUtils
			,
				_scEventUtils
				
){ 
	return _dojodeclare("extn.components.shipment.container.pack.ContainerPackContainerListExtn", [_extnContainerPackContainerListExtnUI],{
		
		handleShowOrHideWidgets: function() {
            _scScreenUtils.clearScreen(
            this);
            _scWidgetUtils.hideWidget(
                this, "containerWeight", false);
            _scWidgetUtils.hideWidget(
                this, "extn_seq", false);
            if (
            _iasContextUtils.isMobileContainer()) {
                _scWidgetUtils.showWidget(
                this, "lbl_containerWeightUOM", false, "");
                var cssClass = null;
                /*cssClass = [];
                cssClass.push("a11yHiddenLabelMandatory");
                _scWidgetUtils.addClass(
                this, "containerWeight", cssClass);*/
            }
            _scScreenUtils.isDirty(
            _iasUIUtils.getParentScreen(
            this, true), null, true);
            if (
            _scBaseUtils.equals(
            _wscContainerPackUtils.getScacIntegrationReqd(
            this), "N")) {
                _scWidgetUtils.hideWidget(
                this, "printLink", false);
                _scWidgetUtils.hideWidget(
                this, "lbl_trackingNo", false);
                var containerModel = null;
                var packageWeight = null;
                containerModel = _scScreenUtils.getModel(
                this, "container_Src");
                packageWeight = _scModelUtils.getStringValueFromPath("Container.ActualWeight", containerModel);
                var numberPackageWeight = 0;
                numberPackageWeight = _scModelUtils.getNumberValueFromPath("Container.ActualWeight", containerModel);
				var parcelType = _scModelUtils.getStringValueFromPath("Container.Extn.ExtnParcelType", containerModel);
                if (_scBaseUtils.isVoid(parcelType)) 
				{
                    _scWidgetUtils.hideWidget(
                    this, "imgPackComplete", false);
                    _scWidgetUtils.hideWidget(
                    this, "lblReady", false);                     
                    _scWidgetUtils.showWidget(this, "saveButton", false, "");
                } else {
                     _scWidgetUtils.showWidget(
                    this, "imgPackComplete", false, "");
                    _scWidgetUtils.showWidget(
                    this, "lblReady", false, ""); 
                    _scWidgetUtils.hideWidget(
                    this, "saveButton", false); 
                }
            } else {
                var containerModel = null;
                var trackingNo = null;
                containerModel = _scScreenUtils.getModel(
                this, "container_Src");
                trackingNo = _scModelUtils.getStringValueFromPath("Container.TrackingNo", containerModel);
                if (!(
                _scBaseUtils.isVoid(
                trackingNo))) {
                    _scWidgetUtils.disableWidget(
                    this, "printLink", false);
                    _scWidgetUtils.showWidget(
                    this, "imgPackComplete", false, "");
                    _scWidgetUtils.showWidget(
                    this, "lblReady", false, "");
                    if (!(
                    _scWidgetUtils.isWidgetVisible(
                    this, "lbl_trackingNo"))) {
                        _scWidgetUtils.showWidget(
                        this, "lbl_trackingNo", false, "");
                    }
                } else {
                    _scWidgetUtils.enableWidget(
                    this, "printLink", false, null);
                    _scWidgetUtils.hideWidget(
                    this, "imgPackComplete", false);
                    _scWidgetUtils.hideWidget(
                    this, "lblReady", false);
                }
            }
        },
		
		saveContainerWeight: function() {
            var isValid = false;
            _wscContainerPackUtils.invokeWeightBlur(
            this, "containerWeight");
            isValid = _scScreenUtils.validate(
            this);
            if (
            _scBaseUtils.equals(
            isValid, true)) {
                var container_Src = null;
                var numOfShipmentLines = 0;
                container_Src = _scScreenUtils.getModel(
                this, "container_Src");
                numOfShipmentLines = _scModelUtils.getNumberValueFromPath("Container.ContainerDetails.TotalNumberOfRecords", container_Src);
                if (
                _scBaseUtils.equals(
                numOfShipmentLines, 0)) {
                    _wscContainerPackUtils.highlightWeightError(
                    this, "Message_EmptyContainer", "errorMsgPnl", "activeRepeatingPanelUId");
                } else {
                    var targetModel = null;
                    var packageWeight = 0;
                    targetModel = _scBaseUtils.getTargetModel(
                    this, "changeShipment_input", null);
                    packageWeight = _scModelUtils.getNumberValueFromPath("Shipment.Containers.Container.ActualWeight", targetModel);
                    if (!(
                    _scModelUtils.hasAttributeInModelPath("Shipment.Containers.Container.ActualWeight", targetModel))) {
                        var msg = null;
                        msg = _scScreenUtils.getString(
                        this, "NoPackageWeight");
                        _wscContainerPackUtils.highlightWeightError(
                        this, "NoPackageWeight", "errorMsgPnl", "activeRepeatingPanelUId");
                    } else if (
                    _scBaseUtils.numberLessThan(
                    packageWeight, 0)) {
                        _wscContainerPackUtils.highlightWeightError(
                        this, "NegativePackageWeight", "errorMsgPnl", "activeRepeatingPanelUId");
                    } else if (!
                    _scBaseUtils.equals(
                    packageWeight, _scModelUtils.getNumberValueFromPath("Container.ActualWeight", container_Src))) {
                        _iasScreenUtils.toggleHighlight(
                        _iasUIUtils.getParentScreen(
                        this, true), this, "activeRepeatingPanelUId", "errorMsgPnl", "information", "PackageWeightNotChanged");
                    } else {
                        if (
                        _scBaseUtils.equals(
                        _wscContainerPackUtils.getScacIntegrationReqd(
                        this), "N")) {
                            _scModelUtils.setStringValueAtModelPath("Shipment.Containers.Container.IsPackProcessComplete", "Y", targetModel);
							_scWidgetUtils.showWidget(
							this, "imgPackComplete", false, "");
							_scWidgetUtils.showWidget(
							this, "lblReady", false, "");
                        } else {
                            var trackingNo = null;
                            trackingNo = _scModelUtils.getStringValueFromPath("Container.TrackingNo", container_Src);
                            if (!(
                            _scBaseUtils.isVoid(
                            trackingNo))) {
                                _scModelUtils.setStringValueAtModelPath("Shipment.CallVoidTrackingNo", "Y", targetModel);
                            }
                        }
                        var eventDefn = null;
                        var blankModel = null;
                        eventDefn = {};
                        blankModel = {};
                        _scBaseUtils.setAttributeValue("argumentList", blankModel, eventDefn);
                        _scBaseUtils.setAttributeValue("argumentList.changeShipment_input", targetModel, eventDefn);
                        _scBaseUtils.setAttributeValue("argumentList.getShipmentContainerDetails_input", _scBaseUtils.getTargetModel(
                        this, "getShipmentContainerDetails_input", null), eventDefn);
                        _scEventUtils.fireEventToParent(
                        this, "recordContainerWeight", eventDefn);
                    }
                }
            } else {
                _wscContainerPackUtils.highlightWeightError(
                this, "NoPackageWeight", "errorMsgPnl", "activeRepeatingPanelUId");
            }
        },
		
		extnToggleUpdateButton:function()
		{
			var targetModel = null;
            targetModel = _scBaseUtils.getTargetModel(this, "changeShipment_input", null);
            var parcelTypeNew = targetModel.Shipment.Containers.Container.Extn.ExtnParcelType;
            var containerModel = null;
            containerModel = _scScreenUtils.getModel(this, "container_Src");
            var parcelTypeOld = containerModel.Container.Extn.ExtnParcelType;
            if(!_scBaseUtils.equals(parcelTypeNew, parcelTypeOld))
            {
            	_scWidgetUtils.hideWidget(this, "imgPackComplete", false);
                _scWidgetUtils.hideWidget(this, "lblReady", false);
				_scWidgetUtils.showWidget(this, "saveButton", false, "");
            }
		}
	
});
});

