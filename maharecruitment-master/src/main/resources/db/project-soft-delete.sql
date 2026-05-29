alter table project_mst
    add column if not exists active_flag varchar(1) not null default 'Y';

update project_mst
set active_flag = 'Y'
where active_flag is null;

create index if not exists idx_project_mst_active_flag
    on project_mst (active_flag);

do $$
begin
    if not exists (
        select 1
        from information_schema.table_constraints
        where constraint_name = 'ck_project_mst_active_flag'
          and table_name = 'project_mst'
    ) then
        alter table project_mst
            add constraint ck_project_mst_active_flag
            check (active_flag in ('Y', 'N'));
    end if;
end $$;
