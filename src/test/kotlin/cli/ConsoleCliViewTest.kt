package cli

import com.google.common.truth.Truth.assertThat
import models.ScrapeState
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.util.Scanner
import kotlin.test.assertIs

class ConsoleCliViewTest {

    @Test
    fun promptSearchQuery_whenInputIsB_returnsBackResult() {
        val scanner = Scanner("b\n")
        val view = ConsoleCliView(scanner)

        val result = view.promptSearchQuery()

        assertIs<SearchPromptResult.Back>(result)
    }

    @Test
    fun promptSearchQuery_whenInputIsNormal_returnsSearchResult() {
        val scanner = Scanner("milk\n")
        val view = ConsoleCliView(scanner)

        val result = view.promptSearchQuery()

        assertIs<SearchPromptResult.Search>(result)
        assertThat(result.query).isEqualTo("milk")
    }

    @Test
    fun promptSearchQuery_whenInputIsD_treatsAsNormalSearch() {
        val scanner = Scanner("d\n")
        val view = ConsoleCliView(scanner)

        val result = view.promptSearchQuery()

        assertIs<SearchPromptResult.Search>(result)
        assertThat(result.query).isEqualTo("d")
    }
}
