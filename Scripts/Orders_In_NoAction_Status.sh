#!/bin/ksh
#
#su - shivraj.ugale -c /home/OMSAutomationScripts/Missing_Credit_Card_Expiry_Date.sh

ORACLE_HOME=/u01/app/oracle/product/11.2.0/db
LD_LIBRARY_PATH=$ORACLE_HOME/lib:$ORACLE_HOME/ctx/lib:$LD_LIBRARY_PATH
TNS_ADMIN=$ORACLE_HOME/network/admin
PATH=$ORACLE_HOME/bin:$PATH:.

#Change user, pswd and db to point to target database
USER=sterling_reader01
PWD=xpruhjJ3Px
DatabaseName=OMS_PROD

#st3rling0ms, xpruhjJ3Px, 10.3.51.167, 1881, OMSPROD_SVC
#connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=10.3.51.167)(PORT= 1881)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=OMSPROD_SVC)))"
#connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=10.3.53.74)(PORT= 1581)))(CONNECT_DATA=(SERVER=DEDICATED)(SID=omstest)))"

#ORACLE_HOME=/u01/app/oracle/product/11.2.0
#LD_LIBRARY_PATH=$ORACLE_HOME/lib:$ORACLE_HOME/ctx/lib:$LD_LIBRARY_PATH
#TNS_ADMIN=$ORACLE_HOME/network/admin
#PATH=$ORACLE_HOME/bin:$PATH:.

#Change user, pswd and db to point to target database
#USER=sterling
#PWD=st3rling0ms
#DatabaseName=QA

export PATH LD_LIBRARY_PATH TNS_ADMIN USER PWD ORACLE_HOME

LOGDIR=/home/OMSAutomationScripts

#DATASRC=/opt/web20/open/weber_order_status/production
#SHELLDIR=/export/home/vsadmin/scripts/weber_order_status/production
#SCRIPTNAME=`basename $0`

ARCDIR=$LOGDIR/arch
LOGFILE1=$LOGDIR/NoActionOrders.txt

#TO_ADDRS="omnicustomercare@vitaminshoppe.com oms@vitaminshoppe.com operators@vitaminshoppe.com SDTeam@vitaminshoppe.com"
#TO_ADDRS_ON_FAILURE = "omnicustomercare@vitaminshoppe.com oms@vitaminshoppe.com operators@vitaminshoppe.com ooyediran@vitaminshoppe.com UnixAdmins@vitaminshoppe.com"
TO_ADDRS="oms@vitaminshoppe.com"
TO_ADDRS_ON_FAILURE="oms@vitaminshoppe.com ooyediran@vitaminshoppe.com UnixAdmins@vitaminshoppe.com"
export LOGDIR ARCDIR PATH ORACLE_HOME USER PWD TO_ADDRS TO_ADDRS_ON_FAILURE

sqlplus -silent /nolog <<END
whenever sqlerror exit 1;
whenever oserror exit 1;
set pagesize 50000
set linesize 1000
set trimspool on
set feedback off
set heading off
connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=lin-vsi-omspdb3)(PORT= 1881)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=OMSPROD)))"
spool $LOGFILE1
ALTER SESSION SET nls_date_format = 'DD-MON-YY HH12:MI:SS PM';
--SELECT DISTINCT(TRIM(YOL.CUSTOMER_PO_NO)) AS ORDER_NO, TRIM(YOL.SHIPNODE_KEY) AS STORE#, TRIM(YOH.CREATETS) AS ORDER_DATE, (CONCAT(CONCAT(CONCAT(TRIM(YPI.CITY), ' '),CONCAT(TRIM(YPI.STATE),', ')), TRIM(YPI.ZIP_CODE))) AS CITY_STATE_ZIP, TRIM(YPI.DAY_PHONE) AS PHONE_NO
--SELECT DISTINCT(TRIM((YOL.CUSTOMER_PO_NO))||'	'||TRIM(YOH.CREATETS)||'		'||TRIM(ROUND(SYSDATE - YORS.STATUS_DATE,2)*24)||'		'||(CONCAT(CONCAT(CONCAT(TRIM(YPI.CITY), ' '),CONCAT(TRIM(YPI.STATE),'  -  ')), TRIM(YPI.DAY_PHONE))))) AS ORDER_DATA, TRIM(YOL.SHIPNODE_KEY) AS STORE_NO
SELECT DISTINCT(RTRIM(YOL.CUSTOMER_PO_NO)) AS ORDER_NO, TRIM(YOL.SHIPNODE_KEY) AS STORE#, TRIM(YOH.CREATETS)||'		    '||(CONCAT(CONCAT(CONCAT(TRIM(YPI.CITY), ' '),CONCAT(TRIM(YPI.STATE),'  -  ')), TRIM(YPI.DAY_PHONE))) AS ORDER_DATA 
FROM STERLING.YFS_INBOX YI, STERLING.YFS_ORDER_HEADER YOH,  STERLING.YFS_ORDER_LINE YOL, STERLING.YFS_ORDER_RELEASE_STATUS YORS, STERLING.YFS_STATUS YS, STERLING.YFS_ORGANIZATION YO, STERLING.YFS_PERSON_INFO YPI
WHERE YOH.ORDER_HEADER_KEY = YOL.ORDER_HEADER_KEY AND YOL.ORDER_LINE_KEY = YI.ORDER_LINE_KEY AND YOL.ORDER_LINE_KEY = YORS.ORDER_LINE_KEY AND YORS.STATUS = YS.STATUS
AND YPI.PERSON_INFO_KEY = YO.CONTACT_ADDRESS_KEY AND YO.ORGANIZATION_CODE = YOL.SHIPNODE_KEY
AND YOH.DRAFT_ORDER_FLAG = 'N' AND YS.PROCESS_TYPE_KEY = 'ORDER_FULFILLMENT' AND YORS.STATUS_QUANTITY > '0' AND YOH.ENTRY_TYPE IN ('POS','WEB','Call Center')
AND YOL.LINE_TYPE = 'PICK_IN_STORE' AND YS.DESCRIPTION IN ('No Action','Acknowledge')
AND YI.CREATETS >= TRUNC(SYSDATE-2,'DD')
AND EXCEPTION_TYPE = 'Hourly Monitor alert'
ORDER BY STORE# ASC;
spool off
exit 0
END

