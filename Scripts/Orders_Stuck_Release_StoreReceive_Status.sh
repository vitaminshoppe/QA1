#!/bin/ksh
#
#su -c /home/OMSAutomationScripts/Missing_Credit_Card_Expiry_Date.sh

ORACLE_HOME=/u01/app/oracle/product/11.2.0/db
LD_LIBRARY_PATH=$ORACLE_HOME/lib:$ORACLE_HOME/ctx/lib:$LD_LIBRARY_PATH
TNS_ADMIN=$ORACLE_HOME/network/admin
PATH=$ORACLE_HOME/bin:$PATH:.

#Change user, pswd and db to point to target database

USER=sterling_reader01
PWD=xpruhjJ3Px
DatabaseName=OMS_PROD

#st3rling0ms, xpruhjJ3Px, 10.3.51.167, 1881, OMSPROD_SVC

export PATH LD_LIBRARY_PATH TNS_ADMIN USER PWD DB ORACLE_HOME

LOGDIR=/home/OMSAutomationScripts

#DATASRC=/opt/web20/open/weber_order_status/production
#SHELLDIR=/export/home/vsadmin/scripts/weber_order_status/production
#SCRIPTNAME=`basename $0`
#LOGFILE2=$LOGDIR/Rele`ase_StoreReceive2.txt

ARCDIR=$LOGDIR/arch
LOGFILE1=$LOGDIR/Release_StoreReceive.txt
#TO_ADDRS="omnicustomercare@vitaminshoppe.com dpadilla@vitaminshoppe.com oms@vitaminshoppe.com helpdesk@vitaminshoppe.com"
TO_ADDRS="oms@vitaminshoppe.com operators@vitaminshoppe.com"
TO_ADDRS_ON_FAILURE = "oms@vitaminshoppe.com"

export LOGDIR ARCDIR PATH ORACLE_HOME USER PWD TO_ADDRS

sqlplus -silent /nolog <<END
whenever sqlerror exit 1;
whenever oserror exit 1;
set pagesize 50000
set linesize 1000
set trimspool on
set feedback off
set heading on
connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=10.3.51.167)(PORT= 1881)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=OMSPROD_SVC)))"
spool $LOGFILE1
SELECT TRIM(YOH.ORDER_DATE) AS ORDER_DATE, TRIM(YOH.ORDER_NO) AS ORDER_NO, 
CASE 
WHEN TRIM(YS.DESCRIPTION) = 'Procurement Transfer Order Created' THEN 'ProcTOCreated' 
WHEN TRIM(YS.DESCRIPTION) = 'JDA STS Acknowledged' THEN 'JDA STS Ack'
ELSE TRIM(YS.DESCRIPTION) END AS "STATUS",TRIM(ROUND(SYSDATE - YORS.STATUS_DATE,3)*24*60) AS AGED_MINS
FROM STERLING.YFS_ORDER_RELEASE_STATUS YORS, STERLING.YFS_STATUS YS, STERLING.YFS_ORDER_HEADER YOH, STERLING.YFS_ORDER_LINE YOL, STERLING.YFS_PERSON_INFO YPI
WHERE YORS.STATUS = YS.STATUS
AND YOL.ORDER_LINE_KEY = YORS.ORDER_LINE_KEY
AND YOH.ORDER_HEADER_KEY = YOL.ORDER_HEADER_KEY
AND YPI.PERSON_INFO_KEY = YOH.BILL_TO_KEY 
AND YORS.STATUS_QUANTITY > '0' --- Do not change this where clause
AND YOH.DRAFT_ORDER_FLAG = 'N'
AND YS.PROCESS_TYPE_KEY = 'ORDER_FULFILLMENT' --- Do not change this where clause
AND YORS.STATUS IN ('3200','2160.400','2160')
AND SYSDATE - YORS.STATUS_DATE > '0.030' 
AND YOH.ORDER_NO NOT LIKE 'Y1%'
AND YOH.ORDER_DATE > SYSDATE - 30
ORDER BY YOH.ORDER_DATE ASC;
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
connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=10.3.51.167)(PORT= 1881)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=OMSPROD_SVC)))"
SELECT COUNT(DISTINCT(YOH.ORDER_NO)) AS C
FROM STERLING.YFS_ORDER_RELEASE_STATUS YORS, STERLING.YFS_STATUS YS, STERLING.YFS_ORDER_HEADER YOH, STERLING.YFS_ORDER_LINE YOL
WHERE YORS.STATUS = YS.STATUS AND YOL.ORDER_LINE_KEY = YORS.ORDER_LINE_KEY AND YOH.ORDER_HEADER_KEY = YOL.ORDER_HEADER_KEY 
AND YORS.STATUS_QUANTITY > '0' AND YOH.DRAFT_ORDER_FLAG = 'N' AND YS.PROCESS_TYPE_KEY = 'ORDER_FULFILLMENT'
AND YORS.STATUS IN ('3200','2160.400','2160')
AND SYSDATE - YORS.STATUS_DATE > '0.030' 
AND YOH.ORDER_NO NOT LIKE 'Y1%'
AND YOH.ORDER_DATE > SYSDATE - 30;
exit 0
END
)

if [ $? -ne 0 ]
then
echo "****Error in the process****"
DT=`date +%h_%d_%Y`
TM=`date +%H:%M:%S`
#echo "${DT}_${TM}"|mailx -s "Error running Orders_Stuck_Release_StoreReceive_Status script" $TO_ADDRS
echo -e "Error running Orders_Stuck_Release_StoreReceive_Status script. Please contact OMS Support.\n\nThanks."|mailx -r "oms_auto_notification@vitaminshoppe.com" -s "Error connecting to OMS Prod DB" $TO_ADDRS_ON_FAILURE
elif [ $orderCount -gt 0 ]
then
echo -e "OMS Support,\n\nOrders from OMS PROD stuck in Released or Store Received status.\n\nPlease look into them at the earliest.\n\n$order_msg"|mailx -r "oms_auto_notification@vitaminshoppe.com" -s "Orders stuck in Release or Store Received status. Count:$orderCount" $TO_ADDRS
fi