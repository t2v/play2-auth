CREATE TABLE users (
  id serial PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  avatar_url VARCHAR(1000) NOT NULL
);

CREATE TABLE github_users (
  user_id INTEGER NOT NULL UNIQUE REFERENCES users(id),
  id bigint PRIMARY KEY,
  login VARCHAR(100) NOT NULL,
  avatar_url VARCHAR(1000) NOT NULL,
  access_token VARCHAR(1000) NOT NULL
);

CREATE TABLE twitter_users (
  user_id INTEGER NOT NULL UNIQUE REFERENCES users(id),
  id bigint PRIMARY KEY,
  screen_name VARCHAR(100) NOT NULL,
  profile_image_url VARCHAR(1000) NOT NULL,
  access_token VARCHAR(1000) NOT NULL,
  access_token_secret VARCHAR(1000) NOT NULL
);

CREATE TABLE facebook_users (
  user_id INTEGER NOT NULL UNIQUE REFERENCES users(id),
  id VARCHAR(100) PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  cover_url VARCHAR(1000) NOT NULL,
  access_token VARCHAR(1000) NOT NULL
);

CREATE TABLE slack_access_token (
  user_id INTEGER NOT NULL UNIQUE REFERENCES users(id),
  access_token VARCHAR(1000) NOT NULL
);