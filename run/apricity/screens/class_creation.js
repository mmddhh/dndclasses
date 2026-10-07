var CLASS_FEATURES = {};

/* dndClasses class progression data.
 * Schema: CLASS_FEATURES["key"] = {
 *   levels:     [ { lv, items:[ { n:"中文名", e:"English", t:"可选描述" } ] } ],
 *   subclasses: [ { name, en, features:[ { lv, items:[...] } ] } ]
 * }
 * If "t" is omitted the page shows "<中文名>特性描述" as a placeholder.
 */
CLASS_FEATURES["barbarian"] = {
  levels: [
    { lv: 1, items: [ { n: "狂暴", e: "Rage" }, { n: "无甲防御", e: "Unarmored Defense" } ] },
    { lv: 2, items: [ { n: "鲁莽攻击", e: "Reckless Attack" }, { n: "危险感知", e: "Danger Sense" } ] },
    { lv: 3, items: [ { n: "原始道途", e: "Primal Path" } ] },
    { lv: 4, items: [ { n: "属性值提升", e: "Ability Score Improvement" } ] },
    { lv: 5, items: [ { n: "额外攻击", e: "Extra Attack" }, { n: "快速移动", e: "Fast Movement" } ] },
    { lv: 6, items: [ { n: "原始道途特性", e: "Primal Path feature" } ] },
    { lv: 7, items: [ { n: "野性直觉", e: "Feral Instinct" } ] },
    { lv: 8, items: [ { n: "属性值提升", e: "Ability Score Improvement" } ] },
    { lv: 9, items: [ { n: "残暴重击（1骰）", e: "Brutal Critical (1 die)" } ] },
    { lv: 10, items: [ { n: "原始道途特性", e: "Primal Path feature" } ] },
    { lv: 11, items: [ { n: "无尽狂暴", e: "Relentless Rage" } ] },
    { lv: 12, items: [ { n: "属性值提升", e: "Ability Score Improvement" } ] },
    { lv: 13, items: [ { n: "残暴重击（2骰）", e: "Brutal Critical (2 dice)" } ] },
    { lv: 14, items: [ { n: "原始道途特性", e: "Primal Path feature" } ] },
    { lv: 15, items: [ { n: "持久狂暴", e: "Persistent Rage" } ] },
    { lv: 16, items: [ { n: "属性值提升", e: "Ability Score Improvement" } ] },
    { lv: 17, items: [ { n: "残暴重击（3骰）", e: "Brutal Critical (3 dice)" } ] },
    { lv: 18, items: [ { n: "不屈威能", e: "Indomitable Might" } ] },
    { lv: 19, items: [ { n: "属性值提升", e: "Ability Score Improvement" } ] },
    { lv: 20, items: [ { n: "原始勇士", e: "Primal Champion" } ] }
  ],
  subclasses: [
    {
      name: "狂战士道途", en: "Path of the Berserker",
      features: [
        { lv: 3, items: [ { n: "狂乱", e: "Frenzy" } ] },
        { lv: 6, items: [ { n: "无心狂暴", e: "Mindless Rage" } ] },
        { lv: 10, items: [ { n: "威吓之姿", e: "Intimidating Presence" } ] },
        { lv: 14, items: [ { n: "报复", e: "Retaliation" } ] }
      ]
    },
    {
      name: "图腾战士道途", en: "Path of the Totem Warrior",
      features: [
        { lv: 3, items: [ { n: "灵魂追寻", e: "Spirit Seeker" }, { n: "图腾之魂", e: "Totem Spirit" } ] },
        { lv: 6, items: [ { n: "野兽之相", e: "Aspect of the Beast" } ] },
        { lv: 14, items: [ { n: "图腾调谐", e: "Totemic Attunement" } ] }
      ]
    }
  ]
};

CLASS_FEATURES["bard"] = {
  levels: [
    { lv: 1, items: [ { n: "施法", e: "Spellcasting" }, { n: "吟游激励（d6）", e: "Bardic Inspiration (d6)" } ] },
    { lv: 2, items: [ { n: "万事通", e: "Jack of All Trades" }, { n: "休憩之歌（d6）", e: "Song of Rest (d6)" } ] },
    { lv: 3, items: [ { n: "吟游诗人学院", e: "Bard College" }, { n: "专精", e: "Expertise" } ] },
    { lv: 4, items: [ { n: "属性值提升", e: "Ability Score Improvement" } ] },
    { lv: 5, items: [ { n: "吟游激励（d8）", e: "Bardic Inspiration (d8)" }, { n: "灵感源泉", e: "Font of Inspiration" } ] },
    { lv: 6, items: [ { n: "反魅惑", e: "Countercharm" }, { n: "吟游诗人学院特性", e: "Bard College feature" } ] },
    { lv: 7, items: [] },
    { lv: 8, items: [ { n: "属性值提升", e: "Ability Score Improvement" } ] },
    { lv: 9, items: [ { n: "休憩之歌（d8）", e: "Song of Rest (d8)" } ] },
    { lv: 10, items: [ { n: "吟游激励（d10）", e: "Bardic Inspiration (d10)" }, { n: "专精", e: "Expertise" } ] },
    { lv: 11, items: [] },
    { lv: 12, items: [ { n: "属性值提升", e: "Ability Score Improvement" } ] },
    { lv: 13, items: [ { n: "休憩之歌（d10）", e: "Song of Rest (d10)" } ] },
    { lv: 14, items: [ { n: "魔法秘辛", e: "Magical Secrets" }, { n: "吟游诗人学院特性", e: "Bard College feature" } ] },
    { lv: 15, items: [ { n: "吟游激励（d12）", e: "Bardic Inspiration (d12)" } ] },
    { lv: 16, items: [ { n: "属性值提升", e: "Ability Score Improvement" } ] },
    { lv: 17, items: [ { n: "休憩之歌（d12）", e: "Song of Rest (d12)" } ] },
    { lv: 18, items: [ { n: "魔法秘辛", e: "Magical Secrets" } ] },
    { lv: 19, items: [ { n: "属性值提升", e: "Ability Score Improvement" } ] },
    { lv: 20, items: [ { n: "超凡灵感", e: "Superior Inspiration" } ] }
  ],
  subclasses: [
    {
      name: "博识学院", en: "College of Lore",
      features: [
        { lv: 3, items: [ { n: "额外熟练", e: "Bonus Proficiencies" }, { n: "尖刻言语", e: "Cutting Words" } ] },
        { lv: 6, items: [ { n: "额外魔法秘辛", e: "Additional Magical Secrets" } ] },
        { lv: 14, items: [ { n: "无匹技艺", e: "Peerless Skill" } ] }
      ]
    },
    {
      name: "勇气学院", en: "College of Valor",
      features: [
        { lv: 3, items: [ { n: "额外熟练", e: "Bonus Proficiencies" }, { n: "战斗激励", e: "Combat Inspiration" } ] },
        { lv: 6, items: [ { n: "额外攻击", e: "Extra Attack" } ] },
        { lv: 14, items: [ { n: "战斗魔法", e: "Battle Magic" } ] }
      ]
    }
  ]
};

CLASS_FEATURES["cleric"] = {
  levels: [
    { lv: 1, items: [ { n: "施法", e: "Spellcasting" }, { n: "神圣领域", e: "Divine Domain" } ] },
    { lv: 2, items: [ { n: "引导神力（驱散不死）", e: "Channel Divinity (Turn Undead)" }, { n: "神圣领域特性", e: "Divine Domain feature" } ] },
    { lv: 3, items: [] },
    { lv: 4, items: [ { n: "属性值提升", e: "Ability Score Improvement" } ] },
    { lv: 5, items: [ { n: "摧毁不死（CR 1/2）", e: "Destroy Undead (CR 1/2)" } ] },
    { lv: 6, items: [ { n: "引导神力（2次/休息）", e: "Channel Divinity (2/rest)" }, { n: "神圣领域特性", e: "Divine Domain feature" } ] },
    { lv: 7, items: [] },
    { lv: 8, items: [ { n: "属性值提升", e: "Ability Score Improvement" }, { n: "摧毁不死（CR 1）", e: "Destroy Undead (CR 1)" }, { n: "神圣领域特性", e: "Divine Domain feature" } ] },
    { lv: 9, items: [] },
    { lv: 10, items: [ { n: "神圣干预", e: "Divine Intervention" } ] },
    { lv: 11, items: [ { n: "摧毁不死（CR 2）", e: "Destroy Undead (CR 2)" } ] },
    { lv: 12, items: [ { n: "属性值提升", e: "Ability Score Improvement" } ] },
    { lv: 13, items: [] },
    { lv: 14, items: [ { n: "摧毁不死（CR 3）", e: "Destroy Undead (CR 3)" } ] },
    { lv: 15, items: [] },
    { lv: 16, items: [ { n: "属性值提升", e: "Ability Score Improvement" } ] },
    { lv: 17, items: [ { n: "摧毁不死（CR 4）", e: "Destroy Undead (CR 4)" }, { n: "神圣领域特性", e: "Divine Domain feature" } ] },
    { lv: 18, items: [ { n: "引导神力（3次/休息）", e: "Channel Divinity (3/rest)" } ] },
    { lv: 19, items: [ { n: "属性值提升", e: "Ability Score Improvement" } ] },
    { lv: 20, items: [ { n: "神圣干预强化", e: "Divine Intervention improvement" } ] }
  ],
  subclasses: [
    {
      name: "知识领域", en: "Knowledge Domain",
      features: [
        { lv: 1, items: [ { n: "知识祝福", e: "Blessings of Knowledge" } ] },
        { lv: 2, items: [ { n: "岁月知识", e: "Knowledge of the Ages" } ] },
        { lv: 6, items: [ { n: "读心", e: "Read Thoughts" } ] },
        { lv: 8, items: [ { n: "强力施法", e: "Potent Spellcasting" } ] },
        { lv: 17, items: [ { n: "过去幻象", e: "Visions of the Past" } ] }
      ]
    },
    {
      name: "生命领域", en: "Life Domain",
      features: [
        { lv: 1, items: [ { n: "额外熟练", e: "Bonus Proficiency" }, { n: "生命门徒", e: "Disciple of Life" } ] },
        { lv: 2, items: [ { n: "保全生命", e: "Preserve Life" } ] },
        { lv: 6, items: [ { n: "受祝医者", e: "Blessed Healer" } ] },
        { lv: 8, items: [ { n: "神圣打击", e: "Divine Strike" } ] },
        { lv: 17, items: [ { n: "至善治疗", e: "Supreme Healing" } ] }
      ]
    },
    {
      name: "光明领域", en: "Light Domain",
      features: [
        { lv: 1, items: [ { n: "额外戏法", e: "Bonus Cantrip" }, { n: "守护闪光", e: "Warding Flare" } ] },
        { lv: 2, items: [ { n: "黎明光辉", e: "Radiance of the Dawn" } ] },
        { lv: 6, items: [ { n: "强化闪光", e: "Improved Flare" } ] },
        { lv: 8, items: [ { n: "强力施法", e: "Potent Spellcasting" } ] },
        { lv: 17, items: [ { n: "光明之冠", e: "Corona of Light" } ] }
      ]
    },
    {
      name: "自然领域", en: "Nature Domain",
      features: [
        { lv: 1, items: [ { n: "自然侍僧", e: "Acolyte of Nature" }, { n: "额外熟练", e: "Bonus Proficiency" } ] },
        { lv: 2, items: [ { n: "魅惑动植物", e: "Charm Animals and Plants" } ] },
        { lv: 6, items: [ { n: "压制元素", e: "Dampen Elements" } ] },
        { lv: 8, items: [ { n: "神圣打击", e: "Divine Strike" } ] },
        { lv: 17, items: [ { n: "自然大师", e: "Master of Nature" } ] }
      ]
    },
    {
      name: "风暴领域", en: "Tempest Domain",
      features: [
        { lv: 1, items: [ { n: "额外熟练", e: "Bonus Proficiencies" }, { n: "风暴之怒", e: "Wrath of the Storm" } ] },
        { lv: 2, items: [ { n: "毁灭之怒", e: "Destructive Wrath" } ] },
        { lv: 6, items: [ { n: "雷击", e: "Thunderbolt Strike" } ] },
        { lv: 8, items: [ { n: "神圣打击", e: "Divine Strike" } ] },
        { lv: 17, items: [ { n: "风暴降生", e: "Stormborn" } ] }
      ]
    },
    {
      name: "诡计领域", en: "Trickery Domain",
      features: [
        { lv: 1, items: [ { n: "诡术师祝福", e: "Blessing of the Trickster" } ] },
        { lv: 2, items: [ { n: "召唤分身", e: "Invoke Duplicity" } ] },
        { lv: 6, items: [ { n: "暗影斗篷", e: "Cloak of Shadows" } ] },
        { lv: 8, items: [ { n: "神圣打击", e: "Divine Strike" } ] },
        { lv: 17, items: [ { n: "强化分身", e: "Improved Duplicity" } ] }
      ]
    },
    {
      name: "战争领域", en: "War Domain",
      features: [
        { lv: 1, items: [ { n: "额外熟练", e: "Bonus Proficiencies" }, { n: "战争牧师", e: "War Priest" } ] },
        { lv: 2, items: [ { n: "指引打击", e: "Guided Strike" } ] },
        { lv: 6, items: [ { n: "战神祝福", e: "War God's Blessing" } ] },
        { lv: 8, items: [ { n: "神圣打击", e: "Divine Strike" } ] },
        { lv: 17, items: [ { n: "战斗化身", e: "Avatar of Battle" } ] }
      ]
    }
  ]
};

