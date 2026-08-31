alter table employee_master add column if not exists photo_path varchar(1000);
alter table employee_master add column if not exists mobile_photo_path varchar(1000);

create table if not exists employee_profile (
    id bigserial primary key,
    employee_id bigint not null,
    dob date,
    gender varchar(20),
    alternate_mobile_no varchar(15),
    pan_no varchar(10),
    marital_status varchar(30),
    spouse_name varchar(100),
    marriage_date date,
    blood_group varchar(10),
    emergency_contact_name varchar(100),
    emergency_contact_no varchar(15),
    current_address varchar(1000),
    permanent_address varchar(1000),
    photo_path varchar(1000),
    created_date timestamp not null default current_timestamp,
    updated_date timestamp not null default current_timestamp,
    created_by varchar(255),
    updated_by varchar(255),
    constraint uk_employee_profile_employee unique (employee_id),
    constraint fk_employee_profile_employee foreign key (employee_id) references employee_master (employee_id),
    constraint chk_employee_profile_pan check (pan_no is null or pan_no = '' or pan_no ~ '^[A-Z]{5}[0-9]{4}[A-Z]$')
);

create index if not exists idx_employee_profile_employee_id on employee_profile (employee_id);
