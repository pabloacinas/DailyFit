@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.azotec.dailyfit

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.azotec.dailyfit.ui.theme.DailyFitTheme
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.input.KeyboardType // You likely have this already if you were trying to use KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import java.text.DateFormatSymbols
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.ScrollState
// Add this line:
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.Dp
// It seems you have an extra import for dp, you can remove one of them
// import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.List
import androidx.compose.ui.window.Dialog
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.LaunchedEffect
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.lerp
import kotlin.math.roundToInt
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.BorderStroke

// Modelo de datos actualizado: vuelve el campo fecha
data class Entrenamiento(
    val fecha: String, // formato dd/MM/yyyy
    val ejercicio: String,
    val peso: Int,
    val repeticiones: Int,
    val series: Int,
    val satisfaccion: Int // 1: Difícil, 2: Normal, 3: Fácil
)

data class Rutina(val nombre: String, val ejercicios: List<Entrenamiento>)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DailyFitTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    DailyFitApp()
                }
            }
        }
    }
}

// Utilidad para obtener el nombre del día y mes en español y formato largo
fun fechaLarga(fecha: String): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val cal = Calendar.getInstance()
    cal.time = sdf.parse(fecha) ?: Date()
    val dias = listOf("Domingo", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado")
    val meses = listOf(
        "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    )
    val diaSemana = dias[cal.get(Calendar.DAY_OF_WEEK) - 1]
    val dia = cal.get(Calendar.DAY_OF_MONTH)
    val mes = meses[cal.get(Calendar.MONTH)]
    val año = cal.get(Calendar.YEAR)
    return "$diaSemana $dia de $mes de $año"
}

@Composable
fun DailyFitTheme(content: @Composable () -> Unit) {
    val darkTheme = isSystemInDarkTheme()
    val colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()
    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}

@Composable
fun ColorPicker(
    color: Color,
    onColorChange: (Color) -> Unit
) {
    // Selector de tono (Hue)
    var hue by remember { mutableStateOf(0f) }
    var saturation by remember { mutableStateOf(1f) }
    var value by remember { mutableStateOf(1f) }
    // Convertir color actual a HSV
    LaunchedEffect(color) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color.toArgb(), hsv)
        hue = hsv[0]
        saturation = hsv[1]
        value = hsv[2]
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Elige el color principal:", fontWeight = FontWeight.SemiBold)
        // Hue
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Tono", modifier = Modifier.width(40.dp))
            Slider(
                value = hue,
                onValueChange = {
                    hue = it
                    val c = Color.hsv(hue, saturation, value)
                    onColorChange(c)
                },
                valueRange = 0f..360f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = Color.hsv(hue, 1f, 1f), activeTrackColor = Color.hsv(hue, 1f, 1f))
            )
        }
        // Saturation
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Sat", modifier = Modifier.width(40.dp))
            Slider(
                value = saturation,
                onValueChange = {
                    saturation = it
                    val c = Color.hsv(hue, saturation, value)
                    onColorChange(c)
                },
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = Color.hsv(hue, 1f, 1f), activeTrackColor = Color.hsv(hue, 1f, 1f))
            )
        }
        // Value
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Lum", modifier = Modifier.width(40.dp))
            Slider(
                value = value,
                onValueChange = {
                    value = it
                    val c = Color.hsv(hue, saturation, value)
                    onColorChange(c)
                },
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(thumbColor = Color.hsv(hue, 1f, 1f), activeTrackColor = Color.hsv(hue, 1f, 1f))
            )
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.hsv(hue, saturation, value))
                .border(2.dp, Color.Black, CircleShape)
                .align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
