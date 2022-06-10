create schema todo;
SET search_path TO todo;

create table if not exists "user"
(
    id   bigserial primary key,
    name text not null
);

create table if not exists todo
(
    id    bigserial primary key,
    owner_id bigint,
    title text not null,
    tags text[],
    CONSTRAINT fk_owner
      FOREIGN KEY(owner_id) 
	  REFERENCES "user"(id)
);

create table if not exists todo_user
(
    user_id bigint NOT NULL,
    todo_id bigint NOT NULL,
    PRIMARY KEY(todo_id, user_id),
    CONSTRAINT fk_user
      FOREIGN KEY(user_id) 
	  REFERENCES "user"(id),
    CONSTRAINT fk_todo
      FOREIGN KEY(todo_id) 
	  REFERENCES todo(id)
);

insert into "user" (name) values ('Bob');
insert into "user" (name) values ('Sarah');
insert into "user" (name) values ('Alice');
insert into todo (title, owner_id, tags) values ('Learn about Loom', 1, '{ reading, web }');
