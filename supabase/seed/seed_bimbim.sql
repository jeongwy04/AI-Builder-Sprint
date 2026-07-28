-- ============================================================================
-- 데모 시드 — 그룹 '삠삠'에 기억 16개 + 자연스러운 문장 메모
--
-- 실행: Supabase 대시보드 → SQL Editor 에 붙여넣고 Run (service_role 권한이라 RLS 우회).
-- 전제: 앱에서 '삠삠' 그룹을 이미 만들었고, bswoojin@gmail.com 이 그 멤버다.
--
-- ⚠️ 검색의 핵심은 사진이 아니라 여기 notes 의 "문장"이다 (CLAUDE.md 핵심 명제).
--    이 SQL 로 memories + notes 를 심은 뒤, embed_bimbim.sh 로 각 기억을 임베딩해야
--    검색에 잡힌다 (search_text/embedding 은 embed-memory 가 채운다 — §5.6).
-- ============================================================================
do $$
declare
    v_archive uuid := (
        select id from public.archives
        where name = '삠삠'
        order by created_at
        limit 1
    );
    v_author uuid := (
        select id from auth.users
        where email = 'bswoojin@gmail.com'
        limit 1
    );
begin
    if v_archive is null then
        raise exception '''삠삠'' 그룹을 찾을 수 없습니다. 앱에서 먼저 그룹을 만드세요.';
    end if;
    if v_author is null then
        raise exception 'bswoojin@gmail.com 사용자를 찾을 수 없습니다. 먼저 로그인하세요.';
    end if;

    -- 기억(게시물) 16개. id 를 고정해 아래 notes 와 embed 스크립트가 참조한다.
    insert into public.memories (id, archive_id, author_id, memory_date, place_name) values
        ('bbbbbbbb-bbbb-bbbb-bbbb-000000000001', v_archive, v_author, '2025-08-14', '강릉 경포해변'),
        ('bbbbbbbb-bbbb-bbbb-bbbb-000000000002', v_archive, v_author, '2025-04-06', '여의도 한강공원'),
        ('bbbbbbbb-bbbb-bbbb-bbbb-000000000003', v_archive, v_author, '2024-12-21', '남산서울타워'),
        ('bbbbbbbb-bbbb-bbbb-bbbb-000000000004', v_archive, v_author, '2025-05-25', '가평 캠핑장'),
        ('bbbbbbbb-bbbb-bbbb-bbbb-000000000005', v_archive, v_author, '2025-03-15', '홍대 카페'),
        ('bbbbbbbb-bbbb-bbbb-bbbb-000000000006', v_archive, v_author, '2025-10-18', '북한산'),
        ('bbbbbbbb-bbbb-bbbb-bbbb-000000000007', v_archive, v_author, '2025-06-30', '올림픽공원'),
        ('bbbbbbbb-bbbb-bbbb-bbbb-000000000008', v_archive, v_author, '2025-01-01', '할머니 댁'),
        ('bbbbbbbb-bbbb-bbbb-bbbb-000000000009', v_archive, v_author, '2025-07-12', '동네 공원'),
        ('bbbbbbbb-bbbb-bbbb-bbbb-000000000010', v_archive, v_author, '2024-11-02', '여의도 한강공원'),
        ('bbbbbbbb-bbbb-bbbb-bbbb-000000000011', v_archive, v_author, '2025-09-20', '광장시장'),
        ('bbbbbbbb-bbbb-bbbb-bbbb-000000000012', v_archive, v_author, '2025-02-28', '회사'),
        ('bbbbbbbb-bbbb-bbbb-bbbb-000000000013', v_archive, v_author, '2024-02-16', '학교 대운동장'),
        ('bbbbbbbb-bbbb-bbbb-bbbb-000000000014', v_archive, v_author, '2025-05-05', '에버랜드'),
        ('bbbbbbbb-bbbb-bbbb-bbbb-000000000015', v_archive, v_author, '2025-08-01', '제주도 성산일출봉'),
        ('bbbbbbbb-bbbb-bbbb-bbbb-000000000016', v_archive, v_author, '2025-12-24', '우리 집');

    -- 각 기억의 메모(이야기). 검색의 유일한 재료다.
    insert into public.notes (memory_id, archive_id, author_id, body) values
        ('bbbbbbbb-bbbb-bbbb-bbbb-000000000001', v_archive, v_author,
         '경포 바다 진짜 미쳤다. 파도 소리 들으면서 회 먹고, 밤엔 다 같이 폭죽 터뜨렸는데 애들처럼 좋아함 ㅋㅋ'),
        ('bbbbbbbb-bbbb-bbbb-bbbb-000000000002', v_archive, v_author,
         '벚꽃 절정일 때 딱 맞춰 갔다. 돗자리 깔고 치킨 먹으면서 꽃잎 떨어지는 거 구경. 사진만 백 장은 찍은 듯'),
        ('bbbbbbbb-bbbb-bbbb-bbbb-000000000003', v_archive, v_author,
         '올해 첫눈 오던 날! 손 시려워서 호빵 사 먹고 자물쇠도 걸었다. 눈 내리는 야경이 진짜 예뻤음'),
        ('bbbbbbbb-bbbb-bbbb-bbbb-000000000004', v_archive, v_author,
         '불멍 하면서 고기 구워 먹은 밤. 텐트 치는 데 한참 걸려서 다 같이 삽질했지만 그것도 추억이지'),
        ('bbbbbbbb-bbbb-bbbb-bbbb-000000000005', v_archive, v_author,
         '비 오는 날 창가에서 커피 마시며 수다 떨었다. 요즘 힘든 얘기 하다가 결국 웃겨서 배 잡고 웃음'),
        ('bbbbbbbb-bbbb-bbbb-bbbb-000000000006', v_archive, v_author,
         '단풍 보러 등산 갔다. 정상에서 먹은 김밥이 인생 김밥이었다. 내려올 때 무릎 나갈 뻔했지만'),
        ('bbbbbbbb-bbbb-bbbb-bbbb-000000000007', v_archive, v_author,
         '좋아하던 밴드 공연 드디어 봤다. 떼창하다가 목 다 쉬었는데 너무 행복해서 상관없었음'),
        ('bbbbbbbb-bbbb-bbbb-bbbb-000000000008', v_archive, v_author,
         '새해 첫날 온 가족 모여서 떡국 먹었다. 할머니가 세뱃돈 주시면서 건강하라고 하신 말이 계속 생각남'),
        ('bbbbbbbb-bbbb-bbbb-bbbb-000000000009', v_archive, v_author,
         '강아지 봄이 입양하고 첫 산책. 잔디밭에서 신나서 뛰어다니는 거 보니까 나도 모르게 눈물 났다'),
        ('bbbbbbbb-bbbb-bbbb-bbbb-000000000010', v_archive, v_author,
         '불꽃축제. 사람 진짜 많았지만 불꽃 터질 때 다 같이 우와 하던 순간이 잊히질 않는다'),
        ('bbbbbbbb-bbbb-bbbb-bbbb-000000000011', v_archive, v_author,
         '광장시장 야시장에서 빈대떡에 마약김밥에 먹부림 제대로 했다. 막걸리 한잔까지 완벽'),
        ('bbbbbbbb-bbbb-bbbb-bbbb-000000000012', v_archive, v_author,
         '떨리는 마음으로 첫 출근. 점심에 팀 사람들이 환영한다고 밥 사줌. 긴장했지만 좋은 사람들이라 다행'),
        ('bbbbbbbb-bbbb-bbbb-bbbb-000000000013', v_archive, v_author,
         '드디어 졸업! 학사모 던지고 꽃다발 받았다. 4년이 이렇게 빨리 갈 줄. 다들 각자 길 가는 게 실감남'),
        ('bbbbbbbb-bbbb-bbbb-bbbb-000000000014', v_archive, v_author,
         '어린이날에 굳이 놀이공원 갔다 ㅋㅋ 롤러코스터 타고 소리 지르다가 회전목마에서 갑자기 감성 잡음'),
        ('bbbbbbbb-bbbb-bbbb-bbbb-000000000015', v_archive, v_author,
         '제주 3박 4일. 성산일출봉 오르고 흑돼지 먹고, 바다 보면서 멍때린 시간이 제일 좋았다'),
        ('bbbbbbbb-bbbb-bbbb-bbbb-000000000016', v_archive, v_author,
         '집에서 트리 꾸미고 케이크 만든 크리스마스. 요리는 망했지만 다 같이 먹으니 꿀맛. 선물 교환하다 빵 터짐');

    raise notice '시드 완료: 기억 16개 + 메모 16개. 이제 embed_bimbim.sh 로 임베딩하세요.';
end $$;
