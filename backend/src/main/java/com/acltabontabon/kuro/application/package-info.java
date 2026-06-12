/**
 * Application layer: orchestrates request workflows (create, refuse, add
 * source, advance lifecycle) and read queries over the persistence layer,
 * applying the framework-free {@code domain} policy.
 *
 * <p>Dependency direction: {@code application -> domain, persistence}. The api
 * layer depends on this; this must not depend on api.
 */
package com.acltabontabon.kuro.application;
