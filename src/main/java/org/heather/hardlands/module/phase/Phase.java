package org.heather.hardlands.module.phase;

public enum Phase {

    IDLE("Inactivo"),
    PREPARATION("Preparación"),

    GRACE_PERIOD("Periodo de Gracia"),
    PVP("Combate Abierto"),
    BORDER_SHRINK("Reducción del Borde"),

    MEETUP("Encuentro"),
    DEATHMATCH("Combate Final"),

    POST_GAME("Fin de la Partida");

    private final String displayName;

    Phase(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public boolean isRunning() {
        return switch (this) {
            case GRACE_PERIOD, PVP, BORDER_SHRINK, MEETUP, DEATHMATCH -> true;
            default -> false;
        };
    }
}