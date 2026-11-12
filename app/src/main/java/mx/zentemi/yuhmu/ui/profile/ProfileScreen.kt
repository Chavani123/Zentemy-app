package mx.zentemi.yuhmu.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.zentemi.yuhmu.data.*
import mx.zentemi.yuhmu.model.SecurityEvent
import mx.zentemi.yuhmu.model.User
import mx.zentemi.yuhmu.ui.auth.YuhmuTextField
import mx.zentemi.yuhmu.ui.components.*
import mx.zentemi.yuhmu.ui.theme.Brand
import mx.zentemi.yuhmu.util.AppLanguage
import mx.zentemi.yuhmu.util.tr
import mx.zentemi.yuhmu.viewmodel.AuthViewModel
import mx.zentemi.yuhmu.viewmodel.YuhmuViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class Dialog { NONE, NAME, PASSWORD, RESET, DELETE, LANGUAGE }

private val EVENT_NAMES = mapOf(
    "REGISTER" to "Cuenta creada",
    "LOGIN_OK" to "Inicio de sesión",
    "LOGIN_FAIL" to "Intento de acceso fallido",
    "LOCKOUT" to "Cuenta bloqueada temporalmente",
    "LOGOUT" to "Cierre de sesión",
    "PASSWORD_CHANGE" to "Contraseña cambiada",
    "PASSWORD_CHANGE_FAIL" to "Cambio de contraseña fallido",
    "PASSWORD_RESET" to "Contraseña restablecida con código",
    "PROGRESS_RESET" to "Progreso reiniciado",
    "ACCOUNT_DELETE_FAIL" to "Intento de borrar la cuenta fallido"
)

@Composable
fun ProfileScreen(user: User, auth: AuthViewModel, vm: YuhmuViewModel, bottomBar: @Composable () -> Unit) {
    val progress by vm.progress.collectAsState()
    val currentLang by vm.language.collectAsState()
    var dialog by remember { mutableStateOf(Dialog.NONE) }
    var showEvents by remember { mutableStateOf(false) }
    var showAccountOptions by remember { mutableStateOf(false) }
    val events by produceState(initialValue = emptyList<SecurityEvent>(), user.id, showEvents) {
        value = if (showEvents) auth.recentEvents() else emptyList()
    }
    val joined = remember(user.createdAt, currentLang) {
        SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag(currentLang.code)).format(Date(user.createdAt))
    }

    YuhmuScaffold("nav_profile".tr(), bottomBar = bottomBar) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                Modifier.size(104.dp).clip(CircleShape).background(Brand.lavender).border(4.dp, Brand.purpleLight, CircleShape),
                contentAlignment = Alignment.Center
            ) { Text(user.avatar, fontSize = 52.sp) }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(user.displayName, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Brand.purple)
                Icon(Icons.Filled.Edit, contentDescription = "change_name".tr(), tint = Brand.purpleMid,
                    modifier = Modifier.size(22.dp).clickable { dialog = Dialog.NAME })
            }
            Text("@${user.username} · $joined", fontSize = 13.sp, color = Brand.gray)
            Text(
                "Nivel ${progress.level()} · ${progress.levelTitle()}",
                fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color.White,
                modifier = Modifier.background(Brand.green, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 4.dp)
            )

            // Resumen de progreso visible desde el perfil, como pide la matriz UAT.
            Column(Modifier.fillMaxWidth().card(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Tu progreso", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Brand.ink)
                    Text("${progress.points} XP", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Brand.purple)
                }
                ChunkyProgress(progress.levelProgress(), color = Brand.gold, height = 12.dp)
                Text("${progress.pointsToNextLevel()} XP para el siguiente nivel", fontSize = 12.sp, color = Brand.gray)
                androidx.compose.material3.HorizontalDivider(color = Brand.lavender.copy(alpha = 0.8f))
                ProfileProgressRow("Palabras aprendidas", progress.learnedWordIds.size, vm.content.allWords.size, Brand.purpleMid)
                ProfileProgressRow("Lecciones completadas", progress.completedLessons.size, vm.content.lessons.size, Brand.green)
            }

            Column(Modifier.fillMaxWidth().card(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("your_achievements".tr(), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Brand.ink)
                InfoRow("current_streak".tr(), "${progress.currentStreak()} ${"days".tr()}")
                InfoRow("completed_lessons".tr(), "${progress.completedLessons.size}")
                InfoRow("learned_words".tr(), "${progress.learnedWordIds.size}")
                InfoRow("played_games".tr(), "${progress.gamesPlayed}")
            }

            Column(Modifier.fillMaxWidth().card(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("app_settings".tr(), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Brand.ink)
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { dialog = Dialog.LANGUAGE }.padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Language, contentDescription = null, tint = Brand.purpleMid)
                        Text("app_language".tr(), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Brand.ink)
                    }
                    Text("${currentLang.flag} ${currentLang.displayName}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Brand.purple)
                }
                androidx.compose.material3.HorizontalDivider(color = Brand.lavender.copy(alpha = 0.8f))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { showAccountOptions = !showAccountOptions }.padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Opciones de cuenta", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Brand.gray)
                    Icon(if (showAccountOptions) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null, tint = Brand.gray)
                }
                if (showAccountOptions) {
                    Text("Estas opciones afectan tu cuenta y no se pueden deshacer.", fontSize = 11.sp, color = Brand.gray)
                    ActionRow(Icons.Filled.DeleteForever, "delete_account".tr(), Brand.red.copy(alpha = 0.8f)) { dialog = Dialog.DELETE }
                }
            }

            Column(Modifier.fillMaxWidth().card(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("account_security".tr(), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Brand.ink)
                ActionRow(Icons.Filled.Key, "change_password".tr(), Brand.purpleMid) { dialog = Dialog.PASSWORD }
                ActionRow(Icons.Filled.History, if (showEvents) "hide_recent_activity".tr() else "view_recent_activity".tr(), Brand.purpleMid) { showEvents = !showEvents }
                if (showEvents) {
                    if (events.isEmpty()) Text("Sin actividad registrada.", fontSize = 13.sp, color = Brand.gray, modifier = Modifier.padding(start = 40.dp))
                    events.forEach { e ->
                        Row(Modifier.fillMaxWidth().padding(start = 40.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(EVENT_NAMES[e.type] ?: e.type, fontSize = 13.sp, color = Brand.ink)
                            Text(
                                SimpleDateFormat("d MMM, HH:mm", Locale.forLanguageTag(currentLang.code)).format(Date(e.createdAt)),
                                fontSize = 12.sp, color = Brand.gray
                            )
                        }
                    }
                }
                ActionRow(Icons.AutoMirrored.Filled.Logout, "log_out".tr(), Brand.purple) { auth.logout() }
            }

            Spacer(Modifier.height(8.dp))
        }
    }

    when (dialog) {
        Dialog.NAME -> EditNameDialog(user.displayName, auth) { dialog = Dialog.NONE }
        Dialog.PASSWORD -> ChangePasswordDialog(auth) { dialog = Dialog.NONE }
        Dialog.DELETE -> DeleteAccountDialog(auth) { dialog = Dialog.NONE }
        Dialog.LANGUAGE -> LanguageSelectionDialog(currentLang, onSelect = { vm.setLanguage(it) }) { dialog = Dialog.NONE }
        Dialog.RESET -> Unit
        Dialog.NONE -> Unit
    }
}

