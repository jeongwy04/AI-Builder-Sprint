package com.ai_builder_hackathon.gttgtt.ui.screen.signup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ai_builder_hackathon.gttgtt.R
import com.ai_builder_hackathon.gttgtt.ui.component.TopBarButton
import com.ai_builder_hackathon.gttgtt.ui.theme.BrandGreen
import com.ai_builder_hackathon.gttgtt.ui.theme.GttgttTheme
import com.ai_builder_hackathon.gttgtt.ui.theme.ScreenBackground
import com.ai_builder_hackathon.gttgtt.ui.theme.SurfaceWhite
import com.ai_builder_hackathon.gttgtt.ui.theme.TextPrimary
import com.ai_builder_hackathon.gttgtt.ui.theme.TextSecondary
import com.ai_builder_hackathon.gttgtt.ui.theme.TopBarIcon
import io.github.jan.supabase.compose.auth.composeAuth
import io.github.jan.supabase.compose.auth.composable.NativeSignInResult
import io.github.jan.supabase.compose.auth.composable.rememberSignInWithGoogle
import kotlinx.coroutines.launch

private val ScreenPadding = 28.dp
private val FieldCorner = 16.dp
private val FieldBorderColor = Color(0xFFE6E7EB)
private val DividerColor = Color(0xFFECEDF1)
private val ErrorColor = Color(0xFFB64B39)
private val FieldFontSize = 14.5.sp
private const val NICKNAME_MAX_LENGTH = 20

/**
 * S0-B 회원가입 화면.
 *
 * 닉네임을 받고, 구글 계정 또는 이메일/비밀번호 중 하나로 인증한다.
 *
 * @param onSignedUp 닉네임 저장까지 끝나고 나면 한 번 호출된다 (uiState.isSignedUp 감시).
 *   내비게이션은 상위(NavHost)에서 처리한다.
 */
