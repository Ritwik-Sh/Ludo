package com.example.ludo

import android.app.Activity
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.*
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlin.random.Random
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import kotlinx.coroutines.delay
import kotlin.math.*

class Pal(val bg: Color, val surf: Color, val text: Color, val sub: Color, val acc: Color, val p: List<Color>,
          val acc2: Color = acc, val acc3: Color = sub, val hi: Color = acc)

val Light = Pal(Color(0xFFF2F2F7), Color.White, Color(0xFF111111), Color(0xFF6B6B70), Color(0xFF3E91FF),
    listOf(0xFFE5484D, 0xFF30A46C, 0xFFF5B50A, 0xFF3E91FF, 0xFF8E4EC6, 0xFFF76B15).map { Color(it) })
val Dark = Pal(Color.Black, Color(0xFF1C1C1E), Color(0xFFF2F2F7), Color(0xFF9A9AA0), Color(0xFF5AA6FF),
    listOf(0xFFFF6369, 0xFF3DD68C, 0xFFFFD60A, 0xFF5AA6FF, 0xFFB07CE8, 0xFFFF9052).map { Color(it) })
val ACCENTS = listOf("Blue" to 0xFF3E91FF, "Green" to 0xFF30A46C, "Purple" to 0xFF8E4EC6,
    "Pink" to 0xFFE5589F, "Orange" to 0xFFF76B15, "Red" to 0xFFE5484D)
fun onAcc(c: Color) = if (c.luminance() > 0.5f) Color.Black else Color.White

val ICONS = listOf("None", "Star", "Heart", "Diamond", "Pawn")

class IntP(private val sp: SharedPreferences, private val k: String, d: Int) {
    private val s = mutableIntStateOf(sp.getInt(k, d))
    var v: Int
        get() = s.intValue
        set(x) { s.intValue = x; sp.edit().putInt(k, x).apply() }
}
class BoolP(private val sp: SharedPreferences, private val k: String, d: Boolean) {
    private val s = mutableStateOf(sp.getBoolean(k, d))
    var v: Boolean
        get() = s.value
        set(x) { s.value = x; sp.edit().putBoolean(k, x).apply() }
}
class Prefs(sp: SharedPreferences) {
    val theme = IntP(sp, "theme", 0); val icon = IntP(sp, "icon", 1); val accent = IntP(sp, "accent", 0)
    val speedPct = IntP(sp, "speedPct", 55); val first = IntP(sp, "first", -1)
    val stack = BoolP(sp, "stack", true); val unlock1 = BoolP(sp, "unlock1", false)
    val autoRoll = BoolP(sp, "autoRoll", false); val autoOut = BoolP(sp, "autoOut", false)
    val autoSingle = BoolP(sp, "autoSingle", true); val sound = BoolP(sp, "sound", true); val vib = BoolP(sp, "vib", true)
    val cancel6 = BoolP(sp, "cancel6", true)
    val indicator = IntP(sp, "indicator", 0)
    val pseudo = BoolP(sp, "pseudo", false)
    val startOne = BoolP(sp, "startOne", false)
    val indLevel = IntP(sp, "indLevel", 0)
    val arrowBonus = BoolP(sp, "arrowBonus", true); val killLock = BoolP(sp, "killLock", true)
    val removeWith1 = BoolP(sp, "removeWith1", false); val pieces = IntP(sp, "pieces", 4)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App() }
    }
}

@Composable
fun App() {
    val ctx = LocalContext.current
    val pf = remember { Prefs(ctx.getSharedPreferences("ludo", 0)) }
    val dark = if (pf.theme.v == 0) isSystemInDarkTheme() else pf.theme.v == 2
    val base = if (dark) Dark else Light
    val sysOk = Build.VERSION.SDK_INT >= 31
    fun sc(id: Int) = Color(ctx.getColor(id))
    val a1 = if (sysOk) sc(android.R.color.system_accent1_500) else base.acc
    val a2 = if (sysOk) sc(android.R.color.system_accent2_300) else base.acc
    val a3 = if (sysOk) sc(android.R.color.system_accent3_300) else base.acc
    val sysPal = listOf(a1, a2, a3)
    val pal = if (pf.accent.v == 0 && sysOk) {
        if (dark) Pal(sc(android.R.color.system_neutral1_1000), base.surf,
            sc(android.R.color.system_neutral1_100), sc(android.R.color.system_neutral2_300), a1, base.p, a2, a3,
            sc(android.R.color.system_accent1_200))
        else Pal(sc(android.R.color.system_neutral2_50), base.surf,
            sc(android.R.color.system_neutral1_900), sc(android.R.color.system_neutral2_600), a1, base.p, a2, a3,
            sc(android.R.color.system_accent1_700))
    } else {
        val c = if (pf.accent.v == 0) base.acc else Color(ACCENTS[pf.accent.v - 1].second)
        Pal(base.bg, base.surf, base.text, base.sub, c, base.p, c, base.sub, if (dark) lerp(c, Color.White, 0.35f) else c)
    }
    val view = LocalView.current
    SideEffect {
        val w = (view.context as Activity).window
        WindowCompat.setDecorFitsSystemWindows(w, false)
        w.statusBarColor = android.graphics.Color.TRANSPARENT
        w.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= 29) w.isNavigationBarContrastEnforced = false
        WindowCompat.getInsetsController(w, view).apply {
            isAppearanceLightStatusBars = !dark; isAppearanceLightNavigationBars = !dark
        }
    }
    var game by remember { mutableStateOf<Game?>(null) }
    var tab by remember { mutableStateOf(0) }
    var mode by remember { mutableStateOf(0) }
    val names = remember { mutableStateListOf("", "", "", "", "", "") }
    val cpu = remember { mutableStateListOf(false, true, true, true, true, true) }
    val scheme = if (dark) darkColorScheme(primary = pal.acc, onPrimary = onAcc(pal.acc), background = pal.bg, surface = pal.surf)
    else lightColorScheme(primary = pal.acc, onPrimary = onAcc(pal.acc), background = pal.bg, surface = pal.surf)
    MaterialTheme(colorScheme = scheme) {
        Surface(Modifier.fillMaxSize(), color = pal.bg, contentColor = pal.text) {
            Box(Modifier.fillMaxSize().statusBarsPadding()) {
                val g = game
                if (g != null) GameScreen(g, pal, pf, sysPal, { game = g.fresh() }) { game = null }
                else Home(pal, pf, tab, mode, names, cpu, sysPal, { tab = it }, { mode = it }) { game = it }
            }
        }
    }
}

@Composable
fun NavIcon(kind: Int) {
    val col = LocalContentColor.current
    if (kind == 3) { Icon(Icons.Default.Settings, null, modifier = Modifier.size(22.dp)); return }
    Canvas(Modifier.size(22.dp)) {
        val s = size.width
        when (kind) {
            0 -> for (i in 0..3) drawCircle(col, s * .17f, Offset(s * (.3f + .4f * (i % 2)), s * (.3f + .4f * (i / 2))))
            1 -> {
                drawCircle(col, s * .28f, Offset(s * .35f, s * .5f), style = Stroke(s * .1f))
                drawCircle(col, s * .28f, Offset(s * .65f, s * .5f), style = Stroke(s * .1f))
            }
            else -> for (i in 0..4) {
                val a = -PI / 2 + i * 2 * PI / 5
                drawCircle(col, s * .11f, Offset(s * .5f + (s * .36f * cos(a)).toFloat(), s * .5f + (s * .36f * sin(a)).toFloat()))
            }
        }
    }
}

