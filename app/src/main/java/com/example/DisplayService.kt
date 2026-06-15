package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlin.math.cos
import kotlin.math.sin

class DisplayService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    // Gerenciamento de Ciclo de Vida do Compose no Serviço
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry = savedStateRegistryController.savedStateRegistry

    private lateinit var windowManager: WindowManager

    // Windows Overlays privadas
    private var menuComposeView: ComposeView? = null
    private var drawingComposeView: ComposeView? = null

    private lateinit var menuParams: WindowManager.LayoutParams
    private lateinit var drawingParams: WindowManager.LayoutParams

    companion object {
        private const val NOTIFICATION_ID = 2026
        private const val CHANNEL_ID = "ItsEngineChannel"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.STARTED

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        createNotificationChannel()
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        setupDrawingOverlay()
        setupMenuOverlay()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        return START_STICKY
    }

    override fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        
        // Remove as views de sobreposição com segurança
        menuComposeView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) {}
        }
        drawingComposeView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) {}
        }
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ItsEngine Foreground Overlays",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Indicador de atividade em segundo plano para HUD calibrador"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ItsEngine Ativo")
            .setContentText("O overlay de alinhamento está operacional")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }

    // LAYER 1: Tela de Desenhos Invisíveis (Pass-Through de Clique)
    private fun setupDrawingOverlay() {
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        drawingParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        drawingComposeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@DisplayService)
            setViewTreeSavedStateRegistryOwner(this@DisplayService)
            setContent {
                DrawingCanvasOverlay()
            }
        }

        try {
            windowManager.addView(drawingComposeView, drawingParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // LAYER 2: Painel do Menu Flutuante (Arrastável, minimizável)
    private fun setupMenuOverlay() {
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        menuParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 200
        }

        menuComposeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@DisplayService)
            setViewTreeSavedStateRegistryOwner(this@DisplayService)
            setContent {
                FloatingMenuContent(
                    onDrag = { dx, dy ->
                        menuParams.x += dx.toInt()
                        menuParams.y += dy.toInt()
                        try {
                            windowManager.updateViewLayout(this, menuParams)
                        } catch (e: Exception) {
                            // Ignora eventuais problemas de concorrência descartando atualizações atrasadas
                        }
                    },
                    onCloseService = {
                        OverlayConfig.isItsEngineActive = false
                        OverlayConfig.isAimPoolActive = false
                        stopSelf()
                    }
                )
            }
        }

        try {
            windowManager.addView(menuComposeView, menuParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

// COMPOSABLE 1: Renderiza o overlay matemático de física, vetores e calibração
@Composable
fun DrawingCanvasOverlay() {
    // Animação contínua para simular pulso do scanner e física correndo
    val infiniteTransition = rememberInfiniteTransition(label = "Radar lines")
    val scannerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Scanner Sweep"
    )

    val physicsTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Physics Pulse"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val scrW = size.width
        val scrH = size.height

        // Define a Mesa Virtual Centralizada modificada pelos sliders de calibração
        val baseTableW = scrW * 0.72f
        val baseTableH = scrH * 0.44f

        val tableX = (scrW / 2) + OverlayConfig.tableXOffset
        val tableY = (scrH / 2) + OverlayConfig.tableYOffset
        val tableW = baseTableW + (OverlayConfig.tableWidthScale * 2)
        val tableH = baseTableH + (OverlayConfig.tableHeightScale * 2)

        val rectX = tableX - (tableW / 2)
        val rectY = tableY - (tableH / 2)

        // 1: Mesa Virtual (Desenha o retângulo verde transparente com as caçapas)
        if (OverlayConfig.isMesaVirtualEnabled) {
            val railColor = Color(0xFF00FFCC)
            val clothColor = Color(0x3300FF88)

            // Espaço interno do feltro virtual
            drawRect(
                color = clothColor,
                topLeft = Offset(rectX, rectY),
                size = Size(tableW, tableH)
            )

            // Trilhas externas (bordas da mesa)
            drawRect(
                color = railColor,
                topLeft = Offset(rectX, rectY),
                size = Size(tableW, tableH),
                style = Stroke(width = 4f)
            )

            // Caçapas (Pockets) - 6 Caçapas regulamentares
            val pocketRadius = 20f
            val pockets = listOf(
                Offset(rectX, rectY), // Canto Superior Esquerdo
                Offset(tableX, rectY), // Superior Meio
                Offset(rectX + tableW, rectY), // Superior Direito
                Offset(rectX, rectY + tableH), // Canto Inferior Esquerdo
                Offset(tableX, rectY + tableH), // Inferior Meio
                Offset(rectX + tableW, rectY + tableH) // Inferior Direito
            )

            for (p in pockets) {
                drawCircle(
                    color = Color.Black,
                    radius = pocketRadius,
                    center = p
                )
                drawCircle(
                    color = Color(0xFF00FFCC),
                    radius = pocketRadius,
                    center = p,
                    style = Stroke(width = 2f)
                )
            }
        }

        // 2: Física (Desenha bolhas físicas ricocheteando continuamente)
        if (OverlayConfig.isPhysicsEnabled && OverlayConfig.isMesaVirtualEnabled) {
            val ballRadius = 16f
            // Círculos virtuais simulando ricochete perfeito
            // Calcula uma rota usando seno e cosseno no tempo da animação
            val ballA_X = tableX + (tableW / 2.5f) * cos(physicsTime)
            val ballA_Y = tableY + (tableH / 2.5f) * sin(physicsTime * 1.5f)

            // Desenha a bola virtual A
            drawCircle(
                color = Color(0xFFFFAA00),
                radius = ballRadius,
                center = Offset(ballA_X, ballA_Y)
            )
            // Desenha a sombra da "Ghost-Ball" de colisão
            drawCircle(
                color = Color.White.copy(alpha = 0.4f),
                radius = ballRadius,
                center = Offset(ballA_X + 45f * cos(physicsTime + 0.3f), ballA_Y + 45f * sin(physicsTime + 0.3f)),
                style = Stroke(width = 2f)
            )

            // Trilha de velocidade da física
            drawLine(
                color = Color(0xFFFFAA00).copy(alpha = 0.5f),
                start = Offset(ballA_X, ballA_Y),
                end = Offset(ballA_X - 60f * cos(physicsTime), ballA_Y - 60f * sin(physicsTime * 1.5f)),
                strokeWidth = 3f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
            )

            // Desenha vetores de impacto nas bordas
            if (ballA_X >= rectX + tableW - 35f || ballA_X <= rectX + 35f) {
                drawCircle(
                    color = Color.Red,
                    radius = 28f,
                    center = Offset(ballA_X, ballA_Y),
                    style = Stroke(width = 3f)
                )
            }
        }

        // 3: ESP Trajetórias (Linha guia infinita)
        if (OverlayConfig.isEspTrajetoriasEnabled) {
            // Desenha uma linha de projeção infinita baseada no centro da mesa
            val originX = tableX - (tableW / 4f)
            val originY = tableY + (tableH / 5f)
            val endX = rectX + tableW
            val endY = rectY + (tableH * 0.2f)

            // Desenha ponto de mira
            drawCircle(color = Color.White, radius = 8f, center = Offset(originX, originY))

            // Projeção extendida do taco (Branca)
            drawLine(
                color = Color.White,
                start = Offset(originX, originY),
                end = Offset(endX, endY),
                strokeWidth = 4f
            )

            // Projeção do ricochete
            drawLine(
                color = Color(0xFF00FFCC),
                start = Offset(endX, endY),
                end = Offset(tableX, rectY + tableH),
                strokeWidth = 3f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f))
            )
        }

        // 4: ESP Ghost Ball
        if (OverlayConfig.isEspGhostBallEnabled) {
            val targetX = tableX + (tableW * 0.15f)
            val targetY = tableY - (tableH * 0.15f)

            // Bola alvo sólida
            drawCircle(
                color = Color(0xFFFF3366),
                radius = 16f,
                center = Offset(targetX, targetY)
            )

            // Fantasma adjacente onde o impacto ocorre
            drawCircle(
                color = Color.White.copy(alpha = 0.7f),
                radius = 16f,
                center = Offset(targetX - 30f, targetY + 12f),
                style = Stroke(width = 3f)
            )

            // Desenha linha indicadora separada pós impacto
            drawLine(
                color = Color(0xFFFF5588),
                start = Offset(targetX, targetY),
                end = Offset(targetX + 120f, targetY - 48f),
                strokeWidth = 3f
            )
        }

        // 5: Detecção BitMap Avance (Scanner Cyberpunk)
        if (OverlayConfig.isDeteccaoBitmapEnabled) {
            val sweepY = rectY + (tableH * scannerOffset)
            
            // Desenha a linha verde brilhante horizontal de varredura
            drawLine(
                color = Color(0xFF00FFCC).copy(alpha = 0.8f),
                start = Offset(rectX, sweepY),
                end = Offset(rectX + tableW, sweepY),
                strokeWidth = 5f
            )

            // Desenha blocos simulados de leitura binária
            drawRect(
                color = Color(0x2200FFCC),
                topLeft = Offset(rectX, sweepY - 15f),
                size = Size(tableW, 30f)
            )
        }

        // 6: ESP Line Cue (3 Ricochetes automáticos na mesa virtual)
        if (OverlayConfig.isEspLineCueEnabled && OverlayConfig.isMesaVirtualEnabled) {
            // Origem do taco
            var currentPos = Offset(tableX - tableW / 3f, tableY + tableH / 4f)
            // Vetor direção
            var dX = 1.6f
            var dY = -1.1f

            // Desenha os 3 saltos da bola calculando colisões perfeitas na matriz do retângulo
            for (bounce in 1..4) {
                // Simula projeção até atingir as bordas rectX e rectY
                val nextX = if (dX > 0) rectX + tableW else rectX
                val nextY = if (dY > 0) rectY + tableH else rectY

                // Calcula qual colisão ocorre primeiro (Cushion horizontal ou vertical)
                val distH = (nextX - currentPos.x) / dX
                val distV = (nextY - currentPos.y) / dY
                val step = minOf(distH, distV)

                val colX = currentPos.x + dX * step
                val colY = currentPos.y + dY * step

                // Desenha o segmento de rebate
                drawLine(
                    color = Color.Yellow.copy(alpha = 0.9f),
                    start = currentPos,
                    end = Offset(colX, colY),
                    strokeWidth = 3f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                )

                // Desenha circulo indicador de colisão
                drawCircle(
                    color = Color.Yellow,
                    radius = 5f,
                    center = Offset(colX, colY)
                )

                // Aplica a física do rebote invertendo eixos
                if (distH < distV) {
                    dX = -dX
                } else {
                    dY = -dY
                }
                currentPos = Offset(colX, colY)
            }
        }
    }
}

