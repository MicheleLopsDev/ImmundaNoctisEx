package io.github.luposolitario.immundanoctisex.tool.editor

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class BookStorageTest {

    private lateinit var dir: File
    private lateinit var libro: File

    @BeforeTest
    fun setup() {
        dir = createTempDirectory("editor-test").toFile()
        libro = File(dir, "libro.json")
        libro.writeText("v1")
    }

    @AfterTest
    fun cleanup() {
        dir.deleteRecursively()
    }

    @Test
    fun salvareScriveIlContenutoNuovo() {
        salvaLibro(libro, "v2")

        assertEquals("v2", libro.readText())
    }

    @Test
    fun salvareRuotaIlBackup() {
        salvaLibro(libro, "v2")

        assertEquals("v1", File(dir, "libro.json.bak1").readText())
    }

    @Test
    fun salvataggiRipetutiSpostanoIBackupPrecedenti() {
        salvaLibro(libro, "v2") // bak1 = v1
        salvaLibro(libro, "v3") // bak1 = v2, bak2 = v1

        assertEquals("v2", File(dir, "libro.json.bak1").readText())
        assertEquals("v1", File(dir, "libro.json.bak2").readText())
        assertEquals("v3", libro.readText())
    }

    @Test
    fun nessunBackupSeIlFileNonEsisteAncora() {
        val nuovo = File(dir, "nuovo.json")

        salvaLibro(nuovo, "v1")

        assertEquals("v1", nuovo.readText())
        assertFalse(File(dir, "nuovo.json.bak1").exists())
    }
}
