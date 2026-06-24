-- Allow STICKER as a message type
ALTER TABLE messages DROP CONSTRAINT IF EXISTS messages_type_check;
ALTER TABLE messages ADD CONSTRAINT messages_type_check
    CHECK (type IN ('TEXT','IMAGE','FILE','VOICE','SYSTEM','STICKER'));
