package mx.zentemi.yuhmu.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.zentemi.yuhmu.model.AVATARS
import mx.zentemi.yuhmu.security.Validators
import mx.zentemi.yuhmu.ui.components.*
import mx.zentemi.yuhmu.ui.theme.Brand
import mx.zentemi.yuhmu.viewmodel.AuthViewModel

// ---------------------------------------------------------------- piezas reutilizables

@Composable
fun YuhmuTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    password: Boolean = false,
    imeAction: ImeAction = ImeAction.Next,
    leading: ImageVector? = null,
    maxLength: Int = 64,
    onDone: () -> Unit = {}
) {
    var visible by remember { mutableStateOf(false) }
    val leadingContent: (@Composable () -> Unit)? =
        if (leading != null) { { Icon(leading, contentDescription = null) } } else null
    val trailingContent: (@Composable () -> Unit)? =
        if (password) {
            {
                IconButton(onClick = { visible = !visible }) {
                    Icon(
                        if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (visible) "Ocultar contraseña" else "Mostrar contraseña"
                    )
                }
            }
        } else null

    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= maxLength) onValueChange(it) },
        label = { Text(label) },
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        leadingIcon = leadingContent,
        trailingIcon = trailingContent,
        visualTransformation = if (password && !visible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (password) KeyboardType.Password else KeyboardType.Text,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Brand.purpleMid,
            unfocusedBorderColor = Brand.grayLight,
            focusedLabelColor = Brand.purple,
            cursorColor = Brand.purple,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        )
    )
}

@Composable
private fun ErrorBanner(message: String) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Brand.redLight).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = Brand.redDark)
        Text(message, color = Brand.redDark, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun RuleRow(ok: Boolean, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(
            if (ok) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (ok) Brand.green else Brand.grayEdge,
            modifier = Modifier.size(18.dp)
        )
        Text(text, fontSize = 13.sp, color = if (ok) Brand.greenDark else Brand.gray)
    }
}

@Composable
private fun StrengthMeter(password: String) {
    val strength = Validators.strength(password)
    val colors = listOf(Brand.red, Brand.orange, Brand.gold, Brand.green)
    val labels = listOf("Muy débil", "Débil", "Regular", "Buena", "Excelente")
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(4) { i ->
                Box(
                    Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(50))
                        .background(if (password.isNotEmpty() && i < strength) colors[(strength - 1).coerceAtLeast(0)] else Brand.grayLight)
                )
            }
        }
        if (password.isNotEmpty()) {
            Text("Seguridad: ${labels[strength]}", fontSize = 12.sp, color = Brand.gray)
        }
    }
}

@Composable
private fun AuthScaffold(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    SecureWindowEffect()
    Column(
        Modifier.fillMaxSize().background(Brand.bg).systemBarsPadding().imePadding()
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = Brand.purple)
            }
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Brand.purple)
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content
        )
    }
}

// ---------------------------------------------------------------- pantallas

@Composable
fun SplashScreen() {
    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Brand.purple, Brand.purpleMid))),
        contentAlignment = Alignment.Center
    ) {
        Mascot(size = 130.dp)
    }
}

@Composable
fun WelcomeScreen(onStart: () -> Unit, onHaveAccount: () -> Unit) {
    Column(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Brand.purple, Brand.purpleMid)))
            .systemBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        Mascot(size = 170.dp, mood = Mood.CHEER)
        Spacer(Modifier.height(20.dp))
        Text("Zentemi", fontSize = 46.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
        Text(
            "Aprende la lengua otomí de Ixtenco, Tlaxcala, jugando.",
            fontSize = 17.sp, color = Brand.lavender, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(Modifier.weight(1f))
        ChunkyButton("Empezar", color = Color.White, edge = Brand.lavender, contentColor = Brand.purple, onClick = onStart)
        Spacer(Modifier.height(12.dp))
        ChunkyButton("Ya tengo una cuenta", color = Brand.purpleMid, edge = Brand.purpleDark, onClick = onHaveAccount)
    }
}

