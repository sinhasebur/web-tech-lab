CREATE TABLE accounts (
    username VARCHAR(50) PRIMARY KEY,
    password_hash VARCHAR(255) NOT NULL,
    phone_number VARCHAR(30),
    address VARCHAR(200)
);
