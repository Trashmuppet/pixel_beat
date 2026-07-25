package com.trashmuppet.pixelbeat.assetcompiler

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO

/**
 * Determinism / spec assertions for [main].
 * Per [11_ASSET_COMPILER.md] "Compilation must be deterministic."
 */
class CompilerTest {

    @Test
    fun `deterministic output for identical inputs`() {
        val temp = Files.createTempDirectory("mbscene_in").toFile()
        val out1 = File(temp, "out1.mbscene")
        val out2 = File(temp, "out2.mbscene")

        try {
            // 1-bit spec-compliant sprite + manifest.
            writeFixture(temp, label = "alpha", originX = 2, originY = 4)
            writeFixture(temp, label = "beta", originX = 0, originY = 0)

            compileFixture(temp, out1)
            compileFixture(temp, out2)

            assertArrayEquals(
                out1.readBytes(),
                out2.readBytes(),
                "Two identical runs must produce identical .mbscene bytes"
            )
        } finally {
            temp.deleteRecursively()
        }
    }

    @Test
    fun `output has valid 16-byte magic header`() {
        val temp = Files.createTempDirectory("mbscene_in").toFile()
        val out = File(temp, "out.mbscene")

        try {
            writeFixture(temp, label = "only", originX = 0, originY = 0)
            compileFixture(temp, out)

            val bytes = out.readBytes()
            val magic = bytes.copyOfRange(0, 7).toString(Charsets.US_ASCII)
            val padding = bytes.copyOfRange(8, 16)

            assertEquals("MBSCENE", magic, "Magic header bytes")
            assertArrayEquals(ByteArray(8), padding, "8-byte zero pad after magic")
        } finally {
            temp.deleteRecursively()
        }
    }

    @Test
    fun `non-monochrome color is rejected`() {
        val temp = Files.createTempDirectory("mbscene_bad").toFile()
        val out = File(temp, "out.mbscene")

        try {
            // Sprite has a single red pixel — spec requires pure B/W only.
            val img = BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB)
            val g = img.createGraphics()
            g.color = Color.RED
            g.fillRect(2, 2, 4, 4)
            g.dispose()
            ImageIO.write(img, "png", File(temp, "red.png"))
            File(temp, "manifest.json").writeText(
                """{"packId":"bad","packVersion":1,"sprites":{}}"""
            )

            assertThrows<IllegalArgumentException> {
                compileFixture(temp, out)
            }
        } finally {
            temp.deleteRecursively()
        }
    }

    private fun writeFixture(dir: File, label: String, originX: Int, originY: Int) {
        val img = BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        g.color = Color.WHITE
        g.fillRect(2, 2, 8, 8)
        g.dispose()
        ImageIO.write(img, "png", File(dir, "$label.png"))
        // Manifest overrides aren't necessary for the base determinism test
        // — the simple PNG packId/version comes from CLI flags below.
    }

    private fun compileFixture(temp: File, out: File) {
        main(
            arrayOf(
                "--in", temp.absolutePath,
                "--out", out.absolutePath,
                "--packId", "test-pack",
                "--version", "1"
            )
        )
    }
}
