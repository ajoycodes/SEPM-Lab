#!/usr/bin/env bash
# Runs every requirement against a running server and prints ok / FAIL per check.
# Start the app first (docker compose up --build), then:  ./scripts/smoke-test.sh [base-url]
set -u
BASE="${1:-http://localhost:8080}"
COMPOSE_FILE="$(cd "$(dirname "$0")/.." && pwd)/compose.yaml"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
S="$(date +%s)"   # suffix so reruns never clash with earlier records
PASS=0
FAIL=0

# check "<what>" <expected status> <curl arguments...>   (the reply body is kept in $TMP/body)
check() {
  local what="$1" expected="$2"
  shift 2
  local actual
  actual=$(curl -s -o "$TMP/body" -w '%{http_code}' "$@")
  if [ "$actual" = "$expected" ]; then
    PASS=$((PASS + 1))
    printf '  ok    %s  %s\n' "$actual" "$what"
  else
    FAIL=$((FAIL + 1))
    printf '  FAIL  %s, expected %s  %s\n        %s\n' "$actual" "$expected" "$what" "$(cat "$TMP/body")"
  fi
}
last_id() { sed -E 's/^\{"id":([0-9]+).*/\1/' "$TMP/body"; }
JSON=(-H 'Content-Type: application/json')

admin_body()   { echo "{\"name\":\"Admin $1\",\"email\":\"admin$1@test.edu\",\"phone\":\"+880170$1\",\"username\":\"admin_$1\",\"password\":\"secret123\"}"; }
teacher_body() { echo "{\"name\":\"Teacher $1\",\"email\":\"teacher$1@test.edu\",\"department\":\"CSE\",\"designation\":\"Lecturer\",\"username\":\"teacher_$1\",\"password\":\"secret123\"}"; }
student_body() { echo "{\"name\":\"Student $1\",\"email\":\"student$1@test.edu\",\"roll\":\"R$1\",\"department\":\"CSE\",\"batch\":2022,\"username\":\"student_$1\",\"password\":\"secret123\"}"; }

echo "Server: $BASE"
echo
echo "Swagger UI"
check "GET / redirects to Swagger UI"         302 "$BASE/"
check "Swagger UI page, no login needed"      200 "$BASE/swagger-ui/index.html"

echo
echo "Login and logout"
check "no session: GET /api/students"           401 "$BASE/api/students"
check "wrong password"                          401 -X POST "$BASE/api/auth/login" -d username=admin -d password=wrong
check "login as admin"                          200 -c "$TMP/admin"   -X POST "$BASE/api/auth/login" -d username=admin   -d password=admin123
check "login as teacher"                        200 -c "$TMP/teacher" -X POST "$BASE/api/auth/login" -d username=teacher -d password=teacher123
check "login as student"                        200 -c "$TMP/student" -X POST "$BASE/api/auth/login" -d username=student -d password=student123
check "teacher: GET /api/auth/me"               200 -b "$TMP/teacher" "$BASE/api/auth/me"

echo
echo "Role-based access (GET lists)"
for pair in admin:admins:200 admin:teachers:200 admin:students:200 \
            teacher:admins:403 teacher:teachers:200 teacher:students:403 \
            student:admins:403 student:teachers:403 student:students:200; do
  IFS=: read -r role resource expected <<< "$pair"
  check "$role: GET /api/$resource" "$expected" -b "$TMP/$role" "$BASE/api/$resource"
done

echo
echo "Admin: full CRUD on all three resources"
for resource in admins teachers students; do
  body_fn="${resource%s}_body"
  check "POST /api/$resource"           201 -b "$TMP/admin" "${JSON[@]}" -X POST "$BASE/api/$resource" -d "$($body_fn "a$S")"
  id=$(last_id)
  check "GET /api/$resource/$id"        200 -b "$TMP/admin" "$BASE/api/$resource/$id"
  check "PUT /api/$resource/$id"        200 -b "$TMP/admin" "${JSON[@]}" -X PUT "$BASE/api/$resource/$id" -d "$($body_fn "a$S")"
  check "DELETE /api/$resource/$id"     204 -b "$TMP/admin" -X DELETE "$BASE/api/$resource/$id"
  check "GET deleted /api/$resource/$id" 404 -b "$TMP/admin" "$BASE/api/$resource/$id"
done

echo
echo "Teacher manages only teachers, student manages only students"
check "teacher: POST /api/teachers"   201 -b "$TMP/teacher" "${JSON[@]}" -X POST "$BASE/api/teachers" -d "$(teacher_body "t$S")"
id=$(last_id)
check "teacher: DELETE /api/teachers/$id" 204 -b "$TMP/teacher" -X DELETE "$BASE/api/teachers/$id"
check "teacher: POST /api/students"   403 -b "$TMP/teacher" "${JSON[@]}" -X POST "$BASE/api/students" -d "$(student_body "t$S")"
check "student: POST /api/students"   201 -b "$TMP/student" "${JSON[@]}" -X POST "$BASE/api/students" -d "$(student_body "s$S")"
id=$(last_id)
check "student: DELETE /api/students/$id" 204 -b "$TMP/student" -X DELETE "$BASE/api/students/$id"
check "student: POST /api/teachers"   403 -b "$TMP/student" "${JSON[@]}" -X POST "$BASE/api/teachers" -d "$(teacher_body "s$S")"

echo
echo "Error codes"
check "invalid body (bad e-mail, no password)" 400 -b "$TMP/admin" "${JSON[@]}" -X POST "$BASE/api/teachers" \
  -d '{"name":"X","email":"nope","department":"CSE","designation":"Lecturer","username":"xx_'"$S"'"}'
check "duplicate username"            409 -b "$TMP/admin" "${JSON[@]}" -X POST "$BASE/api/students" \
  -d "$(student_body "d$S" | sed 's/"username":"[^"]*"/"username":"teacher"/')"

echo
echo "Password hashing (read straight from PostgreSQL)"
if command -v docker > /dev/null && docker compose -f "$COMPOSE_FILE" ps --status running db 2>/dev/null | grep -q db; then
  hash=$(docker compose -f "$COMPOSE_FILE" exec -T db psql -U ums -d ums -tAc "select password from users where username = 'admin'")
  case "$hash" in
    '$2a$'*) PASS=$((PASS + 1)); echo "  ok    admin's stored password is a BCrypt hash: ${hash:0:20}..." ;;
    *)       FAIL=$((FAIL + 1)); echo "  FAIL  admin's stored password: $hash" ;;
  esac
else
  echo "  (skipped: the database container is not reachable from here)"
fi

echo
echo "Logout"
check "admin: POST /api/auth/logout"  200 -b "$TMP/admin" -c "$TMP/admin" -X POST "$BASE/api/auth/logout"
check "admin after logout: GET /api/admins" 401 -b "$TMP/admin" "$BASE/api/admins"

echo
echo "$PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ]
