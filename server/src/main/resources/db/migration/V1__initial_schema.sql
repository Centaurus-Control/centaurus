create table users (
    id uuid not null,
    username varchar(255) not null,
    password_hash varchar(255) not null,
    role varchar(50) not null,
    password_change_required boolean not null default false,
    deleted boolean not null default false,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint pk_users primary key (id)
);

create table machines (
    id uuid not null,
    display_name varchar(255) not null,
    hostname varchar(255) not null,
    status varchar(50) not null,
    last_seen_at timestamp with time zone,
    primary_wol_interface_id uuid,
    wol_enabled boolean not null,
    deleted boolean not null default false,
    deleted_at timestamp with time zone,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint pk_machines primary key (id)
);

create table server_identity_keys (
    id uuid not null,
    key_id varchar(255) not null,
    public_key varchar(255) not null,
    private_key_reference varchar(255) not null,
    active boolean not null,
    created_at timestamp with time zone not null,
    revoked_at timestamp with time zone,
    constraint pk_server_identity_keys primary key (id),
    constraint uk_server_identity_keys_key_id unique (key_id)
);

create table agents (
    id uuid not null,
    machine_id uuid not null,
    installation_id uuid not null,
    display_name varchar(255) not null,
    hostname varchar(255) not null,
    agent_version varchar(255) not null,
    status varchar(50) not null,
    last_connected_at timestamp with time zone,
    last_seen_at timestamp with time zone,
    deleted boolean not null default false,
    deleted_at timestamp with time zone,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint pk_agents primary key (id),
    constraint uk_agents_machine_id unique (machine_id),
    constraint uk_agents_installation_id unique (installation_id),
    constraint fk_agents_machine foreign key (machine_id) references machines (id)
);

create table agent_capabilities (
    id uuid not null,
    agent_id uuid not null,
    capability varchar(80) not null,
    enabled boolean not null,
    constraint pk_agent_capabilities primary key (id),
    constraint uk_agent_capability unique (agent_id, capability),
    constraint fk_agent_capabilities_agent foreign key (agent_id) references agents (id)
);

create table agent_identity_keys (
    id uuid not null,
    agent_id uuid not null,
    key_id varchar(255) not null,
    public_key varchar(255) not null,
    active boolean not null,
    created_at timestamp with time zone not null,
    revoked_at timestamp with time zone,
    last_used_at timestamp with time zone,
    constraint pk_agent_identity_keys primary key (id),
    constraint uk_agent_identity_key_id unique (agent_id, key_id),
    constraint fk_agent_identity_keys_agent foreign key (agent_id) references agents (id)
);

create table enrollment_tokens (
    id uuid not null,
    token_hash varchar(255) not null,
    suggested_name varchar(255),
    expires_at timestamp with time zone not null,
    used_at timestamp with time zone,
    used_by_agent_id uuid,
    created_at timestamp with time zone not null,
    constraint pk_enrollment_tokens primary key (id),
    constraint uk_enrollment_tokens_token_hash unique (token_hash),
    constraint fk_enrollment_tokens_used_by_agent foreign key (used_by_agent_id) references agents (id)
);

create table script_manifests (
    id uuid not null,
    agent_id uuid not null,
    manifest_version bigint not null,
    manifest_hash varchar(255) not null,
    received_at timestamp with time zone not null,
    constraint pk_script_manifests primary key (id),
    constraint fk_script_manifests_agent foreign key (agent_id) references agents (id)
);

create table script_definitions (
    id uuid not null,
    agent_id uuid not null,
    script_id uuid not null,
    label varchar(255) not null,
    description text,
    manifest_version bigint not null,
    parameter_schema_json jsonb not null,
    result_schema_json jsonb not null,
    active boolean not null,
    updated_at timestamp with time zone not null,
    constraint pk_script_definitions primary key (id),
    constraint uk_agent_script_id unique (agent_id, script_id),
    constraint fk_script_definitions_agent foreign key (agent_id) references agents (id)
);

create table script_button_configurations (
    id uuid not null,
    machine_id uuid not null,
    script_definition_id uuid not null,
    label varchar(255) not null,
    enabled boolean not null,
    sort_order integer not null,
    parameters_json jsonb not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint pk_script_button_configurations primary key (id),
    constraint fk_script_button_configurations_machine foreign key (machine_id) references machines (id) on delete cascade,
    constraint fk_script_button_configurations_script_definition foreign key (script_definition_id) references script_definitions (id) on delete cascade
);

