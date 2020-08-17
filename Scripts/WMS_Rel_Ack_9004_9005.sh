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
LOGFILE1=$LOGDIR/WMS_Rel_Ack_9004.txt
LOGFILE2=$LOGDIR/WMS_Rel_Ack_9005.txt
#LOGFILE2=$LOGDIR/MonitorOrderResults2.txt
OMS_ADDRS="oms@vitaminshoppe.com"
VA_ADDRS="supplychainsystems@vitaminshoppe.com SupplyChainSolutionsGroup@vitaminshoppe.com ESBSupportTeam@vitaminshoppe.com oms@vitaminshoppe.com"
AZ_ADDRS="supplychainsystems@vitaminshoppe.com SupplyChainSolutionsGroup@vitaminshoppe.com ESBSupportTeam@vitaminshoppe.com oms@vitaminshoppe.com"
TO_ADDRS_ON_FAILURE="oms@vitaminshoppe.com omsdba@vitaminshoppe.com operators@vitaminshoppe.com"
#VA_ADDRS="shivraj.ugale@vitaminshoppe.com"

export LOGDIR ARCDIR PATH ORACLE_HOME USER PWD VA_ADDRS AZ_ADDRS TO_ADDRS_ON_FAILURE OMS_ADDRS

orderCount1=$(sqlplus -silent /nolog <<END
whenever sqlerror exit 1;
whenever oserror exit 1;
set pagesize 1000
set feedback off
set heading off
connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=lin-vsi-omspdb3)(PORT= 1881)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=OMSPROD)))"
SELECT COUNT(DISTINCT(YOH.ORDER_NO)) AS C
FROM STERLING.YFS_ORDER_HEADER YOH, STERLING.YFS_ORDER_LINE YOL, STERLING.YFS_ORDER_RELEASE YOR, STERLING.YFS_ORDER_RELEASE_STATUS YORS
WHERE YOH.ORDER_HEADER_KEY=YOL.ORDER_HEADER_KEY AND YOH.ORDER_HEADER_KEY=YOR.ORDER_HEADER_KEY 
AND YOL.ORDER_LINE_KEY = YORS.ORDER_LINE_KEY AND YOR.ORDER_RELEASE_KEY = YORS.ORDER_RELEASE_KEY
AND YORS.STATUS_QUANTITY > '0'
AND YOH.ORDER_NO NOT LIKE 'Y1%'
AND YOR.EXTN_JDA_TRN_NBR IS NOT NULL
AND YORS.STATUS IN ('3200.600')
AND SYSDATE - YORS.STATUS_DATE > '0.041' 
AND YOR.SHIPNODE_KEY = '9004'
AND YOR.ORDER_RELEASE_KEY > TO_CHAR(SYSDATE - 30,'YYYYMMDD');
exit 0
END
)

sqlplus -silent /nolog <<END
whenever sqlerror exit 1;
whenever oserror exit 1;
set pagesize 50000
set linesize 1000
set trimspool on
set feedback off
set heading on
connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=lin-vsi-omspdb3)(PORT= 1881)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=OMSPROD)))"
spool $LOGFILE1
ALTER SESSION SET nls_date_format = 'DD-MON-RR HH24:MI:SS'; 
SELECT DISTINCT(TRIM((SHIP_ADVICE_NO))) AS WMS_ORDER, TRIM(YOR.EXTN_JDA_TRN_NBR) AS JDA_TRN_NBR, TRIM(YOH.ORDER_DATE) AS ORDER_DATE, YORS.STATUS_DATE AS RELEASE_TO_WMS, '   ', TRIM(YOR.ENTERPRISE_KEY) AS ENTP_KEY, TRIM(ROUND(SYSDATE - YORS.STATUS_DATE,3)*24*60) AS AGED_MINS
FROM STERLING.YFS_ORDER_HEADER YOH, STERLING.YFS_ORDER_LINE YOL, STERLING.YFS_ORDER_RELEASE YOR, STERLING.YFS_ORDER_RELEASE_STATUS YORS
WHERE YOH.ORDER_HEADER_KEY=YOL.ORDER_HEADER_KEY AND YOH.ORDER_HEADER_KEY=YOR.ORDER_HEADER_KEY 
AND YOL.ORDER_LINE_KEY = YORS.ORDER_LINE_KEY AND YOR.ORDER_RELEASE_KEY = YORS.ORDER_RELEASE_KEY
AND YORS.STATUS_QUANTITY > '0'
AND YOH.ORDER_NO NOT LIKE 'Y1%'
AND YOR.EXTN_JDA_TRN_NBR IS NOT NULL
AND YORS.STATUS IN ('3200.600')
AND SYSDATE - YORS.STATUS_DATE > '0.041' 
AND YOR.SHIPNODE_KEY = '9004'
AND YOR.ORDER_RELEASE_KEY > TO_CHAR(SYSDATE - 30,'YYYYMMDD')
ORDER BY YORS.STATUS_DATE ASC;
spool off
exit 0
END

order_msg1=$(cat $LOGFILE1)

