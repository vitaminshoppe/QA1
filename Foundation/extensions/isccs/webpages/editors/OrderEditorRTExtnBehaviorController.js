


scDefine(["scbase/loader!dojo/_base/declare","scbase/loader!dojo/_base/kernel","scbase/loader!dojo/text","scbase/loader!extn/editors/OrderEditorRTExtn","scbase/loader!sc/plat/dojo/controller/ExtnServerDataController"]
 , function(			 
			    _dojodeclare
			 ,
			    _dojokernel
			 ,
			    _dojotext
			 ,
			    _extnOrderEditorRTExtn
			 ,
			    _scExtnServerDataController
){

return _dojodeclare("extn.editors.OrderEditorRTExtnBehaviorController", 
				[_scExtnServerDataController], {

			
			 screenId : 			'extn.editors.OrderEditorRTExtn'

			
			
			
			
			
						,

			
			
			 mashupRefs : 	[
	 		{
		 mashupRefId : 			'extn_changeOrder_OrderEditorRT'
,
		 mashupId : 			'extn_changeOrder_OrderEditorRT'
,
		 extnType : 			'ADD'

	}
,
	 		{
		 mashupRefId : 			'extn_getOrderHoldTypeList'
,
		 mashupId : 			'extn_getOrderHoldTypeList'
,
		 extnType : 			'ADD'

}
	]

}
);
});

