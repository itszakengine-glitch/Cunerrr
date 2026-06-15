package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object OverlayConfig {
    // Estado global das permissões e ativação das libs do menu principal
    var isCocosPhysicsActive by mutableStateOf(false)
    var isAimPoolActive by mutableStateOf(false)
    var isItsEngineActive by mutableStateOf(false)

    // ABA 1: Ajuste de Layout & Mesa Virtual
    var isMesaVirtualEnabled by mutableStateOf(false)
    var isPhysicsEnabled by mutableStateOf(false)
    
    // Sliders de Calibração (SeekBars) da Mesa Virtual (DP/Pixel offsets)
    var tableXOffset by mutableStateOf(0f)      // Direita / Esquerda (-300 a 300)
    var tableYOffset by mutableStateOf(0f)      // Cima / Baixo (-300 a 300)
    var tableWidthScale by mutableStateOf(0f)   // Ajustar Largura (-200 a 200)
    var tableHeightScale by mutableStateOf(0f)  // Ajustar Altura (-200 a 200)

    // ABA 2: ESP Desenhos do taco e bola branca
    var isEspTrajetoriasEnabled by mutableStateOf(false)
    var isEspGhostBallEnabled by mutableStateOf(false)

    // ABA 3: Análise profunda de pixel & detecção por MediaProjection simulada
    var isDeteccaoBitmapEnabled by mutableStateOf(false)
    var isEspLineCueEnabled by mutableStateOf(false)
}
