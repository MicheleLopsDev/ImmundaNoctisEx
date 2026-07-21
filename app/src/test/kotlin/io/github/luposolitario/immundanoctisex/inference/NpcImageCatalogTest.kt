package io.github.luposolitario.immundanoctisex.inference

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NpcImageCatalogTest {

    @Test
    fun unNomeDelCatalogoEValido() {
        assertTrue(NpcImageCatalog.isValid("npc_king"))
        assertTrue(NpcImageCatalog.isValid("hero_female"))
    }

    @Test
    fun unNomeInventatoNonEValido() {
        assertFalse(NpcImageCatalog.isValid("npc_wizard_inventato"))
    }

    @Test
    fun nullNonEValido() {
        assertFalse(NpcImageCatalog.isValid(null))
    }
}