/** Floating, translucent pill navigation (56dp class height). The 5 / 6 tab opens the pill upward into a menu. */
@Composable
fun NavBar(
    pal: Pal, tab: Int, mode: Int, menu: Int, onTab: (Int) -> Unit, onPick: (Int) -> Unit,
    layer: GraphicsLayer, offset: () -> Offset, onPill: (Offset) -> Unit
) {
    val blurLayer = rememberGraphicsLayer()
    if (Build.VERSION.SDK_INT >= 31) blurLayer.renderEffect = BlurEffect(48f, 48f, TileMode.Clamp)
    val shape = RoundedCornerShape(32.dp)
    Surface(
        shape = shape, color = pal.surf.copy(alpha = if (Build.VERSION.SDK_INT >= 31) 0.6f else 0.9f),
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp).fillMaxWidth().clip(shape)
            .onGloballyPositioned { onPill(it.positionInRoot()) }
            .drawWithContent {
                val o = offset()
                blurLayer.record { translate(-o.x, -o.y) { drawLayer(layer) } }
                drawLayer(blurLayer)
                drawContent()
            }
    ) {
        Column(Modifier.animateContentSize(spring(dampingRatio = 0.8f, stiffness = 350f))) {
            if (menu != 0) Column(Modifier.padding(start = 6.dp, end = 6.dp, top = 6.dp)) {
                val items = if (menu == 1) listOf(4 to "2 Players", 5 to "3 Players", 0 to "4 Players") else listOf(2 to "5 Players", 3 to "6 Players")
                items.forEach { (md, l) ->
                    Text(
                        l, fontWeight = FontWeight.Medium,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
                            .background(if (mode == md && tab == (if (menu == 1) 0 else 2)) pal.text.copy(alpha = .12f) else Color.Transparent)
                            .clickable { onPick(md) }.padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                }
            }
            Row(Modifier.padding(4.dp)) {
                listOf(when (mode) { 4 -> "2 Players"; 5 -> "3 Players"; else -> "Classic" }, "Team Up", if (mode == 2 || mode == 3) "${mode + 3} Players" else "5 / 6", "Settings").forEachIndexed { i, label ->
                    val sel = tab == i
                    val col = if (sel) pal.text else pal.sub
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(28.dp))
                            .background(if (sel) pal.acc2.copy(alpha = .28f) else Color.Transparent)
                            .clickable { onTab(i) }.padding(top = 10.dp, bottom = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CompositionLocalProvider(LocalContentColor provides col) { NavIcon(i) }
                        Text(label, color = col, fontSize = 11.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal, maxLines = 1)
                    }
                }
            }
        }
    }
}

/** One UI expandable app bar: 150dp by default, pulling down expands it to 39.67% of the screen height and centres the title. */
@Composable
fun PullHeader(title: String, sub: String, pal: Pal, pull: Float, maxPull: Float) {
    val f = (pull / maxPull).coerceIn(0f, 1f)
    val h = 150.dp + with(LocalDensity.current) { pull.toDp() }
    Box(Modifier.fillMaxWidth().height(h)) {
        Column(
            Modifier.align(BiasAlignment(-1f + f, 1f - 1.1f * f)).padding(start = 12.dp * (1f - f), bottom = 16.dp * (1f - f)),
            horizontalAlignment = BiasAlignment.Horizontal(-1f + f)
        ) {
            Text(title, fontSize = 36.sp, fontWeight = FontWeight.Bold, color = lerp(pal.text, pal.hi, f))
            if (sub.isNotEmpty()) Text(sub, color = pal.acc3)
        }
    }
}

@Composable fun Label(pal: Pal, t: String) =
    Text(t, color = pal.acc3, fontSize = 13.sp, modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 6.dp))

@Composable
fun Group(pal: Pal, content: @Composable ColumnScope.() -> Unit) =
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(pal.surf), content = content)

@Composable
fun OneSwitch(pal: Pal, on: Boolean, set: ((Boolean) -> Unit)?) = Switch(
    checked = on, onCheckedChange = set,
    colors = SwitchDefaults.colors(
        checkedThumbColor = Color.White, checkedTrackColor = pal.acc, checkedBorderColor = Color.Transparent,
        uncheckedThumbColor = Color.White, uncheckedTrackColor = Color(0xFF5F5F63), uncheckedBorderColor = Color.Transparent
    )
)

@Composable
fun SwitchRow(pal: Pal, title: String, sub: String, on: Boolean, set: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().toggleable(value = on, role = Role.Switch, onValueChange = set)
            .padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 17.sp)
            if (sub.isNotEmpty()) Text(sub, color = pal.sub, fontSize = 13.sp)
        }
        Spacer(Modifier.width(12.dp))
        OneSwitch(pal, on, null)
    }
}

@Composable
fun Div(pal: Pal) = HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = pal.sub.copy(alpha = .2f))

@Composable
fun Seg(pal: Pal, opts: List<String>, sel: Int, set: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(pal.surf).padding(6.dp)) {
        opts.forEachIndexed { i, l ->
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(22.dp)).background(if (sel == i) pal.acc else Color.Transparent)
                    .clickable { set(i) }.padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) { Text(l, color = if (sel == i) onAcc(pal.acc) else pal.text, fontWeight = FontWeight.Medium) }
        }
    }
}

