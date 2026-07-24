package com.example.nervoscompanion.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nervoscompanion.News
import com.example.nervoscompanion.R
import com.example.nervoscompanion.theme.currentTheme
import com.example.nervoscompanion.ui.components.TabHeader
import androidx.navigation3.runtime.NavKey
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.nervoscompanion.data.RpcClient
import com.example.nervoscompanion.data.SettingsStore
import com.example.nervoscompanion.data.ChainStatsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import java.util.TimeZone
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset

@Composable
fun HomeScreen(onNavigate: (NavKey) -> Unit, modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val settingsStore = remember { SettingsStore(context) }
  val chainStatsRepository = remember {
    val db = com.example.nervoscompanion.data.cache.AppDatabase.getDatabase(context)
    ChainStatsRepository(db.chainStatsDao(), settingsStore)
  }
  val coroutineScope = rememberCoroutineScope()

  var rpcUrl by remember { mutableStateOf(settingsStore.rpcUrl) }
  var rpcNetwork by remember { mutableStateOf(settingsStore.rpcNetwork) }

  // Load RPC status & price details
  var blockNumber by remember { mutableStateOf<Long?>(null) }
  var epochNumber by remember { mutableStateOf<Long?>(null) }
  var epochProgress by remember { mutableStateOf<String?>(null) }
  var nodeVersion by remember { mutableStateOf<String?>(null) }

  var ckbPrice by remember { mutableStateOf<Double?>(null) }
  var ckbChange by remember { mutableStateOf<Double?>(null) }
  var ckbMarketCap by remember { mutableStateOf<Double?>(null) }
  var ckbVolume by remember { mutableStateOf<Double?>(null) }
  var priceHistory by remember { mutableStateOf<String?>(null) }

  var isLoading by remember { mutableStateOf(false) }
  var errorMsg by remember { mutableStateOf<String?>(null) }

  // Pulsing Dot Transition
  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val pulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.3f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(1200, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulseAlpha"
  )

  // Floating Hero Glow Alpha
  val heroGlowAlpha by infiniteTransition.animateFloat(
    initialValue = 0.15f,
    targetValue = 0.35f,
    animationSpec = infiniteRepeatable(
      animation = tween(2500, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "heroGlowAlpha"
  )

  val halvingCountdownText = remember(epochNumber) {
    val ep = epochNumber
    if (ep != null) {
      val nextHalvingEpoch = ((ep / 8760L) + 1L) * 8760L
      val epochsRemaining = nextHalvingEpoch - ep
      val hoursRemaining = epochsRemaining * 4L
      val daysRemaining = hoursRemaining / 24L
      val remainingHours = hoursRemaining % 24L
      
      // Calculate estimated date & time (UTC)
      val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
      calendar.add(Calendar.HOUR_OF_DAY, hoursRemaining.toInt())
      val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm 'UTC'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
      }
      val estDateStr = sdf.format(calendar.time)

      "~$daysRemaining days, $remainingHours hours\nEst. Date: $estDateStr (at Epoch #$nextHalvingEpoch)"
    } else {
      "N/A"
    }
  }

  val priceList = remember(priceHistory) {
    priceHistory?.split(",")?.mapNotNull { it.toDoubleOrNull() } ?: emptyList()
  }

  fun loadData(forceRefresh: Boolean = false) {
    isLoading = true
    errorMsg = null
    coroutineScope.launch {
      try {
        val stats = chainStatsRepository.getStats(forceRefresh)
        blockNumber = stats.blockNumber
        epochNumber = stats.epochNumber
        epochProgress = stats.epochProgress
        nodeVersion = stats.nodeVersion
        ckbPrice = stats.ckbPrice
        ckbChange = stats.ckbChange
        ckbMarketCap = stats.ckbMarketCap
        ckbVolume = stats.ckbVolume
        priceHistory = stats.priceHistory
      } catch (e: Exception) {
        errorMsg = "Failed to load data: ${e.localizedMessage}"
      } finally {
        isLoading = false
      }
    }
  }

  // Load configuration updates when settings might have changed
  LaunchedEffect(Unit) {
    rpcUrl = settingsStore.rpcUrl
    rpcNetwork = settingsStore.rpcNetwork
    loadData(forceRefresh = false)
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(16.dp)
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {

    // 1. Hero Logo Card (Premium Graphic Visual Theme)
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp)),
      shape = RoundedCornerShape(16.dp)
    ) {
      Box(
        modifier = Modifier.fillMaxWidth()
      ) {
         Image(
          painter = painterResource(id = currentTheme.heroImageResId),
          contentDescription = null,
          contentScale = ContentScale.Crop,
          modifier = Modifier.matchParentSize()
        )
        Box(
          modifier = Modifier
            .matchParentSize()
            .background(
              Brush.verticalGradient(
                colors = listOf(Color.Black.copy(alpha = 0.3f), Color.Black.copy(alpha = 0.85f))
              )
            )
        )
        // Pulsing glow behind the logo
        Box(
          modifier = Modifier
            .size(180.dp)
            .align(Alignment.TopEnd)
            .background(
              Brush.radialGradient(
                colors = listOf(
                  MaterialTheme.colorScheme.primary.copy(alpha = heroGlowAlpha),
                  Color.Transparent
                )
              )
            )
        )
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
              painter = painterResource(id = R.drawable.logo_transparent),
              contentDescription = "Nervos Logo",
              modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
             Column {
               if (currentTheme.homeHeaderImageResId != null) {
                 Image(
                   painter = painterResource(id = currentTheme.homeHeaderImageResId!!),
                   contentDescription = "CKB Directory",
                   modifier = Modifier.height(60.dp),
                   contentScale = ContentScale.Fit
                 )
               } else {
                 TabHeader(title = "CKB Directory")
               }
             }
          }
          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = "Connected RPC: $rpcUrl",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp
          )
          Text(
            text = "Network: ${rpcNetwork.uppercase()}",
            color = Color(0xFF00CC99),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }

    if (isLoading) {
      Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
      }
    } else if (errorMsg != null) {
      Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(text = errorMsg ?: "", color = MaterialTheme.colorScheme.onErrorContainer)
          Spacer(modifier = Modifier.height(8.dp))
          Button(onClick = { loadData(forceRefresh = true) }) {
            Text("Retry")
          }
        }
      }
    }

    // 2. Latest News Highlight Card
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            text = "Latest News",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = "Fiber Network: Scaling Nervos Network Layer 2 Channels",
          fontWeight = FontWeight.SemiBold,
          style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "Read the latest update on Nervos Talk regarding Fiber Network development, channel mechanics, and next milestones.",
          style = MaterialTheme.typography.bodyMedium,
          color = Color.Gray
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
          onClick = { onNavigate(News) },
          modifier = Modifier.align(Alignment.End)
        ) {
          Text("Go to News")
        }
      }
    }

    // 3. Chain Statistics Card
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Box(
            modifier = Modifier.size(10.dp),
            contentAlignment = Alignment.Center
          ) {
            Box(
              modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(Color(0xFF00CC99).copy(alpha = pulseAlpha))
            )
            Box(
              modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(Color(0xFF00CC99))
            )
          }
          Text(
            text = "Chain Statistics",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
          if (nodeVersion != null) {
            Spacer(modifier = Modifier.weight(1f))
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text(
                text = nodeVersion ?: "",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          // Tip Block Card
          Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(8.dp)
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text(
                text = "Tip Block",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = blockNumber?.let { String.format("%,d", it) } ?: "N/A",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
              )
            }
          }

          // Epoch Number Card
          Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(8.dp)
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text(
                text = "Epoch Number",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = epochNumber?.toString() ?: "N/A",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }

        // Custom Visual Progress Indicator
        val epochProgressParts = remember(epochProgress) {
          epochProgress?.split("/")?.map { it.trim().toFloatOrNull() }
        }
        val currentEpochBlock = epochProgressParts?.getOrNull(0)
        val epochLength = epochProgressParts?.getOrNull(1)
        val epochPercent = if (currentEpochBlock != null && epochLength != null && epochLength > 0f) {
          currentEpochBlock / epochLength
        } else null

        if (epochPercent != null) {
          val progressFraction = epochPercent.coerceIn(0f, 1f)
          Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Epoch Progress",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
              )
              Text(
                text = "${currentEpochBlock!!.toInt()} / ${epochLength!!.toInt()} blocks (${String.format("%.1f", progressFraction * 100)}%)",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
              )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
            ) {
              Box(
                modifier = Modifier
                  .fillMaxWidth(progressFraction)
                  .fillMaxHeight()
                  .clip(RoundedCornerShape(4.dp))
                  .background(
                    Brush.horizontalGradient(
                      colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary
                      )
                    )
                  )
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Next Halving
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)),
          shape = RoundedCornerShape(8.dp)
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Text(
              text = "⏳",
              fontSize = 22.sp
            )
            Column {
              Text(
                text = "Next Halving Countdown",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = halvingCountdownText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
              )
            }
          }
        }
      }
    }

    // 4. Price Summary Card
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = "Price Summary",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = if (ckbPrice != null) "$${String.format("%.6f", ckbPrice)}" else "N/A",
              style = MaterialTheme.typography.headlineMedium,
              fontWeight = FontWeight.Bold
            )
            val change = ckbChange
            if (change != null) {
              val isPositive = change >= 0
              val badgeBgColor = if (isPositive) Color(0xFF00CC99).copy(alpha = 0.15f) else Color.Red.copy(alpha = 0.15f)
              val badgeTextColor = if (isPositive) Color(0xFF00CC99) else Color.Red
              val iconString = if (isPositive) "▲" else "▼"
              
              Box(
                modifier = Modifier
                  .padding(top = 6.dp)
                  .clip(RoundedCornerShape(50.dp))
                  .background(badgeBgColor)
                  .padding(horizontal = 10.dp, vertical = 4.dp)
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Text(
                    text = iconString,
                    color = badgeTextColor,
                    fontSize = 10.sp
                  )
                  Text(
                    text = "${if (isPositive) "+" else ""}${String.format("%.2f", change)}% (24h)",
                    color = badgeTextColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }
          }
        }

        if (priceList.isNotEmpty()) {
          Spacer(modifier = Modifier.height(16.dp))
          PriceSparkline(
            priceList = priceList,
            isPositive = ckbChange?.let { it >= 0 } ?: true,
            modifier = Modifier
              .fillMaxWidth()
              .height(100.dp)
          )
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(text = "7d ago", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Text(text = "Now", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          // Volume Card
          Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(8.dp)
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text(
                text = "24h Volume",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = ckbVolume?.let { "$${String.format("%,.0f", it)}" } ?: "N/A",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
              )
            }
          }

          // Market Cap Card
          Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(8.dp)
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text(
                text = "Market Cap",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = ckbMarketCap?.let { "$${String.format("%,.0f", it)}" } ?: "N/A",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
              )
            }
          }
        }
      }
    }

    Button(
      onClick = { loadData(forceRefresh = true) },
      modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    ) {
      Text("Refresh Dashboard")
    }
  }
}

