CREATE TABLE time_slot (
    id               BIGSERIAL PRIMARY KEY,
    start_time       TIMESTAMP NOT NULL,
    duration_minutes INTEGER   NOT NULL,
    booked           BOOLEAN   NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP NOT NULL,
    CONSTRAINT time_slot_duration_range CHECK (duration_minutes BETWEEN 10 AND 240)
);

CREATE INDEX time_slot_start_time_idx ON time_slot (start_time);

CREATE TABLE booking (
    id           BIGSERIAL PRIMARY KEY,
    time_slot_id BIGINT       NOT NULL UNIQUE REFERENCES time_slot (id),
    name         VARCHAR(100) NOT NULL,
    email        VARCHAR(255) NOT NULL,
    topic        VARCHAR(280),
    created_at   TIMESTAMP    NOT NULL
);
