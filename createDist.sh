#!/bin/bash
#
# Script to create distribution of jodi
#
# 

cd "$(dirname "$0")"
echo "Building..."
echo `date`
echo "Local path:"
export APATH="$PWD"
echo "Current path: ${APATH}"
echo "$PATH"
echo "env"
env
# JAVA VERSION 
# export JAVA7=/usr/java/jdk1.7.0_79
# export JAVA8=/usr/java/default
# export JAVA_HOME=${JAVA8}
export PATH=${JAVA_HOME}/bin:/u02/apache-maven-3.3.9/bin:${PATH}

java -version

#### git pull = git fetch + git merge
# http://stackoverflow.com/questions/16077971/git-produces-gtk-warning-cannot-open-display
# unset SSH_ASKPASS
# export PATH=/u02/git-2.7.3:$PATH
# git version
# older versions should use;
# git reset --hard
# git checkout master
# git pull origin master
#### end git

rm -Rf /tmp/countrylist.csv*
cp jodi_qa/src/test/resources/FunctionalTest/countrylist.csv /tmp
chmod 777 /tmp/countrylist.csv

# clean global distribution folder
rm -Rf jodi 
mkdir jodi

# clean, increment build version and build for all projects
gradle clean increaseBuildNumber
# build must be on a separate line to pickup new build number
# build all code related to jodi_core but test all related project separately
gradle -P distribution=true jodi_core:build 
RETVAL=$?
[ $RETVAL -ne 0 ] && exit 1
echo "jodi_core build exists with code $RETVAL"
# test all related project separately
gradle -P distribution=true jodi_base:findbugsMain jodi_base:findbugsTest jodi_base:test jodi_odi12:findbugsMain jodi_odi12:findbugsTest jodi_odi12:test
RETVAL=$?
[ $RETVAL -ne 0 ] && exit 1
echo "jodi_core subprojects tests exists with code $RETVAL"

##sh ./gradlew -P distribution=true --debug jodi_pbe_tools:build jodi_pbe:build jodi_pbe:distZip jodi_plugins:findbugsMain jodi_plugins:findbugsTest jodi_plugins:test
# RETVAL=$?
#[ $RETVAL -ne 0 ] && exit 1
# echo "jodi_core subprojects tests exists with code $RETVAL"

cp jodi_core/build/libs/* jodi/
RETVAL=$?
[ $RETVAL -ne 0 ] && exit 1

# cp jodi_pbe/build/distributions/* jodi/
# RETVAL=$?
# [ $RETVAL -ne 0 ] && exit 1

# cp jodi_pbe_tools/build/distributions/* jodi/
# RETVAL=$?
# [ $RETVAL -ne 0 ] && exit 1

#
# core
#

# For release change the shared/resources/version.properties isRelease tag to true
cd jodi_core
cp build/distributions/*.zip ../jodi/
RETVAL=$?
[ $RETVAL -ne 0 ] && exit 1
echo "jodi_core exists with code $RETVAL"
gradle vd
RETVAL=$?
[ $RETVAL -ne 0 ] && exit 1
cd ..

# should use libraries compiled using ODI11g libraries
# cd jodi_cache_poc
# sh gradle distZip
# cp build/distributions/*.zip ../jodi/
# RETVAL=$?
# [ $RETVAL -ne 0 ] && exit 1
# echo "jodi_cache_poc added to build target with code $RETVAL"
# cd ..

# cd jodi_tools
# sh gradle distZip
# cp build/distributions/*.zip ../jodi/
# RETVAL=$?
# [ $RETVAL -ne 0 ] && exit 1
# echo "jodi_tools exists with code $RETVAL"
# cd ..

#validate ETL Command Line
sh ./validateETLCmdLine.sh
RETVAL=$?
[ $RETVAL -ne 0 ] && exit 1

#### git
# git pull origin master
# git add ${APATH}/shared/resources/version.properties
# git commit -m "Update version from createDist.sh"
# git push origin master
### end git

echo "Finished Successfully." 
