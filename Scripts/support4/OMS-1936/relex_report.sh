#!/bin/ksh
#
#
#CURRENT_DIR="/home/OMSAutomationScripts/support4/relexscript/"
CURRENT_DIR="/home/OMSAutomationScripts/support4/OMS-1936"
cd  $CURRENT_DIR


ORACLE_HOME=/u01/app/oracle/product/11.2.0/db
LD_LIBRARY_PATH=$ORACLE_HOME/lib:$ORACLE_HOME/ctx/lib:$LD_LIBRARY_PATH
TNS_ADMIN=$ORACLE_HOME/network/admin
PATH=$ORACLE_HOME/bin:$PATH:.


#RUN_SCRIPT (Value should be QA or PROD)

RUN_SCRIPT=PROD


if [ $RUN_SCRIPT == "PROD" ] 
then    	
	USER=sterling_reader01
	PWD=xpruhjJ3Px
	DatabaseName=OMS_PROD
	HOST_NAME=lin-vsi-omspdb3
	PORT=1881
	SERVICE_SID="(SERVICE_NAME=OMSPROD)"	
	#st3rling0ms, xpruhjJ3Px, lin-vsi-omspdb3, 1881, OMSPROD_SVC
else

	ORACLE_HOME=/u01/app/oracle/product/11.2.0/
	LD_LIBRARY_PATH=$ORACLE_HOME/lib:$ORACLE_HOME/ctx/lib:$LD_LIBRARY_PATH
	TNS_ADMIN=$ORACLE_HOME/network/admin
	PATH=$ORACLE_HOME/bin:$PATH:.

    USER=sterling
	PWD=st34rl1ng0ms
	DatabaseName=omstest
	HOST_NAME=10.3.53.74
	PORT=1581
	SERVICE_SID="(SID=omstest)"	
fi


#st3rling0ms, xpruhjJ3Px, 10.3.51.167, 1881, OMSPROD_SVC

export PATH LD_LIBRARY_PATH TNS_ADMIN USER PWD DB ORACLE_HOME

#LOGDIR=/home/OMSAutomationScripts
#LOGDIR=/home/OMSAutomationScripts/support4/relexscript
LOGDIR=$CURRENT_DIR


#DATASRC=/opt/web20/open/weber_order_status/production
#SHELLDIR=/export/home/vsadmin/scripts/weber_order_status/production
#SCRIPTNAME=`basename $0`

#ARCDIR=$LOGDIR/Arch
LOGFILE1=$LOGDIR/relex_data.csv
#ZIPFILE1=$LOGDIR/relex_data.zip
#LOGFILE2=$LOGDIR/MonitorOrderResults2.txt
FROM_ADDRS="oms_auto_notification@vitaminshoppe.com"
TO_ADDRS="OMS@vitaminshoppe.com"
TO_ADDRS_ON_FAILURE="OMS@vitaminshoppe.com"

export LOGDIR ARCDIR PATH ORACLE_HOME USER PWD TO_ADDRS TO_ADDRS_ON_FAILURE ZIPFILE1 LOGFILE1

#connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=lin-vsi-omspdb3)(PORT= 1881)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=OMSPROD)))"
#connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=10.3.51.167)(PORT= 1881)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=OMSPROD_SVC)))"

