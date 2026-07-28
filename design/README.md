# UI 시안

Compose 화면을 만들 때 참조하는 디자인 이미지를 여기에 둔다.
(이미지 자체는 코드가 아니므로 참조용이며, 구현이 끝나도 지우지 않는다 — 이후 수정 요청의 기준이 된다.)

## 파일 이름 규칙

`S{번호}_{화면}_{변형}.png` 형태로 저장한다. 화면-코드 대응이 바로 잡힌다.

| 파일명 예시 | 대응 패키지 |
|---|---|
| `S0_auth.png` | `ui/screen/auth/` |
| `S1_grouplist.png` | `ui/screen/grouplist/` |
| `S2_chat.png` | `ui/screen/chat/` ⭐ 우선 작업 대상 |
| `S2_chat_result.png` | 〃 (검색 결과가 표시된 상태) |
| `S3_memorydetail.png` | `ui/screen/memorydetail/` |
| `S4_memorycreate.png` | `ui/screen/memorycreate/` |
| `S5_timeline.png` | `ui/screen/timeline/` |

한 화면에 상태가 여러 개면 `_empty`, `_loading`, `_error` 등을 뒤에 붙인다.
빈 상태·로딩 시안이 있으면 UiState 설계가 훨씬 정확해진다.

## 있으면 좋은 것

- **컬러 팔레트 / 타이포 스펙** — 없으면 이미지에서 추출하지만 오차가 생긴다
- **아이콘** — `front/app/src/main/res/drawable/` 에 벡터로 넣으면 그대로 쓴다
- **폰트 파일** — `.ttf`/`.otf` 를 `front/app/src/main/res/font/` 에

## 없어도 되는 것

- 화면 전 상태를 다 그린 시안. 대표 상태 1장이면 나머지는 규칙으로 파생 가능하다.
