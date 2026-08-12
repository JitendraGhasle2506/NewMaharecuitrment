-- Reference deployment SQL. The application executes the equivalent statements
-- through db.postmigration.V108__password_reset_otp_lock_support.
alter table password_reset_request
    add column if not exists otp_locked_until timestamp;

alter table password_reset_request
    alter column otp_hash drop not null;

create index if not exists idx_password_reset_otp_locked_until
    on password_reset_request (otp_locked_until);
