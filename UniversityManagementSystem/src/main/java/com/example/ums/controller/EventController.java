package com.example.ums.controller;

import com.example.ums.dto.EventRequest;
import com.example.ums.dto.EventResponse;
import com.example.ums.service.EventService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public List<EventResponse> list() {
        return eventService.list();
    }

    @GetMapping("/{id}")
    public EventResponse get(@PathVariable Long id) {
        return eventService.get(id);
    }

    @PostMapping
    public ResponseEntity<EventResponse> create(@Valid @RequestBody EventRequest request) {
        EventResponse created = eventService.create(request);
        return ResponseEntity.created(URI.create("/api/events/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public EventResponse update(@PathVariable Long id, @Valid @RequestBody EventRequest request) {
        return eventService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        eventService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
