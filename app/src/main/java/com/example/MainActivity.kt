package com.example

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    // Declarações nativas JNI (para quando o usuário rodar no AIDE Plus)
    external fun getEncryptedKey(): String
    external fun validateKeyNative(inputKey: String): Boolean
    external fun getLibraryStatus(): String

    companion object {
        init {
            try {
                System.loadLibrary("ItsLoader")
                System.loadLibrary("ItsLGL")
                Log.d("ItsEngine", "JNI Libs carregadas com sucesso!")
            } catch (e: UnsatisfiedLinkError) {
                Log.e("ItsEngine", "Erro JNI (Tratado para simulador): ${e.message}")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    MainScreenContent(
                        onKeyValidated = {
                            val intent = Intent(this@MainActivity, HomeActivity::class.java)
                            startActivity(intent)
                            finish()
                        },
                        validateKey = { key ->
                            try {
                                validateKeyNative(key)
                            } catch (e: UnsatisfiedLinkError) {
                                // Fallback em Kotlin caso não esteja compilado de fato no emulador
                                key == "ITS8BPVIP-O929-JE2J-0MW2-OWI2"
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContent(
    onKeyValidated: () -> Unit,
    validateKey: (String) -> Boolean
) {
    val context = LocalContext.current
    var progress by remember { mutableStateOf(0f) }
    var loadingFinished by remember { mutableStateOf(false) }
    var keyInput by remember { mutableStateOf("") }
    var isValidating by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf(false) }
    var validationSuccess by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Efeito para contar de 0% a 100% durante 20 segundos
    LaunchedEffect(Unit) {
        val durationMs = 20000L  // 20 segundos
        val intervalMs = 100L    // Atualização a cada 100ms
        val totalSteps = durationMs / intervalMs
        for (i in 1..totalSteps) {
            delay(intervalMs)
            progress = i.toFloat() / totalSteps
        }
        progress = 1f
        loadingFinished = true
    }

    // Texto de status do carregamento das libs
    val loadingStatus = when {
        progress < 0.25f -> "Iniciando barramento de memória..."
        progress < 0.45f -> "Carregando libItsLoader.so (Plug-in)..."
        progress < 0.60f -> "Extraindo algoritmo de chave criptografada do Loader..."
        progress < 0.85f -> "Carregando libItsLGL.so (Lógica de exibição)..."
        progress < 1.00f -> "Resolvendo vínculos de ponteiros JNI..."
        else -> "Módulos JNI ativos!"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Linhas de design sutis em segundo plano (Estilo Hacker/Cyberpunk)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Cabeçalho superior
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 48.dp)
            ) {
                Text(
                    text = "ITS ENGINE",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 6.sp,
                    color = Color(0xFF00FFCC),
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.testTag("app_title")
                )
                Text(
                    text = "V.24.6.0 NATIVE INTERFACE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Área central: transiciona do loading para o formulário de chave
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (!loadingFinished) {
                    // TELA DE LOADING (20 Segundos)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            fontSize = 64.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.shadow(2.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Barra de progresso verde néon com bordas arredondadas e efeito de sombra
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF222222))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress)
                                    .fillMaxHeight()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFF00AAFF), Color(0xFF00FFCC))
                                        )
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = loadingStatus,
                            fontSize = 13.sp,
                            color = Color(0xFF00FFCC).copy(alpha = 0.85f),
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("status_log")
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Aguardando carregamento da memoria ffs...",
                            fontSize = 10.sp,
                            color = Color.DarkGray,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else {
                    // TELA DE CHAVE VIP (Lógica moderna com bordas arredondadas)
                    androidx.compose.animation.AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + scaleIn(initialScale = 0.9f)
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .shadow(16.dp, RoundedCornerShape(24.dp))
                                .border(1.dp, Color(0xFF333333), RoundedCornerShape(24.dp)),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF0D0D0D)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Chave Protegida",
                                    tint = Color(0xFF00FFCC),
                                    modifier = Modifier.size(40.dp)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "AUTENTICAÇÃO NATIVA LGL",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace
                                )

                                Text(
                                    text = "Insira sua Key VIP válida",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                                )

                                OutlinedTextField(
                                    value = keyInput,
                                    onValueChange = {
                                        keyInput = it
                                        validationError = false
                                    },
                                    label = { Text("Chave VIP de Acesso") },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = { keyboardController?.hide() }
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF00FFCC),
                                        unfocusedBorderColor = Color(0xFF333333),
                                        focusedLabelColor = Color(0xFF00FFCC),
                                        unfocusedLabelColor = Color.Gray,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("key_input_field")
                                )

                                if (validationError) {
                                    Text(
                                        text = "Chave Inválida. Tente novamente!",
                                        color = Color.Red,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .padding(top = 8.dp)
                                            .testTag("key_error_msg")
                                    )
                                }

                                if (validationSuccess) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Sucesso",
                                            tint = Color.Green,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Acesso Premium Autorizado!",
                                            color = Color.Green,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Botão de Validar Chave
                                Button(
                                    onClick = {
                                        isValidating = true
                                        keyboardController?.hide()
                                        if (validateKey(keyInput.trim())) {
                                            validationSuccess = true
                                            validationError = false
                                            Toast.makeText(context, "VIP Ativado!", Toast.LENGTH_SHORT).show()
                                            onKeyValidated()
                                        } else {
                                            validationError = true
                                        }
                                        isValidating = false
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF00FFCC)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("validate_button")
                                ) {
                                    Text(
                                        text = "VALIDAR INTEGRIDADE",
                                        color = Color.Black,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Botão de fácil uso para preencher automático
                                TextButton(
                                    onClick = {
                                        keyInput = "ITS8BPVIP-O929-JE2J-0MW2-OWI2"
                                        validationError = false
                                    },
                                    modifier = Modifier.testTag("autofill_button")
                                ) {
                                    Text(
                                        text = "[ PREENCHER CHAVE DE TESTE ]",
                                        color = Color(0xFF00AAFF),
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Rodapé do desenvolvedor
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Text(
                    text = "AIDE PLUS COMPILATION PLATFORM",
                    fontSize = 9.sp,
                    color = Color.DarkGray,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "BY ITS TEAM DEVELOPERS",
                    fontSize = 9.sp,
                    color = Color.DarkGray,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
