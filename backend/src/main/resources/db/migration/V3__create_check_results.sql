CREATE TABLE check_results (
        id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
        monitor_id       BIGINT       NOT NULL REFERENCES monitors (id) ON DELETE CASCADE,
        checked_at       TIMESTAMPTZ  NOT NULL,
        status           VARCHAR(10)  NOT NULL,
        response_time_ms BIGINT       NOT NULL,
        error            VARCHAR(500),
        CONSTRAINT chk_check_results_status CHECK (status IN ('UP', 'DOWN'))
);

CREATE INDEX idx_check_results_monitor_checked ON check_results (monitor_id, checked_at DESC);