@Composable
fun RegisterScreen(auth: AuthViewModel, onBack: () -> Unit) {
    val busy by auth.busy.collectAsState()
    val error by auth.error.collectAsState()
    var username by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var avatar by remember { mutableStateOf(AVATARS.first()) }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { auth.clearError() }

    val mismatch = confirm.isNotEmpty() && confirm != password
    val canSubmit = !busy && username.isNotBlank() && displayName.isNotBlank() &&
        password.isNotEmpty() && password == confirm

    AuthScaffold("Crea tu perfil", onBack) {
        Text("Elige tu personaje", fontWeight = FontWeight.Bold, color = Brand.ink)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            mx.zentemi.yuhmu.model.AppCharacter.entries.forEach { ch ->
                val isSelected = ch.id == avatar
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isSelected) Brand.lavender else Color.White)
                        .border(2.dp, if (isSelected) Brand.purpleMid else Brand.grayLight, RoundedCornerShape(18.dp))
                        .clickable { avatar = ch.id }
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.5.dp, if (isSelected) Brand.purpleMid else Brand.grayEdge, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = ch.drawableRes),
                            contentDescription = ch.characterName,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(ch.characterName.split(" ").first(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Brand.purple)
                }
            }
        }
        YuhmuTextField(displayName, { displayName = it }, "¿Cómo te llamamos?", leading = Icons.Filled.Face, maxLength = 20)
        YuhmuTextField(username, { username = it.filter { c -> c.isLetterOrDigit() || c == '_' } }, "Nombre de usuario", leading = Icons.Filled.AlternateEmail, maxLength = 20)
        YuhmuTextField(password, { password = it }, "Contraseña", password = true, leading = Icons.Filled.Lock)
        StrengthMeter(password)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            RuleRow(Validators.hasMinLength(password), "Al menos ${Validators.MIN_PASSWORD} caracteres")
            RuleRow(Validators.hasLetter(password), "Contiene letras")
            RuleRow(Validators.hasDigit(password), "Contiene números")
        }
        YuhmuTextField(confirm, { confirm = it }, "Repite la contraseña", password = true, leading = Icons.Filled.Lock, imeAction = ImeAction.Done)
        if (mismatch) ErrorBanner("Las contraseñas no coinciden.")
        error?.let { ErrorBanner(it) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Filled.Shield, contentDescription = null, tint = Brand.purpleMid, modifier = Modifier.size(18.dp))
            Text(
                "No pedimos correo ni datos personales. Tu cuenta y tu progreso se guardan sólo en este dispositivo.",
                fontSize = 12.sp, color = Brand.gray
            )
        }
        ChunkyButton("Crear cuenta", enabled = canSubmit) { auth.register(username, displayName, avatar, password) }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun LoginScreen(auth: AuthViewModel, onBack: () -> Unit, onRecover: () -> Unit) {
    val busy by auth.busy.collectAsState()
    val error by auth.error.collectAsState()
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { auth.clearError() }
    val canSubmit = !busy && username.isNotBlank() && password.isNotEmpty()

    AuthScaffold("Inicia sesión", onBack) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Mascot(size = 110.dp) }
        YuhmuTextField(username, { username = it.filter { c -> c.isLetterOrDigit() || c == '_' } }, "Nombre de usuario", leading = Icons.Filled.Person, maxLength = 20)
        YuhmuTextField(
            password, { password = it }, "Contraseña", password = true, leading = Icons.Filled.Lock,
            imeAction = ImeAction.Done, onDone = { if (canSubmit) auth.login(username, password) }
        )
        error?.let { ErrorBanner(it) }
        ChunkyButton("Entrar", enabled = canSubmit) { auth.login(username, password) }
        TextButton(onClick = onRecover, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("¿Olvidaste tu contraseña?", color = Brand.purpleMid, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun RecoverScreen(auth: AuthViewModel, onBack: () -> Unit) {
    val busy by auth.busy.collectAsState()
    val error by auth.error.collectAsState()
    var username by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { auth.clearError() }
    val canSubmit = !busy && username.isNotBlank() && code.isNotBlank() && password.isNotEmpty()

    AuthScaffold("Recuperar cuenta", onBack) {
        Text(
            "Escribe tu usuario y el código de recuperación que guardaste al crear tu cuenta.",
            color = Brand.ink, fontSize = 15.sp
        )
        YuhmuTextField(username, { username = it.filter { c -> c.isLetterOrDigit() || c == '_' } }, "Nombre de usuario", leading = Icons.Filled.Person, maxLength = 20)
        YuhmuTextField(code, { code = it.uppercase() }, "Código de recuperación", leading = Icons.Filled.Key, maxLength = 14)
        YuhmuTextField(password, { password = it }, "Nueva contraseña", password = true, leading = Icons.Filled.Lock, imeAction = ImeAction.Done)
        StrengthMeter(password)
        error?.let { ErrorBanner(it) }
        ChunkyButton("Restablecer contraseña", enabled = canSubmit) { auth.recover(username, code, password) }
    }
}

/** Se muestra una sola vez: el código no se guarda en claro en ningún sitio. */
@Composable
fun RecoveryCodeScreen(code: String, onDone: () -> Unit) {
    SecureWindowEffect()
    var saved by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().background(Brand.bg).systemBarsPadding().padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Mascot(size = 110.dp, mood = Mood.CHEER)
        Text("¡Guarda tu código secreto!", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Brand.purple, textAlign = TextAlign.Center)
        Text(
            "Si olvidas tu contraseña, este código es la única forma de recuperar tu cuenta. Anótalo en un papel y guárdalo con un adulto de confianza.",
            fontSize = 15.sp, color = Brand.ink, textAlign = TextAlign.Center
        )
        Box(
            Modifier.fillMaxWidth().background(Brand.lavender, RoundedCornerShape(18.dp))
                .border(2.dp, Brand.purpleLight, RoundedCornerShape(18.dp)).padding(vertical = 22.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(code, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Brand.purple, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
        }
        Text("No se volverá a mostrar y no lo compartas con nadie.", fontSize = 12.sp, color = Brand.gray, textAlign = TextAlign.Center)
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { saved = !saved }.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = saved, onCheckedChange = { saved = it }, colors = CheckboxDefaults.colors(checkedColor = Brand.green))
            Text("Ya guardé mi código", fontWeight = FontWeight.Bold, color = Brand.ink)
        }
        Spacer(Modifier.weight(1f))
        ChunkyButton("Continuar", enabled = saved, onClick = onDone)
    }
}