CLASS_FEATURES["druid"] = {
  levels: [
    { lv: 1, items: [ { n: "德鲁伊语", e: "Druidic" }, { n: "施法", e: "Spellcasting" } ] },
    { lv: 2, items: [ { n: "野性形态", e: "Wild Shape" }, { n: "德鲁伊结社", e: "Druid Circle" } ] },
    { lv: 3, items: [] },
    { lv: 4, items: [ { n: "野性形态强化（CR 1/2）", e: "Wild Shape improvement" }, { n: "属性值提升", e: "Ability Score Improvement" } ] },
    { lv: 5, items: [] },
    { lv: 6, items: [ { n: "德鲁伊结社特性", e: "Druid Circle feature" } ] },
    { lv: 7, items: [] },
    { lv: 8, items: [ { n: "野性形态强化（CR 1）", e: "Wild Shape improvement" }, { n: "属性值提升", e: "Ability Score Improvement" } ] },
    { lv: 9, items: [] },
    { lv: 10, items: [ { n: "德鲁伊结社特性", e: "Druid Circle feature" } ] },
    { lv: 11, items: [] },
    { lv: 12, items: [ { n: "属性值提升", e: "Ability Score Improvement" } ] },
    { lv: 13, items: [] },
    { lv: 14, items: [ { n: "德鲁伊结社特性", e: "Druid Circle feature" } ] },
    { lv: 15, items: [] },
    { lv: 16, items: [ { n: "属性值提升", e: "Ability Score Improvement" } ] },
    { lv: 17, items: [] },
    { lv: 18, items: [ { n: "不老之躯", e: "Timeless Body" }, { n: "野兽施法", e: "Beast Spells" } ] },
    { lv: 19, items: [ { n: "属性值提升", e: "Ability Score Improvement" } ] },
    { lv: 20, items: [ { n: "大德鲁伊", e: "Archdruid" } ] }
  ],
  subclasses: [
    {
      name: "大地结社", en: "Circle of the Land",
      features: [
        { lv: 2, items: [ { n: "额外戏法", e: "Bonus Cantrip" }, { n: "自然恢复", e: "Natural Recovery" } ] },
        { lv: 3, items: [ { n: "结社法术", e: "Circle Spells" } ] },
        { lv: 6, items: [ { n: "大地疾行", e: "Land's Stride" } ] },
        { lv: 10, items: [ { n: "自然守护", e: "Nature's Ward" } ] },
        { lv: 14, items: [ { n: "自然圣所", e: "Nature's Sanctuary" } ] }
      ]
    },
    {
      name: "月亮结社", en: "Circle of the Moon",
      features: [
        { lv: 2, items: [ { n: "战斗野性形态", e: "Combat Wild Shape" }, { n: "结社形态", e: "Circle Forms" } ] },
        { lv: 6, items: [ { n: "原始打击", e: "Primal Strike" } ] },
        { lv: 10, items: [ { n: "元素野性形态", e: "Elemental Wild Shape" } ] },
        { lv: 14, items: [ { n: "千形", e: "Thousand Forms" } ] }
      ]
    }
  ]
};

CLASS_FEATURES["fighter"] = {
  levels: [
    { lv: 1, items: [ { n: "战斗风格", e: "Fighting Style" }, { n: "回气", e: "Second Wind" } ] },
    { lv: 2, items: [ { n: "动作如潮", e: "Action Surge" } ] },
    { lv: 3, items: [ { n: "武术范型", e: "Martial Archetype" } ] },
    { lv: 4, items: [ { n: "属性值提升", e: "Ability Score Improvement" } ] },
    { lv: 5, items: [ { n: "额外攻击", e: "Extra Attack" } ] },
    { lv: 6, items: [ { n: "属性值提升", e: "Ability Score Improvement" } ] },
    { lv: 7, items: [ { n: "武术范型特性", e: "Martial Archetype feature" } ] },
    { lv: 8, items: [ { n: "属性值提升", e: "Ability Score Improvement" } ] },
    { lv: 9, items: [ { n: "不屈", e: "Indomitable" } ] },
    { lv: 10, items: [ { n: "武术范型特性", e: "Martial Archetype feature" } ] },
    { lv: 11, items: [ { n: "额外攻击（2）", e: "Extra Attack (2)" } ] },
    { lv: 12, items: [ { n: "属性值提升", e: "Ability Score Improvement" } ] },
    { lv: 13, items: [ { n: "不屈（2）", e: "Indomitable (2)" } ] },
    { lv: 14, items: [ { n: "属性值提升", e: "Ability Score Improvement" } ] },
    { lv: 15, items: [ { n: "武术范型特性", e: "Martial Archetype feature" } ] },
    { lv: 16, items: [ { n: "属性值提升", e: "Ability Score Improvement" } ] },
    { lv: 17, items: [ { n: "动作如潮（2）", e: "Action Surge (2)" }, { n: "不屈（3）", e: "Indomitable (3)" } ] },
    { lv: 18, items: [ { n: "武术范型特性", e: "Martial Archetype feature" } ] },
    { lv: 19, items: [ { n: "属性值提升", e: "Ability Score Improvement" } ] },
    { lv: 20, items: [ { n: "额外攻击（3）", e: "Extra Attack (3)" } ] }
  ],
  subclasses: [
    {
      name: "勇士", en: "Champion",
      features: [
        { lv: 3, items: [ { n: "强化重击", e: "Improved Critical" } ] },
        { lv: 7, items: [ { n: "非凡运动员", e: "Remarkable Athlete" } ] },
        { lv: 10, items: [ { n: "额外战斗风格", e: "Additional Fighting Style" } ] },
        { lv: 15, items: [ { n: "卓越重击", e: "Superior Critical" } ] },
        { lv: 18, items: [ { n: "生存者", e: "Survivor" } ] }
      ]
    },
    {
      name: "战斗大师", en: "Battle Master",
      features: [
        { lv: 3, items: [ { n: "战斗优势", e: "Combat Superiority" }, { n: "战争学徒", e: "Student of War" } ] },
        { lv: 7, items: [ { n: "知彼", e: "Know Your Enemy" } ] },
        { lv: 10, items: [ { n: "强化战斗优势（d10）", e: "Improved Combat Superiority (d10)" } ] },
        { lv: 15, items: [ { n: "不屈", e: "Relentless" } ] },
        { lv: 18, items: [ { n: "强化战斗优势（d12）", e: "Improved Combat Superiority (d12)" } ] }
      ]
    },
    {
      name: "奥法骑士", en: "Eldritch Knight",
      features: [
        { lv: 3, items: [ { n: "施法", e: "Spellcasting" }, { n: "武器联结", e: "Weapon Bond" } ] },
        { lv: 7, items: [ { n: "战斗魔法", e: "War Magic" } ] },
        { lv: 10, items: [ { n: "奥法打击", e: "Eldritch Strike" } ] },
        { lv: 15, items: [ { n: "奥术冲锋", e: "Arcane Charge" } ] },
        { lv: 18, items: [ { n: "强化战斗魔法", e: "Improved War Magic" } ] }
      ]
    }
  ]
};

