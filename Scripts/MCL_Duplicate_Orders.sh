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
LOGFILE1=$LOGDIR/MCLDuplicateOrders.txt
#LOGFILE2=$LOGDIR/MonitorOrderResults2.txt
TO_ADDRS="oms@vitaminshoppe.com theresa.graham@vitaminshoppe.com supplychainsystems@vitaminshoppe.com DTCAZ@vitaminshoppe.com"
TO_ADDRS_OMS="oms@vitaminshoppe.com"
#TO_ADDRS_ON_FAILURE="shivraj.ugale@vitaminshoppe.com shivraj.ugale@perficient.com"
#TO_ADDRS="shivraj.ugale@vitaminshoppe.com"
export LOGDIR ARCDIR PATH ORACLE_HOME USER PWD TO_ADDRS TO_ADDRS_ON_FAILURE TO_ADDRS_OMS

orderCount=$(sqlplus -silent /nolog <<END
whenever sqlerror exit 1;
whenever oserror exit 1;
set pagesize 1000
set feedback off
set heading off
connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=lin-vsi-omspdb3)(PORT= 1881)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=OMSPROD)))"
SELECT COUNT(DISTINCT(ORDER_NO)) AS ORDER_COUNT
FROM STERLING.YFS_ORDER_HEADER WHERE  
SUBSTR(ORDER_DATE,1,9) = SUBSTR(SYSDATE,1,9) 
AND ORDER_NO LIKE 'M%'
AND CUSTOMER_PO_NO IN (
SELECT CUSTOMER_PO_NO
FROM STERLING.YFS_ORDER_HEADER 
WHERE 
SUBSTR(CREATETS,1,9) = SUBSTR(SYSDATE,1,9)
AND ORDER_NO LIKE 'M%'
GROUP BY CUSTOMER_PO_NO,ENTERPRISE_KEY
HAVING COUNT(*) > '1')
ORDER BY CUSTOMER_PO_NO, ORDER_DATE DESC;
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
SELECT ORDER_NO, CUSTOMER_PO_NO, ORDER_DATE 
FROM STERLING.YFS_ORDER_HEADER WHERE
SUBSTR(ORDER_DATE,1,9) = SUBSTR(SYSDATE,1,9) 
AND ORDER_NO LIKE 'M%'
AND CUSTOMER_PO_NO IN (
SELECT CUSTOMER_PO_NO
FROM STERLING.YFS_ORDER_HEADER 
WHERE 
SUBSTR(CREATETS,1,9) = SUBSTR(SYSDATE,1,9)
AND ORDER_NO LIKE 'M%'
GROUP BY CUSTOMER_PO_NO,ENTERPRISE_KEY
HAVING COUNT(*) > '1')
ORDER BY CUSTOMER_PO_NO, ORDER_DATE DESC;
spool off
exit 0
END

order_msg=$(cat $LOGFILE1)

if [ $? -ne 0 ]
then
echo "****Error in the process****"
DT=`date +%h_%d_%Y`
TM=`date +%H:%M:%S`
#echo "${DT}_${TM}"|mailx -s "Error running OMS orders stuck in Created status script" $TO_ADDRS_ON_FAILURE
echo -e "Operators,\n\nPlease check connectivity to OMS servers by logging to App Console 1 and 2. If not able to connect call DB ADMIN.\nDuring weekend and off-hours, also call OMS team (follow OMS escalation).\n\nThanks."|mailx -r "oms_auto_notification@vitaminshoppe.com" -s "Error connecting to OMS Prod DB" $TO_ADDRS_ON_FAILURE
elif [ $orderCount -eq 0 ]
then
echo -e "This morning’s query displayed no duplicate Market Place Orders in OMS PROD." | mailx -r "oms_auto_notification@vitaminshoppe.com" -s "No duplicate Market Place Orders in OMS PROD.Count:$orderCount" $TO_ADDRS_OMS 
elif [ $orderCount -gt 0 ]
then
echo -e "OMS Support, \n\nList of duplicate Market Place Orders in OMS PROD.\n\nPlease work with WMS team to cancel only the DUPLICATE order.\n\n$order_msg"|mailx -r "oms_auto_notification@vitaminshoppe.com" -s "Duplicate MCL Orders in OMS PROD. Count:$orderCount" $TO_ADDRS
fi