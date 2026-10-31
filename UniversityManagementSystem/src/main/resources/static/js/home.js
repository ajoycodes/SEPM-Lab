// The homepage: sign-in, the dashboard for managing records, and the departments.
(() => {
  const { request, login, loadUser, user, roleName, el } = Site;
  const $ = id => document.getElementById(id);

  // Which resources each role may manage. The server enforces this; the page only hides the rest.
  const RESOURCES = { ADMIN: ['admins', 'teachers', 'students'], TEACHER: ['teachers'], STUDENT: ['students'] };
  const NOTES = {
    ADMIN: 'You can add, edit and delete admins, teachers and students.',
    TEACHER: 'You can add, edit and delete teachers.',
    STUDENT: 'You can add, edit and delete students.',
  };

  let academics = null;
  let resource = null;
  let records = [];
  let editingId = null;

  /* ---------- Sign-in ---------- */
  function openSignin(event) {
    if (event) event.preventDefault();
    $('signin-error').textContent = '';
    $('signin-dialog').showModal();
  }

  $('signin-link').addEventListener('click', openSignin);
  $('hero-signin').addEventListener('click', event => {
    if (user()) {
      event.preventDefault();
      location.hash = '#portal';
    } else {
      openSignin(event);
    }
  });
  $('signin-cancel').onclick = () => $('signin-dialog').close();

  $('signin-form').onsubmit = async event => {
    event.preventDefault();
    const fields = event.target.elements;
    try {
      await login(fields.username.value, fields.password.value);
      event.target.reset();
      $('signin-dialog').close();
      await loadUser();
      await showDashboard();
      await EventBoard.refresh();
      $('portal').scrollIntoView();
    } catch (error) {
      $('signin-error').textContent = error.status === 401 ? 'Invalid email or password' : error.message;
    }
  };

  /* ---------- Dashboard ---------- */
  async function showDashboard() {
    const me = user();
    $('hero-signin').textContent = 'Open dashboard';
    $('portal').hidden = false;
    $('portal-note').textContent = NOTES[me.role];
    const allowed = RESOURCES[me.role];
    $('tabs').replaceChildren(...allowed.map(name => {
      const tab = el('button', capitalize(name), 'tab');
      tab.setAttribute('role', 'tab');
      tab.dataset.resource = name;
      tab.onclick = () => selectResource(name).catch(showError);
      return tab;
    }));
    $('tabs').hidden = allowed.length === 1;
    await selectResource(allowed[0]);
  }

  async function selectResource(name) {
    resource = name;
    $('record-search').value = '';
    for (const tab of $('tabs').children) {
      tab.setAttribute('aria-selected', String(tab.dataset.resource === name));
    }
    await loadRecords();
  }

  async function loadRecords() {
    records = await request('/api/' + resource);
    $('portal-error').textContent = '';
    render();
  }

  function render() {
    const query = $('record-search').value.trim().toLowerCase();
    const faculty = $('faculty-filter').value;
    const department = $('department-filter').value;
    const shown = records.filter(record =>
      (!faculty || record.faculty === faculty)
      && (!department || record.department === department)
      && [record.name, record.email, record.department, record.designation, record.specialization]
        .some(value => (value || '').toLowerCase().includes(query)));

    $('record-count').textContent = `${shown.length} of ${records.length} records`;
    $('record-rows').replaceChildren(...shown.map(row));
    if (!shown.length) {
      const empty = el('td', 'No records found.', 'muted');
      empty.colSpan = 5;
      const line = el('tr');
      line.append(empty);
      $('record-rows').append(line);
    }
  }

  function row(record) {
    const line = el('tr');
    line.append(el('td', record.name), el('td', record.email), el('td', record.department));

    const position = el('td', record.designation || roleName(record.role));
    for (const detail of [record.departmentHead && 'Department head', record.facultyDean && 'Faculty dean', record.specialization]) {
      if (detail) position.append(el('small', detail));
    }
    line.append(position);

    const actions = el('td');
    const wrap = el('div', undefined, 'row-actions');
    const edit = el('button', 'Edit', 'btn btn-small');
    edit.onclick = () => openEditor(record);
    const remove = el('button', 'Delete', 'btn btn-small btn-danger');
    remove.onclick = () => removeRecord(record);
    wrap.append(edit, remove);
    actions.append(wrap);
    line.append(actions);
    return line;
  }

  async function removeRecord(record) {
    if (!confirm(`Delete ${record.name}? This cannot be undone.`)) return;
    try {
      await request(`/api/${resource}/${record.id}`, { method: 'DELETE' });
      if (record.id === user().id) {
        location.assign('/'); // they deleted their own account
        return;
      }
      await loadRecords();
      await loadAcademics();
    } catch (error) {
      showError(error);
    }
  }

  function showError(error) {
    $('portal-error').textContent = error.message;
  }

  function openEditor(record) {
    editingId = record ? record.id : null;
    const form = $('record-form');
    form.reset();
    $('record-error').textContent = '';
    $('record-title').textContent = record ? 'Edit record' : 'Add record';

    fillDepartmentChoices($('record-department'));
    const current = record ? record.department : (resource === 'admins' ? 'Administration' : '');
    if (current && ![...$('record-department').options].some(option => option.value === current)) {
      $('record-department').add(new Option(current, current));
    }
    $('record-department').value = current;

    form.elements.name.value = record ? record.name : '';
    form.elements.email.value = record ? record.email : '';
    form.elements.designation.value = (record && record.designation) || '';
    form.elements.specialization.value = (record && record.specialization) || '';
    form.elements.departmentHead.checked = Boolean(record && record.departmentHead);
    form.elements.facultyDean.checked = Boolean(record && record.facultyDean);
    $('teacher-fields').hidden = resource !== 'teachers';
    $('leadership-fields').hidden = user().role !== 'ADMIN';

    form.elements.password.required = !record;
    $('password-hint').textContent = record ? 'Leave blank to keep the current password.' : 'At least 8 characters.';
    $('record-dialog').showModal();
  }

  $('record-form').onsubmit = async event => {
    event.preventDefault();
    const fields = event.target.elements;
    const json = { name: fields.name.value, email: fields.email.value, department: fields.department.value };
    if (fields.password.value) json.password = fields.password.value;
    if (resource === 'teachers') {
      json.designation = fields.designation.value;
      json.specialization = fields.specialization.value;
      if (user().role === 'ADMIN') {
        json.departmentHead = fields.departmentHead.checked;
        json.facultyDean = fields.facultyDean.checked;
      }
    }
    $('record-save').disabled = true;
    try {
      const path = `/api/${resource}` + (editingId ? `/${editingId}` : '');
      await request(path, { method: editingId ? 'PUT' : 'POST', json });
      $('record-dialog').close();
      await loadUser(); // their own name may have changed
      await loadRecords();
      await loadAcademics();
    } catch (error) {
      $('record-error').textContent = error.message;
    } finally {
      $('record-save').disabled = false;
    }
  };

  $('add-record').onclick = () => openEditor(null);
  $('record-cancel').onclick = () => $('record-dialog').close();
  $('record-search').oninput = render;
  $('faculty-filter').onchange = () => {
    fillDepartmentFilter();
    render();
  };
  $('department-filter').onchange = render;

  /* ---------- Faculties and departments ---------- */
  async function loadAcademics() {
    academics = await request('/api/academics');
    const chosenFaculty = $('faculty-filter').value;
    const chosenDepartment = $('department-filter').value;
    $('faculty-filter').replaceChildren(new Option('All faculties', ''));
    for (const faculty of academics.faculties) {
      $('faculty-filter').add(new Option(faculty.name, faculty.name));
    }
    $('faculty-filter').value = chosenFaculty;
    fillDepartmentFilter(chosenDepartment);
    renderCounts();
    renderFaculties();
  }

  function fillDepartmentFilter(selected = '') {
    $('department-filter').replaceChildren(new Option('All departments', ''));
    for (const faculty of academics.faculties) {
      if ($('faculty-filter').value && faculty.name !== $('faculty-filter').value) continue;
      for (const department of faculty.departments) {
        $('department-filter').add(new Option(department.name, department.name));
      }
    }
    $('department-filter').value = selected;
  }

  function fillDepartmentChoices(select) {
    select.replaceChildren(new Option('Select a department', ''));
    for (const faculty of academics.faculties) {
      const group = el('optgroup');
      group.label = faculty.name;
      for (const department of faculty.departments) {
        group.append(new Option(`${department.code}: ${department.name}`, department.name));
      }
      select.append(group);
    }
  }

  function renderCounts() {
    const departments = academics.faculties.flatMap(faculty => faculty.departments);
    const total = key => departments.reduce((sum, department) => sum + department[key], 0);
    const counts = [
      ['Faculties', academics.faculties.length],
      ['Departments', departments.length],
      ['Teachers', total('teachers')],
      ['Students', total('students')],
    ];
    $('hero-counts').replaceChildren(...counts.map(([label, value]) => {
      const line = el('div');
      line.append(el('dt', label), el('dd', value));
      return line;
    }));
  }

  function leaderText(title, leader) {
    return leader ? `${title}: ${leader.name}, ${leader.email}` : `${title}: not assigned`;
  }

  function renderFaculties() {
    $('faculty-grid').replaceChildren(...academics.faculties.map(faculty => {
      const card = el('details', undefined, 'faculty-card');
      const summary = el('summary');
      summary.append(el('span', faculty.name), el('small', `${faculty.departments.length} departments`));
      card.append(summary, el('p', leaderText('Dean', faculty.dean), 'dean'));
      for (const department of faculty.departments) {
        const entry = el('div', undefined, 'department');
        entry.append(
          el('h3', `${department.code}: ${department.name}`),
          el('p', leaderText('Head', department.head)),
          el('p', `${department.teachers} teachers, ${department.students} students`));
        card.append(entry);
      }
      return card;
    }));
  }

  function capitalize(text) {
    return text[0].toUpperCase() + text.slice(1);
  }

  /* ---------- Start ---------- */
  (async () => {
    await loadAcademics();
    const me = await loadUser();
    await EventBoard.refresh();
    if (me) {
      await showDashboard();
    } else if (location.hash === '#sign-in') {
      openSignin();
    }
  })().catch(showError);
})();
