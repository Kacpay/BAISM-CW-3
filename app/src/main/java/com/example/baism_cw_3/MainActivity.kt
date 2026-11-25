package com.example.baism_cw_3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.baism_cw_3.ui.theme.BAISMCW3Theme
import java.io.IOException
import javax.net.ssl.SSLException
import kotlinx.coroutines.launch
import org.json.JSONException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BAISMCW3Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    CharacterScreen(Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
private fun CharacterScreen(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var idText by rememberSaveable { mutableStateOf("1") }
    var characters by remember { mutableStateOf(emptyList<Character>()) }
    var nextPage by remember { mutableStateOf<Int?>(1) }
    var selectedCharacter by remember { mutableStateOf<Character?>(null) }
    var requestedId by remember { mutableStateOf<Int?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun request(action: suspend () -> Unit) {
        loading = true
        error = null
        scope.launch {
            try {
                action()
            } catch (exception: HttpError) {
                error = if (exception.statusCode == 404) {
                    "Nie znaleziono postaci (HTTP 404)."
                } else {
                    "API zwróciło błąd HTTP ${exception.statusCode}."
                }
            } catch (exception: SSLException) {
                error = "Błąd TLS: ${exception.message ?: "nie udało się połączyć"}."
            } catch (exception: IOException) {
                error = "Błąd połączenia z API: ${exception.message ?: "spróbuj ponownie"}."
            } catch (_: JSONException) {
                error = "Odpowiedź API ma nieoczekiwany format."
            } finally {
                loading = false
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Rick i Morty", style = MaterialTheme.typography.headlineMedium)
                Button(
                    enabled = !loading && nextPage != null,
                    onClick = {
                        nextPage?.let { page ->
                            request {
                                val result = CharacterApi.listCharacters(page)
                                characters = characters + result.characters
                                nextPage = if (result.hasNextPage) page + 1 else null
                            }
                        }
                    }
                ) {
                    Text(
                        when (nextPage) {
                            1 -> "Pobierz listę postaci"
                            null -> "Pobrano wszystkie postacie"
                            else -> "Pobierz kolejną stronę"
                        }
                    )
                }
                val id = idText.trim().toIntOrNull()?.takeIf { it > 0 }
                OutlinedTextField(
                    value = idText,
                    onValueChange = { idText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("ID postaci") },
                    singleLine = true,
                    enabled = !loading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Button(
                    enabled = !loading && id != null,
                    onClick = {
                        id?.let {
                            requestedId = it
                            selectedCharacter = null
                            request { selectedCharacter = CharacterApi.getCharacter(it) }
                        }
                    }
                ) {
                    Text("Pobierz postać")
                }
                if (loading) Text("Pobieranie danych…")
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                requestedId?.let { Text("Żądane ID: $it") }
                selectedCharacter?.let {
                    Text("${it.name} (ID ${it.id})", style = MaterialTheme.typography.titleMedium)
                    Text("Status: ${it.status} · Gatunek: ${it.species}")
                    Text("Pochodzenie: ${it.origin}")
                }
            }
        }
        if (characters.isNotEmpty()) {
            item {
                Text("Lista postaci (${characters.size})", style = MaterialTheme.typography.titleLarge)
            }
        }
        items(characters, key = { it.id }) { character ->
            Text("${character.id}. ${character.name}")
        }
    }
}
