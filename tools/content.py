"""Единый справочник содержимого мода.

Отсюда берут данные генераторы ресурсов и рецептов, а `validate.py`
сверяет этот справочник с настоящим Java-кодом — чтобы списки не разъехались.
"""

SCHOOLS = ["fire", "frost", "storm", "arcane", "nature", "shadow", "light"]

SCHOOL_NAMES = {
    "fire": ("Пиромантия", "Pyromancy"),
    "frost": ("Криомантия", "Cryomancy"),
    "storm": ("Магия бури", "Storm Magic"),
    "arcane": ("Аркана", "Arcana"),
    "nature": ("Магия природы", "Nature Magic"),
    "shadow": ("Магия тени", "Shadow Magic"),
    "light": ("Магия света", "Light Magic"),
}

CAST_TYPES = {
    "projectile": ("Снаряд", "Projectile"),
    "area": ("По площади", "Area"),
    "target": ("По цели", "Target"),
    "self": ("На себя", "Self"),
    "lingering": ("Длящееся", "Lingering"),
    "summon": ("Призыв", "Summon"),
    "utility": ("Вспомогательное", "Utility"),
}

# Предметы: id -> (русское имя, английское имя)
ITEMS = {
    "arcane_crystal": ("Аркановый кристалл", "Arcane Crystal"),
    "arcane_dust": ("Аркановая пыль", "Arcane Dust"),
    "infused_crystal": ("Напитанный кристалл", "Infused Crystal"),
    "soul_shard": ("Осколок души", "Soul Shard"),
    "phoenix_feather": ("Перо феникса", "Phoenix Feather"),
    "lunar_essence": ("Лунная эссенция", "Lunar Essence"),
    "blank_scroll": ("Чистый свиток", "Blank Scroll"),
    "dim_mana_crystal": ("Тусклый кристалл маны", "Dim Mana Crystal"),
    "charged_mana_crystal": ("Заряженный кристалл маны", "Charged Mana Crystal"),
    "mana_potion": ("Зелье маны", "Mana Potion"),
    "greater_mana_potion": ("Большое зелье маны", "Greater Mana Potion"),
    "mana_gem": ("Печать разума", "Mind Sigil"),
    "apprentice_wand": ("Жезл ученика", "Apprentice Wand"),
    "adept_staff": ("Посох адепта", "Adept Staff"),
    "archmage_staff": ("Посох архимага", "Archmage Staff"),
    "eldritch_scepter": ("Скипетр запредельного", "Eldritch Scepter"),
    "spellbook": ("Книга заклинаний", "Spellbook"),
    "spell_scroll": ("Свиток заклинания", "Spell Scroll"),
    "arcane_hood": ("Капюшон чародея", "Arcane Hood"),
    "arcane_robe": ("Мантия чародея", "Arcane Robe"),
    "arcane_leggings": ("Поножи чародея", "Arcane Leggings"),
    "arcane_boots": ("Башмаки чародея", "Arcane Boots"),
}

for _school, (_ru, _en) in SCHOOL_NAMES.items():
    ITEMS["rune_" + _school] = ("Руна: " + _ru, _en + " Rune")
    ITEMS["focus_" + _school] = ("Фокус: " + _ru, _en + " Focus")

# Блоки: id -> (русское имя, английское имя, вид модели)
BLOCKS = {
    "arcane_crystal_ore": ("Аркановая руда", "Arcane Crystal Ore", "cube"),
    "deepslate_arcane_crystal_ore": ("Глубинносланцевая аркановая руда",
                                     "Deepslate Arcane Crystal Ore", "cube"),
    "arcane_crystal_block": ("Аркановый блок", "Block of Arcane Crystal", "cube"),
    "arcane_crystal_cluster": ("Аркановый друз", "Arcane Crystal Cluster", "cluster"),
    "infusion_altar": ("Алтарь превращения", "Infusion Altar", "altar"),
    "mana_font": ("Источник маны", "Mana Font", "font"),
    "teleport_anchor": ("Якорь перемещения", "Teleport Anchor", "anchor"),
    "arcane_pedestal": ("Аркановый постамент", "Arcane Pedestal", "pedestal"),
    "arcane_lamp": ("Аркановый светильник", "Arcane Lamp", "lamp"),
    "warded_stone": ("Охранный камень", "Warded Stone", "cube"),
    "moonpetal": ("Лунный лепесток", "Moonpetal", "cross"),
}

for _school, (_ru, _en) in SCHOOL_NAMES.items():
    BLOCKS["runestone_" + _school] = ("Рунный камень: " + _ru, _en + " Runestone", "cube")

