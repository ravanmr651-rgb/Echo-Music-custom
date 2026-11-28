package echo.music.iad1tya.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import echo.music.iad1tya.BuildConfig
import echo.music.iad1tya.R

@Composable
fun WelcomeDialog(onDismissRequest: () -> Unit) {
  val uriHandler = LocalUriHandler.current

  Dialog(
    onDismissRequest = onDismissRequest,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Card(
      modifier = Modifier.padding(24.dp).fillMaxWidth(),
      shape = RoundedCornerShape(28.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
      Column(
        modifier = Modifier.fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(vertical = 20.dp, horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        WelcomeAppCard()

        WelcomeSectionCard(title = "Developer") {
          WelcomeActionRow(
            icon = painterResource(R.drawable.ic_telegram_new),
            title = "Telegram",
            subtitle = "@ULTRAAMODS",
            onClick = { uriHandler.openUri("https://t.me/ULTRAAMODS") }
          )
        }

        WelcomeSectionCard(title = "Support") {
          WelcomeActionRow(
            icon = painterResource(R.drawable.upi_new),
            title = "UPI",
            subtitle = "Support AURA Music",
            onClick = { uriHandler.openUri("https://get.famapp.in/?utm_source=qr") }
          )
        }

        WelcomeSectionCard(title = "Community") {
          WelcomeActionRow(
            icon = painterResource(R.drawable.ic_telegram_new),
            title = "Telegram",
            subtitle = "@ULTRAAMODS",
            onClick = { uriHandler.openUri("https://t.me/ULTRAAMODS") }
          )
        }

        Button(
          onClick = onDismissRequest,
          modifier = Modifier.fillMaxWidth().height(50.dp),
          shape = RoundedCornerShape(16.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
          )
        ) {
          Text("Continue", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
      }
    }
  }
}

@Composable
private fun WelcomeAppCard() {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp, horizontal = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Image(
        painter = painterResource(R.drawable.ic_launcher_nobg),
        contentDescription = "AURA Music",
        modifier = Modifier.size(88.dp).clip(CircleShape),
      )
      Text(
        text = "AURA Music",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = "Developed by luv",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
      ) {
        Text(
          text = BuildConfig.VERSION_NAME,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Medium,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
      }
    }
  }
}

@Composable
private fun WelcomeSectionCard(
  title: String,
  content: @Composable ColumnScope.() -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.primary,
      modifier = Modifier.padding(start = 6.dp),
    )
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
      ),
      elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
      Column(
        modifier = Modifier.padding(vertical = 4.dp),
        content = content,
      )
    }
  }
}

@Composable
private fun WelcomeActionRow(
  icon: Painter,
  title: String,
  subtitle: String,
  onClick: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Icon(
      painter = icon,
      contentDescription = null,
      modifier = Modifier.size(24.dp),
      tint = MaterialTheme.colorScheme.primary,
    )
    Column(modifier = Modifier.weight(1f)) {
      Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
      Text(
        subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Icon(
      painter = painterResource(R.drawable.arrow_forward),
      contentDescription = null,
      modifier = Modifier.size(18.dp),
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}
