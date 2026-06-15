package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme

class HomeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    HomeScreenContent()
                }
            }
        }
    }
}

@Composable
fun HomeScreenContent() {
    val context = LocalContext.current
    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    
    // Launcher para verificar o retorno da tela de permissão do sistema
    val overlayLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        hasOverlayPermission = Settings.canDrawOverlays(context)
        if (hasOverlayPermission) {
            Toast.makeText(context, "Permissão de sobreposição concedida!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Permissão negada. Desculpe-nos, o app precisa dessa permissão.", Toast.LENGTH_LONG).show()
        }
    }

    // Gerenciador de inicialização do DisplayService
    fun updateServiceLifecycle() {
        val serviceIntent = Intent(context, DisplayService::class.java)
        val shouldRun = OverlayConfig.isAimPoolActive || OverlayConfig.isItsEngineActive

        if (shouldRun && hasOverlayPermission) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } else {
            context.stopService(serviceIntent)
        }
    }

    // Sempre que os toggles mudarem ou a permissão mudar, atualizamos o serviço
    LaunchedEffect(OverlayConfig.isAimPoolActive, OverlayConfig.isItsEngineActive, hasOverlayPermission) {
        updateServiceLifecycle()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070708))
            .padding(16.dp)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Bloco Superior: Informações e Ícone do Jogo
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // Ícone de Jogo no Centro do Topo (8 Ball Pool)
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color(0xFF00FFCC), CircleShape)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                // Tenta renderizar a imagem gerada. Caso não carregue de imediato usa um fallback
                Image(
                    painter = painterResource(id = R.drawable.icon_8ball),
                    contentDescription = "Ícone 8 Ball Pool",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .testTag("game_icon")
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Mensagem de aviso descriptográfica solicitada
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF141416)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .border(1.dp, Color(0xFF222225), RoundedCornerShape(16.dp))
                    .testTag("warning_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Aviso de Desenvolvimento",
                        tint = Color(0xFFEAA200),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Esse App ainda está em desenvolvimento por favor ative todas as permissões necessárias para o app processar e ser funcional ao seu favor",
                        fontSize = 13.sp,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Bloco do Botão START (Ativador de Serviços)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Se já tem permissão de sobreposição, não precisa insistir no fluxo de permissão
            Button(
                onClick = {
                    if (!hasOverlayPermission) {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        overlayLauncher.launch(intent)
                    } else {
                        Toast.makeText(context, "Sistemas calibrados e prontos!", Toast.LENGTH_SHORT).show()
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (hasOverlayPermission) Color(0xFF00FFCC) else Color(0xFF333333)
                ),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(52.dp)
                    .testTag("start_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Botão de Iniciar",
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (hasOverlayPermission) "SISTEMA SEGURO [ START ]" else "CONCEDER PERMISSÃO DE SOBREPOR [ START ]",
                        color = Color.Black,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Bloco Inferior: Os 3 Switches Ativadores (Disponíveis apenas após Overlay Ativado)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            AnimatedVisibility(
                visible = hasOverlayPermission,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .border(1.dp, Color(0xFF1E1E24), RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0E0E11)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "PAINEL NATIVO DE ATIVAÇÕES",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00FFCC),
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        // Função 1: Library Cocos2D-X Fisics Pro
                        SwitchConfigRow(
                            title = "1. Lib Cocos2D-X Physics Pro",
                            subtitle = "Física 2D, ricochetes e projeção de guidas",
                            checked = OverlayConfig.isCocosPhysicsActive,
                            onCheckedChange = { OverlayConfig.isCocosPhysicsActive = it },
                            tag = "switch_cocos"
                        )

                        Divider(color = Color(0xFF1E1E24), thickness = 0.5.dp)

                        // Função 2: AimPool V1
                        SwitchConfigRow(
                            title = "2. AimPool V1",
                            subtitle = "Inicia as trajetórias virtuais sobre o jogo",
                            checked = OverlayConfig.isAimPoolActive,
                            onCheckedChange = { OverlayConfig.isAimPoolActive = it },
                            tag = "switch_aimpool"
                        )

                        Divider(color = Color(0xFF1E1E24), thickness = 0.5.dp)

                        // Função 3: ItsEngine
                        SwitchConfigRow(
                            title = "3. ItsEngine Overlay Panel",
                            subtitle = "Inicia o painel de recalibração flutuante",
                            checked = OverlayConfig.isItsEngineActive,
                            onCheckedChange = { OverlayConfig.isItsEngineActive = it },
                            tag = "switch_its_engine"
                        )
                    }
                }
            }

            if (!hasOverlayPermission) {
                Text(
                    text = "Aguardando ativação da sobreposição de tela...",
                    fontSize = 11.sp,
                    color = Color.DarkGray,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                Text(
                    text = "Overlay e Injeção JNI prontos para uso!",
                    fontSize = 11.sp,
                    color = Color(0xFF00FFCC).copy(alpha = 0.8f),
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
fun SwitchConfigRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Color.Gray,
                lineHeight = 14.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = Color(0xFF00FFCC),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color(0xFF1F1F24)
            )
        )
    }
}