@Composable
private fun LanguageSelectionDialog(
    current: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("select_language".tr(), fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppLanguage.entries.forEach { lang ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (lang == current) Brand.lavender else Color.Transparent)
                            .clickable {
                                onSelect(lang)
                                onDismiss()
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(lang.flag, fontSize = 22.sp)
                        Text(
                            lang.displayName,
                            fontSize = 16.sp,
                            fontWeight = if (lang == current) FontWeight.Bold else FontWeight.Normal,
                            color = Brand.ink
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("cancel".tr(), color = Brand.purple) }
        }
    )
}

@Composable
private fun ProfileProgressRow(label: String, current: Int, total: Int, color: Color) {
    val fraction = if (total <= 0) 0f else (current.toFloat() / total).coerceIn(0f, 1f)
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 13.sp, color = Brand.ink)
            Text("$current / $total", fontSize = 12.sp, color = Brand.gray, fontWeight = FontWeight.Bold)
        }
        ChunkyProgress(fraction, color = color, height = 8.dp)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 15.sp, color = Brand.ink)
        Text(value, fontWeight = FontWeight.ExtraBold, color = Brand.purpleMid, fontSize = 15.sp)
    }
}

@Composable
private fun ActionRow(icon: ImageVector, text: String, tint: Color, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = if (tint == Brand.red) Brand.redDark else Brand.ink)
    }
}

@Composable
private fun EditNameDialog(current: String, auth: AuthViewModel, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(current) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("change_name".tr(), fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                YuhmuTextField(name, { name = it }, "Nombre", maxLength = 20, imeAction = androidx.compose.ui.text.input.ImeAction.Done)
                error?.let { Text(it, color = Brand.redDark, fontSize = 13.sp) }
            }
        },
        confirmButton = {
            TextButton(onClick = { auth.updateDisplayName(name) { err -> if (err == null) onDismiss() else error = err } }) {
                Text("save".tr(), color = Brand.purple, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("cancel".tr()) } }
    )
}

@Composable
private fun ChangePasswordDialog(auth: AuthViewModel, onDismiss: () -> Unit) {
    var old by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    SecureWindowEffect()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("change_password".tr(), fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                YuhmuTextField(old, { old = it }, "Contraseña actual", password = true)
                YuhmuTextField(new, { new = it }, "Nueva contraseña", password = true, imeAction = androidx.compose.ui.text.input.ImeAction.Done)
                error?.let { Text(it, color = Brand.redDark, fontSize = 13.sp) }
            }
        },
        confirmButton = {
            TextButton(onClick = { auth.changePassword(old, new) { err -> if (err == null) onDismiss() else error = err } }) {
                Text("save".tr(), color = Brand.purple, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("cancel".tr()) } }
    )
}

@Composable
private fun DeleteAccountDialog(auth: AuthViewModel, onDismiss: () -> Unit) {
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    SecureWindowEffect()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("delete_account".tr(), fontWeight = FontWeight.ExtraBold, color = Brand.redDark) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Se borrarán tu cuenta y todo tu progreso de este dispositivo. No se puede deshacer. Escribe tu contraseña para confirmar.", fontSize = 14.sp)
                YuhmuTextField(password, { password = it }, "Contraseña", password = true, imeAction = androidx.compose.ui.text.input.ImeAction.Done)
                error?.let { Text(it, color = Brand.redDark, fontSize = 13.sp) }
            }
        },
        confirmButton = {
            TextButton(onClick = { auth.deleteAccount(password) { err -> if (err != null) error = err } }) {
                Text("Eliminar", color = Brand.red, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("cancel".tr()) } }
    )
}