@Composable
fun SignUpScreen(
    onBackClick: () -> Unit,
    onSignedUp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SignUpViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    // AuthScreen 과 같은 이유 — compose-auth 는 Composable 안에서만 만들 수 있다.
    val googleSignInState = viewModel.supabase.composeAuth.rememberSignInWithGoogle(
        onResult = { result ->
            when (result) {
                is NativeSignInResult.Success ->
                    viewModel.onGoogleResult(SignUpGoogleOutcome.SUCCESS)

                is NativeSignInResult.ClosedByUser ->
                    viewModel.onGoogleResult(SignUpGoogleOutcome.CANCELLED)

                is NativeSignInResult.Error ->
                    viewModel.onGoogleResult(SignUpGoogleOutcome.ERROR, "구글 회원가입에 실패했어요.")

                is NativeSignInResult.NetworkError ->
                    viewModel.onGoogleResult(SignUpGoogleOutcome.ERROR, "네트워크 연결을 확인해주세요.")
            }
        },
    )

    LaunchedEffect(uiState.isSignedUp) {
        if (uiState.isSignedUp) onSignedUp()
    }

    SignUpContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onNicknameChange = viewModel::onNicknameChange,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onTogglePasswordVisibility = viewModel::togglePasswordVisibility,
        onEmailSignUpClick = viewModel::onEmailSignUpClick,
        onGoogleSignUpClick = {
            scope.launch {
                if (viewModel.canStartGoogleSignUp()) {
                    googleSignInState.startFlow()
                }
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun SignUpContent(
    uiState: SignUpUiState,
    onBackClick: () -> Unit,
    onNicknameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onEmailSignUpClick: () -> Unit,
    onGoogleSignUpClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            TopBarButton(
                iconRes = R.drawable.ic_chevron_left,
                contentDescription = "뒤로",
                background = SurfaceWhite,
                tint = TopBarIcon,
                onClick = onBackClick,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ScreenPadding, vertical = 12.dp),
        ) {
            Text(
                text = "회원가입",
                color = TextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.02).em,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "닉네임을 정하고, 구글 계정 또는 이메일로 가입하세요.",
                color = TextSecondary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(Modifier.height(28.dp))

            Text(
                text = "닉네임",
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.height(8.dp))
            NicknameField(
                value = uiState.nickname,
                onValueChange = { if (it.length <= NICKNAME_MAX_LENGTH) onNicknameChange(it) },
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "그룹 멤버들에게 이 이름으로 보여요. (${uiState.nickname.length}/$NICKNAME_MAX_LENGTH)",
                color = TextSecondary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = "이메일",
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.height(8.dp))
            SignUpTextField(
                value = uiState.email,
                onValueChange = onEmailChange,
                placeholder = "이메일을 입력하세요",
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            )

            Spacer(Modifier.height(12.dp))

            SignUpTextField(
                value = uiState.password,
                onValueChange = onPasswordChange,
                placeholder = "비밀번호를 입력하세요",
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                visualTransformation = if (uiState.passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailing = {
                    Text(
                        text = if (uiState.passwordVisible) "숨김" else "표시",
                        color = TextSecondary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(onClick = onTogglePasswordVisibility),
                    )
                },
            )

            if (uiState.errorMessage != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = uiState.errorMessage,
                    color = ErrorColor,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            if (uiState.confirmEmailMessage != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = uiState.confirmEmailMessage,
                    color = TextSecondary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(Modifier.height(18.dp))

            EmailSignUpButton(
                enabled = uiState.canSubmitEmail,
                loading = uiState.isSubmitting,
                onClick = onEmailSignUpClick,
            )

            Spacer(Modifier.height(20.dp))
            OrDivider()
            Spacer(Modifier.height(20.dp))

            GoogleSignUpButton(
                enabled = uiState.canSubmitGoogle,
                loading = uiState.isSubmitting,
                onClick = onGoogleSignUpClick,
            )
        }
    }
}

@Composable
private fun NicknameField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FieldCorner))
            .background(SurfaceWhite)
            .border(1.dp, FieldBorderColor, RoundedCornerShape(FieldCorner))
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        if (value.isEmpty()) {
            Text(
                text = "다른 사람에게 보일 이름을 입력하세요",
                color = TextSecondary,
                fontSize = FieldFontSize,
                fontWeight = FontWeight.SemiBold,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(
                color = TextPrimary,
                fontSize = FieldFontSize,
                fontWeight = FontWeight.SemiBold,
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * 라운드 입력 필드. AuthScreen 의 AuthTextField 와 동일한 시안이지만, 그쪽이 private 라서
 * 여기 따로 둔다.
 */
@Composable
private fun SignUpTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FieldCorner))
            .background(SurfaceWhite)
            .border(1.dp, FieldBorderColor, RoundedCornerShape(FieldCorner))
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    color = TextSecondary,
                    fontSize = FieldFontSize,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                visualTransformation = visualTransformation,
                textStyle = LocalTextStyle.current.copy(
                    color = TextPrimary,
                    fontSize = FieldFontSize,
                    fontWeight = FontWeight.SemiBold,
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType,
                    imeAction = imeAction,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        trailing?.invoke()
    }
}

@Composable
private fun EmailSignUpButton(
    enabled: Boolean,
    loading: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(FieldCorner))
            .background(if (enabled) BrandGreen else BrandGreen.copy(alpha = 0.4f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(
                color = SurfaceWhite,
                strokeWidth = 2.dp,
                modifier = Modifier.size(22.dp),
            )
        } else {
            Text(
                text = "이메일로 가입",
                color = SurfaceWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun OrDivider() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(DividerColor),
        )
        Text(
            text = "또는",
            color = TextSecondary,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(DividerColor),
        )
    }
}

@Composable
private fun GoogleSignUpButton(
    enabled: Boolean,
    loading: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(FieldCorner))
            .border(1.dp, FieldBorderColor, RoundedCornerShape(FieldCorner))
            .background(SurfaceWhite)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(
                color = BrandGreen,
                strokeWidth = 2.dp,
                modifier = Modifier.size(22.dp),
            )
        } else {
            Text(
                text = "Google로 회원가입",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun SignUpContentPreview() {
    GttgttTheme {
        SignUpContent(
            uiState = SignUpUiState(nickname = "민지"),
            onBackClick = {},
            onNicknameChange = {},
            onEmailChange = {},
            onPasswordChange = {},
            onTogglePasswordVisibility = {},
            onEmailSignUpClick = {},
            onGoogleSignUpClick = {},
        )
    }
}
