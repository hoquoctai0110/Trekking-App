create table if not exists admin_audit_logs (
    id bigserial primary key,
    admin_id bigint not null,
    action varchar(100) not null,
    entity_type varchar(100) not null,
    entity_id varchar(100) not null,
    old_value text,
    new_value text,
    reason text,
    created_at timestamp not null default now()
);

create index if not exists idx_admin_audit_logs_admin_id on admin_audit_logs(admin_id);
create index if not exists idx_admin_audit_logs_created_at on admin_audit_logs(created_at desc);

create table if not exists admin_notifications (
    id bigserial primary key,
    title varchar(255) not null,
    body text not null,
    type varchar(50) not null,
    recipient_type varchar(50) not null,
    recipient_ids text,
    status varchar(50) not null,
    idempotency_key varchar(100) not null unique,
    scheduled_at timestamp null,
    sent_at timestamp null,
    created_by bigint null references users(user_id),
    created_at timestamp not null default now()
);

create index if not exists idx_admin_notifications_type on admin_notifications(type);
create index if not exists idx_admin_notifications_status on admin_notifications(status);
create index if not exists idx_admin_notifications_created_at on admin_notifications(created_at desc);

create table if not exists system_settings (
    id bigserial primary key,
    section varchar(50) not null,
    setting_key varchar(100) not null,
    setting_value text null,
    secret boolean not null default false,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),
    constraint uk_system_settings_section_key unique(section, setting_key)
);

create index if not exists idx_system_settings_section on system_settings(section);

create table if not exists admin_checkin_tokens (
    id bigserial primary key,
    tour_id bigint not null references tours(tour_id),
    schedule_id bigint null references tour_schedules(schedule_id),
    token_hash varchar(120) not null unique,
    version integer not null default 1,
    expires_at timestamp not null,
    revoked_at timestamp null,
    created_at timestamp not null default now()
);

create index if not exists idx_admin_checkin_tokens_tour_schedule on admin_checkin_tokens(tour_id, schedule_id);

create table if not exists admin_checkin_logs (
    id bigserial primary key,
    user_id bigint not null references users(user_id),
    tour_id bigint not null references tours(tour_id),
    schedule_id bigint null references tour_schedules(schedule_id),
    checkpoint varchar(255) null,
    check_in_time timestamp not null default now(),
    status varchar(50) not null,
    latitude double precision null,
    longitude double precision null
);

create index if not exists idx_admin_checkin_logs_tour_id on admin_checkin_logs(tour_id);
create index if not exists idx_admin_checkin_logs_schedule_id on admin_checkin_logs(schedule_id);
create index if not exists idx_admin_checkin_logs_status on admin_checkin_logs(status);
create index if not exists idx_admin_checkin_logs_check_in_time on admin_checkin_logs(check_in_time desc);

alter table reviews add column if not exists visible boolean not null default true;
alter table reviews add column if not exists flagged boolean not null default false;
alter table reviews add column if not exists moderation_reason text null;
alter table reviews add column if not exists deleted_at timestamp null;

create index if not exists idx_reviews_visible on reviews(visible);
create index if not exists idx_reviews_flagged on reviews(flagged);
create index if not exists idx_reviews_deleted_at on reviews(deleted_at);

alter table payments add column if not exists refund_status varchar(50) null;
alter table payments add column if not exists refund_reason text null;
alter table payments add column if not exists refund_requested_at timestamp null;
alter table payments add column if not exists refunded_at timestamp null;

create index if not exists idx_payments_refund_status on payments(refund_status);
create index if not exists idx_users_status on users(status);
create index if not exists idx_users_created_at on users(created_at desc);
create index if not exists idx_tour_providers_status on tour_providers(status);
create index if not exists idx_tour_providers_created_at on tour_providers(created_at desc);
create index if not exists idx_tours_status on tours(status);
create index if not exists idx_tours_created_at on tours(created_at desc);
create index if not exists idx_bookings_booking_status on bookings(booking_status);
create index if not exists idx_bookings_payment_status on bookings(payment_status);
create index if not exists idx_bookings_booked_at on bookings(booked_at desc);
