package scrapers

import com.microsoft.playwright.ElementHandle
import com.microsoft.playwright.Page

class PlaywrightElementProxy(private val handle: ElementHandle) : ElementProxy {
    override fun textContent(): String? = handle.textContent()
    override fun getAttribute(name: String): String? = handle.getAttribute(name)
    override fun querySelector(selector: String): ElementProxy? = 
        handle.querySelector(selector)?.let { PlaywrightElementProxy(it) }
    override fun click() {
        handle.click()
    }
    override fun fill(value: String) {
        handle.fill(value)
    }
}

class PlaywrightPageProxy(private val page: Page) : PageProxy {
    override fun navigate(url: String) {
        page.navigate(url)
    }

    override fun waitForSelector(selector: String, timeoutMs: Double) {
        page.waitForSelector(selector, Page.WaitForSelectorOptions().setTimeout(timeoutMs))
    }

    override fun querySelectorAll(selector: String): List<ElementProxy> {
        return page.querySelectorAll(selector).map { PlaywrightElementProxy(it) }
    }

    override fun querySelector(selector: String): ElementProxy? {
        return page.querySelector(selector)?.let { PlaywrightElementProxy(it) }
    }

    override fun fill(selector: String, value: String) {
        page.fill(selector, value)
    }

    override fun click(selector: String, timeoutMs: Double) {
        page.click(selector, Page.ClickOptions().setTimeout(timeoutMs))
    }

    override fun waitForTimeout(timeoutMs: Double) {
        page.waitForTimeout(timeoutMs)
    }

    override fun waitForLoadState(state: String) {
        page.waitForLoadState(com.microsoft.playwright.options.LoadState.valueOf(state.uppercase()))
    }

    override fun title(): String = page.title()

    override fun close() {
        page.close()
    }
}
