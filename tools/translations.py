import os, sys

KEYS = ["title", "empty", "unknown_item", "btn.addtext", "btn.additem", "btn.close",
        "mode.follow", "mode.left", "mode.free", "mode.follow.short", "mode.left.short", "mode.free.short",
        "tip.mini", "tip.mini.remaining", "tip.minimize", "tip.mode", "tip.mode.desc", "tip.drag",
        "tip.check", "tip.delete", "tip.addtext", "tip.additem", "tip.additem.desc", "tip.additem.drop",
        "tip.search", "tip.progress", "tip.row.icon", "tip.row.count", "tip.result",
        "search.hint", "search.prompt", "search.none", "search.count",
        "help.title", "help.edit", "help.modifier", "help.hold", "help.drop", "help.wheel", "help.auto",
        "help.recipe", "help.reorder", "help.move"]

T = {}

T["en_US"] = [
    "ToDo", "Add notes or items here", "[Unknown item]", "+ Note", "+ Item", "Close",
    "Follow inventory", "Pinned to left edge", "Free position", "F", "L", "M",
    "Click to open ToDo (drag to move)", "Remaining: %s", "Minimize", "Position: %s",
    "Click to cycle Follow -> Left -> Free", "Drag to move (switches to free position)",
    "Toggle done", "Delete this row", "Add a note row", "Search items by name or ID",
    "Or %s+click an item, or click the panel while holding one", "You can also drag items here from the recipe viewer",
    "Part of a name, or modid:name[:meta] (right-click clears)", "Have %s / Goal %s",
    "Icon: left-click = recipes / right-click = usages", "Count: click = type / wheel = +-1 (Shift 16, Ctrl 64) / right-click = set to current",
    "Click to add (Shift+click to keep adding)",
    "Item name / ID", "Type a name to see results", "Nothing found", "%s found",
    "How to use ToDo", "Click text to edit (Enter = save / Esc = cancel)", "%s+click: add an inventory item",
    "Click the panel while holding an item to add it", "Drag items here from the recipe viewer",
    "Wheel over the count: change the goal", "Rows are checked automatically when the goal is reached",
    "Recipe viewer keys (R / U ...) work over items", "Shift+wheel: reorder rows",
    "Drag the header to move, \"_\" to minimize"]

T["ja_JP"] = [
    "ToDo", "メモやアイテムを追加しましょう", "[不明なアイテム]", "+ メモ", "+ アイテム", "閉じる",
    "インベントリ追従", "画面左端に固定", "自由配置", "追", "左", "自",
    "クリックで ToDo を開く（ドラッグで移動）", "未完了: %s 件", "最小化（アイコン化）", "配置: %s",
    "クリックで 追従 → 左端 → 自由 を切り替え", "ドラッグで移動（自由配置になります）",
    "完了チェックを切り替え", "この行を削除", "メモ行を追加して入力", "名前や ID でアイテムを検索して追加",
    "%s+クリック、または手に持ってパネルをクリックでも追加できます", "レシピビューアーからドラッグしても追加できます",
    "名前の一部、または modid:name[:meta] で検索（右クリックで消去）", "所持 %s / 目標 %s",
    "アイコン: 左クリック=レシピ / 右クリック=用途", "個数: クリック=入力 / ホイール=±1 (Shift ±16, Ctrl ±64) / 右クリック=所持数を目標に",
    "クリックで追加（Shift+クリックで続けて追加）",
    "アイテム名 / ID を入力", "名前を入力すると候補が出ます", "見つかりませんでした", "%s 件",
    "ToDo の使い方", "文字をクリック: メモを編集（Enter で確定 / Esc で取消）", "%s+クリック: インベントリのアイテムを登録",
    "アイテムを手に持ってパネルをクリック: 登録", "レシピビューアーからドラッグ＆ドロップ: 登録",
    "個数の上でホイール: 目標数を変更", "目標数に達すると自動でチェック（打ち消し線）",
    "アイテムの上でレシピビューアーのキー（R / U など）が使えます", "Shift+ホイール: 行の並べ替え",
    "ヘッダーをドラッグで移動、「_」で最小化"]

