alter table agents
    add column if not exists deleted boolean not null default false,
    add column if not exists deleted_at timestamp with time zone;

alter table machines
    add column if not exists deleted boolean not null default false,
    add column if not exists deleted_at timestamp with time zone;