fun AjustesDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    currentColor: Color,
    onColorChange: (Color) -> Unit,
    onResetData: () -> Unit,
    onShowColorMenu: () -> Unit
) {
    if (!show) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajustes", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Button(
                    onClick = onShowColorMenu,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = currentColor)
                ) {
                    Text("Personalizar color", color = Color.White)
                }
                Button(
                    onClick = onResetData,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Restablecer datos", color = Color.White)
                }
                Button(
                    onClick = {}, // Estructura para idioma
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = currentColor)
                ) {
                    Text("Idioma (próximamente)", color = Color.White)
                }
                Button(
                    onClick = {}, // Acerca de
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = currentColor)
                ) {
                    Text("Acerca de", color = Color.White)
                }
            }
        },
        confirmButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = currentColor)
            ) {
                Text("Cerrar")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun ColorMenuDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    currentColor: Color,
    onColorChange: (Color) -> Unit
) {
    if (!show) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Color de la app", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                ColorPicker(color = currentColor, onColorChange = onColorChange)
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = currentColor)) {
                Text("Cerrar", color = Color.White)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

fun guardarColorPrincipal(context: Context, color: Color) {
    val prefs = context.getSharedPreferences("ajustes_prefs", Context.MODE_PRIVATE)
    prefs.edit().putInt("color_principal", color.toArgb()).apply()
}

fun cargarColorPrincipal(context: Context, default: Color): Color {
    val prefs = context.getSharedPreferences("ajustes_prefs", Context.MODE_PRIVATE)
    val colorInt = prefs.getInt("color_principal", default.toArgb())
    return Color(colorInt)
}

@Composable
fun DailyFitApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("rutinas_prefs", Context.MODE_PRIVATE) }
    val gson = remember { Gson() }
    var entrenamientos by remember { mutableStateOf(listOf<Entrenamiento>()) }
    var showDialog by remember { mutableStateOf(false) }
    var showRutinasDialog by remember { mutableStateOf(false) }
    var showGuardarRutinaDialog by remember { mutableStateOf(false) }
    var showCargarRutinaDialog by remember { mutableStateOf(false) }
    var rutinas by remember { mutableStateOf(listOf<Rutina>()) }
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val today = Calendar.getInstance()
    val daysRange = (-30..30).toList()
    val daysList = daysRange.map { offset ->
        val cal = Calendar.getInstance()
        cal.time = today.time
        cal.add(Calendar.DAY_OF_YEAR, offset)
        sdf.format(cal.time)
    }
    var selectedDate by remember { mutableStateOf(sdf.format(today.time)) }
    val todayIndex = daysList.indexOf(sdf.format(today.time))
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = todayIndex)
    val coroutineScope = rememberCoroutineScope()
    var editDialogData by remember { mutableStateOf<Entrenamiento?>(null) }
    var editDialogIndex by remember { mutableStateOf(-1) }

    // Color principal
    var colorPrincipal by remember { mutableStateOf(cargarColorPrincipal(context, Color(0xFF1976D2))) }
    var showAjustes by remember { mutableStateOf(false) }
    var showColorMenu by remember { mutableStateOf(false) }
    val systemDark = isSystemInDarkTheme()
    var isDarkTheme by remember { mutableStateOf(systemDark) }

    var showConfirmResetDialog by remember { mutableStateOf(false) }

    // Cargar rutinas al iniciar
    LaunchedEffect(Unit) {
        val jsonRutinas = prefs.getString("rutinas", null)
        if (jsonRutinas != null) {
            val type = object : TypeToken<List<Rutina>>() {}.type
            rutinas = gson.fromJson(jsonRutinas, type) ?: emptyList()
        }
        val jsonEntrenamientos = prefs.getString("entrenamientos", null)
        if (jsonEntrenamientos != null) {
            val type = object : TypeToken<List<Entrenamiento>>() {}.type
            entrenamientos = gson.fromJson(jsonEntrenamientos, type) ?: emptyList()
        }
    }
    // Guardar rutinas cada vez que cambian
    LaunchedEffect(rutinas) {
        val json = gson.toJson(rutinas)
        prefs.edit().putString("rutinas", json).apply()
    }
    // Guardar entrenamientos cada vez que cambian
    LaunchedEffect(entrenamientos) {
        val json = gson.toJson(entrenamientos)
        prefs.edit().putString("entrenamientos", json).apply()
    }
    // Guardar color principal cuando cambie
    LaunchedEffect(colorPrincipal) {
        guardarColorPrincipal(context, colorPrincipal)
    }

    // Restablecer datos
    fun resetData() {
        entrenamientos = emptyList()
        rutinas = emptyList()
        prefs.edit().remove("rutinas").remove("entrenamientos").apply()
    }

    // 1. Botones principales con color más claro
    val colorBotonClaro = lerp(colorPrincipal, Color.White, 0.2f)

    var tipoRutina by remember { mutableStateOf("") }
    var showTipoRutinaDialog by remember { mutableStateOf(false) }
    var tipoRutinaInput by remember { mutableStateOf("") }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                FloatingActionButton(
                    onClick = { showAjustes = true },
                    containerColor = colorPrincipal,
                    contentColor = Color.White,
                    modifier = Modifier.padding(start = 24.dp) // Más margen a la derecha
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Ajustes")
                }
                Row {
                    FloatingActionButton(
                        onClick = { showRutinasDialog = true },
                        containerColor = colorPrincipal,
                        contentColor = Color.White,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Icon(Icons.Default.List, contentDescription = "Rutinas")
                    }
                    FloatingActionButton(
                        onClick = { showDialog = true },
                        containerColor = colorPrincipal,
                        contentColor = Color.White
                    ) {
                        Text("+")
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            DiasSelectorAnimado(
                daysList = daysList,
                selectedDate = selectedDate,
                onDateSelected = { date, index ->
                    selectedDate = date
                    coroutineScope.launch {
                        listState.animateScrollToItem(index)
                    }
                },
                listState = listState,
                colorPrincipal = colorPrincipal,
                entrenamientos = entrenamientos
            )
            Text(
                text = fechaLarga(selectedDate),
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = colorPrincipal,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                ),
                modifier = Modifier.padding(16.dp)
            )
            if (tipoRutina.isNotBlank()) {
                Text(
                    text = tipoRutina,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            tipoRutinaInput = tipoRutina
                            showTipoRutinaDialog = true
                        }
                        .padding(8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
            }
            val entrenamientosDelDia = entrenamientos.filter { it.fecha == selectedDate }
            TablaEntrenamientosEditable(
                entrenamientosDelDia,
                onLongClick = { ejercicio, index ->
                    editDialogData = ejercicio
                    editDialogIndex = index
                },
                colorPrincipal = colorPrincipal
            )
        }
        if (showDialog) {
            FormularioEntrenamientoModern(
                onDismiss = { showDialog = false },
                onSave = { nuevo ->
                    entrenamientos = entrenamientos + nuevo
                    showDialog = false
                },
                defaultDate = selectedDate,
                colorPrincipal = colorPrincipal
            )
        }
        if (showRutinasDialog) {
            Dialog(onDismissRequest = { showRutinasDialog = false }) {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("Rutinas", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(bottom = 16.dp))
                        Button(
                            onClick = {
                                showRutinasDialog = false
                                showGuardarRutinaDialog = true
                            },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = lerp(colorPrincipal, Color.White, 0.2f), contentColor = Color.White)
                        ) {
                            Text("Guardar rutina del día actual")
                        }
                        Button(
                            onClick = {
                                showRutinasDialog = false
                                showCargarRutinaDialog = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = lerp(colorPrincipal, Color.White, 0.2f), contentColor = Color.White)
                        ) {
                            Text("Cargar rutina en este día")
                        }
                        OutlinedButton(
                            onClick = { showRutinasDialog = false },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colorPrincipal)
                        ) {
                            Text("Cerrar")
                        }
                    }
                }
            }
        }
        if (showGuardarRutinaDialog) {
            GuardarRutinaDialog(
                onDismiss = { showGuardarRutinaDialog = false },
                onSave = { nombre ->
                    val ejerciciosDia = entrenamientos.filter { it.fecha == selectedDate }
                    if (ejerciciosDia.isNotEmpty()) {
                        rutinas = rutinas + Rutina(nombre, ejerciciosDia.map { it.copy() })
                    }
                    showGuardarRutinaDialog = false
                },
                colorPrincipal = colorPrincipal
            )
        }
        if (showCargarRutinaDialog) {
            CargarRutinaDialog(
                rutinas = rutinas,
                onDismiss = { showCargarRutinaDialog = false },
                onLoad = { rutina ->
                    entrenamientos = entrenamientos.filter { it.fecha != selectedDate } + rutina.ejercicios.map { it.copy(fecha = selectedDate) }
                    showCargarRutinaDialog = false
                },
                colorPrincipal = colorPrincipal
            )
        }
        if (editDialogData != null && editDialogIndex >= 0) {
            EditarEliminarDialog(
                ejercicio = editDialogData!!,
                onEdit = { nuevo ->
                    val lista = entrenamientos.toMutableList()
                    val idxGlobal = entrenamientos.indexOfFirst { it.fecha == selectedDate } + editDialogIndex
                    lista[idxGlobal] = nuevo
                    entrenamientos = lista
                    editDialogData = null
                    editDialogIndex = -1
                },
                onDelete = {
                    val lista = entrenamientos.toMutableList()
                    val idxGlobal = entrenamientos.indexOfFirst { it.fecha == selectedDate } + editDialogIndex
                    lista.removeAt(idxGlobal)
                    entrenamientos = lista
                    editDialogData = null
                    editDialogIndex = -1
                },
                onMoveUp = {
                    val lista = entrenamientos.toMutableList()
                    val idxGlobal = entrenamientos.indexOfFirst { it.fecha == selectedDate } + editDialogIndex
                    if (idxGlobal > 0 && lista[idxGlobal].fecha == lista[idxGlobal - 1].fecha) {
                        lista[idxGlobal] = lista[idxGlobal - 1].also { lista[idxGlobal - 1] = lista[idxGlobal] }
                        entrenamientos = lista
                    }
                    editDialogData = null
                    editDialogIndex = -1
                },
                onMoveDown = {
                    val lista = entrenamientos.toMutableList()
                    val idxGlobal = entrenamientos.indexOfFirst { it.fecha == selectedDate } + editDialogIndex
                    if (idxGlobal < lista.size - 1 && lista[idxGlobal].fecha == lista[idxGlobal + 1].fecha) {
                        lista[idxGlobal] = lista[idxGlobal + 1].also { lista[idxGlobal + 1] = lista[idxGlobal] }
                        entrenamientos = lista
                    }
                    editDialogData = null
                    editDialogIndex = -1
                },
                onDismiss = {
                    editDialogData = null
                    editDialogIndex = -1
                },
                colorPrincipal = colorPrincipal
            )
        }
        // Diálogo de ajustes
        AjustesDialog(
            show = showAjustes,
            onDismiss = { showAjustes = false },
            currentColor = colorPrincipal,
            onColorChange = { colorPrincipal = it },
            onResetData = {
                showConfirmResetDialog = true
            },
            onShowColorMenu = { showColorMenu = true }
        )
        // Diálogo de color
        ColorMenuDialog(
            show = showColorMenu,
            onDismiss = { showColorMenu = false },
            currentColor = colorPrincipal,
            onColorChange = { colorPrincipal = it }
        )
        // Diálogo de confirmación para restablecer datos
        ConfirmarResetDialog(
            show = showConfirmResetDialog,
            onConfirm = {
                resetData()
                showConfirmResetDialog = false
            },
            onDismiss = { showConfirmResetDialog = false },
            colorPrincipal = colorPrincipal
        )
        if (showTipoRutinaDialog) {
            AlertDialog(
                onDismissRequest = { showTipoRutinaDialog = false },
                title = { Text("¿Qué vas a entrenar hoy?") },
                text = {
                    OutlinedTextField(
                        value = tipoRutinaInput,
                        onValueChange = { tipoRutinaInput = it },
                        label = { Text("Tipo de rutina") }
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        tipoRutina = tipoRutinaInput
                        showTipoRutinaDialog = false
                        showDialog = true
                    }) { Text("Guardar") }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showTipoRutinaDialog = false }) { Text("Cerrar") }
                }
            )
        }
    }
}

