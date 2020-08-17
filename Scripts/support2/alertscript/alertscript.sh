#!/bin/ksh
#
#su -  c /home/OMSAutomationScripts/Count of Orders Created/Processed from ATG,OMS,AZDC,VADC.sh

CURRENT_DIR="/home/OMSAutomationScripts/support2/alertscript/"
ORACLE_HOME=/u01/app/oracle/product/11.2.0/db
LD_LIBRARY_PATH=$ORACLE_HOME/lib:$ORACLE_HOME/ctx/lib:$LD_LIBRARY_PATH
TNS_ADMIN=$ORACLE_HOME/network/admin
PATH=$ORACLE_HOME/bin:$PATH:.
NEWLINE=$'\n'

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

#######################################################
queue_listfilename=$CURRENT_DIR"queue_list.txt"

queuestr=""

while read -r line; do
    #name="$line"
    queuestr=$queuestr"'""$line""',"
done < "$queue_listfilename"

#echo "read end queue_id"$queuestr
queuestr=$(sed 's/.\{1\}$//' <<< "$queuestr")

#echo "after"$queuestr

###############################################

thresholdcsvqueues_filename=$CURRENT_DIR"config_file_alert_script.txt"

typeset -A thresholdcsvqueues
IFS=","
while read f1 f2
do        
		thresholdcsvqueues+=( ["$f1"]="$f2" )
done < $thresholdcsvqueues_filename



#connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=10.3.51.167)(PORT= 1881)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=OMSPROD_SVC)))"
#connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=lin-vsi-omspdb3)(PORT= 1881)))(CONNECT_DATA=(SERVER=DEDICATED)(SERVICE_NAME=OMSPROD)))"



export PATH LD_LIBRARY_PATH TNS_ADMIN USER PWD DB ORACLE_HOME

LOGDIR=/home/OMSAutomationScripts
ARCDIR=$LOGDIR/Arch
LOGFILE4=$LOGDIR/omsalertcount1.txt


TO_ADDRS="omnicustomercare@vitaminshoppe.com oms@vitaminshoppe.com operators@vitaminshoppe.com"

TO_ADDRS_ON_FAILURE="oms@vitaminshoppe.com"
#TO_ADDRS_ON_FAILURE = "omnicustomercare@vitaminshoppe.com oms@vitaminshoppe.com operators@vitaminshoppe.com ooyediran@vitaminshoppe.com UnixAdmins@vitaminshoppe.com"
AZ_ADDRS="oms@vitaminshoppe.com"
TO_ADDRS_ON_FAILURE="oms@vitaminshoppe.com operators@vitaminshoppe.com"
TO_ADDRS="oms@vitaminshoppe.com operators@vitaminshoppe.com"
#TO_ADDRS="Yadavendra.Singh@vitaminshoppe.com"

export LOGDIR ARCDIR PATH ORACLE_HOME USER PWD FTC_ADDRS TO_ADDRS_ON_FAILURE

oraMsg1=`$ORACLE_HOME/bin/sqlplus /nolog << EOF |grep -i ,
connect $USER/$PWD@"(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST=$HOST_NAME)(PORT= $PORT)))(CONNECT_DATA=(SERVER=DEDICATED)$SERVICE_SID))"
set echo off linesize 850 heading off pagesize 1000 serveroutput on
whenever sqlerror exit sql.sqlcode;
declare
cursor c1 is 

select trim(q.QUEUE_ID) as queue_id,
COUNT(*) as queuecount
from STERLING.yfs_inbox inbox , STERLING.YFS_QUEUE q where
trim(q.QUEUE_KEY)=trim(inbox.queue_key) and   q.QUEUE_ID in ($queuestr)
AND inbox.STATUS='OPEN'
GROUP BY q.QUEUE_ID order by queuecount DESC;

begin
for i in c1 loop
  dbms_output.put_line(i.queue_id||','||i.queuecount||':');
end loop;
end;
/
exit
EOF
`

echo 'yadavendra'$oraMsg1



myoutput="Queues which are above the threshold"
myoutput=$myoutput"$NEWLINE"



IFS=:
ary=($oraMsg1)
for key in "${!ary[@]}"; do 
	#echo "$key ${ary[$key]}";
	
	IFS=,
	ary1=(${ary[$key]})	
	#echo "${ary1[$key1]}";	
	#echo "${ary1[0]}""-""${ary1[1]}"	
	dbqname="${ary1[0]}"
	dbqcount="${ary1[1]}"	
	dbqname=$(echo "$dbqname" | xargs) 
	dbqcount=$(echo "$dbqcount" | xargs) 
	dbqcount=(`echo $dbqcount | awk '{printf( "%d", $1 )}'`)
	#echo "$dbqname"
	propvalue=${thresholdcsvqueues["$dbqname"]}
	propvalue=(`echo $propvalue | awk '{printf( "%d", $1 )}'`)
	if [ $propvalue  -gt 0 ]
	then
		echo $propvalue
		echo $dbqcount
		if [ $dbqcount -gt $propvalue ]
		then
			#echo "found"	
			myoutput=$myoutput"$NEWLINE"
			myoutput=$myoutput$(echo "Queue " "$dbqname" " has count  " "$dbqcount"" in OMS") 
			myoutput=$myoutput"$NEWLINE"
		fi
	fi
done


echo "now email"

if [ $? -ne 0 ]
then
echo "****Error in the process****"
DT=`date +%h_%d_%Y`
TM=`date +%H:%M:%S`

echo "now email00"

echo -e "Operators,\n\nPlease check connectivity to OMS servers by logging to App Console 1 and 2. If not able to connect call DB ADMIN.\nDuring weekend and off-hours, also call OMS team (follow OMS escalation).\n\nThanks."|mailx -r "oms_auto_notification@vitaminshoppe.com" -s "Error connecting to OMS Prod DB"  $TO_ADDRS_ON_FAILURE

else

#echo "now email1111"$myoutput
echo -e "Summary of OMS alert queues.\n\nPlease follow OMS escalation chart if there are more than 5 orders below during WEEKENDS, HOLIDAYS or AFTER OFFICE HOURS.\n\nScript Name:alertscript.sh .\n\n$myoutput"|mailx -r "oms_auto_notification@vitaminshoppe.com" -s "Summary of OMS alert queues. " $TO_ADDRS

fi

