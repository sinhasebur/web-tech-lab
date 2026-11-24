package com.example.profile;

import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class AccountRepository {

    private final JdbcClient jdbc;

    public AccountRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void createAccount(String username, String passwordHash) {
        jdbc.sql("INSERT INTO accounts (username, password_hash) VALUES (:username, :passwordHash)")
                .param("username", username)
                .param("passwordHash", passwordHash)
                .update();
    }

    public Optional<String> findPasswordHash(String username) {
        return jdbc.sql("SELECT password_hash FROM accounts WHERE username = :username")
                .param("username", username)
                .query(String.class)
                .optional();
    }

    public Optional<Profile> findProfile(String username) {
        return jdbc.sql("""
                SELECT phone_number, address FROM accounts
                WHERE username = :username AND phone_number IS NOT NULL
                """)
                .param("username", username)
                .query((row, rowNumber) -> new Profile(row.getString("phone_number"), row.getString("address")))
                .optional();
    }

    // An account starts with empty contact details. The changed row count tells us
    // whether the matching profile was in the right state for the operation.
    public boolean createProfile(String username, Profile profile) {
        return jdbc.sql("""
                UPDATE accounts SET phone_number = :phoneNumber, address = :address
                WHERE username = :username AND phone_number IS NULL
                """)
                .param("username", username)
                .param("phoneNumber", profile.phoneNumber())
                .param("address", profile.address())
                .update() == 1;
    }

    public boolean updateProfile(String username, Profile profile) {
        return jdbc.sql("""
                UPDATE accounts SET phone_number = :phoneNumber, address = :address
                WHERE username = :username AND phone_number IS NOT NULL
                """)
                .param("username", username)
                .param("phoneNumber", profile.phoneNumber())
                .param("address", profile.address())
                .update() == 1;
    }

    public boolean deleteProfile(String username) {
        return jdbc.sql("""
                UPDATE accounts SET phone_number = NULL, address = NULL
                WHERE username = :username AND phone_number IS NOT NULL
                """)
                .param("username", username)
                .update() == 1;
    }
}
