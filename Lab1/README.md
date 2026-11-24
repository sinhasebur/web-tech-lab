# Profile API

A small Spring Boot app where you can register, log in, and manage one profile with a phone number and an address. H2 runs in memory, so accounts and profiles reset when you restart the app.

## Run locally

Install Java 17 or later and Maven, then run this command from the project folder.

```sh
mvn spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080) in your browser. Create an account, log in, and use the contact details screen to add, view, edit, or delete your profile.

## API endpoints

| Request | Body | Success |
| --- | --- | --- |
| `POST /register` | JSON with `username` and `password` | `201 Created` |
| `POST /login` | Form fields `username` and `password` | `204 No Content` |
| `POST /profile` | JSON with `phoneNumber` and `address` | `201 Created` |
| `GET /profile` | None | `200 OK` with the saved details |
| `PUT /profile` | JSON with both profile fields | `200 OK` with the updated details |
| `DELETE /profile` | None | `204 No Content` |
| `POST /logout` | None | `204 No Content` |

The session identifies which profile belongs to the logged-in user. Reading a missing profile returns `404`. Creating a second profile returns `409`. The browser sends the session cookie automatically. The frontend calls `GET /csrf` before each change and includes the returned token in the request header.

## Run the integration tests

```sh
mvn test
```

`ProfileIntegrationTest.java` uses MockMvc to send requests through Spring Security and the controllers with a separate in-memory H2 database. One test registers, logs in, and verifies that contact details can be created, read, updated, and deleted. The other expects `401 Unauthorized` when a logged-out user tries to read the profile.

## Read the code

1. `src/main/resources/static/index.html` defines the forms. The `app.js` file in the same folder uses `fetch` to call the API.
2. `ProfileController.java` maps the four profile endpoints. Each method gets the logged-in username from `Principal`.
3. `AccountRepository.java` runs the SQL using that username. The table in `schema.sql` keeps one row per account. Deleting a profile clears the contact fields in that row.
4. `AccountController.java` validates registration and uses Spring Security to hash the password. `SecurityConfig.java` handles login and logout and controls which endpoints require a session.
