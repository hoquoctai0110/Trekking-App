alter table tours
    add column if not exists cover_image_url varchar(1000),
    add column if not exists cover_image_public_id varchar(255);

create table if not exists tour_images (
    image_id bigserial primary key,
    tour_id bigint not null references tours(tour_id) on delete cascade,
    image_url varchar(1000) not null,
    public_id varchar(255) not null,
    display_order integer not null default 0,
    is_cover boolean not null default false,
    created_at timestamp not null default current_timestamp
);

create index if not exists idx_tour_images_tour_id on tour_images (tour_id);

create table if not exists community_posts (
    post_id bigserial primary key,
    author_id bigint not null references users(user_id) on delete cascade,
    content text not null,
    status varchar(50) not null default 'ACTIVE',
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp
);

create index if not exists idx_community_posts_author_id on community_posts (author_id);
create index if not exists idx_community_posts_status on community_posts (status);

create table if not exists community_post_images (
    image_id bigserial primary key,
    post_id bigint not null references community_posts(post_id) on delete cascade,
    image_url varchar(1000) not null,
    public_id varchar(255) not null,
    display_order integer not null default 0,
    created_at timestamp not null default current_timestamp
);

create index if not exists idx_community_post_images_post_id on community_post_images (post_id);
