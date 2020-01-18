-- To be applied to container db.
DECLARE
 profile_count number(1);
BEGIN
    EXECUTE immediate 'CREATE PROFILE C##CORE LIMIT
FAILED_LOGIN_ATTEMPTS 30000  -- Account locked after x failed logins.
PASSWORD_LOCK_TIME (1 / 24 / 60 / 60) * 1     -- Number of days account is locked for. UNLIMITED required explicit unlock by DBA.
PASSWORD_LIFE_TIME 9999    -- Password expires after x days.
PASSWORD_GRACE_TIME 3    -- Grace period for password expiration.
PASSWORD_REUSE_TIME unlimited  --
PASSWORD_REUSE_MAX unlimited    -- The number of changes required before a password can be reused. UNLIMITED means never.
';
EXCEPTION WHEN OTHERS then
    if sqlcode = -2379 THEN
    EXECUTE immediate 'ALTER PROFILE C##CORE LIMIT PASSWORD_REUSE_MAX UNLIMITED';
    EXECUTE immediate 'alter profile C##CORE limit PASSWORD_REUSE_TIME unlimited';
    ELSE
    RAISE_APPLICATION_ERROR(-20101, 'Unknown error occured when creating profiles. it exit-ed with code '||sqlcode);
    end if;
select count(distinct profile) into profile_count from dba_profiles where profile = 'C##CORE';
if profile_count != 1 THEN
    RAISE_APPLICATION_ERROR(-20101, 'Profile C##CORE does not exist.');
END IF;
for i in (select * from dba_profiles where profile = 'C##CORE') loop
if i.resource_name = 'PASSWORD_REUSE_TIME' AND i.LIMIT != 'UNLIMITED' THEN
    RAISE_APPLICATION_ERROR(-20101, 'PASSWORD_REUSE_TIME should be unlimited.');
END IF;
if i.resource_name = 'PASSWORD_REUSE_MAX' AND i.LIMIT != 'UNLIMITED' THEN
    RAISE_APPLICATION_ERROR(-20101, 'PASSWORD_REUSE_MAX should be unlimited.');
END IF;
end loop;
END;
/