--liquibase formatted sql

--changeset skullking:1
-- version is here used as optimistic locker
CREATE TABLE IF NOT EXISTS STREAMS
(
    uuid VARCHAR(50) NOT NULL,
    version int NOT NULL,
    PRIMARY KEY (uuid)
);

CREATE TABLE IF NOT EXISTS EVENTS
(
    sequence_num BIGSERIAL NOT NULL,
    stream_id VARCHAR(50) NOT NULL,
    version INT NOT NULL,
    data JSON NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (sequence_num),
    UNIQUE (stream_id, version),
    FOREIGN KEY (stream_id)
        REFERENCES STREAMS (uuid)
);

--rollback DROP TABLE EVENTS;
--rollback DROP TABLE STREAMS;
