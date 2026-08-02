package io.github.luposolitario.immundanoctisex.core.engine.inference

// I token di servizio del modello non devono mai arrivare al lettore
// (02/08/2026): la prima traduzione fatta nell'editor è uscita come
// "<pad>Stai in piedi sul bordo della Foresta…". Sul telefono non era
// mai successo — là il campionamento usa il sampler WebGPU dedicato,
// sul PC ripiega su quello staticamente linkato (lo dice il log:
// "WebGPU sampler not available, falling back to statically linked C
// API") — ma è esattamente il genere di differenza che non deve
// diventare un difetto visibile solo su una piattaforma.
//
// Sta in :core:engine, non nell'editor: client ed editor devono
// ripulire allo stesso modo, altrimenti la stessa risposta si vedrebbe
// diversa nei due posti — proprio ciò che la simulazione deve evitare.
private val TOKEN_DI_SERVIZIO = listOf(
    "<pad>",
    "<eos>",
    "<bos>",
    // Marcatori di turno e di canale del formato Gemma 4 (si leggono
    // nel `jinja_prompt_template` del modello).
    "<|turn>",
    "<turn|>",
    "<|channel>",
    "<channel|>",
    "<|think|>",
    "<end_of_turn>",
    "<start_of_turn>",
)

// Toglie i token di servizio dal testo generato. Idempotente e sicura
// sui pezzi di uno stream: se un token venisse spezzato a metà fra due
// blocchi non verrebbe riconosciuto, ma applicandola anche al testo
// completo il residuo sparisce comunque.
//
// NON tocca la punteggiatura né gli spazi interni: qui si rimuove
// soltanto ciò che il modello non intendeva far leggere a nessuno.
fun ripulisciTokenDiServizio(grezzo: String): String {
    var testo = grezzo
    TOKEN_DI_SERVIZIO.forEach { token ->
        if (testo.contains(token)) testo = testo.replace(token, "")
    }
    return testo
}