// COMPOSABLE 2: Menu Principal Flutuante (Arrastável pelas bordas/cabeçalho)
@Composable
fun FloatingMenuContent(
    onDrag: (Float, Float) -> Unit,
    onCloseService: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf(0) }

    if (!isExpanded) {
        // MODO BUBBLE (Minimizado a um pequeno ícone flutuante verde redondo)
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFF0F1E1B), Color.Black)))
                .border(2.dp, Color(0xFF00FFCC), CircleShape)
                .clickable { isExpanded = true }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.x, dragAmount.y)
                    }
                }
                .testTag("floating_bubble"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Expandir Menu",
                tint = Color(0xFF00FFCC),
                modifier = Modifier.size(24.dp)
            )
        }
    } else {
        // EXPANDIDO: Menu Completo Elegante
        Card(
            modifier = Modifier
                .width(310.dp)
                .padding(4.dp)
                .border(1.dp, Color(0xFF1E1E24), RoundedCornerShape(16.dp))
                .shadow(12.dp, RoundedCornerShape(16.dp))
                .testTag("floating_panel"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFDF0F0F13) // Levemente transparente
            )
        ) {
            Column {
                // CABEÇALHO DO PAINEL (Touch-to-Drag)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0E0E11))
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                onDrag(dragAmount.x, dragAmount.y)
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00FFCC))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ItsEngine - HUD VIP",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Row {
                        // Minimiza para bolha
                        IconButton(
                            onClick = { isExpanded = false },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Build, // Representa minimizar/recolher
                                contentDescription = "Minimizar",
                                tint = Color.LightGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // SUB ABA: 3 TABULADORES (ABA 1, ABA 2, ABA 3)
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF09090C),
                    contentColor = Color(0xFF00FFCC),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Aba 1", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Aba 2", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Aba 3", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                // CONTEÚDO DINÂMICO DE ABAS
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
                            // --- ABA 1 ---
                            FloatRowToggle(
                                label = "Mesa Virtual",
                                checked = OverlayConfig.isMesaVirtualEnabled,
                                onCheckedChange = { OverlayConfig.isMesaVirtualEnabled = it }
                            )

                            FloatRowToggle(
                                label = "Fisica (Fisycs)",
                                checked = OverlayConfig.isPhysicsEnabled,
                                onCheckedChange = { OverlayConfig.isPhysicsEnabled = it }
                            )

                            if (OverlayConfig.isMesaVirtualEnabled) {
                                Divider(color = Color.DarkGray, thickness = 0.5.dp)
                                Text(
                                    text = "Movimente Mesa Virtual",
                                    fontSize = 11.sp,
                                    color = Color(0xFF00FFCC),
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(top = 4.dp)
                                )

                                // Sliders de Ajuste Fino de Layout (4 SeekBars)
                                FloatSliderItem(
                                    label = "Posicionar X",
                                    value = OverlayConfig.tableXOffset,
                                    valueRange = -300f..300f,
                                    onValueChange = { OverlayConfig.tableXOffset = it }
                                )
                                FloatSliderItem(
                                    label = "Posicionar Y",
                                    value = OverlayConfig.tableYOffset,
                                    valueRange = -300f..300f,
                                    onValueChange = { OverlayConfig.tableYOffset = it }
                                )
                                FloatSliderItem(
                                    label = "Largura (Width)",
                                    value = OverlayConfig.tableWidthScale,
                                    valueRange = -200f..200f,
                                    onValueChange = { OverlayConfig.tableWidthScale = it }
                                )
                                FloatSliderItem(
                                    label = "Altura (Height)",
                                    value = OverlayConfig.tableHeightScale,
                                    valueRange = -200f..200f,
                                    onValueChange = { OverlayConfig.tableHeightScale = it }
                                )
                            }
                        }
                        1 -> {
                            // --- ABA 2 ---
                            FloatRowToggle(
                                label = "ESP Trajetórias",
                                checked = OverlayConfig.isEspTrajetoriasEnabled,
                                onCheckedChange = { OverlayConfig.isEspTrajetoriasEnabled = it }
                            )

                            FloatRowToggle(
                                label = "ESP Ghost Ball",
                                checked = OverlayConfig.isEspGhostBallEnabled,
                                onCheckedChange = { OverlayConfig.isEspGhostBallEnabled = it }
                            )
                        }
                        2 -> {
                            // --- ABA 3 ---
                            FloatRowToggle(
                                label = "Detecção BitMap Avance",
                                checked = OverlayConfig.isDeteccaoBitmapEnabled,
                                onCheckedChange = { OverlayConfig.isDeteccaoBitmapEnabled = it }
                            )

                            FloatRowToggle(
                                label = "ESP Line Cue",
                                checked = OverlayConfig.isEspLineCueEnabled,
                                onCheckedChange = { OverlayConfig.isEspLineCueEnabled = it }
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Botão Fechar Menu completamente
                            Button(
                                onClick = onCloseService,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF3355)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp)
                                    .testTag("close_menu_button")
                                    .align(Alignment.CenterHorizontally)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Fechar Menu",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Fechar Menu",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FloatRowToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = Color(0xFF00FFCC),
                uncheckedThumbColor = Color.DarkGray,
                uncheckedTrackColor = Color(0xFF222226)
            ),
            modifier = Modifier.scale(0.82f)
        )
    }
}

// Escala de Switches automática pelo import

@Composable
fun FloatSliderItem(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontSize = 10.sp, color = Color.Gray)
            Text(text = "${value.toInt()}px", fontSize = 10.sp, color = Color(0xFF00FFCC))
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF00FFCC),
                activeTrackColor = Color(0xFF00FFCC),
                inactiveTrackColor = Color.DarkGray
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
        )
    }
}
