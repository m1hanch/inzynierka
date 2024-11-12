package org.bugReportSystem.enums;

public enum Priority {
    LOWEST,
    LOW,
    MEDIUM,
    HIGH,
    HIGHEST;

    public static Priority stringToPriority(String name) {
        return Priority.valueOf(name);
    }
}
