package io.github.luposolitario.immundanoctisex.core.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

// Comando di scena scritto dall'autore del libro (mai generato da Gemma).
// Rappresentazione generica command+params: i tipi puri per i 18 comandi
// (addItem, removeItem, rollOnItemTable...) sono comportamento e arrivano
// con :core:engine in Fase 2. Qui serve solo a caricare/validare il
// pacchetto — i validatori che devono leggere i parametri (es.
// rollOnItemTable) lo fanno leggendo "params" direttamente.
@Serializable
data class GameMechanic(
    val command: String,
    val params: JsonObject = JsonObject(emptyMap()),
)
