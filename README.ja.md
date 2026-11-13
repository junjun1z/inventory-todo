<p align="center"><img src="docs/icon_128.png" alt="Inventory ToDo のアイコン" width="128"></p>

<h1 align="center">Inventory ToDo</h1>

<p align="center">
インベントリの横に出せる ToDo リスト。メモとアイテムの目標数を書いておくと、集めた数が自動で増えていきます。<br>
<a href="https://modrinth.com/mod/inventory-todo">Modrinth</a> ·
<a href="https://www.curseforge.com/minecraft/mc-mods/inventory-todo">CurseForge</a> ·
<a href="README.md">English</a>
</p>

> **どんな modpack・どんな mod 構成でもそのまま使えます（前提 mod 不要）。**
> Forge・Fabric・NeoForge に対応し、1.7.10 から最新バージョンまで使えます。レシピビューアー（NEI / JEI / EMI / REI）は、入っていれば連携する任意の機能です。

## できること

- **メモとアイテム目標を 1 つのリストに** — 何でも書けるメモと、「鉄インゴット 64 個」のようなアイテム目標を並べて管理できます。
- **所持数を自動でカウント** — インベントリの中身から `[32/64]` のように表示し、目標に届くと自動でチェック（打ち消し線）が付きます。
- **アイテムの追加がすぐできる** — インベントリで Alt+クリック、アイテムを持ってパネルをクリック、名前や ID で検索（日本語でも英語名でも検索可）、レシピビューアーからドラッグ＆ドロップ。
- **目標数をすばやく変更** — ホイール（Shift で ±16、Ctrl で ±64）、クリックで数値入力、右クリックで今の所持数を目標にする。
- **邪魔にならない** — インベントリ追従・画面左端に固定・好きな位置にドラッグ、の 3 つの配置。最小化すると残りの件数だけを表示します。
- **HUD 表示** — インベントリを閉じていても未完了の項目を画面に表示（キーで切り替え）。
- **レシピの材料を自動追加** — アイテムのアイコンをCtrl+クリックすると、作業台レシピの材料を「あと必要な数」に合わせて追加。
- **開いているチェストの中身も数える** — チェストなどを開いているときは、その中身も合算（設定で切替可）。
- **ワールド別リスト／全ワールド共通リスト**、**キー割り当て**、メタ値・NBT まで区別した正確なカウント、**10 言語対応**。

クライアント専用です。サーバーには不要で、この mod が入っていないサーバーにもそのまま接続できます。

## 対応バージョン

<!-- support-table:start -->
| Minecraft | ローダー |
| --- | --- |
| 26.3 | Forge, NeoForge, Fabric |
| 26.2 | NeoForge, Fabric |
| 26.1.2 | NeoForge, Fabric |
| 26.1.1 | NeoForge, Fabric |
| 26.1 | NeoForge, Fabric |
| 1.21.11 | NeoForge, Fabric |
| 1.21.1 | NeoForge, Fabric |
| 1.20.1 | Forge, Fabric |
| 1.19.2 | Forge, Fabric |
| 1.18.2 | Forge, Fabric |
| 1.16.5 | Forge, Fabric |
| 1.12.2 | Forge |
| 1.7.10 | Forge |
<!-- support-table:end -->

## 任意の連携

| mod | できること |
| --- | --- |
| NEI（元祖版・GTNH 版） | アイテム一覧からの修飾キー+クリック／ドラッグ＆ドロップ、アイコンクリックでレシピ・用途、リスト上で NEI のキー（R / U） |
| JEI（1.20.1 以降）/ EMI / REI | マウス下のアイテムを追加、ドラッグ＆ドロップ、アイコンクリックでレシピ・用途 |

## よくある質問

- **サーバーに必要？** 不要です（クライアント専用）。
- **modpack に入れていい？** MIT ライセンスなので、許可なく自由に同梱できます。
- **リストはどこに保存される？** ゲームフォルダの `config/inventorytodo/`（ワールド・サーバーごとのファイルと、共通リスト）。
- **「GTNH ToDo」を使っていた** 初回起動時にリストと設定を自動で引き継ぎます。

## 開発者向け

ビルド方法とリリース手順は [README.md](README.md#for-developers) を参照してください（タグ `1.2.3` を push すると、GitHub Releases・Modrinth・CurseForge へ自動で公開されます）。

## ライセンス

[MIT](LICENSE)