@Composable
fun DiasSelectorAnimado(
    daysList: List<String>,
    selectedDate: String,
    onDateSelected: (String, Int) -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState,
    colorPrincipal: Color,
    entrenamientos: List<Entrenamiento>
) {
    val sdfShort = SimpleDateFormat("dd/MM", Locale.getDefault())
    val visibleItems = 5
    LaunchedEffect(selectedDate) {
        val selectedIndex = daysList.indexOf(selectedDate)
        val centerIndex = visibleItems / 2
        val scrollTo = (selectedIndex - centerIndex).coerceIn(0, daysList.size - visibleItems)
        listState.animateScrollToItem(scrollTo)
    }
    LazyRow(
        state = listState,
        modifier = Modifier
            .background(colorPrincipal)
            .padding(vertical = 12.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        itemsIndexed(daysList) { index, fullDate ->
            val cal = Calendar.getInstance()
            cal.time = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(fullDate) ?: Date()
            val dayStr = sdfShort.format(cal.time)
            val isToday = index == daysList.indexOf(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()))
            val isSelected = fullDate == selectedDate
            val colorHoy = lerp(colorPrincipal, Color.Black, 0.2f)
            val colorClaro = lerp(colorPrincipal, Color.White, 0.7f)
            val colorSeleccionado = lerp(colorPrincipal, Color.White, 0.4f)
            val hayEjercicios = entrenamientos.any { it.fecha == fullDate }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(horizontal = 6.dp)
            ) {
                Button(
                    onClick = { onDateSelected(fullDate, index) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when {
                            isToday -> colorHoy
                            isSelected -> colorSeleccionado
                            else -> colorClaro
                        }
                    ),
                    contentPadding = PaddingValues(8.dp),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = dayStr,
                        color = when {
                            isToday || isSelected -> Color.White
                            else -> colorPrincipal
                        },
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = FontFamily.SansSerif
                    )
                }
                if (hayEjercicios) {
                    Box(
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(8.dp)
                            .background(Color(0xFF43A047), shape = CircleShape)
                    )
                } else {
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
fun TablaEntrenamientosEditable(
    entrenamientos: List<Entrenamiento>,
    onLongClick: (Entrenamiento, Int) -> Unit,
    colorPrincipal: Color
) {
    if (entrenamientos.isEmpty()) {
        Text("No hay registros para este día", modifier = Modifier.padding(16.dp), fontFamily = FontFamily.SansSerif)
        return
    }
    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth().background(colorPrincipal.copy(alpha = 0.2f))) {
            Text("Ejercicio", modifier = Modifier.weight(1.5f).padding(4.dp), fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif, fontSize = 14.sp, color = colorPrincipal)
            Text("Peso", modifier = Modifier.weight(1.2f).padding(4.dp), fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif, fontSize = 14.sp, textAlign = TextAlign.Center, color = colorPrincipal)
            Text("Reps", modifier = Modifier.weight(1f).padding(4.dp), fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif, fontSize = 14.sp, textAlign = TextAlign.Center, color = colorPrincipal)
            Text("Series", modifier = Modifier.weight(1f).padding(4.dp), fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif, fontSize = 14.sp, textAlign = TextAlign.Center, color = colorPrincipal)
            Text("Satisf.", modifier = Modifier.weight(1f).padding(4.dp), fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif, fontSize = 14.sp, textAlign = TextAlign.Center, color = colorPrincipal)
        }
        entrenamientos.forEachIndexed { idx, ent ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .clickable { onLongClick(ent, idx) }, // abrir menú con click
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(ent.ejercicio, modifier = Modifier.weight(1.5f).padding(4.dp), fontFamily = FontFamily.SansSerif, fontSize = 14.sp)
                Text("${ent.peso} kg", modifier = Modifier.weight(1.2f).padding(4.dp), fontFamily = FontFamily.SansSerif, fontSize = 14.sp, textAlign = TextAlign.Center)
                Text("${ent.repeticiones}", modifier = Modifier.weight(1f).padding(4.dp), fontFamily = FontFamily.SansSerif, fontSize = 14.sp, textAlign = TextAlign.Center)
                Text("${ent.series}", modifier = Modifier.weight(1f).padding(4.dp), fontFamily = FontFamily.SansSerif, fontSize = 14.sp, textAlign = TextAlign.Center)
                Box(modifier = Modifier.weight(1f).padding(4.dp), contentAlignment = Alignment.Center) {
                    IconoSatisfaccion(ent.satisfaccion, size = 20.dp)
                }
            }
        }
    }
}