CLASS_FEATURES["monk"] = {
  levels: [
    { lv: 1, items: [ { n: "无甲防御", e: "Unarmored Defense" }, { n: "武艺", e: "Martial Arts" } ] },
    { lv: 2, items: [ { n: "气", e: "Ki" }, { n: "无甲移动", e: "Unarmored Movement" } ] },
    { lv: 3, items: [ { n: "武僧传统", e: "Monastic Tradition" }, { n: "偏转飞弹", e: "Deflect Missiles" } ] },
    { lv: 4, items: [ { n: "属性值提升", e: "Ability Score Improvement" }, { n: "缓落", e: "Slow Fall" } ] },
    { lv: 5, items: [ { n: "额外攻击", e: "Extra Attack" }, { n: "震慑拳", e: "Stunning Strike" } ] },
    { lv: 6, items: [ { n: "气灌打击", e: "Ki-Empowered Strikes" }, { n: "武僧传统特性", e: "Monastic Tradition feature" } ] },
    { lv: 7, items: [ { n: "闪避", e: "Evasion" }, { n: "心境止水", e: "Stillness of Mind" } ] },
    { lv: 8, items: [ { n: "属性值提升", e: "Ability Score Improvement" } ] },
    { lv: 9, items: [ { n: "无甲移动强化", e: "Unarmored Movement improvement" } ] },
    { lv: 10, items: [ { n: "身心纯净", e: "Purity of Body" } ] },
    { lv: 11, items: [ { n: "武僧传统特性", e: "Monastic Tradition feature" } ] },
    { lv: 12, items: [ { n: "属性值提升", e: "Ability Score Improvement" } ] },
    { lv: 13, items: [ { n: "日月之语", e: "Tongue of the Sun and Moon" } ] },
    { lv: 14, items: [ { n: "金刚心", e: "Diamond Soul" } ] },
    { lv: 15, items: [ { n: "不老之躯", e: "Timeless Body" } ] },
    { lv: 16, items: [ { n: "属性值提升", e: "Ability Score Improvement" } ] },
    { lv: 17, items: [ { n: "武僧传统特性", e: "Monastic Tradition feature" } ] },
    { lv: 18, items: [ { n: "空之躯", e: "Empty Body" } ] },
    { lv: 19, items: [ { n: "属性值提升", e: "Ability Score Improvement" } ] },
    { lv: 20, items: [ { n: "完美自我", e: "Perfect Self" } ] }
  ],
  subclasses: [
    {
      name: "开手之道", en: "Way of the Open Hand",
      features: [
        { lv: 3, items: [ { n: "开手技法", e: "Open Hand Technique" } ] },
        { lv: 6, items: [ { n: "身体健全", e: "Wholeness of Body" } ] },
        { lv: 11, items: [ { n: "宁静", e: "Tranquility" } ] },
        { lv: 17, items: [ { n: "震颤掌", e: "Quivering Palm" } ] }
      ]
    },
    {
      name: "暗影之道", en: "Way of Shadow",
      features: [
        { lv: 3, items: [ { n: "暗影技艺", e: "Shadow Arts" } ] },
        { lv: 6, items: [ { n: "暗影步", e: "Shadow Step" } ] },
        { lv: 11, items: [ { n: "暗影斗篷", e: "Cloak of Shadows" } ] },
        { lv: 17, items: [ { n: "投机者", e: "Opportunist" } ] }
      ]
    },
    {
      name: "四象之道", en: "Way of the Four Elements",
      features: [
        { lv: 3, items: [ { n: "元素门徒", e: "Disciple of the Elements" }, { n: "元素法门", e: "Elemental Disciplines" } ] },
        { lv: 6, items: [ { n: "元素法门（更多）", e: "Elemental Disciplines" } ] },
        { lv: 11, items: [ { n: "元素法门（更多）", e: "Elemental Disciplines" } ] },
        { lv: 17, items: [ { n: "元素法门（更多）", e: "Elemental Disciplines" } ] }
      ]
    }
  ]
};

CLASS_FEATURES["paladin"] = {
  levels: [
    { lv: 1, items: [ { n: "神圣感知", e: "Divine Sense" }, { n: "圣疗", e: "Lay on Hands" } ] },
    { lv: 2, items: [ { n: "战斗风格", e: "Fighting Style" }, { n: "施法", e: "Spellcasting" }, { n: "神圣破斩", e: "Divine Smite" } ] },
    { lv: 3, items: [ { n: "神圣健康", e: "Divine Health" }, { n: "神圣誓言", e: "Sacred Oath" } ] },
    { lv: 4, items: [ { n: "属性值提升", e: "Ability Score Improvement" } ] },
    { lv: 5, items: [ { n: "额外攻击", e: "Extra Attack" } ] },
    { lv: 6, items: [ { n: "守护灵光", e: "Aura of Protection" } ] },
    { lv: 7, items: [ { n: "神圣誓言特性", e: "Sacred Oath feature" } ] },
    { lv: 8, items: [ { n: "属性值提升", e: "Ability Score Improvement" } ] },
    { lv: 9, items: [] },
    { lv: 10, items: [ { n: "勇气灵光", e: "Aura of Courage" } ] },
    { lv: 11, items: [ { n: "强化神圣破斩", e: "Improved Divine Smite" } ] },
    { lv: 12, items: [ { n: "属性值提升", e: "Ability Score Improvement" } ] },
    { lv: 13, items: [] },
    { lv: 14, items: [ { n: "净化之触", e: "Cleansing Touch" } ] },
    { lv: 15, items: [ { n: "神圣誓言特性", e: "Sacred Oath feature" } ] },
    { lv: 16, items: [ { n: "属性值提升", e: "Ability Score Improvement" } ] },
    { lv: 17, items: [] },
    { lv: 18, items: [ { n: "灵光范围强化", e: "Aura improvements" } ] },
    { lv: 19, items: [ { n: "属性值提升", e: "Ability Score Improvement" } ] },
    { lv: 20, items: [ { n: "神圣誓言特性", e: "Sacred Oath feature" } ] }
  ],
  subclasses: [
    {
      name: "奉献之誓", en: "Oath of Devotion",
      features: [
        { lv: 3, items: [ { n: "神圣武器", e: "Sacred Weapon" }, { n: "驱散邪物", e: "Turn the Unholy" } ] },
        { lv: 7, items: [ { n: "奉献灵光", e: "Aura of Devotion" } ] },
        { lv: 15, items: [ { n: "灵魂纯净", e: "Purity of Spirit" } ] },
        { lv: 20, items: [ { n: "神圣光环", e: "Holy Nimbus" } ] }
      ]
    },
    {
      name: "上古之誓", en: "Oath of the Ancients",
      features: [
        { lv: 3, items: [ { n: "自然之怒", e: "Nature's Wrath" }, { n: "驱散无信者", e: "Turn the Faithless" } ] },
        { lv: 7, items: [ { n: "守护灵光", e: "Aura of Warding" } ] },
        { lv: 15, items: [ { n: "不灭哨兵", e: "Undying Sentinel" } ] },
        { lv: 20, items: [ { n: "上古勇士", e: "Elder Champion" } ] }
      ]
    },
    {
      name: "复仇之誓", en: "Oath of Vengeance",
      features: [
        { lv: 3, items: [ { n: "弃绝敌人", e: "Abjure Enemy" }, { n: "敌誓", e: "Vow of Enmity" } ] },
        { lv: 7, items: [ { n: "不屈复仇者", e: "Relentless Avenger" } ] },
        { lv: 15, items: [ { n: "复仇之魂", e: "Soul of Vengeance" } ] },
        { lv: 20, items: [ { n: "复仇天使", e: "Avenging Angel" } ] }
      ]
    }
  ]
};

CLASS_FEATURES["ranger"] = {
  levels: [
    { lv: 1, items: [ { n: "宿敌", e: "Favored Enemy" }, { n: "自然探索者", e: "Natural Explorer" } ] },
    { lv: 2, items: [ { n: "战斗风格", e: "Fighting Style" }, { n: "施法", e: "Spellcasting" } ] },
    { lv: 3, items: [ { n: "原始感知", e: "Primeval Awareness" }, { n: "游侠范型", e: "Ranger Archetype" } ] },
    { lv: 4, items: [ { n: "属性值提升", e: "Ability Score Improvement" } ] },
    { lv: 5, items: [ { n: "额外攻击", e: "Extra Attack" } ] },
    { lv: 6, items: [ { n: "宿敌与自然探索者强化", e: "Favored Enemy and Natural Explorer improvements" } ] },
    { lv: 7, items: [ { n: "游侠范型特性", e: "Ranger Archetype feature" } ] },
    { lv: 8, items: [ { n: "属性值提升", e: "Ability Score Improvement" }, { n: "大地疾行", e: "Land's Stride" } ] },
    { lv: 9, items: [] },
    { lv: 10, items: [ { n: "自然探索者强化", e: "Natural Explorer improvement" }, { n: "林间隐匿", e: "Hide in Plain Sight" } ] },
    { lv: 11, items: [ { n: "游侠范型特性", e: "Ranger Archetype feature" } ] },
    { lv: 12, items: [ { n: "属性值提升", e: "Ability Score Improvement" } ] },
    { lv: 13, items: [] },
    { lv: 14, items: [ { n: "宿敌强化", e: "Favored Enemy improvement" }, { n: "消失", e: "Vanish" } ] },
    { lv: 15, items: [ { n: "游侠范型特性", e: "Ranger Archetype feature" } ] },
    { lv: 16, items: [ { n: "属性值提升", e: "Ability Score Improvement" } ] },
    { lv: 17, items: [] },
    { lv: 18, items: [ { n: "野性感知", e: "Feral Senses" } ] },
    { lv: 19, items: [ { n: "属性值提升", e: "Ability Score Improvement" } ] },
    { lv: 20, items: [ { n: "屠戮者", e: "Foe Slayer" } ] }
  ],
  subclasses: [
    {
      name: "猎人", en: "Hunter",
      features: [
        { lv: 3, items: [ { n: "猎人猎物", e: "Hunter's Prey" } ] },
        { lv: 7, items: [ { n: "防御战术", e: "Defensive Tactics" } ] },
        { lv: 11, items: [ { n: "多重攻击", e: "Multiattack" } ] },
        { lv: 15, items: [ { n: "卓越猎手防御", e: "Superior Hunter's Defense" } ] }
      ]
    },
    {
      name: "驯兽师", en: "Beast Master",
      features: [
        { lv: 3, items: [ { n: "游侠伙伴", e: "Ranger's Companion" } ] },
        { lv: 7, items: [ { n: "卓越训练", e: "Exceptional Training" } ] },
        { lv: 11, items: [ { n: "野兽之怒", e: "Bestial Fury" } ] },
        { lv: 15, items: [ { n: "共享法术", e: "Share Spells" } ] }
      ]
    }
  ]
};

