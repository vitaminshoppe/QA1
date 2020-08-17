
#CURRENT_DIR="/home/OMSAutomationScripts/support4/relexscript/"

CURRENT_DIR="/home/OMSAutomationScripts/support4/OMS-1936"
cd  $CURRENT_DIR

./relex_report.sh
sleep 5
./sftp_jda_relex.sh
sleep 5
exit