package render

/**
 * Loads bundled assets (CSS/JS) from the classpath so they can be inlined into
 * the produced HTML. The HTML must be self-contained — no external URLs, no
 * file:// neighbours — so every asset is read at render time and embedded.
 */
object Assets {
    fun css(name: String): String = read("/styles/$name")
    fun script(name: String): String = read("/scripts/$name")
    fun vendor(name: String): String = read("/vendor/$name")

    private fun read(path: String): String =
        Assets::class.java.getResourceAsStream(path)
            ?.bufferedReader()
            ?.use { it.readText() }
            ?: error("Bundled asset not found on classpath: $path")
}
