package mx.zentemi.yuhmu.ui.culture

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.zentemi.yuhmu.ui.components.*
import mx.zentemi.yuhmu.ui.theme.Brand

private data class CultureSection(val title: String, val icon: ImageVector, val color: Color, val body: String)

/** Contenido cultural de Ixtenco, Tlaxcala (RF-09). Para agregar contenido, añade un elemento a SECTIONS. */
private val SECTIONS = listOf(
    CultureSection(
        "Sobre nosotros", Icons.Filled.Favorite, Color(0xFFB0368F),
        "Cuando una lengua muere, una forma de ver al mundo desaparece para siempre. En Ixtenco innovamos digitalmente para conectar la sabiduría de los sabios hablantes con las nuevas generaciones, manteniendo vivo el orgullo Yumhu.\n\n#LenguaYumhu #Libertad"
    ),
    CultureSection(
        "Misión", Icons.Filled.TrackChanges, Brand.purple,
        "Preservar y revitalizar la lengua Yumhu en Ixtenco, Tlaxcala, mediante el desarrollo de herramientas tecnológicas interactivas y accesibles que conecten el conocimiento de los hablantes con el interés digital de las nuevas generaciones, promoviendo el orgullo por la identidad cultural."
    ),
    CultureSection(
        "Visión", Icons.Filled.Visibility, Color(0xFF1FA89A),
        "Ser una plataforma digital de aprendizaje lingüístico y cultural que sirva de referencia en la región, logrando detener la pérdida de la lengua Yumhu, además de expandir nuestra tecnología hacia la preservación de otras lenguas indígenas en peligro de extinción, inspirando un movimiento de difusión del patrimonio originario a través de la innovación digital."
    ),
    CultureSection(
        "Nuestros hablantes", Icons.Filled.Groups, Color(0xFFE08A1E),
        "Concepción Verdad Gaspar, de 80 años, creció escuchando y hablando Yuhmu en casa. Reconoce el valor y la belleza de su lengua, a pesar de la discriminación que vivió en su infancia.\n\nManuel Mexicano trabajó como albañil y campesino; hoy comparte sus conocimientos en la Universidad Intercultural de Tlaxcala y nos enseñó la importancia de la pronunciación.\n\nJuan Cajero nos recuerda que la lengua es parte de las tradiciones, la historia y la identidad de la comunidad, y que necesita espacios donde seguir hablándose."
    ),
    CultureSection(
        "Aliado lingüístico", Icons.Filled.Book, Brand.purpleMid,
        "Rafael Alarcón Montero, cronista y asesor lingüístico, colabora con Zentemi como guía en el desarrollo de los contenidos en Yuhmu: revisa y verifica la escritura y orienta sobre el uso adecuado de la lengua."
    )
)

@Composable
fun CultureScreen(bottomBar: @Composable () -> Unit, onOpenDictionary: () -> Unit) {
    YuhmuScaffold("Cultura", bottomBar = bottomBar) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Mascot(size = 110.dp, mood = Mood.CHEER)
            Text("Cultura Yuhmu", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Brand.purple)
            Text("Ixtenco, Tlaxcala", fontSize = 14.sp, color = Brand.gray)

            Row(
                Modifier.fillMaxWidth().card().clickable(onClick = onOpenDictionary),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(Brand.lavender), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.MenuBook, contentDescription = null, tint = Brand.purple)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Diccionario Yuhmu", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Brand.ink)
                    Text("Consulta vocabulario, significados y contexto", fontSize = 12.sp, color = Brand.gray)
                }
                Icon(Icons.Filled.ChevronRight, contentDescription = "Abrir diccionario", tint = Brand.gray)
            }

            SECTIONS.forEach { s ->
                Column(Modifier.fillMaxWidth().card(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(40.dp).clip(CircleShape).background(s.color), contentAlignment = Alignment.Center) {
                            Icon(s.icon, contentDescription = null, tint = Color.White)
                        }
                        Text(s.title, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = s.color)
                    }
                    Text(s.body, fontSize = 15.sp, color = Brand.ink, lineHeight = 22.sp)
                }
            }
            Text("Un proyecto de Zentemi", fontSize = 12.sp, color = Brand.gray)
        }
    }
}