@Composable
fun ChipRow(pal: Pal, labels: List<String>, sel: Int, onSel: (Int) -> Unit, lead: @Composable (Int, Color) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.forEachIndexed { k, name ->
            val s = sel == k
            val tint = if (s) onAcc(pal.acc) else pal.text
            Row(
                Modifier.clip(RoundedCornerShape(22.dp)).background(if (s) pal.acc else pal.surf)
                    .clickable { onSel(k) }.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                lead(k, tint)
                Text(name, color = tint, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun PaletteSwatch(cols: List<Color>) {
    Row(Modifier.size(18.dp).clip(CircleShape)) { cols.forEach { Box(Modifier.weight(1f).fillMaxHeight().background(it)) } }
    Spacer(Modifier.width(8.dp))
}

@Composable
fun DropdownRow(pal: Pal, title: String, options: List<String>, sel: Int, onSel: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().clickable { open = true }.padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontSize = 17.sp, modifier = Modifier.weight(1f))
            Text(options[sel], color = pal.hi)
        }
        if (open) Popup(alignment = Alignment.TopStart, onDismissRequest = { open = false }, properties = PopupProperties(focusable = true)) {
            var shown by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { shown = true }
            val sc by animateFloatAsState(if (shown) 1f else 0.85f, spring(dampingRatio = 0.8f, stiffness = 500f), label = "pop")
            Surface(
                shape = RoundedCornerShape(28.dp), color = lerp(pal.surf, pal.text, 0.14f), shadowElevation = 12.dp,
                modifier = Modifier.width(260.dp).graphicsLayer {
                    scaleX = sc; scaleY = sc; alpha = if (shown) 1f else 0f; transformOrigin = TransformOrigin(0f, 0f)
                }
            ) {
                Column(Modifier.padding(vertical = 6.dp)) {
                    options.forEachIndexed { i, o ->
                        val on = i == sel
                        Row(
                            Modifier.fillMaxWidth().clickable { onSel(i); open = false }.padding(horizontal = 22.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(o, fontSize = 18.sp, color = if (on) pal.hi else pal.text,
                                fontWeight = if (on) FontWeight.Medium else FontWeight.Normal, modifier = Modifier.weight(1f))
                            if (on) Canvas(Modifier.size(20.dp)) {
                                drawPath(Path().apply {
                                    moveTo(size.width * .1f, size.height * .55f); lineTo(size.width * .4f, size.height * .85f); lineTo(size.width * .92f, size.height * .18f)
                                }, pal.hi, style = Stroke(size.width * .1f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Swatch(c: Color, ring: Color) {
    Box(Modifier.size(18.dp).clip(CircleShape).background(c).border(2.dp, ring.copy(alpha = .6f), CircleShape))
    Spacer(Modifier.width(8.dp))
}

@Composable
fun Home(
    pal: Pal, pf: Prefs, tab: Int, mode: Int, names: MutableList<String>, cpu: MutableList<Boolean>, sysPal: List<Color>,
    setTab: (Int) -> Unit, setMode: (Int) -> Unit, onStart: (Game) -> Unit
) {
    var menu by remember { mutableIntStateOf(0) }
    BackHandler(menu != 0) { menu = 0 }
    val n = when (mode) { 2 -> 5; 3 -> 6; else -> 4 }
    val team = mode == 1
    val activeSeats = when (mode) { 4 -> listOf(0, 2); 5 -> listOf(0, 1, 2); else -> (0 until n).toList() }
    val fieldColors = TextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent
    )
    // One-hand mode: expandable app bar (expanded = 39.67% of screen height per the One UI guidelines)
    val density = LocalDensity.current
    val sh = LocalConfiguration.current.screenHeightDp
    val maxPull = with(density) { (sh * 0.3967f - 150f).coerceAtLeast(60f).dp.toPx() }
    var pullPx by remember { mutableFloatStateOf(0f) }
    val conn = remember(maxPull) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < 0f && pullPx > 0f) {
                    val nv = (pullPx + available.y).coerceAtLeast(0f); val used = nv - pullPx; pullPx = nv
                    return Offset(0f, used)
                }
                return Offset.Zero
            }
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0f && source == NestedScrollSource.Drag) {
                    pullPx = (pullPx + available.y).coerceAtMost(maxPull)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }
            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                animate(pullPx, if (pullPx > maxPull * 0.35f) maxPull else 0f, animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)) { v, _ -> pullPx = v }
                return Velocity.Zero
            }
        }
    }
    val contentLayer = rememberGraphicsLayer()
    var contentPos by remember { mutableStateOf(Offset.Zero) }
    var pillPos by remember { mutableStateOf(Offset.Zero) }
    Box(Modifier.fillMaxSize().imePadding()) {
        Box(
            Modifier.fillMaxSize().onGloballyPositioned { contentPos = it.positionInRoot() }
                .drawWithContent { contentLayer.record { this@drawWithContent.drawContent() }; drawLayer(contentLayer) }
        ) {
        Column(Modifier.fillMaxSize().nestedScroll(conn).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp)) {
            if (tab == 3) {
                PullHeader("Settings", "", pal, pullPx, maxPull)
                SettingsList(pal, pf, sysPal)
                Spacer(Modifier.height(140.dp))
            } else {
                PullHeader(
                    listOf("Classic", "Team Up", "5 Players", "6 Players", "2 Players", "3 Players")[mode],
                    listOf("Every player for themselves", "2v2", "Five-arm ring board", "Six-arm ring board", "Classic board, two players", "Classic board, three players")[mode], pal, pullPx, maxPull
                )
                val groups = if (team) listOf(listOf(0, 2) to "Team 1", listOf(1, 3) to "Team 2") else listOf(activeSeats to "Players")
                groups.forEach { (seats, label) ->
                    Label(pal, label)
                    Group(pal) {
                        seats.forEachIndexed { k, i ->
                            if (k > 0) Div(pal)
                            Row(Modifier.padding(start = 20.dp, end = 16.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(20.dp).clip(CircleShape).background(pal.p[i]))
                                TextField(
                                    value = names[i], onValueChange = { names[i] = it }, singleLine = true,
                                    placeholder = { Text("Player ${activeSeats.indexOf(i).let { j -> if (j >= 0) j + 1 else i + 1 }}") }, colors = fieldColors, modifier = Modifier.weight(1f)
                                )
                                Text("CPU", color = pal.sub, fontSize = 13.sp)
                                Spacer(Modifier.width(6.dp))
                                OneSwitch(pal, cpu[i]) { cpu[i] = it }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(220.dp))
            }
        }
        }
        Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding()) {
            if (tab != 3) Button(
                onClick = {
                    onStart(Game(n, team, cpu.take(n), List(n) { s -> names[s].trim().ifEmpty { "Player ${activeSeats.indexOf(s).let { j -> if (j >= 0) j + 1 else s + 1 }}" } }, pf.stack.v,
                        pf.unlock1.v, pf.first.v, pf.autoOut.v, pf.autoSingle.v, pf.cancel6.v, pf.pseudo.v, pf.startOne.v, activeSeats,
                        pf.arrowBonus.v, pf.killLock.v, pf.removeWith1.v, pf.pieces.v))
                },
                shape = RoundedCornerShape(50), modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(52.dp)
            ) { Text("Start game") }
            NavBar(pal, tab, mode, menu,
                onTab = { i ->
                    when (i) {
                        0 -> { menu = if (menu == 1) 0 else 1; if (mode != 0 && mode != 4 && mode != 5) setMode(0); setTab(0) }
                        1 -> { menu = 0; setMode(1); setTab(1) }
                        2 -> { menu = if (menu == 2) 0 else 2; if (mode != 2 && mode != 3) setMode(2); setTab(2) }
                        else -> { menu = 0; setTab(3) }
                    }
                },
                onPick = { m -> setMode(m); setTab(if (m == 2 || m == 3) 2 else 0); menu = 0 },
                layer = contentLayer, offset = { pillPos - contentPos }, onPill = { pillPos = it })
        }
    }
}

private fun DrawScope.star(ctr: Offset, r: Float, col: Color) {
    val p = Path()
    for (i in 0 until 10) {
        val rad = if (i % 2 == 0) r else r * 0.45f
        val a = -PI / 2 + i * PI / 5
        val x = ctr.x + (rad * cos(a)).toFloat()
        val y = ctr.y + (rad * sin(a)).toFloat()
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close(); drawPath(p, col)
}

private fun DrawScope.pieceIcon(kind: Int, c: Offset, r: Float, col: Color) {
    when (kind) {
        1 -> star(c, r, col)
        2 -> {
            drawCircle(col, r * .5f, Offset(c.x - r * .48f, c.y - r * .25f))
            drawCircle(col, r * .5f, Offset(c.x + r * .48f, c.y - r * .25f))
            drawPath(Path().apply { moveTo(c.x - r * .95f, c.y - r * .05f); lineTo(c.x + r * .95f, c.y - r * .05f); lineTo(c.x, c.y + r * .9f); close() }, col)
        }
        3 -> drawPath(Path().apply { moveTo(c.x, c.y - r); lineTo(c.x + r * .75f, c.y); lineTo(c.x, c.y + r); lineTo(c.x - r * .75f, c.y); close() }, col)
        4 -> {
            drawCircle(col, r * .38f, Offset(c.x, c.y - r * .55f))
            drawPath(Path().apply { moveTo(c.x - r * .3f, c.y - r * .2f); lineTo(c.x + r * .3f, c.y - r * .2f); lineTo(c.x + r * .6f, c.y + r * .65f); lineTo(c.x - r * .6f, c.y + r * .65f); close() }, col)
        }
    }
}

private val PIPS = listOf(
    emptyList(),
    listOf(.5f to .5f),
    listOf(.27f to .27f, .73f to .73f),
    listOf(.27f to .27f, .5f to .5f, .73f to .73f),
    listOf(.27f to .27f, .73f to .27f, .27f to .73f, .73f to .73f),
    listOf(.27f to .27f, .73f to .27f, .5f to .5f, .27f to .73f, .73f to .73f),
    listOf(.27f to .22f, .73f to .22f, .27f to .5f, .73f to .5f, .27f to .78f, .73f to .78f)
)

private class Face(val n: FloatArray, val u: FloatArray, val v: FloatArray, val value: Int)
private val FACES = listOf(
    Face(floatArrayOf(0f, 0f, 1f), floatArrayOf(1f, 0f, 0f), floatArrayOf(0f, 1f, 0f), 1),
    Face(floatArrayOf(0f, 0f, -1f), floatArrayOf(-1f, 0f, 0f), floatArrayOf(0f, 1f, 0f), 6),
    Face(floatArrayOf(1f, 0f, 0f), floatArrayOf(0f, 0f, -1f), floatArrayOf(0f, 1f, 0f), 3),
    Face(floatArrayOf(-1f, 0f, 0f), floatArrayOf(0f, 0f, 1f), floatArrayOf(0f, 1f, 0f), 4),
    Face(floatArrayOf(0f, -1f, 0f), floatArrayOf(1f, 0f, 0f), floatArrayOf(0f, 0f, 1f), 2),
    Face(floatArrayOf(0f, 1f, 0f), floatArrayOf(1f, 0f, 0f), floatArrayOf(0f, 0f, -1f), 5)
)

/** A real 3D cube with pips on all six faces (opposite faces sum to 7), projected with perspective. */
private fun DrawScope.cube(rx: Float, ry: Float, edge: Color, ew: Float, tilt: Float, grow: Float) {
    val k = (PI / 180).toFloat()
    val angles = floatArrayOf(ry, rx, -30f * tilt, -25f * tilt)
    fun tf(x0: Float, y0: Float, z0: Float): FloatArray {
        var x = x0; var y = y0; var z = z0
        for (i in 0..3) {
            val a = angles[i] * k; val c = cos(a); val s = sin(a)
            if (i % 2 == 0) { val nx = x * c + z * s; z = -x * s + z * c; x = nx }
            else { val ny = y * c - z * s; z = y * s + z * c; y = ny }
        }
        return floatArrayOf(x, y, z)
    }
    val sc = size.minDimension * (0.27f + 0.1f * grow)
    fun pr(p: FloatArray): Offset {
        val f = 6f / (6f - p[2])
        return Offset(size.width / 2 + p[0] * sc * f, size.height / 2 + p[1] * sc * f)
    }
    for (f in FACES) {
        val nt = tf(f.n[0], f.n[1], f.n[2])
        if (nt[2] <= 0.02f) continue
        fun pt(a: Float, b: Float) = tf(
            f.n[0] + a * f.u[0] + b * f.v[0], f.n[1] + a * f.u[1] + b * f.v[1], f.n[2] + a * f.u[2] + b * f.v[2]
        )
        val pts = listOf(1f to 1f, -1f to 1f, -1f to -1f, 1f to -1f).map { (a, b) -> pr(pt(a, b)) }
        val poly = Path()
        val rad = 12.dp.toPx()
        for (i in 0..3) {
            val cur = pts[i]; val pv = pts[(i + 3) % 4]; val nx = pts[(i + 1) % 4]
            val d1 = (pv - cur).getDistance(); val d2 = (nx - cur).getDistance()
            val p1 = cur + (pv - cur) * (min(rad, d1 / 2) / d1)
            val p2 = cur + (nx - cur) * (min(rad, d2 / 2) / d2)
            if (i == 0) poly.moveTo(p1.x, p1.y) else poly.lineTo(p1.x, p1.y)
            poly.quadraticBezierTo(cur.x, cur.y, p2.x, p2.y)
        }
        poly.close()
        val shade = 0.70f + 0.30f * (nt[0] * -0.35f + nt[1] * -0.55f + nt[2] * 0.75f).coerceIn(0f, 1f)
        drawPath(poly, Color(shade, shade, shade))
        drawPath(poly, edge, style = Stroke(ew, join = StrokeJoin.Round))
        for ((px, py) in PIPS[f.value]) {
            val a = (px - .5f) * 1.9f; val b = (py - .5f) * 1.9f
            val pp = Path()
            for (j in 0 until 12) {
                val th = j * 2f * PI.toFloat() / 12f
                val o = pr(pt(a + .17f * cos(th), b + .17f * sin(th)))
                if (j == 0) pp.moveTo(o.x, o.y) else pp.lineTo(o.x, o.y)
            }
            pp.close(); drawPath(pp, Color(0xFF222222))
        }
    }
}

@Composable
fun PlayerDice(g: Game, p: Int, pal: Pal, pulse: Float, side: Int, enabled: Boolean, fx: Fx, onRoll: () -> Unit) {
    val active = g.turn == p && !g.over
    val canRoll = active && enabled && !g.cpu[p] && !g.rolled
    val v = g.lastRoll[p]
    val base = when (v) { 6 -> 0f to 180f; 3 -> 0f to -90f; 4 -> 0f to 90f; 2 -> -90f to 0f; 5 -> 90f to 0f; else -> 0f to 0f }
    val rx by animateFloatAsState(base.first + g.rolls[p] * 360f, tween(800, easing = FastOutSlowInEasing), label = "rx")
    val ry by animateFloatAsState(base.second + g.rolls[p] * 720f, tween(800, easing = FastOutSlowInEasing), label = "ry")
    val spinning = abs(rx - (base.first + g.rolls[p] * 360f)) > 0.5f || abs(ry - (base.second + g.rolls[p] * 720f)) > 0.5f
    val tilt by animateFloatAsState(if (spinning) 1f else 0f, tween(250), label = "tilt")
    LaunchedEffect(g.rolls[p]) { if (g.rolls[p] > 0) fx.roll() }
    val col = pal.p[p]
    val sub = if (active && g.mover() != p) "helping partner" else if (g.cpu[p]) "CPU" else " "
    val diceBox: @Composable () -> Unit = {
        Box(
            Modifier.size(76.dp).clickable(
                interactionSource = remember { MutableInteractionSource() }, indication = null, enabled = canRoll, onClick = onRoll
            )
        ) {
            Canvas(Modifier.fillMaxSize()) {
                if (canRoll) drawCircle(col.copy(alpha = 0.22f), size.minDimension * 0.48f * (0.9f + (pulse - 0.85f) * 0.25f))
                cube(rx, ry, col, if (active) 5f else 3f, tilt, 1f - tilt)
            }
        }
    }
    val label: @Composable (Alignment.Horizontal) -> Unit = { al ->
        Column(horizontalAlignment = al) {
            Text(g.names[p], fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, modifier = Modifier.widthIn(max = 92.dp))
            Text(sub, fontSize = 11.sp, color = pal.sub)
        }
    }
    Box(Modifier.alpha(if (active) 1f else 0.6f)) {
        when (side) {
            0 -> Row(verticalAlignment = Alignment.CenterVertically) { diceBox(); Spacer(Modifier.width(8.dp)); label(Alignment.Start) }
            1 -> Row(verticalAlignment = Alignment.CenterVertically) { label(Alignment.End); Spacer(Modifier.width(8.dp)); diceBox() }
            else -> Column(horizontalAlignment = Alignment.CenterHorizontally) { label(Alignment.CenterHorizontally); diceBox() }
        }
    }
}

@Composable
fun DiceRow(l: List<Int>, g: Game, pal: Pal, pulse: Float, enabled: Boolean, fx: Fx, roll: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = if (l.size >= 3) Arrangement.SpaceEvenly
        else if (l.size == 1 && g.b.baseCenter(l[0]).first > 7.5f) Arrangement.End else Arrangement.SpaceBetween
    ) {
        l.forEachIndexed { k, p -> PlayerDice(g, p, pal, pulse, if (l.size >= 3) 2 else if (l.size == 1 && g.b.baseCenter(p).first > 7.5f) 1 else k, enabled, fx) { roll() } }
    }
}

@Composable
fun GameScreen(g: Game, pal: Pal, pf: Prefs, sysPal: List<Color>, onRestart: () -> Unit, onQuit: () -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    var paused by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    BackHandler { confirm = true }
    val ctx = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val fx = remember(g, pf.sound.v, pf.vib.v) { Fx(ctx, pf.sound.v, pf.vib.v) }
    DisposableEffect(fx) { onDispose { fx.release() } }
    val b = g.b
    val total = g.ppp * g.n
    val stepMs = (450 - 3.5f * pf.speedPct.v).toInt()
    var busy by remember { mutableIntStateOf(0) }
    var burst by remember { mutableIntStateOf(0) }
    val burstAnim = remember { Animatable(0f) }
    LaunchedEffect(burst) {
        if (burst > 0) { burstAnim.snapTo(0f); burstAnim.animateTo(1f, tween(2600, easing = LinearEasing)) }
    }
    LaunchedEffect(g.tick, g.turn, g.rolled, g.over, busy, paused) {
        if (g.over || busy > 0 || paused) return@LaunchedEffect
        val cpuTurn = g.cpu[g.turn]
        if (!cpuTurn && !(pf.autoRoll.v && !g.rolled)) return@LaunchedEffect
        delay(if (cpuTurn) 700 else 450)
        if (!g.rolled) g.roll() else g.move(g.aiPick())
    }
    // Moving steps on every square; a captured piece waits for the capturer, hisses, then glides back along its path.
    val shown = remember(g) { List(total) { Animatable(g.pos[it].toFloat()) } }
    shown.forEachIndexed { idx, a ->
        LaunchedEffect(g.pos[idx]) {
            val t = g.pos[idx].toFloat()
            busy++
            try {
                if (t < 0f) {
                    if (a.value > 0f) {
                        delay(g.dice * (stepMs + 40L) + 100)
                        fx.hiss()
                        a.animateTo(t, tween((a.value * 70f).toInt().coerceIn(300, 2000), easing = FastOutSlowInEasing))
                    } else a.snapTo(t)
                } else if (t > a.value) {
                    var s = floor(a.value).toInt() + 1
                    while (s <= t.toInt()) {
                        a.animateTo(s.toFloat(), tween(stepMs, easing = FastOutSlowInEasing))
                        fx.step(); delay(40); s++
                    }
                    if (t == b.home.toFloat()) { burst++; fx.cheer() }
                    else if (t <= b.last.toFloat() && (b.start(idx / g.ppp) + t.toInt()) % b.tl in b.safe) fx.safe()
                } else a.snapTo(t)
            } finally { busy-- }
        }
    }
    val pulse by rememberInfiniteTransition(label = "pulse")
        .animateFloat(0.85f, 1.25f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "pulse")
    val spin by rememberInfiniteTransition(label = "spin")
        .animateFloat(0f, 360f, infiniteRepeatable(tween(450, easing = LinearEasing)), label = "spin")
    val human = !g.cpu[g.turn] && !g.over
    val mv = if (human && g.rolled) g.moves() else emptyList()
    val roll = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); g.roll() }
    val tops = g.active.filter { b.baseCenter(it).second < 7.5f }.sortedBy { b.baseCenter(it).first }
    val bots = g.active.filter { it !in tops }.sortedBy { b.baseCenter(it).first }

    Box(Modifier.fillMaxSize()) {
    BurstLayer(pal, burst, burstAnim)
    Box(
        Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 16.dp).size(44.dp).clip(CircleShape)
            .background(pal.surf).clickable { paused = true },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(18.dp)) {
            drawRoundRect(pal.text, Offset(size.width * .16f, size.height * .08f), Size(size.width * .26f, size.height * .84f), CornerRadius(3f))
            drawRoundRect(pal.text, Offset(size.width * .58f, size.height * .08f), Size(size.width * .26f, size.height * .84f), CornerRadius(3f))
        }
    }
    Column(
        Modifier.fillMaxSize().navigationBarsPadding().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DiceRow(tops, g, pal, pulse, busy == 0 && !paused, fx, roll)
        Spacer(Modifier.height(16.dp))
        Canvas(
            Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(24.dp)).background(pal.surf)
                .pointerInput(g) {
                    detectTapGestures { off ->
                        if (g.cpu[g.turn] || !g.rolled || g.over || paused) return@detectTapGestures
                        val c = size.width / 15f
                        fun d(i: Int): Float {
                            val (x, y) = b.xy(i / g.ppp, i % g.ppp, g.pos[i].toFloat())
                            return (Offset(x * c, y * c) - off).getDistance()
                        }
                        val best = g.moves().minByOrNull { d(it) }
                        if (best != null && d(best) < c * (if (b.radial) 0.7f else 0.9f)) g.move(best)
                    }
                }
        ) {
            val c = size.width / 15f
            val line = pal.sub.copy(alpha = 0.35f)
            fun pc(o: Int) = if (o in g.active) pal.p[o] else pal.p[o].copy(alpha = 0.2f)
            fun P(p: Pair<Float, Float>) = Offset(p.first * c, p.second * c)
            if (!b.radial) {
                fun cell(x: Int, y: Int, fill: Color) {
                    drawRect(fill, Offset(x * c, y * c), Size(c, c))
                    drawRect(line, Offset(x * c, y * c), Size(c, c), style = Stroke(1.5f))
                }
                for (o in 0..3) {
                    val x0 = if (o == 1 || o == 2) 9 else 0
                    val y0 = if (o >= 2) 9 else 0
                    drawRect(pc(o), Offset(x0 * c, y0 * c), Size(6 * c, 6 * c))
                    drawRoundRect(pal.surf, Offset((x0 + 1) * c, (y0 + 1) * c), Size(4 * c, 4 * c), CornerRadius(c * 0.6f))
                    for (i in 0 until g.ppp) drawCircle(pc(o).copy(alpha = 0.35f), c * 0.42f, P(b.base(o, i)))
                }
                TRACK.forEach { (x, y) -> cell(x, y, pal.surf) }
                for (o in 0..3) for (s in 0..4) {
                    val (x, y) = homeCell(o, s)
                    cell(x, y, if (o in g.active && g.homeLocked(o)) pc(o).copy(alpha = 0.3f) else pc(o))
                }
                for (o in g.active) {
                    if (g.homeLocked(o)) { val (lx, ly) = b.col(o, 2); lock(Offset(lx * c, ly * c), c * 0.32f, pal.p[o]) }
                    if (g.arrowBonus) {
                        val a = b.track((b.start(o) + b.last) % b.tl); val c0 = b.col(o, 0)
                        arrow(P(a), Offset(c0.first - a.first, c0.second - a.second), c * 0.28f, pal.p[o])
                    }
                }
                TRACK.forEachIndexed { k, (x, y) ->
                    val st = if (k % 13 == 0) k / 13 else -1
                    if (st >= 0) { cell(x, y, pc(st)); star(Offset((x + .5f) * c, (y + .5f) * c), c * 0.34f, Color.White) }
                    else if (k in b.safe) star(Offset((x + .5f) * c, (y + .5f) * c), c * 0.34f, pal.sub)
                }
                val m = 7.5f * c
                fun tri(o: Int, a: Offset, bb: Offset) = drawPath(
                    Path().apply { moveTo(a.x, a.y); lineTo(bb.x, bb.y); lineTo(m, m); close() }, pc(o)
                )
                tri(0, Offset(6 * c, 6 * c), Offset(6 * c, 9 * c))
                tri(1, Offset(6 * c, 6 * c), Offset(9 * c, 6 * c))
                tri(2, Offset(9 * c, 6 * c), Offset(9 * c, 9 * c))
                tri(3, Offset(6 * c, 9 * c), Offset(9 * c, 9 * c))
            } else {
                val cr = c * (if (g.n == 5) .34f else .28f)
                for (o in 0 until g.n) {
                    drawCircle(pal.p[o].copy(alpha = .22f), c * 1.25f, P(b.baseCenter(o)))
                    for (i in 0 until g.ppp) drawCircle(pal.p[o].copy(alpha = .5f), c * .3f, P(b.base(o, i)))
                }
                for (k in 0 until b.tl) {
                    val ctr = P(b.track(k))
                    val st = k % b.per == 0
                    drawCircle(if (st) pal.p[k / b.per] else pal.surf, cr, ctr)
                    drawCircle(line, cr, ctr, style = Stroke(1.5f))
                    if (k in b.safe) star(ctr, cr * .8f, if (st) Color.White else pal.sub)
                }
                for (o in 0 until g.n) {
                    for (s in 0..4) drawCircle(if (g.homeLocked(o)) pal.p[o].copy(alpha = .3f) else pal.p[o], c * .34f, P(b.col(o, s)))
                    if (g.homeLocked(o)) lock(P(b.col(o, 2)), c * .3f, pal.text)
                    if (g.arrowBonus) {
                        val a = b.track((b.start(o) + b.last) % b.tl); val c0 = b.col(o, 0)
                        arrow(P(a), Offset(c0.first - a.first, c0.second - a.second), c * .24f, pal.p[o])
                    }
                    val a = b.colAng(o); val h = PI / g.n
                    val pa = Path().apply {
                        moveTo(7.5f * c, 7.5f * c)
                        lineTo(c * (7.5f + 1.7f * cos(a - h)).toFloat(), c * (7.5f + 1.7f * sin(a - h)).toFloat())
                        lineTo(c * (7.5f + 1.7f * cos(a + h)).toFloat(), c * (7.5f + 1.7f * sin(a + h)).toFloat())
                        close()
                    }
                    drawPath(pa, pal.p[o])
                }
            }
            val mvr = g.mover()
            val groups = HashMap<Pair<Float, Float>, MutableList<Int>>()
            for (i in 0 until total) {
                if (i / g.ppp !in g.active || g.gone(i)) continue
                groups.getOrPut(b.xy(i / g.ppp, i % g.ppp, g.pos[i].toFloat())) { mutableListOf() }.add(i)
            }
            val slot = IntArray(total)
            val zIdx = IntArray(total)
            for (grp in groups.values) {
                grp.sortWith(compareBy<Int>({ if (it / g.ppp == mvr) 1 else 0 }, { it }))
                grp.forEachIndexed { z, i -> zIdx[i] = z; slot[i] = grp.size - 1 - z }
            }
            for (idx in (0 until total).sortedWith(compareBy<Int>({ abs(shown[it].value - g.pos[it]) > 0.01f }, { zIdx[it] }))) {
                val o = idx / g.ppp
                if (o !in g.active || g.gone(idx)) continue
                val q = shown[idx].value
                val (x, y) = b.xy(o, idx % g.ppp, q)
                val hop = 1f + .3f * sin(PI * (q - floor(q))).toFloat()
                val ctr = Offset(x * c + slot[idx] * c * 0.17f, y * c - slot[idx] * c * 0.17f)
                val r = c * b.tokR * hop
                val rr = r + c * 0.06f
                drawCircle(Color.Black.copy(alpha = 0.12f), rr * 1.18f, ctr + Offset(0f, c * 0.09f))
                drawCircle(Color.Black.copy(alpha = 0.28f), rr, ctr + Offset(0f, c * 0.07f))
                drawCircle(Color.White, rr, ctr)
                drawCircle(pal.p[o], r, ctr)
                val icon = pf.icon.v
                if (icon == 0) drawCircle(Color.White.copy(alpha = 0.4f), r * 0.4f, ctr - Offset(r * 0.25f, r * 0.25f))
                else pieceIcon(icon, ctr, r * 0.58f, Color.White)
                if (idx in mv) {
                    val bo = pf.indLevel.v / 50f
                    val sw = c * 0.07f * (1f + 0.7f * bo)
                    if (bo > 0f) drawCircle(pal.text.copy(alpha = 0.16f * bo), rr * 1.5f, ctr)
                    if (pf.indicator.v == 0) drawCircle(pal.text, rr * pulse, ctr, style = Stroke(sw))
                    else {
                        val rad = rr * 1.3f
                        val gap = (2 * PI * rad / 12).toFloat()
                        rotate(spin, ctr) {
                            drawCircle(pal.text, rad, ctr, style = Stroke(sw * 1.3f, cap = StrokeCap.Round,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.01f, gap))))
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        DiceRow(bots, g, pal, pulse, busy == 0 && !paused, fx, roll)
    }
    }
    val red = Color(0xFFE5484D)
    val modeLabel = if (g.team) "Team Up 2v2" else when (g.n) { 5 -> "5 players"; 6 -> "6 players"; else -> "Classic · ${g.active.size} players" }
    if (paused && !confirm && !settingsOpen) OneDialog(pal, "Paused", "", listOf(
        DlgAction("Resume", pal.hi, true) { paused = false },
        DlgAction("Settings", pal.text) { settingsOpen = true },
        DlgAction("Restart game", pal.text) { onRestart() },
        DlgAction("Quit game", red) { confirm = true }
    ), extra = { GameDetails(g, pal, modeLabel) }) { paused = false }
    if (settingsOpen) SettingsSheet(pal, pf, sysPal) { settingsOpen = false }
    if (confirm) OneDialog(pal, "Quit game?", "Your current game will be lost.", listOf(
        DlgAction("Cancel", pal.text) { confirm = false },
        DlgAction("Quit", red, true) { confirm = false; onQuit() }
    )) { confirm = false }
    if (g.over) OneDialog(pal, g.title, g.order.mapIndexed { i, o -> "${i + 1}. ${g.names[o]}" }.joinToString("\n"),
        listOf(DlgAction("New game", pal.hi, true, onQuit)), confetti = true, onDismiss = null)
}

class DlgAction(val label: String, val color: Color, val bold: Boolean = false, val onClick: () -> Unit)

private class Bit(val x: Float, val phase: Float, val speed: Float, val w: Float, val h: Float, val spin: Float, val ci: Int)

/** Endless confetti: every particle completes whole cycles per loop, so it never jumps. */
@Composable
fun Confetti(pal: Pal) {
    val t by rememberInfiniteTransition(label = "confetti")
        .animateFloat(0f, 1f, infiniteRepeatable(tween(7000, easing = LinearEasing)), label = "confetti")
    val bits = remember {
        List(80) {
            Bit(Random.nextFloat(), Random.nextFloat(), Random.nextInt(1, 4).toFloat(), 6f + Random.nextFloat() * 8f,
                10f + Random.nextFloat() * 10f, Random.nextInt(1, 4).toFloat(), Random.nextInt(6))
        }
    }
    Canvas(Modifier.fillMaxSize()) {
        for (b in bits) {
            val prog = (t * b.speed + b.phase) % 1f
            val y = prog * (size.height + 60.dp.toPx()) - 30.dp.toPx()
            val x = b.x * size.width + sin((prog * 4f + b.phase * 6f) * 2f * PI.toFloat()) * 18.dp.toPx()
            val w = b.w.dp.toPx(); val h = b.h.dp.toPx()
            rotate(prog * 360f * b.spin + b.phase * 360f, Offset(x, y)) {
                drawRect(pal.p[b.ci], Offset(x - w / 2, y - h / 2), Size(w, h))
            }
        }
    }
}

/** One UI style dialog: bottom-anchored rounded card, centred text, pill buttons (side by side, or stacked when 3+). */
@Composable
fun OneDialog(pal: Pal, title: String, text: String, actions: List<DlgAction>, extra: (@Composable () -> Unit)? = null, confetti: Boolean = false, onDismiss: (() -> Unit)?) {
    Dialog(
        onDismissRequest = { onDismiss?.invoke() },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = onDismiss != null, dismissOnClickOutside = false)
    ) {
        Box(
            Modifier.fillMaxSize().then(
                if (onDismiss != null) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDismiss() }
                else Modifier
            )
        ) {
            if (confetti) Confetti(pal)
            Box(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 24.dp), contentAlignment = Alignment.BottomCenter) {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(pal.surf).pointerInput(Unit) { detectTapGestures { } }.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = pal.text)
                    if (text.isNotEmpty()) Text(text, color = pal.sub, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp))
                    extra?.invoke()
                    Spacer(Modifier.height(24.dp))
                    val btn: @Composable (DlgAction, Modifier) -> Unit = { a, mod ->
                        Box(mod.height(52.dp).clip(CircleShape).background(pal.bg).clickable(onClick = a.onClick), contentAlignment = Alignment.Center) {
                            Text(a.label, fontWeight = if (a.bold) FontWeight.Bold else FontWeight.Medium, color = a.color)
                        }
                    }
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        actions.chunked(if (actions.size == 4 || actions.size <= 2) 2 else 1).forEach { r ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) { r.forEach { btn(it, Modifier.weight(1f)) } }
                        }
                    }
                }
            }
        }
    }
}

