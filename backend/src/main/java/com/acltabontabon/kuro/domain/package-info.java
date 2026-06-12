/**
 * Core domain model of KURO: framework-free types mirroring the canonical
 * {@code @kuro/schemas} vocabulary.
 *
 * <p>Dependency direction: depends on <strong>nothing internal and no
 * frameworks</strong> — no Spring, no JPA/Jakarta persistence, no Hibernate,
 * no Flyway. All other packages may depend on this one; this one depends on
 * none of them. Enforced by the ArchUnit boundary test.
 */
package com.acltabontabon.kuro.domain;
