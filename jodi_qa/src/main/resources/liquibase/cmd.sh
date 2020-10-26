#!/bin/sh
cd /opt/git/jodi/jodi_qa/./src/main/resources/liquibase/DWH_STO;
sql -cloudconfig /opt/git/opc/src/main/resources/wallet/Wallet_DB202007280549.zip -S DWH_STO/'DaViDGoGoGoggins42$'@DB202007280549_high @/opt/git/jodi/jodi_qa/./src/main/resources/liquibase/cmd.sql
