package com.acltabontabon.kuro.api;

/** Attach-source body: exactly one of url/text (validated in the application layer). */
record AddSourceDto(String url, String text) {
}
