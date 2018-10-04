BEGIN TRANSACTION;

DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS notification CASCADE;
DROP TABLE IF EXISTS account CASCADE;

/* ------------------------------------------- */

create table if not exists users
(
  id           serial primary key,
  created_date timestamp default now(),
  chat_id      bigint UNIQUE NOT NULL
);

create table if not exists account
(
  id             serial primary key,
  created_date   timestamp default now(),
  login          text UNIQUE    NOT NULL,
  access_token   text,
  enabled_modes  text           NOT NULL,
  operation_data jsonb,
  user_id        bigint unique  not null
    constraint account_user_id_fk
    references users
);

create table if not exists notification
(
  id           serial primary key,
  created_date timestamp default now(),
  type         text    NOT NULL,
  day_of_week  text    NOT NULL,
  hour         integer NOT NULL,
  account_id   bigint  not null
    constraint notification_account_id_fk
    references account,
  constraint notification_uniq unique (type, day_of_week, hour, account_id)
);


END TRANSACTION;