T["zh_CN"] = [
    "待办", "在这里添加备注或物品", "[未知物品]", "+ 备注", "+ 物品", "关闭",
    "跟随物品栏", "固定在左侧", "自由位置", "跟", "左", "自",
    "点击打开待办（拖动可移动）", "未完成：%s", "最小化", "位置：%s",
    "点击切换 跟随 → 左侧 → 自由", "拖动以移动（切换为自由位置）",
    "切换完成状态", "删除此行", "添加备注行", "按名称或 ID 搜索物品",
    "也可以 %s+点击物品，或手持物品点击面板", "也可以从配方查看器拖入物品",
    "名称的一部分，或 modid:name[:meta]（右键清空）", "拥有 %s / 目标 %s",
    "图标：左键 = 配方 / 右键 = 用途", "数量：点击 = 输入 / 滚轮 = ±1（Shift ±16，Ctrl ±64）/ 右键 = 设为当前数量",
    "点击添加（Shift+点击可连续添加）",
    "物品名称 / ID", "输入名称以显示结果", "未找到", "找到 %s 个",
    "待办使用方法", "点击文字编辑（Enter 保存 / Esc 取消）", "%s+点击：添加物品栏中的物品",
    "手持物品点击面板：添加", "从配方查看器拖放：添加",
    "在数量上滚动滚轮：修改目标", "达到目标后自动勾选",
    "在物品上可使用配方查看器的按键（R / U 等）", "Shift+滚轮：调整行顺序",
    "拖动标题栏移动，点击“_”最小化"]

T["zh_TW"] = [
    "待辦", "在這裡新增備註或物品", "[未知物品]", "+ 備註", "+ 物品", "關閉",
    "跟隨物品欄", "固定在左側", "自由位置", "跟", "左", "自",
    "點擊開啟待辦（拖曳可移動）", "未完成：%s", "最小化", "位置：%s",
    "點擊切換 跟隨 → 左側 → 自由", "拖曳以移動（切換為自由位置）",
    "切換完成狀態", "刪除此列", "新增備註列", "依名稱或 ID 搜尋物品",
    "也可以 %s+點擊物品，或手持物品點擊面板", "也可以從配方檢視器拖入物品",
    "名稱的一部分，或 modid:name[:meta]（右鍵清除）", "擁有 %s / 目標 %s",
    "圖示：左鍵 = 配方 / 右鍵 = 用途", "數量：點擊 = 輸入 / 滾輪 = ±1（Shift ±16，Ctrl ±64）/ 右鍵 = 設為目前數量",
    "點擊新增（Shift+點擊可連續新增）",
    "物品名稱 / ID", "輸入名稱以顯示結果", "找不到", "找到 %s 個",
    "待辦使用方式", "點擊文字編輯（Enter 儲存 / Esc 取消）", "%s+點擊：新增物品欄中的物品",
    "手持物品點擊面板：新增", "從配方檢視器拖放：新增",
    "在數量上滾動滾輪：修改目標", "達到目標後自動勾選",
    "在物品上可使用配方檢視器的按鍵（R / U 等）", "Shift+滾輪：調整列順序",
    "拖曳標題列移動，點擊「_」最小化"]

T["ko_KR"] = [
    "할 일", "메모나 아이템을 추가하세요", "[알 수 없는 아이템]", "+ 메모", "+ 아이템", "닫기",
    "인벤토리 따라가기", "왼쪽 끝에 고정", "자유 위치", "따", "왼", "자",
    "클릭하여 할 일 열기 (드래그로 이동)", "남은 항목: %s", "최소화", "위치: %s",
    "클릭하여 따라가기 → 왼쪽 → 자유 전환", "드래그하여 이동 (자유 위치로 전환)",
    "완료 표시 전환", "이 줄 삭제", "메모 줄 추가", "이름이나 ID로 아이템 검색",
    "%s+클릭하거나 아이템을 든 채 패널을 클릭해도 추가됩니다", "레시피 뷰어에서 드래그해도 추가됩니다",
    "이름의 일부 또는 modid:name[:meta] (우클릭으로 지우기)", "보유 %s / 목표 %s",
    "아이콘: 좌클릭 = 레시피 / 우클릭 = 용도", "개수: 클릭 = 입력 / 휠 = ±1 (Shift ±16, Ctrl ±64) / 우클릭 = 현재 보유량으로",
    "클릭하여 추가 (Shift+클릭으로 계속 추가)",
    "아이템 이름 / ID", "이름을 입력하면 결과가 표시됩니다", "찾을 수 없습니다", "%s개 찾음",
    "할 일 사용법", "글자 클릭: 메모 편집 (Enter 저장 / Esc 취소)", "%s+클릭: 인벤토리 아이템 추가",
    "아이템을 든 채 패널 클릭: 추가", "레시피 뷰어에서 드래그 앤 드롭: 추가",
    "개수 위에서 휠: 목표 변경", "목표에 도달하면 자동으로 체크됩니다",
    "아이템 위에서 레시피 뷰어 키 (R / U 등) 사용 가능", "Shift+휠: 줄 순서 변경",
    "헤더를 드래그하여 이동, \"_\"로 최소화"]

