package com.quranicwords.app.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quranicwords.app.R

@Composable
fun QwLogo(modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 96.dp) {
    Image(
        painter = painterResource(R.drawable.deany_logo),
        contentDescription = stringResource(R.string.app_logo_content_description),
        modifier = modifier.size(size)
    )
}
