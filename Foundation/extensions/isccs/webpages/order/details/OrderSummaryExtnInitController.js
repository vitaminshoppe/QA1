


scDefine(["scbase/loader!dojo/_base/declare","scbase/loader!dojo/_base/kernel","scbase/loader!dojo/text","scbase/loader!extn/order/details/OrderSummaryExtn","scbase/loader!sc/plat/dojo/controller/ExtnScreenController"]
 , function(			 
			    _dojodeclare
			 ,
			    _dojokernel
			 ,
			    _dojotext
			 ,
			    _extnOrderSummaryExtn
			 ,
			    _scExtnScreenController
){

return _dojodeclare("extn.order.details.OrderSummaryExtnInitController", 
				[_scExtnScreenController], {

			
			 screenId : 			'extn.order.details.OrderSummaryExtn'

			
			
			
			
			
						,

			
			
			 mashupRefs : 	[
	 		{
		 sourceBindingOptions : 			''
,
		 sequence : 			''
,
		 mashupId : 			'orderSummary_getCompleteOrderDetails'
,
		 sourceNamespace : 			'getCompleteOrderDetails_output'
,
		 mashupRefId : 			'getCompleteOrderDetails'
,
		 extnType : 			''
,
		 callSequence : 			''

	}

	]

}
);
});

