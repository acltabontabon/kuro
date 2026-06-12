package com.acltabontabon.kuro.domain;

/**
 * A domain enum whose members correspond 1:1 to string literals in the
 * canonical Zod model (packages/schemas/src). {@code wire()} returns that
 * exact literal — it is the value stored in enum columns (matching the DDL
 * CHECK lists) and serialized in JSON payloads.
 */
public interface WireEnum {

    String wire();

    static <E extends Enum<E> & WireEnum> E fromWire(Class<E> type, String wire) {
        for (E constant : type.getEnumConstants()) {
            if (constant.wire().equals(wire)) {
                return constant;
            }
        }
        throw new IllegalArgumentException("Unknown " + type.getSimpleName() + " wire value: " + wire);
    }
}
