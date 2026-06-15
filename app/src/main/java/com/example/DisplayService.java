package com.example;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import androidx.core.app.NotificationCompat;

public class DisplayService extends Service {

    private static final int NOTIFICATION_ID = 2026;
    private static final String CHANNEL_ID = "ItsEngineChannel";

    private WindowManager windowManager;

    // Elementos de Sobreposicao
    private DrawingOverlayView drawingOverlayView;
    private FrameLayout menuOverlayContainer;
    
    // Layout params
    private WindowManager.LayoutParams drawingParams;
    private WindowManager.LayoutParams menuParams;

    // Estados locais de animacao
    private float physicsTime = 0.0f;
    private float scannerOffset = 0.0f;
    private int scannerDirection = 1;
    private final Handler animationHandler = new Handler();

    private final Runnable animationTick = new Runnable() {
        @Override
        public void run() {
            // Incrementa o tempo fisico continuamente
            physicsTime += 0.035f;
            if (physicsTime > 6.28f) {
                physicsTime = 0.0f;
            }

            // Flutuacao do scanner cyberpunk
            scannerOffset += scannerDirection * 0.012f;
            if (scannerOffset > 1.0f) {
                scannerOffset = 1.0f;
                scannerDirection = -1;
            } else if (scannerOffset < 0.0f) {
                scannerOffset = 0.0f;
                scannerDirection = 1;
            }

            // Invalida a tela para desenhar o proximo quadro (fps ideal)
            if (drawingOverlayView != null) {
                drawingOverlayView.invalidate();
            }

            animationHandler.postDelayed(this, 18); // ~55 FPS
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        createNotificationChannel();
        Notification notification = createNotification();
        startForeground(NOTIFICATION_ID, notification);

        setupDrawingOverlay();
        setupMenuOverlay();

        // Inicia ticker de animacoes
        animationHandler.post(animationTick);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        animationHandler.removeCallbacks(animationTick);

        if (drawingOverlayView != null) {
            try {
                windowManager.removeView(drawingOverlayView);
            } catch (Exception e) {}
        }
        if (menuOverlayContainer != null) {
            try {
                windowManager.removeView(menuOverlayContainer);
            } catch (Exception e) {}
        }
        super.onDestroy();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "ItsEngine Foreground Overlays",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Indicador de atividade em segundo plano para HUD calibrador");
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("ItsEngine Ativo")
                .setContentText("O overlay de alinhamento está operacional")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();
    }

    // LAYER 1: TELA DE DESENHOS MATEMÁTICOS E FÍSICOS (Pass-Through de cliques)
    private void setupDrawingOverlay() {
        int layoutType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutType = WindowManager.LayoutParams.TYPE_PHONE;
        }

        drawingParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );
        drawingParams.gravity = Gravity.CENTER;

        drawingOverlayView = new DrawingOverlayView(this);
        try {
            windowManager.addView(drawingOverlayView, drawingParams);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // LAYER 2: MENU FLUTUANTE DE CONFIGURAÇÃO (Arrastável, minimizável)
    private void setupMenuOverlay() {
        int layoutType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutType = WindowManager.LayoutParams.TYPE_PHONE;
        }

        menuParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );
        menuParams.gravity = Gravity.TOP | Gravity.START;
        menuParams.x = 100;
        menuParams.y = 200;

        menuOverlayContainer = new FrameLayout(this);
        menuOverlayContainer.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        final MenuOverlayLayout panelLayout = new MenuOverlayLayout(this);
        menuOverlayContainer.addView(panelLayout);

