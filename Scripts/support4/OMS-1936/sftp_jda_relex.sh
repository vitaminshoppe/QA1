#!/bin/bash
d=`date +%Y%m%d%H%M%S`

#oms_auto_notification@vitaminshoppe.com
FROM_ADDRS="oms_auto_notification@vitaminshoppe.com"
TO_ADDRS="Steve.Finkelstein@vitaminshoppe.com OMS@vitaminshoppe.com JDADevelopment@vitaminshoppe.com"

#cd  /u01/jda_data/DirectBackOrder
#CURRENT_DIR="/home/OMSAutomationScripts/support4/relexscript/"
#cd  /home/OMSAutomationScripts/support4/relexscript/
CURRENT_DIR="/home/OMSAutomationScripts/support4/OMS-1936/"
cd  $CURRENT_DIR


echo "Relex Report log" > Relex_Report_$d.log

sftp omsuser@10.3.2.140:/relex <<EOF &> Relex_Report_$d.log
put relex_data.csv
quit
EOF

if [ $? -ne 0 ] ; then
       echo 'Connection Error' > Relex_Report_$d.log
       echo -e "Hello,\n\nJDA Relex Report was executed but there was a connection error during SFTP. Please look into the issue\n\nThank You" | mailx -r $FROM_ADDRS -s "Relex Report Connection Issue - $d" $TO_ADDRS
	else
		echo "" | mail -r $FROM_ADDRS -s "RELEX Report uploaded to JDAS `date +%Y%m%d`" -a Relex_Report_$d.log $TO_ADDRS
fi

exit
