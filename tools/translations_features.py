"""Strings for the features added after the first version (HUD, ingredients, key bindings, config screen).

Same structure as translations.py: KEYS (without the "inventorytodo." prefix) and T = {lang: [values]}.
translations.py merges these into its output.
"""

KEYS = ["state.on", "state.off", "btn.hud.short", "tip.hud", "tip.hud.desc", "tip.row.ingredients",
        "status.ingredients", "status.norecipe", "help.ingredients", "help.hud",
        "key.category", "key.togglehud", "key.addhovered",
        "config.title", "config.addModifier", "config.maxVisibleRows", "config.panelWidth", "config.ignoreNbt",
        "config.hud", "config.hudAnchor", "config.hudMaxRows", "config.countContainer", "config.listScope",
        "hud.title"]

T = {}

T["en_US"] = [
    "ON", "OFF", "H", "HUD (list on screen while playing): %s", "Shows unfinished rows while the inventory is closed",
    "Ctrl+click icon: add its crafting ingredients",
    "Added ingredients for %s", "No crafting recipe found",
    "Ctrl+click an item icon: add its crafting ingredients", "\"H\" button: show the list on screen while playing",
    "Inventory ToDo", "Toggle ToDo HUD", "Add hovered item to ToDo",
    "Inventory ToDo Settings", "Add-item modifier key", "Rows shown at once", "Panel width", "Ignore NBT when counting",
    "Show HUD while playing", "HUD position", "HUD rows", "Also count the open container", "List scope (WORLD / GLOBAL)",
    "ToDo"]

T["ja_JP"] = [
    "オン", "オフ", "H", "HUD（プレイ中に画面へ表示）: %s", "インベントリを閉じている間、未完了の行を表示します",
    "Ctrl+アイコンをクリック: 作業台レシピの材料を追加",
    "%s の材料を追加しました", "作業台のレシピが見つかりません",
    "Ctrl+アイコンをクリック: 作業台レシピの材料を追加", "「H」ボタン: プレイ中もリストを画面に表示",
    "Inventory ToDo", "ToDo の HUD 表示を切り替え", "マウス下のアイテムを ToDo に追加",
    "Inventory ToDo の設定", "アイテム追加の修飾キー", "一度に表示する行数", "パネルの幅", "数えるときに NBT を無視",
    "プレイ中に HUD を表示", "HUD の位置", "HUD の行数", "開いているコンテナの中も数える", "リストの範囲（WORLD=ワールド別 / GLOBAL=共通）",
    "ToDo"]

T["zh_CN"] = [
    "开", "关", "H", "HUD（游戏中显示在屏幕上）：%s", "关闭物品栏时显示未完成的行",
    "Ctrl+点击图标：添加合成材料",
    "已添加 %s 的材料", "未找到合成配方",
    "Ctrl+点击物品图标：添加合成材料", "“H”按钮：游戏中也在屏幕上显示列表",
    "Inventory ToDo", "切换待办 HUD", "将鼠标下的物品添加到待办",
    "Inventory ToDo 设置", "添加物品的修饰键", "一次显示的行数", "面板宽度", "计数时忽略 NBT",
    "游戏中显示 HUD", "HUD 位置", "HUD 行数", "同时统计打开的容器", "列表范围（WORLD=按世界 / GLOBAL=共用）",
    "待办"]

T["zh_TW"] = [
    "開", "關", "H", "HUD（遊戲中顯示在畫面上）：%s", "關閉物品欄時顯示未完成的列",
    "Ctrl+點擊圖示：新增合成材料",
    "已新增 %s 的材料", "找不到合成配方",
    "Ctrl+點擊物品圖示：新增合成材料", "「H」按鈕：遊戲中也在畫面上顯示清單",
    "Inventory ToDo", "切換待辦 HUD", "將滑鼠下的物品加入待辦",
    "Inventory ToDo 設定", "新增物品的修飾鍵", "一次顯示的列數", "面板寬度", "計數時忽略 NBT",
    "遊戲中顯示 HUD", "HUD 位置", "HUD 列數", "同時計算開啟的容器", "清單範圍（WORLD=依世界 / GLOBAL=共用）",
    "待辦"]

T["ko_KR"] = [
    "켜짐", "꺼짐", "H", "HUD (플레이 중 화면에 표시): %s", "인벤토리를 닫은 동안 미완료 항목을 표시합니다",
    "Ctrl+아이콘 클릭: 제작 재료 추가",
    "%s의 재료를 추가했습니다", "제작 레시피를 찾을 수 없습니다",
    "Ctrl+아이템 아이콘 클릭: 제작 재료 추가", "\"H\" 버튼: 플레이 중에도 목록을 화면에 표시",
    "Inventory ToDo", "할 일 HUD 전환", "마우스 아래 아이템을 할 일에 추가",
    "Inventory ToDo 설정", "아이템 추가 보조 키", "한 번에 표시할 줄 수", "패널 너비", "개수 셀 때 NBT 무시",
    "플레이 중 HUD 표시", "HUD 위치", "HUD 줄 수", "열린 컨테이너도 세기", "목록 범위 (WORLD=월드별 / GLOBAL=공통)",
    "할 일"]

