#!/bin/sh
cd /opt/git/jodi/jodi_qa/./src/main/resources/liquibase/DWH_STO;
sql -cloudconfig /opt/git/opc/src/main/resources/wallet/Wallet_JODI2010270733.zip -S DWH_STO/'DaViDGoGoGoggins42$'@JODI2010270733_high @/opt/git/jodi/jodi_qa/./src/main/resources/liquibase/cmd.sql
