--
-- -
-- Create Profile and unlock users
-- -
-- Usage sqlplus sys/<passwd>@hostname:port/service_name as sysdba @create_profile.sql
--

declare
l_password varchar2(30);
BEGIN
  dbms_output.enable(null);
  FOR i IN
  (SELECT username
   FROM dba_users
   WHERE (username LIKE '%ODI_REPO%'
          -- OR username LIKE 'SYS%'
          OR username LIKE 'OBI%'
          OR username LIKE 'DWH%'
          OR username LIKE 'OHI%'
          OR username LIKE 'REF%'
          OR username LIKE '%HCD%'
          OR username LIKE 'MC_REPORTS%'
          OR username LIKE 'CDM%'
          OR username LIKE 'CHINOOK%'
          OR username like '%HCD%'
          OR username like '%OHI%'
          OR username like '%ODI%REPO%'
          OR username like '%STB%'
          OR username like '%REPO%'
          OR username like 'DEV%'
          OR username like 'HCD%'
          OR username like 'HDM%'
          OR username like 'HCD%'
          OR username like 'OHADI%'
          OR username like 'OHF%'
          OR username like 'OACS%'
          OR username like 'ESCS%'
          OR username like 'BICS%'
          OR username like 'M12%'
         OR username like 'ODITMP'
         OR username like 'MTM'
         OR username like 'MCM'
         OR username like 'ERM'
         OR username like 'INF'
         OR username like 'MCV'
         OR username like 'DWHPUBLIC'
         OR username like 'EXN%'
         OR username like 'STG%'
   )
  )
   LOOP
       EXECUTE immediate 'alter user '||i.username||' profile c##core account unlock' ;
       IF i.username IN ('REF') OR i.username like 'DEV%' or i.username like '%ODI_REPO' or i.username like 'DWH%' THEN
             EXECUTE immediate 'alter user '||i.username||' identified by xxxx profile c##core account unlock' ;
             EXECUTE immediate 'alter user '||i.username||' identified by &password profile c##core account unlock' ;
       END IF;
    END LOOP;
END;
/