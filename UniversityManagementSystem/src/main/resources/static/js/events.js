// The list of events, on the homepage and on a teacher's profile.
const EventBoard = (() => {
  const { request, user, el } = Site;
  const format = new Intl.DateTimeFormat('en-GB', { dateStyle: 'medium', timeStyle: 'short', timeZone: 'Asia/Dhaka' });
  const board = document.getElementById('event-board');
  const postButton = document.getElementById('post-event');
  const errorLine = document.getElementById('events-error');
  let authorId = null; // set on a teacher's profile, to show only their events
  let editingId = null;
  let dialog = null;

  async function refresh() {
    const events = await request('/api/events');
    render(authorId ? events.filter(event => event.authorId === authorId) : events);
  }

  function render(events) {
    board.replaceChildren();
    const me = user();
    postButton.hidden = !(me && me.role === 'TEACHER' && (!authorId || authorId === me.id));
    if (!events.length) {
      board.append(el('p', 'No events have been published yet.', 'muted'));
    }
    for (const event of events) {
      board.append(card(event, me));
    }
  }

  function card(event, me) {
    const article = el('article', undefined, 'event-card');
    const time = el('time', format.format(new Date(event.startsAt)));
    time.dateTime = event.startsAt;
    article.append(time, el('h3', event.title), el('p', event.venue, 'venue'), el('p', event.description, 'description'));
    if (event.endsAt) {
      article.append(el('small', 'Ends ' + format.format(new Date(event.endsAt))));
    }
    const author = el('a', 'Posted by ' + event.authorName);
    author.href = '/profile.html?teacher=' + event.authorId;
    article.append(author);

    if (me && me.id === event.authorId) {
      const actions = el('div', undefined, 'form-actions');
      const edit = el('button', 'Edit', 'btn btn-small');
      edit.onclick = () => open(event);
      const remove = el('button', 'Delete', 'btn btn-small btn-danger');
      remove.onclick = () => removeEvent(event);
      actions.append(edit, remove);
      article.append(actions);
    }
    return article;
  }

  async function removeEvent(event) {
    if (!confirm(`Delete "${event.title}"?`)) return;
    try {
      await request('/api/events/' + event.id, { method: 'DELETE' });
      await refresh();
    } catch (error) {
      errorLine.textContent = error.message;
    }
  }

  function buildDialog() {
    dialog = el('dialog');
    dialog.innerHTML = `
      <form id="event-form">
        <h2 id="event-title">Post an event</h2>
        <label>Title<input name="title" required maxlength="150"></label>
        <label>Description<textarea name="description" required maxlength="4000" rows="4"></textarea></label>
        <label>Venue<input name="venue" required maxlength="200"></label>
        <label>Starts (Bangladesh time)<input name="startsAt" type="datetime-local" required></label>
        <label>Ends (optional)<input name="endsAt" type="datetime-local"></label>
        <p class="error" id="event-error" role="alert"></p>
        <div class="form-actions">
          <button class="btn btn-primary" id="event-save">Publish</button>
          <button class="btn" type="button" id="event-cancel">Cancel</button>
        </div>
      </form>`;
    document.body.append(dialog);
    dialog.querySelector('#event-cancel').onclick = () => dialog.close();
    dialog.querySelector('#event-form').onsubmit = submit;
  }

  function open(event) {
    if (!dialog) buildDialog();
    editingId = event ? event.id : null;
    const form = dialog.querySelector('#event-form');
    form.reset();
    if (event) {
      form.elements.title.value = event.title;
      form.elements.description.value = event.description;
      form.elements.venue.value = event.venue;
      form.elements.startsAt.value = toLocalInput(event.startsAt);
      form.elements.endsAt.value = toLocalInput(event.endsAt);
    }
    dialog.querySelector('#event-title').textContent = event ? 'Edit event' : 'Post an event';
    dialog.querySelector('#event-save').textContent = event ? 'Save' : 'Publish';
    dialog.querySelector('#event-error').textContent = '';
    dialog.showModal();
  }

  async function submit(submitEvent) {
    submitEvent.preventDefault();
    const fields = submitEvent.target.elements;
    const json = {
      title: fields.title.value,
      description: fields.description.value,
      venue: fields.venue.value,
      startsAt: fromLocalInput(fields.startsAt.value),
      endsAt: fields.endsAt.value ? fromLocalInput(fields.endsAt.value) : null,
    };
    try {
      await request('/api/events' + (editingId ? '/' + editingId : ''), { method: editingId ? 'PUT' : 'POST', json });
      dialog.close();
      await refresh();
    } catch (error) {
      dialog.querySelector('#event-error').textContent = error.message;
    }
  }

  // The form works in Bangladesh time (UTC+06) wherever the browser is; the API stores UTC.
  function fromLocalInput(value) {
    return new Date(value + '+06:00').toISOString();
  }

  function toLocalInput(instant) {
    if (!instant) return '';
    return new Date(new Date(instant).getTime() + 6 * 3600000).toISOString().slice(0, 16);
  }

  postButton.onclick = () => open();

  function showError(error) {
    errorLine.textContent = error.message;
  }

  return {
    refresh: () => refresh().catch(showError),
    forAuthor(id) {
      authorId = id;
    },
  };
})();