T["ru_RU"] = [
    "Задачи", "Добавьте заметки или предметы", "[Неизвестный предмет]", "+ Заметка", "+ Предмет", "Закрыть",
    "Рядом с инвентарём", "У левого края", "Свободно", "И", "Л", "С",
    "Нажмите, чтобы открыть (перетащите для перемещения)", "Осталось: %s", "Свернуть", "Положение: %s",
    "Нажмите: инвентарь -> левый край -> свободно", "Перетащите для перемещения (свободное положение)",
    "Отметить выполнение", "Удалить строку", "Добавить заметку", "Поиск предметов по названию или ID",
    "Или %s+клик по предмету, или клик по панели с предметом в руке", "Можно также перетащить предмет из просмотрщика рецептов",
    "Часть названия или modid:name[:meta] (ПКМ очищает)", "Есть %s / Цель %s",
    "Значок: ЛКМ = рецепты / ПКМ = применение", "Количество: клик = ввод / колесо = ±1 (Shift 16, Ctrl 64) / ПКМ = текущее",
    "Клик — добавить (Shift+клик — добавлять дальше)",
    "Название / ID", "Введите название", "Ничего не найдено", "Найдено: %s",
    "Как пользоваться", "Клик по тексту — редактировать (Enter — сохранить / Esc — отмена)", "%s+клик: добавить предмет из инвентаря",
    "Клик по панели с предметом в руке — добавить", "Перетащите предмет из просмотрщика рецептов",
    "Колесо над количеством — изменить цель", "Строки отмечаются автоматически при достижении цели",
    "Клавиши просмотрщика рецептов (R / U ...) работают над предметами", "Shift+колесо — порядок строк",
    "Перетащите заголовок для перемещения, «_» — свернуть"]

T["de_DE"] = [
    "ToDo", "Notizen oder Items hinzufügen", "[Unbekanntes Item]", "+ Notiz", "+ Item", "Schließen",
    "Am Inventar", "Am linken Rand", "Frei", "I", "L", "F",
    "Klicken zum Öffnen (ziehen zum Verschieben)", "Offen: %s", "Minimieren", "Position: %s",
    "Klicken: Inventar -> Links -> Frei", "Ziehen zum Verschieben (wechselt zu frei)",
    "Erledigt umschalten", "Zeile löschen", "Notizzeile hinzufügen", "Items nach Name oder ID suchen",
    "Oder %s+Klick auf ein Item, oder mit Item in der Hand auf das Panel klicken", "Items können auch aus dem Rezeptbetrachter hierher gezogen werden",
    "Teil des Namens oder modid:name[:meta] (Rechtsklick leert)", "Vorhanden %s / Ziel %s",
    "Symbol: Linksklick = Rezepte / Rechtsklick = Verwendung", "Anzahl: Klick = eingeben / Mausrad = ±1 (Shift 16, Strg 64) / Rechtsklick = aktueller Bestand",
    "Klicken zum Hinzufügen (Shift+Klick für mehrere)",
    "Itemname / ID", "Namen eingeben für Ergebnisse", "Nichts gefunden", "%s gefunden",
    "So funktioniert ToDo", "Text anklicken zum Bearbeiten (Enter = speichern / Esc = abbrechen)", "%s+Klick: Item aus dem Inventar hinzufügen",
    "Mit Item in der Hand auf das Panel klicken: hinzufügen", "Items aus dem Rezeptbetrachter hierher ziehen",
    "Mausrad über der Anzahl: Ziel ändern", "Zeilen werden beim Erreichen des Ziels automatisch abgehakt",
    "Tasten des Rezeptbetrachters (R / U ...) funktionieren über Items", "Shift+Mausrad: Zeilen sortieren",
    "Kopfzeile ziehen zum Verschieben, \"_\" zum Minimieren"]