CLASS_FEATURES["rogue"] = {
  levels: [
    { lv: 1, items: [ { n: "专精", e: "Expertise" }, { n: "偷袭", e: "Sneak Attack" }, { n: "盗贼黑话", e: "Thieves' Cant" } ] },
    { lv: 2, items: [ { n: "灵巧动作", e: "Cunning Action" } ] },
    { lv: 3, items: [ { n: "游荡者范型", e: "Roguish Archetype" } ] },
    { lv: 4, items: [ { n: "属性值提升", e: "Ability Score Improvement" } ] },
    { lv: 5, items: [ { n: "闪避", e: "Uncanny Dodge" } ] },
    { lv: 6, items: [ { n: "专精", e: "Expertise" } ] },
    { lv: 7, items: [ { n: "闪避", e: "Evasion" } ] },
    { lv: 8, items: [ { n: "属性值提升", e: "Ability Score Improvement" } ] },
    { lv: 9, items: [ { n: "游荡者范型特性", e: "Roguish Archetype feature" } ] },
    { lv: 10, items: [ { n: "属性值提升", e: "Ability Score Improvement" } ] },
    { lv: 11, items: [ { n: "可靠才能", e: "Reliable Talent" } ] },
    { lv: 12, items: [ { n: "属性值提升", e: "Ability Score Improvement" } ] },
    { lv: 13, items: [ { n: "游荡者范型特性", e: "Roguish Archetype feature" } ] },
    { lv: 14, items: [ { n: "盲感", e: "Blindsense" } ] },
    { lv: 15, items: [ { n: "机敏心智", e: "Slippery Mind" } ] },
    { lv: 16, items: [ { n: "属性值提升", e: "Ability Score Improvement" } ] },
    { lv: 17, items: [ { n: "游荡者范型特性", e: "Roguish Archetype feature" } ] },
    { lv: 18, items: [ { n: "飘忽不定", e: "Elusive" } ] },
    { lv: 19, items: [ { n: "属性值提升", e: "Ability Score Improvement" } ] },
    { lv: 20, items: [ { n: "幸运一击", e: "Stroke of Luck" } ] }
  ],
  subclasses: [
    {
      name: "盗贼", en: "Thief",
      features: [
        { lv: 3, items: [ { n: "快手", e: "Fast Hands" }, { n: "飞檐走壁", e: "Second-Story Work" } ] },
        { lv: 9, items: [ { n: "超凡潜行", e: "Supreme Sneak" } ] },
        { lv: 13, items: [ { n: "使用魔法装置", e: "Use Magic Device" } ] },
        { lv: 17, items: [ { n: "盗贼反射", e: "Thief's Reflexes" } ] }
      ]
    },
    {
      name: "刺客", en: "Assassin",
      features: [
        { lv: 3, items: [ { n: "额外熟练", e: "Bonus Proficiencies" }, { n: "暗杀", e: "Assassinate" } ] },
        { lv: 9, items: [ { n: "渗透专家", e: "Infiltration Expertise" } ] },
        { lv: 13, items: [ { n: "伪装大师", e: "Impostor" } ] },
        { lv: 17, items: [ { n: "死亡打击", e: "Death Strike" } ] }
      ]
    },
    {
      name: "奥法诡术师", en: "Arcane Trickster",
      features: [
        { lv: 3, items: [ { n: "施法", e: "Spellcasting" }, { n: "法师之手巧技", e: "Mage Hand Legerdemain" } ] },
        { lv: 9, items: [ { n: "魔法伏击", e: "Magical Ambush" } ] },
        { lv: 13, items: [ { n: "多面诡术师", e: "Versatile Trickster" } ] },
        { lv: 17, items: [ { n: "法术窃取", e: "Spell Thief" } ] }
      ]
    }
  ]
};

CLASS_FEATURES["sorcerer"] = {
  levels: [
    { lv: 1, items: [ { n: "施法", e: "Spellcasting" }, { n: "术法起源", e: "Sorcerous Origin" } ] },
    { lv: 2, items: [ { n: "魔法源泉", e: "Font of Magic" } ] },
    { lv: 3, items: [ { n: "超魔法", e: "Metamagic" } ] },
    { lv: 4, items: [ { n: "属性值提升", e: "Ability Score Improvement" } ] },
    { lv: 5, items: [] },
    { lv: 6, items: [ { n: "术法起源特性", e: "Sorcerous Origin feature" } ] },
    { lv: 7, items: [] },
    { lv: 8, items: [ { n: "属性值提升", e: "Ability Score Improvement" } ] },
    { lv: 9, items: [] },
    { lv: 10, items: [ { n: "超魔法", e: "Metamagic" } ] },
    { lv: 11, items: [] },
    { lv: 12, items: [ { n: "属性值提升", e: "Ability Score Improvement" } ] },
    { lv: 13, items: [] },
    { lv: 14, items: [ { n: "术法起源特性", e: "Sorcerous Origin feature" } ] },
    { lv: 15, items: [] },
    { lv: 16, items: [ { n: "属性值提升", e: "Ability Score Improvement" } ] },
    { lv: 17, items: [ { n: "超魔法", e: "Metamagic" } ] },
    { lv: 18, items: [ { n: "术法起源特性", e: "Sorcerous Origin feature" } ] },
    { lv: 19, items: [ { n: "属性值提升", e: "Ability Score Improvement" } ] },
    { lv: 20, items: [ { n: "术法恢复", e: "Sorcerous Restoration" } ] }
  ],
  subclasses: [
    {
      name: "龙族血脉", en: "Draconic Bloodline",
      features: [
        { lv: 1, items: [ { n: "龙族先祖", e: "Dragon Ancestor" }, { n: "龙族韧性", e: "Draconic Resilience" } ] },
        { lv: 6, items: [ { n: "元素亲和", e: "Elemental Affinity" } ] },
        { lv: 14, items: [ { n: "龙翼", e: "Dragon Wings" } ] },
        { lv: 18, items: [ { n: "龙之威仪", e: "Draconic Presence" } ] }
      ]
    },
    {
      name: "狂野魔法", en: "Wild Magic",
      features: [
        { lv: 1, items: [ { n: "狂野魔法涌动", e: "Wild Magic Surge" }, { n: "混沌之潮", e: "Tides of Chaos" } ] },
        { lv: 6, items: [ { n: "扭曲幸运", e: "Bend Luck" } ] },
        { lv: 14, items: [ { n: "受控混沌", e: "Controlled Chaos" } ] },
        { lv: 18, items: [ { n: "法术轰击", e: "Spell Bombardment" } ] }
      ]
    }
  ]
};

CLASS_FEATURES["warlock"] = {
  levels: [
    { lv: 1, items: [ { n: "异界宗主", e: "Otherworldly Patron" }, { n: "契约魔法", e: "Pact Magic" } ] },
    { lv: 2, items: [ { n: "魔能祈唤", e: "Eldritch Invocations" } ] },
    { lv: 3, items: [ { n: "契约恩赐", e: "Pact Boon" } ] },
    { lv: 4, items: [ { n: "属性值提升", e: "Ability Score Improvement" } ] },
    { lv: 5, items: [] },
    { lv: 6, items: [ { n: "异界宗主特性", e: "Otherworldly Patron feature" } ] },
    { lv: 7, items: [] },
    { lv: 8, items: [ { n: "属性值提升", e: "Ability Score Improvement" } ] },
    { lv: 9, items: [] },
    { lv: 10, items: [ { n: "异界宗主特性", e: "Otherworldly Patron feature" } ] },
    { lv: 11, items: [ { n: "秘法奥秘（6环）", e: "Mystic Arcanum (6th level)" } ] },
    { lv: 12, items: [ { n: "属性值提升", e: "Ability Score Improvement" } ] },
    { lv: 13, items: [ { n: "秘法奥秘（7环）", e: "Mystic Arcanum (7th level)" } ] },
    { lv: 14, items: [ { n: "异界宗主特性", e: "Otherworldly Patron feature" } ] },
    { lv: 15, items: [ { n: "秘法奥秘（8环）", e: "Mystic Arcanum (8th level)" } ] },
    { lv: 16, items: [ { n: "属性值提升", e: "Ability Score Improvement" } ] },
    { lv: 17, items: [ { n: "秘法奥秘（9环）", e: "Mystic Arcanum (9th level)" } ] },
    { lv: 18, items: [] },
    { lv: 19, items: [ { n: "属性值提升", e: "Ability Score Improvement" } ] },
    { lv: 20, items: [ { n: "魔能大师", e: "Eldritch Master" } ] }
  ],
  subclasses: [
    {
      name: "妖精宗主", en: "The Archfey",
      features: [
        { lv: 1, items: [ { n: "妖精显灵", e: "Fey Presence" } ] },
        { lv: 6, items: [ { n: "迷雾脱逃", e: "Misty Escape" } ] },
        { lv: 10, items: [ { n: "迷惑防御", e: "Beguiling Defenses" } ] },
        { lv: 14, items: [ { n: "黑暗谵妄", e: "Dark Delirium" } ] }
      ]
    },
    {
      name: "邪魔宗主", en: "The Fiend",
      features: [
        { lv: 1, items: [ { n: "黑暗祝福", e: "Dark One's Blessing" } ] },
        { lv: 6, items: [ { n: "黑暗幸运", e: "Dark One's Own Luck" } ] },
        { lv: 10, items: [ { n: "邪魔韧性", e: "Fiendish Resilience" } ] },
        { lv: 14, items: [ { n: "掷入地狱", e: "Hurl Through Hell" } ] }
      ]
    },
    {
      name: "旧日支配者", en: "The Great Old One",
      features: [
        { lv: 1, items: [ { n: "觉醒心灵", e: "Awakened Mind" } ] },
        { lv: 6, items: [ { n: "熵之守护", e: "Entropic Ward" } ] },
        { lv: 10, items: [ { n: "思想护盾", e: "Thought Shield" } ] },
        { lv: 14, items: [ { n: "创造奴仆", e: "Create Thrall" } ] }
      ]
    }
  ]
};

