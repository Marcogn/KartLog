package com.marcogn.kartlog.ui.medallions

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.marcogn.kartlog.R
import com.marcogn.kartlog.ui.common.PlaceholderScreen

@Composable
fun PeachMedallionsScreen(onMenuClick: () -> Unit) {
    PlaceholderScreen(title = stringResource(R.string.medallions_title), onMenuClick = onMenuClick)
}
