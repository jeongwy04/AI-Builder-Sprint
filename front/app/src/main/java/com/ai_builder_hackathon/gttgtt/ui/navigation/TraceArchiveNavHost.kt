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
import com.ai_builder_hackathon.gttgtt.ui.screen.memorylist.MemoryListKind
import com.ai_builder_hackathon.gttgtt.ui.screen.memorylist.MemoryListScreen
import com.ai_builder_hackathon.gttgtt.ui.screen.mypage.MyPageScreen
import com.ai_builder_hackathon.gttgtt.ui.screen.signup.SignUpScreen

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
        // 구글 로그인이 붙어서 다시 Route.Auth 로 시작한다.
        startDestination = Route.Auth,
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
                onSignUpClick = { navController.navigate(Route.SignUp) },
            )
        }

        composable<Route.SignUp> {
            SignUpScreen(
                onBackClick = { navController.popBackStack() },
                onSignedUp = {
                    navController.navigate(Route.GroupList) {
                        // 로그인/회원가입 화면을 백스택에서 제거 → 뒤로가기로 안 돌아옴
                        popUpTo(Route.Auth) { inclusive = true }
                    }
                },
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
                onMyMemoriesClick = { navController.navigate(Route.MyMemories) },
                onLikedClick = { navController.navigate(Route.LikedMemories) },
                onSignedOut = {
                    // 로그아웃하면 뒤로가기로 되돌아올 수 없어야 한다.
                    navController.navigate(Route.Auth) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
            )
        }

        composable<Route.MyMemories> {
            MemoryListScreen(
                kind = MemoryListKind.MINE,
                onBackClick = { navController.popBackStack() },
                onMemoryClick = { memoryId -> navController.navigate(Route.MemoryDetail(memoryId)) },
            )
        }

        composable<Route.LikedMemories> {
            MemoryListScreen(
                kind = MemoryListKind.LIKED,
                onBackClick = { navController.popBackStack() },
                onMemoryClick = { memoryId -> navController.navigate(Route.MemoryDetail(memoryId)) },
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
            MemoryDetailScreen(
                onBackClick = { navController.popBackStack() },
                onEditClick = { archiveId, memoryId ->
                    navController.navigate(Route.MemoryCreate(archiveId = archiveId, memoryId = memoryId))
                },
                onDeleted = { navController.popBackStack() },
            )
        }

        composable<Route.MemoryCreate> { entry ->
            val isEditMode = entry.toRoute<Route.MemoryCreate>().memoryId != null
            MemoryCreateScreen(
                onBackClick = { navController.popBackStack() },
                onSaved = { memoryId ->
                    if (isEditMode) {
                        // 수정 화면 밑에는 "수정 전" 상세 화면이 그대로 깔려 있다 —
                        // 그걸 남겨두면 저장 후 뒤로가기를 눌렀을 때 수정 전 내용이 다시 보인다.
                        // 같은 memoryId 의 상세 화면까지 걷어내고 새로 고친 화면 하나만 남긴다.
                        navController.navigate(Route.MemoryDetail(memoryId)) {
                            popUpTo(Route.MemoryDetail(memoryId)) { inclusive = true }
                        }
                    } else {
                        // 작성 화면을 백스택에서 걷어내고 방금 만든 기억으로 이동한다.
                        // 뒤로가기로 작성 폼에 되돌아오면 안 되기 때문.
                        navController.popBackStack()
                        navController.navigate(Route.MemoryDetail(memoryId))
                    }
                },
            )
        }
    }
}