sqlplus -silent /nolog <<END
whenever sqlerror exit 1;
whenever oserror exit 1;
set pagesize 0
set linesize 3000
set trimspool on
set colsep ,
set heading off
set feedback off
connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=$HOST_NAME)(PORT= $PORT)))(CONNECT_DATA=(SERVER=DEDICATED)$SERVICE_SID))"
spool $LOGFILE1
ALTER SESSION SET nls_date_format = 'DD-MON-RR';
SELECT  YOH.ORDER_NO As Order_Number,
YOH.ORDER_DATE As Order_Date,
YOH.createts As Order_createts,
YOH.ENTERPRISE_KEY,
YOH.ORDER_TYPE,
CASE 
WHEN YOH.DOCUMENT_TYPE= '0001'   THEN 'Sales' 
WHEN YOH.DOCUMENT_TYPE= '0003' THEN 'Return'
END AS "Sale_Type",
CASE 
WHEN YOL.LINE_TYPE= 'PICK_IN_STORE'   THEN YOL.SHIPNODE_KEY 
WHEN YOL.LINE_TYPE= 'SHIP_TO_HOME' THEN yoh.entered_by
WHEN YOH.ORDER_TYPE= 'Marketplace'  AND YOH.DOCUMENT_TYPE= '0003' AND ENTERPRISE_KEY='VS_AMAZON'  THEN '6401'
WHEN YOH.ORDER_TYPE= 'Marketplace'  AND YOH.DOCUMENT_TYPE= '0003' AND ENTERPRISE_KEY='EBAY'  THEN '6403'
WHEN YOH.ORDER_TYPE= 'Marketplace'  AND YOH.DOCUMENT_TYPE= '0003' AND ENTERPRISE_KEY='WALMART'  THEN '6407'
WHEN YOH.ORDER_TYPE= 'Marketplace'  AND YOH.DOCUMENT_TYPE= '0003' AND ENTERPRISE_KEY='JET'  THEN '6408'
WHEN YOH.ORDER_TYPE= 'Marketplace'  AND YOH.DOCUMENT_TYPE= '0003' AND ENTERPRISE_KEY='NUTRITION_DEPOT'  THEN '6402'
WHEN YOH.ORDER_TYPE= 'Marketplace'  AND YOH.DOCUMENT_TYPE= '0003' AND ENTERPRISE_KEY='GOOGLE_EXPRESS'  THEN '6409'
WHEN YOH.ORDER_TYPE= 'WEB'  AND YOH.DOCUMENT_TYPE= '0003'  THEN '6101'
END AS "Store_Number",
YOL.ITEM_ID As SKU,
YOL.EXTN_ACT_ITEM_ID As ATG_SKU,
YOL.ORDERED_QTY AS ORDERED_QTY,
YOL.ORIGINAL_ORDERED_QTY AS ORIGINAL_ORDERED_QTY,
YOL.LINE_TYPE,
YFULLFILL.Expected_fulfill,
YFULLFILL.Actual_fulfill,
NVL(YLC.CHARGEAMOUNT ,0) AS DISCOUNT
--(YLC.CHARGEAMOUNT) AS DISCOUNT
, (YOL.unit_price*ORDERED_QTY) AS total_price,
--((YOL.unit_price*ORDERED_QTY) - YLC.CHARGEAMOUNT) AS retail_price
((YOL.unit_price*ORDERED_QTY) - NVL(YLC.CHARGEAMOUNT ,0)) AS retail_price
from 
sterling.yfs_order_header YOH
INNER JOIN (SELECT ORDER_HEADER_KEY,ITEM_ID,ORDERED_QTY,ORIGINAL_ORDERED_QTY,LINE_TYPE,ORDER_LINE_KEY,UNIT_PRICE,EXTN_ACT_ITEM_ID,SHIPNODE_KEY
            FROM sterling.YFS_ORDER_LINE
            WHERE  LINE_TYPE not in ('SHIP_TO_STORE','PICK_IN_STORE')
            ) YOL
     ON YOL.ORDER_HEADER_KEY = YOH.ORDER_HEADER_KEY 
INNER JOIN (SELECT ors.ORDER_HEADER_KEY,
			CASE 
			WHEN yrd.region_key = '201808100302192786799675'  OR yrd.region_key='201808100306482786799678'  THEN '9005'
			WHEN yrd.region_key = '201808100300412786799672'  OR yrd.region_key='201808100308282786799681'  THEN '9004'
			END AS Expected_fulfill,
			ors.shipnode_key AS Actual_fulfill,
			yrd.from_zip,
			yps.zip_code
            FROM sterling.yfs_order_release ors,sterling.yfs_person_info yps,sterling.yfs_region_detail yrd 
            WHERE  ors.ship_to_key=yps.person_info_key and ors.shipnode_key in ('9005','9004')
			and yrd.from_zip=yps.zip_code
			--and (yrd.region_key = '201808100302192786799675' or yrd.region_key='201808100306482786799678')  --9005
            --and (yrd.region_key = '201808100300412786799672' or yrd.region_key='201808100308282786799681')  --9004
            and yrd.region_key in ('201808100302192786799675','201808100306482786799678','201808100300412786799672','201808100308282786799681')
            ) YFULLFILL
     ON YFULLFILL.ORDER_HEADER_KEY = YOH.ORDER_HEADER_KEY 	 
Left JOIN (SELECT HEADER_KEY AS HEADER_KEY,LINE_KEY , SUM(CHARGEAMOUNT) AS CHARGEAMOUNT
            FROM sterling.YFS_LINE_CHARGES  
            GROUP BY HEADER_KEY,LINE_KEY
            ) YLC
    ON YLC.LINE_KEY = YOL.ORDER_LINE_KEY
where 
--YOH.ORDER_HEADER_KEY > TO_CHAR(SYSDATE-1,'YYYYMMDD')
YOH.ORDER_HEADER_KEY like ''||TO_CHAR(SYSDATE-1,'YYYYMMDD')||'%'
and YOH.DOCUMENT_TYPE in ('0001','0003')
and YOH.ORDER_TYPE not in ('POS')
and YOH.DRAFT_ORDER_FLAG  = 'N'
ORDER BY YOH.CREATETS , YOH.ORDER_NO
;
spool off
exit 0
END

order_msg=$(cat $LOGFILE1)

#zip DiscountReport.zip DiscountReport.txt

if [ $? -ne 0 ]
then
echo "****Error in the process****"
DT=`date +%h_%d_%Y`
TM=`date +%H:%M:%S`
#echo "${DT}_${TM}"|mailx -s "Error running CSR Discount and Appeasement report script" $TO_ADDRS_ON_FAILURE
echo -e "Error running RELEX report script. Please contact OMS Support.\n\nThanks."|mailx -r $FROM_ADDRS -s "Error connecting to OMS Prod DB" $TO_ADDRS_ON_FAILURE
else
REPORTDATE=`date +%Y_%m_%d`
DATADATE=`date -d '-1 day' '+%Y_%m_%d'`
echo -e "Hello,\n\nPFA RELEX report. Report contains orders of date $DATADATE \nThanks"|mailx -r $FROM_ADDRS -s "RELEX Report $REPORTDATE" -a $LOGFILE1 $TO_ADDRS
fi