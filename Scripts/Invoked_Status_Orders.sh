#!/bin/ksh
#

ORACLE_HOME=/u01/app/oracle/product/11.2.0/db
LD_LIBRARY_PATH=$ORACLE_HOME/lib:$ORACLE_HOME/ctx/lib:$LD_LIBRARY_PATH
TNS_ADMIN=$ORACLE_HOME/network/admin
PATH=$ORACLE_HOME/bin:$PATH:.

#Change user, pswd and db to point to target database

RUN_SCRIPT=PROD


if [ $RUN_SCRIPT == "PROD" ] 
then    	
	USER=sterling_reader01
	PWD=xpruhjJ3Px
	DatabaseName=OMS_PROD
	HOST_NAME=10.3.51.167
	PORT=1881
	SERVICE_SID="(SERVICE_NAME=OMSPROD_SVC)"	
	#st3rling0ms, xpruhjJ3Px, lin-vsi-omspdb3, 1881, OMSPROD
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



export PATH LD_LIBRARY_PATH TNS_ADMIN USER PWD DB ORACLE_HOME

LOGDIR=/home/OMSAutomationScripts
ARCDIR=$LOGDIR/Arch
LOGFILE=$LOGDIR/InvokedStatusOrderResults.txt
#LOGFILE2=$LOGDIR/InvokedStatusOrderResults2.txt
#TO_ADDRS="oms@vitaminshoppe.com"
TO_ADDRS="Yadavendra.Singh@vitaminshoppe.com"
TO_ADDRS="oms@vitaminshoppe.com"
TO_ADDRS_ON_ZERO_COUNT="oms@vitaminshoppe.com"
FROM_ADDRS="oms_auto_notification@vitaminshoppe.com"
export LOGDIR ARCDIR PATH ORACLE_HOME USER PWD TO_ADDRS FROM_ADDRS LOGFILE

# query the Order Header table and email the orders loaded


sqlplus -silent /nolog <<END
whenever sqlerror exit 1;
whenever oserror exit 1;
set pagesize 50000
set linesize 1000
set trimspool on
set feedback off
set heading on
connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=$HOST_NAME)(PORT= $PORT)))(CONNECT_DATA=(SERVER=DEDICATED)$SERVICE_SID))"
spool $LOGFILE
ALTER SESSION SET nls_date_format = 'DD-MON-RR HH24:MI:SS';
SELECT TO_CHAR(TRIM(YOH.ORDER_NO)) AS ORDER_NO, TRIM(YCT.CHARGE_TRANSACTION_KEY) AS CHARGE_TRAN_KEY
FROM STERLING.YFS_CHARGE_TRANSACTION YCT, STERLING.YFS_ORDER_HEADER YOH
WHERE YCT.ORDER_HEADER_KEY = YOH.ORDER_HEADER_KEY
AND YCT.USER_EXIT_STATUS = 'INVOKED'
AND YCT.STATUS = 'OPEN'
AND ORDER_NO IN (SELECT YOH.ORDER_NO
FROM STERLING.YFS_ORDER_HEADER YOH, STERLING.YFS_PAYMENT YP
WHERE YOH.ORDER_HEADER_KEY = YP.ORDER_HEADER_KEY
AND YOH.ORDER_DATE > SYSDATE - 10)
ORDER BY ORDER_NO ASC;
exit 0
END

order_msg=$(cat $LOGFILE)




VALUE=`sqlplus -silent /nolog <<EOF
whenever sqlerror exit 1;
whenever oserror exit 1;
set pagesize 50000
set linesize 1000
set trimspool on
set feedback off
set heading off
set pagesize 0
connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=$HOST_NAME)(PORT= $PORT)))(CONNECT_DATA=(SERVER=DEDICATED)$SERVICE_SID))"
ALTER SESSION SET nls_date_format = 'DD-MON-RR HH24:MI:SS';
SELECT TRIM(YCT.CHARGE_TRANSACTION_KEY) AS CHARGE_TRAN_KEY
FROM STERLING.YFS_CHARGE_TRANSACTION YCT, STERLING.YFS_ORDER_HEADER YOH
WHERE YCT.ORDER_HEADER_KEY = YOH.ORDER_HEADER_KEY
AND YCT.USER_EXIT_STATUS = 'INVOKED'
AND YCT.STATUS = 'OPEN'
AND ORDER_NO IN (SELECT YOH.ORDER_NO
FROM STERLING.YFS_ORDER_HEADER YOH, STERLING.YFS_PAYMENT YP
WHERE YOH.ORDER_HEADER_KEY = YP.ORDER_HEADER_KEY
AND YOH.ORDER_DATE > SYSDATE - 30)
ORDER BY ORDER_NO ASC;
exit 0
EOF`


