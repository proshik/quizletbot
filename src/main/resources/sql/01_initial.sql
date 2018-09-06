BEGIN TRANSACTION;

DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS account CASCADE;

/* ------------------------------------------- */

create table if not exists users
(
  id           serial primary key,
  created_date timestamp default now(),
  char_id      text UNIQUE NOT NULL
);

create table if not exists account
(
  id           serial primary key,
  created_date timestamp default now(),
  login        text UNIQUE NOT NULL,
  access_token text,
  user_id      bigint      not null
    constraint account_user_id_fk
    references users
);

END TRANSACTION;