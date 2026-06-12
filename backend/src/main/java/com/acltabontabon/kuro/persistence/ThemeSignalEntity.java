package com.acltabontabon.kuro.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

// Theme.signalIds join rows; ordinal preserves the schema's array order.
@Entity
@Table(name = "theme_signal")
class ThemeSignalEntity extends BaseEntity {

    String themeId;
    String signalId;
    int ordinal;
}
