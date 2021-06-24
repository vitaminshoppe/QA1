
scDefine(["dojo/text!./templates/ViewBundleComponentsPopupExtn.html","scbase/loader!dijit/form/Button","scbase/loader!dojo/_base/declare","scbase/loader!dojo/_base/kernel","scbase/loader!dojo/_base/lang","scbase/loader!dojo/text","scbase/loader!gridx/Grid","scbase/loader!gridx/modules/CellWidget","scbase/loader!gridx/modules/ColumnResizer","scbase/loader!gridx/modules/ColumnWidth","scbase/loader!gridx/modules/Edit","scbase/loader!gridx/modules/HLayout","scbase/loader!gridx/modules/IndirectSelect","scbase/loader!gridx/modules/RowHeader","scbase/loader!gridx/modules/extendedSelect/Row","scbase/loader!gridx/modules/select/Row","scbase/loader!idx/layout/ContentPane","scbase/loader!sc/plat","scbase/loader!sc/plat/dojo/binding/ButtonDataBinder","scbase/loader!sc/plat/dojo/binding/GridxDataBinder","scbase/loader!sc/plat/dojo/utils/BaseUtils"]
 , function(			 
			    templateText
			 ,
			    _dijitButton
			 ,
			    _dojodeclare
			 ,
			    _dojokernel
			 ,
			    _dojolang
			 ,
			    _dojotext
			 ,
			    _gridxGrid
			 ,
			    _gridxCellWidget
			 ,
			    _gridxColumnResizer
			 ,
			    _gridxColumnWidth
			 ,
			    _gridxEdit
			 ,
			    _gridxHLayout
			 ,
			    _gridxIndirectSelect
			 ,
			    _gridxRowHeader
			 ,
			    _gridxRow
			 ,
			    _gridxRow
			 ,
			    _idxContentPane
			 ,
			    _scplat
			 ,
			    _scButtonDataBinder
			 ,
			    _scGridxDataBinder
			 ,
			    _scBaseUtils
){
return _dojodeclare("extn.order.create.additems.ViewBundleComponentsPopupExtnUI",
				[], {
			templateString: templateText
	
	
	
	
	
	
	
					,	
	namespaces : {
		targetBindingNamespaces :
		[
			{
	  value: 'extn_orderHoldTypeList'
						,
	  scExtensibilityArrayItemId: 'extn_TargetNamespaces_1'
						
			}
			
		],
		sourceBindingNamespaces :
		[
			{
	  value: 'extn_getOrderHoldTypeList'
						,
	  scExtensibilityArrayItemId: 'extn_SourceNamespaces_6'
						
			}
			
		]
	}

	
	,
	hotKeys: [ 
	]

,events : [
	]

,subscribers : {

local : [

{
	  eventId: 'afterScreenInit'

,	  sequence: '51'




,handler : {
methodName : "extn_afterScreenInit"

 
}
}
,
{
	  eventId: 'extn_button_apply_onClick'

,	  sequence: '51'




,handler : {
methodName : "extn_resolveHoldOnApply"

 
}
}

]
}

});
});


