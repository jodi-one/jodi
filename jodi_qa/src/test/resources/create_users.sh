#/bin/bash
# create all users for datavault


exnUsers=("EXN1" "EXN2" "EXN3" "EXN4" "EXN5" "EXN6" "EXN7" "EXN8" "EXN9" "EXN10"
          "EXN11" "EXN12" "EXN13" "EXN14" "EXN15" "EXN16" "EXN17" "EXN18" "EXN19" "EXN20"
          "EXN21" "EXN22" "EXN23" "EXN24" "EXN25" "EXN26" "EXN27" "EXN28" "EXN29" "EXN30"
          )


stgUsers=("STG1" "STG2" "STG3" "STG4" "STG5" "STG6" "STG7" "STG8" "STG9" "STG10"
          "STG11" "STG12" "STG13" "STG14" "STG15" "STG16" "STG17" "STG18" "STG19" "STG20"
          "STG21" "STG22" "STG23" "STG24" "STG25" "STG26" "STG27" "STG28" "STG29" "STG30"
          )

URL="jodi:1521/sample21"
LB_DB_PWD="DB_PASSWORD_MR"

for i in ${!exnUsers[@]};
do
  usr=${exnUsers[$i]}
  sql SYS/${ORCL_SYS_USER_PWD}@${URL} @create_user.sql ${usr} ${LB_DB_PWD} EXN TEMP
done

for i in ${!stgUsers[@]};
do
  usr=${stgUsers[$i]}
  sql SYS/${ORCL_SYS_USER_PWD}@${URL} @create_user.sql ${usr} ${LB_DB_PWD} STG TEMP
done

sql SYS/${ORCL_SYS_USER_PWD}@${URL}@create_user.sql ODITMP ${LB_DB_PWD} ODITMP TEMP

sql SYS/${ORCL_SYS_USER_PWD}@${URL} @create_user.sql MTM ${LB_DB_PWD} MTM TEMP

sql SYS/${ORCL_SYS_USER_PWD}@${URL} @create_user.sql MCM ${LB_DB_PWD} MCM TEMP

sql SYS/${ORCL_SYS_USER_PWD}@${URL} @create_user.sql ERM ${LB_DB_PWD} ERM TEMP

sql SYS/${ORCL_SYS_USER_PWD}@${URL} @create_user.sql INF ${LB_DB_PWD} INF TEMP

sql SYS/${ORCL_SYS_USER_PWD}@${URL} @create_user.sql MCV ${LB_DB_PWD} MCV TEMP

sql SYS/${ORCL_SYS_USER_PWD}@${URL} @create_user.sql DWHPUBLIC ${LB_DB_PWD} EDW TEMP