@Composable
fun EditarEliminarDialog(
    ejercicio: Entrenamiento,
    onEdit: (Entrenamiento) -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDismiss: () -> Unit,
    colorPrincipal: Color
) {
    var showEditForm by remember { mutableStateOf(false) }
    if (showEditForm) {
        FormularioEntrenamientoModern(
            onDismiss = { showEditForm = false },
            onSave = {
                onEdit(it)
                showEditForm = false
            },
            defaultDate = ejercicio.fecha,
            colorPrincipal = colorPrincipal,
            ejercicioExistente = ejercicio
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Opciones del ejercicio", color = colorPrincipal) },
            text = {
                Column {
                    Text("¿Qué quieres hacer con este ejercicio?")
                }
            },
            confirmButton = {
                Column {
                    Button(
                        onClick = { showEditForm = true },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = lerp(colorPrincipal, Color.White, 0.2f), contentColor = Color.White)
                    ) {
                        Text("Editar")
                    }
                    Button(
                        onClick = onDelete,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = lerp(colorPrincipal, Color.White, 0.2f), contentColor = Color.White)
                    ) {
                        Text("Eliminar")
                    }
                    Button(
                        onClick = onMoveUp,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = lerp(colorPrincipal, Color.White, 0.2f), contentColor = Color.White)
                    ) {
                        Text("Mover arriba")
                    }
                    Button(
                        onClick = onMoveDown,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = lerp(colorPrincipal, Color.White, 0.2f), contentColor = Color.White)
                    ) {
                        Text("Mover abajo")
                    }
                    // Botón Cerrar destacado
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent, contentColor = colorPrincipal)
                    ) {
                        Text("Cerrar")
                    }
                }
            },
            containerColor = if (isSystemInDarkTheme()) Color(0xFF222222) else MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