CLASS_FEATURES["wizard"] = {
  levels: [
    { lv: 1, items: [ { n: "施法", e: "Spellcasting" }, { n: "奥术恢复", e: "Arcane Recovery" } ] },
    { lv: 2, items: [ { n: "奥术传统", e: "Arcane Tradition" } ] },
    { lv: 3, items: [] },
    { lv: 4, items: [ { n: "属性值提升", e: "Ability Score Improvement" } ] },
    { lv: 5, items: [] },
    { lv: 6, items: [ { n: "奥术传统特性", e: "Arcane Tradition feature" } ] },
    { lv: 7, items: [] },
    { lv: 8, items: [ { n: "属性值提升", e: "Ability Score Improvement" } ] },
    { lv: 9, items: [] },
    { lv: 10, items: [ { n: "奥术传统特性", e: "Arcane Tradition feature" } ] },
    { lv: 11, items: [] },
    { lv: 12, items: [ { n: "属性值提升", e: "Ability Score Improvement" } ] },
    { lv: 13, items: [] },
    { lv: 14, items: [ { n: "奥术传统特性", e: "Arcane Tradition feature" } ] },
    { lv: 15, items: [] },
    { lv: 16, items: [ { n: "属性值提升", e: "Ability Score Improvement" } ] },
    { lv: 17, items: [] },
    { lv: 18, items: [ { n: "法术精通", e: "Spell Mastery" } ] },
    { lv: 19, items: [ { n: "属性值提升", e: "Ability Score Improvement" } ] },
    { lv: 20, items: [ { n: "招牌法术", e: "Signature Spells" } ] }
  ],
  subclasses: [
    {
      name: "防护学派", en: "School of Abjuration",
      features: [
        { lv: 2, items: [ { n: "防护学者", e: "Abjuration Savant" }, { n: "奥术守御", e: "Arcane Ward" } ] },
        { lv: 6, items: [ { n: "投射守御", e: "Projected Ward" } ] },
        { lv: 10, items: [ { n: "强化防护", e: "Improved Abjuration" } ] },
        { lv: 14, items: [ { n: "法术抗性", e: "Spell Resistance" } ] }
      ]
    },
    {
      name: "咒法学派", en: "School of Conjuration",
      features: [
        { lv: 2, items: [ { n: "咒法学者", e: "Conjuration Savant" }, { n: "小型咒法", e: "Minor Conjuration" } ] },
        { lv: 6, items: [ { n: "良性瞬移", e: "Benign Transposition" } ] },
        { lv: 10, items: [ { n: "专注咒法", e: "Focused Conjuration" } ] },
        { lv: 14, items: [ { n: "持久召唤", e: "Durable Summons" } ] }
      ]
    },
    {
      name: "预言学派", en: "School of Divination",
      features: [
        { lv: 2, items: [ { n: "预言学者", e: "Divination Savant" }, { n: "预兆", e: "Portent" } ] },
        { lv: 6, items: [ { n: "专家预言", e: "Expert Divination" } ] },
        { lv: 10, items: [ { n: "第三只眼", e: "The Third Eye" } ] },
        { lv: 14, items: [ { n: "高等预兆", e: "Greater Portent" } ] }
      ]
    },
    {
      name: "附魔学派", en: "School of Enchantment",
      features: [
        { lv: 2, items: [ { n: "附魔学者", e: "Enchantment Savant" }, { n: "催眠凝视", e: "Hypnotic Gaze" } ] },
        { lv: 6, items: [ { n: "本能魅惑", e: "Instinctive Charm" } ] },
        { lv: 10, items: [ { n: "分裂附魔", e: "Split Enchantment" } ] },
        { lv: 14, items: [ { n: "篡改记忆", e: "Alter Memories" } ] }
      ]
    },
    {
      name: "塑能学派", en: "School of Evocation",
      features: [
        { lv: 2, items: [ { n: "塑能学者", e: "Evocation Savant" }, { n: "塑形法术", e: "Sculpt Spells" } ] },
        { lv: 6, items: [ { n: "强力戏法", e: "Potent Cantrip" } ] },
        { lv: 10, items: [ { n: "强化塑能", e: "Empowered Evocation" } ] },
        { lv: 14, items: [ { n: "超载", e: "Overchannel" } ] }
      ]
    },
    {
      name: "幻术学派", en: "School of Illusion",
      features: [
        { lv: 2, items: [ { n: "幻术学者", e: "Illusion Savant" }, { n: "强化次级幻影", e: "Improved Minor Illusion" } ] },
        { lv: 6, items: [ { n: "可塑幻术", e: "Malleable Illusions" } ] },
        { lv: 10, items: [ { n: "幻影自我", e: "Illusory Self" } ] },
        { lv: 14, items: [ { n: "虚幻真实", e: "Illusory Reality" } ] }
      ]
    },
    {
      name: "死灵学派", en: "School of Necromancy",
      features: [
        { lv: 2, items: [ { n: "死灵学者", e: "Necromancy Savant" }, { n: "恐怖收割", e: "Grim Harvest" } ] },
        { lv: 6, items: [ { n: "亡灵仆从", e: "Undead Thralls" } ] },
        { lv: 10, items: [ { n: "不死适应", e: "Inured to Undeath" } ] },
        { lv: 14, items: [ { n: "命令亡灵", e: "Command Undead" } ] }
      ]
    },
    {
      name: "变化学派", en: "School of Transmutation",
      features: [
        { lv: 2, items: [ { n: "变化学者", e: "Transmutation Savant" }, { n: "小型炼金", e: "Minor Alchemy" } ] },
        { lv: 6, items: [ { n: "变化师之石", e: "Transmuter's Stone" } ] },
        { lv: 10, items: [ { n: "变形者", e: "Shapechanger" } ] },
        { lv: 14, items: [ { n: "变化大师", e: "Master Transmuter" } ] }
      ]
    }
  ]
};

/* ------------------------------------------------------------------ page logic */

var BUDGET = 27;
var MIN_SCORE = 8;
var MAX_SCORE = 15;
var POINT_COST = { 8: 0, 9: 1, 10: 2, 11: 3, 12: 4, 13: 5, 14: 7, 15: 9 };

var ITEM_NAMES = {
  "minecraft:iron_axe": "战斧",
  "minecraft:iron_sword": "长剑",
  "minecraft:wooden_axe": "硬头锤",
  "minecraft:shield": "盾牌",
  "minecraft:stick": "长棍",
  "minecraft:chainmail_chestplate": "锁子甲",
  "minecraft:leather_chestplate": "皮甲",
  "minecraft:diamond_chestplate": "板甲",
  "minecraft:bow": "长弓",
  "minecraft:crossbow": "轻弩"
};

var ABILITIES = [
  { key: "str", name: "力量" },
  { key: "dex", name: "敏捷" },
  { key: "con", name: "体质" },
  { key: "int", name: "智力" },
  { key: "wis", name: "感知" },
  { key: "cha", name: "魅力" }
];

var TABS = ["职业概览", "等级特性", "子职", "起始装备"];

/* 子职元数据：该职业子职选择等级 + 子职名 -> 全局 ID。
 * 只有选择等级 <= 1 的职业可在创建界面选择，其余仅作预览。 */
var SUBCLASS_META = {
  barbarian: { level: 3, ids: { "狂战士道途": "dndclasses:berserker", "图腾战士道途": "dndclasses:totem_warrior" } },
  bard: { level: 3, ids: { "博识学院": "dndclasses:lore", "勇气学院": "dndclasses:valor" } },
  cleric: { level: 1, ids: {
    "知识领域": "dndclasses:knowledge", "生命领域": "dndclasses:life", "光明领域": "dndclasses:light",
    "自然领域": "dndclasses:nature", "风暴领域": "dndclasses:tempest", "诡计领域": "dndclasses:trickery",
    "战争领域": "dndclasses:war" } },
  druid: { level: 2, ids: { "大地结社": "dndclasses:land", "月亮结社": "dndclasses:moon" } },
  fighter: { level: 3, ids: {
    "勇士": "dndclasses:champion", "战斗大师": "dndclasses:battle_master", "奥法骑士": "dndclasses:eldritch_knight" } },
  monk: { level: 3, ids: {
    "开手之道": "dndclasses:open_hand", "暗影之道": "dndclasses:shadow", "四象之道": "dndclasses:four_elements" } },
  paladin: { level: 3, ids: {
    "奉献之誓": "dndclasses:devotion", "上古之誓": "dndclasses:ancients", "复仇之誓": "dndclasses:vengeance" } },
  ranger: { level: 3, ids: { "猎人": "dndclasses:hunter", "驯兽师": "dndclasses:beast_master" } },
  rogue: { level: 3, ids: {
    "盗贼": "dndclasses:thief", "刺客": "dndclasses:assassin", "奥法诡术师": "dndclasses:arcane_trickster" } },
  sorcerer: { level: 1, ids: { "龙族血脉": "dndclasses:draconic_bloodline", "狂野魔法": "dndclasses:wild_magic" } },
  warlock: { level: 1, ids: {
    "妖精宗主": "dndclasses:archfey", "邪魔宗主": "dndclasses:fiend", "旧日支配者": "dndclasses:great_old_one" } },
  wizard: { level: 2, ids: {
    "防护学派": "dndclasses:abjuration", "咒法学派": "dndclasses:conjuration", "预言学派": "dndclasses:divination",
    "附魔学派": "dndclasses:enchantment", "塑能学派": "dndclasses:evocation", "幻术学派": "dndclasses:illusion",
    "死灵学派": "dndclasses:necromancy", "变化学派": "dndclasses:transmutation" } }
};

function subclassMeta(c) {
  return SUBCLASS_META[c.key] || { level: 3, ids: {} };
}

function eq(label, items) { return { label: label, items: items }; }

