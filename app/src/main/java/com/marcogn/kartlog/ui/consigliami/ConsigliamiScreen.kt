package com.marcogn.kartlog.ui.consigliami

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.marcogn.kartlog.R
import com.marcogn.kartlog.ui.common.PlaceholderScreen

@Composable
fun ConsigliamiScreen(onMenuClick: () -> Unit) {
    PlaceholderScreen(title = stringResource(R.string.consigliami_title), onMenuClick = onMenuClick)
}
