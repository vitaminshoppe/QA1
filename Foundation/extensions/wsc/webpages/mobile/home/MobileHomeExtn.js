
scDefine(["scbase/loader!dojo/_base/declare","scbase/loader!extn/mobile/home/MobileHomeExtnUI",
"scbase/loader!sc/plat/dojo/utils/ModelUtils","scbase/loader!sc/plat/dojo/utils/ResourcePermissionUtils",
"scbase/loader!sc/plat/dojo/utils/WidgetUtils","scbase/loader!sc/plat/dojo/utils/ControllerUtils",
"scbase/loader!ias/utils/UIUtils","scbase/loader!sc/plat/dojo/utils/BaseUtils","scbase/loader!sc/plat/dojo/utils/ScreenUtils"]
,
function(			 
			    _dojodeclare
			 ,
			    _extnMobileHomeExtnUI
			,
				_scModelUtils
			,
				_scResourcePermissionUtils
			,
				_scWidgetUtils
			,
				_scControllerUtils
			,
				_iasUIUtils
			,
				_scBaseUtils
			,
				_scScreenUtils
){ 
	return _dojodeclare("extn.mobile.home.MobileHomeExtn", [_extnMobileHomeExtnUI],{
	// custom code here
	initializeScreen: function(
        event, bEvent, ctrl, args) {
            this.checkQueryString();
            var mashupInputModelList = null;
            mashupInputModelList = [];
            var mashupRefIdList = null;
            mashupRefIdList = [];
            
			/*OMS - 2983 */
            var inputShipPackagesCount = null;
            inputShipPackagesCount = _scModelUtils.createNewModelObjectWithRootKey("Container");
            inputShipPackagesCount.Container.Shipment = "";
            inputShipPackagesCount.Container.Shipment = {};
            inputShipPackagesCount.Container.Shipment.SCAC = "FEDEX";

            mashupRefIdList.push("extn_shipPackages_getShipmentListCount");
            mashupInputModelList.push(
                inputShipPackagesCount);
            /*OMS - 2983 END */
            var emptyInput = null;
            emptyInput = _scModelUtils.createNewModelObjectWithRootKey("Shipment");
            if (
            _scResourcePermissionUtils.hasPermission("WSC000020")) {
                _scWidgetUtils.showWidget(
                this, "pnlConfirmOrders", false, null);
            }
            if (
            _scResourcePermissionUtils.hasPermission("WSC000011")) {
                mashupRefIdList.push("tasks_getShipmentListCount");
				var emptyInputNew = null;
				emptyInputNew = _scModelUtils.createNewModelObjectWithRootKey("Shipment");
				emptyInputNew.Shipment.AssignedToUserId = " ";
                mashupInputModelList.push(
                emptyInputNew);
            }
            if (
            _scResourcePermissionUtils.hasPermission("WSC000012")) {
                mashupRefIdList.push("openPicks_getShipmentListCount");
                mashupInputModelList.push(
                emptyInput);
            }
            if (
            _scResourcePermissionUtils.hasPermission("WSC000015")) {
                mashupRefIdList.push("pickShip_getShipmentListCount");
                mashupInputModelList.push(
                emptyInput);
            }
            if (
            _scResourcePermissionUtils.hasPermission("WSC000019")) {
                mashupRefIdList.push("pack_getShipmentListCount");
                mashupInputModelList.push(
                emptyInput);
            }
			if (_scResourcePermissionUtils.hasPermission("WSC000028")) {
				 _scWidgetUtils.showWidget( this, "pnlBatchPick", false, null);
            }
			if (_scResourcePermissionUtils.hasPermission("WSC000031") || _scResourcePermissionUtils.hasPermission("WSC000033")) {
				 _scWidgetUtils.showWidget( this, "pnlOrderCapture", false, null);
           }
            var mashupContext = null;
            mashupContext = _scControllerUtils.getMashupContext(
            this);
            _iasUIUtils.callApis(
            this, mashupInputModelList, mashupRefIdList, mashupContext, null);
        },
		
		handleMashupOutput: function(
        mashupRefId, modelOutput, mashupInput, mashupContext, applySetModel) {
            if (
            _scBaseUtils.equals(
            mashupRefId, "getShipmentList")) {
                this.handle_getShipmentList(
                mashupRefId, modelOutput, mashupInput, mashupContext, applySetModel);
            }
            if (
            _scBaseUtils.equals(
            mashupRefId, "tasks_getShipmentListCount")) {
                if (!(
                _scBaseUtils.equals(
                false, applySetModel))) {
                    _scScreenUtils.setModel(
                    this, "tasks_getShipmentListCount_output", modelOutput, null);
                }
                this.handle_tasks_getShipmentListCount(
                mashupRefId, modelOutput, mashupInput, mashupContext, applySetModel);
            }
            if (
            _scBaseUtils.equals(
            mashupRefId, "openPicks_getShipmentListCount")) {
                if (!(
                _scBaseUtils.equals(
                false, applySetModel))) {
                    _scScreenUtils.setModel(
                    this, "openPicks_getShipmentListCount_output", modelOutput, null);
                }
                this.handle_openPicks_getShipmentListCount(
                mashupRefId, modelOutput, mashupInput, mashupContext, applySetModel);
            }
            if (
            _scBaseUtils.equals(
            mashupRefId, "pickShip_getShipmentListCount")) {
                if (!(
                _scBaseUtils.equals(
                false, applySetModel))) {
                    _scScreenUtils.setModel(
                    this, "pickShip_getShipmentListCount_output", modelOutput, null);
                }
                this.handle_pickShip_getShipmentListCount(
                mashupRefId, modelOutput, mashupInput, mashupContext, applySetModel);
            }
            if (
            _scBaseUtils.equals(
            mashupRefId, "pack_getShipmentListCount")) {
                if (!(
                _scBaseUtils.equals(
                false, applySetModel))) {
                    _scScreenUtils.setModel(
                    this, "pack_getShipmentListCount_output", modelOutput, null);
                }
                this.handle_pack_getShipmentListCount(
                mashupRefId, modelOutput, mashupInput, mashupContext, applySetModel);
            }
			if (
            _scBaseUtils.equals(
            mashupRefId, "extn_shipPackages_getShipmentListCount")) {
                if (!(
                _scBaseUtils.equals(
                false, applySetModel))) {
                    _scScreenUtils.setModel(
                    this, "extn_shipPackages_getShipmentListCount_output", modelOutput, null);
                }
            }
        },
		
		extn_shipPackagesBindingFunc: function(
        dataValue, screen, widget, nameSpace, shipmentModel) {
            if (
            _scBaseUtils.equals("0", dataValue)) {
                _scWidgetUtils.addClass(
                this, "pnlConfirmOrders", "zeroCount");
            }
            return dataValue;
        }
});
});