        try {
            windowManager.addView(menuOverlayContainer, menuParams);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- SUB-CLASSE DE SUPORTE 1: CANVAS DE DESENHOS DE VETORES ---
    private class DrawingOverlayView extends View {

        private Paint clothPaint;
        private Paint railPaint;
        private Paint pocketBgPaint;
        private Paint pocketStrokePaint;
        private Paint ballPaint;
        private Paint shadowPaint;
        private Paint tracePaint;
        private Paint cueWhitePaint;
        private Paint reflexCyanPaint;
        private Paint targetBallPaint;
        private Paint ghostBallPaint;
        private Paint sweepLinePaint;
        private Paint sweepBandPaint;
        private Paint lineCuePaint;

        public DrawingOverlayView(Context context) {
            super(context);
            initPaints();
        }

        private void initPaints() {
            clothPaint = new Paint();
            clothPaint.setStyle(Paint.Style.FILL);
            clothPaint.setColor(0x3300FF88); // Transparente verde

            railPaint = new Paint();
            railPaint.setStyle(Paint.Style.STROKE);
            railPaint.setColor(0xFF00FFCC);
            railPaint.setStrokeWidth(4.0f);
            railPaint.setAntiAlias(true);

            pocketBgPaint = new Paint();
            pocketBgPaint.setStyle(Paint.Style.FILL);
            pocketBgPaint.setColor(Color.BLACK);

            pocketStrokePaint = new Paint();
            pocketStrokePaint.setStyle(Paint.Style.STROKE);
            pocketStrokePaint.setColor(0xFF00FFCC);
            pocketStrokePaint.setStrokeWidth(2.0f);
            pocketStrokePaint.setAntiAlias(true);

            ballPaint = new Paint();
            ballPaint.setStyle(Paint.Style.FILL);
            ballPaint.setColor(0xFFFFAA00); // Laranja para bola de calibracao
            ballPaint.setAntiAlias(true);

            shadowPaint = new Paint();
            shadowPaint.setStyle(Paint.Style.STROKE);
            shadowPaint.setColor(Color.WHITE);
            shadowPaint.setAlpha(100);
            shadowPaint.setStrokeWidth(2.0f);
            shadowPaint.setAntiAlias(true);

            tracePaint = new Paint();
            tracePaint.setStyle(Paint.Style.STROKE);
            tracePaint.setColor(0x80FFAA00);
            tracePaint.setStrokeWidth(3.0f);
            tracePaint.setPathEffect(new DashPathEffect(new float[]{10.0f, 10.0f}, 0.0f));
            tracePaint.setAntiAlias(true);

            cueWhitePaint = new Paint();
            cueWhitePaint.setStyle(Paint.Style.STROKE);
            cueWhitePaint.setColor(Color.WHITE);
            cueWhitePaint.setStrokeWidth(4.0f);
            cueWhitePaint.setAntiAlias(true);

            reflexCyanPaint = new Paint();
            reflexCyanPaint.setStyle(Paint.Style.STROKE);
            reflexCyanPaint.setColor(0xFF00FFCC);
            reflexCyanPaint.setStrokeWidth(3.0f);
            reflexCyanPaint.setPathEffect(new DashPathEffect(new float[]{15.0f, 10.0f}, 0.0f));
            reflexCyanPaint.setAntiAlias(true);

            targetBallPaint = new Paint();
            targetBallPaint.setStyle(Paint.Style.FILL);
            targetBallPaint.setColor(0xFFFF3366); // Rosa avermelhado
            targetBallPaint.setAntiAlias(true);

            ghostBallPaint = new Paint();
            ghostBallPaint.setStyle(Paint.Style.STROKE);
            ghostBallPaint.setColor(Color.WHITE);
            ghostBallPaint.setAlpha(180);
            ghostBallPaint.setStrokeWidth(3.0f);
            ghostBallPaint.setAntiAlias(true);

            sweepLinePaint = new Paint();
            sweepLinePaint.setStyle(Paint.Style.STROKE);
            sweepLinePaint.setColor(0xCC00FFCC);
            sweepLinePaint.setStrokeWidth(5.0f);
            sweepLinePaint.setAntiAlias(true);

            sweepBandPaint = new Paint();
            sweepBandPaint.setStyle(Paint.Style.FILL);
            sweepBandPaint.setColor(0x2200FFCC);

            lineCuePaint = new Paint();
            lineCuePaint.setStyle(Paint.Style.STROKE);
            lineCuePaint.setColor(Color.YELLOW);
            lineCuePaint.setStrokeWidth(3.0f);
            lineCuePaint.setPathEffect(new DashPathEffect(new float[]{12.0f, 8.0f}, 0.0f));
            lineCuePaint.setAntiAlias(true);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            
            float scrW = getWidth();
            float scrH = getHeight();

            if (scrW == 0 || scrH == 0) return;

            // Calcula geometria base da mesa virtual
            float baseTableW = scrW * 0.72f;
            float baseTableH = scrH * 0.44f;

            float tableX = (scrW / 2.0f) + OverlayConfig.tableXOffset;
            float tableY = (scrH / 2.0f) + OverlayConfig.tableYOffset;
            float tableW = baseTableW + (OverlayConfig.tableWidthScale * 2.0f);
            float tableH = baseTableH + (OverlayConfig.tableHeightScale * 2.0f);

            float rectX = tableX - (tableW / 2.0f);
            float rectY = tableY - (tableH / 2.0f);

            // 1. MESA VIRTUAL
            if (OverlayConfig.isMesaVirtualEnabled) {
                // Tecido interno
                canvas.drawRect(rectX, rectY, rectX + tableW, rectY + tableH, clothPaint);

                // Bordadura da tabela
                canvas.drawRect(rectX, rectY, rectX + tableW, rectY + tableH, railPaint);

                // 6 Caçapas
                float pocketRadius = 20.0f;
                float[][] pockets = {
                        {rectX, rectY},                           // Canto Superior Esquerdo
                        {tableX, rectY},                          // Superior Meio
                        {rectX + tableW, rectY},                  // Superior Direito
                        {rectX, rectY + tableH},                  // Canto Inferior Esquerdo
                        {tableX, rectY + tableH},                 // Inferior Meio
                        {rectX + tableW, rectY + tableH}          // Inferior Direito
                };

                for (int i = 0; i < pockets.length; i++) {
                    float px = pockets[i][0];
                    float py = pockets[i][1];
                    canvas.drawCircle(px, py, pocketRadius, pocketBgPaint);
                    canvas.drawCircle(px, py, pocketRadius, pocketStrokePaint);
                }
            }

            // 2. FÍSICA BOUNCING BALL COCOS SIMULAÇÃO
            if (OverlayConfig.isPhysicsEnabled && OverlayConfig.isMesaVirtualEnabled) {
                float ballRadius = 16.0f;

                // Formula senoidal de trajetoria de impacto continuo
                float ballA_X = tableX + (tableW / 2.5f) * (float) Math.cos(physicsTime);
                float ballA_Y = tableY + (tableH / 2.5f) * (float) Math.sin(physicsTime * 1.5f);

                // Desenha a bola sólida
                canvas.drawCircle(ballA_X, ballA_Y, ballRadius, ballPaint);

                // Desenha Ghost Ball projetada adjacente
                float ghostX = ballA_X + 45.0f * (float) Math.cos(physicsTime + 0.3f);
                float ghostY = ballA_Y + 45.0f * (float) Math.sin(physicsTime + 0.3f);
                canvas.drawCircle(ghostX, ghostY, ballRadius, shadowPaint);

                // Linha de trilha traseira de velocidade
                float tailX = ballA_X - 60.0f * (float) Math.cos(physicsTime);
                float tailY = ballA_Y - 60.0f * (float) Math.sin(physicsTime * 1.5f);
                canvas.drawLine(ballA_X, ballA_Y, tailX, tailY, tracePaint);

                // Impact indicator circle in red if close to table boundaries
                if (ballA_X >= rectX + tableW - 35.0f || ballA_X <= rectX + 35.0f) {
                    Paint borderImpactPaint = new Paint();
                    borderImpactPaint.setStyle(Paint.Style.STROKE);
                    borderImpactPaint.setColor(Color.RED);
                    borderImpactPaint.setStrokeWidth(3.0f);
                    borderImpactPaint.setAntiAlias(true);
                    canvas.drawCircle(ballA_X, ballA_Y, 28.0f, borderImpactPaint);
                }
            }

            // 3. ESP TRAJETÓRIAS (Linha de mira branca e canalizadora de rebote)
            if (OverlayConfig.isEspTrajetoriasEnabled) {
                float originX = tableX - (tableW / 4.0f);
                float originY = tableY + (tableH / 5.0f);
                float endX = rectX + tableW;
                float endY = rectY + (tableH * 0.2f);

                // Ponto inicial da mira branca
                canvas.drawCircle(originX, originY, 8.0f, cueWhitePaint);

                // Linha de Projeção central
                canvas.drawLine(originX, originY, endX, endY, cueWhitePaint);

                // Projetor de ricochete (verde-lime) apontando para a cacapa do canto inferior oposto
                canvas.drawLine(endX, endY, tableX, rectY + tableH, reflexCyanPaint);
            }

            // 4. ESP GHOST BALL
            if (OverlayConfig.isEspGhostBallEnabled) {
                float targetX = tableX + (tableW * 0.15f);
                float targetY = tableY - (tableH * 0.15f);

                // Desenha Bola sólida alvo (vermelha)
                canvas.drawCircle(targetX, targetY, 16.0f, targetBallPaint);

                // Fantasma de impacto (Círculo vazado branco)
                canvas.drawCircle(targetX - 30.0f, targetY + 12.0f, 16.0f, ghostBallPaint);

                // Indicadora pós colisão
                canvas.drawLine(targetX, targetY, targetX + 120.0f, targetY - 48.0f, targetBallPaint);
            }

            // 5. DETECÇÃO BITMAP AVANCE (Scanner de varredura Cyberpunk)
            if (OverlayConfig.isDeteccaoBitmapEnabled) {
                float sweepY = rectY + (tableH * scannerOffset);

                // Linha de Scan verde brilhante
                canvas.drawLine(rectX, sweepY, rectX + tableW, sweepY, sweepLinePaint);

                // Faixa semitransparente
                canvas.drawRect(rectX, sweepY - 15.0f, rectX + tableW, sweepY + 15.0f, sweepBandPaint);
            }

            // 6. ESP LINE CUE (4 Ricochetes preditivos perfeitos)
            if (OverlayConfig.isEspLineCueEnabled && OverlayConfig.isMesaVirtualEnabled) {
                float curX = tableX - tableW / 3.0f;
                float curY = tableY + tableH / 4.0f;
                float dX = 1.6f;
                float dY = -1.1f;

                Paint jointPaint = new Paint();
                jointPaint.setStyle(Paint.Style.FILL);
                jointPaint.setColor(Color.YELLOW);
                jointPaint.setAntiAlias(true);

                for (int bounce = 0; bounce < 4; bounce++) {
                    float nextBoundX = dX > 0 ? (rectX + tableW) : rectX;
                    float nextBoundY = dY > 0 ? (rectY + tableH) : rectY;

                    float distH = (nextBoundX - curX) / dX;
                    float distV = (nextBoundY - curY) / dY;
                    float step = Math.min(distH, distV);

                    float colX = curX + dX * step;
                    float colY = curY + dY * step;

                    // Linha amarela tracejada do rebate
                    canvas.drawLine(curX, curY, colX, colY, lineCuePaint);

                    // Circulo terminal indicador do rebate
                    canvas.drawCircle(colX, colY, 5.0f, jointPaint);

                    // Reversao de matriz fisica
                    if (distH < distV) {
                        dX = -dX;
                    } else {
                        dY = -dY;
                    }
                    curX = colX;
                    curY = colY;
                }
            }
        }
    }

    // --- SUB-CLASSE DE SUPORTE 2: MENU COM BOAO REDONDO MINIMIZADO E PAINEL EXPANDIDO ---
    private class MenuOverlayLayout extends FrameLayout {

        private boolean isExpanded = true;
        private int selectedTab = 0;

        // Containeres primários
        private LinearLayout bubbleLayout;
        private LinearLayout panelLayout;

        // Containeres dinâmicos de abas
        private LinearLayout tabsHeaderRow;
        private FrameLayout abaContentFrame;

        // Parametros para drag de tela
        private int initialX;
        private int initialY;
        private float initialTouchX;
        private float initialTouchY;
        private boolean isMoving = false;

        public MenuOverlayLayout(Context context) {
            super(context);
            setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            buildUI();
            refreshState();
        }

        private void buildUI() {
            // ---------------- MODO BUBBLE (MINIMIZADO) ----------------
            bubbleLayout = new LinearLayout(getContext());
            bubbleLayout.setGravity(Gravity.CENTER);
            FrameLayout.LayoutParams bubbleParams = new FrameLayout.LayoutParams(
                    dpToPx(54), dpToPx(54));
            bubbleLayout.setLayoutParams(bubbleParams);

            GradientDrawable bubbleGd = new GradientDrawable();
            bubbleGd.setShape(GradientDrawable.OVAL);
            bubbleGd.setColor(0xFF0F1E1B);
            bubbleGd.setStroke(dpToPx(2), 0xFF00FFCC);
            bubbleLayout.setBackground(bubbleGd);

            TextView gearSymbol = new TextView(getContext());
            gearSymbol.setText("⚙️");
            gearSymbol.setTextSize(24);
            gearSymbol.setGravity(Gravity.CENTER);
            gearSymbol.setTextColor(0xFF00FFCC);
            bubbleLayout.addView(gearSymbol);

            // Evento drag no botao de bubble
            bubbleLayout.setOnTouchListener(createDragTouchListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    isExpanded = true;
                    refreshState();
                }
            }));
            addView(bubbleLayout);