T["fr_FR"] = [
    "ToDo", "Ajoutez des notes ou des objets", "[Objet inconnu]", "+ Note", "+ Objet", "Fermer",
    "Suivre l'inventaire", "Fixé à gauche", "Position libre", "I", "G", "L",
    "Cliquer pour ouvrir (glisser pour déplacer)", "Restant : %s", "Réduire", "Position : %s",
    "Cliquer : inventaire -> gauche -> libre", "Glisser pour déplacer (passe en position libre)",
    "Marquer comme fait", "Supprimer cette ligne", "Ajouter une note", "Chercher des objets par nom ou ID",
    "Ou %s+clic sur un objet, ou cliquer sur le panneau avec un objet en main", "Vous pouvez aussi glisser des objets depuis le visualiseur de recettes",
    "Partie du nom, ou modid:name[:meta] (clic droit pour effacer)", "Possédé %s / Objectif %s",
    "Icône : clic gauche = recettes / clic droit = utilisations", "Quantité : clic = saisir / molette = ±1 (Maj 16, Ctrl 64) / clic droit = quantité actuelle",
    "Cliquer pour ajouter (Maj+clic pour continuer)",
    "Nom d'objet / ID", "Tapez un nom pour voir les résultats", "Aucun résultat", "%s trouvé(s)",
    "Utilisation de ToDo", "Cliquer sur le texte pour modifier (Entrée = valider / Échap = annuler)", "%s+clic : ajouter un objet de l'inventaire",
    "Cliquer sur le panneau avec un objet en main : ajouter", "Glisser des objets depuis le visualiseur de recettes",
    "Molette sur la quantité : changer l'objectif", "Les lignes sont cochées automatiquement quand l'objectif est atteint",
    "Les touches du visualiseur de recettes (R / U ...) fonctionnent sur les objets", "Maj+molette : réordonner les lignes",
    "Glisser l'en-tête pour déplacer, « _ » pour réduire"]

T["es_ES"] = [
    "ToDo", "Añade notas u objetos", "[Objeto desconocido]", "+ Nota", "+ Objeto", "Cerrar",
    "Seguir inventario", "Fijo a la izquierda", "Posición libre", "I", "Z", "L",
    "Clic para abrir (arrastra para mover)", "Pendientes: %s", "Minimizar", "Posición: %s",
    "Clic: inventario -> izquierda -> libre", "Arrastra para mover (cambia a posición libre)",
    "Marcar como hecho", "Eliminar esta fila", "Añadir una nota", "Buscar objetos por nombre o ID",
    "O %s+clic en un objeto, o clic en el panel con un objeto en la mano", "También puedes arrastrar objetos desde el visor de recetas",
    "Parte del nombre, o modid:name[:meta] (clic derecho borra)", "Tienes %s / Meta %s",
    "Icono: clic izquierdo = recetas / clic derecho = usos", "Cantidad: clic = escribir / rueda = ±1 (Mayús 16, Ctrl 64) / clic derecho = cantidad actual",
    "Clic para añadir (Mayús+clic para seguir añadiendo)",
    "Nombre / ID", "Escribe un nombre para ver resultados", "Sin resultados", "%s encontrados",
    "Cómo usar ToDo", "Clic en el texto para editar (Intro = guardar / Esc = cancelar)", "%s+clic: añadir un objeto del inventario",
    "Clic en el panel con un objeto en la mano: añadir", "Arrastra objetos desde el visor de recetas",
    "Rueda sobre la cantidad: cambiar la meta", "Las filas se marcan solas al alcanzar la meta",
    "Las teclas del visor de recetas (R / U ...) funcionan sobre los objetos", "Mayús+rueda: reordenar filas",
    "Arrastra la cabecera para mover, \"_\" para minimizar"]

