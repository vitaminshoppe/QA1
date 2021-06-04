scDefine([
		"scbase/loader!dojo/_base/declare",
		"scbase/loader!extn/order/fulfillmentSummary/FSUnavailableShippingListScreenExtnUI",
		"scbase/loader!sc/plat/dojo/utils/WidgetUtils",
		"scbase/loader!sc/plat/dojo/utils/ScreenUtils",
		"scbase/loader!sc/plat/dojo/utils/ModelUtils", 
		"scbase/loader!sc/plat/dojo/utils/BaseUtils",
		"scbase/loader!isccs/utils/OrderUtils",
		"scbase/loader!sc/plat/dojo/utils/EventUtils",
		"scbase/loader!dijit",
		"scbase/loader!sc/plat/dojo/utils/GridxUtils",
		"scbase/loader!isccs/utils/UIUtils",
		"scbase/loader!isccs/utils/SharedComponentUtils"
		]
,
function(			 
		_dojodeclare,
		_extnFSUnavailableShippingListScreenExtnUI,
		_scWidgetUtils,
		_scScreenUtils,
		_scModelUtils,
		_scBaseUtils,
		_isccsOrderUtils,
		_scEventUtils,
		_dijit,
		_scGridxUtils,
		_isccsUIUtils,
		_isccsSharedComponentUtils
){ 
	return _dojodeclare("extn.order.fulfillmentSummary.FSUnavailableShippingListScreenExtn", [_extnFSUnavailableShippingListScreenExtnUI],{
	// custom code here
	
	selectAllRows: function(){
		var grid = this.getWidgetByUId("OLST_listGrid");
		
		_scGridxUtils.selectAllRowsUsingUId(this,"OLST_listGrid");
		//this.checkOrderStatusforAddressChange();
	},
    updateShippingAddress: function(
            argsModel) {
                var selectedSourceRecordsList = null;
                selectedSourceRecordsList = _scGridxUtils.getSelectedSourceRecordsWithRowIndex(
                this, "OLST_listGrid");
                var selectedTargetRecordsList = null;
                selectedTargetRecordsList = _scGridxUtils.getSelectedTargetRecordsWithRowIndex(
                this, "OLST_listGrid");
                var listOfMergedModel = null;
                listOfMergedModel = this.mergeModel(
                selectedSourceRecordsList, selectedTargetRecordsList);
                var newModelData = null;
                newModelData = {};
                _scModelUtils.addModelToModelPath("PersonInfoShipTo", _scModelUtils.getModelObjectFromPath("Order.PersonInfoShipTo", argsModel), newModelData);
                var orderLineKeyList = null;
                orderLineKeyList = _scModelUtils.getModelListFromPath("Order.OrderLineKeyList", argsModel);
                var lengthOfSourceRecord = null;
                lengthOfSourceRecord = _scBaseUtils.getAttributeCount(
                listOfMergedModel);
                for (
                var z = 0;
                z < lengthOfSourceRecord;
                z = z + 1) {
                    var beanItem = null;
                    beanItem = listOfMergedModel[
                    z];
                    var lineIndex = null;
                    lineIndex = _scBaseUtils.getNumberValueFromBean("rowIndex", beanItem);
                    var srcRowData = null;
                    srcRowData = _scGridxUtils.getItemFromRowIndexUsingUId(
                    this, "OLST_listGrid", lineIndex);
                    var rowJson = null;
                    rowJson = _scGridxUtils.getRowJsonFromRowIndex(
                    this, "OLST_listGrid", lineIndex);
                    var orderLineKey = null;
                    orderLineKey = _scModelUtils.getStringValueFromPath("OrderLineKey", rowJson);
                    var isInArray = false;
                    var isAnyRowUpdated = false;
                    isInArray = this.isInArray(
                    orderLineKey, orderLineKeyList);
                    if (
                    isInArray) {
                        _scGridxUtils.updateRecordToGridUsingUId(
                        this, "OLST_listGrid", srcRowData, newModelData, true, true);
                        isAnyRowUpdated = true;
                    }
                }
                if (
                isAnyRowUpdated) {
                    var temp = null;
                    temp = {};
                    _scEventUtils.fireEventToParent(
                    this, "ScreenChanged", temp);
                    _scEventUtils.fireEventToParent(
                    this, "SaveOrder", temp);
                }
               // _scGridxUtils.deselectAllRowsInGridUsingUId(
               // this, "OLST_listGrid");
            },
        	checkOrderStatusforAddressChange: function()
        	{
        		var model = _scScreenUtils.getModel(this, "getFulfillmentSummaryDetails_output");
        		var orderLine = _scModelUtils.getStringValueFromPath("Page.Output.OrderLines.OrderLine", model);
        		var flag = false;
        		for(i=0;i<orderLine.length;i++){
        			var orderStatus = _scModelUtils.getStringValueFromPath("OrderStatuses.OrderStatus", orderLine[i]);		
        			if(_scBaseUtils.isVoid(orderStatus))
        			{
        				return;
        			}
        			for(j=0;j<orderStatus.length;j++){				
        				var status = _scModelUtils.getNumberValueFromPath("Status", orderStatus[j]);
        				//console.log('ss',status);
        				var b = 3200;
        				if (parseFloat(status.toFixed(2)) > parseFloat(b.toFixed(2)))
        				 {				
        					flag = true;
        					_scWidgetUtils.disableWidget(this, "changeAddress", true);
        					_scWidgetUtils.disableWidget(this, "changeLOS", true);
        					break;
        				}
        			}
        			if(flag){
        				break;
        			}
        			
        		} 
        	},
            updateDeliveryMethodDetails: function(
                    mapModelData) {
						console.log("** updateDeliveryMethodDetails ** mapModelData", mapModelData);
                        var selectedRecordsList = null;
                        selectedRecordsList = _scGridxUtils.getSelectedSourceRecordsWithRowIndex(
                        this, "OLST_listGrid");
                        var noOfOrderLine = null;
                        noOfOrderLine = _scBaseUtils.getAttributeCount(
                        selectedRecordsList);
						var countToCallUpdateOrder = 0;
                        for (
                        var i = 0;
                        i < noOfOrderLine;
                        i = i + 1) {
                            var beanItem = null;
                            beanItem = selectedRecordsList[
                            i];
                            var lineIndex = null;
                            lineIndex = _scBaseUtils.getNumberValueFromBean("rowIndex", beanItem);
                            var srcRowData = null;
                            srcRowData = _scGridxUtils.getItemFromRowIndexUsingUId(
                            this, "OLST_listGrid", lineIndex);
							// ARH-176 : Modified to stamp LineType and FulfillmentType for pick up lines : BEGIN
							console.log("srcRowData", srcRowData);
							// Updated code for adding OrderLineKey in Map for future inventory : ARR7 and SU -74 - START
            				var orderLineKeyArr = _scModelUtils.getModelListFromKey("OrderLineKey", srcRowData);
            				var orderLineKey = orderLineKeyArr[0];
            				// pass the orderlinekey and fetch all required data respective to that orderline from mapdata
            				var newModelData = mapModelData[orderLineKey];
							console.log("newModelData", newModelData);
                            var shipNodeColData = _scModelUtils.getModelListFromKey("ShipNode", srcRowData);
                            var currentShipNode = shipNodeColData[0];
                            var currentShipNodeStr = currentShipNode;
							var newShipNode = _scModelUtils.getStringValueFromPath("ShipNode", newModelData);
							console.log("currentShipNodeStr", currentShipNodeStr);
							console.log("newShipNode", newShipNode);
                            if (!(_scBaseUtils.equals(currentShipNodeStr, newShipNode))) {
            					this.ownerScreen.hashMapFuturePickUpLines["APPLY"] = "Y"
            					var isFutureAvailability = _scModelUtils.getStringValueFromPath("IsFutureAvailability", newModelData);
            					// Added isAvailableOnStore flag and condition check to fix Defect SU-74.
            					var isAvailableOnStore = _scModelUtils.getStringValueFromPath("IsAvailableOnStore", newModelData);
            					if (_scBaseUtils.equals(isFutureAvailability, "Y") && !_scBaseUtils.equals(isAvailableOnStore, "Y")) {
            						// Set each OrderLineKey as key and the flag = 'Y' as value.
            						this.ownerScreen.hashMapFuturePickUpLines[orderLineKey] = "Y";
            						this.ownerScreen.hashMapFuturePickUpLines['itemid_'+orderLineKey] = srcRowData._dataItem.Item.ItemID;
            					}
            					
                                _scGridxUtils.updateRecordToGridUsingUId(this, "OLST_listGrid", srcRowData, newModelData, true, true);
                                countToCallUpdateOrder = countToCallUpdateOrder + 1;
                            }
            				// Updated code for adding OrderLineKey in Map for future inventory : ARR7 and SU-74 - END
                            //_scGridxUtils.updateRecordToGridUsingUId(
                            //this, "OLST_listGrid", srcRowData, newModelData, true, true);
                            if (countToCallUpdateOrder > 0) {
								var temp = null;
								temp = {};
								_scEventUtils.fireEventToParent(
								this, "SaveOrder", temp);
							}
							// ARH-176 : Modified to stamp LineType and FulfillmentType for pick up lines : END
                        }
                       // _scGridxUtils.deselectAllRowsInGridUsingUId(
                      //  this, "OLST_listGrid");
                    },
	/* Mixed Cart Implementation : Below code is commented */
	/* onRowDeselect: function(
		        event, bEvent, ctrl, args) {
		            var deselectedRow = null;
		            deselectedRow = _scBaseUtils.getBeanValueFromBean("rowIndex", args);
		            var argument = null;
		            argument = {};
		            var orderLineKey = null;
		            orderLineKey = _scBaseUtils.getStringValueFromBean("OrderLineKey", deselectedRow);
		            _scBaseUtils.setAttributeValue("argumentList.OrderLineKey", orderLineKey, argument);
		            _scBaseUtils.setAttributeValue("argumentList.Select", "N", argument);
		            //_scEventUtils.fireEventToParent(
		           // this, "SelectOrUnselectRowInShippingPanel", argument);
		            var warningString = null;
        			warningString = _scScreenUtils.getString(
        			this, "WARNING_selecteOneItem");
        			var textObj = null;
        			textObj = {};
        			var textOK = null;
        			textOK = _scScreenUtils.getString(
        			this, "OK");
        			textObj["OK"] = textOK;
		            _scGridxUtils.selectRowUsingUId(this,"OLST_listGrid",deselectedRow);
					var errorMessage = _scScreenUtils.getString(this, "extn_Row_Deselected_Msg_Shipping");
        			_scScreenUtils.showErrorMessageBox(
        				this, errorMessage, "waringCallback", textObj, null);
		        }, */
		/* Mixed Cart - End */
        onRowOrHeaderDeselect: function(
        event, bEvent, ctrl, args) {
            var argument = null;
            argument = {};
            var selectedSourceRecordsList = null;
            selectedSourceRecordsList = _scGridxUtils.getSelectedSourceRecordsWithRowIndex(
            this, "OLST_listGrid");
            var lengthOfSourceRecord = null;
            lengthOfSourceRecord = _scBaseUtils.getAttributeCount(
            selectedSourceRecordsList);
            if (
            _scBaseUtils.equals(
            lengthOfSourceRecord, 0)) {
                _scWidgetUtils.enableWidget(
                this, "changeAddress");
                _scWidgetUtils.enableWidget(
                    this, "changeToDelivery");
                _scWidgetUtils.enableWidget(
                this, "changeToPickup");
            } else {
                this.validateSelectedRecordsForPermission(
                selectedSourceRecordsList, lengthOfSourceRecord);
            }
            var deselectedRowsList = null;
            deselectedRowsList = _scBaseUtils.getListFromBean("deselectedRows", args);
            var rowCount = 0;
            rowCount = _scBaseUtils.getAttributeCount(
            deselectedRowsList);
            for (
            var i = 0;
            i < rowCount;
            i = i + 1) {
                var deselectedRowData = null;
                deselectedRowData = deselectedRowsList[
                i];
                /* var rowData = null;
                rowData = _scBaseUtils.getAttributeValue("rowData", false, deselectedRowData);
                var orderLineKey = null;
                orderLineKey = _scModelUtils.getStringValueFromKey("OrderLineKey", rowData);
                _scBaseUtils.setAttributeValue("argumentList.OrderLineKey", orderLineKey, argument);
                _scBaseUtils.setAttributeValue("argumentList.Select", "N", argument);
                _scEventUtils.fireEventToParent(
                this, "SelectOrUnselectRowInShippingPanel", argument); */
				_scGridxUtils.selectRowUsingUId(this,"OLST_listGrid",deselectedRowData.rowIndex);
				if (i == 0) {
					var errorMessage = _scScreenUtils.getString(this, "extn_Header_Row_Deselected_Msg");
					_scScreenUtils.showErrorMessageBox(this, errorMessage, "error",null,null);
				}
            }
        },
        changeToPickupCallback: function(
        actionPerformed, model, popupParams) {
            var shipnode = null;
            var tmodel = null;
            var shipNodeDesc = null;
            var orderLineModel = null;
            var sApply = null;
            var availabilityDate = null;
            var currentContainer = null;
            var newModelData = null;
			var mapModelData = {};
            var gridScreens = null;
            //newModelData = {};
            var argument = null;
            argument = {};
            _scBaseUtils.setAttributeValue("argumentList.Select", "N", argument);
            var argumentForStore = null;
            argumentForStore = {};
            if (
            _scBaseUtils.equals(
            actionPerformed, "APPLY")) {
                if (!(
                _scBaseUtils.isVoid(
                model))) {
                    shipnode = _scModelUtils.getStringValueFromPath("Node.Availability.ShipNode", model);
					// ARH-176 : Modified to stamp LineType and FulfillmentType for pick up lines : BEGIN
                    //availabilityDate = _scModelUtils.getStringValueFromPath("Node.Availability.AvailableDate", model);
                    shipNodeDesc = _scModelUtils.getStringValueFromPath("Node.Description", model);
					
					// Updated code for ARR7 - START
    				var orderLineList = _scModelUtils.getStringValueFromPath("Node.OrderLines.OrderLine", model);
    				for (var j = 0; j < _scBaseUtils.getAttributeCount(orderLineList); j = j + 1) {
    					newModelData = {};
    					var orderLine = orderLineList[j];
    					console.log("orderLine", orderLine);
    					var isAvailable = _scModelUtils.getStringValueFromPath("IsAvailable", orderLine);
    					var orderLineKey = _scModelUtils.getStringValueFromPath("OrderLineKey", orderLine);
    					var isFutureAvailability = _scModelUtils.getStringValueFromPath("IsFutureAvailability", orderLine);
    					// Added isAvailableOnStore flag and condition check to fix Defect SU-74.
    					var isAvailableOnStore = _scModelUtils.getStringValueFromPath("IsAvailableOnStore", orderLine);
    					var availabilityDate = _scModelUtils.getStringValueFromPath("AvailableDate", orderLine);
    					if (_scBaseUtils.equals(isAvailable, "Y") && _scBaseUtils.equals(isFutureAvailability, "Y")) {
    						_scModelUtils.setStringValueAtModelPath("IsFutureAvailability", isFutureAvailability, newModelData);
    						_scModelUtils.setStringValueAtModelPath("IsAvailableOnStore", isAvailableOnStore, newModelData);
    					}
    					// Updated code for ARR7 - END
						
						_scModelUtils.setStringValueAtModelPath("EarliestShipDate", availabilityDate, newModelData);
						_scModelUtils.setStringValueAtModelPath("ReqShipDate", "", newModelData);
						_scModelUtils.setStringValueAtModelPath("ShipNode", shipnode, newModelData);
						_scModelUtils.setStringValueAtModelPath("DeliveryMethod", "PICK", newModelData);
						var selectedLineZero = false;
						var orderLines = null;
						orderLines = [];
						_scBaseUtils.setAttributeValue("argumentList.StoreData", newModelData, argumentForStore);
						console.log("changeToPickupCallback orderLineKey", orderLineKey);
						console.log("newModelData", newModelData);
						mapModelData[orderLineKey] = newModelData;
					}
                    this.updateDeliveryMethodDetails(
                    mapModelData);
					// ARH-176 : Modified to stamp LineType and FulfillmentType for pick up lines : END
                }
            } else {}
        },
        changeToPickup: function(
        event, bEvent, ctrl, args) {
            var selectedRecords = null;
            selectedRecords = _scGridxUtils.getSelectedSourceRecordsWithRowIndex(
            this, "OLST_listGrid");
            var processedRecords = null;
			// ARH-176 : Modified to stamp LineType and FulfillmentType for pick up lines : BEGIN
            //processedRecords = _isccsOrderUtils.processSelectedRecordsForStoreSelection(
            //this, "OLST_listGrid", selectedRecords);
            processedRecords = this.processSelectedRecordsForStoreSelection(
            this, "OLST_listGrid", selectedRecords);
			// ARH-176 : Modified to stamp LineType and FulfillmentType for pick up lines : END
            var noOfOrderLine = null;
            noOfOrderLine = _scBaseUtils.getAttributeCount(
            processedRecords);
            if (
            noOfOrderLine > 0) {
                var fulfillmentModel = null;
                fulfillmentModel = _scScreenUtils.getModel(
                this, "getCompleteOrderDetails_output");
                var callingOrgCode = null;
                callingOrgCode = _scModelUtils.getStringValueFromPath("Order.EnterpriseCode", fulfillmentModel);
                var orderHeaderKey = null;
                orderHeaderKey = _scModelUtils.getStringValueFromPath("Order.OrderHeaderKey", fulfillmentModel);
                var ePersonInfo = null;
                ePersonInfo = _scModelUtils.getModelObjectFromPath("Order.PersonInfoShipTo", fulfillmentModel);
                var orderInfoModel = null;
                var orderModel = null;
                orderInfoModel = {};
                orderModel = {};
                _scModelUtils.addStringValueToModelObject("OrderHeaderKey", orderHeaderKey, orderInfoModel);
                _scModelUtils.addStringValueToModelObject("EnterpriseCode", callingOrgCode, orderInfoModel);
                _scModelUtils.addModelObjectAsChildToModelObject("Order", orderInfoModel, orderModel);
                _isccsSharedComponentUtils.openStoreSelectionForMultipleLines(
                this, processedRecords, orderModel, ePersonInfo, callingOrgCode, "ViewStores", "changeToPickupCallback");
            } else {
                var warningString = null;
                warningString = _scScreenUtils.getString(
                this, "WARNING_selecteOneItem");
                var textObj = null;
                textObj = {};
                var textOK = null;
                textOK = _scScreenUtils.getString(
                this, "textOK");
                textObj["OK"] = textOK;
                _scScreenUtils.showErrorMessageBox(
                this, warningString, "waringCallback", textObj, null);
            }
        },
        
		// Added below method as part of SU74 Fix / ARH-176
		processSelectedRecordsForStoreSelection: function(screen,uId,selectedRecords){
			/*This util will take the array of records, fetch the OrderLineKey & Qty and return an array back which contain orderLines which have only these two attributes.
						  This is used to prepare the model to send the orderLines to the select storePopup.*/
			var orderLines = [];
			for(var i=0;i<selectedRecords.length;i++){
				// to clean up and add a model despite of removing
				var currentRecord = selectedRecords[i].rowData;
				var orderlineKey = _scBaseUtils.getAttributeValue("OrderLineKey",false,currentRecord);
				var giftWrap = _scBaseUtils.getAttributeValue("GiftWrap",false,currentRecord);
				
				
				
				var itemObj = _scModelUtils.getModelObjectFromPath("Item", currentRecord);
				itemObj.UOMDisplayFormat = "";
				itemObj.DisplayUnitOfMeasure = "";
				_scBaseUtils.removeBlankAttributes(itemObj);
				var reqQty = _scBaseUtils.getAttributeValue("OrderedQty",false,currentRecord);
				var orderLine = {};
				
				if(itemObj){
					orderLine.Item = {};
					orderLine.Item=currentRecord.Item;
				}
				orderLine.OrderLineKey= orderlineKey;
				orderLine.GiftWrap= giftWrap;
				orderLine.RequiredQty = reqQty;
				orderLines.push(orderLine);
			}
			return orderLines;
		}
});
});

