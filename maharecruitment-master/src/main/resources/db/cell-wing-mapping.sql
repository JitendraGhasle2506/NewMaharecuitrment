create table if not exists m_wing_master (
    wing_id bigserial primary key,
    wing_name varchar(100) not null,
    active_flag varchar(1) not null default 'Y',
    created_user_id bigint,
    updated_user_id bigint,
    created_date_time timestamp,
    updated_date_time timestamp,
    constraint uk_m_wing_master_wing_name unique (wing_name),
    constraint ck_m_wing_master_active_flag check (active_flag in ('Y', 'N'))
);

create unique index if not exists ux_m_wing_master_wing_name_lower
    on m_wing_master (lower(wing_name));

insert into m_wing_master (wing_name, active_flag, created_date_time, updated_date_time)
select 'Default Wing', 'Y', current_timestamp, current_timestamp
where not exists (
    select 1 from m_wing_master where lower(wing_name) = lower('Default Wing')
);

alter table m_cell_master
    add column if not exists wing_id bigint;

update m_cell_master
set wing_id = (
    select wing_id
    from m_wing_master
    where lower(wing_name) = lower('Default Wing')
    order by wing_id
    fetch first 1 row only
)
where wing_id is null;

create index if not exists idx_m_cell_master_wing_id
    on m_cell_master (wing_id);

do $$
begin
    if not exists (
        select 1
        from information_schema.table_constraints
        where constraint_name = 'fk_m_cell_master_wing'
          and table_name = 'm_cell_master'
    ) then
        alter table m_cell_master
            add constraint fk_m_cell_master_wing
            foreign key (wing_id) references m_wing_master(wing_id);
    end if;
end $$;

alter table m_cell_master
    alter column wing_id set not null;
