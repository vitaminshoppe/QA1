
scDefine(["dojo/text!./templates/OrderEditorRTExtn.html","scbase/loader!dojo/_base/declare","scbase/loader!dojo/_base/kernel","scbase/loader!dojo/_base/lang","scbase/loader!dojo/text","scbase/loader!idx/layout/TitlePane","scbase/loader!sc/plat","scbase/loader!sc/plat/dojo/binding/CurrencyDataBinder","scbase/loader!sc/plat/dojo/utils/BaseUtils","scbase/loader!sc/plat/dojo/widgets/Link"]
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
			    _idxTitlePane
			 ,
			    _scplat
			 ,
			    _scCurrencyDataBinder
			 ,
			    _scBaseUtils
			 ,
			    _scLink
){
return _dojodeclare("extn.editors.OrderEditorRTExtnUI",
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
methodName : "restrictAddLinesForShippedOrder"

 
}
}
,
{
	  eventId: 'afterScreenInit'

,	  sequence: '52'




,handler : {
methodName : "extn_afterScreenInit"

 
}
}
,
{
	  eventId: 'extn_giftCardFulfillment_onClick'

,	  sequence: '51'

,	  description: 'fulfillGiftCard_onClick'



,handler : {
methodName : "fulfillGiftCard_onClick"

 
}
}
,
{
	  eventId: 'extn_link_updateBol_onClick'

,	  sequence: '51'




,handler : {
methodName : "extn_UpdateBOLNoOnClickHandler"

 
}
}
,
{
	  eventId: 'onExtnMashupCompletion'

,	  sequence: '51'




,handler : {
methodName : "onExtnMashupCompletion"

 
}
}
,
{
	  eventId: 'extn_link_resolve_wholesale_hold_onClick'

,	  sequence: '51'




,handler : {
methodName : "extn_OpenResolveWholesaleOrderPopup"

 
}
}

]
}

});
});


