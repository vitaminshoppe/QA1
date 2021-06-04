
scDefine([
	"scbase/loader!dojo/_base/declare",
	"scbase/loader!extn/editors/OrderEditorRTExtnUI",
	"scbase/loader!isccs/utils/RelatedTaskUtils",
	"scbase/loader!isccs/utils/UIUtils",
	"scbase/loader!sc/plat/dojo/utils/ControllerUtils",
	"scbase/loader!sc/plat/dojo/utils/EditorUtils",
	"scbase/loader!sc/plat/dojo/utils/ScreenUtils",
	"scbase/loader!sc/plat/dojo/utils/ModelUtils",
	"scbase/loader!sc/plat/dojo/utils/WidgetUtils",
	"scbase/loader!sc/plat/dojo/widgets/Screen",
	"scbase/loader!sc/plat/dojo/utils/WizardUtils",
		"scbase/loader!isccs/utils/BaseTemplateUtils",
		"scbase/loader!sc/plat/dojo/utils/BaseUtils",
		"scbase/loader!sc/plat/dojo/utils/ModelUtils"
	]
,
function(			 
	_dojodeclare,
	_extnOrderEditorRTExtnUI,
	_isccsRelatedTaskUtils,
	_isccsUIUtils,
	_scControllerUtils,
	_scEditorUtils,
	_scScreenUtils,
	_scModelUtils,
		_scWidgetUtils,
		scScreen,
		_scWizardUtils,
			_isccsBaseTemplateUtils,
			_scBaseUtils,
			_scModelUtils
			
){ 
	return _dojodeclare("extn.editors.OrderEditorRTExtn", [_extnOrderEditorRTExtnUI],{
	
	fulfillGiftCard_onClick: function(
        event, bEvent, ctrl, args) {
            var taskInput = null;
            var taskConfig = null;
            taskInput = _isccsRelatedTaskUtils.getRelatedTaskInput(
            this);
            _isccsUIUtils.openWizardInEditor("extn.order.wizards.giftCard.GiftCardFulfillmentWizard", taskInput, "isccs.editors.OrderEditor", this);
        },
		restrictAddLinesForShippedOrder: function()
		{
			mCompleteOrdDetails = this.getModel("getCompleteOrderLineList_output");
				console.log("return mCompleteOrdDetails: ", mCompleteOrdDetails);
				var taskInput = _isccsRelatedTaskUtils.getRelatedTaskInput(this);
				console.log('I am here',taskInput.Order.OrderName);
				if(taskInput.Order.Status=='Shipped')
				{
					_scWidgetUtils.disableWidget(this, "AddLinesToOrderWizard", true);
				}	
		},	
		 dataMigration: function() {
               // mCompleteOrdDetails = this.getModel("getCompleteOrderLineList_output");
				//console.log("return mCompleteOrdDetails: ", mCompleteOrdDetails);
				var taskInput = _isccsRelatedTaskUtils.getRelatedTaskInput(
				this);
				//console.log('I am here',taskInput.Order.OrderName);
				if(taskInput.Order.OrderName=='MigratedOrder')
				{
					//console.log('ddddd')
					_scWidgetUtils.disableWidget(this, "cancelOrder", true);
        			_scWidgetUtils.disableWidget(this, "addRemovePromotion", true);
        			_scWidgetUtils.disableWidget(this, "AddLinesToOrderWizard", true);
        			_scWidgetUtils.disableWidget(this, "changeOrderAddress", true);
        			_scWidgetUtils.disableWidget(this, "ResolveHoldWizard", true);
        			_scWidgetUtils.disableWidget(this, "ApplyHoldWizard", true);
        			_scWidgetUtils.disableWidget(this, "AddModifyChargesWizard", true);
        			_scWidgetUtils.disableWidget(this, "customerAppeasement", true);
        			_scWidgetUtils.disableWidget(this, "createReturn", true);
        			_scWidgetUtils.disableWidget(this, "changeFulfillmentOptions", true);
        			_scWidgetUtils.disableWidget(this, "extn_giftCardFulfillment", true);
        			_scWidgetUtils.disableWidget(this, "customerOptions", true);
        			_scWidgetUtils.disableWidget(this, "emailOrder", true);
        			_scWidgetUtils.disableWidget(this, "viewAllInvoices", true);
					//_isccsBaseTemplateUtils.showMessage(this, "extn_migrated_order_message", "warning", null);
				}
					
			},
		
			handleSendEmail: function ( model ){
	        	
	            var taskInput = null;
	            taskInput = _isccsRelatedTaskUtils.getRelatedTaskInput(this);
	            console.log('Hiii',taskInput);
	            var currentEditor = _scEditorUtils.getCurrentEditor();
	            var mashupContext = _scBaseUtils.getNewBeanInstance();
	            var input = _scModelUtils.createNewModelObjectWithRootKey("InvokeUE");
	            var deliveryMethod = taskInput.Order.OrderLines.OrderLine[0].DeliveryMethod;
	            _scModelUtils.setStringValueAtModelPath("InvokeUE.DisplayLocalizedFieldInLocale", "xml:CurrentUser:/User/@Localecode", input);
	            _scModelUtils.setStringValueAtModelPath("InvokeUE.DocumentType", "0001", input);
	            _scModelUtils.setStringValueAtModelPath("InvokeUE.IsUserExitImplemented", "Y", input);
	            _scModelUtils.setStringValueAtModelPath("InvokeUE.UserExit", "com.yantra.pca.ycd.japi.ue.YCDEmailOrderUE", input);
	            _scModelUtils.setStringValueAtModelPath("InvokeUE.XMLData.Order.DocumentType", _scModelUtils.getStringValueFromPath("Order.DocumentType", taskInput), input);
	            _scModelUtils.setStringValueAtModelPath("InvokeUE.XMLData.Order.EnterpriseCode", _scModelUtils.getStringValueFromPath("Order.EnterpriseCode", taskInput), input);
	            _scModelUtils.setStringValueAtModelPath("InvokeUE.XMLData.Order.OrderHeaderKey", _scModelUtils.getStringValueFromPath("Order.OrderHeaderKey", taskInput), input);
	            _scModelUtils.setStringValueAtModelPath("InvokeUE.XMLData.Order.OrderNo", _scModelUtils.getStringValueFromPath("Order.OrderNo", taskInput), input);
	            _scModelUtils.setStringValueAtModelPath("InvokeUE.XMLData.Order.DeliveryMethod",deliveryMethod, input);
	            _scModelUtils.setStringValueAtModelPath("InvokeUE.XMLData.Order.To", _scModelUtils.getStringValueFromPath("Email.emailID", model), input);
	            _isccsUIUtils.callApi(currentEditor, input, "invokeUE", mashupContext);
	        	
	        },
			
			extn_UpdateBOLNoOnClickHandler: function(event, bEvent, ctrl, args) {
				var currentEditor = _scEditorUtils.getCurrentEditor();
				var taskInput = _scScreenUtils.getInitialInputData(currentEditor);
				var dialogParams = null;
				dialogParams = _scBaseUtils.getNewBeanInstance();
				_scBaseUtils.addStringValueToBean("closeCallBackHandler", "extn_updateBOLAndDeliveryDatecallBackHandler", dialogParams);
			
				var popUpParams=null;
				popUpParams = _scBaseUtils.getNewBeanInstance();
				_scModelUtils.setStringValueAtModelPath("Order.Extn.ExtnBOLNumberPopup", "Y", taskInput);
				
				_scBaseUtils.addModelValueToBean("screenInput",taskInput,popUpParams);
				_isccsUIUtils.openSimplePopup("isccs.order.create.additems.GetOrganizationList", "Update BOL/Delivery Date", this, popUpParams, dialogParams);
			
			},
			
			extn_updateBOLAndDeliveryDatecallBackHandler: function(event, bEvent, ctrl, args) {
				

				if(event!="CLOSE")
				{
					var currentEditor = _scEditorUtils.getCurrentEditor();
					var taskInput = _scScreenUtils.getInitialInputData(currentEditor);
					var  input =_scModelUtils.createNewModelObjectWithRootKey("Order");
					_scModelUtils.setStringValueAtModelPath("Order.OrderHeaderKey",_scModelUtils.getStringValueFromPath("Order.OrderHeaderKey",taskInput),input);
					var extnModel= _scModelUtils.getModelObjectFromPath("Order.Extn",bEvent);
					_scModelUtils.addModelToModelPath("Order.Extn",extnModel,input);					

					_isccsUIUtils.callApi(this, input, "extn_changeOrder_OrderEditorRT");
				}

			},
			
			onExtnMashupCompletion: function (event, bEvent, ctrl, args) {
				if (args && args.mashupArray) {

					for (var index = 0; index < args.mashupArray.length; index++) {
						var mashupObj = args.mashupArray[index];

						if (mashupObj.mashupRefId == "extn_changeOrder_OrderEditorRT") {
							isccs.utils.OrderUtils.openOrderSummary(event, bEvent, ctrl, args);
						}
					}
				}
			},
			
			extn_afterScreenInit: function (event, bEvent, ctrl, args) {
				var currentEditor = _scEditorUtils.getCurrentEditor();
				var taskInput = _scScreenUtils.getInitialInputData(currentEditor);
				var entryType= _scModelUtils.getStringValueFromPath("Order.EntryType",taskInput);
				if(_scBaseUtils.equals(entryType,"WHOLESALE")){
					_scWidgetUtils.showWidget(this,"extn_link_updateBol",false);
					
				}
			}
        
});
});