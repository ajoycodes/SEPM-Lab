// A profile page. With ?teacher=ID it shows that teacher's public profile; otherwise the visitor's own.
(() => {
  const { request, loadUser, initials, el } = Site;
  const $ = id => document.getElementById(id);
  let profile = null;
  let kind = null;

  /* ---------- Showing the profile ---------- */
  function panel(title) {
    const section = el('section', undefined, 'panel');
    section.append(el('h2', title));
    $('profile-panels').append(section);
    return section;
  }

  function facts(section, rows) {
    const list = el('dl', undefined, 'facts');
    for (const [label, value] of rows) {
      list.append(el('dt', label), el('dd', value === null || value === undefined || value === '' ? 'Not added' : String(value)));
    }
    section.append(list);
  }

  function render() {
    $('profile-view').hidden = false;
    $('profile-name').textContent = profile.name;
    $('profile-avatar').textContent = initials(profile.name);
    document.title = `${profile.name} | University Management System`;
    $('profile-panels').replaceChildren();
    facts(panel('Contact'), [['Email', profile.email], ['Phone', profile.phone]]);

    if (kind === 'TEACHER') {
      renderTeacher();
    } else if (kind === 'STUDENT') {
      renderStudent();
    } else {
      $('profile-subtitle').textContent = 'Administrator';
      $('profile-note').textContent = 'Only you can see and edit this profile.';
    }
  }

  function renderTeacher() {
    $('profile-subtitle').textContent = [profile.designation, profile.department].filter(Boolean).join(', ');
    $('profile-note').textContent = 'Anyone can view this profile.';
    const about = panel('About');
    about.append(el('p', profile.bio || 'No bio added yet.', profile.bio ? 'prose' : 'muted'));

    const posts = [profile.departmentHead && 'Department head', profile.facultyDean && 'Faculty dean'].filter(Boolean);
    facts(panel('Appointment'), [
      ['Department', profile.department],
      ['Faculty', profile.faculty],
      ['Designation', profile.designation],
      ['Leadership', posts.join(', ') || 'None'],
    ]);
    facts(panel('Research'), [['Field', profile.researchField], ['Interests', profile.researchInterests]]);

    const education = panel('Education');
    if (!profile.education.length) {
      education.append(el('p', 'No education history added yet.', 'muted'));
    }
    for (const entry of profile.education) {
      const line = el('div', undefined, 'education-entry');
      line.append(el('h3', entry.qualification), el('p', entry.institution));
      if (entry.startYear || entry.endYear) {
        line.append(el('small', `${entry.startYear || ''} to ${entry.endYear || 'present'}`));
      }
      education.append(line);
    }
  }

  function renderStudent() {
    $('profile-subtitle').textContent = profile.department;
    $('profile-note').textContent = 'Only you can see and edit this profile.';
    facts(panel('Academic information'), [
      ['Roll', profile.roll],
      ['Department', profile.department],
      ['Faculty', profile.faculty],
      ['Program', profile.program],
      ['CGPA', profile.cgpa === null ? null : Number(profile.cgpa).toFixed(2)],
    ]);

    const results = panel('Previous semester results');
    if (!profile.semesterResults.length) {
      results.append(el('p', 'No semester results added yet.', 'muted'));
      return;
    }
    const table = el('table');
    const head = el('thead');
    const headRow = el('tr');
    headRow.append(el('th', 'Semester'), el('th', 'GPA'));
    head.append(headRow);
    const body = el('tbody');
    for (const result of profile.semesterResults) {
      const line = el('tr');
      line.append(el('td', result.semester), el('td', Number(result.gpa).toFixed(2)));
      body.append(line);
    }
    table.append(head, body);
    results.append(table);
  }

  /* ---------- Editing ---------- */
  function labelled(text, control) {
    const label = el('label', text);
    label.append(control);
    return label;
  }

  function input(type, field, value, maxLength) {
    const control = el('input');
    control.type = type;
    control.dataset.field = field;
    control.value = value ?? '';
    if (maxLength) control.maxLength = maxLength;
    return control;
  }

  function removeButton(row) {
    const button = el('button', 'Remove', 'btn btn-small');
    button.type = 'button';
    button.onclick = () => row.remove();
    return button;
  }

  function addEducationRow(entry = {}) {
    const row = el('div', undefined, 'repeat-row education-row');
    const qualification = input('text', 'qualification', entry.qualification, 160);
    const institution = input('text', 'institution', entry.institution, 200);
    qualification.required = true;
    institution.required = true;
    const start = input('number', 'startYear', entry.startYear);
    const end = input('number', 'endYear', entry.endYear);
    for (const year of [start, end]) {
      year.min = '1900';
      year.max = '2100';
    }
    row.append(labelled('Qualification', qualification), labelled('Institution', institution),
      labelled('Start year', start), labelled('End year', end), removeButton(row));
    $('education-rows').append(row);
  }

  function addResultRow(result = {}) {
    const row = el('div', undefined, 'repeat-row result-row');
    const semester = input('text', 'semester', result.semester, 100);
    semester.required = true;
    const gpa = input('number', 'gpa', result.gpa);
    gpa.required = true;
    gpa.min = '0';
    gpa.max = '4';
    gpa.step = '0.01';
    row.append(labelled('Semester', semester), labelled('GPA (0 to 4)', gpa), removeButton(row));
    $('result-rows').append(row);
  }

  async function openEditor() {
    $('profile-form-error').textContent = '';
    $('edit-name').value = profile.name;
    $('edit-email').value = profile.email;
    $('edit-phone').value = profile.phone ?? '';
    $('edit-teacher').hidden = kind !== 'TEACHER';
    $('edit-student').hidden = kind !== 'STUDENT';
    $('education-rows').replaceChildren();
    $('result-rows').replaceChildren();

    if (kind === 'TEACHER') {
      $('edit-bio').value = profile.bio ?? '';
      $('edit-field').value = profile.researchField ?? '';
      $('edit-interests').value = profile.researchInterests ?? '';
      profile.education.forEach(addEducationRow);
    } else if (kind === 'STUDENT') {
      $('edit-roll').value = profile.roll ?? '';
      $('edit-program').value = profile.program ?? '';
      $('edit-cgpa').value = profile.cgpa ?? '';
      profile.semesterResults.forEach(addResultRow);
      const catalog = await request('/api/academics');
      $('edit-department').replaceChildren();
      for (const faculty of catalog.faculties) {
        const group = el('optgroup');
        group.label = faculty.name;
        for (const department of faculty.departments) {
          group.append(new Option(department.name, department.name));
        }
        $('edit-department').append(group);
      }
      $('edit-department').value = profile.department;
    }
    $('profile-editor').hidden = false;
    $('profile-editor').scrollIntoView();
    $('edit-name').focus();
  }

  function readRow(row, field) {
    return row.querySelector(`[data-field="${field}"]`).value;
  }

  function formData() {
    const json = { name: $('edit-name').value, email: $('edit-email').value, phone: $('edit-phone').value };
    if (kind === 'TEACHER') {
      json.bio = $('edit-bio').value;
      json.researchField = $('edit-field').value;
      json.researchInterests = $('edit-interests').value;
      json.education = [...document.querySelectorAll('.education-row')].map(row => ({
        qualification: readRow(row, 'qualification'),
        institution: readRow(row, 'institution'),
        startYear: readRow(row, 'startYear') ? Number(readRow(row, 'startYear')) : null,
        endYear: readRow(row, 'endYear') ? Number(readRow(row, 'endYear')) : null,
      }));
    } else if (kind === 'STUDENT') {
      json.roll = $('edit-roll').value;
      json.department = $('edit-department').value;
      json.program = $('edit-program').value;
      json.cgpa = $('edit-cgpa').value === '' ? null : Number($('edit-cgpa').value);
      json.semesterResults = [...document.querySelectorAll('.result-row')].map(row => ({
        semester: readRow(row, 'semester'),
        gpa: Number(readRow(row, 'gpa')),
      }));
    }
    return json;
  }

  $('edit-profile').onclick = () => openEditor().catch(error => {
    $('profile-error').textContent = error.message;
  });
  $('cancel-profile').onclick = () => {
    $('profile-editor').hidden = true;
  };
  $('add-education').onclick = () => addEducationRow();
  $('add-result').onclick = () => addResultRow();

  $('profile-form').onsubmit = async event => {
    event.preventDefault();
    $('save-profile').disabled = true;
    try {
      profile = await request('/api/profile/me', { method: 'PUT', json: formData() });
      render();
      await loadUser();
      $('profile-editor').hidden = true;
      $('profile-status').textContent = 'Profile saved. Use your current email next time you sign in.';
      $('profile-view').scrollIntoView();
    } catch (error) {
      $('profile-form-error').textContent = error.message;
    } finally {
      $('save-profile').disabled = false;
    }
  };

  /* ---------- Start ---------- */
  (async () => {
    const me = await loadUser();
    const teacherId = new URLSearchParams(location.search).get('teacher');
    if (teacherId) {
      profile = await request('/api/public/teachers/' + encodeURIComponent(teacherId));
      kind = 'TEACHER';
    } else if (me) {
      profile = await request('/api/profile/me');
      kind = me.role;
    } else {
      $('profile-signin').hidden = false;
      return;
    }
    render();
    $('edit-profile').hidden = !(me && me.id === profile.id);
    if (kind === 'TEACHER') {
      $('teacher-events').hidden = false;
      $('teacher-events-title').textContent = me && me.id === profile.id ? 'My events' : `Events by ${profile.name}`;
      EventBoard.forAuthor(profile.id);
      await EventBoard.refresh();
    }
  })().catch(error => {
    $('profile-error').textContent = error.message;
  });
})();
