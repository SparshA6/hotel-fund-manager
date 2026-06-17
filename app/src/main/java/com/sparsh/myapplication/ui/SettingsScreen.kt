package com.sparsh.myapplication.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sparsh.myapplication.SettingsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var taxRateStr by remember { mutableStateOf(SettingsManager.getTaxRate(context).toString()) }
    var tdsRateStr by remember { mutableStateOf(SettingsManager.getTdsRate(context).toString()) }
    var tcsRateStr by remember { mutableStateOf(SettingsManager.getTcsRate(context).toString()) }

    val platforms = listOf("MMT", "Booking.com", "Agoda", "Goibibo", "Cleartrip")
    val commissionRates = remember {
        mutableStateMapOf<String, String>().apply {
            platforms.forEach { platform ->
                put(platform, SettingsManager.getCommissionRate(context, platform).toString())
            }
        }
    }

    var showSaveSuccessAlert by remember { mutableStateOf(false) }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
            MaterialTheme.colorScheme.surface
        )
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent,
        modifier = modifier.background(backgroundGradient)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Success alert
            if (showSaveSuccessAlert) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Settings saved successfully!",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // General Rates Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Tax & Government Deductions",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedTextField(
                            value = taxRateStr,
                            onValueChange = { taxRateStr = it },
                            label = { Text("Online Portal Tax Rate") },
                            suffix = { Text("%") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = tdsRateStr,
                            onValueChange = { tdsRateStr = it },
                            label = { Text("TDS Rate") },
                            suffix = { Text("%") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = tcsRateStr,
                            onValueChange = { tcsRateStr = it },
                            label = { Text("TCS Rate") },
                            suffix = { Text("%") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Platform Commission Rates Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Platform Commission Rates",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        platforms.forEach { platform ->
                            OutlinedTextField(
                                value = commissionRates[platform] ?: "",
                                onValueChange = { commissionRates[platform] = it },
                                label = { Text("$platform Commission") },
                                suffix = { Text("%") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Save Button
            item {
                Button(
                    onClick = {
                        val tax = taxRateStr.toFloatOrNull() ?: 5.0f
                        val tds = tdsRateStr.toFloatOrNull() ?: 0.5f
                        val tcs = tcsRateStr.toFloatOrNull() ?: 0.1f

                        SettingsManager.setTaxRate(context, tax)
                        SettingsManager.setTdsRate(context, tds)
                        SettingsManager.setTcsRate(context, tcs)

                        platforms.forEach { platform ->
                            val comm = commissionRates[platform]?.toFloatOrNull() ?: 0.0f
                            SettingsManager.setCommissionRate(context, platform, comm)
                        }

                        // Re-sync UI text fields with saved values
                        taxRateStr = SettingsManager.getTaxRate(context).toString()
                        tdsRateStr = SettingsManager.getTdsRate(context).toString()
                        tcsRateStr = SettingsManager.getTcsRate(context).toString()
                        platforms.forEach { p ->
                            commissionRates[p] = SettingsManager.getCommissionRate(context, p).toString()
                        }

                        showSaveSuccessAlert = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Settings", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
