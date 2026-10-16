package com.cosmin.trackmyfood.ui.screens

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cosmin.trackmyfood.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val bitmap by viewModel.currentBitmap.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val name by viewModel.draftMealName.collectAsState()
    val description by viewModel.draftDescription.collectAsState()
    val ingredients by viewModel.draftIngredients.collectAsState()

    var newIngredientInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review & Modify Meal") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        if (isAnalyzing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Gemini is analyzing your meal photo...", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Photo Preview & Scan Again Button
                if (bitmap != null) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                elevation = CardDefaults.cardElevation(4.dp)
                            ) {
                                Image(
                                    bitmap = bitmap!!.asImageBitmap(),
                                    contentDescription = "Meal Photo",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    viewModel.analyzeCurrentBitmap()
                                    Toast.makeText(context, "Re-scanning meal photo...", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Scan Again with AI")
                            }
                        }
                    }
                }

                // Name field
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { viewModel.updateDraftName(it) },
                        label = { Text("Meal Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // Description field
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { viewModel.updateDraftDescription(it) },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }

                // Ingredients header & add input
                item {
                    Text(
                        text = "Ingredients (Editable)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newIngredientInput,
                            onValueChange = { newIngredientInput = it },
                            label = { Text("Add new ingredient") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (newIngredientInput.isNotBlank()) {
                                    viewModel.addIngredient(newIngredientInput)
                                    newIngredientInput = ""
                                }
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Ingredient")
                        }
                    }
                }

                // Ingredients editable list
                itemsIndexed(ingredients) { index, ingredient ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = ingredient,
                            onValueChange = { newText -> viewModel.updateIngredient(index, newText) },
                            label = { Text("Ingredient ${index + 1}") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = { viewModel.removeIngredient(index) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                // Save button
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            viewModel.saveCurrentMeal {
                                Toast.makeText(context, "Meal saved & ingredients updated!", Toast.LENGTH_SHORT).show()
                                onBack()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(text = "Accept & Persist to Database", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
