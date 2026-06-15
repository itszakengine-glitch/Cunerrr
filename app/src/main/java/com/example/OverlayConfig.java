package com.example;

public class OverlayConfig {
    // Estado global das permissões e ativação das libs do menu principal
    public static boolean isCocosPhysicsActive = false;
    public static boolean isAimPoolActive = false;
    public static boolean isItsEngineActive = false;

    // ABA 1: Ajuste de Layout & Mesa Virtual
    public static boolean isMesaVirtualEnabled = false;
    public static boolean isPhysicsEnabled = false;
    
    // Sliders de Calibração (SeekBars) da Mesa Virtual (DP/Pixel offsets)
    public static float tableXOffset = 0f;      // Direita / Esquerda (-300 a 300)
    public static float tableYOffset = 0f;      // Cima / Baixo (-300 a 300)
    public static float tableWidthScale = 0f;   // Ajustar Largura (-200 a 200)
    public static float tableHeightScale = 0f;  // Ajustar Altura (-200 a 200)

    // ABA 2: ESP Desenhos do taco e bola branca
    public static boolean isEspTrajetoriasEnabled = false;
    public static boolean isEspGhostBallEnabled = false;

    // ABA 3: Análise profunda de pixel & detecção por MediaProjection simulada
    public static boolean isDeteccaoBitmapEnabled = false;
    public static boolean isEspLineCueEnabled = false;
}
