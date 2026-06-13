/**
 * AI adapter boundary: clients for model providers and prompt/response
 * handling. Defines the vendor-neutral {@link com.acltabontabon.kuro.ai.AiProvider}
 * seam (issue #17) — request/response/options records and the typed
 * {@link com.acltabontabon.kuro.ai.exception.AiProviderException} hierarchy.
 * Vendor SDK imports are confined to this package.
 *
 * <p>Dependency direction: {@code ai → domain}.
 */
package com.acltabontabon.kuro.ai;
