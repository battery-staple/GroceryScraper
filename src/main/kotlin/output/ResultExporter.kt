package output

import models.FinalResponse

/**
 * Interface for exporting scraping results to various output targets.
 */
interface ResultExporter {
    /**
     * Exports the given scraping results for the specified query.
     *
     * @param query The search query that was scraped.
     * @param response The final response containing results and failures.
     */
    fun export(query: String, response: FinalResponse)
}