private class Spark(val side: Float, val y0: Float, val speed: Float, val ang: Float, val w: Float, val h: Float, val spin: Float, val ci: Int)

/** Confetti cannons behind the board: fires from both side edges when a piece reaches home. */
@Composable
fun BurstLayer(pal: Pal, key: Int, anim: Animatable<Float, AnimationVector1D>) {
    val sparks = remember(key) {
        List(70) {
            Spark(if (it % 2 == 0) 1f else -1f, Random.nextFloat() * 2f - 1f, 650f + Random.nextFloat() * 450f,
                8f + Random.nextFloat() * 45f, 6f + Random.nextFloat() * 6f, 10f + Random.nextFloat() * 8f,
                Random.nextFloat() * 6f - 3f, Random.nextInt(6))
        }
    }
    Canvas(Modifier.fillMaxSize()) {
        val p = anim.value
        if (p <= 0f || p >= 1f) return@Canvas
        val t = p * 2.6f
        val edge = 16.dp.toPx()
        val g = 1000.dp.toPx()
        val alpha = min(1f, (1f - p) * 3f)
        for (s in sparks) {
            val a = s.ang * PI.toFloat() / 180f
            val v = s.speed.dp.toPx()
            val x0 = if (s.side > 0f) edge else size.width - edge
            val y0 = size.height / 2 + s.y0 * 120.dp.toPx()
            val x = x0 + s.side * v * sin(a) * t
            val y = y0 - v * cos(a) * t + 0.5f * g * t * t
            val w = s.w.dp.toPx(); val h = s.h.dp.toPx()
            rotate(s.spin * t * 360f, Offset(x, y)) {
                drawRect(pal.p[s.ci].copy(alpha = alpha), Offset(x - w / 2, y - h / 2), Size(w, h))
            }
        }
    }
}

