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

export PATH LD_LIBRARY_PATH TNS_ADMIN USER PWD DB ORACLE_HOME

LOGDIR=/home/OMSAutomationScripts

#DATASRC=/opt/web20/open/weber_order_status/production
#SHELLDIR=/export/home/vsadmin/scripts/weber_order_status/production
#SCRIPTNAME=`basename $0`

ARCDIR=$LOGDIR/Arch
LOGFILE1=$LOGDIR/UnitTestScript.txt
#LOGFILE2=$LOGDIR/MonitorOrderResults2.txt
TO_ADDRS="shivraj.ugale@vitaminshoppe.com"
TO_ADDRS_ON_FAILURE="shivraj.ugale@vitaminshoppe.com"
#TO_ADDRS="shivraj.ugale@vitaminshoppe.com"
export LOGDIR ARCDIR PATH ORACLE_HOME USER PWD TO_ADDRS TO_ADDRS_ON_FAILURE

echo "Entering DB Query for COUNT"
orderCount=$(sqlplus -silent /nolog <<END
whenever sqlerror exit 1;
whenever oserror exit 1;
set pagesize 1000
set feedback off
set heading off
connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=10.3.51.16)(PORT= 1881)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=OMSPROD_SVC)))"
SELECT COUNT(DISTINCT(YOH.ORDER_NO)) AS C
FROM STERLING.YFS_ORDER_RELEASE_STATUS YORS, STERLING.YFS_STATUS YS, STERLING.YFS_ORDER_HEADER YOH, STERLING.YFS_ORDER_LINE YOL
WHERE YORS.STATUS = YS.STATUS AND YOL.ORDER_LINE_KEY = YORS.ORDER_LINE_KEY AND YOH.ORDER_HEADER_KEY = YOL.ORDER_HEADER_KEY 
AND YORS.STATUS_QUANTITY > '0' AND YOH.DRAFT_ORDER_FLAG = 'N' AND YS.PROCESS_TYPE_KEY = 'ORDER_FULFILLMENT'
AND YOH.ORDER_HEADER_KEY > '2016120710';
exit 0
END
)

echo "Entering DB Query for ROWS & COLUMN Info"
sqlplus -silent /nolog <<END
whenever sqlerror exit 1;
whenever oserror exit 1;
set pagesize 50000
set linesize 1000
set trimspool on
set feedback off
set heading on
connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=10.3.51.16)(PORT= 1881)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=OMSPROD_SVC)))"
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
AND YOH.ORDER_HEADER_KEY > '2016120710'
ORDER BY YOH.ORDER_DATE ASC;
spool off
exit 0
END

order_msg=$(cat $LOGFILE1)

echo "Entering eMail Logic"

if [ $? -ne 0 ]
then
echo "****Error in the process****"
DT=`date +%h_%d_%Y`
TM=`date +%H:%M:%S`
#echo "${DT}_${TM}"|mailx -s "Error running OMS orders stuck in Created status script" $TO_ADDRS_ON_FAILURE
echo -e "Operators,\n\nPlease check connectivity to OMS servers by logging to App Console 1 and 2. If not able to connect call DB ADMIN.\nDuring weekend and off-hours, also call OMS team (follow OMS escalation).\n\nThanks."|mailx -r "oms_auto_notification@vitaminshoppe.com" -s "Error connecting to OMS Prod DB" $TO_ADDRS_ON_FAILURE
elif [ $orderCount -eq 0 ]
then
echo -e "This morning’s query displayed no orders stuck in Created status." | mailx -r "oms_auto_notification@vitaminshoppe.com" -s "No orders stuck in Created status.Count:$orderCount" oms@vitaminshoppe.com
elif [ $orderCount -gt 0 ]
then
echo -e "Orders from OMS PROD.\n\nPlease follow OMS escalation chart if there are more than 5 orders below during WEEKENDS, HOLIDAYS or AFTER OFFICE HOURS.\n\n$order_msg"|mailx -r "oms_auto_notification@vitaminshoppe.com" -s "Orders stuck in Created status. Count:$orderCount" $TO_ADDRS
fi
#elif [ $orderCount -le 5 ]
#then
#echo -e "OMS Support,\n\nOrders from OMS PROD.\n\nPlease look into them at the earliest.\n\n$order_msg"|mailx -r "oms_auto_notification@vitaminshoppe.com" -s "Orders stuck in Created status. Count:$orderCount" oms@vitaminshoppe.com