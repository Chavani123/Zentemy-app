package mx.zentemi.yuhmu.data

/**
 * Historias cortas estilo Duolingo: narración en español con palabras Yuhmu del vocabulario
 * ({id}) y una pregunta por escena. Solo usan palabras de vocabulary.json (validado por el
 * Socio Formador, RNF-05). Si se agregan historias, el texto debe revisarse con el Socio.
 */
data class Scene(
    val emoji: String,
    val text: String,          // usa {categoria_000} para insertar una palabra Yuhmu
    val ask: String,           // id de la palabra por la que se pregunta
    val wrong: List<String>    // ids de distractores
)

data class Story(
    val id: String,
    val title: String,
    val emoji: String,
    val colorIndex: Int,
    val scenes: List<Scene>
)

val STORIES = listOf(
    Story("milpa", "Un día en la milpa", "🌽", 0, listOf(
        Scene("🌅", "Amanece en Ixtenco. Yuhmu corre a la {plantas_030} con su sombrero.", "plantas_030", listOf("plantas_007", "plantas_004", "plantas_028")),
        Scene("🌽", "En la milpa crece el {plantas_028}. ¡Ya hay muchas {plantas_029}!", "plantas_029", listOf("plantas_028", "plantas_018", "plantas_005")),
        Scene("🫘", "Entre las matas también hay {plantas_018} y {plantas_005}.", "plantas_018", listOf("plantas_005", "plantas_032", "plantas_033")),
        Scene("🌸", "Yuhmu encuentra una {plantas_017} morada junto a un {plantas_004}.", "plantas_017", listOf("plantas_004", "plantas_022", "plantas_023")),
        Scene("🌵", "Para la comida, Yuhmu corta un {plantas_032} y vuelve a casa feliz.", "plantas_032", listOf("plantas_043", "plantas_027", "plantas_018"))
    )),
    Story("cocina", "La cocina de la abuela", "🫓", 1, listOf(
        Scene("👵", "Yuhmu visita a su {familia_012} y a su {familia_013}.", "familia_012", listOf("familia_013", "familia_011", "familia_010")),
        Scene("🔥", "La abuela prepara {alimentos_020} calientitas en el comal.", "alimentos_020", listOf("alimentos_018", "alimentos_013", "alimentos_011")),
        Scene("💧", "Yuhmu trae {alimentos_002} fresca del pozo.", "alimentos_002", listOf("alimentos_004", "alimentos_016", "alimentos_017")),
        Scene("☕", "Sobre la mesa hay {alimentos_004} y un rico {alimentos_018}.", "alimentos_018", listOf("alimentos_020", "alimentos_009", "alimentos_010")),
        Scene("🏠", "Llegan {familia_011} y {familia_010}. ¡Toda la familia come junta!", "familia_011", listOf("familia_010", "familia_012", "familia_027"))
    )),
    Story("despierta", "Despierta, Yuhmu", "🌞", 2, listOf(
        Scene("😴", "Yuhmu abre un {cuerpo_026} y luego el otro.", "cuerpo_026", listOf("cuerpo_028", "cuerpo_023", "cuerpo_002")),
        Scene("🐓", "Escucha cantar al gallo con su {cuerpo_028}.", "cuerpo_028", listOf("cuerpo_026", "cuerpo_020", "cuerpo_003")),
        Scene("🙌", "Estira la {cuerpo_020} hacia el cielo.", "cuerpo_020", listOf("cuerpo_031", "cuerpo_011", "cuerpo_038")),
        Scene("🦶", "Pone el {cuerpo_031} en el suelo frío.", "cuerpo_031", listOf("cuerpo_020", "cuerpo_035", "cuerpo_032")),
        Scene("😋", "Abre la {cuerpo_002} y bosteza. ¡Hora del desayuno!", "cuerpo_002", listOf("cuerpo_019", "cuerpo_023", "cuerpo_013"))
    )),
    Story("mazorcas", "Contando mazorcas", "🔢", 3, listOf(
        Scene("🌽", "Yuhmu cuenta las {plantas_029} de la cosecha. Encuentra {numeros_001} sola.", "numeros_001", listOf("numeros_002", "numeros_003", "numeros_004")),
        Scene("🌽", "Más adelante ve {numeros_002} juntas.", "numeros_002", listOf("numeros_001", "numeros_003", "numeros_005")),
        Scene("🧺", "En la canasta caben {numeros_003} más.", "numeros_003", listOf("numeros_004", "numeros_005", "numeros_002")),
        Scene("🌱", "En otro surco hay {numeros_004}.", "numeros_004", listOf("numeros_003", "numeros_005", "numeros_001")),
        Scene("🎉", "¡Al final suma {numeros_005} mazorcas!", "numeros_005", listOf("numeros_004", "numeros_002", "numeros_003"))
    )),
    Story("fiesta", "Fiesta en familia", "🎉", 4, listOf(
        Scene("🎊", "Hoy hay fiesta. Llegan {familia_019} y {familia_020}.", "familia_019", listOf("familia_020", "familia_013", "familia_040")),
        Scene("💃", "Su {familia_027} baila con su {familia_025}.", "familia_027", listOf("familia_025", "familia_020", "familia_002")),
        Scene("🍞", "En la mesa hay {alimentos_013} y {alimentos_018}.", "alimentos_013", listOf("alimentos_018", "alimentos_020", "alimentos_007")),
        Scene("🥣", "{familia_011} sirve {alimentos_004} para todos.", "alimentos_004", listOf("alimentos_002", "alimentos_016", "alimentos_005")),
        Scene("❤️", "Yuhmu abraza a su {familia_012}. ¡Qué linda fiesta!", "familia_012", listOf("familia_013", "familia_014", "familia_011"))
    ))
)
