package com.example;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    // Declarações nativas JNI
    public native String getEncryptedKey();
    public native boolean validateKeyNative(String inputKey);
    public native String getLibraryStatus();

    static {
        try {
            System.loadLibrary("ItsLoader");
            System.loadLibrary("ItsLGL");
            Log.d("ItsEngine", "JNI Libs carregadas com sucesso!");
        } catch (UnsatisfiedLinkError e) {
            Log.e("ItsEngine", "Erro JNI (Tratado para simulador): " + e.getMessage());
        }
    }

    private FrameLayout rootLayout;
    private LinearLayout mainContentContainer;
    
    // De Loading
    private LinearLayout loadingLayout;
    private TextView percentTextView;
    private View progressTrackView;
    private View progressBarView;
    private TextView logTextView;
    
    // De Formulario
    private ScrollView formScrollView;
    private LinearLayout cardContainer;
    private EditText keyEditText;
    private TextView errorTextView;
    private TextView successTextView;

    private int progressPercent = 0;
    private final Handler updateHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Edge to Edge - tela cheia
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        // Criação do layout principal
        rootLayout = new FrameLayout(this);
        rootLayout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        rootLayout.setBackgroundColor(0xFF000000); // Fundo total preto

        buildUI();
        setContentView(rootLayout);

        startLoadingTimer();
    }

    private void buildUI() {
        // Container principal linear com margens adequadas
        mainContentContainer = new LinearLayout(this);
        mainContentContainer.setOrientation(LinearLayout.VERTICAL);
        FrameLayout.LayoutParams mainParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        mainContentContainer.setLayoutParams(mainParams);
        mainContentContainer.setGravity(Gravity.CENTER_HORIZONTAL);
        mainContentContainer.setPadding(dpToPx(24), dpToPx(64), dpToPx(24), dpToPx(32));

        // ---------------- CABEÇALHO ----------------
        LinearLayout headerLayout = new LinearLayout(this);
        headerLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        headerParams.setMargins(0, dpToPx(24), 0, dpToPx(24));
        headerLayout.setLayoutParams(headerParams);
        headerLayout.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView titleView = new TextView(this);
        titleView.setText("ITS ENGINE");
        titleView.setTextSize(28);
        titleView.setTextColor(0xFF00FFCC); // Verde neon
        titleView.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        titleView.setLetterSpacing(0.25f); // Letras espacadas para cara cyberpunk
        headerLayout.addView(titleView);

        TextView subtitleView = new TextView(this);
        subtitleView.setText("V.24.6.0 NATIVE INTERFACE");
        subtitleView.setTextSize(11);
        subtitleView.setTextColor(0xFF888888); // Cinza
        subtitleView.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        headerLayout.addView(subtitleView);

        mainContentContainer.addView(headerLayout);

        // ---------------- ÁREA CENTRAL CONTAINER (LOADING / FORMULARIO) ----------------
        FrameLayout dynamicContentFrame = new FrameLayout(this);
        LinearLayout.LayoutParams centerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0, 1.0f); // Ocupa todo o espaco do meio
        dynamicContentFrame.setLayoutParams(centerParams);

        // 1. TELA DE LOADING
        loadingLayout = new LinearLayout(this);
        loadingLayout.setOrientation(LinearLayout.VERTICAL);
        loadingLayout.setGravity(Gravity.CENTER);
        loadingLayout.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        percentTextView = new TextView(this);
        percentTextView.setText("0%");
        percentTextView.setTextSize(64);
        percentTextView.setTextColor(0xFFFFFFFF);
        percentTextView.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        loadingLayout.addView(percentTextView);

        // Barra de progresso customizada neon
        progressTrackView = new View(this);
        LinearLayout.LayoutParams trackParams = new LinearLayout.LayoutParams(
                dpToPx(260), dpToPx(8));
        trackParams.setMargins(0, dpToPx(16), 0, dpToPx(24));
        progressTrackView.setLayoutParams(trackParams);
        GradientDrawable trackGd = new GradientDrawable();
        trackGd.setColor(0xFF222222);
        trackGd.setCornerRadius(dpToPx(4));
        progressTrackView.setBackground(trackGd);

        // Barra de progresso interna que cresce
        FrameLayout progressContainer = new FrameLayout(this);
        LinearLayout.LayoutParams pContainerParams = new LinearLayout.LayoutParams(
                dpToPx(260), dpToPx(8));
        pContainerParams.setMargins(0, dpToPx(16), 0, dpToPx(24));
        progressContainer.setLayoutParams(pContainerParams);
        progressContainer.setBackground(trackGd);

        progressBarView = new View(this);
        FrameLayout.LayoutParams progressFillParams = new FrameLayout.LayoutParams(
                0, FrameLayout.LayoutParams.MATCH_PARENT);
        progressBarView.setLayoutParams(progressFillParams);
        GradientDrawable progressGd = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[] { 0xFF00AAFF, 0xFF00FFCC });
        progressGd.setCornerRadius(dpToPx(4));
        progressBarView.setBackground(progressGd);
        progressContainer.addView(progressBarView);

        loadingLayout.addView(progressContainer);

        logTextView = new TextView(this);
        logTextView.setText("Iniciando barramento de memória...");
        logTextView.setTextSize(13);
        logTextView.setTextColor(0xDD00FFCC);
        logTextView.setTypeface(Typeface.MONOSPACE, Typeface.NORMAL);
        logTextView.setGravity(Gravity.CENTER);
        loadingLayout.addView(logTextView);

        TextView subHintView = new TextView(this);
        subHintView.setText("Aguardando carregamento da memoria ffs...");
        subHintView.setTextSize(10);
        subHintView.setTextColor(0xFF555555);
        subHintView.setTypeface(Typeface.MONOSPACE, Typeface.NORMAL);
        subHintView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subHintParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        subHintParams.setMargins(0, dpToPx(8), 0, 0);
        subHintView.setLayoutParams(subHintParams);
        loadingLayout.addView(subHintView);

        dynamicContentFrame.addView(loadingLayout);

        // 2. TELA DE FORMULARIO COM CHAVE VIP (Inicialmente GONE)
        formScrollView = new ScrollView(this);
        formScrollView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        formScrollView.setVisibility(View.GONE);

        cardContainer = new LinearLayout(this);
        cardContainer.setOrientation(LinearLayout.VERTICAL);
        cardContainer.setGravity(Gravity.CENTER_HORIZONTAL);
        ScrollView.LayoutParams cardParams = new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT);
        cardParams.gravity = Gravity.CENTER_VERTICAL;
        cardContainer.setLayoutParams(cardParams);
        cardContainer.setPadding(dpToPx(24), dpToPx(24), dpToPx(24), dpToPx(24));
        
        GradientDrawable cardGd = new GradientDrawable();
        cardGd.setColor(0xFF0D0D0D); // Deep dark gray
        cardGd.setCornerRadius(dpToPx(24));
        cardGd.setStroke(dpToPx(1), 0xFF333333);
        cardContainer.setBackground(cardGd);

        // Icone trancado simulado por texto ou caractere
        TextView iconLockView = new TextView(this);
        iconLockView.setText("🔒");
        iconLockView.setTextSize(36);
        iconLockView.setGravity(Gravity.CENTER);
        iconLockView.setTextColor(0xFF00FFCC);
        cardContainer.addView(iconLockView);

        TextView authTitleView = new TextView(this);
        authTitleView.setText("AUTENTICAÇÃO NATIVA LGL");
        authTitleView.setTextSize(15);
        authTitleView.setTextColor(Color.WHITE);
        authTitleView.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        authTitleView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams authTitleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        authTitleParams.setMargins(0, dpToPx(12), 0, 0);
        authTitleView.setLayoutParams(authTitleParams);
        cardContainer.addView(authTitleView);

        TextView authSubtitleView = new TextView(this);
        authSubtitleView.setText("Insira sua Key VIP válida");
        authSubtitleView.setTextSize(12);
        authSubtitleView.setTextColor(0xFF888888);
        authSubtitleView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams authSubParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        authSubParams.setMargins(0, dpToPx(4), 0, dpToPx(16));
        authSubtitleView.setLayoutParams(authSubParams);
        cardContainer.addView(authSubtitleView);

        // EditText de senha da chave VIP
        keyEditText = new EditText(this);
        keyEditText.setHint("Chave VIP de Acesso");
        keyEditText.setHintTextColor(0xFF555555);
        keyEditText.setTextColor(Color.WHITE);
        keyEditText.setTextSize(14);
        keyEditText.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12));
        keyEditText.setSingleLine(true);
        keyEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        keyEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
        
        GradientDrawable editGd = new GradientDrawable();
        editGd.setColor(0xFF161618);
        editGd.setCornerRadius(dpToPx(12));
        editGd.setStroke(dpToPx(1), 0xFF333333);
        keyEditText.setBackground(editGd);
        
        LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        keyEditText.setLayoutParams(editParams);
        cardContainer.addView(keyEditText);

        // Erro TextView
        errorTextView = new TextView(this);
        errorTextView.setText("Chave Inválida. Tente novamente!");
        errorTextView.setTextColor(Color.RED);
        errorTextView.setTextSize(12);
        errorTextView.setTypeface(null, Typeface.BOLD);
        errorTextView.setVisibility(View.GONE);
        LinearLayout.LayoutParams errorParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        errorParams.setMargins(0, dpToPx(8), 0, 0);
        errorTextView.setLayoutParams(errorParams);
        cardContainer.addView(errorTextView);

        // Sucesso TextView
        successTextView = new TextView(this);
        successTextView.setText("Acesso Premium Autorizado!");
        successTextView.setTextColor(Color.GREEN);
        successTextView.setTextSize(12);
        successTextView.setTypeface(null, Typeface.BOLD);
        successTextView.setVisibility(View.GONE);
        LinearLayout.LayoutParams successParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        successParams.setMargins(0, dpToPx(8), 0, 0);
        successTextView.setLayoutParams(successParams);
        cardContainer.addView(successTextView);

        // Botao de envio
        final Button submitBtn = new Button(this);
        submitBtn.setText("VALIDAR INTEGRIDADE");
        submitBtn.setTextColor(Color.BLACK);
        submitBtn.setTypeface(null, Typeface.BOLD);
        submitBtn.setTextSize(13);
        
        GradientDrawable btnGd = new GradientDrawable();
        btnGd.setColor(0xFF00FFCC);
        btnGd.setCornerRadius(dpToPx(12));
        submitBtn.setBackground(btnGd);

        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(48));
        btnParams.setMargins(0, dpToPx(24), 0, 0);
        submitBtn.setLayoutParams(btnParams);
        submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String input = keyEditText.getText().toString().trim();
                boolean isOk = false;
                try {
                    isOk = validateKeyNative(input);
                } catch (UnsatisfiedLinkError e) {
                    // Fallback local se rodando sob teste rápido local
                    isOk = "ITS8BPVIP-O929-JE2J-0MW2-OWI2".equals(input);
                }

                if (isOk) {
                    successTextView.setVisibility(View.VISIBLE);
                    errorTextView.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, "VIP Ativado!", Toast.LENGTH_SHORT).show();
                    
                    // Vai para HomeActivity com delay de 800ms
                    updateHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    }, 800);
                } else {
                    errorTextView.setVisibility(View.VISIBLE);
                    successTextView.setVisibility(View.GONE);
                }
            }
        });
        cardContainer.addView(submitBtn);

        // Botao Preencher Chave de Teste
        TextView autofillView = new TextView(this);
        autofillView.setText("[ PREENCHER CHAVE DE TESTE ]");
        autofillView.setTextColor(0xFF00AAFF);
        autofillView.setTextSize(11);
        autofillView.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        autofillView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams autoParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        autoParams.setMargins(0, dpToPx(16), 0, 0);
        autofillView.setLayoutParams(autoParams);
        autofillView.setClickable(true);
        autofillView.setFocusable(true);
        autofillView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                keyEditText.setText("ITS8BPVIP-O929-JE2J-0MW2-OWI2");
                errorTextView.setVisibility(View.GONE);
            }
        });
        cardContainer.addView(autofillView);

        formScrollView.addView(cardContainer);
        dynamicContentFrame.addView(formScrollView);

        mainContentContainer.addView(dynamicContentFrame);

        // ---------------- RODAPÉ ----------------
        LinearLayout footerLayout = new LinearLayout(this);
        footerLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams footerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        footerParams.setMargins(0, dpToPx(24), 0, dpToPx(12));
        footerLayout.setLayoutParams(footerParams);
        footerLayout.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView fLabel1 = new TextView(this);
        fLabel1.setText("AIDE PLUS COMPILATION PLATFORM");
        fLabel1.setTextSize(9);
        fLabel1.setTextColor(0xFF333333);
        fLabel1.setTypeface(Typeface.MONOSPACE, Typeface.NORMAL);
        footerLayout.addView(fLabel1);

        TextView fLabel2 = new TextView(this);
        fLabel2.setText("BY ITS TEAM DEVELOPERS");
        fLabel2.setTextSize(9);
        fLabel2.setTextColor(0xFF333333);
        fLabel2.setTypeface(Typeface.MONOSPACE, Typeface.NORMAL);
        footerLayout.addView(fLabel2);

        mainContentContainer.addView(footerLayout);

        rootLayout.addView(mainContentContainer);
    }

    private void startLoadingTimer() {
        // Incrementa o progresso ao longo de 20 segundos (ticks a cada 100ms)
        // 20 segundos total = 200 ticks
        final int totalTicks = 100; // vamos fazer 100 ticks onde cada tick demora 200ms
        final int tickDelayMs = 200;

        updateHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                progressPercent++;
                if (progressPercent <= 100) {
                    percentTextView.setText(progressPercent + "%");
                    
                    // Modifica a barra interna programaticamente
                    int maxBarWidth = dpToPx(260);
                    int currentFillWidth = (maxBarWidth * progressPercent) / 100;
                    progressBarView.getLayoutParams().width = currentFillWidth;
                    progressBarView.requestLayout();

                    // Altera o log correspondente
                    if (progressPercent < 25) {
                        logTextView.setText("Iniciando barramento de memória...");
                    } else if (progressPercent < 45) {
                        logTextView.setText("Carregando libItsLoader.so (Plug-in)...");
                    } else if (progressPercent < 60) {
                        logTextView.setText("Extraindo algoritmo de chave criptografada do Loader...");
                    } else if (progressPercent < 85) {
                        logTextView.setText("Carregando libItsLGL.so (Lógica de exibição)...");
                    } else if (progressPercent < 100) {
                        logTextView.setText("Resolvendo vínculos de ponteiros JNI...");
                    } else {
                        logTextView.setText("Módulos JNI ativos!");
                    }

                    updateHandler.postDelayed(this, tickDelayMs);
                } else {
                    // Completo! Esconde Loading e mostra Formulario de Chave VIP
                    loadingLayout.setVisibility(View.GONE);
                    formScrollView.setVisibility(View.VISIBLE);
                }
            }
        }, tickDelayMs);
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }
}