var CLASSES = [
  {
    key: "barbarian", name: "野蛮人", en: "Barbarian", icon: "蛮",
    hitDie: "D12", primary: "力量", saves: ["力量", "体质"],
    armor: ["轻甲", "中甲", "盾牌"], weapons: ["军用武器", "简易武器"],
    desc: "狂暴的战士，用愤怒摧毁敌人。来自荒野的怒吼，在鲜血与狂怒中撕裂一切阻碍。",
    equipment: [
      eq("巨斧 + 手斧 ×2", ["minecraft:iron_axe", "minecraft:iron_axe", "minecraft:iron_axe"]),
      eq("任意军用近战武器", ["minecraft:iron_sword"])
    ]
  },
  {
    key: "bard", name: "吟游诗人", en: "Bard", icon: "吟",
    hitDie: "D8", primary: "魅力", saves: ["敏捷", "魅力"],
    armor: ["轻甲"], weapons: ["手弩", "长剑", "细剑", "短剑", "简易武器"],
    desc: "用音乐和魔法激励同伴的艺术家，言词与旋律皆能化作力量。",
    equipment: [
      eq("细剑 + 匕首", ["minecraft:iron_sword", "minecraft:iron_sword"]),
      eq("长剑 + 匕首", ["minecraft:iron_sword", "minecraft:iron_sword"])
    ]
  },
  {
    key: "cleric", name: "牧师", en: "Cleric", icon: "牧",
    hitDie: "D8", primary: "感知", saves: ["感知", "魅力"],
    armor: ["轻甲", "中甲", "盾牌"], weapons: ["简易武器"],
    desc: "侍奉神祇的施法者，以信仰引导神术，治愈同伴或降下裁决。",
    equipment: [
      eq("钉头锤 + 盾牌", ["minecraft:iron_axe", "minecraft:shield"]),
      eq("硬头锤 + 盾牌", ["minecraft:wooden_axe", "minecraft:shield"])
    ]
  },
  {
    key: "druid", name: "德鲁伊", en: "Druid", icon: "德",
    hitDie: "D8", primary: "感知", saves: ["智力", "感知"],
    armor: ["轻甲", "中甲", "盾牌—非金属"], weapons: ["简易武器", "弯刀", "长棍", "投石索"],
    desc: "守护自然的古老施法者，能化身野兽，呼唤风雷与藤蔓。",
    equipment: [
      eq("橡木法杖", ["minecraft:stick"]),
      eq("弯刀 + 木盾", ["minecraft:iron_sword", "minecraft:shield"])
    ]
  },
  {
    key: "fighter", name: "战士", en: "Fighter", icon: "战",
    hitDie: "D10", primary: "力量或敏捷", saves: ["力量", "体质"],
    armor: ["所有护甲", "盾牌"], weapons: ["军用武器", "简易武器"],
    desc: "武器大师，战场上的勇士，以钢铁与纪律赢得每一场交锋。",
    equipment: [
      eq("链甲 + 盾牌 + 长剑", ["minecraft:chainmail_chestplate", "minecraft:shield", "minecraft:iron_sword"]),
      eq("链甲 + 巨剑", ["minecraft:chainmail_chestplate", "minecraft:iron_sword"])
    ]
  },
  {
    key: "monk", name: "武僧", en: "Monk", icon: "武",
    hitDie: "D8", primary: "敏捷与感知", saves: ["力量", "敏捷"],
    armor: ["无"], weapons: ["简易武器", "短剑"],
    desc: "以气为力量的武术大师，拳脚之间流转着超越肉身的力量。",
    equipment: [
      eq("短剑 + 匕首", ["minecraft:iron_sword", "minecraft:iron_sword"]),
      eq("长棍", ["minecraft:stick"])
    ]
  },
  {
    key: "paladin", name: "圣骑士", en: "Paladin", icon: "圣",
    hitDie: "D10", primary: "力量与魅力", saves: ["感知", "魅力"],
    armor: ["所有护甲", "盾牌"], weapons: ["军用武器", "简易武器"],
    desc: "誓约守护者，圣光的使者，以信念为盾、以誓言为剑。",
    equipment: [
      eq("链甲 + 盾牌 + 长剑", ["minecraft:chainmail_chestplate", "minecraft:shield", "minecraft:iron_sword"]),
      eq("板甲 + 巨剑", ["minecraft:diamond_chestplate", "minecraft:iron_sword"])
    ]
  },
  {
    key: "ranger", name: "游侠", en: "Ranger", icon: "游",
    hitDie: "D10", primary: "敏捷与感知", saves: ["力量", "敏捷"],
    armor: ["轻甲", "中甲", "盾牌"], weapons: ["军用武器", "简易武器"],
    desc: "荒野中的追踪者和猎手，熟悉每一条兽径与每一处险境。",
    equipment: [
      eq("皮甲 + 短剑 + 长弓", ["minecraft:leather_chestplate", "minecraft:iron_sword", "minecraft:bow"]),
      eq("皮甲 + 双短剑", ["minecraft:leather_chestplate", "minecraft:iron_sword", "minecraft:iron_sword"])
    ]
  },
  {
    key: "rogue", name: "游荡者", en: "Rogue", icon: "盗",
    hitDie: "D8", primary: "敏捷", saves: ["敏捷", "智力"],
    armor: ["轻甲"], weapons: ["手弩", "长剑", "细剑", "短剑", "简易武器"],
    desc: "阴影中的潜行者和偷袭专家，每一击都精准地落在要害。",
    equipment: [
      eq("细剑 + 短弓", ["minecraft:iron_sword", "minecraft:bow"]),
      eq("双短剑", ["minecraft:iron_sword", "minecraft:iron_sword"])
    ]
  },
  {
    key: "sorcerer", name: "术士", en: "Sorcerer", icon: "术",
    hitDie: "D6", primary: "魅力", saves: ["体质", "魅力"],
    armor: ["无"], weapons: ["匕首", "投石索", "长棍", "轻弩"],
    desc: "体内流淌着魔法之血的施法者，无需研习便可驱使奥术。",
    equipment: [
      eq("轻弩 + 匕首", ["minecraft:crossbow", "minecraft:iron_sword"]),
      eq("长棍 + 匕首", ["minecraft:stick", "minecraft:iron_sword"])
    ]
  },
  {
    key: "warlock", name: "邪术师", en: "Warlock", icon: "邪",
    hitDie: "D8", primary: "魅力", saves: ["感知", "魅力"],
    armor: ["轻甲"], weapons: ["简易武器"],
    desc: "与强大存在缔结契约的施法者，以禁忌的知识换取力量。",
    equipment: [
      eq("轻弩 + 匕首", ["minecraft:crossbow", "minecraft:iron_sword"]),
      eq("长棍 + 匕首", ["minecraft:stick", "minecraft:iron_sword"])
    ]
  },
  {
    key: "wizard", name: "法师", en: "Wizard", icon: "法",
    hitDie: "D6", primary: "智力", saves: ["智力", "感知"],
    armor: ["无"], weapons: ["匕首", "投石索", "长棍", "轻弩"],
    desc: "通过研习掌握奥术的学者，法术书是他们最锋利的武器。",
    equipment: [
      eq("长棍 + 匕首", ["minecraft:stick", "minecraft:iron_sword"]),
      eq("轻弩 + 匕首", ["minecraft:crossbow", "minecraft:iron_sword"])
    ]
  }
];

var state = {
  selected: null,
  equipment: 0,
  tab: 0,
  subclass: 0,
  subclassChosen: null,
  abilities: { str: 8, dex: 8, con: 8, int: 8, wis: 8, cha: 8 }
};

function byId(id) { return document.getElementById(id); }

function make(tag, cls, text) {
  var e = document.createElement(tag);
  if (cls) e.setAttribute("class", cls);
  if (text !== undefined && text !== null) e.textContent = String(text);
  return e;
}

function clearNode(node) {
  var kids = node.getChildren();
  for (var i = kids.size() - 1; i >= 0; i--) {
    node.removeChild(kids.get(i));
  }
}

function findClass(key) {
  for (var i = 0; i < CLASSES.length; i++) {
    if (CLASSES[i].key === key) return CLASSES[i];
  }
  return null;
}

function itemTexture(id) {
  var parts = String(id).split(":");
  var ns = parts.length > 1 ? parts[0] : "minecraft";
  var path = parts.length > 1 ? parts[1] : parts[0];
  return ns + ":textures/item/" + path + ".png";
}

function profBonus(level) {
  return 2 + Math.floor((level - 1) / 4);
}

function scoreCost(score) {
  return POINT_COST[score] === undefined ? -1 : POINT_COST[score];
}

function totalCost() {
  var sum = 0;
  for (var i = 0; i < ABILITIES.length; i++) {
    sum += scoreCost(state.abilities[ABILITIES[i].key]);
  }
  return sum;
}

function describeAbilities() {
  var parts = [];
  for (var i = 0; i < ABILITIES.length; i++) {
    var a = ABILITIES[i];
    var sc = state.abilities[a.key];
    var mod = Math.floor((sc - 10) / 2);
    parts.push(a.name + " " + sc + "(" + (mod >= 0 ? "+" : "") + mod + ")");
  }
  return parts.join("  ");
}

/* ------------------------------------------------------------- rendering */

function renderClassList() {
  var list = byId("class-list");
  if (!list) return;
  clearNode(list);
  for (var i = 0; i < CLASSES.length; i++) {
    var c = CLASSES[i];
    var active = state.selected === c.key ? " active" : "";
    var item = make("div", "class-item" + active);
    var seal = make("span", "seal", c.icon);
    var info = make("span", "class-info");
    info.appendChild(make("span", "class-name", c.name));
    info.appendChild(make("span", "class-en", c.en));
    var die = make("span", "class-die", c.hitDie);
    item.appendChild(seal);
    item.appendChild(info);
    item.appendChild(die);
    bindClassItem(item, c.key);
    list.appendChild(item);
  }
}

function bindClassItem(item, key) {
  item.addEventListener("click", function () { selectClass(key); });
}

function selectClass(key) {
  state.selected = key;
  state.equipment = 0;
  state.tab = 0;
  state.subclass = 0;
  state.subclassChosen = null;
  renderClassList();
  renderDetail();
  renderAbility();
  updateFooterSummary();
  refreshConfirmState();
  setMsg("", false);
}

function sectionLabel(text) {
  return make("div", "section-label", text);
}

function runeRow(items) {
  var row = make("div", "rune-row");
  for (var i = 0; i < items.length; i++) {
    row.appendChild(make("span", "rune", items[i]));
  }
  return row;
}

function statBox(label, value) {
  var box = make("div", "stat");
  box.appendChild(make("div", "stat-label", label));
  box.appendChild(make("div", "stat-value", value));
  return box;
}

function renderDetail() {
  var header = byId("detail-header");
  var body = byId("detail-body");
  if (!header || !body) return;
  clearNode(body);
  if (!state.selected) {
    header.textContent = "职业详情";
    var empty = make("div", "empty-state");
    empty.innerHTML = "从左侧名册中挑选一个职业，<br>翻开属于你的命运之页。";
    body.appendChild(empty);
    return;
  }

  var c = findClass(state.selected);
  header.textContent = c.name + " · " + c.en;

  var hero = make("div", "detail-hero");
  hero.appendChild(make("span", "seal seal-lg", c.icon));
  var heroText = make("div", null, null);
  heroText.appendChild(make("div", "detail-name", c.name));
  heroText.appendChild(make("div", "detail-en", c.en + " · " + c.hitDie));
  hero.appendChild(heroText);
  body.appendChild(hero);

  body.appendChild(make("p", "detail-desc", c.desc));

  var stats = make("div", "stat-grid");
  stats.appendChild(statBox("生命骰", c.hitDie));
  stats.appendChild(statBox("主属性", c.primary));
  stats.appendChild(statBox("豁免熟练", c.saves.join(" / ")));
  body.appendChild(stats);

  var tabsBar = make("div", "tabs detail-tabs");
  for (var t = 0; t < TABS.length; t++) {
    bindTabButton(tabsBar, t, TABS[t]);
  }
  body.appendChild(tabsBar);

  body.appendChild(renderActivePane(c));
}

