package com.acltabontabon.kuro.domain;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * The request lifecycle state machine (#14). Pure policy: framework-free, the
 * single source of truth for which transitions are legal.
 *
 * <p>Normal path: {@code CREATED -> COLLECTING -> EXTRACTING -> SYNTHESIZING ->
 * READY}, with {@code FAILED} reachable from any non-terminal state. {@code
 * READY} and {@code FAILED} are terminal — a re-run is a new result version
 * (#15), never a transition out of a terminal state.
 *
 * <p>{@code INSUFFICIENT_DATA} is deliberately NOT a state: insufficiency is a
 * property of the result ({@code dataSufficiency}); a run that finishes with an
 * insufficient result has still reached {@code READY}.
 */
public final class RequestLifecycle {

    private static final Map<RequestStatus, Set<RequestStatus>> TRANSITIONS = transitions();

    private RequestLifecycle() {
    }

    private static Map<RequestStatus, Set<RequestStatus>> transitions() {
        var map = new EnumMap<RequestStatus, Set<RequestStatus>>(RequestStatus.class);
        map.put(RequestStatus.CREATED, Set.of(RequestStatus.COLLECTING, RequestStatus.FAILED));
        map.put(RequestStatus.COLLECTING, Set.of(RequestStatus.EXTRACTING, RequestStatus.FAILED));
        map.put(RequestStatus.EXTRACTING, Set.of(RequestStatus.SYNTHESIZING, RequestStatus.FAILED));
        map.put(RequestStatus.SYNTHESIZING, Set.of(RequestStatus.READY, RequestStatus.FAILED));
        map.put(RequestStatus.READY, Set.of());
        map.put(RequestStatus.FAILED, Set.of());
        return map;
    }

    /** Whether {@code from -> to} is a legal normal transition. */
    public static boolean canTransition(RequestStatus from, RequestStatus to) {
        return TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }

    /**
     * Reserved edge: a synchronous {@code unsupported_category} refusal lands
     * {@code READY} directly from {@code CREATED} without traversing collection,
     * extraction, or synthesis. This is NOT a general transition — it is never
     * reachable via {@link #canTransition} and only the application refusal path
     * may use it.
     */
    public static boolean isUnsupportedCategoryRefusalLanding(RequestStatus from, RequestStatus to) {
        return from == RequestStatus.CREATED && to == RequestStatus.READY;
    }
}
