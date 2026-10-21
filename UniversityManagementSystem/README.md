# University Management System

CSE 3220 (SEPM Lab), home task for lab 4. A Spring Boot REST API with three roles (Admin, Teacher, Student), Spring Security login and logout, BCrypt password hashing, role-based access control, Docker Compose, and a Swagger UI page for trying it in the browser.

Stack: Java 17, Spring Boot 3.5, Spring Web, Spring Data JPA, Spring Security, Bean Validation, springdoc-openapi (Swagger UI), PostgreSQL 17, Docker.

## Run it

```bash
docker compose up --build
```

This starts two containers: `db` (PostgreSQL) and `app` (this API on http://localhost:8080). The app waits until the database is healthy. On the first start it creates one account per role:

| Username | Password | Role |
|---|---|---|
| admin | admin123 | ADMIN |
| teacher | teacher123 | TEACHER |
| student | student123 | STUDENT |

Stop with `docker compose down`. Add `-v` to also delete the database volume and start fresh.

### Try it

- **Browser (Swagger UI):** open http://localhost:8080. It shows every endpoint with a form and an **Execute** button. Start with **Auth → POST /api/auth/login** (the form is filled with `admin` / `admin123`), then try anything else; the browser keeps the login. Log in as `teacher` or `student` to see the 403s.
- **Postman:** import `postman/University-Management-System.postman_collection.json`. Run a "Login as …" request first; Postman keeps the session cookie, so every later request runs as that user. Log in as someone else to switch roles.
- **One command that checks everything:** `./scripts/smoke-test.sh` logs in as every role, tries every endpoint, and prints `ok` or `FAIL` for each expected status code.
- **curl:**

```bash
# log in as admin; the session cookie is saved in admin.txt
curl -c admin.txt -X POST localhost:8080/api/auth/login -d username=admin -d password=admin123

# use the session
curl -b admin.txt localhost:8080/api/teachers

curl -b admin.txt -X POST localhost:8080/api/teachers -H 'Content-Type: application/json' -d '{
  "name": "Karim Ahmed", "email": "karim@university.edu", "department": "EEE",
  "designation": "Lecturer", "username": "karim", "password": "secret123"}'

# log out; the cookie stops working
curl -b admin.txt -X POST localhost:8080/api/auth/logout
```

## Endpoints

| Method | Path | What it does | Success |
|---|---|---|---|
| POST | `/api/auth/login` | Log in. Form fields `username`, `password` (x-www-form-urlencoded) | 200 + the user |
| POST | `/api/auth/logout` | Log out, end the session | 200 |
| GET | `/api/auth/me` | The logged-in user | 200 |
| GET | `/api/{admins, teachers, students}` | List all | 200 |
| GET | `/api/{…}/{id}` | Get one | 200 |
| POST | `/api/{…}` | Create (also creates that person's login) | 201 + `Location` header |
| PUT | `/api/{…}/{id}` | Update. Leave `password` out to keep the current one | 200 |
| DELETE | `/api/{…}/{id}` | Delete | 204 |

### Who can do what

| | `/api/admins` | `/api/teachers` | `/api/students` |
|---|---|---|---|
| ADMIN | full CRUD | full CRUD | full CRUD |
| TEACHER | 403 | full CRUD | 403 |
| STUDENT | 403 | 403 | full CRUD |
| not logged in | 401 | 401 | 401 |

### Request bodies

```json
// POST /api/admins
{ "name": "Rina Das", "email": "rina@university.edu", "phone": "+8801711111111",
  "username": "rina", "password": "secret123" }

// POST /api/teachers
{ "name": "Karim Ahmed", "email": "karim@university.edu", "department": "EEE",
  "designation": "Lecturer", "username": "karim", "password": "secret123" }

// POST /api/students
{ "name": "Nusrat Jahan", "email": "nusrat@university.edu", "roll": "2207045",
  "department": "CSE", "batch": 2022, "username": "nusrat", "password": "secret123" }
```

Replies never contain the password. They include `id`, `username` and `role`.

### Status codes

| Code | When |
|---|---|
| 200 OK | GET, PUT, login, logout |
| 201 Created | POST; the `Location` header points at the new record |
| 204 No Content | DELETE |
| 400 Bad Request | Invalid fields (`fieldErrors` lists each one), broken JSON, non-numeric id |
| 401 Unauthorized | Not logged in, or wrong username/password |
| 403 Forbidden | Logged in, but your role may not use this resource |
| 404 Not Found | No record with that id |
| 405 Method Not Allowed | e.g. `DELETE /api/students` without an id |
| 409 Conflict | Username, e-mail or roll already used; deleting the last admin |
| 415 Unsupported Media Type | Body is not sent as JSON |

Every error has the same JSON shape:

```json
{ "timestamp": "2026-10-21T10:15:30Z", "status": 403, "error": "Forbidden",
  "message": "Your role is not allowed to access this resource", "path": "/api/students" }
```

## How each requirement is met

### REST API

- One controller per resource (`AdminController`, `TeacherController`, `StudentController`) with GET, GET by id, POST, PUT and DELETE. Plural nouns in the URL, the HTTP method says the action.
- Controllers return `ResponseEntity` so the status code is explicit: `ok(…)`, `created(location)`, `noContent()`.
- Requests and replies are DTOs (Java records), never the entities, so the password hash can't leak.
- `@Valid` checks the request body. Password is required on POST only, using the validation group `OnCreate`.
- `GlobalExceptionHandler` (`@RestControllerAdvice`) maps exceptions to 400, 404, 405, 409 and 415.
- Swagger UI (springdoc-openapi) builds an interactive page from the controllers. `OpenApiConfig` adds the login and logout endpoints, which are Spring Security filters rather than controllers, and the real status codes of each endpoint. The page itself is public; the data behind it still needs a login.

### Spring Security

- **Login:** Spring Security's form login filter handles `POST /api/auth/login`. `AppUserDetailsService` loads the user from the `users` table, Spring compares the password with the stored hash, and on success keeps the user in the HTTP session (the `JSESSIONID` cookie). The success and failure handlers in `SecurityConfig` reply with JSON instead of redirecting.
- **Logout:** `POST /api/auth/logout` invalidates the session and deletes the cookie. The old cookie then gets 401.
- **Password hashing:** `BCryptPasswordEncoder`. Passwords are hashed in `AccountService` before saving, so the database holds `$2a$10$…`, never the plain text. BCrypt adds a random salt and is slow on purpose, so the same password gives a different hash every time.
- **RBAC:** each user has one role, given to Spring as `ROLE_ADMIN`, `ROLE_TEACHER` or `ROLE_STUDENT`. `SecurityConfig` has one rule per resource (`hasRole`, `hasAnyRole`). Not logged in gives 401, the wrong role gives 403.
- The role comes from the resource, never from the request body: anything created through `/api/teachers` is a teacher. A teacher cannot create an admin by sending `"role": "ADMIN"`.
- CSRF protection is off because this is a JSON API used from Postman and curl. The session cookie is `SameSite=Strict` and `HttpOnly` instead, so another website can't send it or read it.

### Docker

- `Dockerfile` has two stages: a Maven image builds the jar, then only the jar is copied onto a small Java 17 runtime image that runs as a non-root user.
- `compose.yaml` runs `db` (PostgreSQL with a health check and a named volume, so data survives restarts) and `app` (built from the Dockerfile). The app waits for `service_healthy` and reaches the database by its service name, `db`. `SPRING_DATASOURCE_*` environment variables override `application.yml`.

### Data model

`AppUser` holds the fields every role shares (name, email, username, password, role) and is the `users` table. `Admin`, `Teacher` and `Student` extend it with JPA `JOINED` inheritance, so each has its own table (`admins`, `teachers`, `students`) with only its extra columns, linked by the same id. Logging in looks at one table, and usernames and e-mails are unique across all roles.

## Project structure

```
src/main/java/com/example/ums
├── config/       SecurityConfig (rules, login, logout, BCrypt), DataSeeder (starting accounts),
│                 OpenApiConfig (Swagger UI)
├── controller/   AuthController, AdminController, TeacherController, StudentController
├── dto/          request and response records, ApiError, OnCreate
├── entity/       AppUser, Admin, Teacher, Student, Role
├── exception/    ResourceNotFoundException, ConflictException, GlobalExceptionHandler
├── repository/   Spring Data JPA repositories
├── security/     AppUserDetailsService (loads a user for login)
└── service/      AccountService (shared login fields + hashing), Admin/Teacher/StudentService
```

## Tests

```bash
./mvnw test
```

38 tests run the whole app against an in-memory H2 database (no Docker needed). They cover login and logout, BCrypt storage, the full role matrix, CRUD with every status code, validation, duplicates, the last-admin rule and the Swagger page.

## Run from IntelliJ instead

Start only the database with `docker compose up -d db`, then run `UniversityManagementApplication`. `application.yml` already points at `localhost:5432`.
