BEGIN TRANSACTION;

DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS notification CASCADE;
DROP TABLE IF EXISTS account CASCADE;

/* ------------------------------------------- */

create table if not exists users
(
  id           serial primary key,
  created_date timestamp default now(),
  chat_id      bigint UNIQUE  NOT NULL,
  login        text UNIQUE    NOT NULL,
  access_token text
  --   enabled_modes        text           NOT NULL,
  --   operation_data jsonb,
);


END TRANSACTION;