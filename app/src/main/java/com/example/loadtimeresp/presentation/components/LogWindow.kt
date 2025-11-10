package com.example.loadtimeresp.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loadtimeresp.presentation.viewmodels.MainViewModel

@Composable
fun LogWindow(viewModel: MainViewModel) {
    val logs by viewModel.logList.collectAsState()
    val listState = rememberLazyListState()

    // auto scroll to bottom when logs change
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.scrollToItem(logs.lastIndex)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .background(Color(0xFF111111))
            .padding(8.dp)
    ) {
        LazyColumn(state = listState) {
            items(logs) { line ->
                Text(
                    text = line,
                    color = Color.Green,
                    fontSize = 12.sp
                )
            }
        }
    }
}
