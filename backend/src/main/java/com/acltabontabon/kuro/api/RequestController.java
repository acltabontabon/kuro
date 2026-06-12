package com.acltabontabon.kuro.api;

import com.acltabontabon.kuro.application.EvidenceView;
import com.acltabontabon.kuro.application.RequestCommandService;
import com.acltabontabon.kuro.application.RequestQueryService;
import com.acltabontabon.kuro.domain.DecisionCategory;
import com.acltabontabon.kuro.domain.Subject;
import com.acltabontabon.kuro.domain.SubjectKind;
import com.acltabontabon.kuro.domain.WireEnum;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The KURO request REST surface (#13). Thin: parses wire values, delegates every
 * workflow to the application layer, and shapes HTTP responses. Out-of-scope
 * categories are answered with a 200 refusal result, not an error.
 */
@RestController
@RequestMapping("/api/requests")
class RequestController {

    private final RequestCommandService commands;
    private final RequestQueryService queries;

    RequestController(RequestCommandService commands, RequestQueryService queries) {
        this.commands = commands;
        this.queries = queries;
    }

    @PostMapping
    ResponseEntity<Object> create(@Valid @RequestBody CreateRequestDto body) {
        Subject subject = toSubject(body.subject());
        Optional<DecisionCategory> supported = supportedCategory(body.category());
        if (supported.isEmpty()) {
            return ResponseEntity.ok(commands.refuseUnsupportedCategory(body.category(), subject));
        }
        String requestId = commands.createSupportedRequest(supported.get(), subject);
        return ResponseEntity.status(HttpStatus.CREATED).body(queries.getRequest(requestId).orElseThrow());
    }

    @PostMapping("/{id}/sources")
    ResponseEntity<Void> addSource(@PathVariable String id, @RequestBody AddSourceDto body) {
        commands.addSource(id, body.url(), body.text());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/{id}")
    ResponseEntity<Object> get(@PathVariable String id) {
        return queries.getRequest(id)
                .<ResponseEntity<Object>>map(ResponseEntity::ok)
                .orElseGet(() -> notFound(id));
    }

    @GetMapping
    List<?> list(@RequestParam(defaultValue = "20") int limit, @RequestParam(defaultValue = "0") int offset) {
        return queries.listRequests(Math.max(1, limit), Math.max(0, offset));
    }

    @GetMapping("/{id}/result")
    ResponseEntity<Object> result(@PathVariable String id) {
        return queries.getCurrentResult(id)
                .<ResponseEntity<Object>>map(ResponseEntity::ok)
                .orElseGet(() -> notReady(id));
    }

    @GetMapping("/{id}/evidence")
    ResponseEntity<Object> evidence(@PathVariable String id) {
        Optional<List<EvidenceView>> chain = queries.getEvidence(id);
        return chain.<ResponseEntity<Object>>map(ResponseEntity::ok).orElseGet(() -> notReady(id));
    }

    private static Subject toSubject(SubjectDto dto) {
        SubjectKind kind = WireEnum.fromWire(SubjectKind.class, dto.kind());
        return new Subject(dto.id(), kind, dto.displayName(), dto.description());
    }

    private static Optional<DecisionCategory> supportedCategory(String wire) {
        for (DecisionCategory category : DecisionCategory.values()) {
            if (category.wire().equals(wire)) {
                return Optional.of(category);
            }
        }
        return Optional.empty();
    }

    private static ResponseEntity<Object> notFound(String id) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorEnvelope("request_not_found", "Request not found: " + id));
    }

    private static ResponseEntity<Object> notReady(String id) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorEnvelope("result_not_ready", "No current result for request: " + id));
    }
}
