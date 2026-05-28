package scrapers

interface ElementProxy {
    fun textContent(): String?
    fun getAttribute(name: String): String?
    fun querySelector(selector: String): ElementProxy?
    fun click()
    fun fill(value: String)
}

interface PageProxy {
    fun navigate(url: String)
    fun waitForSelector(selector: String, timeoutMs: Double = 10000.0)
    fun querySelectorAll(selector: String): List<ElementProxy>
    fun querySelector(selector: String): ElementProxy?
    fun fill(selector: String, value: String)
    fun click(selector: String, timeoutMs: Double = 2000.0)
    fun waitForTimeout(timeoutMs: Double)
    fun waitForLoadState(state: String)
    fun title(): String
    fun close()
}
