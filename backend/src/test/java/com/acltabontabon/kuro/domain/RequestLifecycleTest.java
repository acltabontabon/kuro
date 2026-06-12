package com.acltabontabon.kuro.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RequestLifecycleTest {

    @Test
    void allowsTheLinearForwardPath() {
        assertThat(RequestLifecycle.canTransition(RequestStatus.CREATED, RequestStatus.COLLECTING)).isTrue();
        assertThat(RequestLifecycle.canTransition(RequestStatus.COLLECTING, RequestStatus.EXTRACTING)).isTrue();
        assertThat(RequestLifecycle.canTransition(RequestStatus.EXTRACTING, RequestStatus.SYNTHESIZING)).isTrue();
        assertThat(RequestLifecycle.canTransition(RequestStatus.SYNTHESIZING, RequestStatus.READY)).isTrue();
    }

    @Test
    void allowsFailureFromEveryNonTerminalState() {
        assertThat(RequestLifecycle.canTransition(RequestStatus.CREATED, RequestStatus.FAILED)).isTrue();
        assertThat(RequestLifecycle.canTransition(RequestStatus.COLLECTING, RequestStatus.FAILED)).isTrue();
        assertThat(RequestLifecycle.canTransition(RequestStatus.EXTRACTING, RequestStatus.FAILED)).isTrue();
        assertThat(RequestLifecycle.canTransition(RequestStatus.SYNTHESIZING, RequestStatus.FAILED)).isTrue();
    }

    @Test
    void rejectsIllegalAndTerminalTransitions() {
        assertThat(RequestLifecycle.canTransition(RequestStatus.READY, RequestStatus.COLLECTING)).isFalse();
        assertThat(RequestLifecycle.canTransition(RequestStatus.CREATED, RequestStatus.SYNTHESIZING)).isFalse();
        assertThat(RequestLifecycle.canTransition(RequestStatus.FAILED, RequestStatus.COLLECTING)).isFalse();
        assertThat(RequestLifecycle.canTransition(RequestStatus.READY, RequestStatus.FAILED)).isFalse();
    }

    @Test
    void createdToReadyIsReservedForRefusalsOnly() {
        assertThat(RequestLifecycle.canTransition(RequestStatus.CREATED, RequestStatus.READY)).isFalse();
        assertThat(RequestLifecycle.isUnsupportedCategoryRefusalLanding(RequestStatus.CREATED, RequestStatus.READY))
                .isTrue();
        assertThat(RequestLifecycle.isUnsupportedCategoryRefusalLanding(RequestStatus.COLLECTING, RequestStatus.READY))
                .isFalse();
        assertThat(RequestLifecycle.isUnsupportedCategoryRefusalLanding(RequestStatus.CREATED, RequestStatus.COLLECTING))
                .isFalse();
    }
}
