create extension if not exists pgcrypto;

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values (
    'avatars',
    'avatars',
    true,
    5242880,
    array['image/jpeg', 'image/png', 'image/webp']
)
on conflict (id) do update
set
    public = excluded.public,
    file_size_limit = excluded.file_size_limit,
    allowed_mime_types = excluded.allowed_mime_types;

drop policy if exists "Public can read profile avatars" on storage.objects;
drop policy if exists "Users can upload their own avatar" on storage.objects;
drop policy if exists "Users can update their own avatar" on storage.objects;
drop policy if exists "Users can delete their own avatar" on storage.objects;

create policy "Public can read profile avatars"
on storage.objects
for select
using (bucket_id = 'avatars');

create policy "Users can upload their own avatar"
on storage.objects
for insert
to authenticated
with check (
    bucket_id = 'avatars'
    and (storage.foldername(name))[1] = auth.uid()::text
);

create policy "Users can update their own avatar"
on storage.objects
for update
to authenticated
using (
    bucket_id = 'avatars'
    and (storage.foldername(name))[1] = auth.uid()::text
)
with check (
    bucket_id = 'avatars'
    and (storage.foldername(name))[1] = auth.uid()::text
);

create policy "Users can delete their own avatar"
on storage.objects
for delete
to authenticated
using (
    bucket_id = 'avatars'
    and (storage.foldername(name))[1] = auth.uid()::text
);

create table if not exists public.trips (
    id uuid primary key default gen_random_uuid(),
    code text not null,
    name text not null default 'Viaje sin nombre',
    description text not null default '',
    owner_id text not null,
    active boolean not null default true,
    created_at bigint not null default ((extract(epoch from now()) * 1000)::bigint)
);

create table if not exists public.locations (
    trip_id uuid not null references public.trips(id) on delete cascade,
    user_id text not null,
    display_name text not null default '',
    latitude double precision not null,
    longitude double precision not null,
    status text not null default 'ok',
    help_message text not null default '',
    updated_at bigint not null default ((extract(epoch from now()) * 1000)::bigint),
    primary key (trip_id, user_id)
);

create table if not exists public.location_points (
    trip_id uuid not null references public.trips(id) on delete cascade,
    user_id text not null,
    display_name text not null default '',
    latitude double precision not null,
    longitude double precision not null,
    status text not null default 'ok',
    help_message text not null default '',
    created_at bigint not null default ((extract(epoch from now()) * 1000)::bigint),
    primary key (trip_id, user_id, created_at)
);

create index if not exists location_points_trip_created_at_idx
on public.location_points (trip_id, created_at);

alter table public.trips enable row level security;
alter table public.locations enable row level security;
alter table public.location_points enable row level security;

with ranked_trips as (
    select
        id,
        row_number() over (partition by code order by created_at desc, id desc) as row_number
    from public.trips
    where active = true
)
update public.trips
set active = false
where id in (
    select id
    from ranked_trips
    where row_number > 1
);

alter table public.trips drop constraint if exists trips_code_key;

drop index if exists public.trips_active_code_unique;

create unique index trips_active_code_unique
on public.trips (code)
where active = true;

drop policy if exists "Authenticated riders can read active trips" on public.trips;
drop policy if exists "Riders can create their own trips" on public.trips;
drop policy if exists "Owners can update their trips" on public.trips;
drop policy if exists "Authenticated riders can read locations" on public.locations;
drop policy if exists "Riders can share their own location" on public.locations;
drop policy if exists "Riders can update their own location" on public.locations;
drop policy if exists "Riders can delete their own location" on public.locations;
drop policy if exists "Authenticated riders can read location points" on public.location_points;
drop policy if exists "Riders can share their own location points" on public.location_points;

create policy "Authenticated riders can read active trips"
on public.trips
for select
to authenticated
using (active = true or owner_id = auth.uid()::text);

create policy "Riders can create their own trips"
on public.trips
for insert
to authenticated
with check (owner_id = auth.uid()::text);

create policy "Owners can update their trips"
on public.trips
for update
to authenticated
using (owner_id = auth.uid()::text)
with check (owner_id = auth.uid()::text);

create policy "Authenticated riders can read locations"
on public.locations
for select
to authenticated
using (
    exists (
        select 1
        from public.trips
        where trips.id = locations.trip_id
    )
);

create policy "Riders can share their own location"
on public.locations
for insert
to authenticated
with check (user_id = auth.uid()::text);

create policy "Riders can update their own location"
on public.locations
for update
to authenticated
using (user_id = auth.uid()::text)
with check (user_id = auth.uid()::text);

create policy "Riders can delete their own location"
on public.locations
for delete
to authenticated
using (user_id = auth.uid()::text);

create policy "Authenticated riders can read location points"
on public.location_points
for select
to authenticated
using (
    exists (
        select 1
        from public.trips
        where trips.id = location_points.trip_id
    )
);

create policy "Riders can share their own location points"
on public.location_points
for insert
to authenticated
with check (user_id = auth.uid()::text);
