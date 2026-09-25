package com.marcogn.kartlog.ui.skin

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.marcogn.kartlog.R
import com.marcogn.kartlog.ui.common.PlaceholderScreen

@Composable
fun SkinScreen(onMenuClick: () -> Unit) {
    PlaceholderScreen(title = stringResource(R.string.skin_title), onMenuClick = onMenuClick)
}
