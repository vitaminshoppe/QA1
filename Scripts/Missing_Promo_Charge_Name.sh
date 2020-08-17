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
#LOGFILE2=$LOGDIR/Rele`ase_StoreReceive2.txt

ARCDIR=$LOGDIR/arch
LOGFILE1=$LOGDIR/MissingPromoChargeName.txt
#TO_ADDRS="omnicustomercare@vitaminshoppe.com dpadilla@vitaminshoppe.com oms@vitaminshoppe.com helpdesk@vitaminshoppe.com"
TO_ADDRS="shivraj.ugale@vitaminshoppe.com"
TO_ADDRS_ON_FAILURE="oms@vitaminshoppe.com"


export LOGDIR ARCDIR PATH ORACLE_HOME USER PWD TO_ADDRS TO_ADDRS_ON_FAILURE

sqlplus -silent /nolog <<END
whenever sqlerror exit 1;
whenever oserror exit 1;
set pagesize 0
set linesize 1000
set trimspool on
set feedback off
set heading off
connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=10.3.51.167)(PORT= 1881)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=OMSPROD_SVC)))"
spool $LOGFILE1
SELECT YOH.ORDER_NO, YCT.CHARGE_TYPE, YOH.PAYMENT_STATUS, YOH.HOLD_FLAG, COUNT(YCT.CHARGE_TYPE)
FROM STERLING.YFS_CHARGE_TRANSACTION YCT, STERLING.YFS_ORDER_HEADER YOH
WHERE YCT.ORDER_HEADER_KEY = YOH.ORDER_HEADER_KEY
--AND YOH.ORDER_NO NOT IN ('WO16364164','WO16314811','WO16370602','WO16353078','WO16317865','WO16316164','WO16349583','WO40074371','00151171101778829100','WO16370500','WO45284640','WO45282359','WO16316260','WO16379906','WO16359436','WO16354093','WO45281803','00453171103361119100','WO16380224','WO45284764')
AND YCT.CHARGE_TYPE = 'CHARGE'
AND YOH.DOCUMENT_TYPE = '0001' AND YOH.DRAFT_ORDER_FLAG = 'N'
--AND YCT.CHARGE_TRANSACTION_KEY > '20171129'
AND YCT.CHARGE_TRANSACTION_KEY > TO_CHAR(SYSDATE - 2,'YYYYMMDD') 
GROUP BY YOH.ORDER_NO, YCT.CHARGE_TYPE, YOH.PAYMENT_STATUS, YOH.HOLD_FLAG
HAVING COUNT(YCT.CHARGE_TYPE) > '10'
ORDER BY COUNT(YCT.CHARGE_TYPE) DESC;
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
SELECT COUNT(YOH.ORDER_NO) AS C
FROM STERLING.YFS_CHARGE_TRANSACTION YCT, STERLING.YFS_ORDER_HEADER YOH
WHERE YCT.ORDER_HEADER_KEY = YOH.ORDER_HEADER_KEY
AND YOH.ORDER_NO NOT IN ('WO16364164','WO16314811','WO16370602','WO16353078','WO16317865','WO16316164','WO16349583','WO40074371','00151171101778829100','WO16370500','WO45284640','WO45282359','WO16316260','WO16379906','WO16359436','WO16354093','WO45281803','00453171103361119100','WO16380224','WO45284764')
AND YCT.CHARGE_TYPE = 'CHARGE'
AND YOH.DOCUMENT_TYPE = '0001' AND YOH.DRAFT_ORDER_FLAG = 'N'
AND YCT.CHARGE_TRANSACTION_KEY > '20171129'
GROUP BY YOH.ORDER_NO, YCT.CHARGE_TYPE, YOH.PAYMENT_STATUS, YOH.HOLD_FLAG
HAVING COUNT(YCT.CHARGE_TYPE) > '16';
exit 0
END
)

if [ $? -ne 0 ]
then
echo "****Error in the process****"
DT=`date +%h_%d_%Y`
TM=`date +%H:%M:%S`
#echo "${DT}_${TM}"|mailx -s "Error running Missing_Promo_Charge_Name script" $TO_ADDRS
echo -e "Error running Missing_Promo_Charge_Name script"|mailx -r "oms_auto_notification@vitaminshoppe.com" -s "Error connecting to OMS Prod DB" $TO_ADDRS_ON_FAILURE
elif [ $orderCount -gt 0 ]
then
echo -e "Orders with multiple auths.\n\n$order_msg"|mailx -r "oms_auto_notification@vitaminshoppe.com" -s "Orders with multiple auths.Count: $orderCount" $TO_ADDRS
fi