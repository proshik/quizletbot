BEGIN TRANSACTION;

drop table if exists notification cascade;

create table if not exists notification
(
  id           serial primary key,
  created_date timestamp default now(),
  type         text    NOT NULL,
  day_of_week  text    NOT NULL,
  hour         integer NOT NULL,
  user_id      bigint  not null
    constraint notification_users_id_fk
    references users,
  constraint notification_uniq unique (type, day_of_week, hour, user_id)
);

END TRANSACTION;