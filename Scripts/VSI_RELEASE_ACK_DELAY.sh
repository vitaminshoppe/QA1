#!/bin/ksh

ORACLE_HOME=/u01/app/oracle/product/11.2.0/db
LD_LIBRARY_PATH=$ORACLE_HOME/lib:$ORACLE_HOME/ctx/lib:$LD_LIBRARY_PATH
TNS_ADMIN=$ORACLE_HOME/network/admin
PATH=$ORACLE_HOME/bin:$PATH:.


#Change user, pswd and db to point to target database

USER=sterling_reader01
PWD=xpruhjJ3Px
DatabaseName=OMS_PROD

export PATH LD_LIBRARY_PATH TNS_ADMIN USER PWD DB ORACLE_HOME

LOGDIR=/home/OMSAutomationScripts
ARCDIR=$LOGDIR/Arch
LOGFILE=$LOGDIR/VSI_RELEASE_ACK_DELAY.txt
#TO_ADDRS="oms@vitaminshoppe.com"
TO_ADDRS="oms@vitaminshoppe.com"
TO_ADDRS_ON_FAILURE = "oms@vitaminshoppe.com"
export LOGDIR ARCDIR PATH ORACLE_HOME USER PWD TO_ADDRS LOGFILE

# query the Order Header table and email the orders loaded

sqlplus -silent /nolog <<END
whenever sqlerror exit 1;
whenever oserror exit 1;
set pagesize 50000
set linesize 1000
set trimspool on
set feedback off
set heading on
connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=10.3.51.167)(PORT= 1881)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=OMSPROD_SVC)))"
spool $LOGFILE
ALTER SESSION SET nls_date_format = 'DD-MON-RR HH24:MI:SS';
SELECT
 TRIM(B.sales_Order_No)     AS SALES_ORDER_NO,
 TRIM(A.transfer_order_no)  AS TRANSFER_ORDER_NO,
 TRIM(A.SHIPNODE_KEY)  AS SHIPNODE_KEY,
 TRIM(A.RELEASE_NO)  AS RELEASE_NO,
 --TRIM(A.DESCRIPTION)        AS DESCRIPTION,
 TRIM(B.ORDER_DATE)       AS ORDER_DATE
FROM
 (SELECT oh.order_no transfer_order_no,
   yol.CHAINED_FROM_ORDER_HEADER_KEY chained_from_ohk,
   inx.EXCEPTION_TYPE,
   inx.DESCRIPTION,
   inx.GENERATED_ON,
   inx.createts,
   yor.SHIPNODE_KEY,
   yor.RELEASE_NO
 FROM STERLING.YFS_INBOX inx ,
   STERLING.YFS_ORDER_HEADER oh,
   STERLING.YFS_ORDER_LINE yol,
   STERLING.YFS_ORDER_RELEASE yor
 WHERE inx.ORDER_HEADER_KEY= OH.ORDER_HEADER_KEY
 AND inx.ORDER_HEADER_KEY  = yol.ORDER_HEADER_KEY
 AND OH.ORDER_HEADER_KEY   =yol.ORDER_HEADER_KEY
 AND yor.ORDER_HEADER_KEY= OH.ORDER_HEADER_KEY
 AND yor.ORDER_HEADER_KEY = yol.ORDER_HEADER_KEY
 AND yor.ORDER_HEADER_KEY=  inx.ORDER_HEADER_KEY
 AND inx.QUEUE_KEY         ='20170318143338321283'
 AND inx.active_flag       ='Y'
 )A
LEFT OUTER JOIN
 (SELECT order_no sales_order_no,
   ORDER_HEADER_KEY main_ohk,
   ORDER_DATE ORDER_DATE
 FROM Sterling.YFS_ORDER_HEADER
 ) B
ON A.chained_from_ohk = B.main_ohk
--ORDER BY A.GENERATED_ON DESC
GROUP BY B.sales_order_no, A.transfer_order_no, A.DESCRIPTION, B.ORDER_DATE, A.SHIPNODE_KEY, A.RELEASE_NO;
exit 0
END

order_msg=$(cat $LOGFILE)

RELEASE_ACK_Count=$(sqlplus -silentsilent /nolog <<END
whenever sqlerror exit 1;
whenever oserror exit 1;
set pagesize 0
set feedback off
set heading off
connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=10.3.51.167)(PORT= 1881)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=OMSPROD_SVC)))"
Select COUNT(DISTINCT(OH.ORDER_NO)) AS C
from STERLING.YFS_INBOX inx , STERLING.YFS_ORDER_HEADER oh, STERLING.YFS_ORDER_LINE  yol  where
inx.ORDER_HEADER_KEY= OH.ORDER_HEADER_KEY
AND inx.ORDER_HEADER_KEY= yol.ORDER_HEADER_KEY
AND OH.ORDER_HEADER_KEY =yol.ORDER_HEADER_KEY
and inx.QUEUE_KEY='20170318143338321283' 
AND inx.active_flag='Y';
exit 0
END
)


if [ $? -ne 0 ]
then
echo "****Error in the process****"
DT=`date +%h_%d_%Y`
TM=`date +%H:%M:%S`
#echo "${DT}_${TM}"|mailx -s "Error running Orders_VSI_RELEASE_ACK_DELAY script" $TO_ADDRS
echo -e "Error running VSI_RELEASE_ACK_DELAY script. Please contact OMS Support.\n\nThanks."|mailx -r "oms_auto_notification@vitaminshoppe.com" -s "Error connecting to OMS Prod DB" $TO_ADDRS_ON_FAILURE
elif [ $RELEASE_ACK_Count -gt 0 ]
then
echo -e "Orders in VSI_RELEASE_ACK_DELAY queue.\n\n$order_msg"|mailx -r "oms_auto_notification@vitaminshoppe.com" -s "Orders in VSI_RELEASE_ACK_DELAY queue. Count:$RELEASE_ACK_Count" -a /home/OMSAutomationScripts/VSI_RELEASE_ACK_DELAY.txt $TO_ADDRS
fi