order_msg=$(cat $LOGFILE1)

orderCount=$(sqlplus -silent /nolog <<END
whenever sqlerror exit 1;
whenever oserror exit 1;
set pagesize 0
set feedback off
set heading off
connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=lin-vsi-omspdb3)(PORT= 1881)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=OMSPROD)))"
SELECT COUNT(DISTINCT(YOL.CUSTOMER_PO_NO)) AS C
FROM STERLING.YFS_INBOX YI, STERLING.YFS_ORDER_HEADER YOH,  STERLING.YFS_ORDER_LINE YOL, STERLING.YFS_ORDER_RELEASE_STATUS YORS, STERLING.YFS_STATUS YS
WHERE YOH.ORDER_HEADER_KEY = YOL.ORDER_HEADER_KEY AND YOL.ORDER_LINE_KEY = YI.ORDER_LINE_KEY AND YOL.ORDER_LINE_KEY = YORS.ORDER_LINE_KEY AND YORS.STATUS = YS.STATUS 
AND YOH.DRAFT_ORDER_FLAG = 'N' AND YS.PROCESS_TYPE_KEY = 'ORDER_FULFILLMENT' AND YORS.STATUS_QUANTITY > '0' AND YOH.ENTRY_TYPE IN ('POS','WEB','Call Center')
AND YOL.LINE_TYPE = 'PICK_IN_STORE' AND YS.DESCRIPTION IN ('No Action','Acknowledge')
AND YI.CREATETS >= TRUNC(SYSDATE-2,'DD')
AND EXCEPTION_TYPE = 'Hourly Monitor alert';
exit 0
END
)

echo $orderCount

if [ $? -ne 0 ]
then
echo "****Error in the process****"
DT=`date +%h_%d_%Y`
TM=`date +%H:%M:%S`
#echo "${DT}_${TM}"|mailx -s "Error running OMS orders in NCMP or Acknowledge status script" $TO_ADDRS_ON_FAILURE
echo -e "Operators,\n\nPlease check connectivity to OMS servers by logging to App Console 1 and 2. If not able to connect call DB_ADMIN.\nDuring weekend and off-hours, also call OMS team (follow OMS escalation).\n\nThanks."|mailx -r "oms_auto_notification@vitaminshoppe.com" -s "Error connecting to OMS Prod DB" $TO_ADDRS_ON_FAILURE


elif [ $orderCount -eq 0 ]
then
echo -e "Service Desk,\n\nThere are no orders pending to be picked by Stores.\n\nThank You" | mailx -r "oms_auto_notification@vitaminshoppe.com" -s "No orders pending store pick pack" $TO_ADDRS

elif [ $orderCount -gt 0 ]
then
echo -e "Service Desk,\n\nPlease contact the stores for processing the orders.\n\n$order_msg\n\n\nThank You"|mailx -r "oms_auto_notification@vitaminshoppe.com" -s "Orders to be PICKED by STORES. Count:$orderCount" $TO_ADDRS
fi

#elif [ $orderCount -le 5 ]
#then
#echo -e "OMS Support,\n\nOrders from OMS PROD.\n\nPlease look into them at the earliest.\n\n$order_msg"|mailx -r "oms_auto_notification@vitaminshoppe.com" -s "Orders stuck in Created status. Count:$orderCount" oms@vitaminshoppe.com