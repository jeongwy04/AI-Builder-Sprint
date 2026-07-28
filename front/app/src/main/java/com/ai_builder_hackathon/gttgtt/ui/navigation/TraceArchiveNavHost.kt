package com.ai_builder_hackathon.gttgtt.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.ai_builder_hackathon.gttgtt.ui.screen.groupchat.GroupChatScreen
import com.ai_builder_hackathon.gttgtt.ui.screen.groupfeed.GroupFeedScreen
import com.ai_builder_hackathon.gttgtt.ui.screen.grouplist.GroupListScreen
import com.ai_builder_hackathon.gttgtt.ui.screen.memorydetail.MemoryDetailScreen
import com.ai_builder_hackathon.gttgtt.ui.screen.mypage.MyPageScreen

/**
 * 화면 그래프. 각 composable 블록의 내용은 디자인 시안이 확정되는 대로
 * ui/screen/<화면>/XxxScreen.kt 호출로 교체한다.
 */
@Composable
fun TraceArchiveNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        // 로그인 붙기 전까지 목록 화면을 바로 띄운다. 인증 구현 시 Route.Auth 로 되돌린다.
        startDestination = Route.GroupList,
        modifier = modifier,
    ) {
        composable<Route.Auth> {
            Placeholder("S0 로그인") { navController.navigate(Route.GroupList) }
        }

        composable<Route.GroupList> {
            GroupListScreen(
                onGroupClick = { archiveId -> navController.navigate(Route.GroupFeed(archiveId)) },
                onProfileClick = { navController.navigate(Route.MyPage) },
            )
        }

        composable<Route.MyPage> {
            MyPageScreen(
                onBackClick = { navController.popBackStack() },
                onSignedOut = {
                    // 로그아웃하면 뒤로가기로 되돌아올 수 없어야 한다.
                    navController.navigate(Route.Auth) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
            )
        }

        composable<Route.GroupFeed> { entry ->
            val archiveId = entry.toRoute<Route.GroupFeed>().archiveId
            GroupFeedScreen(
                onBackClick = { navController.popBackStack() },
                onChatClick = { navController.navigate(Route.GroupChat(archiveId)) },
                onMemoryClick = { memoryId -> navController.navigate(Route.MemoryDetail(memoryId)) },
            )
        }

        composable<Route.GroupChat> {
            GroupChatScreen(onBackClick = { navController.popBackStack() })
        }

        composable<Route.Timeline> {
            Placeholder("S5 타임라인") { navController.popBackStack() }
        }

        composable<Route.MemoryDetail> {
            MemoryDetailScreen(onBackClick = { navController.popBackStack() })
        }

        composable<Route.MemoryCreate> {
            Placeholder("S4 기억 작성") { navController.popBackStack() }
        }
    }
}

@Composable
private fun Placeholder(label: String, onNext: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = label, style = MaterialTheme.typography.headlineSmall)
        TextButton(onClick = onNext) { Text("뒤로") }
    }
}
