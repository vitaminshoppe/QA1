
scDefine(["dojo/text!./templates/GetOrganizationListExtn.html","scbase/loader!dojo/_base/declare","scbase/loader!dojo/_base/kernel","scbase/loader!dojo/_base/lang","scbase/loader!dojo/text","scbase/loader!idx/form/DateTextBox","scbase/loader!idx/form/TextBox","scbase/loader!sc/plat","scbase/loader!sc/plat/dojo/binding/DateDataBinder","scbase/loader!sc/plat/dojo/binding/SimpleDataBinder","scbase/loader!sc/plat/dojo/utils/BaseUtils"]
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
			    _idxDateTextBox
			 ,
			    _idxTextBox
			 ,
			    _scplat
			 ,
			    _scDateDataBinder
			 ,
			    _scSimpleDataBinder
			 ,
			    _scBaseUtils
){
return _dojodeclare("extn.order.create.additems.GetOrganizationListExtnUI",
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
	  eventId: 'afterScreenInit'

,	  sequence: '51'




,handler : {
methodName : "extn_afterScreenInit"

 
}
}

]
}

});
});


