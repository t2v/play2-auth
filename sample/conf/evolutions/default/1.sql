# Account schema
 
# --- !Ups

CREATE TABLE account (
    id         integer NOT NULL PRIMARY KEY,
    email      text NOT NULL UNIQUE,
    password   text NOT NULL,
    name       text NOT NULL,
    permission text NOT NULL
);

# --- !Downs

DROP TABLE account;

