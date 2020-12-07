--liquibase formatted sql

--changeset skullking:1
-- version is here used as optimistic locker
CREATE TABLE IF NOT EXISTS STREAMS
(
    stream_id uuid NOT NULL,
    version int NOT NULL,
    PRIMARY KEY (stream_id)
);

CREATE TABLE IF NOT EXISTS EVENTS
(
    sequence_num BIGSERIAL NOT NULL,
    stream_id UUID NOT NULL,
    version INT NOT NULL,
    data JSONB NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (sequence_num),
    UNIQUE (stream_id, version),
    FOREIGN KEY (stream_id)
        REFERENCES STREAMS (stream_id)
);

--rollback DROP TABLE EVENTS;
--rollback DROP TABLE STREAMS;