function bindTabButton(bar, index, label) {
  var active = state.tab === index ? " active" : "";
  var btn = make("button", "tab" + active, label);
  btn.setAttribute("type", "button");
  btn.addEventListener("click", function () { state.tab = index; renderDetail(); });
  bar.appendChild(btn);
}

function renderActivePane(c) {
  if (state.tab === 0) return paneOverview(c);
  if (state.tab === 1) return paneLevels(c);
  if (state.tab === 2) return paneSubclasses(c);
  return paneEquipment(c);
}

function paneOverview(c) {
  var pane = make("div", "tab-pane");
  pane.appendChild(sectionLabel("护甲熟练"));
  pane.appendChild(runeRow(c.armor));
  pane.appendChild(sectionLabel("武器熟练"));
  pane.appendChild(runeRow(c.weapons));
  pane.appendChild(sectionLabel("豁免熟练"));
  pane.appendChild(runeRow(c.saves));
  return pane;
}

function featureBlock(item) {
  var box = make("div", "feature");
  var title = make("div", "feature-title");
  title.appendChild(make("span", "feature-name", item.e ? (item.n + " " + item.e) : item.n));
  box.appendChild(title);
  var text = item.t ? item.t : (item.n + "特性描述");
  box.appendChild(make("div", "feature-text", text));
  return box;
}

function levelRow(level, items) {
  var row = make("div", "level-row" + (items.length ? "" : " empty"));
  var head = make("div", "level-head");
  head.appendChild(make("span", "level-badge", "Lv " + level));
  head.appendChild(make("span", "level-prof", "熟练 +" + profBonus(level)));
  row.appendChild(head);
  var list = make("div", "level-items");
  if (!items.length) {
    list.appendChild(make("div", "level-none", "—"));
  } else {
    for (var i = 0; i < items.length; i++) {
      list.appendChild(featureBlock(items[i]));
    }
  }
  row.appendChild(list);
  return row;
}

function itemsAtLevel(levels, level) {
  if (!levels) return [];
  for (var i = 0; i < levels.length; i++) {
    if (levels[i].lv === level) return levels[i].items;
  }
  return [];
}

function paneLevels(c) {
  var pane = make("div", "tab-pane");
  var data = CLASS_FEATURES[c.key];
  if (!data) {
    pane.appendChild(make("div", "empty-state", "暂无等级特性数据。"));
    return pane;
  }
  if (data.intro) {
    pane.appendChild(sectionLabel("职业总述"));
    pane.appendChild(make("div", "level-intro", data.intro));
  }
  pane.appendChild(sectionLabel("等级特性"));
  for (var lv = 1; lv <= 20; lv++) {
    pane.appendChild(levelRow(lv, itemsAtLevel(data.levels, lv)));
  }
  return pane;
}

function paneSubclasses(c) {
  var pane = make("div", "tab-pane");
  var data = CLASS_FEATURES[c.key];
  if (!data || !data.subclasses || data.subclasses.length === 0) {
    pane.appendChild(make("div", "empty-state", "暂无子职数据。"));
    return pane;
  }
  var meta = subclassMeta(c);
  var selectable = meta.level <= 1;

  pane.appendChild(make("div", "subclass-hint",
      selectable ? "选择你的子职（点击下方选项）" : ("该职业将于 " + meta.level + " 级选择子职，以下仅供预览")));

  var bar = make("div", "subclass-list");
  for (var i = 0; i < data.subclasses.length; i++) {
    bindSubclassButton(bar, i, data.subclasses[i].name, selectable);
  }
  pane.appendChild(bar);

  var idx = state.subclass < data.subclasses.length ? state.subclass : 0;
  var sc = data.subclasses[idx];
  pane.appendChild(make("div", "subclass-intro", sc.en ? (sc.name + " · " + sc.en) : sc.name));
  if (sc.intro) {
    pane.appendChild(make("div", "level-intro", sc.intro));
  }
  for (var lv = 1; lv <= 20; lv++) {
    var items = itemsAtLevel(sc.features, lv);
    if (!items.length) continue;
    pane.appendChild(levelRow(lv, items));
  }
  return pane;
}

function bindSubclassButton(bar, index, label, selectable) {
  var active = state.subclass === index ? " active" : "";
  var chosen = selectable && state.subclassChosen === index ? " chosen" : "";
  var btn = make("button", "subclass-tab" + active + chosen, label);
  btn.setAttribute("type", "button");
  btn.addEventListener("click", function () {
    state.subclass = index;
    if (selectable) state.subclassChosen = index;
    renderDetail();
  });
  bar.appendChild(btn);
}

function chosenSubclassId(c) {
  var meta = subclassMeta(c);
  if (meta.level > 1 || state.subclassChosen === null) return "";
  var data = CLASS_FEATURES[c.key];
  if (!data || !data.subclasses || state.subclassChosen >= data.subclasses.length) return "";
  var name = data.subclasses[state.subclassChosen].name;
  return meta.ids[name] || "";
}

function paneEquipment(c) {
  var pane = make("div", "tab-pane");
  pane.appendChild(sectionLabel("选择一套起始装备"));
  for (var i = 0; i < c.equipment.length; i++) {
    bindEquipChoice(pane, c, i);
  }
  return pane;
}

function bindEquipChoice(pane, c, idx) {
  var choice = c.equipment[idx];
  var active = state.equipment === idx ? " active" : "";
  var row = make("div", "equip-choice" + active);
  row.appendChild(make("span", "equip-radio"));
  var body = make("div", "equip-body");
  body.appendChild(make("div", "equip-label", choice.label));
  var items = make("div", "item-row");
  for (var j = 0; j < choice.items.length; j++) {
    var id = choice.items[j];
    var chip = make("div", "item-chip");
    chip.appendChild(make("div", "item-placeholder"));
    chip.appendChild(make("span", "item-name", ITEM_NAMES[id] ? ITEM_NAMES[id] : String(id).split(":")[1]));
    items.appendChild(chip);
  }
  body.appendChild(items);
  row.appendChild(body);
  row.addEventListener("click", function () { state.equipment = idx; renderDetail(); });
  pane.appendChild(row);
}

function renderAbility() {
  var body = byId("ability-body");
  if (!body) return;
  clearNode(body);
  var spent = totalCost();
  var remaining = BUDGET - spent;
  var c = state.selected ? findClass(state.selected) : null;
  var primary = c ? c.primary : "";

  var line = make("div", "points-line");
  line.appendChild(make("span", "points-label", "已花费 / 总预算"));
  line.appendChild(make("span", "points-value", spent + " / " + BUDGET));
  body.appendChild(line);

  var progress = make("div", "progress");
  var bar = make("div", "progress-bar");
  bar.setAttribute("style", "width:" + Math.round(spent * 100 / BUDGET) + "%");
  progress.appendChild(bar);
  body.appendChild(progress);

  var costRow = make("div", "cost-row rune-row");
  costRow.appendChild(make("span", "rune", "剩余 " + remaining + " 点"));
  costRow.appendChild(make("span", "rune", "范围 8 - 15"));
  body.appendChild(costRow);

  for (var i = 0; i < ABILITIES.length; i++) {
    bindAbilityRow(body, ABILITIES[i], remaining, primary);
  }

  body.appendChild(make("div", "ability-caption", "金色 = 本职业主属性，建议优先提升"));
}

function bindAbilityRow(body, a, remaining, primary) {
  var sc = state.abilities[a.key];
  var mod = Math.floor((sc - 10) / 2);
  var canPlus = sc < MAX_SCORE && remaining >= (scoreCost(sc + 1) - scoreCost(sc));
  var canMinus = sc > MIN_SCORE;
  var isPrimary = primary.indexOf(a.name) >= 0;

  var row = make("div", "ability-row" + (isPrimary ? " primary" : ""));
  var minus = make("button", "pip-btn", "-");
  minus.setAttribute("type", "button");
  if (!canMinus) minus.setAttribute("disabled", "disabled");
  var score = make("span", "ability-score", String(sc));
  var plus = make("button", "pip-btn", "+");
  plus.setAttribute("type", "button");
  if (!canPlus) plus.setAttribute("disabled", "disabled");
  row.appendChild(minus);
  row.appendChild(score);
  row.appendChild(plus);
  row.appendChild(make("span", "ability-name", a.name));
  row.appendChild(make("span", "ability-mod", (mod >= 0 ? "+" : "") + mod));

  minus.addEventListener("click", function () { changeAbility(a.key, -1); });
  plus.addEventListener("click", function () { changeAbility(a.key, 1); });
  body.appendChild(row);
}

function changeAbility(key, delta) {
  var sc = state.abilities[key];
  if (delta > 0) {
    if (sc >= MAX_SCORE) return;
    if (totalCost() + (scoreCost(sc + 1) - scoreCost(sc)) > BUDGET) return;
    state.abilities[key] = sc + 1;
  } else {
    if (sc <= MIN_SCORE) return;
    state.abilities[key] = sc - 1;
  }
  renderAbility();
  updateFooterSummary();
  refreshConfirmState();
  setMsg("", false);
}

/* --------------------------------------------------------------- helpers */

function updateFooterSummary() {
  var el = byId("footer-summary");
  if (!el) return;
  if (!state.selected) {
    el.textContent = "尚未选择职业。";
    return;
  }
  var c = findClass(state.selected);
  var remaining = BUDGET - totalCost();
  el.textContent = "已选职业：" + c.name + "（" + c.hitDie + "）　剩余属性点：" + remaining;
}

function refreshConfirmState() {
  var btn = byId("btn-confirm");
  if (!btn) return;
  var ok = state.selected !== null && totalCost() === BUDGET;
  if (ok) btn.removeAttribute("disabled");
  else btn.setAttribute("disabled", "disabled");
}

function setMsg(text, ok) {
  var el = byId("confirm-msg");
  if (!el) return;
  el.textContent = text ? text : "";
  if (ok) el.setAttribute("class", "confirm-msg ok");
  else el.setAttribute("class", "confirm-msg");
}

/* ---------------------------------------------------------------- modal */

function onConfirm() {
  if (!state.selected) { setMsg("请先选择一个职业。", false); return; }
  if (totalCost() !== BUDGET) { setMsg("必须恰好分配 " + BUDGET + " 点属性。", false); return; }
  setMsg("", false);
  openSummary();
}

function summaryLine(key, value) {
  var line = make("div", "summary-line");
  line.appendChild(make("span", "summary-key", key));
  line.appendChild(make("span", "summary-val", value));
  return line;
}

function findAbility(key) {
  for (var i = 0; i < ABILITIES.length; i++) {
    if (ABILITIES[i].key === key) return ABILITIES[i];
  }
  return null;
}

