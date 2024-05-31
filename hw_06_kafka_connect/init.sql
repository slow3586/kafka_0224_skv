CREATE TABLE IF NOT EXISTS products
(
    id    uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
    name  character varying(1000),
    count int
);