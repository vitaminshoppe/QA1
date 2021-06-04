
scDefine(["dojo/text!./templates/AdvancedSearchExtn.html","scbase/loader!dojo/_base/declare","scbase/loader!dojo/_base/kernel","scbase/loader!dojo/_base/lang","scbase/loader!dojo/text","scbase/loader!idx/form/TextBox","scbase/loader!sc/plat","scbase/loader!sc/plat/dojo/binding/SimpleDataBinder","scbase/loader!sc/plat/dojo/utils/BaseUtils"]
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
			    _idxTextBox
			 ,
			    _scplat
			 ,
			    _scSimpleDataBinder
			 ,
			    _scBaseUtils
){
return _dojodeclare("extn.mobile.home.search.AdvancedSearchExtnUI",
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
	  eventId: 'extn_searchLastName_onKeyUp'

,	  sequence: '51'




,handler : {
methodName : "searchOrdersOnEnter"

 
}
}
,
{
	  eventId: 'extn_searchFirstName_onKeyUp'

,	  sequence: '51'




,handler : {
methodName : "searchOrdersOnEnter"

 
}
}

]
}

});
});