fun FormularioEntrenamientoModern(
    onDismiss: () -> Unit,
    onSave: (Entrenamiento) -> Unit,
    defaultDate: String? = null,
    colorPrincipal: Color,
    ejercicioExistente: Entrenamiento? = null
) {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val context = LocalContext.current
    var fecha by rememberSaveable { mutableStateOf(ejercicioExistente?.fecha ?: defaultDate ?: sdf.format(Date())) }
    var ejercicio by rememberSaveable { mutableStateOf(ejercicioExistente?.ejercicio ?: "") }
    var peso by rememberSaveable { mutableStateOf(ejercicioExistente?.peso?.toString() ?: "") }
    var repeticiones by rememberSaveable { mutableStateOf(ejercicioExistente?.repeticiones?.toString() ?: "") }
    var series by rememberSaveable { mutableStateOf(ejercicioExistente?.series?.toString() ?: "") }
    var satisfaccion by rememberSaveable { mutableStateOf(ejercicioExistente?.satisfaccion?.toFloat() ?: 2f) } // Slider de 1 a 3

    val calendar = Calendar.getInstance()
    LaunchedEffect(defaultDate) {
        defaultDate?.let {
            try {
                calendar.time = sdf.parse(it) ?: Date()
            } catch (_: Exception) {}
        }
    }
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val selected = Calendar.getInstance()
            selected.set(year, month, dayOfMonth)
            fecha = sdf.format(selected.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    // Menú de añadir ejercicio
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (ejercicioExistente != null) "Editar Entrenamiento" else "Nuevo Entrenamiento", color = colorPrincipal, fontFamily = FontFamily.SansSerif) },
        text = {
            Column(modifier = Modifier.defaultMinSize(minWidth = 340.dp)) {
                OutlinedTextField(
                    value = fecha,
                    onValueChange = {},
                    label = { Text("Fecha") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp), // quitar .height(44.dp)
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { datePickerDialog.show() }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Seleccionar fecha")
                        }
                    },
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.SansSerif)
                )
                OutlinedTextField(
                    value = ejercicio,
                    onValueChange = { ejercicio = it },
                    label = { Text("Nombre del Ejercicio") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), // quitar height extra
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.SansSerif)
                )
                // Peso:
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Peso:", modifier = Modifier.width(80.dp), fontFamily = FontFamily.SansSerif)
                    IconButton(onClick = {
                        val v = peso.toIntOrNull() ?: 0
                        if (v > 0) peso = (v - 1).toString()
                    }) { Text("-") }
                    OutlinedTextField(
                        value = peso,
                        onValueChange = { nuevo ->
                            val soloNumeros = nuevo.filter { it.isDigit() }
                            val valor = soloNumeros.toIntOrNull() ?: 0
                            peso = if (valor > 500) "500" else soloNumeros
                        },
                        modifier = Modifier.width(60.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.SansSerif, textAlign = TextAlign.Center, fontSize = 13.sp)
                    )
                    IconButton(onClick = {
                        val v = peso.toIntOrNull() ?: 0
                        if (v < 500) peso = (v + 1).toString()
                    }) { Text("+") }
                }
                Spacer(modifier = Modifier.height(8.dp))
                // Reps:
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Reps:", modifier = Modifier.width(80.dp), fontFamily = FontFamily.SansSerif)
                    IconButton(onClick = {
                        val v = repeticiones.toIntOrNull() ?: 0
                        if (v > 0) repeticiones = (v - 1).toString()
                    }) { Text("-") }
                    OutlinedTextField(
                        value = repeticiones,
                        onValueChange = { nuevo ->
                            val soloNumeros = nuevo.filter { it.isDigit() }
                            val valor = soloNumeros.toIntOrNull() ?: 0
                            repeticiones = if (valor > 50) "50" else soloNumeros
                        },
                        modifier = Modifier.width(60.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.SansSerif, textAlign = TextAlign.Center, fontSize = 13.sp)
                    )
                    IconButton(onClick = {
                        val v = repeticiones.toIntOrNull() ?: 0
                        if (v < 50) repeticiones = (v + 1).toString()
                    }) { Text("+") }
                }
                Spacer(modifier = Modifier.height(8.dp))
                // Series:
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Series:", modifier = Modifier.width(80.dp), fontFamily = FontFamily.SansSerif)
                    IconButton(onClick = {
                        val v = series.toIntOrNull() ?: 0
                        if (v > 0) series = (v - 1).toString()
                    }) { Text("-") }
                    OutlinedTextField(
                        value = series,
                        onValueChange = { nuevo ->
                            val soloNumeros = nuevo.filter { it.isDigit() }
                            val valor = soloNumeros.toIntOrNull() ?: 0
                            series = if (valor > 10) "10" else soloNumeros
                        },
                        modifier = Modifier.width(60.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.SansSerif, textAlign = TextAlign.Center, fontSize = 13.sp)
                    )
                    IconButton(onClick = {
                        val v = series.toIntOrNull() ?: 0
                        if (v < 10) series = (v + 1).toString()
                    }) { Text("+") }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Satisfacción:", color = colorPrincipal, fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Slider(
                        value = satisfaccion,
                        onValueChange = { satisfaccion = it },
                        valueRange = 1f..3f,
                        steps = 1,
                        colors = SliderDefaults.colors(
                            thumbColor = colorPrincipal,
                            activeTrackColor = colorPrincipal
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    IconoSatisfaccion(satisfaccion.toInt(), size = 24.dp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (ejercicio.isNotBlank() && peso.toIntOrNull() != null && repeticiones.toIntOrNull() != null && series.toIntOrNull() != null) {
                        onSave(
                            Entrenamiento(
                                fecha = fecha,
                                ejercicio = ejercicio,
                                peso = peso.toIntOrNull() ?: 0,
                                repeticiones = repeticiones.toIntOrNull() ?: 0,
                                series = series.toIntOrNull() ?: 0,
                                satisfaccion = satisfaccion.toInt()
                            )
                        )
                    } else {
                        // Manejar el caso donde hay campos vacíos o inválidos
                        // Por ejemplo, mostrar un mensaje de error o un estado de error
                        // Aquí, simplemente no guardamos si hay campos inválidos
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = lerp(colorPrincipal, Color.White, 0.2f), contentColor = Color.White)
            ) {
                Text("Guardar", fontFamily = FontFamily.SansSerif)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, colors = ButtonDefaults.outlinedButtonColors(contentColor = colorPrincipal)) {
                Text("Cerrar", fontFamily = FontFamily.SansSerif)
            }
        },
        containerColor = if (isSystemInDarkTheme()) Color(0xFF222222) else MaterialTheme.colorScheme.surface
    )
}

