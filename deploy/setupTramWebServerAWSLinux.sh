#!/bin/bash

# AWS LINUX VERSION #################

logger Begin setup of tramchester server

export USERDATA=http://169.254.169.254/latest/user-data

wget -nv $USERDATA -O userdata.txt

export PLACE=`cat userdata.txt | grep ENV | cut -d = -f 2-`
export BUILD=`cat userdata.txt | grep BUILD | cut -d = -f 2-`
export ARTIFACTSURL=`cat userdata.txt | grep ARTIFACTSURL | cut -d = -f 2-`
export REDIRECTHTTP=`cat userdata.txt | grep REDIRECTHTTP | cut -d = -f 2-`
export TFGMAPIKEY=`cat userdata.txt | grep TFGMAPIKEY | cut -d = -f 2-`
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
if [ "$REDIRECTHTTP" == '' ]; then
        export REDIRECTHTTP=false
fi

logger Set up Web server Build: $BUILD Url: $ARTIFACTSURL Env: $PLACE

# fetch and install the package
distUrl=$ARTIFACTSURL/$BUILD/$target.zip
dist=`basename $distUrl`

# set up overrides for server config so data is pulled from S3 at start up
export TRAM_DATAURL=$ARTIFACTSURL/$BUILD/tfgm_data.zip

cd ~ec2-user
mkdir -p server
cd server

logger Get $distUrl
wget -nv $distUrl -O $dist
unzip $dist

# cloudwatch logs agent (NEW)
logger set up amazon cloudwatch logs agent new
wget -nv https://s3.amazonaws.com/amazoncloudwatch-agent/amazon_linux/amd64/latest/amazon-cloudwatch-agent.rpm
sudo rpm -U ./amazon-cloudwatch-agent.rpm
sed -i.orig "s/PREFIX/web_${PLACE}_${BUILD}/" $target/config/cloudwatch_agent.json
sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl -a fetch-config -m ec2 -c file:$target/config/cloudwatch_agent.json -s
logger cloud watch agent installed

# download pre-built DB
logger Get DB from $ARTIFACTSURL/$BUILD/tramchester.db.zip
wget -nv $ARTIFACTSURL/$BUILD/tramchester.db.zip -O tramchester.db.zip
unzip tramchester.db.zip -d tramchester.db

# fix ownership
chown -R ec2-user .

# start 
logger Start tramchester
export JAVA_OPTS="-Xmx450m"
sudo -E -u ec2-user bash ./$target/bin/start.sh &

logger Finish Web bootstrap script for $BUILD and $PLACE
