package com.example;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class HomeActivity extends Activity {

    private FrameLayout rootLayout;
    private ScrollView mainScrollView;
    private LinearLayout mainContentContainer;

    // Blocos da UI
    private LinearLayout gameIconContainer;
    private LinearLayout warningCard;
    private Button startButton;
    private LinearLayout activationPanel;
    private TextView bottomStatusText;

    // Switches
    private Switch switchCocos;
    private Switch switchAimPool;
    private Switch switchItsEngine;

    private boolean isOverlayPermissionGranted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Edge-to-Edge Fullscreen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        rootLayout = new FrameLayout(this);
        rootLayout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        rootLayout.setBackgroundColor(0xFF070708); // Dark black/blue

        buildUI();
        setContentView(rootLayout);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAndRefreshPermissions();
    }

    private void checkAndRefreshPermissions() {
        isOverlayPermissionGranted = Settings.canDrawOverlays(this);
        
        if (isOverlayPermissionGranted) {
            // Mudanças para quando tem permissão
            startButton.setText("SISTEMA SEGURO [ START ]");
            
            GradientDrawable btnOkGd = new GradientDrawable();
            btnOkGd.setColor(0xFF00FFCC);
            btnOkGd.setCornerRadius(dpToPx(12));
            startButton.setBackground(btnOkGd);
            startButton.setTextColor(Color.BLACK);

            activationPanel.setVisibility(View.VISIBLE);
            bottomStatusText.setText("Overlay e Injeção JNI prontos para uso!");
            bottomStatusText.setTextColor(0xFF00FFCC);

            // Restaura o estado anterior dos switches da configuração global
            switchCocos.setChecked(OverlayConfig.isCocosPhysicsActive);
            switchAimPool.setChecked(OverlayConfig.isAimPoolActive);
            switchItsEngine.setChecked(OverlayConfig.isItsEngineActive);

            // Inicializa ciclo de vida do serviço overlay seguro
            updateServiceLifecycle();
        } else {
            // Mudanças para quando falta permissão
            startButton.setText("CONCEDER PERMISSÃO DE SOBREPOR [ START ]");
            
            GradientDrawable btnNoGd = new GradientDrawable();
            btnNoGd.setColor(0xFF333333);
            btnNoGd.setCornerRadius(dpToPx(12));
            startButton.setBackground(btnNoGd);
            startButton.setTextColor(0xFF888888);

            activationPanel.setVisibility(View.GONE);
            bottomStatusText.setText("Aguardando ativação da sobreposição de tela...");
            bottomStatusText.setTextColor(Color.GRAY);

            // Caso perca permissão no meio do processo, desliga serviço
            stopService(new Intent(this, DisplayService.class));
        }
    }

    private void buildUI() {
        mainScrollView = new ScrollView(this);
        mainScrollView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        mainScrollView.setFillViewport(true);

        mainContentContainer = new LinearLayout(this);
        mainContentContainer.setOrientation(LinearLayout.VERTICAL);
        mainContentContainer.setGravity(Gravity.CENTER_HORIZONTAL);
        mainContentContainer.setPadding(dpToPx(16), dpToPx(64), dpToPx(16), dpToPx(48));
        mainContentContainer.setLayoutParams(new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT));

        // 1. ÍCONE DO JOGO (8 BALL POOL)
        gameIconContainer = new LinearLayout(this);
        gameIconContainer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams iconContainerParams = new LinearLayout.LayoutParams(
                dpToPx(110), dpToPx(110));
        iconContainerParams.setMargins(0, dpToPx(24), 0, dpToPx(20));
        gameIconContainer.setLayoutParams(iconContainerParams);

        GradientDrawable circularBorderGd = new GradientDrawable();
        circularBorderGd.setShape(GradientDrawable.OVAL);
        circularBorderGd.setColor(Color.BLACK);
        circularBorderGd.setStroke(dpToPx(2), 0xFF00FFCC);
        gameIconContainer.setBackground(circularBorderGd);

        ImageView iconView = new ImageView(this);
        iconView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        iconView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        
        // Carrega a imagem do drawable local (icon_8ball)
        int resourceId = getResources().getIdentifier("icon_8ball", "drawable", getPackageName());
        if (resourceId != 0) {
            iconView.setImageResource(resourceId);
        } else {
            iconView.setImageResource(android.R.drawable.sym_def_app_icon);
        }
        gameIconContainer.addView(iconView);
        mainContentContainer.addView(gameIconContainer);

        // 2. CARD DE AVISO DE DESENVOLVIMENTO
        warningCard = new LinearLayout(this);
        warningCard.setOrientation(LinearLayout.VERTICAL);
        warningCard.setGravity(Gravity.CENTER_HORIZONTAL);
        warningCard.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
        LinearLayout.LayoutParams warningParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        warningParams.setMargins(0, 0, 0, dpToPx(24));
        warningCard.setLayoutParams(warningParams);

        GradientDrawable warningGd = new GradientDrawable();
        warningGd.setColor(0xFF141416);
        warningGd.setCornerRadius(dpToPx(16));
        warningGd.setStroke(dpToPx(1), 0xFF222225);
        warningCard.setBackground(warningGd);

        TextView alertIcon = new TextView(this);
        alertIcon.setText("⚠️");
        alertIcon.setTextSize(22);
        alertIcon.setGravity(Gravity.CENTER);
        warningCard.addView(alertIcon);

        TextView alertText = new TextView(this);
        alertText.setText("Esse App ainda está em desenvolvimento por favor ative todas as permissões necessárias para o app processar e ser funcional ao seu favor");
        alertText.setTextSize(13);
        alertText.setTextColor(0xFFCCCCCC);
        alertText.setLineSpacing(dpToPx(2), 1.0f);
        alertText.setGravity(Gravity.CENTER);
        alertText.setTypeface(null, Typeface.NORMAL);
        LinearLayout.LayoutParams alertTextParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        alertTextParams.setMargins(0, dpToPx(8), 0, 0);
        alertText.setLayoutParams(alertTextParams);
        warningCard.addView(alertText);

        mainContentContainer.addView(warningCard);

        // 3. BOTÃO START (ATIVADOR)
        startButton = new Button(this);
        startButton.setText("CONCEDER PERMISSÃO DE SOBREPOR [ START ]");
        startButton.setTextSize(13);
        startButton.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        startButton.setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14));
        LinearLayout.LayoutParams startBtnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(52));
        startBtnParams.setMargins(0, 0, 0, dpToPx(24));
        startButton.setLayoutParams(startBtnParams);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isOverlayPermissionGranted) {
                    Intent intent = new Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName())
                    );
                    startActivity(intent);
                } else {
                    Toast.makeText(HomeActivity.this, "Sistemas calibrados e prontos!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        mainContentContainer.addView(startButton);

        // 4. PAINEL NATIVO DE ATIVAÇÕES (CARD INFERIOR COM SWITCHES)
        activationPanel = new LinearLayout(this);
        activationPanel.setOrientation(LinearLayout.VERTICAL);
        activationPanel.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
        LinearLayout.LayoutParams panelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        panelParams.setMargins(0, 0, 0, dpToPx(12));
        activationPanel.setLayoutParams(panelParams);

        GradientDrawable panelGd = new GradientDrawable();
        panelGd.setColor(0xFF0E0E11);
        panelGd.setCornerRadius(dpToPx(20));
        panelGd.setStroke(dpToPx(1), 0xFF1E1E24);
        activationPanel.setBackground(panelGd);

        TextView panelTitle = new TextView(this);
        panelTitle.setText("PAINEL NATIVO DE ATIVAÇÕES");
        panelTitle.setTextSize(12);
        panelTitle.setTextColor(0xFF00FFCC);
        panelTitle.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        LinearLayout.LayoutParams pTitleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        pTitleParams.setMargins(0, 0, 0, dpToPx(12));
        panelTitle.setLayoutParams(pTitleParams);
        activationPanel.addView(panelTitle);

        // Função 1: Switch 1 Row
        View switchRowCocos = buildSwitchRow(
                "1. Lib Cocos2D-X Physics Pro",
                "Física 2D, ricochetes e projeção de guidas",
                switchCocos = new Switch(this),
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        OverlayConfig.isCocosPhysicsActive = isChecked;
                    }
                }
        );
        activationPanel.addView(switchRowCocos);

        // Divisor 1
        activationPanel.addView(buildPanelDivider());

        // Função 2: Switch 2 Row
        View switchRowAimPool = buildSwitchRow(
                "2. AimPool V1",
                "Inicia as trajetórias virtuais sobre o jogo",
                switchAimPool = new Switch(this),
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        OverlayConfig.isAimPoolActive = isChecked;
                        updateServiceLifecycle();
                    }
                }
        );
        activationPanel.addView(switchRowAimPool);

        // Divisor 2
        activationPanel.addView(buildPanelDivider());

        // Função 3: Switch 3 Row
        View switchRowItsEngine = buildSwitchRow(
                "3. ItsEngine Overlay Panel",
                "Inicia o painel de recalibração flutuante",
                switchItsEngine = new Switch(this),
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        OverlayConfig.isItsEngineActive = isChecked;
                        updateServiceLifecycle();
                    }
                }
        );
        activationPanel.addView(switchRowItsEngine);

        mainContentContainer.addView(activationPanel);

        // 5. STATUS DO RODAPÉ (CONFORME PERMISSÃO)
        bottomStatusText = new TextView(this);
        bottomStatusText.setText("Aguardando ativação da sobreposição de tela...");
        bottomStatusText.setTextSize(11);
        bottomStatusText.setTypeface(Typeface.MONOSPACE, Typeface.NORMAL);
        bottomStatusText.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        statusParams.setMargins(0, dpToPx(12), 0, dpToPx(24));
        bottomStatusText.setLayoutParams(statusParams);
        mainContentContainer.addView(bottomStatusText);

        mainScrollView.addView(mainContentContainer);
        rootLayout.addView(mainScrollView);
    }

    private View buildSwitchRow(String title, String subtitle, Switch actualSwitch, CompoundButton.OnCheckedChangeListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        row.setPadding(0, dpToPx(8), 0, dpToPx(8));

        LinearLayout textBlock = new LinearLayout(this);
        textBlock.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textBParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        textBlock.setLayoutParams(textBParams);

        TextView labelTitle = new TextView(this);
        labelTitle.setText(title);
        labelTitle.setTextSize(14);
        labelTitle.setTextColor(Color.WHITE);
        labelTitle.setTypeface(null, Typeface.BOLD);
        textBlock.addView(labelTitle);

        TextView labelSub = new TextView(this);
        labelSub.setText(subtitle);
        labelSub.setTextSize(11);
        labelSub.setTextColor(Color.GRAY);
        labelSub.setLineSpacing(dpToPx(1), 1.0f);
        LinearLayout.LayoutParams labelSubParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        labelSubParams.setMargins(0, dpToPx(2), 0, 0);
        labelSub.setLayoutParams(labelSubParams);
        textBlock.addView(labelSub);

        row.addView(textBlock);

        // Customizações básicas do switch em Java para ficar bonito estilo M3
        actualSwitch.setOnCheckedChangeListener(listener);
        row.addView(actualSwitch);

        return row;
    }

    private View buildPanelDivider() {
        View divider = new View(this);
        LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(1));
        divParams.setMargins(0, dpToPx(8), 0, dpToPx(8));
        divider.setLayoutParams(divParams);
        divider.setBackgroundColor(0xFF1E1E24);
        return divider;
    }

    private void updateServiceLifecycle() {
        Intent serviceIntent = new Intent(this, DisplayService.class);
        boolean shouldRun = OverlayConfig.isAimPoolActive || OverlayConfig.isItsEngineActive;

        if (shouldRun && Settings.canDrawOverlays(this)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        } else {
            stopService(serviceIntent);
        }
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }
}