orderCount2=$(sqlplus -silent /nolog <<END
whenever sqlerror exit 1;
whenever oserror exit 1;
set pagesize 1000
set feedback off
set heading off
connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=lin-vsi-omspdb3)(PORT= 1881)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=OMSPROD)))"
SELECT COUNT(DISTINCT(YOH.ORDER_NO)) AS C
FROM STERLING.YFS_ORDER_HEADER YOH, STERLING.YFS_ORDER_LINE YOL, STERLING.YFS_ORDER_RELEASE YOR, STERLING.YFS_ORDER_RELEASE_STATUS YORS
WHERE YOH.ORDER_HEADER_KEY=YOL.ORDER_HEADER_KEY AND YOH.ORDER_HEADER_KEY=YOR.ORDER_HEADER_KEY 
AND YOL.ORDER_LINE_KEY = YORS.ORDER_LINE_KEY AND YOR.ORDER_RELEASE_KEY = YORS.ORDER_RELEASE_KEY
AND YORS.STATUS_QUANTITY > '0'
AND YOH.ORDER_NO NOT LIKE 'Y1%'
AND YOR.EXTN_JDA_TRN_NBR IS NOT NULL
AND YORS.STATUS IN ('3200.600')
AND SYSDATE - YORS.STATUS_DATE > '0.041' 
AND YOR.SHIPNODE_KEY = '9005'
AND YOR.ORDER_RELEASE_KEY > TO_CHAR(SYSDATE - 30,'YYYYMMDD');
exit 0
END
)

sqlplus -silent /nolog <<END
whenever sqlerror exit 1;
whenever oserror exit 1;
set pagesize 50000
set linesize 1000
set trimspool on
set feedback off
set heading on
connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=lin-vsi-omspdb3)(PORT= 1881)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=OMSPROD)))"
spool $LOGFILE2
ALTER SESSION SET nls_date_format = 'DD-MON-RR HH24:MI:SS'; 
SELECT DISTINCT(TRIM((SHIP_ADVICE_NO))) AS WMS_ORDER, TRIM(YOR.EXTN_JDA_TRN_NBR) AS JDA_TRN_NBR, TRIM(YOH.ORDER_DATE) AS ORDER_DATE, YORS.STATUS_DATE AS RELEASE_TO_WMS, '   ', TRIM(YOR.ENTERPRISE_KEY) AS ENTP_KEY, TRIM(ROUND(SYSDATE - YORS.STATUS_DATE,3)*24*60) AS AGED_MINS
FROM STERLING.YFS_ORDER_HEADER YOH, STERLING.YFS_ORDER_LINE YOL, STERLING.YFS_ORDER_RELEASE YOR, STERLING.YFS_ORDER_RELEASE_STATUS YORS
WHERE YOH.ORDER_HEADER_KEY=YOL.ORDER_HEADER_KEY AND YOH.ORDER_HEADER_KEY=YOR.ORDER_HEADER_KEY 
AND YOL.ORDER_LINE_KEY = YORS.ORDER_LINE_KEY AND YOR.ORDER_RELEASE_KEY = YORS.ORDER_RELEASE_KEY
AND YORS.STATUS_QUANTITY > '0'
AND YOH.ORDER_NO NOT LIKE 'Y1%'
AND YOR.EXTN_JDA_TRN_NBR IS NOT NULL
AND YORS.STATUS IN ('3200.600')
AND SYSDATE - YORS.STATUS_DATE > '0.041' 
AND YOR.SHIPNODE_KEY = '9005'
AND YOR.ORDER_RELEASE_KEY > TO_CHAR(SYSDATE - 30,'YYYYMMDD')
ORDER BY YORS.STATUS_DATE ASC;
spool off
exit 0
END

order_msg2=$(cat $LOGFILE2)

if [ $? -ne 0 ]
then
echo "****Error in the process****"
DT=`date +%h_%d_%Y`
TM=`date +%H:%M:%S`
#echo "${DT}_${TM}"|mailx -s "Error running OMS orders stuck in Created status script" $TO_ADDRS_ON_FAILURE
echo -e "Operators,\n\nPlease check connectivity to OMS servers by logging to App Console 1 and 2. If not able to connect call DB ADMIN.\nDuring weekend and off-hours, also call OMS team (follow OMS escalation).\n\nThanks."|mailx -r "oms_auto_notification@vitaminshoppe.com" -s "Error connecting to OMS Prod DB" $TO_ADDRS_ON_FAILURE
elif [ $orderCount1 -eq 0 ]
then
echo -e "No orders are waiting to get acknowledged." | mailx -r "oms_auto_notification@vitaminshoppe.com" -s "No orders waiting to be acknowledged.Count:$orderCount1" $OMS_ADDRS
elif [ $orderCount1 -gt 0 ]
then
echo -e "VA DC,\n\n Please look into the below list of orders sent to 9004 but not yet acknowledged.\n\nScript Name:WMS_Rel_Ack_9004_9005.sh \nCriteria: Orders older than 60 mins and upto 30 days\n\n$order_msg1"|mailx -r "oms_auto_notification@vitaminshoppe.com" -s "STH Orders released to 9004 not Ack after 1 hr. Count:$orderCount1" $VA_ADDRS

echo -e "AZ DC,\n\n Please look into the below list of orders sent to 9005 but not yet acknowledged.\n\nScript Name:WMS_Rel_Ack_9004_9005.sh \nCriteria: Orders older than 60 mins and upto 30 days\n\n$order_msg2"|mailx -r "oms_auto_notification@vitaminshoppe.com" -s "STH Orders released to 9005 not Ack after 1 hr. Count:$orderCount2" $AZ_ADDRS
fi
