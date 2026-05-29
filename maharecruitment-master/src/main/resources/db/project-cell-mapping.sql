alter table project_mst
    add column if not exists cell_id bigint;

create index if not exists idx_project_mst_cell_id
    on project_mst (cell_id);

do $$
begin
    if not exists (
        select 1
        from information_schema.table_constraints
        where constraint_name = 'fk_project_mst_cell'
          and table_name = 'project_mst'
    ) then
        alter table project_mst
            add constraint fk_project_mst_cell
            foreign key (cell_id) references m_cell_master(cell_id);
    end if;
end $$;