@Composable
fun NumberSelectorEditableV2(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    range: IntRange,
    step: Int = 1,
    modifier: Modifier = Modifier
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = {
            val v = value.text.toIntOrNull() ?: if (range.first > 0) range.first else 0
            if (v > range.first) onValueChange(TextFieldValue((v - step).toString()))
        }) {
            Text("-")
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp)) {
            OutlinedTextField(
                value = value,
                onValueChange = {
                    val filtered = it.text.filter { c -> c.isDigit() }
                    val num = filtered.toIntOrNull() ?: 0
                    if (filtered.isEmpty() || num in range) onValueChange(TextFieldValue(filtered))
                },
                modifier = Modifier.width(60.dp), // más alto
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.SansSerif, textAlign = TextAlign.Center, fontSize = 13.sp), // letra más pequeña
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            IconButton(onClick = {
                val v = value.text.toIntOrNull() ?: if (range.first > 0) range.first else 0
                if (v < range.last) onValueChange(TextFieldValue((v + step).toString()))
            }) {
                Text("+")
            }
        }
    }
}

@Composable
fun IconoSatisfaccion(satisfaccion: Int, size: Dp = 20.dp) {
    when (satisfaccion) {
        1 -> Icon(
            painter = painterResource(android.R.drawable.ic_delete),
            contentDescription = "Difícil",
            tint = Color.Red,
            modifier = Modifier.size(size)
        )
        2 -> Icon(
            painter = painterResource(android.R.drawable.ic_media_pause),
            contentDescription = "Normal",
            tint = Color.Gray,
            modifier = Modifier.size(size)
        )
        3 -> Icon(
            painter = painterResource(android.R.drawable.star_on), // Icono temporal de tic verde
            contentDescription = "Fácil",
            tint = Color.Green,
            modifier = Modifier.size(size)
        )
    }
}

