alter table users
add column if not exists password_change_required boolean;

update users
set password_change_required = (last_login_at is null)
where password_change_required is null;

alter table users
alter column password_change_required set default true;

alter table users
alter column password_change_required set not null;