            // ---------------- MODO EXPANDIDO (PAINEL COMPLETO) ----------------
            panelLayout = new LinearLayout(getContext());
            panelLayout.setOrientation(LinearLayout.VERTICAL);
            FrameLayout.LayoutParams panelParams = new FrameLayout.LayoutParams(
                    dpToPx(310), FrameLayout.LayoutParams.WRAP_CONTENT);
            panelLayout.setLayoutParams(panelParams);
            panelLayout.setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));

            GradientDrawable panelGd = new GradientDrawable();
            panelGd.setColor(0xED0F0F13); // Semitransparente escuro
            panelGd.setCornerRadius(dpToPx(16));
            panelGd.setStroke(dpToPx(1), 0xFF1E1E24);
            panelLayout.setBackground(panelGd);

            // CABEÇALHO DO MENU (Touch To Drag)
            LinearLayout topHeaderRow = new LinearLayout(getContext());
            topHeaderRow.setOrientation(LinearLayout.HORIZONTAL);
            topHeaderRow.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams tHeaderParam = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            topHeaderRow.setLayoutParams(tHeaderParam);
            topHeaderRow.setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10));
            
            GradientDrawable headerBg = new GradientDrawable();
            headerBg.setColor(0xFF0E0E11);
            headerBg.setCornerRadius(dpToPx(12));
            topHeaderRow.setBackground(headerBg);

            // Indicador de Status Verde
            View lightDot = new View(getContext());
            LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(
                    dpToPx(8), dpToPx(8));
            dotParams.setMargins(0, 0, dpToPx(8), 0);
            lightDot.setLayoutParams(dotParams);
            GradientDrawable dotGd = new GradientDrawable();
            dotGd.setShape(GradientDrawable.OVAL);
            dotGd.setColor(0xFF00FFCC);
            lightDot.setBackground(dotGd);
            topHeaderRow.addView(lightDot);

            TextView pTitleView = new TextView(getContext());
            pTitleView.setText("ItsEngine - HUD VIP");
            pTitleView.setTextSize(12);
            pTitleView.setTextColor(Color.WHITE);
            pTitleView.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
            pTitleView.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
            topHeaderRow.addView(pTitleView);

            // Botão Minimizar
            TextView minimizeBtn = new TextView(getContext());
            minimizeBtn.setText("🛠️");
            minimizeBtn.setTextSize(14);
            minimizeBtn.setTextColor(Color.LTGRAY);
            minimizeBtn.setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
            minimizeBtn.setClickable(true);
            minimizeBtn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    isExpanded = false;
                    refreshState();
                }
            });
            topHeaderRow.addView(minimizeBtn);

            // Adiciona Handler de Arraste de Tela no cabecalho
            topHeaderRow.setOnTouchListener(createDragTouchListener(null));

            panelLayout.addView(topHeaderRow);

            // SUB ABAS DE SISTEMA (3 BOTOES COM ABAS)
            tabsHeaderRow = new LinearLayout(getContext());
            tabsHeaderRow.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams tabsHeaderParam = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dpToPx(38));
            tabsHeaderParam.setMargins(0, dpToPx(4), 0, dpToPx(4));
            tabsHeaderRow.setLayoutParams(tabsHeaderParam);
            tabsHeaderRow.setBackgroundColor(0xFF09090C);

            tabsHeaderRow.addView(buildTabButton("Aba 1", 0));
            tabsHeaderRow.addView(buildTabButton("Aba 2", 1));
            tabsHeaderRow.addView(buildTabButton("Aba 3", 2));

            panelLayout.addView(tabsHeaderRow);

            // CONTAINER DA EXPANSÃO DO CONTEÚDO DAS ABAS
            abaContentFrame = new FrameLayout(getContext());
            abaContentFrame.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            panelLayout.addView(abaContentFrame);

            addView(panelLayout);
        }

        private View buildTabButton(String label, final int tabIndex) {
            final Button tabBtn = new Button(getContext());
            tabBtn.setText(label);
            tabBtn.setTextSize(11);
            tabBtn.setTextColor(selectedTab == tabIndex ? 0xFF00FFCC : Color.GRAY);
            tabBtn.setBackgroundColor(Color.TRANSPARENT);
            tabBtn.setTypeface(null, Typeface.BOLD);
            tabBtn.setPadding(0, 0, 0, 0);

            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.MATCH_PARENT, 1.0f);
            tabBtn.setLayoutParams(btnParams);
            tabBtn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedTab = tabIndex;
                    updateTabColors();
                    updateAbaContent();
                }
            });
            return tabBtn;
        }

        private void updateTabColors() {
            for (int i = 0; i < tabsHeaderRow.getChildCount(); i++) {
                Button btn = (Button) tabsHeaderRow.getChildAt(i);
                btn.setTextColor(selectedTab == i ? 0xFF00FFCC : Color.GRAY);
            }
        }

        private void updateAbaContent() {
            abaContentFrame.removeAllViews();

            ScrollView contentScroll = new ScrollView(getContext());
            contentScroll.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dpToPx(240))); // Limit height and scroll

            LinearLayout container = new LinearLayout(getContext());
            container.setOrientation(LinearLayout.VERTICAL);
            container.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
            container.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT));

            if (selectedTab == 0) {
                // --- ABA 1 ---
                container.addView(buildToggleRow("Mesa Virtual", OverlayConfig.isMesaVirtualEnabled, new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        OverlayConfig.isMesaVirtualEnabled = isChecked;
                        updateAbaContent(); // Re-render to show/hide sliders
                    }
                }));

                container.addView(buildToggleRow("Fisica (Fisycs)", OverlayConfig.isPhysicsEnabled, new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        OverlayConfig.isPhysicsEnabled = isChecked;
                    }
                }));

                if (OverlayConfig.isMesaVirtualEnabled) {
                    // Divider
                    View divider = new View(getContext());
                    divider.setBackgroundColor(Color.DKGRAY);
                    LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1));
                    divParams.setMargins(0, dpToPx(8), 0, dpToPx(8));
                    divider.setLayoutParams(divParams);
                    container.addView(divider);

                    TextView sliderTitle = new TextView(getContext());
                    sliderTitle.setText("Movimente Mesa Virtual");
                    sliderTitle.setTextColor(0xFF00FFCC);
                    sliderTitle.setTextSize(11);
                    sliderTitle.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
                    sliderTitle.setPadding(0, dpToPx(4), 0, dpToPx(8));
                    container.addView(sliderTitle);

                    // 4 Seekbars de calibracao fina
                    container.addView(buildSliderItem("Posicionar X", OverlayConfig.tableXOffset, -300, 300, new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                            OverlayConfig.tableXOffset = progress;
                        }
                        @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                        @Override public void onStopTrackingTouch(SeekBar seekBar) {}
                    }));

                    container.addView(buildSliderItem("Posicionar Y", OverlayConfig.tableYOffset, -300, 300, new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                            OverlayConfig.tableYOffset = progress;
                        }
                        @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                        @Override public void onStopTrackingTouch(SeekBar seekBar) {}
                    }));

                    container.addView(buildSliderItem("Largura (Width)", OverlayConfig.tableWidthScale, -200, 200, new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                            OverlayConfig.tableWidthScale = progress;
                        }
                        @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                        @Override public void onStopTrackingTouch(SeekBar seekBar) {}
                    }));

                    container.addView(buildSliderItem("Altura (Height)", OverlayConfig.tableHeightScale, -200, 200, new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                            OverlayConfig.tableHeightScale = progress;
                        }
                        @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                        @Override public void onStopTrackingTouch(SeekBar seekBar) {}
                    }));
                }
            } else if (selectedTab == 1) {
                // --- ABA 2 ---
                container.addView(buildToggleRow("ESP Trajetórias", OverlayConfig.isEspTrajetoriasEnabled, new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        OverlayConfig.isEspTrajetoriasEnabled = isChecked;
                    }
                }));

                container.addView(buildToggleRow("ESP Ghost Ball", OverlayConfig.isEspGhostBallEnabled, new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        OverlayConfig.isEspGhostBallEnabled = isChecked;
                    }
                }));
            } else if (selectedTab == 2) {
                // --- ABA 3 ---
                container.addView(buildToggleRow("Detecção BitMap Avance", OverlayConfig.isDeteccaoBitmapEnabled, new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        OverlayConfig.isDeteccaoBitmapEnabled = isChecked;
                    }
                }));

                container.addView(buildToggleRow("ESP Line Cue", OverlayConfig.isEspLineCueEnabled, new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        OverlayConfig.isEspLineCueEnabled = isChecked;
                    }
                }));

                // Botão fechar serviço
                Button closeBtn = new Button(getContext());
                closeBtn.setText("Fechar Menu");
                closeBtn.setTextColor(Color.WHITE);
                closeBtn.setTypeface(null, Typeface.BOLD);
                closeBtn.setTextSize(11);
                
                GradientDrawable closeGd = new GradientDrawable();
                closeGd.setColor(0xFFFF3355);
                closeGd.setCornerRadius(dpToPx(8));
                closeBtn.setBackground(closeGd);

                LinearLayout.LayoutParams closeParam = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(38));
                closeParam.setMargins(0, dpToPx(16), 0, dpToPx(8));
                closeBtn.setLayoutParams(closeParam);
                closeBtn.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        OverlayConfig.isItsEngineActive = false;
                        OverlayConfig.isAimPoolActive = false;
                        stopSelf();
                    }
                });
                container.addView(closeBtn);
            }

            contentScroll.addView(container);
            abaContentFrame.addView(contentScroll);
        }

        private View buildToggleRow(String label, boolean initialChecked, CompoundButton.OnCheckedChangeListener listener) {
            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
            row.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));

            TextView textLabel = new TextView(getContext());
            textLabel.setText(label);
            textLabel.setTextColor(Color.WHITE);
            textLabel.setTextSize(12);
            textLabel.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
            row.addView(textLabel);

            Switch sw = new Switch(getContext());
            sw.setChecked(initialChecked);
            sw.setOnCheckedChangeListener(listener);
            
            LinearLayout.LayoutParams swParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            sw.setLayoutParams(swParams);
            row.addView(sw);

            return row;
        }

        private View buildSliderItem(final String label, float currentValue, final int min, final int max, final SeekBar.OnSeekBarChangeListener listener) {
            LinearLayout sliderBlock = new LinearLayout(getContext());
            sliderBlock.setOrientation(LinearLayout.VERTICAL);
            sliderBlock.setPadding(0, dpToPx(4), 0, dpToPx(4));
            sliderBlock.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));

            LinearLayout headerRow = new LinearLayout(getContext());
            headerRow.setOrientation(LinearLayout.HORIZONTAL);
            headerRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView labelView = new TextView(getContext());
            labelView.setText(label);
            labelView.setTextColor(Color.GRAY);
            labelView.setTextSize(10);
            labelView.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
            headerRow.addView(labelView);

            final TextView valView = new TextView(getContext());
            valView.setText((int)currentValue + "px");
            valView.setTextColor(0xFF00FFCC);
            valView.setTextSize(10);
            headerRow.addView(valView);

            sliderBlock.addView(headerRow);

            SeekBar bar = new SeekBar(getContext());
            bar.setMax(max - min);
            bar.setProgress((int)currentValue - min);
            
            SeekBar.OnSeekBarChangeListener wrapperListener = new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    int realVal = progress + min;
                    valView.setText(realVal + "px");
                    if (listener != null) {
                        listener.onProgressChanged(seekBar, realVal, fromUser);
                    }
                }
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    if (listener != null) listener.onStartTrackingTouch(seekBar);
                }
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    if (listener != null) listener.onStopTrackingTouch(seekBar);
                }
            };
            
            bar.setOnSeekBarChangeListener(wrapperListener);
            sliderBlock.addView(bar);

            return sliderBlock;
        }

        private void refreshState() {
            if (isExpanded) {
                bubbleLayout.setVisibility(View.GONE);
                panelLayout.setVisibility(View.VISIBLE);
                updateAbaContent();
            } else {
                bubbleLayout.setVisibility(View.VISIBLE);
                panelLayout.setVisibility(View.GONE);
            }
        }

        // UNIFICADOR DE ARRASTE E CLICKE DE SOBREPOSIÇÃO MODERNO (OLD-SCHOOL JAVA 7 STYLE)
        private OnTouchListener createDragTouchListener(final OnClickListener clickListener) {
            return new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            initialX = menuParams.x;
                            initialY = menuParams.y;
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();
                            isMoving = false;
                            return true;
                        case MotionEvent.ACTION_MOVE:
                            int dx = (int) (event.getRawX() - initialTouchX);
                            int dy = (int) (event.getRawY() - initialTouchY);
                            
                            // Tolerancia pequena para diferenciar arraste de clique rápido
                            if (Math.abs(dx) > 12 || Math.abs(dy) > 12) {
                                isMoving = true;
                                menuParams.x = initialX + dx;
                                menuParams.y = initialY + dy;
                                try {
                                    windowManager.updateViewLayout(menuOverlayContainer, menuParams);
                                } catch (Exception e) {}
                            }
                            return true;
                        case MotionEvent.ACTION_UP:
                            if (!isMoving) {
                                if (clickListener != null) {
                                    clickListener.onClick(v);
                                } else {
                                    v.performClick();
                                }
                            }
                            return true;
                    }
                    return false;
                }
            };
        }
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }
}
