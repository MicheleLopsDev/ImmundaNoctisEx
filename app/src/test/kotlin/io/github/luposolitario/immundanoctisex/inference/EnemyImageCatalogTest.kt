package io.github.luposolitario.immundanoctisex.inference

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EnemyImageCatalogTest {

    @Test
    fun unNomeDelCatalogoEValido() {
        assertTrue(EnemyImageCatalog.isValid("enemy_giak"))
        assertTrue(EnemyImageCatalog.isValid("beast_wolves"))
    }

    @Test
    fun unNomeInventatoNonEValido() {
        assertFalse(EnemyImageCatalog.isValid("enemy_dragon_inventato"))
    }

    @Test
    fun nullNonEValido() {
        assertFalse(EnemyImageCatalog.isValid(null))
    }
}