@Composable
fun SettingsList(pal: Pal, pf: Prefs, sysPal: List<Color>) {
                Label(pal, "Appearance")
                Seg(pal, listOf("System", "Light", "Dark"), pf.theme.v) { pf.theme.v = it }
                Label(pal, "Accent color")
                ChipRow(pal, listOf("System") + ACCENTS.map { it.first }, pf.accent.v, { pf.accent.v = it }) { k, tint ->
                    if (k == 0) PaletteSwatch(sysPal) else Swatch(Color(ACCENTS[k - 1].second), tint)
                }
                Label(pal, "Piece icon")
                ChipRow(pal, ICONS, pf.icon.v, { pf.icon.v = it }) { k, tint ->
                    if (k > 0) { Canvas(Modifier.size(18.dp)) { pieceIcon(k, center, size.width * .48f, tint) }; Spacer(Modifier.width(8.dp)) }
                }
                Label(pal, "Turn indicator")
                Group(pal) {
                    DropdownRow(pal, "Movable pieces", listOf("Pulsing circle", "Spinning dotted circle"), pf.indicator.v) { pf.indicator.v = it }
                }
                Label(pal, "Indicator brightness")
                Group(pal) { SliderRow(pal, "Brightness", "${pf.indLevel.v}%", pf.indLevel.v, { pf.indLevel.v = it }, "Normal", "Intense") }
                Label(pal, "Rules")
                Group(pal) {
                    SwitchRow(pal, "Unlock pieces with 1 and 6", "Off: only a 6 unlocks a piece", pf.unlock1.v) { pf.unlock1.v = it }
                    Div(pal)
                    SwitchRow(pal, "Start with 1 piece unlocked", "Each player begins with one piece already out on its start square.", pf.startOne.v) { pf.startOne.v = it }
                    Div(pal)
                    SwitchRow(pal, "Extra turn on arrow squares", "Landing exactly on your arrow square, the last square before your home lane, gives another roll.", pf.arrowBonus.v) { pf.arrowBonus.v = it }
                    Div(pal)
                    SwitchRow(pal, "Kill needed to enter home", "Your home lane stays locked until you capture at least one opposing piece. If nobody can move for a long time, all lanes unlock.", pf.killLock.v) { pf.killLock.v = it }
                    Div(pal)
                    SwitchRow(pal, "Remove finished pieces with a 1", "A piece that reaches the final spot stays there until you roll a 1 to take it off the board. You win once all your pieces are removed.", pf.removeWith1.v) { pf.removeWith1.v = it }
                    Div(pal)
                    SwitchRow(pal, "Cancel 3rd 6 in a row", "Rolling three 6s in a row cancels the last move and passes your turn. When off, the dice never lands a third 6 in a row, so no turn is lost.", pf.cancel6.v) { pf.cancel6.v = it }
                    Div(pal)
                    SwitchRow(pal, "Pseudo-random dice", "Draws from a shuffled set of rolls, so streaks and droughts are rarer. When a roll could capture an opponent's piece, a capturing number comes up 55% of the time. Off: every roll is truly random.", pf.pseudo.v) { pf.pseudo.v = it }
                    Div(pal)
                    SwitchRow(pal, "Same-color stack is safe", "A stack of 2+ is safe until more opposing pieces gather on its square than the stack holds; then the whole stack is captured. Classic, 5 and 6 players.", pf.stack.v) { pf.stack.v = it }
                }
                Label(pal, "Pieces per player")
                Seg(pal, listOf("4", "5", "6"), pf.pieces.v - 4) { pf.pieces.v = it + 4 }
                Label(pal, "First player")
                ChipRow(pal, listOf("Random", "Red", "Green", "Yellow", "Blue"), pf.first.v + 1, { pf.first.v = it - 1 }) { k, tint ->
                    if (k > 0) Swatch(pal.p[k - 1], tint)
                }
                Label(pal, "Piece speed")
                Group(pal) { SliderRow(pal, "Speed", "${pf.speedPct.v}%", pf.speedPct.v, { pf.speedPct.v = it }, "Slow", "Fast") }
                Label(pal, "Gameplay")
                Group(pal) {
                    SwitchRow(pal, "Auto roll dice", "", pf.autoRoll.v) { pf.autoRoll.v = it }
                    Div(pal)
                    SwitchRow(pal, "Auto-move out of home", "When only unlocking moves are possible", pf.autoOut.v) { pf.autoOut.v = it }
                    Div(pal)
                    SwitchRow(pal, "Auto-move when only 1 option", "", pf.autoSingle.v) { pf.autoSingle.v = it }
                }
                Label(pal, "Feedback")
                Group(pal) {
                    SwitchRow(pal, "Sound effects", "", pf.sound.v) { pf.sound.v = it }
                    Div(pal)
                    SwitchRow(pal, "Vibration", "", pf.vib.v) { pf.vib.v = it }
                }
}

