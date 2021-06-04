
scDefine(["dojo/text!./templates/ShipmentRTExtn.html","scbase/loader!dojo/_base/declare","scbase/loader!dojo/_base/kernel","scbase/loader!dojo/_base/lang","scbase/loader!dojo/text","scbase/loader!sc/plat","scbase/loader!sc/plat/dojo/binding/CurrencyDataBinder","scbase/loader!sc/plat/dojo/utils/BaseUtils","scbase/loader!sc/plat/dojo/widgets/Link"]
 , function(			 
			    templateText
			 ,
			    _dojodeclare
			 ,
			    _dojokernel
			 ,
			    _dojolang
			 ,
			    _dojotext
			 ,
			    _scplat
			 ,
			    _scCurrencyDataBinder
			 ,
			    _scBaseUtils
			 ,
			    _scLink
){
return _dojodeclare("extn.components.shipment.summary.ShipmentRTExtnUI",
				[], {
			templateString: templateText
	
	
	
	
	
	
	
	
	,
	hotKeys: [ 
	]

,events : [
	]

,subscribers : {

local : [

{
	  eventId: 'extn_printpickpackreceipt_onClick'

,	  sequence: '51'




,handler : {
methodName : "printPickPackReceipt"

 
}
}
,
{
	  eventId: 'extn_customerpickupreceipt_onClick'

,	  sequence: '51'




,handler : {
methodName : "printCustomerReceipt"

 
}
}
,
{
	  eventId: 'extn_printpackreceipt_onClick'

,	  sequence: '51'




,handler : {
methodName : "printPackReceipt"

 
}
}

]
}

});
});


