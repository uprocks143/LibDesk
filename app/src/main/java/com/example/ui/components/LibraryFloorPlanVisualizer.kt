package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.FloorElementEntity
import com.example.ui.theme.LibDeskColors
import com.example.viewmodel.LibDeskViewModel

@Composable
fun LibraryFloorPlanVisualizer(
    viewModel: LibDeskViewModel,
    onSeatSelected: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val floorElements by viewModel.floorElements.collectAsState()
    val seats by viewModel.seats.collectAsState()

    var selectedElement by remember { mutableStateOf<FloorElementEntity?>(null) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Layers,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Interactive 2D Hall Floor Map", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                // Legend
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LegendIndicator(color = Color(0xFF10B981), label = "Available")
                    LegendIndicator(color = Color(0xFFEF4444), label = "Occupied")
                    LegendIndicator(color = Color(0xFF38BDF8), label = "Facility")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2D Grid Representation (8 columns x 6 rows)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(8),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val totalCells = 48
                    items(totalCells) { index ->
                        val x = index % 8
                        val y = index / 8
                        val element = floorElements.find { it.gridX == x && it.gridY == y }
                        
                        FloorGridCell(
                            element = element,
                            onClick = {
                                if (element != null) {
                                    selectedElement = element
                                    if (element.type == "SEAT" && element.seatNumberRef.isNotBlank()) {
                                        onSeatSelected(element.seatNumberRef)
                                    }
                                }
                            }
                        )
                    }
                }
            }

            if (selectedElement != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Selected: ${selectedElement!!.label}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Type: ${selectedElement!!.type} (Pos: ${selectedElement!!.gridX}, ${selectedElement!!.gridY})", fontSize = 11.sp)
                        }
                        IconButton(onClick = { selectedElement = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Deselect", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FloorGridCell(
    element: FloorElementEntity?,
    onClick: () -> Unit
) {
    if (element == null) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .background(Color.Transparent, RoundedCornerShape(4.dp))
                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
        )
    } else {
        val (bgColor, icon, labelColor) = when (element.type) {
            "DOOR" -> Triple(Color(0xFF64748B), Icons.Default.MeetingRoom, Color.White)
            "RECEPTION" -> Triple(Color(0xFF8B5CF6), Icons.Default.Desk, Color.White)
            "AC" -> Triple(Color(0xFF0EA5E9), Icons.Default.AcUnit, Color.White)
            "WATER_COOLER" -> Triple(Color(0xFF06B6D4), Icons.Default.WaterDrop, Color.White)
            "RESTROOM" -> Triple(Color(0xFF475569), Icons.Default.Wc, Color.White)
            "SEAT" -> {
                if (element.isOccupied) Triple(Color(0xFFEF4444), Icons.Default.Person, Color.White)
                else Triple(Color(0xFF10B981), Icons.Default.Chair, Color.White)
            }
            else -> Triple(MaterialTheme.colorScheme.primary, Icons.Default.Square, Color.White)
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(6.dp))
                .background(bgColor)
                .clickable(onClick = onClick)
                .padding(2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = element.label,
                tint = labelColor,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun LegendIndicator(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
