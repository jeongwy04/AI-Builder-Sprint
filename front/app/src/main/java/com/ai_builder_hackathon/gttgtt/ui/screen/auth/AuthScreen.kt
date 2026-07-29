package com.ai_builder_hackathon.gttgtt.ui.screen.auth

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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
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
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.compose.auth.composeAuth
import io.github.jan.supabase.compose.auth.composable.NativeSignInResult
import io.github.jan.supabase.compose.auth.composable.rememberSignInWithGoogle
import com.ai_builder_hackathon.gttgtt.ui.theme.BrandGreen
import com.ai_builder_hackathon.gttgtt.ui.theme.BrandGreenDark
import com.ai_builder_hackathon.gttgtt.ui.theme.GttgttTheme
import com.ai_builder_hackathon.gttgtt.ui.theme.ScreenBackground
import com.ai_builder_hackathon.gttgtt.ui.theme.SurfaceWhite
import com.ai_builder_hackathon.gttgtt.ui.theme.TextPrimary
import com.ai_builder_hackathon.gttgtt.ui.theme.TextSecondary

private val ScreenPadding = 28.dp
private val FieldCorner = 16.dp
private val FieldBorderColor = Color(0xFFE6E7EB)
private val DividerColor = Color(0xFFECEDF1)
private val FieldFontSize = 14.5.sp

/**
 * S0 로그인 화면.
 *
 * ViewModel 은 여기서 주입받고, 실제 UI 는 상태만 받는 [AuthContent] 가 그린다
 * (@Preview 를 Hilt 없이 돌리기 위함 — 다른 화면과 동일 패턴).
 *
 * @param onAuthenticated 구글 로그인 성공 시 한 번 호출된다 (uiState.isAuthenticated 감시).
 *   내비게이션은 상위(NavHost)에서 처리한다.
 */
@Composable
fun AuthScreen(
    onAuthenticated: () -> Unit,
    onSignUpClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // ── 로그인 유지: 저장된 세션이 있으면 로그인 화면을 건너뛴다 ──
    // supabase-kt Auth 가 세션을 기기에 저장하고 자동 갱신한다. 우리는 그 상태만 관찰한다.
    // 확인이 끝나기 전(Initializing)에는 폼 대신 스피너를 보여 깜빡임을 막는다.
    var checkingSession by remember { mutableStateOf(true) }
    var navigated by remember { mutableStateOf(false) }
    fun goNext() {
        if (!navigated) {
            navigated = true
            onAuthenticated()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.supabase.auth.sessionStatus.collect { status ->
            when (status) {
                is SessionStatus.Authenticated -> goNext() // 세션 있음 → 바로 진입
                SessionStatus.Initializing -> Unit         // 저장소에서 불러오는 중 — 스피너 유지
                else -> checkingSession = false            // 세션 없음/만료 → 로그인 폼 표시
            }
        }
    }

    // compose-auth 는 Composable 함수라서 여기서만 만들 수 있다 (AuthViewModel 상단 주석 참고).
    // NativeSignInResult(compose-auth 타입)는 여기서 GoogleSignInOutcome 으로 바꿔서 넘긴다 —
    // 패턴 매칭(is)만 하고 내부 데이터는 안 건드리므로 라이브러리 세부 구조가 바뀌어도 안전하다.
    val googleSignInState = viewModel.supabase.composeAuth.rememberSignInWithGoogle(
        onResult = { result ->
            when (result) {
                is NativeSignInResult.Success ->
                    viewModel.onGoogleResult(GoogleSignInOutcome.SUCCESS)

                is NativeSignInResult.ClosedByUser ->
                    viewModel.onGoogleResult(GoogleSignInOutcome.CANCELLED)

                // Error/NetworkError 내부 필드는 라이브러리 버전마다 달라질 수 있어 굳이 안 읽는다.
                // 어차피 사용자에게 보여줄 메시지는 우리가 정하는 편이 안전하다.
                is NativeSignInResult.Error ->
                    viewModel.onGoogleResult(GoogleSignInOutcome.ERROR, "구글 로그인에 실패했어요.")

                is NativeSignInResult.NetworkError ->
                    viewModel.onGoogleResult(GoogleSignInOutcome.ERROR, "네트워크 연결을 확인해주세요.")
            }
        },
    )

    // 구글 로그인 성공 등으로 isAuthenticated 가 되면 진입. (세션 관찰과 중복돼도 goNext 가 한 번만 실행)
    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) goNext()
    }

    if (checkingSession) {
        // 세션 확인 중 — 로그인 폼 대신 스피너.
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(ScreenBackground),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = BrandGreen)
        }
    } else {
        AuthContent(
            uiState = uiState,
            onEmailChange = viewModel::onEmailChange,
            onPasswordChange = viewModel::onPasswordChange,
            onTogglePasswordVisibility = viewModel::togglePasswordVisibility,
            onToggleKeepSignedIn = viewModel::toggleKeepSignedIn,
            onLoginClick = viewModel::onLoginClick,
            onSocialClick = { provider ->
                if (provider == "google") {
                    viewModel.onGoogleFlowStarted()
                    googleSignInState.startFlow()
                } else {
                    viewModel.onSocialClick(provider)
                }
            },
            onForgotPasswordClick = { /* TODO: 비밀번호 찾기 */ },
            onSignUpClick = onSignUpClick,
            modifier = modifier,
        )
    }
}