function summaryAbilities() {
  var order = [["str", "con", "dex"], ["wis", "int", "cha"]];
  var wrap = make("div", "summary-abilities");
  for (var r = 0; r < order.length; r++) {
    var row = make("div", "summary-ability-row");
    for (var i = 0; i < order[r].length; i++) {
      var ab = findAbility(order[r][i]);
      var sc = state.abilities[ab.key];
      var mod = Math.floor((sc - 10) / 2);
      var cell = make("div", "summary-ability");
      cell.appendChild(make("div", "summary-ability-name", ab.name));
      cell.appendChild(make("div", "summary-ability-score", sc + " (" + (mod >= 0 ? "+" : "") + mod + ")"));
      row.appendChild(cell);
    }
    wrap.appendChild(row);
  }
  return wrap;
}

function openSummary() {
  var c = findClass(state.selected);
  var choice = c.equipment[state.equipment];
  var body = byId("sheet-body");
  if (!body) return;
  clearNode(body);
  body.appendChild(summaryLine("职业", c.name + " · " + c.en));
  body.appendChild(summaryLine("生命骰", c.hitDie));
  body.appendChild(summaryLine("主属性", c.primary));
  body.appendChild(summaryLine("豁免熟练", c.saves.join("，")));
  var subId = chosenSubclassId(c);
  if (subId) {
    var subName = CLASS_FEATURES[c.key].subclasses[state.subclassChosen].name;
    body.appendChild(summaryLine("子职", subName));
  }
  body.appendChild(make("div", "summary-key summary-abilities-label", "属性值"));
  body.appendChild(summaryAbilities());
  body.appendChild(summaryLine("起始装备", choice.label));
  var overlay = byId("sheet-overlay");
  if (overlay) overlay.setAttribute("class", "sheet-overlay open");
}

function closeSummary() {
  var overlay = byId("sheet-overlay");
  if (overlay) overlay.setAttribute("class", "sheet-overlay");
}

function commitCharacter() {
  var c = findClass(state.selected);
  var choice = c.equipment[state.equipment];
  var payload = {
    class: state.selected,
    className: c.name,
    subclass: chosenSubclassId(c),
    abilities: state.abilities,
    pointsSpent: totalCost(),
    equipmentChoice: state.equipment,
    equipmentLabel: choice.label,
    equipment: choice.items
  };

  var payloadInput = byId("class-payload");
  if (payloadInput) payloadInput.value = JSON.stringify(payload);

  try {
    if (typeof window !== "undefined" && window && typeof window.auiClassChosen === "function") {
      window.auiClassChosen(payload);
    }
  } catch (err) {
  }

  console.log("[dndClasses] character payload " + JSON.stringify(payload));
  closeSummary();
  setMsg("角色已提交，愿命运眷顾你。", true);
}

/* ------------------------------------------------------------- sheet mode */

var pageMode = "";
var lastStateValue = "";

function setHidden(el, hidden) {
  if (!el) return;
  var cls = el.getAttribute("class") || "";
  if (hidden) {
    if (cls.indexOf("hidden") < 0) el.setAttribute("class", cls + " hidden");
  } else {
    var parts = cls.split(" ");
    var out = "";
    for (var i = 0; i < parts.length; i++) {
      if (parts[i] && parts[i] !== "hidden") out += (out ? " " : "") + parts[i];
    }
    el.setAttribute("class", out);
  }
}

function subclassNameByKey(c, key) {
  if (!key) return "";
  var data = CLASS_FEATURES[c.key];
  var meta = subclassMeta(c);
  if (!data || !data.subclasses) return "";
  for (var i = 0; i < data.subclasses.length; i++) {
    if (meta.ids[data.subclasses[i].name] === key) return data.subclasses[i].name;
  }
  return "";
}

function findSubclassByKey(c, key) {
  if (!key) return null;
  var data = CLASS_FEATURES[c.key];
  var meta = subclassMeta(c);
  if (!data || !data.subclasses) return null;
  for (var i = 0; i < data.subclasses.length; i++) {
    if (meta.ids[data.subclasses[i].name] === key) return data.subclasses[i];
  }
  return null;
}

function sheetAbilities(abilities) {
  var order = [["str", "con", "dex"], ["wis", "int", "cha"]];
  var wrap = make("div", "summary-abilities");
  for (var r = 0; r < order.length; r++) {
    var row = make("div", "summary-ability-row");
    for (var i = 0; i < order[r].length; i++) {
      var ab = findAbility(order[r][i]);
      var sc = abilities && abilities[ab.key] !== undefined ? abilities[ab.key] : 8;
      var mod = Math.floor((sc - 10) / 2);
      var cell = make("div", "summary-ability");
      cell.appendChild(make("div", "summary-ability-name", ab.name));
      cell.appendChild(make("div", "summary-ability-score", sc + " (" + (mod >= 0 ? "+" : "") + mod + ")"));
      row.appendChild(cell);
    }
    wrap.appendChild(row);
  }
  return wrap;
}

function expBlock(data) {
  var level = data.level ? data.level : 1;
  var exp = data.exp ? data.exp : 0;
  var start = data.expLevelStart ? data.expLevelStart : 0;
  var next = data.expToNext ? data.expToNext : 0;
  var wrap = make("div", "exp-block");

  var line = make("div", "exp-line");
  line.appendChild(make("span", "exp-label", "经验"));

  var bar = make("div", "progress-bar");
  if (level >= 20) {
    line.appendChild(make("span", "exp-value", exp + " / 已满级"));
    bar.setAttribute("style", "width:100%");
  } else {
    line.appendChild(make("span", "exp-value", exp + " / " + next));
    var span = next - start;
    var into = exp - start;
    var pct = span > 0 ? Math.round(into * 100 / span) : 0;
    if (pct < 0) pct = 0;
    if (pct > 100) pct = 100;
    bar.setAttribute("style", "width:" + pct + "%");
  }

  var progress = make("div", "progress");
  progress.appendChild(bar);
  wrap.appendChild(line);
  wrap.appendChild(progress);
  return wrap;
}

function collectFeatures(c, level, subclassKey) {
  var groups = [];
  var data = CLASS_FEATURES[c.key];
  if (!data) return groups;
  var sub = findSubclassByKey(c, subclassKey);
  for (var lv = 1; lv <= level; lv++) {
    var items = itemsAtLevel(data.levels, lv).slice();
    if (sub) {
      var subItems = itemsAtLevel(sub.features, lv);
      for (var i = 0; i < subItems.length; i++) items.push(subItems[i]);
    }
    if (items.length) groups.push({ lv: lv, items: items });
  }
  return groups;
}

function renderSheet(data) {
  var body = byId("character-sheet-body");
  var header = byId("sheet-header");
  if (!body) return;
  clearNode(body);
  var c = findClass(data["class"]);
  if (header) header.textContent = "角色信息";

  if (!c) {
    body.appendChild(make("div", "empty-state", "未知职业。"));
  } else {
    var hero = make("div", "detail-hero");
    hero.appendChild(make("span", "seal seal-lg", c.icon));
    var ht = make("div");
    ht.appendChild(make("div", "detail-name", c.name));
    var subName = subclassNameByKey(c, data.subclass);
    ht.appendChild(make("div", "detail-en", c.en + (subName ? (" · " + subName) : "")));
    hero.appendChild(ht);
    body.appendChild(hero);

    body.appendChild(make("p", "detail-desc", c.desc));

    var stats = make("div", "stat-grid");
    stats.appendChild(statBox("等级", "Lv " + (data.level ? data.level : 1)));
    stats.appendChild(statBox("熟练加值", "+" + (data.proficiency ? data.proficiency : 2)));
    stats.appendChild(statBox("生命骰", c.hitDie));
    body.appendChild(stats);

    body.appendChild(sectionLabel("经验"));
    body.appendChild(expBlock(data));

    body.appendChild(sectionLabel("属性值"));
    body.appendChild(sheetAbilities(data.abilities));

    body.appendChild(sectionLabel("已获得特性"));
    var groups = collectFeatures(c, data.level ? data.level : 1, data.subclass);
    if (!groups.length) {
      body.appendChild(make("div", "empty-state", "暂无特性。"));
    } else {
      for (var g = 0; g < groups.length; g++) {
        body.appendChild(levelRow(groups[g].lv, groups[g].items));
      }
    }
  }

  setHidden(byId("col-left"), true);
  setHidden(byId("col-mid"), true);
  setHidden(byId("col-right"), true);
  setHidden(byId("sheet-view"), false);
  setHidden(byId("grimoire-footer"), true);
  setHidden(byId("page-loading"), true);
}

function enterLoading() {
  setHidden(byId("col-left"), true);
  setHidden(byId("col-mid"), true);
  setHidden(byId("col-right"), true);
  setHidden(byId("sheet-view"), true);
  setHidden(byId("grimoire-footer"), true);
  setHidden(byId("page-loading"), false);
}

function showCreateView() {
  setHidden(byId("page-loading"), true);
  setHidden(byId("sheet-view"), true);
  setHidden(byId("col-left"), false);
  setHidden(byId("col-mid"), false);
  setHidden(byId("col-right"), false);
  setHidden(byId("grimoire-footer"), false);
}

function pollPageState() {
  var el = byId("page-state");
  if (!el) return;
  var value = el.value;
  if (!value || value === lastStateValue) return;
  var data;
  try {
    data = JSON.parse(value);
  } catch (err) {
    return;
  }
  if (!data || !data.mode) return;
  lastStateValue = value;
  if (data.mode === "sheet") {
    pageMode = "sheet";
    renderSheet(data);
  } else if (!pageMode) {
    pageMode = "create";
    showCreateView();
  }
}

/* ----------------------------------------------------------------- init */

function init() {
  var confirm = byId("btn-confirm");
  if (confirm) confirm.addEventListener("click", onConfirm);
  var cancel = byId("sheet-cancel");
  if (cancel) cancel.addEventListener("click", closeSummary);
  var commit = byId("sheet-commit");
  if (commit) commit.addEventListener("click", commitCharacter);

  renderClassList();
  renderDetail();
  renderAbility();
  updateFooterSummary();
  refreshConfirmState();

  enterLoading();
  pollPageState();
  if (typeof setInterval === "function") {
    setInterval(pollPageState, 150);
  }
  if (typeof setTimeout === "function") {
    setTimeout(function () {
      if (!pageMode) {
        pageMode = "create";
        showCreateView();
      }
    }, 1500);
  }
}

var pageStarted = false;

function boot() {
  if (pageStarted) return;
  pageStarted = true;
  init();
}

document.addEventListener("DOMContentLoaded", boot);
if (document.readyState === "complete" || document.readyState === "interactive") {
  boot();
}
