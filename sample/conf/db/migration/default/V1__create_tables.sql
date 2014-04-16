CREATE TABLE account (
    id         integer NOT NULL PRIMARY KEY,
    email      varchar NOT NULL UNIQUE,
    password   varchar NOT NULL,
    name       varchar NOT NULL,
    permission varchar NOT NULL
);
