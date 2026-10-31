package com.example.ums.service;

import com.example.ums.dto.EventRequest;
import com.example.ums.dto.EventResponse;
import com.example.ums.entity.Account;
import com.example.ums.entity.UniversityEvent;
import com.example.ums.repository.EventRepository;
import com.example.ums.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

/** Anyone can read events. SecurityConfig lets only teachers write, and this class limits edits to the author. */
@Service
@Transactional
public class EventService {

    private final EventRepository eventRepository;
    private final CurrentUser currentUser;

    public EventService(EventRepository eventRepository, CurrentUser currentUser) {
        this.eventRepository = eventRepository;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<EventResponse> list() {
        return eventRepository.findAllByOrderByStartsAtAscIdAsc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public EventResponse get(Long id) {
        return toResponse(find(id));
    }

    public EventResponse create(EventRequest request) {
        UniversityEvent event = new UniversityEvent();
        event.setAuthor(currentUser.require());
        event.setCreatedAt(Instant.now());
        return save(event, request);
    }

    public EventResponse update(Long id, EventRequest request) {
        UniversityEvent event = find(id);
        requireAuthor(event, "You can edit only your own events");
        return save(event, request);
    }

    public void delete(Long id) {
        UniversityEvent event = find(id);
        requireAuthor(event, "You can delete only your own events");
        eventRepository.delete(event);
    }

    private EventResponse save(UniversityEvent event, EventRequest request) {
        if (request.endsAt() != null && request.endsAt().isBefore(request.startsAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "An event must end after it starts");
        }
        event.setTitle(request.title().strip());
        event.setDescription(request.description().strip());
        event.setVenue(request.venue().strip());
        event.setStartsAt(request.startsAt());
        event.setEndsAt(request.endsAt());
        return toResponse(eventRepository.saveAndFlush(event));
    }

    private void requireAuthor(UniversityEvent event, String message) {
        Account actor = currentUser.require();
        if (!event.getAuthor().getId().equals(actor.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, message);
        }
    }

    private UniversityEvent find(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
    }

    private EventResponse toResponse(UniversityEvent event) {
        Account author = event.getAuthor();
        return new EventResponse(event.getId(), event.getTitle(), event.getDescription(), event.getVenue(),
                event.getStartsAt(), event.getEndsAt(), event.getCreatedAt(), author.getId(), author.getName(),
                author.getDepartment());
    }
}