@Composable
private fun AuthContent(
    uiState: AuthUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onToggleKeepSignedIn: () -> Unit,
    onLoginClick: () -> Unit,
    onSocialClick: (provider: String) -> Unit,
    onForgotPasswordClick: () -> Unit,
    onSignUpClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .verticalScroll(rememberScrollState()),
    ) {
        Hero()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(SurfaceWhite)
                .padding(horizontal = ScreenPadding, vertical = 28.dp),
        ) {
            Text(
                text = "로그인",
                color = TextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.02).em,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "계정으로 로그인하여 나의 추억을 확인하세요.",
                color = TextSecondary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(Modifier.height(22.dp))

            AuthTextField(
                value = uiState.email,
                onValueChange = onEmailChange,
                placeholder = "이메일을 입력하세요",
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            )

            Spacer(Modifier.height(12.dp))

            AuthTextField(
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

            Spacer(Modifier.height(14.dp))

            OptionsRow(
                keepSignedIn = uiState.keepSignedIn,
                onToggleKeepSignedIn = onToggleKeepSignedIn,
                onForgotPasswordClick = onForgotPasswordClick,
            )

            if (uiState.errorMessage != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = uiState.errorMessage,
                    color = Color(0xFFB64B39),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(Modifier.height(18.dp))

            PrimaryButton(
                text = "로그인",
                enabled = uiState.canSubmit,
                loading = uiState.isSubmitting,
                onClick = onLoginClick,
            )

            Spacer(Modifier.height(24.dp))
            OrDivider()
            Spacer(Modifier.height(20.dp))

            GoogleButton(onClick = { onSocialClick("google") })

            Spacer(Modifier.height(24.dp))
            SignUpPrompt(onSignUpClick = onSignUpClick)
        }
    }
}

/** 상단 브랜드/카피 영역. 시안의 3D 일러스트 자리는 아직 에셋이 없어 텍스트만 둔다. */
@Composable
private fun Hero() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenPadding, vertical = 40.dp),
    ) {
        Text(
            text = "그때그때",
            color = BrandGreen,
            fontSize = 40.sp,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            letterSpacing = (-0.04).em,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "다시 함께하는 순간들",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "기록하고, 남기고, 추억을 모아보세요",
            color = BrandGreenDark,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/**
 * 라운드 입력 필드. Material3 TextField 는 라벨·밑줄·기본 패딩이 시안과 안 맞아
 * BasicTextField 로 직접 그린다 (SearchField 와 동일 방침).
 */
@Composable
private fun AuthTextField(
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
private fun OptionsRow(
    keepSignedIn: Boolean,
    onToggleKeepSignedIn: () -> Unit,
    onForgotPasswordClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable(onClick = onToggleKeepSignedIn),
        ) {
            Checkbox(
                checked = keepSignedIn,
                onCheckedChange = { onToggleKeepSignedIn() },
                colors = CheckboxDefaults.colors(
                    checkedColor = BrandGreen,
                    uncheckedColor = TextSecondary,
                    checkmarkColor = SurfaceWhite,
                ),
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = "로그인 상태 유지",
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(
            text = "비밀번호 찾기 ›",
            color = BrandGreenDark,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable(onClick = onForgotPasswordClick),
        )
    }
}

@Composable
private fun PrimaryButton(
    text: String,
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
                text = text,
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

/** 구글로 계속 — 구글 로고 + 텍스트 (구글 단독). */
@Composable
private fun GoogleButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FieldCorner))
            .border(1.dp, FieldBorderColor, RoundedCornerShape(FieldCorner))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_google),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.size(10.dp))
        Text(
            text = "Google로 계속",
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SignUpPrompt(onSignUpClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "아직 계정이 없으신가요?",
            color = TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.size(6.dp))
        Text(
            text = "회원가입하기 ›",
            color = BrandGreenDark,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable(onClick = onSignUpClick),
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun AuthContentPreview() {
    GttgttTheme {
        AuthContent(
            uiState = AuthUiState(email = "me@example.com", password = "secret"),
            onEmailChange = {},
            onPasswordChange = {},
            onTogglePasswordVisibility = {},
            onToggleKeepSignedIn = {},
            onLoginClick = {},
            onSocialClick = {},
            onForgotPasswordClick = {},
            onSignUpClick = {},
        )
    }
}
