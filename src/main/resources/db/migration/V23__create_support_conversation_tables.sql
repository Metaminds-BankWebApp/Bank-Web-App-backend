CREATE TABLE IF NOT EXISTS support_conversations (
    conversation_id BIGSERIAL PRIMARY KEY,
    created_by_user_id BIGINT NOT NULL,
    category VARCHAR(60) NOT NULL,
    subject VARCHAR(160) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    last_message_preview VARCHAR(500),
    last_message_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_support_conversations_created_by
        FOREIGN KEY (created_by_user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT chk_support_conversations_status
        CHECK (status IN ('OPEN', 'IN_PROGRESS', 'CLOSED'))
);

CREATE TABLE IF NOT EXISTS support_messages (
    message_id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    sender_user_id BIGINT,
    message_text TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_support_messages_conversation
        FOREIGN KEY (conversation_id) REFERENCES support_conversations (conversation_id) ON DELETE CASCADE,
    CONSTRAINT fk_support_messages_sender
        FOREIGN KEY (sender_user_id) REFERENCES users (user_id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS support_message_reads (
    support_message_read_id BIGSERIAL PRIMARY KEY,
    message_id BIGINT NOT NULL,
    reader_user_id BIGINT NOT NULL,
    read_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_support_message_reads_message
        FOREIGN KEY (message_id) REFERENCES support_messages (message_id) ON DELETE CASCADE,
    CONSTRAINT fk_support_message_reads_reader
        FOREIGN KEY (reader_user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT uk_support_message_reads_message_reader UNIQUE (message_id, reader_user_id)
);

CREATE INDEX IF NOT EXISTS idx_support_conversations_creator_activity
    ON support_conversations (created_by_user_id, last_message_at DESC);
CREATE INDEX IF NOT EXISTS idx_support_conversations_status_activity
    ON support_conversations (status, last_message_at);
CREATE INDEX IF NOT EXISTS idx_support_messages_conversation_created
    ON support_messages (conversation_id, created_at);
CREATE INDEX IF NOT EXISTS idx_support_message_reads_reader
    ON support_message_reads (reader_user_id, message_id);
