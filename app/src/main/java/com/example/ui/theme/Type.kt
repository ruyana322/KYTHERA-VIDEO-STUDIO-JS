package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val InterFamily = FontFamily(
  Font(R.font.inter, FontWeight.Normal),
  Font(R.font.inter, FontWeight.Medium),
  Font(R.font.inter, FontWeight.SemiBold),
  Font(R.font.inter, FontWeight.Bold),
  Font(R.font.inter, FontWeight.Black),
)

val PoppinsFamily = FontFamily(
  Font(R.font.poppins, FontWeight.Normal),
  Font(R.font.poppins, FontWeight.Medium),
  Font(R.font.poppins, FontWeight.SemiBold),
  Font(R.font.poppins, FontWeight.Bold),
)

val PlusJakartaSansFamily = FontFamily(
  Font(R.font.plus_jakarta_sans, FontWeight.Normal),
  Font(R.font.plus_jakarta_sans, FontWeight.Medium),
  Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
  Font(R.font.plus_jakarta_sans, FontWeight.Bold),
)

val JetBrainsMonoFamily = FontFamily(
  Font(R.font.jetbrains_mono, FontWeight.Normal),
  Font(R.font.jetbrains_mono, FontWeight.Medium),
  Font(R.font.jetbrains_mono, FontWeight.Bold),
)

val KytheraTypography = Typography(
  displaySmall = TextStyle(
    fontFamily = InterFamily,
    fontWeight = FontWeight.Black,
    fontSize = 28.sp,
    lineHeight = 34.sp,
    letterSpacing = (-0.5).sp,
  ),
  headlineSmall = TextStyle(
    fontFamily = InterFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 22.sp,
    lineHeight = 28.sp,
    letterSpacing = (-0.3).sp,
  ),
  titleLarge = TextStyle(
    fontFamily = InterFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 18.sp,
    lineHeight = 24.sp,
  ),
  titleMedium = TextStyle(
    fontFamily = PoppinsFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 15.sp,
    lineHeight = 20.sp,
  ),
  bodyLarge = TextStyle(
    fontFamily = PoppinsFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 20.sp,
  ),
  bodyMedium = TextStyle(
    fontFamily = PoppinsFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    lineHeight = 18.sp,
  ),
  bodySmall = TextStyle(
    fontFamily = PoppinsFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 11.5.sp,
    lineHeight = 16.sp,
  ),
  labelLarge = TextStyle(
    fontFamily = InterFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 13.sp,
    lineHeight = 18.sp,
  ),
  labelMedium = TextStyle(
    fontFamily = PoppinsFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 11.5.sp,
    lineHeight = 15.sp,
  ),
  labelSmall = TextStyle(
    fontFamily = JetBrainsMonoFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 10.5.sp,
    lineHeight = 14.sp,
  ),
)
