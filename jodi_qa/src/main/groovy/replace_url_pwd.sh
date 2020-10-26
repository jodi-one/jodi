#!/bin/bash
# created by Duke
# script to replace connect url and password in file generated from liquibase
#
# usage
# sh replace_password_url.sh localhost:1521/ORCL <password for db> legacy.sql
# sh replace_password_url.sh localhost:1521/ORCL <password for db> update.sql
#
if [ -z "$1" ]
then
      echo "\$1 is empty and should be set to url (e.g. localhost:1521/ORCL"
      exit 1
else
      echo "url set to $1"
fi

if [ -z "$2" ]
then
      echo "\$2 is empty and should be set to the password"
      exit 1
else
      echo "Password is set, is it correct?"
fi

if [ -z "$3" ]
then
      echo "\$3 is empty and should be set to the file it should do the replacement in"
      exit 1
else
      echo "File to replace string in is set to $3"
fi

sed -i -e "s/localhost:1521\/ORCL/$1/g" $3
sed -i -e "s/LETMEIN/$2/g" $3