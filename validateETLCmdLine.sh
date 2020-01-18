#!/bin/bash
#SETTING PATHS`
TRUNKPATH="$PWD"
export JAVA8=/usr/java/default
export JAVA_HOME=${JAVA8}
java -version
#remove tmp dir
rm -rf /tmp/jodi_core

#make tmp dir
mkdir -p /tmp/jodi_core

#copy latest zip file to temp folder
cp ./jodi/jodi_core*.zip /tmp/jodi_core

cd /tmp/jodi_core
echo "In tmp directory"
pwd

#unzip distribution file
unzip jodi_core*.zip

cd jodi_core*
pwd

mkdir -p xml

#copy the properties files
cp $TRUNKPATH/jodi_qa/src/test/resources/SampleC/conf/Sample.properties ./conf
cp $TRUNKPATH/jodi_qa/src/test/resources/SampleC/conf/Sample_rt.properties ./conf
cp -r $TRUNKPATH/jodi_qa/src/test/resources/SampleC/xml/* ./xml/
cp $TRUNKPATH/jodi_core/src/main/resources/jodi-model.v1.1.xsd ./conf
cp $TRUNKPATH/jodi_core/src/main/resources/jodi-packages.v1.1.xsd ./conf

sed -i 's/..\/jodi_core\/src\/main\/resources/..\/conf/' ./conf/Sample.properties

. $TRUNKPATH/jodi_core/src/bin/setOdiLibPath.sh

echo "ODI LIB PATH Settings"
echo $ODI_LIB_PATH

cd ./bin

chmod 777 *.sh
#validate OdiAlter Table cmd line
./OdiAlterTable.sh -c ../conf/Sample.properties -pw $ODI_USER_PWD -mpw $ODI_REPO_PWD
RETVAL=$?
if [ $RETVAL -ne 0 ] 
then
  echo "OdiAlterTable command line failed ..."
  . $TRUNKPATH/jodi_core/src/bin/unSetOdiLibPath.sh
  exit 1
else
  echo "OdiAlterTable command line passed ..."
fi

#Validate OdiCheckTable cmd line
./OdiCheckTable.sh -c ../conf/Sample.properties -pw $ODI_USER_PWD -mpw $ODI_REPO_PWD
RETVAL=$?
if [ $RETVAL -ne 0 ]
then
  echo "OdiCheckTable command line failed ..."
  . $TRUNKPATH/jodi_core/src/bin/unSetOdiLibPath.sh
  exit 1
else
  echo "OdiCheckTable command line passed ..."
fi

#Validate OdiSCD cmd line
./OdiSCD.sh -c ../conf/Sample.properties -pw $ODI_USER_PWD -mpw $ODI_REPO_PWD
RETVAL=$?
if [ $RETVAL -ne 0 ]
then
  echo "OdiSCD command line failed ..."
  . $TRUNKPATH/jodi_core/src/bin/unSetOdiLibPath.sh
  exit 1
else
  echo "OdiSCD command line passed ..."
fi

#Validate OdiCreatePackage cmd line
./OdiCreatePackage.sh -c ../conf/Sample.properties -m ../xml -pw $ODI_USER_PWD -mpw $ODI_REPO_PWD
RETVAL=$?
if [ $RETVAL -ne 0 ]
then
  echo "OdiCreatePackage command line failed ..."
  . $TRUNKPATH/jodi_core/src/bin/unSetOdiLibPath.sh
  exit 1
else
  echo "OdiCreatePackage command line passed ..."
fi

#Validate OdiCreateInterface cmd line
./OdiCreateInterface.sh -c ../conf/Sample.properties -m ../xml -p "Init " -pw $ODI_USER_PWD -mpw $ODI_REPO_PWD
RETVAL=$?
if [ $RETVAL -ne 0 ]
then
  echo "OdiCreateInterface command line failed ..."
  . $TRUNKPATH/jodi_core/src/bin/unSetOdiLibPath.sh
  exit 1
else
  echo "OdiCreateInterface command line passed ..."
fi

#Validate OdiCreateEtls cmd line
./OdiCreateEtls.sh -c ../conf/Sample.properties -m ../xml -p "Init " -pw $ODI_USER_PWD -mpw $ODI_REPO_PWD
RETVAL=$?
if [ $RETVAL -ne 0 ]
then
  echo "OdiCreateEtls command line failed ..."
  . $TRUNKPATH/jodi_core/src/bin/unSetOdiLibPath.sh
  exit 1
else
  echo "OdiCreateEtls command line passed ..."
fi

#Validate OdiExtractionTables cmd line
./OdiExtractionTables.sh -c ../conf/Sample.properties -ps 1001 -m ../xml -srcmdl "ORACLE_CHINOOK" -pw $ODI_USER_PWD -mpw $ODI_REPO_PWD
RETVAL=$?
if [ $RETVAL -ne 0 ]
then
  echo "OdiExtractionTables command line failed ..."
  . $TRUNKPATH/jodi_core/src/bin/unSetOdiLibPath.sh
  exit 1
else
  echo "OdiExtractionTables command line passed ..."
fi

. $TRUNKPATH/jodi_core/src/bin/unSetOdiLibPath.sh