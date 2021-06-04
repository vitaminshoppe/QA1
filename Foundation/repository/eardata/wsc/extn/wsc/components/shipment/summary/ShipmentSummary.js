scDefine(["scbase/loader!dojo/_base/declare", "scbase/loader!ias/utils/BaseTemplateUtils", "scbase/loader!ias/utils/ContextUtils", "scbase/loader!ias/utils/PrintUtils", "scbase/loader!ias/utils/RepeatingScreenUtils", "scbase/loader!ias/utils/ScreenUtils", "scbase/loader!ias/utils/UIUtils", "scbase/loader!sc/plat/dojo/utils/BaseUtils", "scbase/loader!sc/plat/dojo/utils/EditorUtils", "scbase/loader!sc/plat/dojo/utils/EventUtils", "scbase/loader!sc/plat/dojo/utils/ModelUtils", "scbase/loader!sc/plat/dojo/utils/RepeatingPanelUtils", "scbase/loader!sc/plat/dojo/utils/ScreenUtils", "scbase/loader!sc/plat/dojo/utils/WidgetUtils", "scbase/loader!wsc/components/shipment/common/utils/ShipmentUtils", "scbase/loader!wsc/components/shipment/container/pack/ContainerPackUtils", "scbase/loader!wsc/components/shipment/container/unpack/UnpackShipmentUtils", "scbase/loader!wsc/components/shipment/summary/ShipmentSummaryUI", "scbase/loader!wsc/components/shipment/summary/ShipmentSummaryUtils"], function(
_dojodeclare, _iasBaseTemplateUtils, _iasContextUtils, _iasPrintUtils, _iasRepeatingScreenUtils, _iasScreenUtils, _iasUIUtils, _scBaseUtils, _scEditorUtils, _scEventUtils, _scModelUtils, _scRepeatingPanelUtils, _scScreenUtils, _scWidgetUtils, _wscShipmentUtils, _wscContainerPackUtils, _wscUnpackShipmentUtils, _wscShipmentSummaryUI, _wscShipmentSummaryUtils) {
    return _dojodeclare("wsc.components.shipment.summary.ShipmentSummary", [_wscShipmentSummaryUI], {
        // custom code here
        handle_containerPack_StorePackSlip_94: function(
        mashupRefId, modelOutput, mashupInput, mashupContext, applySetModel) {
            _iasPrintUtils.printHtmlOutput(
            modelOutput);
        },
        handle_containerPack_StoreLabelReprint_94: function(
        mashupRefId, modelOutput, mashupInput, mashupContext, applySetModel) {
            if (
            _scModelUtils.hasAttributeInModelPath("Output.out", modelOutput)) {
                _wscContainerPackUtils.decodeShippingLabelURL(
                modelOutput);
            } else {
                var msg = null;
                msg = _scScreenUtils.getString(
                this, "printFailure");
                _iasBaseTemplateUtils.showMessage(
                this, msg, "error", null);
            }
        },
        handle_getShipmentDetailsForRecordCustomerPick: function(
        mashupRefId, modelOutput, mashupInput, mashupContext, applySetModel) {
            _wscShipmentUtils.handleValidationOutput(
            this, mashupRefId, modelOutput, mashupInput, mashupContext, applySetModel);
        },
        afterScreenLoad: function(
        event, bEvent, ctrl, args) {
            var initialInput = null;
            initialInput = _scScreenUtils.getInitialInputData(
            this);
            var isPackSlipPrintRequired = null;
            var errorDescription = null;
            isPackSlipPrintRequired = _scModelUtils.getStringValueFromPath("Shipment.isPackSlipPrintRequired", initialInput);
            errorDescription = _scModelUtils.getStringValueFromPath("Shipment.ErrorDescription", initialInput);
            if (!(
            _scBaseUtils.isVoid(
            isPackSlipPrintRequired))) {
                if (
                _scBaseUtils.equals(
                isPackSlipPrintRequired, "Yes")) {
                    _scModelUtils.setStringValueAtModelPath("Shipment.isPackSlipPrintRequired", "", initialInput);
                    _scScreenUtils.setInitialInputData(
                    _scEditorUtils.getCurrentEditor(), initialInput);
                    _scScreenUtils.setInitialInputData(
                    _iasUIUtils.getParentScreen(
                    this, true), initialInput);
                }
            }
            if (!(
            _scBaseUtils.isVoid(
            errorDescription))) {
                if (
                _scBaseUtils.equals(
                errorDescription, "NoPermission")) {
                    _iasBaseTemplateUtils.showMessage(
                    this, _scScreenUtils.getString(
                    this, "Label_NoPermission"), "error", null);
                }
            }
        },
        printPackSlipOnPackCompletion: function(
        res) {
            if (
            _scBaseUtils.equals(
            res, "Ok")) {
                var StorePackSlip_94_input = null;
                StorePackSlip_94_input = _scBaseUtils.getTargetModel(
                this, "StorePackSlip_94_input", null);
                _iasUIUtils.callApi(
                this, StorePackSlip_94_input, "containerPack_StorePackSlip_94", null);
            }
        },
        reprintCarrierLabel: function(
        event, bEvent, ctrl, args) {
            var getTrackingNoAndPrintLabel_input = null;
            getTrackingNoAndPrintLabel_input = _scBaseUtils.getValueFromPath("getTrackingNoAndPrintLabel_input", args);
            _iasUIUtils.callApi(
            this, getTrackingNoAndPrintLabel_input, "containerPack_StoreLabelReprint_94", null);
        },
        initializeScreen: function(
        event, bEvent, ctrl, args) {
            var shipmentData = null;
            shipmentData = _scScreenUtils.getModel(
            this, "getShipmentDetails_output");
            _wscShipmentUtils.showNextTask(
            this, shipmentData, "Shipment.ShipNode.ShipNode", "lnkBRP", "lnkBRP", "", "lnkPack", "lnkPack", "lnkStartCustomerPickup");
            if (
            _iasContextUtils.isMobileContainer()) {
                this.fireSetWizardDescEvent(
                shipmentData);
                _scWidgetUtils.hideWidget(
                this, "img_TimeRmnClockWeb", false);
            } else {
                this.updateScreenTitle(
                this, shipmentData);
                this.initializeSLAwidget(
                shipmentData);
            }
            status = _scModelUtils.getStringValueFromPath("Shipment.Status.Status", shipmentData);
            if (!(
            _wscShipmentUtils.showSLA(
            status))) {
                _scWidgetUtils.hideWidget(
                this, "img_TimeRmnClockWeb", true);
            }
            _wscShipmentUtils.showHideHoldLocation(
            this, shipmentData, "lblHoldLocation");
            this.handleStoreAddressPanel(
            shipmentData);
            var deliveryMethod = null;
            deliveryMethod = _scModelUtils.getStringValueFromPath("Shipment.DeliveryMethod", shipmentData);
            if (
            _scBaseUtils.equals(
            deliveryMethod, "PICK")) {
                this.initializeScreenPickupInStore(
                shipmentData);
            } else if (
            _scBaseUtils.equals(
            deliveryMethod, "SHP")) {
                this.initializeScreenShipFromStore(
                shipmentData);
            }
        },
        initializeSLAwidget: function(
        getShipmentDetails_output) {
            var urlString = null;
            urlString = _scModelUtils.getStringValueFromPath("Shipment.ImageUrl", getShipmentDetails_output);
            if (!(
            _scBaseUtils.isVoid(
            urlString))) {
                var imageUrlModel = null;
                imageUrlModel = _scModelUtils.createNewModelObjectWithRootKey("CommonCode");
                _scModelUtils.setStringValueAtModelPath("CommonCode.CodeLongDescription", urlString, imageUrlModel);
                _scModelUtils.setStringValueAtModelPath("CommonCode.CodeShortDescription", _scModelUtils.getStringValueFromPath("Shipment.ImageAltText", getShipmentDetails_output), imageUrlModel);
                _scScreenUtils.setModel(
                this, "clockImageBindingValues", imageUrlModel, null);
                _scWidgetUtils.changeImageTitle(
                this, "img_TimeRmnClockWeb", _scModelUtils.getStringValueFromPath("Shipment.ImageAltText", getShipmentDetails_output));
            }
        },
        initializeScreenPickupInStore: function(
        shipmentModel) {
            _scWidgetUtils.showWidget(
            this, "pnlPickupInStoreInfo", false, null);
        },
        initializeScreenShipFromStore: function(
        shipmentModel) {
            var numOfContainers = null;
            numOfContainers = _scModelUtils.getNumberValueFromPath("Shipment.Containers.TotalNumberOfRecords", shipmentModel);
            if (!(
            _scBaseUtils.isVoid(
            numOfContainers))) {
                if (!(
                _scBaseUtils.equals(
                numOfContainers, 0))) {
                    _scWidgetUtils.showWidget(
                    this, "lblContainerCount", true, null);
                    _scWidgetUtils.showWidget(
                    this, "tpContainerDetails", true, null);
                }
            }
            var actualShipDate = null;
            actualShipDate = _scModelUtils.getStringValueFromPath("Shipment.ActualShipmentDate", shipmentModel);
            if (!(
            _scBaseUtils.isVoid(
            actualShipDate))) {
                _scWidgetUtils.showWidget(
                this, "lblShipDate", false, null);
            }
            var personInfoShipToModel = null;
            personInfoShipToModel = _scModelUtils.getModelObjectFromPath("Shipment.ToAddress", shipmentModel);
            if (!(
            _scBaseUtils.isVoid(
            personInfoShipToModel))) {
                _scWidgetUtils.showWidget(
                this, "pnlShipToAddress", false, null);
            }
            _scWidgetUtils.showWidget(
            this, "pnlShipFromStoreInfo", false, null);
        },
        handleAdditionalInformation: function(
        shipmentMoreDetails) {
            if (
            _scBaseUtils.equals(
            _scModelUtils.getStringValueFromPath("Shipment.NoPersonInfoMarkFor", shipmentMoreDetails), "Y")) {
                _scWidgetUtils.hideWidget(
                this, "pnlPersonMarkFor", true);
            }
            _scScreenUtils.setModel(
            this, "getShipmentDetailsMore_output", shipmentMoreDetails, null);
            this.isAdditionalInfoPainted = "true";
        },
        titlePaneAddressEvent: function(
        event, bEvent, ctrl, args) {
            if (
            _scBaseUtils.equals(
            this.isAdditionalInfoPainted, "false")) {
                var shipModel = null;
                shipModel = _scScreenUtils.getTargetModel(
                this, "relatedTaskInput", null);
                _iasUIUtils.callApi(
                this, shipModel, "getShipmentMoreDetails", null);
            }
            _scWidgetUtils.showWidget(
            this, "pnlBillToAddress", false, "");
        },
        titlePaneProductsEvent: function(
        event, bEvent, ctrl, args) {
            if (
            _scBaseUtils.equals(
            this.isProductListPainted, "false")) {
                var getShipmentLineListTargetModel = null;
                getShipmentLineListTargetModel = _scScreenUtils.getTargetModel(
                this, "getShipmentLineList_input", null);
                _scRepeatingPanelUtils.startPaginationUsingUId(
                this, "cpShipmentLinesPaginationPanel", "getShipmentLineList_output", getShipmentLineListTargetModel, "getShipmentLineList");
                this.isProductListPainted = "true";
            }
        },
        reloadProductList: function() {
            if (
            _scBaseUtils.equals(
            this.isProductListPainted, "true")) {
                var getShipmentLineListTargetModel = null;
                getShipmentLineListTargetModel = _scScreenUtils.getTargetModel(
                this, "getShipmentLineList_input", null);
                _scRepeatingPanelUtils.startPaginationUsingUId(
                this, "cpShipmentLinesPaginationPanel", "getShipmentLineList_output", getShipmentLineListTargetModel, "getShipmentLineList");
                this.isProductListPainted = "true";
            }
        },
        titlePaneContainerEvent: function(
        event, bEvent, ctrl, args) {
            if (
            _scBaseUtils.equals(
            this.isContainerListPainted, "false")) {
                var getShipmentContainerListTargetModel = null;
                getShipmentContainerListTargetModel = _scScreenUtils.getTargetModel(
                this, "getShipmentContainerList_input", null);
                _scModelUtils.setStringValueAtModelPath("Shipment.isTrackingURLRequired", "Yes", getShipmentContainerListTargetModel);
                _scRepeatingPanelUtils.startPaginationUsingUId(
                this, "cpContainerLinesPaginationPanel", "getShipmentContainerList_output", getShipmentContainerListTargetModel, "getShipmentContainerList");
                this.isContainerListPainted = "true";
                this.getUnpackReasonCount();
            }
        },
        getUnpackReasonCount: function() {
            var commonCodeInput = null;
            commonCodeInput = _scScreenUtils.getTargetModel(
            this, "getNumCommonCodes", null);
            _iasUIUtils.callApi(
            this, commonCodeInput, "unpack_getNumReasonCodes_refid", null);
        },
        fireSetWizardDescEvent: function(
        inputShipmentModel) {
            _scWidgetUtils.hideWidget(
            this, "lblShipmentDesc", true);
            var eventBean = null;
            eventBean = {};
            var blankModel = null;
            blankModel = {};
            _scBaseUtils.setAttributeValue("argumentList", blankModel, eventBean);
            _scBaseUtils.setAttributeValue("argumentList.Shipment", inputShipmentModel, eventBean);
            _scEventUtils.fireEventToParent(
            this, "setWizardDescription", eventBean);
        },
        handlePaymentMethod: function(
        dataValue, screen, widget, namespace, modelObj, options) {
            if (!(
            _scBaseUtils.isVoid(
            dataValue))) {
                var formattedPaymentMtd = null;
                var inputArray = null;
                inputArray = [];
                inputArray.push(
                _scModelUtils.getStringValueFromPath("Shipment.CreditCardTypeDesc", modelObj));
                inputArray.push(
                _scModelUtils.getStringValueFromPath("Shipment.PaymentMethod", modelObj));
                formattedPaymentMtd = _scScreenUtils.getFormattedString(
                this, "PaymentMethod", inputArray);
                _scWidgetUtils.showWidget(
                this, "lblPaymentMethod", false, "");
                return formattedPaymentMtd;
            }
            return dataValue;
        },
        handleStoreAddressPanel: function(
        shipmentDetailsModel) {
            var currentStore = null;
            currentStore = _iasContextUtils.getFromContext("CurrentStore");
            if (!(
            _scBaseUtils.equals(
            currentStore, _scModelUtils.getStringValueFromPath("Shipment.ShipNode.ShipNode", shipmentDetailsModel)))) {
                _scWidgetUtils.showWidget(
                this, "storeAddressPnl", true, null);
            }
        },
        updateScreenTitle: function(
        screen, mashupRefOutput) {
            var returnVal = null;
            var args = null;
            returnVal = _wscShipmentSummaryUtils.getTitleForShipmentEditor(
            mashupRefOutput);
            args = {};
            _scBaseUtils.setAttributeValue("title", returnVal, args);
            _scEventUtils.fireEventGlobally(
            this, "changeEditorTitle", null, args);
        },
        getFormattedMarkForNameDisplay: function(
        dataValue, screen, widget, namespace, modelObj, options) {
            var nameModel = null;
            var formattedName = null;
            nameModel = {};
            _scModelUtils.setStringValueAtModelPath("FirstName", _scModelUtils.getStringValueFromPath("Shipment.FirstNameForDisplay", modelObj), nameModel);
            _scModelUtils.setStringValueAtModelPath("LastName", _scModelUtils.getStringValueFromPath("Shipment.LastNameForDisplay", modelObj), nameModel);
            formattedName = _wscShipmentUtils.getNameDisplay(
            this, nameModel);
            if (!(
            _scBaseUtils.isVoid(formattedName))) {
                _scWidgetUtils.showWidget(
                this, "lblMarkFor", true, null);
                _scWidgetUtils.showWidget(
                this, "lblMarkForName", true, null);
            }
            return formattedName;
        },
        getFormattedNameDisplay: function(
        dataValue, screen, widget, namespace, modelObj, options) {
            var nameModel = null;
            var formattedName = null;
            nameModel = {};
            _scModelUtils.setStringValueAtModelPath("FirstName", _scModelUtils.getStringValueFromPath("Shipment.BillToAddress.FirstName", modelObj), nameModel);
            _scModelUtils.setStringValueAtModelPath("LastName", _scModelUtils.getStringValueFromPath("Shipment.BillToAddress.LastName", modelObj), nameModel);
            formattedName = _wscShipmentUtils.getNameDisplay(
            this, nameModel);
            return formattedName;
        },
        getMarkForDayPhone: function(
        dataValue, screen, widget, namespace, modelObj, options) {
            if (!(
            _scBaseUtils.isVoid(
            dataValue))) {
                _scWidgetUtils.showWidget(
                this, "lblMarkFor", true, null);
                _scWidgetUtils.showWidget(
                this, "pnlMarkForPhoneHolder", false, "");
            }
            return dataValue;
        },
        getDayPhone: function(
        dataValue, screen, widget, namespace, modelObj, options) {
            if (
            _scBaseUtils.isVoid(
            dataValue)) {
                _scWidgetUtils.hideWidget(
                this, "pnlPhoneHolder", true);
            }
            return dataValue;
        },
        getMarkForEmailID: function(
        dataValue, screen, widget, namespace, modelObj, options) {
            if (!(
            _scBaseUtils.isVoid(
            dataValue))) {
                _scWidgetUtils.showWidget(
                this, "lblMarkFor", true, null);
                _scWidgetUtils.showWidget(
                this, "pnlMarkForEmailHolder", false, null);
            }
            return dataValue;
        },
        getEmailID: function(
        dataValue, screen, widget, namespace, modelObj, options) {
            if (
            _scBaseUtils.isVoid(
            dataValue)) {
                _scWidgetUtils.hideWidget(
                this, "pnlEmailHolder", true);
            }
            return dataValue;
        },
        getUniqueIDForShipmentLineListRepPanel: function(
        screen, widget, namespace, dataObject, modelObject, repeatingScreenObject, repeatingScreenIndex) {
            var shipmentLineKey = null;
            shipmentLineKey = _scModelUtils.getStringValueFromPath("ShipmentLineKey", dataObject);
            return shipmentLineKey;
        },
        handleMashupCompletion: function(
        mashupContext, mashupRefObj, mashupRefList, inputData, hasError, data) {
            _iasBaseTemplateUtils.handleMashupCompletion(
            mashupContext, mashupRefObj, mashupRefList, inputData, hasError, data, this);
        },
        startBackroomPick: function(
        event, bEvent, ctrl, args) {
            var shipmentModel = null;
            shipmentModel = _scScreenUtils.getTargetModel(
            this, "relatedTaskInput", null);
            _wscShipmentUtils.openBackroomPickWizard(
            this, shipmentModel);
        },
        startCustomerPick: function(
        event, bEvent, ctrl, args) {
            var shipmentInputModel = null;
            shipmentInputModel = _scScreenUtils.getModel(
            this, "getShipmentDetails_output");
            _wscShipmentUtils.handleCustomerPick_OnClick(
            this, shipmentInputModel, "getShipmentDetailsForRecordCustomerPick");
        },
        startPack: function(
        event, bEvent, ctrl, args) {
            var shipmentModel = null;
            shipmentModel = _scScreenUtils.getTargetModel(
            this, "relatedTaskInput", null);
            _wscShipmentUtils.openPackingWizard(
            this, shipmentModel);
        },
        getDisplayOrderNumber: function(
        dataValue, screen, widget, namespace, modelObj, options) {
            var formattedOrderNo = null;
            formattedOrderNo = _wscShipmentUtils.DisplayOrderNoForShipSummary(
            dataValue);
            if (
            _scBaseUtils.isVoid(
            formattedOrderNo)) {
                _scWidgetUtils.hideWidget(
                this, "lblOrderNo", true);
            }
            return formattedOrderNo;
        },
        abandonedCallbackHandler: function(
        actionPerformed, outputModel, popupParams) {
            _wscShipmentUtils.handleAssignedUserPopupResponse(
            this, actionPerformed, outputModel, popupParams, "containerPack_unpackShipment", "containerPack_changeShipment", "containerPack_changeShipmentStatus", "changeShipmentToUpdateQty");
        },
        deleteContainer: function(
        event, bEvent, widget_uId, args) {
            this.showSelectedContainerRepPanel(
            event, bEvent, widget_uId, args);
            var containerId = null;
            containerId = _scBaseUtils.getStringValueFromBean("containerNoToDelete", args);
            var containerKey = null;
            containerKey = _scBaseUtils.getStringValueFromBean("containerKeyToDelete", args);
            var arguments = null;
            arguments = {};
            _scBaseUtils.setAttributeValue("containerKeyToDelete", containerKey, arguments);
            _scBaseUtils.setAttributeValue("containerNoToDelete", containerId, arguments);
            if (
            this.isUnpackRCConfigured) {
                this.openUnpackRCPopup(
                event, bEvent, widget_uId, arguments);
            } else {
                _wscUnpackShipmentUtils.askDeleteConfirmation(
                this, event, bEvent, widget_uId, arguments);
            }
        },
        openUnpackRCPopup: function(
        event, bEvent, ctrl, popupParams) {
            var screenInputModel = null;
            screenInputModel = _scScreenUtils.getModel(
            this, "getShipmentDetails_output");
            var bindings = null;
            bindings = {};
            popupParams["screenInput"] = screenInputModel;
            popupParams["binding"] = bindings;
            var dialogParams = null;
            dialogParams = {};
            dialogParams["closeCallBackHandler"] = "onUnpackReasonPopupClose";
            dialogParams["class"] = "popupTitleBorder";
            _iasUIUtils.openSimplePopup("wsc.components.shipment.container.unpack.UnpackReasonPopup", "Title_UnpackReason", this, popupParams, dialogParams);
        },
        onUnpackReasonPopupClose: function(
        actionPerformed, model, popupParams) {
            if (!(
            _scBaseUtils.equals(
            actionPerformed, "CLOSE"))) {
                var unpackReason = null;
                unpackReason = _scModelUtils.getStringValueFromPath("ReasonCode.CodeShortDescription", model);
                var containerId = null;
                containerId = _scBaseUtils.getStringValueFromBean("containerNoToDelete", popupParams);
                var bundleArray = null;
                bundleArray = [];
                bundleArray.push(
                containerId);
                bundleArray.push(
                unpackReason);
                var noteText = null;
                noteText = _scScreenUtils.getFormattedString(
                this, "DeleteContainerNoteText", bundleArray);
                this.callUnpackShipmentAPI(
                noteText, popupParams);
            }
        },
        handleDeleteMsgConfirmation: function(
        confirmBoxResult, args) {
            if (
            _scBaseUtils.equals(
            confirmBoxResult, "Ok")) {
                this.callUnpackShipmentAPI("", args);
            }
        },
        callUnpackShipmentAPI: function(
        noteText, args) {
            var containerKey = null;
            containerKey = _scBaseUtils.getStringValueFromBean("containerKeyToDelete", args);
            var containerNo = null;
            containerNo = _scBaseUtils.getStringValueFromBean("containerNoToDelete", args);
            var unpackShipmentInput = null;
            unpackShipmentInput = _scBaseUtils.getTargetModel(
            this, "unpack_deleteContainer_inTrgt", null);
            _scModelUtils.setStringValueAtModelPath("Shipment.Containers.Container.ShipmentContainerKey", containerKey, unpackShipmentInput);
            _scModelUtils.setStringValueAtModelPath("Shipment.Containers.Container.ContainerNo", containerNo, unpackShipmentInput);
            if (!(
            _scBaseUtils.isVoid(
            noteText))) {
                _scModelUtils.setStringValueAtModelPath("Shipment.Note.NoteText", noteText, unpackShipmentInput);
                _scModelUtils.setStringValueAtModelPath("Shipment.Note.ReasonCode", "YCD_UNPACK_REASON", unpackShipmentInput);
            }
            _scModelUtils.setStringValueAtModelPath("Shipment.ChangeShipmentStatusMashupId", "unpack_changeShipmentStatusForSummary", unpackShipmentInput);
            _iasUIUtils.callApi(
            this, unpackShipmentInput, "unpack_deleteContainer_refid", null);
        },
        handle_unpack_getNumReasonCodes_refid: function(
        mashupRefId, modelOutput, mashupInput, mashupContext, applySetModel) {
            var numRC = null;
            numRC = _scModelUtils.getNumberValueFromPath("CommonCodeList.TotalNumberOfRecords", modelOutput);
            if (
            _scBaseUtils.equals(
            numRC, 0)) {
                this.isUnpackRCConfigured = false;
            }
        },
        handle_unpack_deleteContainer_refid: function(
        mashupRefId, modelOutput, mashupInput, mashupContext, applySetModel) {
            var contNo = null;
            contNo = _scModelUtils.getStringValueFromPath("Shipment.Containers.Container.ContainerNo", mashupInput);
            var bundleArray = null;
            bundleArray = [];
            bundleArray.push(
            contNo);
            var warnMsg = null;
            warnMsg = _scScreenUtils.getFormattedString(
            this, "Message_ContainerDeleted", bundleArray);
            var messageConfig = null;
            messageConfig = {};
            messageConfig["callBackHandler"] = "handleContainerDeleteWarning";
            var callBackHandlerArgs = null;
            callBackHandlerArgs = {};
            callBackHandlerArgs["shipmentModel"] = modelOutput;
            messageConfig["messageArgs"] = callBackHandlerArgs;
            _iasBaseTemplateUtils.showMessage(
            this, warnMsg, "success", messageConfig);
            if (!(
            _iasContextUtils.isMobileContainer())) {
                this.reloadSummaryAfterContDelete(
                modelOutput);
            }
        },
        handleContainerDeleteWarning: function(
        result, args) {
            this.reloadSummaryAfterContDelete(
            _scBaseUtils.getModelValueFromBean("shipmentModel", args));
        },
        reloadSummaryAfterContDelete: function(
        modelOutput) {
            this.reloadPrimaryDetails(
            modelOutput);
            this.reloadContainerList();
            this.reloadProductList();
        },
        reloadPrimaryDetails: function(
        modelOutput) {
            _scScreenUtils.setModel(
            this, "getShipmentDetails_output", modelOutput, null);
            _wscShipmentUtils.showNextTask(
            this, modelOutput, "Shipment.ShipNode.ShipNode", "lnkBRP", "lnkBRP", "", "lnkPack", "lnkPack", "lnkStartCustomerPickup");
            if (
            _iasContextUtils.isMobileContainer()) {
                this.fireSetWizardDescEvent(
                modelOutput);
            }
            var eventArgs = null;
            var eventDefn = null;
            eventDefn = {};
            eventArgs = {};
            _scBaseUtils.setAttributeValue("shipmentModel", modelOutput, eventArgs);
            _scBaseUtils.setAttributeValue("statusPath", "Shipment.Status.Status", eventArgs);
            _scBaseUtils.setAttributeValue("argumentList", eventArgs, eventDefn);
            _scEventUtils.fireEventToParent(
            this, "reloadRelatedTask", eventDefn);
        },
        reloadContainerList: function() {
            var getShipmentContainerListTargetModel = null;
            getShipmentContainerListTargetModel = _scScreenUtils.getTargetModel(
            this, "getShipmentContainerList_input", null);
            _scModelUtils.setStringValueAtModelPath("Shipment.isTrackingURLRequired", "Yes", getShipmentContainerListTargetModel);
            _scRepeatingPanelUtils.startPaginationUsingUId(
            this, "cpContainerLinesPaginationPanel", "getShipmentContainerList_output", getShipmentContainerListTargetModel, "getShipmentContainerList");
            this.isContainerListPainted = "true";
        },
        resetScreenAfterDelete: function(
        event, bEvent, widget_uId, args) {
            this.lastSelectedContainerUId = "";
        },
        showSelectedContainerRepPanel: function(
        event, bEvent, widget_uId, args) {
            var currentSelectedContainerUId = null;
            currentSelectedContainerUId = _scBaseUtils.getStringValueFromBean("childScreenuId", args);
            if (!(
            _scBaseUtils.isVoid(
            this.lastSelectedContainerUId))) {
                var lastSelectedContainer = null;
                lastSelectedContainer = _scScreenUtils.getChildScreen(
                this, this.lastSelectedContainerUId);
                _scScreenUtils.removeClass(
                lastSelectedContainer, "highlightRepeatingPanel");
            }
            if (!(
            _scBaseUtils.isVoid(
            currentSelectedContainerUId))) {
                var currentSelectedContainer = null;
                currentSelectedContainer = _scScreenUtils.getChildScreen(
                this, currentSelectedContainerUId);
                _scScreenUtils.addClass(
                currentSelectedContainer, "highlightRepeatingPanel");
                this.lastSelectedContainerUId = currentSelectedContainerUId;
            }
        }
    });
});