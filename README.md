# Question Bank Backend

A Spring Boot backend for managing question-bank subjects, user accounts, and quizzes.

## Requirements

- Java 17
- MySQL

The application connects to `dev_db` on `127.0.0.1:3306` by default. Override the
database connection with `SPRING_DATASOURCE_URL` if needed. Set `DB_USERNAME` and
`DB_PASSWORD` for the database credentials. The username defaults to `root`; the
password defaults to empty.

Operator registration is disabled unless `OPERATOR_SIGNUP_CODE` is configured.

## Run

```sh
./mvnw spring-boot:run
```

## Test

```sh
./mvnw test
```
