package com.acltabontabon.kuro.domain;

// mirrors packages/schemas/src/evidence.ts (discriminated union on "kind")
public sealed interface Locator {

    LocatorKind kind();

    record CharRange(int start, int end) implements Locator {
        public CharRange {
            if (start < 0 || end < start) {
                throw new IllegalArgumentException("charRange requires 0 <= start <= end");
            }
        }

        @Override
        public LocatorKind kind() {
            return LocatorKind.CHAR_RANGE;
        }
    }

    record LineRange(int startLine, int endLine) implements Locator {
        public LineRange {
            if (startLine < 1 || endLine < startLine) {
                throw new IllegalArgumentException("lineRange requires 1 <= startLine <= endLine");
            }
        }

        @Override
        public LocatorKind kind() {
            return LocatorKind.LINE_RANGE;
        }
    }

    record Anchor(String value) implements Locator {
        public Anchor {
            if (value == null || value.isEmpty()) {
                throw new IllegalArgumentException("anchor requires a non-empty value");
            }
        }

        @Override
        public LocatorKind kind() {
            return LocatorKind.ANCHOR;
        }
    }
}