#UPDATE STERLING.YFS_CHARGE_TRANSACTION SET USER_EXIT_STATUS = ' ' WHERE CHARGE_TRANSACTION_KEY in ('<CHARGE_TRANSACTION_KEY1>','<CHARGE_TRANSACTION_KEY2>',......);

echo "resultset: "
#echo "${VALUE}"

echo " "

echo "now iterate ..."
let rec=0
CATCHARGE_TRANSACTION_KEY="";

echo "${VALUE}" |while read line
do
  #echo "processing $rec: $line"
  CATCHARGE_TRANSACTION_KEY=$CATCHARGE_TRANSACTION_KEY"'"$line"'"
  let rec=rec+1
  CATCHARGE_TRANSACTION_KEY=$CATCHARGE_TRANSACTION_KEY","
done

echo "before"$CATCHARGE_TRANSACTION_KEY

CATCHARGE_TRANSACTION_KEY=$(sed 's/.\{1\}$//' <<< "$CATCHARGE_TRANSACTION_KEY")

echo "after"$CATCHARGE_TRANSACTION_KEY






orderInvokedCount=$(sqlplus -silentsilent /nolog <<END
whenever sqlerror exit 1;
whenever oserror exit 1;
set pagesize 0
set feedback off
set heading off
connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=$HOST_NAME)(PORT= $PORT)))(CONNECT_DATA=(SERVER=DEDICATED)$SERVICE_SID))"
SELECT COUNT(DISTINCT(YOH.ORDER_NO))
FROM STERLING.YFS_CHARGE_TRANSACTION YCT, STERLING.YFS_ORDER_HEADER YOH
WHERE YCT.ORDER_HEADER_KEY = YOH.ORDER_HEADER_KEY
AND YCT.USER_EXIT_STATUS = 'INVOKED'
AND YCT.STATUS = 'OPEN'
AND ORDER_NO IN (SELECT YOH.ORDER_NO
FROM STERLING.YFS_ORDER_HEADER YOH, STERLING.YFS_PAYMENT YP
WHERE YOH.ORDER_HEADER_KEY = YP.ORDER_HEADER_KEY
AND YOH.ORDER_DATE > SYSDATE - 30);
exit 0
END
)

#CATCHARGE_TRANSACTION_KEY=""

if [ -z "$CATCHARGE_TRANSACTION_KEY" ]
then
      echo "\$CATCHARGE_TRANSACTION_KEY is empty"
else
sqlplus -s -silent /nolog <<EOF
whenever sqlerror exit 1;
whenever oserror exit 1;
SET ECHO OFF
SET FEEDBACK OFF
SET PAGES 0
SET SERVEROUTPUT ON
SET VERIFY OFF
SET TRIMSPOOL ON
connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=$HOST_NAME)(PORT= $PORT)))(CONNECT_DATA=(SERVER=DEDICATED)$SERVICE_SID))"
UPDATE STERLING.YFS_CHARGE_TRANSACTION SET USER_EXIT_STATUS = ' ' WHERE CHARGE_TRANSACTION_KEY in ($CATCHARGE_TRANSACTION_KEY);
/
   
commit;
exit 0
EOF
fi

if [ $? -ne 0 ]
then
echo "****Error in the process****"
DT=`date +%h_%d_%Y`
TM=`date +%H:%M:%S`
#echo "${DT}_${TM}"|mailx -s "Error running Invoked Status Orders" $TO_ADDRS
echo -e "Error running Invoked_Status_Orders script. Please contact OMS Support.\n\nThanks."|mailx -r "oms_auto_notification@vitaminshoppe.com" -s "Error connecting to OMS Test DB" $TO_ADDRS
#elif [ $orderInvokedCount -eq 0 ]
#then
#echo -e "No orders stuck in Invoked Status" | mailx -r "oms_auto_notification@vitaminshoppe.com" -s "No orders stuck in Invoked Status:$orderCount" $TO_ADDRS_ON_ZERO_COUNT
elif [ $orderInvokedCount -gt 0 ]
then 
#cd $ARCDIR
#mv weber_order_status.dat weber_order_status.dat_${DT}_${TM}
echo -e "ORDERS with Payment User Exit Status as INVOKED.\n\nPlease update the USER EXIT STATUS of INVOKED to blank and confirm if the authorization is successful or not. Ignore CANCELLED orders which are in INVOKED status.\n\n$order_msg"|mailx -r $FROM_ADDRS -s "Invoked Status Orders. Count:$orderInvokedCount" -a /home/OMSAutomationScripts/InvokedStatusOrderResults.txt $TO_ADDRS
fi