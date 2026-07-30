alter table leave_application
add column if not exists comp_off_work_date date;

create index if not exists idx_leave_application_compoff_lookup
on leave_application (employee_id, comp_off_work_date, status)
where comp_off_work_date is not null;

insert into leave_master (leave_name, leave_code)
select 'Comp Off', 'CO'
where not exists (
    select 1
    from leave_master
    where upper(regexp_replace(coalesce(leave_code, ''), '[^A-Za-z0-9]', '', 'g')) in ('CO', 'COMPOFF')
       or upper(regexp_replace(coalesce(leave_name, ''), '[^A-Za-z0-9]', '', 'g')) in ('CO', 'COMPOFF', 'COMPENSATORYOFF')
);
