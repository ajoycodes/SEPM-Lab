// The public teacher directory.
(() => {
  const { request, loadUser, initials, el } = Site;
  const $ = id => document.getElementById(id);
  let teachers = [];

  function render() {
    const query = $('teacher-search').value.trim().toLowerCase();
    const shown = teachers.filter(teacher =>
      [teacher.name, teacher.department, teacher.designation, teacher.researchField, teacher.researchInterests]
        .some(value => (value || '').toLowerCase().includes(query)));

    $('teacher-count').textContent = `${shown.length} teachers`;
    $('teacher-grid').replaceChildren(...shown.map(card));
    if (!shown.length) {
      $('teacher-grid').append(el('p', 'No teachers match your search.', 'muted'));
    }
  }

  function card(teacher) {
    const link = el('a', undefined, 'person-card');
    link.href = '/profile.html?teacher=' + teacher.id;
    const avatar = el('div', initials(teacher.name), 'avatar');
    avatar.setAttribute('aria-hidden', 'true');
    link.append(avatar, el('h2', teacher.name), el('p', teacher.designation || 'Teacher'), el('p', teacher.department, 'muted'));
    if (teacher.researchField) link.append(el('p', teacher.researchField, 'muted'));
    return link;
  }

  $('teacher-search').oninput = render;

  (async () => {
    await loadUser();
    teachers = await request('/api/public/teachers');
    render();
  })().catch(error => {
    $('teacher-error').textContent = error.message;
  });
})();
