/**
 * Persistence adapter: JPA entities, repositories, and mapping between
 * database records and domain types. Schema is owned by Flyway migrations
 * ({@code src/main/resources/db/migration}) — every entity added here must
 * land with a matching migration.
 *
 * <p>Dependency direction: {@code persistence → domain}.
 */
package com.acltabontabon.kuro.persistence;
