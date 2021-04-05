whenever sqlerror continue

DROP USER "&1" cascade ;

create role DWH_PUBLIC_ROLE;
create role DWH_PERSONAL_ROLE;
create role DWH_SBX_ROLE;

whenever sqlerror exit failure


-- usage; create_user.sql username password tablespace temptablespace
-- USER SQL
CREATE USER "&1" IDENTIFIED BY "&2"
DEFAULT TABLESPACE "&3"
TEMPORARY TABLESPACE "&4";

-- QUOTAS
ALTER USER "&1" QUOTA UNLIMITED ON "USERS";

-- ROLES

-- SYSTEM PRIVILEGES
GRANT CREATE TRIGGER TO "&1" ;
GRANT CREATE MATERIALIZED VIEW TO "&1" ;
GRANT CREATE DIMENSION TO "&1" ;
GRANT CREATE INDEXTYPE TO "&1" ;
GRANT CREATE MEASURE FOLDER TO "&1" ;
GRANT CREATE VIEW TO "&1" ;
GRANT CREATE SESSION TO "&1" ;
GRANT CREATE TABLE TO "&1" ;
GRANT CREATE TYPE TO "&1" ;
GRANT CREATE TABLESPACE TO "&1" ;
GRANT ADVISOR TO "&1" ;
GRANT CREATE ATTRIBUTE DIMENSION TO "&1" ;
GRANT CREATE ROLLBACK SEGMENT TO "&1" ;
GRANT CREATE SYNONYM TO "&1" ;
GRANT CREATE SEQUENCE TO "&1" ;
GRANT CREATE HIERARCHY TO "&1" ;
GRANT CREATE CUBE BUILD PROCESS TO "&1" ;
GRANT CREATE PROFILE TO "&1" ;
GRANT CREATE MINING MODEL TO "&1" ;
GRANT CREATE RULE SET TO "&1" ;
GRANT CREATE CLUSTER TO "&1" ;
GRANT CREATE PROCEDURE TO "&1" ;
GRANT CREATE CUBE DIMENSION TO "&1" ;
GRANT CREATE CUBE TO "&1" ;

GRANT "CONNECT" TO "&1";
GRANT "RESOURCE" TO "&1";
GRANT "SELECT_CATALOG_ROLE" TO "&1";
GRANT "OEM_ADVISOR" TO "&1";
GRANT "XDBADMIN" TO "&1";


GRANT CREATE SESSION TO "&1";
GRANT ALTER SESSION TO "&1";
GRANT UNLIMITED TABLESPACE TO "&1";
GRANT CREATE TABLE TO "&1";
GRANT CREATE ANY TABLE TO "&1";
GRANT ALTER ANY TABLE TO "&1";
GRANT DROP ANY TABLE TO "&1";
GRANT COMMENT ANY TABLE TO "&1";
GRANT CREATE ANY INDEX TO "&1";
GRANT ALTER ANY INDEX TO "&1";
GRANT DROP ANY INDEX TO "&1";
GRANT CREATE SYNONYM TO "&1";
GRANT CREATE VIEW TO "&1";
GRANT CREATE ANY VIEW TO "&1";
GRANT DROP ANY VIEW TO "&1";
GRANT CREATE SEQUENCE TO "&1";
GRANT CREATE ANY SEQUENCE TO "&1";
GRANT DROP ANY SEQUENCE TO "&1";
GRANT CREATE DATABASE LINK TO "&1";
GRANT CREATE PROCEDURE TO "&1";
GRANT CREATE ANY PROCEDURE TO "&1";
GRANT DROP ANY PROCEDURE TO "&1";
GRANT CREATE TRIGGER TO "&1";
GRANT ANALYZE ANY TO "&1";
GRANT CREATE ANY MATERIALIZED VIEW TO "&1";
GRANT ALTER ANY MATERIALIZED VIEW TO "&1";
GRANT DROP ANY MATERIALIZED VIEW TO "&1";
GRANT CREATE ANY DIRECTORY TO "&1";
GRANT DROP ANY DIRECTORY TO "&1";
GRANT CREATE TYPE TO "&1";
GRANT QUERY REWRITE TO "&1";
GRANT GLOBAL QUERY REWRITE TO "&1";
GRANT ON COMMIT REFRESH TO "&1";
GRANT DEBUG CONNECT SESSION TO "&1";
grant select_catalog_role to "&1" with admin option;
grant select on dba_segments to "&1" with grant option;
grant select on dba_dependencies to "&1" with grant option;
exit