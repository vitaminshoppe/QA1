
scDefine(["scbase/loader!dojo/_base/declare","scbase/loader!extn/return/create/lines/ReturnOrderLinesExtnUI", "scbase/loader!sc/plat/dojo/utils/GridxUtils", "scbase/loader!sc/plat/dojo/utils/ModelUtils", "scbase/loader!isccs/utils/SearchUtils", "scbase/loader!isccs/utils/OrderUtils", "scbase/loader!sc/plat/dojo/utils/ScreenUtils","scbase/loader!sc/plat/dojo/utils/BaseUtils"]
,
function(			 
			    _dojodeclare
			 ,
			    _extnReturnOrderLinesExtnUI, _scGridxUtils, _scModelUtils, _isccsSearchUtils, _isccsOrderUtils, _scScreenUtils, _scBaseUtils
){ 
	return _dojodeclare("extn.return.create.lines.ReturnOrderLinesExtn", [_extnReturnOrderLinesExtnUI],{
	// custom code here
	
	extn_listGrid_afterPagingload : function (event, bEvent, ctrl, args) {
			_scScreenUtils.setModel(this, "getReturnReasonList_output", _scScreenUtils.getModel(this, "getReturnReasonList_output"), null);
		},
		
	setRetunableQtyForReshippedLines: function (event, bEvent, ctrl, args) {
		var namespace = _scBaseUtils.getModelValueFromBean("namespace", args)
		var modelObj = _scBaseUtils.getModelValueFromBean("modelObject",args);
		if(namespace == 'getCompleteOrderLineList_output'){
			var orderLineList = _scModelUtils.getModelListFromPath("OrderLineList.OrderLine",modelObj);
			
			for(var i=0; i< orderLineList.length;i++){
				var orderLine = orderLineList[i];
				var extnReshippedLineFlag = _scModelUtils.getStringValueFromPath("Extn.ExtnReshippedLineFlag",orderLine);
				var extnReshipQty = _scModelUtils.getNumberValueFromPath("Extn.ExtnReshippedQty",orderLine);
			    var returnableQty =  _scModelUtils.getNumberValueFromPath("ReturnableQty",orderLine);
			    if(extnReshippedLineFlag == 'Y'){
            if(returnableQty >= extnReshipQty){
			    	  var calRetQty = returnableQty - extnReshipQty;
			         _scModelUtils.setStringValueAtModelPath("ReturnableQty",_scBaseUtils.toString(calRetQty),orderLine);
			      }  
          }
			}
		}
	}
});
});

