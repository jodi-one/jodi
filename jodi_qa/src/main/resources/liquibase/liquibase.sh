#!/bin/bash
#
if [ -z "$1" ] ;
then
	echo "a parameter is required e.g. generateChangelog";
	exit 1;
fi
cd ../../../..;

LB_COMMAND=$1;

processSchema() 
{
	echo "Processing $1 $2" ;
	gradle $1 $2
	if [ $? -eq 0 ]
	then
		echo "Processed successful $1 $2";
	else
        	echo "Failure in $1 $2";
		exit 1;
	fi
}


processSchema lb $LB_COMMAND;
