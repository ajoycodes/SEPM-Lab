# University Management System

CSE 3220 (SEPM Lab), home task for lab 4. A Spring Boot application with a website, a JSON REST API, Spring Security (login, logout, password hashing, role-based access) and Docker Compose.

Stack: Java 17, Spring Boot 3.5, Spring Web, Spring Data JPA, Spring Security, Bean Validation, PostgreSQL 17 (H2 when run without Docker), Docker, plain HTML, CSS and JavaScript.

## Run with Docker Compose

```bash
cp .env.example .env
```

Edit `.env` and choose `DB_PASSWORD` and `ADMIN_PASSWORD` (at least 8 characters). Then:

```bash
docker compose up --build -d
```

Open http://localhost:8081 and sign in with `ADMIN_EMAIL` and `ADMIN_PASSWORD` from `.env`. The first admin is created only when the database is empty.

Stop with `docker compose down`. Add `-v` to also delete the database.

### Demo accounts

To add 60 teachers and 300 students, run this while the app is running:

```bash
python3 scripts/seed_demo.py
```

It uses the REST API, checks that all 360 logins work, and writes `demo-accounts.csv` and `DEMO-LOGINS.md`. Every demo account uses the admin password from `.env`. All the people are fictional.

## Run without Docker

```bash
./mvnw spring-boot:run
```

Open http://localhost:8080 and sign in with `admin@university.edu` / `Admin123!`. This uses an H2 database file in `data/`, separate from the Docker database. It needs Java 17 or newer.

## Who can do what

| Logged in as | `/api/admins` | `/api/teachers` | `/api/students` |
|---|---|---|---|
| Admin | Full CRUD | Full CRUD | Full CRUD |
| Teacher | 403 | Full CRUD | 403 |
| Student | 403 | 403 | Full CRUD |
| Not logged in | 401 | 401 | 401 |

- The role comes from the endpoint and can't be set in the request body. Sending `"role": "ADMIN"` to `/api/students` still creates a student.
- Teacher profiles are public. A student's roll, program and grades are visible only to that student, through their own profile.

## REST API

Replace `{resource}` with `admins`, `teachers` or `students`.

| Method | Path | Success |
|---|---|---|
| GET | `/api/{resource}` | 200, JSON array |
| GET | `/api/{resource}/{id}` | 200, JSON object |
| POST | `/api/{resource}` | 201, JSON object and `Location` header |
| PUT | `/api/{resource}/{id}` | 200, updated JSON object |
| DELETE | `/api/{resource}/{id}` | 204, no body |

Request body:

```json
{
  "name": "Nusrat Jahan",
  "email": "nusrat@university.edu",
  "password": "Student123!",
  "department": "Computer Science and Engineering"
}
```

`name`, `email` and `department` are required. `password` is required when creating; leave it out when updating to keep the current one. Teachers can also have `designation` and `specialization`. Replies never include passwords or hashes.

Errors are JSON with a `message`: 400 invalid input, 401 not logged in or wrong password, 403 not allowed or missing CSRF token, 404 no such record, 409 email or roll already used.

### Other endpoints

| Method | Path | Who | What |
|---|---|---|---|
| GET | `/api/auth/csrf` | Anyone | The CSRF token |
| POST | `/api/auth/login` | Anyone | Log in with form fields `username` (the email) and `password` |
| POST | `/api/auth/logout` | Logged in | Log out (204) |
| GET | `/api/auth/me` | Logged in | The current account |
| GET, PUT | `/api/profile/me` | Logged in | Your own full profile |
| GET | `/api/public/teachers`, `/api/public/teachers/{id}` | Anyone | Public teacher profiles |

### Calling the API from Postman or curl

CSRF protection is on, so the website sends a token automatically but other clients must do it themselves:

1. `GET /api/auth/csrf`. Keep the session cookie. The reply has `token` and `headerName`.
2. `POST /api/auth/login` with form fields `username` and `password`, the cookie, and the CSRF header.
3. `GET /api/auth/csrf` again, because Spring issues a new token after login.
4. Send the cookie on every request, and the CSRF header on every POST, PUT and DELETE.

## Website

- `/`: homepage with sign-in, and the dashboard for managing records.
- `/teachers.html`: public teacher directory with search.
- `/profile.html?teacher={id}`: a teacher's public profile.
- `/profile.html`: your own profile, which you can edit.

## Project layout

```
src/main/java/com/example/ums
├── config/       SecurityConfig (rules, login, logout, BCrypt), DataSeeder (first admin)
├── controller/   Admin, Teacher, Student, Auth and Profile controllers
├── dto/          Request and response records
├── entity/       Account, EducationEntry, SemesterResult, Role
├── exception/    ApiExceptionHandler (JSON errors)
├── repository/   AccountRepository
├── security/     AccountPrincipal, AccountUserDetailsService, CurrentUser
└── service/      AccountService, ProfileService
src/main/resources/static/   The website: HTML, CSS and JavaScript
```

One `accounts` table stores everyone, with a `role` column. Three controllers give each role its own REST collection.

## Tests

```bash
./mvnw test
```

- **6 unit tests** in `src/test/java/com/example/ums/unit/AccountServiceUnitTest.java` test `AccountService` alone, with the repository and password encoder replaced by Mockito mocks. They cover create (role and BCrypt hash), a missing password, a duplicate email, update without a password, an id from another role, and delete.
- **3 integration tests** in `src/test/java/com/example/ums/integration/UniversityApiIntegrationTest.java` start the whole app on an in-memory H2 database and go through the real login, CSRF and role checks. They cover an admin's full CRUD on a student, which resources each role can reach, and password hashing with login and logout.

Run one group with `./mvnw test -Dtest='*UnitTest'` or `./mvnw test -Dtest='*IntegrationTest'`.

GitHub Actions runs both groups on every push and pull request. The workflow is `.github/workflows/university-management-tests.yml` at the root of the repository, and the results are on the repository's Actions tab.

## Known limits

- With the rule above, a teacher can change another teacher's password, and a student can change another student's.
- Nothing stops the last admin account from being deleted, and there is no account recovery. If that happens, reset with `docker compose down -v`.
- Tables are created by Hibernate (`ddl-auto=update`), which suits a class project. A production system would use versioned migrations and HTTPS.
