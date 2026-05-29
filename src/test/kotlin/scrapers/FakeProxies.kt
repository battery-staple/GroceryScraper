package scrapers

/**
 * Fake implementation of an element proxy to simulate DOM node behavior during testing.
 * @property text The simulated text content.
 * @property attrs The simulated attributes map.
 * @property children The simulated child nodes map.
 */
class FakeElementProxy(
    private val text: String? = null,
    private val attrs: Map<String, String> = emptyMap(),
    private val children: Map<String, FakeElementProxy> = emptyMap()
) : ElementProxy {
    /** Returns the simulated text. */
    override fun textContent(): String? = text
    /** Returns the simulated attribute value for [name]. */
    override fun getAttribute(name: String): String? = attrs[name]
    /** Returns the simulated matching child for [selector]. */
    override fun querySelector(selector: String): ElementProxy? = children[selector]
    /** Simulates clicking the element. */
    override fun click() {}
    /** Simulates filling the element with [value]. */
    override fun fill(value: String) {}
}

/**
 * Fake implementation of [PageProxy] to simulate the behavior of the entire page being scraped.
 *
 * @property pageTitle The simulated title of the page.
 * @property onFill An optional callback invoked when [fill] is called.
 */
class FakePageProxy(
    private val pageTitle: String = "Fake Page",
    private val onFill: ((selector: String, value: String) -> Unit)? = null
) : PageProxy {
    /** The URL the page navigated to. */
    var navigatedUrl: String? = null
    /** The list of fake elements on the page. */
    var elements: List<FakeElementProxy> = emptyList()
    /** Controls whether actions should throw a timeout exception. */
    var shouldTimeout: Boolean = false

    /** Records the [url] navigated to. */
    override fun navigate(url: String) { navigatedUrl = url }
    /** Simulates waiting for [selector]. */
    override fun waitForSelector(selector: String, timeoutMs: Double) {
        if (shouldTimeout) throw Exception("Timeout")
    }
    /** Returns all fake [elements]. */
    override fun querySelectorAll(selector: String): List<ElementProxy> = elements
    /** Returns null. */
    override fun querySelector(selector: String): ElementProxy? = null
    /** Simulates filling a selector with value and calls [onFill] if provided. */
    override fun fill(selector: String, value: String) {
        onFill?.invoke(selector, value)
    }
    /** Simulates clicking a selector. */
    override fun click(selector: String, timeoutMs: Double) {}
    /** Simulates waiting. */
    override fun waitForTimeout(timeoutMs: Double) {}
    /** Simulates waiting for load state. */
    override fun waitForLoadState(state: String) {}
    /** Returns the simulated title. */
    override fun title(): String = pageTitle
    /** Simulates closing the page. */
    override fun close() {}
}
