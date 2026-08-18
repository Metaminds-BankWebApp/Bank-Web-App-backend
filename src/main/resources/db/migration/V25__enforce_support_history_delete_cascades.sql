-- Some development databases created the support tables through Hibernate before
-- V23 ran. Hibernate's generated foreign keys did not include the delete rules
-- required by the support-history lifecycle. Resolve the foreign-key names
-- dynamically, then recreate the intended rules.
DO $$
DECLARE
    existing_constraint TEXT;
BEGIN
    SELECT conname INTO existing_constraint
    FROM pg_constraint
    WHERE conrelid = 'support_conversations'::regclass
      AND contype = 'f'
      AND confrelid = 'users'::regclass
      AND conkey = ARRAY[(SELECT attnum FROM pg_attribute WHERE attrelid = 'support_conversations'::regclass AND attname = 'created_by_user_id')];
    IF existing_constraint IS NOT NULL THEN
        EXECUTE format('ALTER TABLE support_conversations DROP CONSTRAINT %I', existing_constraint);
    END IF;
    ALTER TABLE support_conversations
        ADD CONSTRAINT fk_support_conversations_created_by
        FOREIGN KEY (created_by_user_id) REFERENCES users(user_id) ON DELETE CASCADE;

    SELECT conname INTO existing_constraint
    FROM pg_constraint
    WHERE conrelid = 'support_messages'::regclass
      AND contype = 'f'
      AND confrelid = 'support_conversations'::regclass
      AND conkey = ARRAY[(SELECT attnum FROM pg_attribute WHERE attrelid = 'support_messages'::regclass AND attname = 'conversation_id')];
    IF existing_constraint IS NOT NULL THEN
        EXECUTE format('ALTER TABLE support_messages DROP CONSTRAINT %I', existing_constraint);
    END IF;
    ALTER TABLE support_messages
        ADD CONSTRAINT fk_support_messages_conversation
        FOREIGN KEY (conversation_id) REFERENCES support_conversations(conversation_id) ON DELETE CASCADE;

    SELECT conname INTO existing_constraint
    FROM pg_constraint
    WHERE conrelid = 'support_messages'::regclass
      AND contype = 'f'
      AND confrelid = 'users'::regclass
      AND conkey = ARRAY[(SELECT attnum FROM pg_attribute WHERE attrelid = 'support_messages'::regclass AND attname = 'sender_user_id')];
    IF existing_constraint IS NOT NULL THEN
        EXECUTE format('ALTER TABLE support_messages DROP CONSTRAINT %I', existing_constraint);
    END IF;
    ALTER TABLE support_messages
        ADD CONSTRAINT fk_support_messages_sender
        FOREIGN KEY (sender_user_id) REFERENCES users(user_id) ON DELETE SET NULL;

    SELECT conname INTO existing_constraint
    FROM pg_constraint
    WHERE conrelid = 'support_message_reads'::regclass
      AND contype = 'f'
      AND confrelid = 'support_messages'::regclass
      AND conkey = ARRAY[(SELECT attnum FROM pg_attribute WHERE attrelid = 'support_message_reads'::regclass AND attname = 'message_id')];
    IF existing_constraint IS NOT NULL THEN
        EXECUTE format('ALTER TABLE support_message_reads DROP CONSTRAINT %I', existing_constraint);
    END IF;
    ALTER TABLE support_message_reads
        ADD CONSTRAINT fk_support_message_reads_message
        FOREIGN KEY (message_id) REFERENCES support_messages(message_id) ON DELETE CASCADE;

    SELECT conname INTO existing_constraint
    FROM pg_constraint
    WHERE conrelid = 'support_message_reads'::regclass
      AND contype = 'f'
      AND confrelid = 'users'::regclass
      AND conkey = ARRAY[(SELECT attnum FROM pg_attribute WHERE attrelid = 'support_message_reads'::regclass AND attname = 'reader_user_id')];
    IF existing_constraint IS NOT NULL THEN
        EXECUTE format('ALTER TABLE support_message_reads DROP CONSTRAINT %I', existing_constraint);
    END IF;
    ALTER TABLE support_message_reads
        ADD CONSTRAINT fk_support_message_reads_reader
        FOREIGN KEY (reader_user_id) REFERENCES users(user_id) ON DELETE CASCADE;
END $$;