/** Full-screen settings opened from the pause menu. */
@Composable
fun SettingsSheet(pal: Pal, pf: Prefs, sysPal: List<Color>, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxSize().background(pal.bg)) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(pal.surf).clickable(onClick = onClose), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.size(18.dp)) {
                        drawPath(Path().apply {
                            moveTo(size.width * .65f, size.height * .1f); lineTo(size.width * .3f, size.height * .5f); lineTo(size.width * .65f, size.height * .9f)
                        }, pal.text, style = Stroke(size.width * .12f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text("Settings", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = pal.hi)
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp)) {
                Text("Rule changes apply from the next game.", color = pal.sub, fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp))
                SettingsList(pal, pf, sysPal)
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun GameDetails(g: Game, pal: Pal, mode: String) {
    Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(mode, color = pal.hi, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Text("${g.rolls.sum()} rolls · ${g.kills.sum()} captures", color = pal.sub, fontSize = 13.sp)
        }
        Spacer(Modifier.height(6.dp))
        g.active.forEach { p ->
            val home = (0 until g.ppp).count { g.pos[p * g.ppp + it] >= g.b.home }
            val base = (0 until g.ppp).count { g.pos[p * g.ppp + it] == -1 }
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(pal.p[p]))
                Spacer(Modifier.width(8.dp))
                Text(g.names[p] + if (g.turn == p) "  (turn)" else "", color = pal.text,
                    fontWeight = if (g.turn == p) FontWeight.Bold else FontWeight.Normal, maxLines = 1, modifier = Modifier.weight(1f))
                Text("$home home · ${g.ppp - home - base} out · $base in base", color = pal.sub, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun SliderRow(pal: Pal, title: String, valueText: String, value: Int, onChange: (Int) -> Unit, left: String, right: String) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontSize = 17.sp, modifier = Modifier.weight(1f))
            Text(valueText, color = pal.hi)
        }
        Slider(
            value = value.toFloat(), onValueChange = { onChange(it.toInt()) }, valueRange = 0f..100f,
            colors = SliderDefaults.colors(thumbColor = pal.acc, activeTrackColor = pal.acc, inactiveTrackColor = pal.text.copy(alpha = 0.15f))
        )
        Row {
            Text(left, color = pal.sub, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Text(right, color = pal.sub, fontSize = 12.sp)
        }
    }
}

private fun DrawScope.arrow(ctr: Offset, dir: Offset, r: Float, col: Color) {
    val len = dir.getDistance()
    if (len == 0f) return
    val d = Offset(dir.x / len, dir.y / len)
    val nrm = Offset(-d.y, d.x)
    val tip = ctr + d * r
    val b1 = ctr - d * (r * 0.7f) + nrm * (r * 0.8f)
    val b2 = ctr - d * (r * 0.7f) - nrm * (r * 0.8f)
    drawPath(Path().apply { moveTo(tip.x, tip.y); lineTo(b1.x, b1.y); lineTo(b2.x, b2.y); close() }, col)
}

private fun DrawScope.lock(ctr: Offset, r: Float, col: Color) {
    drawRoundRect(col, Offset(ctr.x - r * .55f, ctr.y - r * .05f), Size(r * 1.1f, r * .85f), CornerRadius(r * .15f))
    drawArc(col, 180f, 180f, false, Offset(ctr.x - r * .35f, ctr.y - r * .75f), Size(r * .7f, r * .9f), style = Stroke(r * .18f))
}
