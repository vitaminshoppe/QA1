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

ARCDIR=$LOGDIR/arch
LOGFILE1=$LOGDIR/MissingCCExpiryDate.txt
#LOGFILE2=$LOGDIR/CCMissingExpiryDate2.txt
TO_ADDRS="bob.ponik@vitaminshoppe.com omnicustomercare@vitaminshoppe.com oms@vitaminshoppe.com"
#TO_ADDRS="shivraj.ugale@vitaminshoppe.com"
export LOGDIR ARCDIR PATH ORACLE_HOME USER PWD TO_ADDRS

sqlplus -silent /nolog <<END
whenever sqlerror exit 1;
whenever oserror exit 1;
set pagesize 0
set linesize 500
set trimspool on
set feedback off
set heading off
connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=10.3.51.167)(PORT= 1881)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=OMSPROD_SVC)))"
spool $LOGFILE1
ALTER SESSION SET nls_date_format = 'DD-MON-YY HH12:MI:SS PM';
SELECT TRIM(YOH.ORDER_DATE||'	|	'|| YOH.ORDER_NO ||'	|	'||YOH.ENTERED_BY||'	|	'|| 
CASE 
WHEN YP.CREDIT_CARD_TYPE = 'MASTERCARD' THEN 'MC'
WHEN YP.CREDIT_CARD_TYPE = 'DISCOVER' THEN 'DISC'
ELSE YP.CREDIT_CARD_TYPE END
||'	|'||YP.CREDIT_CARD_NO||'	|'||YOH.BILL_TO_ID ||'	|'|| YOH.CUSTOMER_FIRST_NAME ||' '|| YOH.CUSTOMER_LAST_NAME ||'		|'|| 
CASE 
WHEN YP.CREDIT_CARD_NO  LIKE '%*%'  THEN 'Bad Credit Card #' 
WHEN YP.CREDIT_CARD_EXP_DATE = ' ' THEN 'Credit Card Date Missing'
END ) AS MISSING_CREDIT_CARD_DETAILS
FROM STERLING.YFS_ORDER_HEADER YOH, STERLING.YFS_PAYMENT YP
WHERE YOH.ORDER_HEADER_KEY = YP.ORDER_HEADER_KEY
AND YP.PAYMENT_TYPE = 'CREDIT_CARD'
AND YOH.ORDER_DATE > TRUNC(SYSDATE-5,'DD')
AND (YP.CREDIT_CARD_NO  LIKE '%*%' OR YP.CREDIT_CARD_EXP_DATE = ' ')
ORDER BY ORDER_DATE DESC; 
spool off
exit 0
END

order_msg=$(cat $LOGFILE1)

orderCount=$(sqlplus -silent /nolog <<END
whenever sqlerror exit 1;
whenever oserror exit 1;
set pagesize 1000
set feedback off
set heading off
connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=10.3.51.167)(PORT= 1881)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=OMSPROD_SVC)))"
SELECT COUNT(DISTINCT(YOH.ORDER_NO)) AS C
FROM STERLING.YFS_ORDER_HEADER YOH, STERLING.YFS_PAYMENT YP
WHERE YOH.ORDER_HEADER_KEY = YP.ORDER_HEADER_KEY
AND YP.PAYMENT_TYPE = 'CREDIT_CARD'
AND YOH.ORDER_DATE > TRUNC(SYSDATE-5,'DD')
AND (YP.CREDIT_CARD_NO  LIKE '%*%' OR YP.CREDIT_CARD_EXP_DATE = ' ');
exit 0
END
)

if [ $? -ne 0 ]
then
echo "****Error in the process****"
DT=`date +%h_%d_%Y`
TM=`date +%H:%M:%S`
echo "${DT}_${TM}"|mailx -s "Error running Missing Credit Card Expiry Date script" $TO_ADDRS
elif [ $orderCount -eq 0 ]
then
echo -e "This mornings query displayed no orders with missing credit card expiration dates or bad credit card number." | mailx -r "oms_auto_notification@vitaminshoppe.com" -s "No orders with invalid payment details" $TO_ADDRS
elif [ $orderCount -gt 0 ]
then
echo -e "ORDERS with missing credit card expiry date or bad credit card number.\n\nPlease request for expiry date from customer care and update the same in YFS_PAYMENT table.\nFor orders with bad credit card number, they will have to be cancelled or getting auto cancelled during authorization and possibly recreated by customer care.\n\n$order_msg"|mailx -r "oms_auto_notification@vitaminshoppe.com" -s "Missing Credit Card Details. Count:$orderCount" $TO_ADDRS
fi