@Composable
fun PriceSparkline(
  priceList: List<Double>,
  isPositive: Boolean,
  modifier: Modifier = Modifier
) {
  if (priceList.isEmpty()) return

  val minPrice = priceList.minOrNull() ?: 0.0
  val maxPrice = priceList.maxOrNull() ?: 1.0
  val delta = (maxPrice - minPrice).coerceAtLeast(0.00001)

  val sparklineColor = if (isPositive) Color(0xFF00CC99) else Color.Red

  Canvas(modifier = modifier) {
    val width = size.width
    val height = size.height

    val points = priceList.mapIndexed { index, price ->
      val x = (index.toFloat() / (priceList.size - 1)) * width
      val y = height - (((price - minPrice) / delta).toFloat() * height)
      Offset(x, y)
    }

    val path = Path().apply {
      if (points.isNotEmpty()) {
        moveTo(points.first().x, points.first().y)
        for (i in 1 until points.size) {
          lineTo(points[i].x, points[i].y)
        }
      }
    }

    // Draw gradient fill below the path
    val fillPath = Path().apply {
      addPath(path)
      if (points.isNotEmpty()) {
        lineTo(points.last().x, height)
        lineTo(points.first().x, height)
        close()
      }
    }

    drawPath(
      path = fillPath,
      brush = Brush.verticalGradient(
        colors = listOf(
          sparklineColor.copy(alpha = 0.2f),
          Color.Transparent
        )
      )
    )

    // Draw sparkline stroke line
    drawPath(
      path = path,
      color = sparklineColor,
      style = Stroke(
        width = 2.dp.toPx(),
        cap = StrokeCap.Round,
        join = StrokeJoin.Round
      )
    )
  }
}
