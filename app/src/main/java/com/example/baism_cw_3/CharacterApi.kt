package com.example.baism_cw_3

import java.io.IOException
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class Character(
    val id: Int,
    val name: String,
    val status: String,
    val species: String,
    val origin: String
)

data class CharacterPage(val characters: List<Character>, val hasNextPage: Boolean)

class HttpError(val statusCode: Int) : IOException("HTTP $statusCode")

object CharacterApi {
    private const val CHARACTER_URL = "https://rickandmortyapi.com/api/character"

    suspend fun listCharacters(page: Int): CharacterPage {
        val url = if (page == 1) CHARACTER_URL else "$CHARACTER_URL?page=$page"
        val response = getJson(url)
        val results = response.getJSONArray("results")
        return CharacterPage(
            characters = List(results.length()) { index -> results.getJSONObject(index).toCharacter() },
            hasNextPage = !response.getJSONObject("info").isNull("next")
        )
    }

    suspend fun getCharacter(id: Int): Character = getJson("$CHARACTER_URL/$id").toCharacter()

    private suspend fun getJson(url: String): JSONObject = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpsURLConnection
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Accept", "application/json")
            val statusCode = connection.responseCode
            if (statusCode !in 200..299) throw HttpError(statusCode)
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                JSONObject(reader.readText())
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun JSONObject.toCharacter() = Character(
        id = getInt("id"),
        name = getString("name"),
        status = getString("status"),
        species = getString("species"),
        origin = getJSONObject("origin").getString("name")
    )
}