T["pt_BR"] = [
    "ToDo", "Adicione notas ou itens", "[Item desconhecido]", "+ Nota", "+ Item", "Fechar",
    "Seguir inventário", "Fixo à esquerda", "Posição livre", "I", "E", "L",
    "Clique para abrir (arraste para mover)", "Restantes: %s", "Minimizar", "Posição: %s",
    "Clique: inventário -> esquerda -> livre", "Arraste para mover (muda para posição livre)",
    "Marcar como feito", "Excluir esta linha", "Adicionar uma nota", "Buscar itens por nome ou ID",
    "Ou %s+clique em um item, ou clique no painel segurando um item", "Também é possível arrastar itens do visualizador de receitas",
    "Parte do nome, ou modid:name[:meta] (clique direito limpa)", "Possui %s / Meta %s",
    "Ícone: clique esquerdo = receitas / clique direito = usos", "Quantidade: clique = digitar / roda = ±1 (Shift 16, Ctrl 64) / clique direito = quantidade atual",
    "Clique para adicionar (Shift+clique para continuar)",
    "Nome / ID", "Digite um nome para ver resultados", "Nada encontrado", "%s encontrados",
    "Como usar o ToDo", "Clique no texto para editar (Enter = salvar / Esc = cancelar)", "%s+clique: adicionar um item do inventário",
    "Clique no painel segurando um item: adicionar", "Arraste itens do visualizador de receitas",
    "Roda sobre a quantidade: mudar a meta", "As linhas são marcadas sozinhas ao atingir a meta",
    "As teclas do visualizador de receitas (R / U ...) funcionam sobre os itens", "Shift+roda: reordenar linhas",
    "Arraste o cabeçalho para mover, \"_\" para minimizar"]

# ---------------------------------------------------------------------------
# Output. Usage:
#   python tools/translations.py <dir>          -> 1.7.10 .lang files
#   python tools/translations.py <dir> --json   -> 1.13+ JSON files (lower-case names)
# Optional extra tables with the same structure (KEYS + T) are merged when present:
#   translations_features.py  (features shared by all versions; both outputs)
#   translations_modern.py    (keys only the modern versions use; JSON only)
# Keys starting with "!" are written as-is, others get the "inventorytodo." prefix.
# ---------------------------------------------------------------------------

NL = chr(10)


def _load_extra(name):
    path = os.path.join(os.path.dirname(os.path.abspath(__file__)), name + ".py")
    if not os.path.exists(path):
        return None
    ns = {}
    with open(path, encoding="utf-8") as f:
        exec(compile(f.read(), path, "exec"), ns)
    return ns["KEYS"], ns["T"]


def merged(json_mode):
    keys = list(KEYS)
    tables = {lang: list(vals) for lang, vals in T.items()}
    for name in ["translations_features"] + (["translations_modern"] if json_mode else []):
        extra = _load_extra(name)
        if extra is None:
            continue
        k2, t2 = extra
        for lang, vals in t2.items():
            assert len(vals) == len(k2), (name, lang, len(vals), len(k2))
        keys += k2
        for lang in tables:
            tables[lang] += t2.get(lang, t2["en_US"])
    return keys, tables


def full_key(k):
    return k[1:] if k.startswith("!") else "inventorytodo." + k


def main():
    out = sys.argv[1]
    json_mode = "--json" in sys.argv[2:]
    keys, tables = merged(json_mode)
    os.makedirs(out, exist_ok=True)
    for lang, vals in tables.items():
        assert len(vals) == len(keys), (lang, len(vals), len(keys))
        if json_mode:
            import json
            data = {full_key(k): v for k, v in zip(keys, vals)}
            with open(os.path.join(out, lang.lower() + ".json"), "w", encoding="utf-8", newline=NL) as f:
                json.dump(data, f, ensure_ascii=False, indent=2)
                f.write(NL)
        else:
            with open(os.path.join(out, lang + ".lang"), "w", encoding="utf-8", newline=NL) as f:
                for k, v in zip(keys, vals):
                    if not k.startswith("!"):
                        f.write(full_key(k) + "=" + v + NL)
    print("ok", len(tables), "languages,", len(keys), "keys", "(json)" if json_mode else "(.lang)")


if __name__ == "__main__":
    main()
