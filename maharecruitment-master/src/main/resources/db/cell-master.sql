create table if not exists m_cell_master (
    cell_id bigserial primary key,
    cell_name varchar(100) not null,
    active_flag varchar(1) not null default 'Y',
    created_user_id bigint,
    updated_user_id bigint,
    created_date_time timestamp,
    updated_date_time timestamp,
    constraint uk_m_cell_master_cell_name unique (cell_name),
    constraint ck_m_cell_master_active_flag check (active_flag in ('Y', 'N'))
);

create unique index if not exists ux_m_cell_master_cell_name_lower
    on m_cell_master (lower(cell_name));
