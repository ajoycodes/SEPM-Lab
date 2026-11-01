"""Fills the running app with demo people through its REST API, then checks that every login works.

Creates 60 teachers and 300 students (3 and 15 per department). Everyone is fictional, and everyone
gets the admin password from .env.
Writes demo-accounts.csv and DEMO-LOGINS.md. Safe to run again: existing accounts are updated.

Usage, after `docker compose up`:  python3 scripts/seed_demo.py
"""
import csv
import http.cookiejar
import json
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CONFIG = dict(line.split('=', 1) for line in (ROOT / '.env').read_text().splitlines()
              if line and not line.startswith('#') and '=' in line)
BASE = 'http://127.0.0.1:' + CONFIG.get('APP_PORT', '8081')
ADMIN_EMAIL = CONFIG.get('ADMIN_EMAIL', 'admin@university.edu')
PASSWORD = CONFIG['ADMIN_PASSWORD']

# Department code, name, and one specialization for each of its three teachers.
DEPARTMENTS = [
    ('CE', 'Civil Engineering', ['Structural Engineering', 'Geotechnical Engineering', 'Transportation Engineering']),
    ('URP', 'Urban and Regional Planning', ['Urban Design', 'Regional Planning', 'Transport Planning']),
    ('BECM', 'Building Engineering and Construction Management', ['Construction Management', 'Building Services', 'Construction Materials']),
    ('ARCH', 'Architecture', ['Architectural Design', 'Sustainable Architecture', 'Urban Heritage']),
    ('EEE', 'Electrical and Electronic Engineering', ['Power Systems', 'Control Systems', 'Electronics']),
    ('CSE', 'Computer Science and Engineering', ['Artificial Intelligence', 'Software Engineering', 'Computer Networks']),
    ('ECE', 'Electronics and Communication Engineering', ['Wireless Communication', 'Signal Processing', 'Embedded Systems']),
    ('BME', 'Biomedical Engineering', ['Medical Instrumentation', 'Biomedical Imaging', 'Biomaterials']),
    ('MSE', 'Materials Science and Engineering', ['Advanced Materials', 'Materials Characterization', 'Nanomaterials']),
    ('ME', 'Mechanical Engineering', ['Thermal Engineering', 'Fluid Mechanics', 'Machine Design']),
    ('IEM', 'Industrial Engineering and Management', ['Operations Research', 'Manufacturing Systems', 'Supply Chain Management']),
    ('ESE', 'Energy Science and Engineering', ['Renewable Energy', 'Energy Systems', 'Energy Efficiency']),
    ('LE', 'Leather Engineering', ['Leather Processing', 'Leather Products', 'Environmental Management']),
    ('TE', 'Textile Engineering', ['Textile Manufacturing', 'Fabric Engineering', 'Textile Chemistry']),
    ('CHE', 'Chemical Engineering', ['Reaction Engineering', 'Process Engineering', 'Separation Processes']),
    ('MTE', 'Mechatronics Engineering', ['Robotics', 'Automation', 'Intelligent Control']),
    ('MATH', 'Mathematics', ['Applied Mathematics', 'Numerical Analysis', 'Mathematical Modeling']),
    ('PHY', 'Physics', ['Solid State Physics', 'Optics', 'Computational Physics']),
    ('CHEM', 'Chemistry', ['Analytical Chemistry', 'Organic Chemistry', 'Physical Chemistry']),
    ('HUM', 'Humanities and Business', ['Economics', 'English Communication', 'Business Management']),
]
DESIGNATIONS = ['Professor', 'Associate Professor', 'Lecturer']

FIRST_NAMES = ['Mahmud', 'Tahmina', 'Rezaul', 'Sharmin', 'Kamrul', 'Nasrin', 'Shafiq', 'Rumana', 'Mehedi', 'Farzana',
               'Anisur', 'Lamia', 'Habib', 'Sabrina', 'Jahid', 'Tania', 'Mizan', 'Rokeya', 'Shakil', 'Munira']
LAST_NAMES = ['Uddin', 'Begum', 'Miah', 'Sultana', 'Bhuiyan', 'Akter', 'Das', 'Roy', 'Sikder', 'Molla',
              'Biswas', 'Paul', 'Howlader', 'Khatun', 'Shikder', 'Sheikh', 'Mondal', 'Ali', 'Pramanik', 'Nahar']


