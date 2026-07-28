package com.ai_builder_hackathon.gttgtt.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.ai_builder_hackathon.gttgtt.ui.screen.auth.AuthScreen
import com.ai_builder_hackathon.gttgtt.ui.screen.groupchat.GroupChatScreen
import com.ai_builder_hackathon.gttgtt.ui.screen.groupfeed.GroupFeedScreen
import com.ai_builder_hackathon.gttgtt.ui.screen.grouplist.GroupListScreen
import com.ai_builder_hackathon.gttgtt.ui.screen.memorycreate.MemoryCreateScreen
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
            AuthScreen(
                onAuthenticated = {
                    navController.navigate(Route.GroupList) {
                        // 로그인 화면을 백스택에서 제거 → 뒤로가기로 로그인에 안 돌아옴
                        popUpTo(Route.Auth) { inclusive = true }
                    }
                },
                onSignUpClick = { /* TODO: 회원가입 화면 */ },
            )
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
                onCreateMemoryClick = { navController.navigate(Route.MemoryCreate(archiveId)) },
            )
        }

        composable<Route.GroupChat> {
            GroupChatScreen(onBackClick = { navController.popBackStack() })
        }

        composable<Route.MemoryDetail> {
            MemoryDetailScreen(onBackClick = { navController.popBackStack() })
        }

        composable<Route.MemoryCreate> {
            MemoryCreateScreen(
                onBackClick = { navController.popBackStack() },
                onSaved = { memoryId ->
                    // 작성 화면을 백스택에서 걷어내고 방금 만든 기억으로 이동한다.
                    // 뒤로가기로 작성 폼에 되돌아오면 안 되기 때문.
                    navController.popBackStack()
                    navController.navigate(Route.MemoryDetail(memoryId))
                },
            )
        }
    }
}
