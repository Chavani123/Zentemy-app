package mx.zentemi.yuhmu.ui.learn

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.zentemi.yuhmu.model.Word
import mx.zentemi.yuhmu.ui.components.LearningIllustration
import mx.zentemi.yuhmu.ui.components.card
import mx.zentemi.yuhmu.ui.components.YuhmuScaffold
import mx.zentemi.yuhmu.ui.theme.Brand
import mx.zentemi.yuhmu.viewmodel.YuhmuViewModel
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment

@Composable
fun DictionaryScreen(vm: YuhmuViewModel, bottomBar: @Composable () -> Unit) {
    var query by remember { mutableStateOf("") }
    val words = vm.content.allWords
    val categoryTitles = remember(vm.content.categories) {
        vm.content.categories.associate { it.id to it.title }
    }
    val normalizedQuery = query.trim()
    val filteredWords = remember(words, normalizedQuery) {
        if (normalizedQuery.isBlank()) words
        else words.filter {
            it.spanish.contains(normalizedQuery, ignoreCase = true) ||
                it.yuhmu.contains(normalizedQuery, ignoreCase = true) ||
                (it.note?.contains(normalizedQuery, ignoreCase = true) == true) ||
                (it.context?.contains(normalizedQuery, ignoreCase = true) == true)
        }
    }

    YuhmuScaffold("Diccionario", bottomBar = bottomBar) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Brand.purple) },
                placeholder = { Text("Busca en español o Yuhmu") }
            )
            Text(
                "${filteredWords.size} palabras · consulta su significado, contexto e ilustración",
                Modifier.padding(horizontal = 18.dp),
                fontSize = 12.sp,
                color = Brand.gray
            )
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredWords, key = { it.id }) { word ->
                    DictionaryWordCard(word, categoryTitles[word.categoryId] ?: "Vocabulario")
                }
            }
        }
    }
}

@Composable
private fun DictionaryWordCard(word: Word, category: String) {
    Row(
        Modifier.fillMaxWidth().card(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LearningIllustration(word, size = 62.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(category.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Brand.purpleMid)
            Text(word.yuhmu, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Brand.purple)
            Text(word.spanish, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Brand.ink)
            word.context?.let {
                Text(it, fontSize = 12.sp, lineHeight = 17.sp, color = Brand.gray)
            }
            word.note?.takeIf { it.isNotBlank() }?.let {
                Text(it, fontSize = 11.sp, color = Brand.gray)
            }
        }
    }
}