create table machine_function_assignments (
    id uuid not null,
    machine_id uuid not null,
    function_type varchar(50) not null,
    script_configuration_id uuid,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint pk_machine_function_assignments primary key (id),
    constraint uk_machine_function unique (machine_id, function_type),
    constraint fk_machine_function_assignments_machine foreign key (machine_id) references machines (id) on delete cascade,
    constraint fk_machine_function_assignments_script_configuration foreign key (script_configuration_id) references script_button_configurations (id) on delete set null
);

create table status_check_configurations (
    id uuid not null,
    machine_id uuid not null,
    script_definition_id uuid not null,
    label varchar(255) not null,
    enabled boolean not null,
    interval_seconds integer not null,
    sort_order integer not null,
    parameters_json jsonb not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint pk_status_check_configurations primary key (id),
    constraint fk_status_check_configurations_machine foreign key (machine_id) references machines (id) on delete cascade,
    constraint fk_status_check_configurations_script_definition foreign key (script_definition_id) references script_definitions (id) on delete cascade
);

create table machine_network_interfaces (
    id uuid not null,
    machine_id uuid not null,
    agent_id uuid not null,
    interface_name varchar(255) not null,
    display_name varchar(255),
    mac_address varchar(50),
    ip_address varchar(255),
    prefix_length integer,
    family varchar(20) not null,
    up boolean not null,
    loopback boolean not null,
    virtual boolean not null,
    wireless boolean not null,
    default_route boolean not null,
    wol_candidate boolean not null,
    last_seen_at timestamp with time zone not null,
    constraint pk_machine_network_interfaces primary key (id),
    constraint fk_machine_network_interfaces_machine foreign key (machine_id) references machines (id),
    constraint fk_machine_network_interfaces_agent foreign key (agent_id) references agents (id)
);

create table machine_stats_latest (
    machine_id uuid not null,
    agent_id uuid not null,
    cpu_load double precision not null,
    memory_used_percent double precision not null,
    uptime_seconds bigint not null,
    updated_at timestamp with time zone not null,
    constraint pk_machine_stats_latest primary key (machine_id),
    constraint fk_machine_stats_latest_machine foreign key (machine_id) references machines (id),
    constraint fk_machine_stats_latest_agent foreign key (agent_id) references agents (id)
);

create table machine_status_check_latest (
    id uuid not null,
    machine_id uuid not null,
    agent_id uuid not null,
    check_id uuid not null,
    label varchar(255) not null,
    healthy boolean,
    exit_code integer,
    stdout varchar(2048),
    stderr varchar(2048),
    error varchar(100),
    sort_order integer not null,
    checked_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint pk_machine_status_check_latest primary key (id),
    constraint uk_machine_status_check_latest_check unique (machine_id, check_id),
    constraint fk_machine_status_check_latest_machine foreign key (machine_id) references machines (id) on delete cascade,
    constraint fk_machine_status_check_latest_agent foreign key (agent_id) references agents (id) on delete cascade
);

create table commands (
    id uuid not null,
    command_id uuid not null,
    machine_id uuid,
    agent_id uuid,
    command_type varchar(80) not null,
    status varchar(50) not null,
    created_by_user_id uuid,
    created_at timestamp with time zone not null,
    sent_at timestamp with time zone,
    accepted_at timestamp with time zone,
    finished_at timestamp with time zone,
    hidden_from_ui boolean not null,
    payload_json jsonb not null,
    result_json jsonb,
    error_json jsonb,
    constraint pk_commands primary key (id),
    constraint uk_commands_command_id unique (command_id),
    constraint fk_commands_machine foreign key (machine_id) references machines (id),
    constraint fk_commands_agent foreign key (agent_id) references agents (id),
    constraint fk_commands_created_by_user foreign key (created_by_user_id) references users (id) on delete cascade
);

create table user_sessions (
    id uuid not null,
    user_id uuid not null,
    refresh_token_hash varchar(255) not null,
    expires_at timestamp with time zone not null,
    revoked_at timestamp with time zone,
    last_used_at timestamp with time zone,
    created_at timestamp with time zone not null,
    user_agent varchar(255),
    ip_address varchar(255),
    constraint pk_user_sessions primary key (id),
    constraint uk_user_sessions_refresh_token_hash unique (refresh_token_hash),
    constraint fk_user_sessions_user foreign key (user_id) references users (id) on delete cascade
);

create table audit_events (
    id uuid not null,
    created_at timestamp with time zone not null,
    action varchar(255) not null,
    result varchar(50) not null,
    user_id uuid,
    username varchar(255),
    target_type varchar(255),
    target_id uuid,
    target_label varchar(255),
    details_json jsonb,
    constraint pk_audit_events primary key (id),
    constraint fk_audit_events_user foreign key (user_id) references users (id) on delete set null
);