EFFECTS = {
    "mana_surge": ("Прилив маны", "Mana Surge"),
    "mana_burn": ("Выжигание маны", "Mana Burn"),
    "arcane_shield": ("Магический щит", "Arcane Shield"),
    "chilled": ("Промёрзший", "Chilled"),
    "combustion": ("Метка возгорания", "Combustion"),
    "blessed": ("Благословение", "Blessed"),
    "arcane_sight": ("Взор мага", "Arcane Sight"),
    "silence": ("Безмолвие", "Silence"),
}

# Заклинания: id -> (школа, ru имя, ru описание, en имя, en описание)
SPELLS = {
    # --- Пиромантия ---
    "fire_bolt": ("fire", "Огненная стрела", "Быстрый снаряд: урон и поджог цели.",
                  "Fire Bolt", "A quick bolt that burns what it hits."),
    "flame_wave": ("fire", "Волна пламени", "Конус огня перед собой; отбрасывает и поджигает.",
                   "Flame Wave", "A cone of fire that burns and pushes back."),
    "fire_shield": ("fire", "Огненный доспех",
                    "12 секунд огнестойкости; всё живое вплотную получает урон.",
                    "Fire Shield", "12s of fire resistance; scorches anyone who closes in."),
    "combustion": ("fire", "Возгорание",
                   "Метит цель: через 3 секунды она взрывается изнутри.",
                   "Combustion", "Marks a target; after 3 seconds it detonates."),
    "meteor": ("fire", "Метеор", "Через 1,5 секунды в точку прицела падает метеор.",
               "Meteor", "A meteor falls on the aimed point after 1.5 seconds."),
    "inferno": ("fire", "Инферно", "10 секунд огненной бури в радиусе 7 блоков.",
                "Inferno", "A 10-second firestorm within 7 blocks."),

    # --- Криомантия ---
    "ice_shard": ("frost", "Ледяная игла", "Снаряд, подмораживающий цель.",
                  "Ice Shard", "A shard that chills whatever it strikes."),
    "frost_nova": ("frost", "Морозная вспышка",
                   "Кольцо холода вокруг себя: урон, замедление и промерзание.",
                   "Frost Nova", "A ring of cold: damage, slowness and chill."),
    "ice_wall": ("frost", "Ледяная стена",
                 "Стена из плотного льда 5x3 перед собой на 20 секунд.",
                 "Ice Wall", "A 5x3 packed-ice wall in front of you for 20 seconds."),
    "glacial_armor": ("frost", "Ледяной доспех",
                      "20 секунд сопротивления и поглощения; снимает заморозку.",
                      "Glacial Armor", "20s of resistance and absorption; clears freezing."),
    "deep_freeze": ("frost", "Глубокая заморозка",
                    "Цель не может сдвинуться с места 5 секунд.",
                    "Deep Freeze", "Roots a target in place for 5 seconds."),
    "blizzard": ("frost", "Метель", "12 секунд метели в радиусе 8 блоков.",
                 "Blizzard", "A 12-second blizzard within 8 blocks."),

    # --- Магия бури ---
    "spark": ("storm", "Искра", "Дешёвый быстрый разряд почти без перезарядки.",
              "Spark", "A cheap, fast jolt with almost no cooldown."),
    "chain_lightning": ("storm", "Цепная молния",
                        "Разряд перескакивает до 5 целей, слабея с каждым скачком.",
                        "Chain Lightning", "Arcs across up to 5 targets, weaker each jump."),
    "thunderstrike": ("storm", "Удар грома", "Настоящая молния в точку прицела.",
                      "Thunderstrike", "Calls real lightning onto the aimed point."),
    "gale": ("storm", "Шквал", "Расшвыривает всех вокруг и подбрасывает вас вверх.",
             "Gale", "Blasts everyone away and launches you upward."),
    "static_field": ("storm", "Статическое поле",
                     "8 секунд поле бьёт током и замедляет всех в радиусе 5 блоков.",
                     "Static Field", "8 seconds of shocking, slowing field within 5 blocks."),
    "storm_call": ("storm", "Зов бури",
                   "Призывает грозу и бьёт молниями по врагам вокруг.",
                   "Storm Call", "Summons a thunderstorm and strikes nearby foes."),

    # --- Аркана ---
    "magic_missile": ("arcane", "Магические стрелы", "Три снаряда чистой магии.",
                      "Magic Missile", "Three bolts of raw magic."),
    "blink": ("arcane", "Скачок", "Мгновенный рывок на 16 блоков вперёд.",
              "Blink", "An instant dash up to 16 blocks ahead."),
    "arcane_explosion": ("arcane", "Аркановый разрыв",
                         "Взрыв чистой магии вокруг себя, не портящий ландшафт.",
                         "Arcane Explosion", "A burst of raw magic that leaves terrain intact."),
    "telekinesis": ("arcane", "Телекинез",
                    "Притягивает цель; в приседе — отбрасывает.",
                    "Telekinesis", "Pulls a target in; sneak to push it away."),
    "mana_shield": ("arcane", "Мановый щит",
                    "30 секунд входящий урон списывается с маны, а не со здоровья.",
                    "Mana Shield", "For 30s damage drains mana instead of health."),
    "arcane_sight": ("arcane", "Взор мага",
                     "45 секунд подсвечивает всё живое вокруг и даёт ночное зрение.",
                     "Arcane Sight", "45s of highlighted creatures and night vision."),
    "recall": ("arcane", "Возврат", "Переносит к установленному якорю перемещения.",
               "Recall", "Teleports you to your bound teleport anchor."),

    # --- Магия природы ---
    "mend": ("nature", "Исцеление", "Восстанавливает здоровье заклинателю.",
             "Mend", "Restores your own health."),
    "healing_bloom": ("nature", "Цветение жизни",
                      "Лечит всех союзников в радиусе 8 блоков и даёт регенерацию.",
                      "Healing Bloom", "Heals allies within 8 blocks and grants regeneration."),
    "entangle": ("nature", "Опутывание",
                 "Оплетает врагов паутиной и замедляет их.",
                 "Entangle", "Webs and slows enemies in the area."),
    "verdant_growth": ("nature", "Буйный рост",
                       "Как костная мука, но по области 9x5x9.",
                       "Verdant Growth", "Bone meal across a 9x5x9 area."),
    "thorn_volley": ("nature", "Залп шипов", "Пять ядовитых шипов веером.",
                     "Thorn Volley", "Five poisoned thorns in a spread."),
    "call_of_the_wild": ("nature", "Зов дикой природы",
                         "Призывает двух усиленных волков на 90 секунд.",
                         "Call of the Wild", "Summons two strengthened wolves for 90 seconds."),

    # --- Магия тени ---
    "shadow_bolt": ("shadow", "Теневой сгусток", "Снаряд, накладывающий иссушение.",
                    "Shadow Bolt", "A bolt that inflicts wither."),
    "life_drain": ("shadow", "Высасывание жизни",
                   "Урон цели возвращается вам здоровьем.",
                   "Life Drain", "Damage dealt returns to you as health."),
    "curse_of_decay": ("shadow", "Проклятие распада",
                       "Иссушение, слабость, выжигание маны и безмолвие.",
                       "Curse of Decay", "Wither, weakness, mana burn and silence."),
    "veil": ("shadow", "Покров тьмы", "20 секунд невидимости и ускорения.",
             "Veil", "20 seconds of invisibility and speed."),
    "summon_shades": ("shadow", "Призыв теней", "Три призрака на 60 секунд.",
                      "Summon Shades", "Three shades for 60 seconds."),
    "soul_harvest": ("shadow", "Жатва душ",
                     "Бьёт всех вокруг и лечит за каждую задетую цель.",
                     "Soul Harvest", "Strikes everyone near and heals per target hit."),

    # --- Магия света ---
    "smite": ("light", "Кара", "Точечный удар; по нежити — двойной урон.",
              "Smite", "A focused strike; double damage to the undead."),
    "holy_light": ("light", "Священный свет",
                   "Лечит союзников, жжёт нежить и ослепляет прочих.",
                   "Holy Light", "Heals allies, burns undead, blinds the rest."),
    "radiance": ("light", "Сияние",
                 "Расставляет вокруг источники света на 30 секунд.",
                 "Radiance", "Places light sources around you for 30 seconds."),
    "blessing": ("light", "Благословение",
                 "30 секунд усиленной магии, сопротивления и силы для вас и союзников.",
                 "Blessing", "30s of stronger magic, resistance and strength for your party."),
    "guardian": ("light", "Страж", "Призывает железного голема на 2 минуты.",
                 "Guardian", "Summons an iron golem for 2 minutes."),
    "purify": ("light", "Очищение",
               "Снимает все вредные эффекты, тушит огонь и немного лечит.",
               "Purify", "Clears all harmful effects, douses fire and heals a little."),
}
