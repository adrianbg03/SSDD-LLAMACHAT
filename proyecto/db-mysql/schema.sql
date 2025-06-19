CREATE SCHEMA IF NOT EXISTS ssdd;
USE ssdd;

CREATE TABLE IF NOT EXISTS users(
	id varchar(50),
       	email varchar(50),
	password_hash text,
       	name text,
	token text,
	visits int,
	PRIMARY KEY(id)
);
CREATE TABLE IF NOT EXISTS conversations (
    dialogue_id VARCHAR(50) PRIMARY KEY,
    user_id VARCHAR(50),
    dname VARCHAR(100),
    status ENUM('READY', 'BUSY', 'FINISHED') DEFAULT 'READY',
    dialogue JSON, -- array de objetos {prompt, answer, timestamp, next}
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
-- Para búsquedas con email
CREATE INDEX user_email_idx ON users (email);

-- CUIDADO!! AÑADO UN USUARIO PARA PROBAR, PASSWORD: "admin"
INSERT INTO users VALUES ("dsevilla", "dsevilla@um.es", "21232f297a57a5a743894a0e4a801fc3", "diego", "TOKEN", 0);

