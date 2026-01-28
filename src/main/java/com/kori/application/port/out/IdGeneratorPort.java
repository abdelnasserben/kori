package com.kori.application.port.out;

import java.util.UUID;

/**
 * Générateur d'identifiants techniques.
 */
public interface IdGeneratorPort {

    UUID newUuid();
}
