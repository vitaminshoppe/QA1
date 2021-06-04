
scDefine(["scbase/loader!dojo/_base/declare","scbase/loader!extn/order/create/additems/GetOrganizationListExtnUI","scbase/loader!isccs/utils/UIUtils",
	"scbase/loader!sc/plat/dojo/utils/ControllerUtils",
	"scbase/loader!sc/plat/dojo/utils/EditorUtils",
	"scbase/loader!sc/plat/dojo/utils/ScreenUtils",
	"scbase/loader!sc/plat/dojo/utils/ModelUtils",
	"scbase/loader!sc/plat/dojo/utils/WidgetUtils",
	"scbase/loader!sc/plat/dojo/widgets/Screen",
	"scbase/loader!sc/plat/dojo/utils/WizardUtils",
		"scbase/loader!isccs/utils/BaseTemplateUtils",
		"scbase/loader!sc/plat/dojo/utils/BaseUtils",
		"scbase/loader!sc/plat/dojo/utils/ModelUtils"]
,
function(			 
			    _dojodeclare
			 ,
			    _extnGetOrganizationListExtnUI
			,	_isccsUIUtils,
				_scControllerUtils,
				_scEditorUtils,
				_scScreenUtils,
				_scModelUtils,
				_scWidgetUtils,
				_scScreen,
				_scWizardUtils,
				_isccsBaseTemplateUtils,
				_scBaseUtils,
				_scModelUtils	
){ 
	return _dojodeclare("extn.order.create.additems.GetOrganizationListExtn", [_extnGetOrganizationListExtnUI],{
		
		extn_afterScreenInit: function(event, bEvent, ctrl, args){
			var vInitialInputData = null;
			var orderDetailsModel=null;
			vInitialInputData = _scScreenUtils.getInitialInputData(this);
			var bolPopupFlag = _scModelUtils.getStringValueFromPath("Order.Extn.ExtnBOLNumberPopup", vInitialInputData);
			if(_scBaseUtils.equals(bolPopupFlag,"Y")){
				_scWidgetUtils.hideWidget(this, "orgList", false);
				_scWidgetUtils.showWidget(this, "extn_textfield_BOLNumber", false);
				_scWidgetUtils.showWidget(this, "extn_datetextbox_deliveryDate", false);
			}
			
			//extn_textfield_BOL_No,orgList
		}
	// custom code here
});
});