class Client:
    """A logged-in browser: keeps the session cookie and sends the CSRF token on every write."""

    def __init__(self):
        cookies = urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar())
        self.opener = urllib.request.build_opener(urllib.request.ProxyHandler({}), cookies)
        self.csrf = None

    def call(self, path, method='GET', json_body=None, form=None, expected=200):
        headers = {}
        data = None
        if self.csrf:
            headers[self.csrf['headerName']] = self.csrf['token']
        if json_body is not None:
            data = json.dumps(json_body).encode()
            headers['Content-Type'] = 'application/json'
        if form is not None:
            data = urllib.parse.urlencode(form).encode()
            headers['Content-Type'] = 'application/x-www-form-urlencoded'
        request = urllib.request.Request(BASE + path, data=data, method=method, headers=headers)
        try:
            response = self.opener.open(request, timeout=30)
        except urllib.error.HTTPError as error:
            response = error
        raw = response.read()
        body = json.loads(raw) if raw else None
        if response.status != expected:
            raise RuntimeError(f'{method} {path} gave {response.status}: {body}')
        return body

    def login(self, email):
        self.csrf = self.call('/api/auth/csrf')
        self.call('/api/auth/login', 'POST', form={'username': email, 'password': PASSWORD})
        self.csrf = self.call('/api/auth/csrf')  # Spring issues a new token after login


def person_name(index, teacher=False):
    # Stepping through the surnames at a different rate keeps neighbours from sharing one.
    name = FIRST_NAMES[index % len(FIRST_NAMES)] + ' ' + LAST_NAMES[(index * 3 + index // len(FIRST_NAMES)) % len(LAST_NAMES)]
    return 'Dr. ' + name if teacher else name


def build_people():
    people = []
    teacher_index = 0
    student_index = 60
    for code, department, specializations in DEPARTMENTS:
        code = code.lower()
        for number in range(1, 4):
            people.append(('teachers', {
                'name': person_name(teacher_index, teacher=True),
                'email': f'{code}.teacher{number:02d}@university.edu',
                'password': PASSWORD,
                'department': department,
                'designation': DESIGNATIONS[number - 1],
                'specialization': specializations[number - 1],
            }))
            teacher_index += 1
        for number in range(1, 16):
            people.append(('students', {
                'name': person_name(student_index),
                'email': f'{code}.student{number:02d}@university.edu',
                'password': PASSWORD,
                'department': department,
            }))
            student_index += 1
    return people


def write_files(people):
    with (ROOT / 'demo-accounts.csv').open('w', newline='', encoding='utf-8') as output:
        writer = csv.writer(output)
        writer.writerow(['role', 'department', 'name', 'email', 'password', 'designation'])
        writer.writerow(['ADMIN', 'Administration', 'University Admin', ADMIN_EMAIL, PASSWORD, ''])
        for resource, body in people:
            writer.writerow(['TEACHER' if resource == 'teachers' else 'STUDENT', body['department'], body['name'],
                             body['email'], PASSWORD, body.get('designation', '')])

    lines = ['# Demo logins', '',
             'Everyone here is fictional.', '',
             f'Password for every account: `{PASSWORD}`', '',
             f'Admin: `{ADMIN_EMAIL}`', '',
             'Each department has 3 teachers and 15 students.', '',
             '| Code | Department | Teachers | Students |', '|---|---|---|---|']
    for code, department, _ in DEPARTMENTS:
        lower = code.lower()
        lines.append(f"| {code} | {department} | "
                     f"`{lower}.teacher01@university.edu` to `{lower}.teacher03@university.edu` | "
                     f"`{lower}.student01@university.edu` to `{lower}.student15@university.edu` |")
    (ROOT / 'DEMO-LOGINS.md').write_text('\n'.join(lines) + '\n', encoding='utf-8')


def main():
    admin = Client()
    admin.login(ADMIN_EMAIL)
    existing = {row['email']: row for resource in ('teachers', 'students')
                for row in admin.call('/api/' + resource)}
    people = build_people()

    for count, (resource, body) in enumerate(people, 1):
        account = existing.get(body['email'])
        if account:
            admin.call(f"/api/{resource}/{account['id']}", 'PUT', body)
        else:
            admin.call('/api/' + resource, 'POST', body, expected=201)
        if count % 60 == 0:
            print(f'Saved {count}/{len(people)} accounts', flush=True)
    write_files(people)

    for count, (resource, body) in enumerate(people, 1):
        client = Client()
        client.login(body['email'])
        me = client.call('/api/auth/me')
        assert me['email'] == body['email'] and me['department'] == body['department']
        assert me['role'] == ('TEACHER' if resource == 'teachers' else 'STUDENT')
        client.call('/api/auth/logout', 'POST', expected=204)
        if count % 60 == 0:
            print(f'Checked {count}/{len(people)} logins', flush=True)

    admin.call('/api/auth/logout', 'POST', expected=204)
    print('Done: 60 teachers and 300 students. All 360 logins work.', flush=True)


if __name__ == '__main__':
    main()
