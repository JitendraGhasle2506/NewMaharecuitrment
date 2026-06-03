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
