create table trusted_certificates (
    id uuid not null,
    alias varchar(255) not null,
    display_name varchar(255) not null,
    certificate_pem text not null,
    enabled boolean not null,
    subject_dn text not null,
    issuer_dn text not null,
    serial_number varchar(255) not null,
    not_before timestamp with time zone not null,
    not_after timestamp with time zone not null,
    sha256_fingerprint varchar(95) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint pk_trusted_certificates primary key (id),
    constraint uk_trusted_certificates_alias unique (alias)
);