fun textoSatisfaccion(satisfaccion: Int): String = when (satisfaccion) {
    1 -> "Difícil"
    2 -> "Normal"
    3 -> "Fácil"
    else -> ""
}

@Composable
fun GuardarRutinaDialog(onDismiss: () -> Unit, onSave: (String) -> Unit, colorPrincipal: Color) {
    var nombre by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Guardar rutina") },
        text = {
            Column {
                Text("Introduce un nombre para la rutina:")
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre de la rutina") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = { if (nombre.isNotBlank()) onSave(nombre) }, colors = ButtonDefaults.buttonColors(containerColor = lerp(colorPrincipal, Color.White, 0.2f), contentColor = Color.White)) {
                Text("Guardar")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent, contentColor = colorPrincipal)) {
                Text("Cerrar")
            }
        }
    )
}

@Composable
fun CargarRutinaDialog(rutinas: List<Rutina>, onDismiss: () -> Unit, onLoad: (Rutina) -> Unit, colorPrincipal: Color) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cargar rutina") },
        text = {
            if (rutinas.isEmpty()) {
                Text("No hay rutinas guardadas.")
            } else {
                Column {
                    rutinas.forEach { rutina ->
                        Button(onClick = { onLoad(rutina) }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = ButtonDefaults.buttonColors(containerColor = lerp(colorPrincipal, Color.White, 0.2f), contentColor = Color.White)) {
                            Text(rutina.nombre)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            OutlinedButton(onClick = onDismiss, colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent, contentColor = colorPrincipal)) {
                Text("Cerrar", color = colorPrincipal)
            }
        }
    )
}

@Composable
fun ConfirmarResetDialog(show: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit, colorPrincipal: Color) {
    if (!show) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirmar borrado") },
        text = { Text("¿Estás seguro de que quieres borrar todos los entrenamientos y rutinas? Esta acción no se puede deshacer.") },
        confirmButton = {
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                Text("Sí, borrar todo", color = Color.White)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent, contentColor = colorPrincipal)) {
                Text("Cerrar", color = colorPrincipal)
            }
        }
    )
}