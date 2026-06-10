# LaundryFlow DX

街のクリーニング店向け管理システム。Delphi(VCL)製既存アプリと同一仕様で、モダン構成のWeb版を並走させるポートフォリオプロジェクト。

## 技術スタック

| 層 | 技術 |
|---|---|
| フロントエンド | React 19 / Vite 8 / Tailwind CSS / lucide-react |
| バックエンド | Ktor 2.3.9 (Kotlin) / Exposed ORM 0.41.1 |
| データベース | SQLite (`laundryflow.db`) |
| テスト | Kotest 5.8.1 (backend) / Vitest (frontend) |

## ディレクトリ構成

```
laundry-flow-dx/
├── frontend/          # React + Vite
│   └── src/
│       ├── components/    # Dashboard, Customers, Orders, NewOrder, OrderDetailsModal
│       ├── services/      # api.js (Ktor REST クライアント)
│       └── App.jsx
└── backend/           # Ktor
    └── src/main/kotlin/com/laundryflow/
        ├── Application.kt
        ├── models/        # Database.kt, Enums.kt, Customer.kt, Order.kt
        ├── routes/        # CustomerRoutes, OrderRoutes, DashboardRoutes
        └── services/      # CustomerService, OrderService, PriceCalculator, DashboardService
```

## セットアップ

### バックエンド（ポート 8080）

```bash
cd backend
./gradlew run
```

初回起動時に `laundryflow.db` が自動生成され、テーブルが作成されます。

### フロントエンド（ポート 5173）

```bash
cd frontend
npm install
npm run dev
```

## API エンドポイント

| Method | Path | 説明 |
|--------|------|------|
| GET | `/api/customers` | 顧客一覧 |
| POST | `/api/customers` | 顧客登録 |
| GET | `/api/customers/{id}` | 顧客詳細 |
| PUT | `/api/customers/{id}` | 顧客更新 |
| DELETE | `/api/customers/{id}` | 顧客削除 |
| GET | `/api/orders` | 注文一覧 |
| POST | `/api/orders` | 注文作成 |
| GET | `/api/orders/{id}` | 注文詳細 |
| PATCH | `/api/orders/{id}/status` | ステータス更新 |
| DELETE | `/api/orders/{id}` | 注文削除 |
| GET | `/api/dashboard/stats` | 売上集計 |

## 業務ロジック

### 料金計算

1. **シミ抜き**: 基本料金 + ¥500（点数分）
2. **急ぎ**: 上記小計 × 1.3（端数切り捨て）
3. **プレミアム会員**: 上記小計 × 0.9（端数切り捨て）
4. **数量割引**: 5点以上 5% OFF / 10点以上 10% OFF
5. **プロモコード**: `WELCOME10`（10% OFF）/ `SPRING20`（20% OFF）/ `SUMMER25`（25% OFF）
6. **消費税**: 10%

### 料金計算の適用順

```
基本料金 → シミ抜き加算 → 急ぎ割増 → プレミアム割引 → 数量割引 → プロモ割引 → 消費税
```

### 衣類種別と基本料金

| 種別 | 料金 |
|------|------|
| シャツ | ¥300 |
| スーツ | ¥1,500 |
| コート | ¥2,000 |
| ドレス | ¥1,800 |
| 毛布 | ¥2,500 |

### ステータス遷移

```
受付 (Received) → 洗浄中 (Washing) → 仕上げ中 (Finishing) → 受取待ち (WaitingForPickup) → 完了 (Completed)
受付 (Received) → キャンセル (Cancelled)
```

## テスト実行

```bash
cd backend
./gradlew test
```

対象テスト:
- `PriceCalculatorTest` — 料金計算ロジック単体テスト
- `OrderServiceTest` — 注文サービス単体テスト
- `OrderServiceIntegrationTest` — DB を使った統合テスト
- `OrderStatusTransitionTest` — ステータス遷移ルールテスト

## 画面構成

| 画面 | 機能 |
|------|------|
| Dashboard | 本日/今月の売上、全注文数、衣類種別ランキング |
| Orders | 注文一覧、ステータス更新、注文詳細モーダル |
| Customers | 顧客一覧、新規登録、検索 |
| New Order | 顧客選択、衣類追加、オプション設定、リアルタイム金額計算 |