T["ru_RU"] = [
    "ВКЛ", "ВЫКЛ", "H", "HUD (список на экране во время игры): %s", "Показывает невыполненные строки, когда инвентарь закрыт",
    "Ctrl+клик по значку: добавить ингредиенты рецепта",
    "Добавлены ингредиенты для: %s", "Рецепт крафта не найден",
    "Ctrl+клик по значку предмета: добавить ингредиенты", "Кнопка «H»: показывать список на экране во время игры",
    "Inventory ToDo", "Переключить HUD задач", "Добавить предмет под курсором",
    "Настройки Inventory ToDo", "Клавиша-модификатор для добавления", "Строк одновременно", "Ширина панели", "Игнорировать NBT при подсчёте",
    "Показывать HUD в игре", "Положение HUD", "Строк в HUD", "Считать и открытый контейнер", "Область списка (WORLD / GLOBAL)",
    "Задачи"]

T["de_DE"] = [
    "AN", "AUS", "H", "HUD (Liste im Spiel anzeigen): %s", "Zeigt offene Zeilen, während das Inventar geschlossen ist",
    "Strg+Klick auf Symbol: Zutaten des Rezepts hinzufügen",
    "Zutaten für %s hinzugefügt", "Kein Werkbankrezept gefunden",
    "Strg+Klick auf ein Item-Symbol: Zutaten hinzufügen", "\"H\"-Knopf: Liste auch im Spiel anzeigen",
    "Inventory ToDo", "ToDo-HUD umschalten", "Item unter der Maus zu ToDo hinzufügen",
    "Inventory ToDo Einstellungen", "Zusatztaste zum Hinzufügen", "Gleichzeitig sichtbare Zeilen", "Panelbreite", "NBT beim Zählen ignorieren",
    "HUD im Spiel anzeigen", "HUD-Position", "HUD-Zeilen", "Auch geöffneten Behälter zählen", "Listenbereich (WORLD / GLOBAL)",
    "ToDo"]

T["fr_FR"] = [
    "OUI", "NON", "H", "HUD (liste à l'écran en jeu) : %s", "Affiche les lignes non terminées quand l'inventaire est fermé",
    "Ctrl+clic sur l'icône : ajouter les ingrédients",
    "Ingrédients ajoutés pour %s", "Aucune recette d'établi trouvée",
    "Ctrl+clic sur l'icône d'un objet : ajouter ses ingrédients", "Bouton « H » : afficher la liste à l'écran en jeu",
    "Inventory ToDo", "Afficher/masquer le HUD ToDo", "Ajouter l'objet survolé au ToDo",
    "Paramètres d'Inventory ToDo", "Touche de modification pour ajouter", "Lignes affichées", "Largeur du panneau", "Ignorer le NBT au comptage",
    "Afficher le HUD en jeu", "Position du HUD", "Lignes du HUD", "Compter aussi le conteneur ouvert", "Portée de la liste (WORLD / GLOBAL)",
    "ToDo"]

T["es_ES"] = [
    "SÍ", "NO", "H", "HUD (lista en pantalla al jugar): %s", "Muestra las filas pendientes con el inventario cerrado",
    "Ctrl+clic en el icono: añadir los ingredientes",
    "Ingredientes añadidos para %s", "No se encontró receta de mesa de trabajo",
    "Ctrl+clic en el icono de un objeto: añadir sus ingredientes", "Botón \"H\": mostrar la lista en pantalla al jugar",
    "Inventory ToDo", "Mostrar/ocultar HUD de ToDo", "Añadir objeto bajo el cursor a ToDo",
    "Ajustes de Inventory ToDo", "Tecla modificadora para añadir", "Filas visibles", "Ancho del panel", "Ignorar NBT al contar",
    "Mostrar HUD al jugar", "Posición del HUD", "Filas del HUD", "Contar también el contenedor abierto", "Ámbito de la lista (WORLD / GLOBAL)",
    "ToDo"]

T["pt_BR"] = [
    "LIGADO", "DESLIGADO", "H", "HUD (lista na tela ao jogar): %s", "Mostra linhas pendentes com o inventário fechado",
    "Ctrl+clique no ícone: adicionar os ingredientes",
    "Ingredientes adicionados para %s", "Nenhuma receita de bancada encontrada",
    "Ctrl+clique no ícone de um item: adicionar seus ingredientes", "Botão \"H\": mostrar a lista na tela ao jogar",
    "Inventory ToDo", "Alternar HUD do ToDo", "Adicionar item sob o cursor ao ToDo",
    "Configurações do Inventory ToDo", "Tecla modificadora para adicionar", "Linhas visíveis", "Largura do painel", "Ignorar NBT ao contar",
    "Mostrar HUD ao jogar", "Posição do HUD", "Linhas do HUD", "Contar também o contêiner aberto", "Escopo da lista (WORLD / GLOBAL)",
    "ToDo"]

for _lang, _vals in T.items():
    assert len(_vals) == len(KEYS), (_lang, len(_vals), len(KEYS))
