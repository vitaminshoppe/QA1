
scDefine(["scbase/loader!dojo/_base/declare","scbase/loader!extn/components/shipment/container/pack/ContainerPackExtnUI","scbase/loader!sc/plat/dojo/utils/BaseUtils", "scbase/loader!sc/plat/dojo/utils/ModelUtils", "scbase/loader!sc/plat/dojo/utils/ScreenUtils", "scbase/loader!ias/utils/UIUtils", "scbase/loader!ias/utils/WizardUtils", "scbase/loader!sc/plat/dojo/utils/EventUtils"]
,
function(			 
			    _dojodeclare
			 ,
			    _extnContainerPackExtnUI
			,
				_scBaseUtils
			,
				_scModelUtils
			,
				_scScreenUtils
			,
				_iasUIUtils
			,
				_iasWizardUtils
			,
				_scEventUtils
){ 
	return _dojodeclare("extn.components.shipment.container.pack.ContainerPackExtn", [_extnContainerPackExtnUI],{
	// custom code here
	handleMashupOutput: function(
        mashupRefId, modelOutput, modelInput, mashupContext) {
            if (
            _scBaseUtils.equals(
            mashupRefId, "containerPack_getShipmentContainerList_NoScac") || _scBaseUtils.equals(
            mashupRefId, "containerPack_getShipmentContainerList_Scac")) {
                var shipmentContainerizedFlag = 0;
                shipmentContainerizedFlag = _scModelUtils.getNumberValueFromPath("Containers.ShipmentContainerizedFlag", modelOutput);
                if (
                _scBaseUtils.equals(
                shipmentContainerizedFlag, 3)) {
                    var totalNumberOfRecords = 0;
					var countForVoidParcelType = 0;
                    totalNumberOfRecords = _scModelUtils.getNumberValueFromPath("Containers.TotalNumberOfRecords", modelOutput);
                    if (
                    totalNumberOfRecords > 0) {
                        if (
                        _scBaseUtils.equals(
                        this.scacIntegrationReqd, "N")) {
                           			
							var container = modelOutput.Containers.Container;
							for(var eachContainer in container)
							{
								var parcelType = container[eachContainer].Extn.ExtnParcelType;
								if(_scBaseUtils.isVoid(parcelType))
								{
									countForVoidParcelType++;
								}								
							}
							if(countForVoidParcelType == 0)
							{
								var targetModel = null;
								targetModel = _scBaseUtils.getTargetModel(
								this, "changeShipmentStatus_input", null);
								_iasUIUtils.callApi(
								this, targetModel, "containerPacking_changeShipment", null);
							}
							else
							{
								var argsBean = null;
								argsBean = {};
								var goToPackagesView = true;
								_scBaseUtils.setAttributeValue("goToPackagesView", goToPackagesView, argsBean);
								var message = "All the containers are not updated. Do you still want to proceed ?";
								_scScreenUtils.showConfirmMessageBox(
								this, message, "openShipmentSummaryOnConfirm", null, argsBean);
								
							}
							
                        } else if (
                        _scBaseUtils.equals(
                        this.scacIntegrationReqd, "Y")) {
                            var argsBean = null;
                            argsBean = {};
                            var goToPackagesView = true;
                            _scBaseUtils.setAttributeValue("goToPackagesView", goToPackagesView, argsBean);
                            _scScreenUtils.showConfirmMessageBox(
                            this, _scScreenUtils.getString(
                            this, "Message_NotAllContainersTracked"), "openShipmentSummaryOnConfirm", null, argsBean);
                        }
                    } else {
                        var targetModel = null;
                        targetModel = _scBaseUtils.getTargetModel(
                        this, "changeShipmentStatus_input", null);
                        _iasUIUtils.callApi(
                        this, targetModel, "containerPacking_changeShipment", null);
                    }
                } else {
                    _scScreenUtils.showConfirmMessageBox(
                    this, _scScreenUtils.getString(
                    this, "Message_NotAllLinesPacked"), "openShipmentSummaryOnConfirm", null);
                }
            } else if (
            _scBaseUtils.equals(
            mashupRefId, "containerPacking_changeShipment")) {
                this.openShipmentSummary("Yes");
            }
        },
		
		initializeScreen: function(
        event, bEvent, ctrl, args) {
            var parentScreen = null;
            var getShipmentDetails_output = null;
            var shipmentContainerizedFlag = 0;
            var activeContainerInfo = null;
            var scacIntegrationReqd = null;
            parentScreen = _iasUIUtils.getParentScreen(
            this, true);
            _iasWizardUtils.setLabelOnNavigationalWidget(
            parentScreen, "confirmBttn", _scScreenUtils.getString(
            this, "Action_done"));
            getShipmentDetails_output = _scScreenUtils.getModel(this, "getShipmentDetails_output");
            var contListModel = _scModelUtils.createNewModelObjectWithRootKey("Containers");
            _scModelUtils.addModelToModelPath("Containers",_scModelUtils.getModelObjectFromPath("Shipment.Containers",getShipmentDetails_output),contListModel);
            _scScreenUtils.setModel(this, "getShipmentContainerList_output", contListModel, null);
            
            this.setEditorTitle(
            getShipmentDetails_output);
            this.initializeMobileOrDesktopWidgets(
            getShipmentDetails_output);
            scacIntegrationReqd = _scModelUtils.getStringValueFromPath("Shipment.ScacIntegrationRequired", getShipmentDetails_output);
            this.scacIntegrationReqd = scacIntegrationReqd;
            this.initializeactiveContainerModel(getShipmentDetails_output);
            _scScreenUtils.showChildScreen(this, "ContainerPackItemScan", null, "", null, null);
            
            if(_scBaseUtils.equals(3,_scModelUtils.getNumberValueFromPath("Shipment.ShipmentContainerizedFlag", getShipmentDetails_output))){
            	this.showContainersViewOnLoad();
            }else{
                 var initialInput = _scScreenUtils.getInitialInputData(this);
                 if (! _scBaseUtils.isVoid(_scModelUtils.getStringValueFromPath("Shipment.ItemID", initialInput))) {
                 	var eventDefn = {};
                     _scBaseUtils.setAttributeValue("argumentList",{}, eventDefn);
                 	_scEventUtils.fireEventToChild(this, "ContainerPackItemScan", "showProducts", eventDefn);
                 }
            }
            var eventDefn = {};
			_scBaseUtils.setAttributeValue("argumentList",{}, eventDefn);
			_scEventUtils.fireEventToChild(this, "ContainerPackItemScan", "showProducts", eventDefn);
            this.initializeSLAwidget(getShipmentDetails_output);
        }
});
});

