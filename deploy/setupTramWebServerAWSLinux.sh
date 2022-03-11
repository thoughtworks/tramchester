#!/bin/bash

# AWS LINUX VERSION #################

logger Begin setup of tramchester server

export USERDATA=http://169.254.169.254/latest/user-data

wget -nv $USERDATA -O /tmp/userdata.txt

# extract from instance user data
export PLACE=`cat /tmp/userdata.txt | grep ENV | cut -d = -f 2-`
export BUILD=`cat /tmp/userdata.txt | grep BUILD | cut -d = -f 2-`
export ARTIFACTSURL=`cat /tmp/userdata.txt | grep ARTIFACTSURL | cut -d = -f 2-`
export TFGMAPIKEY=`cat /tmp/userdata.txt | grep TFGMAPIKEY | cut -d = -f 2-`
export NESSUS_LINKING_KEY=`cat /tmp/userdata.txt | grep NESSUS_LINKING_KEY | cut -d = -f 2-`

target=tramchester-1.0

if [ "$BUILD" == '' ]; then
        echo 'BUILD missing'
        exit;
fi
if [ "$PLACE" == '' ]; then
        echo 'PLACE missing'
        exit;
fi
if [ "$ARTIFACTSURL" == '' ]; then
        echo 'ARTIFACTSURL missing'
        exit;
fi

logger Set up Web server Build: $BUILD Url: $ARTIFACTSURL Env: $PLACE

# fetch and install the package
distUrl=$ARTIFACTSURL/$BUILD/$target.zip
dist=`basename $distUrl`

# set up overrides for server config so data is pulled from S3 at start up
# TODO these could be s3:// urls
export TRAM_DATAURL=$ARTIFACTSURL/$BUILD/tfgm_data.zip
export NAPTAN_DATAURL=$ARTIFACTSURL/$BUILD/Stops.xml.zip
export RAIL_DATAURL=$ARTIFACTSURL/$BUILD/rail_data.zip
export NPTG_DATAURL=$ARTIFACTSURL/$BUILD/nptgcsv.zip

logger TRAM_DATAURL is $TRAM_DATAURL
logger NAPTAN_DATAURL is $NAPTAN_DATAURL
logger RAIL_DATAURL is $RAIL_DATAURL
logger NPTG_DATAURL is $NPTG_DATAURL

cd ~ec2-user || (logger Could not cd to ec2-user && exit)
mkdir -p server
cd server || (logger Could not cd to ec2-user/server && exit)

logger Get $distUrl
wget -nv $distUrl -O $dist
unzip $dist || (logger Could not unzip from $dist from $distUrl && exit)

# cloudwatch logs agent (NEW)
logger set up amazon cloudwatch logs agent new
wget -nv https://s3.amazonaws.com/amazoncloudwatch-agent/amazon_linux/amd64/latest/amazon-cloudwatch-agent.rpm
sudo rpm -U ./amazon-cloudwatch-agent.rpm
sed -i.orig "s/PREFIX/web_${PLACE}_${BUILD}/" $target/config/cloudwatch_agent.json
sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl -a fetch-config -m ec2 -c file:$target/config/cloudwatch_agent.json -s
logger cloud watch agent installed

# download pre-built DB
dbURL=$ARTIFACTSURL/$BUILD/database.zip
logger Get DB from $dbURL
wget -nv $dbURL -O database.zip
unzip database.zip

# fix ownership
chown -R ec2-user .

## install nessus agent if key is provided
if [ "$NESSUS_LINKING_KEY" != '' ]; then
  sudo -E bash ./$target/bin/nessus_agent_install.sh &
fi

# start 
logger Start tramchester
export JAVA_OPTS="-Xmx550m"
sudo -E -u ec2-user bash ./$target/bin/start.sh &

logger Finish Web bootstrap script for $BUILD and $PLACE
