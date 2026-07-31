package com.ai_builder_hackathon.gttgtt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.ai_builder_hackathon.gttgtt.ui.navigation.TraceArchiveNavHost
import com.ai_builder_hackathon.gttgtt.ui.theme.GttgttTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GttgttTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // ⚠️ innerPadding(상태바+내비게이션바)을 여백으로만 주고 끝내면 안 된다 —
                    // GroupFeedScreen/GroupChatScreen/MemoryDetailScreen 이 각자 화면 안에서
                    // 따로 imePadding() 을 붙이는데(키보드 위로 밀어 올리려고), consumeWindowInsets 없이는
                    // Compose가 이 innerPadding.bottom(내비게이션바 높이)이 이미 반영됐다는 걸 몰라서
                    // imePadding() 이 키보드 높이 전체를 또 더한다. 그러면 입력창과 실제 키보드 사이에
                    // 내비게이션바 높이만큼 빈 틈(반투명하게 보이는 부분)이 남는다.
                    TraceArchiveNavHost(
                        modifier = Modifier
                            .padding(innerPadding)
                            .consumeWindowInsets(innerPadding)
                    )
                }
            }
        }
    }
}
