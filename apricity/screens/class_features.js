var CLASS_FEATURES = {};

/* dndClasses class progression data.
 * Schema: CLASS_FEATURES["key"] = {
 *   levels:     [ { lv, items:[ { n:"涓枃鍚?, e:"English", t:"鍙€夋弿杩? } ] } ],
 *   subclasses: [ { name, en, features:[ { lv, items:[...] } ] } ]
 * }
 * If "t" is omitted the page shows "<涓枃鍚?鐗规€ф弿杩? as a placeholder.
 */
CLASS_FEATURES["barbarian"] = {
  levels: [
    { lv: 1, items: [ { n: "鐙傛毚", e: "Rage" }, { n: "鏃犵敳闃插尽", e: "Unarmored Defense" } ] },
    { lv: 2, items: [ { n: "椴佽幗鏀诲嚮", e: "Reckless Attack" }, { n: "鍗遍櫓鎰熺煡", e: "Danger Sense" } ] },
    { lv: 3, items: [ { n: "鍘熷閬撻€?, e: "Primal Path" } ] },
    { lv: 4, items: [ { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" } ] },
    { lv: 5, items: [ { n: "棰濆鏀诲嚮", e: "Extra Attack" }, { n: "蹇€熺Щ鍔?, e: "Fast Movement" } ] },
    { lv: 6, items: [ { n: "鍘熷閬撻€旂壒鎬?, e: "Primal Path feature" } ] },
    { lv: 7, items: [ { n: "閲庢€х洿瑙?, e: "Feral Instinct" } ] },
    { lv: 8, items: [ { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" } ] },
    { lv: 9, items: [ { n: "娈嬫毚閲嶅嚮锛?楠帮級", e: "Brutal Critical (1 die)" } ] },
    { lv: 10, items: [ { n: "鍘熷閬撻€旂壒鎬?, e: "Primal Path feature" } ] },
    { lv: 11, items: [ { n: "鏃犲敖鐙傛毚", e: "Relentless Rage" } ] },
    { lv: 12, items: [ { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" } ] },
    { lv: 13, items: [ { n: "娈嬫毚閲嶅嚮锛?楠帮級", e: "Brutal Critical (2 dice)" } ] },
    { lv: 14, items: [ { n: "鍘熷閬撻€旂壒鎬?, e: "Primal Path feature" } ] },
    { lv: 15, items: [ { n: "鎸佷箙鐙傛毚", e: "Persistent Rage" } ] },
    { lv: 16, items: [ { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" } ] },
    { lv: 17, items: [ { n: "娈嬫毚閲嶅嚮锛?楠帮級", e: "Brutal Critical (3 dice)" } ] },
    { lv: 18, items: [ { n: "涓嶅眻濞佽兘", e: "Indomitable Might" } ] },
    { lv: 19, items: [ { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" } ] },
    { lv: 20, items: [ { n: "鍘熷鍕囧＋", e: "Primal Champion" } ] }
  ],
  subclasses: [
    {
      name: "鐙傛垬澹亾閫?, en: "Path of the Berserker",
      features: [
        { lv: 3, items: [ { n: "鐙備贡", e: "Frenzy" } ] },
        { lv: 6, items: [ { n: "鏃犲績鐙傛毚", e: "Mindless Rage" } ] },
        { lv: 10, items: [ { n: "濞佸悡涔嬪Э", e: "Intimidating Presence" } ] },
        { lv: 14, items: [ { n: "鎶ュ", e: "Retaliation" } ] }
      ]
    },
    {
      name: "鍥捐吘鎴樺＋閬撻€?, en: "Path of the Totem Warrior",
      features: [
        { lv: 3, items: [ { n: "鐏甸瓊杩藉", e: "Spirit Seeker" }, { n: "鍥捐吘涔嬮瓊", e: "Totem Spirit" } ] },
        { lv: 6, items: [ { n: "閲庡吔涔嬬浉", e: "Aspect of the Beast" } ] },
        { lv: 14, items: [ { n: "鍥捐吘璋冭皭", e: "Totemic Attunement" } ] }
      ]
    }
  ]
};

CLASS_FEATURES["bard"] = {
  levels: [
    { lv: 1, items: [ { n: "鏂芥硶", e: "Spellcasting" }, { n: "鍚熸父婵€鍔憋紙d6锛?, e: "Bardic Inspiration (d6)" } ] },
    { lv: 2, items: [ { n: "涓囦簨閫?, e: "Jack of All Trades" }, { n: "浼戞啯涔嬫瓕锛坉6锛?, e: "Song of Rest (d6)" } ] },
    { lv: 3, items: [ { n: "鍚熸父璇椾汉瀛﹂櫌", e: "Bard College" }, { n: "涓撶簿", e: "Expertise" } ] },
    { lv: 4, items: [ { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" } ] },
    { lv: 5, items: [ { n: "鍚熸父婵€鍔憋紙d8锛?, e: "Bardic Inspiration (d8)" }, { n: "鐏垫劅婧愭硥", e: "Font of Inspiration" } ] },
    { lv: 6, items: [ { n: "鍙嶉瓍鎯?, e: "Countercharm" }, { n: "鍚熸父璇椾汉瀛﹂櫌鐗规€?, e: "Bard College feature" } ] },
    { lv: 7, items: [] },
    { lv: 8, items: [ { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" } ] },
    { lv: 9, items: [ { n: "浼戞啯涔嬫瓕锛坉8锛?, e: "Song of Rest (d8)" } ] },
    { lv: 10, items: [ { n: "鍚熸父婵€鍔憋紙d10锛?, e: "Bardic Inspiration (d10)" }, { n: "涓撶簿", e: "Expertise" } ] },
    { lv: 11, items: [] },
    { lv: 12, items: [ { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" } ] },
    { lv: 13, items: [ { n: "浼戞啯涔嬫瓕锛坉10锛?, e: "Song of Rest (d10)" } ] },
    { lv: 14, items: [ { n: "榄旀硶绉樿緵", e: "Magical Secrets" }, { n: "鍚熸父璇椾汉瀛﹂櫌鐗规€?, e: "Bard College feature" } ] },
    { lv: 15, items: [ { n: "鍚熸父婵€鍔憋紙d12锛?, e: "Bardic Inspiration (d12)" } ] },
    { lv: 16, items: [ { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" } ] },
    { lv: 17, items: [ { n: "浼戞啯涔嬫瓕锛坉12锛?, e: "Song of Rest (d12)" } ] },
    { lv: 18, items: [ { n: "榄旀硶绉樿緵", e: "Magical Secrets" } ] },
    { lv: 19, items: [ { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" } ] },
    { lv: 20, items: [ { n: "瓒呭嚒鐏垫劅", e: "Superior Inspiration" } ] }
  ],
  subclasses: [
    {
      name: "鍗氳瘑瀛﹂櫌", en: "College of Lore",
      features: [
        { lv: 3, items: [ { n: "棰濆鐔熺粌", e: "Bonus Proficiencies" }, { n: "灏栧埢瑷€璇?, e: "Cutting Words" } ] },
        { lv: 6, items: [ { n: "棰濆榄旀硶绉樿緵", e: "Additional Magical Secrets" } ] },
        { lv: 14, items: [ { n: "鏃犲尮鎶€鑹?, e: "Peerless Skill" } ] }
      ]
    },
    {
      name: "鍕囨皵瀛﹂櫌", en: "College of Valor",
      features: [
        { lv: 3, items: [ { n: "棰濆鐔熺粌", e: "Bonus Proficiencies" }, { n: "鎴樻枟婵€鍔?, e: "Combat Inspiration" } ] },
        { lv: 6, items: [ { n: "棰濆鏀诲嚮", e: "Extra Attack" } ] },
        { lv: 14, items: [ { n: "鎴樻枟榄旀硶", e: "Battle Magic" } ] }
      ]
    }
  ]
};

CLASS_FEATURES["cleric"] = {
  levels: [
    { lv: 1, items: [ { n: "鏂芥硶", e: "Spellcasting" }, { n: "绁炲湥棰嗗煙", e: "Divine Domain" } ] },
    { lv: 2, items: [ { n: "寮曞绁炲姏锛堥┍鏁ｄ笉姝伙級", e: "Channel Divinity (Turn Undead)" }, { n: "绁炲湥棰嗗煙鐗规€?, e: "Divine Domain feature" } ] },
    { lv: 3, items: [] },
    { lv: 4, items: [ { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" } ] },
    { lv: 5, items: [ { n: "鎽ф瘉涓嶆锛圕R 1/2锛?, e: "Destroy Undead (CR 1/2)" } ] },
    { lv: 6, items: [ { n: "寮曞绁炲姏锛?娆?浼戞伅锛?, e: "Channel Divinity (2/rest)" }, { n: "绁炲湥棰嗗煙鐗规€?, e: "Divine Domain feature" } ] },
    { lv: 7, items: [] },
    { lv: 8, items: [ { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" }, { n: "鎽ф瘉涓嶆锛圕R 1锛?, e: "Destroy Undead (CR 1)" }, { n: "绁炲湥棰嗗煙鐗规€?, e: "Divine Domain feature" } ] },
    { lv: 9, items: [] },
    { lv: 10, items: [ { n: "绁炲湥骞查", e: "Divine Intervention" } ] },
    { lv: 11, items: [ { n: "鎽ф瘉涓嶆锛圕R 2锛?, e: "Destroy Undead (CR 2)" } ] },
    { lv: 12, items: [ { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" } ] },
    { lv: 13, items: [] },
    { lv: 14, items: [ { n: "鎽ф瘉涓嶆锛圕R 3锛?, e: "Destroy Undead (CR 3)" } ] },
    { lv: 15, items: [] },
    { lv: 16, items: [ { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" } ] },
    { lv: 17, items: [ { n: "鎽ф瘉涓嶆锛圕R 4锛?, e: "Destroy Undead (CR 4)" }, { n: "绁炲湥棰嗗煙鐗规€?, e: "Divine Domain feature" } ] },
    { lv: 18, items: [ { n: "寮曞绁炲姏锛?娆?浼戞伅锛?, e: "Channel Divinity (3/rest)" } ] },
    { lv: 19, items: [ { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" } ] },
    { lv: 20, items: [ { n: "绁炲湥骞查寮哄寲", e: "Divine Intervention improvement" } ] }
  ],
  subclasses: [
    {
      name: "鐭ヨ瘑棰嗗煙", en: "Knowledge Domain",
      features: [
        { lv: 1, items: [ { n: "鐭ヨ瘑绁濈", e: "Blessings of Knowledge" } ] },
        { lv: 2, items: [ { n: "宀佹湀鐭ヨ瘑", e: "Knowledge of the Ages" } ] },
        { lv: 6, items: [ { n: "璇诲績", e: "Read Thoughts" } ] },
        { lv: 8, items: [ { n: "寮哄姏鏂芥硶", e: "Potent Spellcasting" } ] },
        { lv: 17, items: [ { n: "杩囧幓骞昏薄", e: "Visions of the Past" } ] }
      ]
    },
    {
      name: "鐢熷懡棰嗗煙", en: "Life Domain",
      features: [
        { lv: 1, items: [ { n: "棰濆鐔熺粌", e: "Bonus Proficiency" }, { n: "鐢熷懡闂ㄥ緬", e: "Disciple of Life" } ] },
        { lv: 2, items: [ { n: "淇濆叏鐢熷懡", e: "Preserve Life" } ] },
        { lv: 6, items: [ { n: "鍙楃鍖昏€?, e: "Blessed Healer" } ] },
        { lv: 8, items: [ { n: "绁炲湥鎵撳嚮", e: "Divine Strike" } ] },
        { lv: 17, items: [ { n: "鑷冲杽娌荤枟", e: "Supreme Healing" } ] }
      ]
    },
    {
      name: "鍏夋槑棰嗗煙", en: "Light Domain",
      features: [
        { lv: 1, items: [ { n: "棰濆鎴忔硶", e: "Bonus Cantrip" }, { n: "瀹堟姢闂厜", e: "Warding Flare" } ] },
        { lv: 2, items: [ { n: "榛庢槑鍏夎緣", e: "Radiance of the Dawn" } ] },
        { lv: 6, items: [ { n: "寮哄寲闂厜", e: "Improved Flare" } ] },
        { lv: 8, items: [ { n: "寮哄姏鏂芥硶", e: "Potent Spellcasting" } ] },
        { lv: 17, items: [ { n: "鍏夋槑涔嬪啝", e: "Corona of Light" } ] }
      ]
    },
    {
      name: "鑷劧棰嗗煙", en: "Nature Domain",
      features: [
        { lv: 1, items: [ { n: "鑷劧渚嶅儳", e: "Acolyte of Nature" }, { n: "棰濆鐔熺粌", e: "Bonus Proficiency" } ] },
        { lv: 2, items: [ { n: "榄呮儜鍔ㄦ鐗?, e: "Charm Animals and Plants" } ] },
        { lv: 6, items: [ { n: "鍘嬪埗鍏冪礌", e: "Dampen Elements" } ] },
        { lv: 8, items: [ { n: "绁炲湥鎵撳嚮", e: "Divine Strike" } ] },
        { lv: 17, items: [ { n: "鑷劧澶у笀", e: "Master of Nature" } ] }
      ]
    },
    {
      name: "椋庢毚棰嗗煙", en: "Tempest Domain",
      features: [
        { lv: 1, items: [ { n: "棰濆鐔熺粌", e: "Bonus Proficiencies" }, { n: "椋庢毚涔嬫€?, e: "Wrath of the Storm" } ] },
        { lv: 2, items: [ { n: "姣佺伃涔嬫€?, e: "Destructive Wrath" } ] },
        { lv: 6, items: [ { n: "闆峰嚮", e: "Thunderbolt Strike" } ] },
        { lv: 8, items: [ { n: "绁炲湥鎵撳嚮", e: "Divine Strike" } ] },
        { lv: 17, items: [ { n: "椋庢毚闄嶇敓", e: "Stormborn" } ] }
      ]
    },
    {
      name: "璇¤棰嗗煙", en: "Trickery Domain",
      features: [
        { lv: 1, items: [ { n: "璇℃湳甯堢绂?, e: "Blessing of the Trickster" } ] },
        { lv: 2, items: [ { n: "鍙敜鍒嗚韩", e: "Invoke Duplicity" } ] },
        { lv: 6, items: [ { n: "鏆楀奖鏂楃", e: "Cloak of Shadows" } ] },
        { lv: 8, items: [ { n: "绁炲湥鎵撳嚮", e: "Divine Strike" } ] },
        { lv: 17, items: [ { n: "寮哄寲鍒嗚韩", e: "Improved Duplicity" } ] }
      ]
    },
    {
      name: "鎴樹簤棰嗗煙", en: "War Domain",
      features: [
        { lv: 1, items: [ { n: "棰濆鐔熺粌", e: "Bonus Proficiencies" }, { n: "鎴樹簤鐗у笀", e: "War Priest" } ] },
        { lv: 2, items: [ { n: "鎸囧紩鎵撳嚮", e: "Guided Strike" } ] },
        { lv: 6, items: [ { n: "鎴樼绁濈", e: "War God's Blessing" } ] },
        { lv: 8, items: [ { n: "绁炲湥鎵撳嚮", e: "Divine Strike" } ] },
        { lv: 17, items: [ { n: "鎴樻枟鍖栬韩", e: "Avatar of Battle" } ] }
      ]
    }
  ]
};

CLASS_FEATURES["druid"] = {
  levels: [
    { lv: 1, items: [ { n: "寰烽瞾浼婅", e: "Druidic" }, { n: "鏂芥硶", e: "Spellcasting" } ] },
    { lv: 2, items: [ { n: "閲庢€у舰鎬?, e: "Wild Shape" }, { n: "寰烽瞾浼婄粨绀?, e: "Druid Circle" } ] },
    { lv: 3, items: [] },
    { lv: 4, items: [ { n: "閲庢€у舰鎬佸己鍖栵紙CR 1/2锛?, e: "Wild Shape improvement (CR 1/2)" }, { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" } ] },
    { lv: 5, items: [] },
    { lv: 6, items: [ { n: "寰烽瞾浼婄粨绀剧壒鎬?, e: "Druid Circle feature" } ] },
    { lv: 7, items: [] },
    { lv: 8, items: [ { n: "閲庢€у舰鎬佸己鍖栵紙CR 1锛?, e: "Wild Shape improvement (CR 1)" }, { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" } ] },
    { lv: 9, items: [] },
    { lv: 10, items: [ { n: "寰烽瞾浼婄粨绀剧壒鎬?, e: "Druid Circle feature" } ] },
    { lv: 11, items: [] },
    { lv: 12, items: [ { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" } ] },
    { lv: 13, items: [] },
    { lv: 14, items: [ { n: "寰烽瞾浼婄粨绀剧壒鎬?, e: "Druid Circle feature" } ] },
    { lv: 15, items: [] },
    { lv: 16, items: [ { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" } ] },
    { lv: 17, items: [] },
    { lv: 18, items: [ { n: "涓嶈€佷箣韬?, e: "Timeless Body" }, { n: "閲庡吔鏂芥硶", e: "Beast Spells" } ] },
    { lv: 19, items: [ { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" } ] },
    { lv: 20, items: [ { n: "澶у痉椴佷紛", e: "Archdruid" } ] }
  ],
  subclasses: [
    {
      name: "澶у湴缁撶ぞ", en: "Circle of the Land",
      features: [
        { lv: 2, items: [ { n: "棰濆鎴忔硶", e: "Bonus Cantrip" }, { n: "鑷劧鎭㈠", e: "Natural Recovery" } ] },
        { lv: 3, items: [ { n: "缁撶ぞ娉曟湳", e: "Circle Spells" } ] },
        { lv: 6, items: [ { n: "澶у湴鐤捐", e: "Land's Stride" } ] },
        { lv: 10, items: [ { n: "鑷劧瀹堟姢", e: "Nature's Ward" } ] },
        { lv: 14, items: [ { n: "鑷劧鍦ｆ墍", e: "Nature's Sanctuary" } ] }
      ]
    },
    {
      name: "鏈堜寒缁撶ぞ", en: "Circle of the Moon",
      features: [
        { lv: 2, items: [ { n: "鎴樻枟閲庢€у舰鎬?, e: "Combat Wild Shape" }, { n: "缁撶ぞ褰㈡€?, e: "Circle Forms" } ] },
        { lv: 6, items: [ { n: "鍘熷鎵撳嚮", e: "Primal Strike" } ] },
        { lv: 10, items: [ { n: "鍏冪礌閲庢€у舰鎬?, e: "Elemental Wild Shape" } ] },
        { lv: 14, items: [ { n: "鍗冨舰", e: "Thousand Forms" } ] }
      ]
    }
  ]
};

CLASS_FEATURES["fighter"] = {
  levels: [
    { lv: 1, items: [ { n: "鎴樻枟椋庢牸", e: "Fighting Style" }, { n: "鍥炴皵", e: "Second Wind" } ] },
    { lv: 2, items: [ { n: "鍔ㄤ綔濡傛疆", e: "Action Surge" } ] },
    { lv: 3, items: [ { n: "姝︽湳鑼冨瀷", e: "Martial Archetype" } ] },
    { lv: 4, items: [ { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" } ] },
    { lv: 5, items: [ { n: "棰濆鏀诲嚮", e: "Extra Attack" } ] },
    { lv: 6, items: [ { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" } ] },
    { lv: 7, items: [ { n: "姝︽湳鑼冨瀷鐗规€?, e: "Martial Archetype feature" } ] },
    { lv: 8, items: [ { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" } ] },
    { lv: 9, items: [ { n: "涓嶅眻", e: "Indomitable" } ] },
    { lv: 10, items: [ { n: "姝︽湳鑼冨瀷鐗规€?, e: "Martial Archetype feature" } ] },
    { lv: 11, items: [ { n: "棰濆鏀诲嚮锛?锛?, e: "Extra Attack (2)" } ] },
    { lv: 12, items: [ { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" } ] },
    { lv: 13, items: [ { n: "涓嶅眻锛?锛?, e: "Indomitable (2)" } ] },
    { lv: 14, items: [ { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" } ] },
    { lv: 15, items: [ { n: "姝︽湳鑼冨瀷鐗规€?, e: "Martial Archetype feature" } ] },
    { lv: 16, items: [ { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" } ] },
    { lv: 17, items: [ { n: "鍔ㄤ綔濡傛疆锛?锛?, e: "Action Surge (2)" }, { n: "涓嶅眻锛?锛?, e: "Indomitable (3)" } ] },
    { lv: 18, items: [ { n: "姝︽湳鑼冨瀷鐗规€?, e: "Martial Archetype feature" } ] },
    { lv: 19, items: [ { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" } ] },
    { lv: 20, items: [ { n: "棰濆鏀诲嚮锛?锛?, e: "Extra Attack (3)" } ] }
  ],
  subclasses: [
    {
      name: "鍕囧＋", en: "Champion",
      features: [
        { lv: 3, items: [ { n: "寮哄寲閲嶅嚮", e: "Improved Critical" } ] },
        { lv: 7, items: [ { n: "闈炲嚒杩愬姩鍛?, e: "Remarkable Athlete" } ] },
        { lv: 10, items: [ { n: "棰濆鎴樻枟椋庢牸", e: "Additional Fighting Style" } ] },
        { lv: 15, items: [ { n: "鍗撹秺閲嶅嚮", e: "Superior Critical" } ] },
        { lv: 18, items: [ { n: "鐢熷瓨鑰?, e: "Survivor" } ] }
      ]
    },
    {
      name: "鎴樻枟澶у笀", en: "Battle Master",
      features: [
        { lv: 3, items: [ { n: "鎴樻枟浼樺娍", e: "Combat Superiority" }, { n: "鎴樹簤瀛﹀緬", e: "Student of War" } ] },
        { lv: 7, items: [ { n: "鐭ュ郊", e: "Know Your Enemy" } ] },
        { lv: 10, items: [ { n: "寮哄寲鎴樻枟浼樺娍锛坉10锛?, e: "Improved Combat Superiority (d10)" } ] },
        { lv: 15, items: [ { n: "涓嶅眻", e: "Relentless" } ] },
        { lv: 18, items: [ { n: "寮哄寲鎴樻枟浼樺娍锛坉12锛?, e: "Improved Combat Superiority (d12)" } ] }
      ]
    },
    {
      name: "濂ユ硶楠戝＋", en: "Eldritch Knight",
      features: [
        { lv: 3, items: [ { n: "鏂芥硶", e: "Spellcasting" }, { n: "姝﹀櫒鑱旂粨", e: "Weapon Bond" } ] },
        { lv: 7, items: [ { n: "鎴樻枟榄旀硶", e: "War Magic" } ] },
        { lv: 10, items: [ { n: "濂ユ硶鎵撳嚮", e: "Eldritch Strike" } ] },
        { lv: 15, items: [ { n: "濂ユ湳鍐查攱", e: "Arcane Charge" } ] },
        { lv: 18, items: [ { n: "寮哄寲鎴樻枟榄旀硶", e: "Improved War Magic" } ] }
      ]
    }
  ]
};

CLASS_FEATURES["monk"] = {
  levels: [
    { lv: 1, items: [ { n: "鏃犵敳闃插尽", e: "Unarmored Defense" }, { n: "姝﹁壓", e: "Martial Arts" } ] },
    { lv: 2, items: [ { n: "姘?, e: "Ki" }, { n: "鏃犵敳绉诲姩", e: "Unarmored Movement" } ] },
    { lv: 3, items: [ { n: "姝﹀儳浼犵粺", e: "Monastic Tradition" }, { n: "鍋忚浆椋炲脊", e: "Deflect Missiles" } ] },
    { lv: 4, items: [ { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" }, { n: "缂撹惤", e: "Slow Fall" } ] },
    { lv: 5, items: [ { n: "棰濆鏀诲嚮", e: "Extra Attack" }, { n: "闇囨厬鎷?, e: "Stunning Strike" } ] },
    { lv: 6, items: [ { n: "姘旂亴鎵撳嚮", e: "Ki-Empowered Strikes" }, { n: "姝﹀儳浼犵粺鐗规€?, e: "Monastic Tradition feature" } ] },
    { lv: 7, items: [ { n: "闂伩", e: "Evasion" }, { n: "蹇冨姝㈡按", e: "Stillness of Mind" } ] },
    { lv: 8, items: [ { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" } ] },
    { lv: 9, items: [ { n: "鏃犵敳绉诲姩寮哄寲", e: "Unarmored Movement improvement" } ] },
    { lv: 10, items: [ { n: "韬績绾噣", e: "Purity of Body" } ] },
    { lv: 11, items: [ { n: "姝﹀儳浼犵粺鐗规€?, e: "Monastic Tradition feature" } ] },
    { lv: 12, items: [ { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" } ] },
    { lv: 13, items: [ { n: "鏃ユ湀涔嬭", e: "Tongue of the Sun and Moon" } ] },
    { lv: 14, items: [ { n: "閲戝垰蹇?, e: "Diamond Soul" } ] },
    { lv: 15, items: [ { n: "涓嶈€佷箣韬?, e: "Timeless Body" } ] },
    { lv: 16, items: [ { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" } ] },
    { lv: 17, items: [ { n: "姝﹀儳浼犵粺鐗规€?, e: "Monastic Tradition feature" } ] },
    { lv: 18, items: [ { n: "绌轰箣韬?, e: "Empty Body" } ] },
    { lv: 19, items: [ { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" } ] },
    { lv: 20, items: [ { n: "瀹岀編鑷垜", e: "Perfect Self" } ] }
  ],
  subclasses: [
    {
      name: "寮€鎵嬩箣閬?, en: "Way of the Open Hand",
      features: [
        { lv: 3, items: [ { n: "寮€鎵嬫妧娉?, e: "Open Hand Technique" } ] },
        { lv: 6, items: [ { n: "韬綋鍋ュ叏", e: "Wholeness of Body" } ] },
        { lv: 11, items: [ { n: "瀹侀潤", e: "Tranquility" } ] },
        { lv: 17, items: [ { n: "闇囬ⅳ鎺?, e: "Quivering Palm" } ] }
      ]
    },
    {
      name: "鏆楀奖涔嬮亾", en: "Way of Shadow",
      features: [
        { lv: 3, items: [ { n: "鏆楀奖鎶€鑹?, e: "Shadow Arts" } ] },
        { lv: 6, items: [ { n: "鏆楀奖姝?, e: "Shadow Step" } ] },
        { lv: 11, items: [ { n: "鏆楀奖鏂楃", e: "Cloak of Shadows" } ] },
        { lv: 17, items: [ { n: "鎶曟満鑰?, e: "Opportunist" } ] }
      ]
    },
    {
      name: "鍥涜薄涔嬮亾", en: "Way of the Four Elements",
      features: [
        { lv: 3, items: [ { n: "鍏冪礌闂ㄥ緬", e: "Disciple of the Elements" }, { n: "鍏冪礌娉曢棬", e: "Elemental Disciplines" } ] },
        { lv: 6, items: [ { n: "鍏冪礌娉曢棬锛堟洿澶氾級", e: "Elemental Disciplines (More)" } ] },
        { lv: 11, items: [ { n: "鍏冪礌娉曢棬锛堟洿澶氾級", e: "Elemental Disciplines (More)" } ] },
        { lv: 17, items: [ { n: "鍏冪礌娉曢棬锛堟洿澶氾級", e: "Elemental Disciplines (More)" } ] }
      ]
    }
  ]
};

CLASS_FEATURES["paladin"] = {
  levels: [
    { lv: 1, items: [ { n: "绁炲湥鎰熺煡", e: "Divine Sense" }, { n: "鍦ｇ枟", e: "Lay on Hands" } ] },
    { lv: 2, items: [ { n: "鎴樻枟椋庢牸", e: "Fighting Style" }, { n: "鏂芥硶", e: "Spellcasting" }, { n: "绁炲湥鐮存柀", e: "Divine Smite" } ] },
    { lv: 3, items: [ { n: "绁炲湥鍋ュ悍", e: "Divine Health" }, { n: "绁炲湥瑾撹█", e: "Sacred Oath" } ] },
    { lv: 4, items: [ { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" } ] },
    { lv: 5, items: [ { n: "棰濆鏀诲嚮", e: "Extra Attack" } ] },
    { lv: 6, items: [ { n: "瀹堟姢鐏靛厜", e: "Aura of Protection" } ] },
    { lv: 7, items: [ { n: "绁炲湥瑾撹█鐗规€?, e: "Sacred Oath feature" } ] },
    { lv: 8, items: [ { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" } ] },
    { lv: 9, items: [] },
    { lv: 10, items: [ { n: "鍕囨皵鐏靛厜", e: "Aura of Courage" } ] },
    { lv: 11, items: [ { n: "寮哄寲绁炲湥鐮存柀", e: "Improved Divine Smite" } ] },
    { lv: 12, items: [ { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" } ] },
    { lv: 13, items: [] },
    { lv: 14, items: [ { n: "鍑€鍖栦箣瑙?, e: "Cleansing Touch" } ] },
    { lv: 15, items: [ { n: "绁炲湥瑾撹█鐗规€?, e: "Sacred Oath feature" } ] },
    { lv: 16, items: [ { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" } ] },
    { lv: 17, items: [] },
    { lv: 18, items: [ { n: "鐏靛厜鑼冨洿寮哄寲", e: "Aura improvements" } ] },
    { lv: 19, items: [ { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" } ] },
    { lv: 20, items: [ { n: "绁炲湥瑾撹█鐗规€?, e: "Sacred Oath feature" } ] }
  ],
  subclasses: [
    {
      name: "濂夌尞涔嬭獡", en: "Oath of Devotion",
      features: [
        { lv: 3, items: [ { n: "绁炲湥姝﹀櫒", e: "Sacred Weapon" }, { n: "椹辨暎閭墿", e: "Turn the Unholy" } ] },
        { lv: 7, items: [ { n: "濂夌尞鐏靛厜", e: "Aura of Devotion" } ] },
        { lv: 15, items: [ { n: "鐏甸瓊绾噣", e: "Purity of Spirit" } ] },
        { lv: 20, items: [ { n: "绁炲湥鍏夌幆", e: "Holy Nimbus" } ] }
      ]
    },
    {
      name: "涓婂彜涔嬭獡", en: "Oath of the Ancients",
      features: [
        { lv: 3, items: [ { n: "鑷劧涔嬫€?, e: "Nature's Wrath" }, { n: "椹辨暎鏃犱俊鑰?, e: "Turn the Faithless" } ] },
        { lv: 7, items: [ { n: "瀹堟姢鐏靛厜", e: "Aura of Warding" } ] },
        { lv: 15, items: [ { n: "涓嶇伃鍝ㄥ叺", e: "Undying Sentinel" } ] },
        { lv: 20, items: [ { n: "涓婂彜鍕囧＋", e: "Elder Champion" } ] }
      ]
    },
    {
      name: "澶嶄粐涔嬭獡", en: "Oath of Vengeance",
      features: [
        { lv: 3, items: [ { n: "寮冪粷鏁屼汉", e: "Abjure Enemy" }, { n: "鏁岃獡", e: "Vow of Enmity" } ] },
        { lv: 7, items: [ { n: "涓嶅眻澶嶄粐鑰?, e: "Relentless Avenger" } ] },
        { lv: 15, items: [ { n: "澶嶄粐涔嬮瓊", e: "Soul of Vengeance" } ] },
        { lv: 20, items: [ { n: "澶嶄粐澶╀娇", e: "Avenging Angel" } ] }
      ]
    }
  ]
};

CLASS_FEATURES["ranger"] = {
  levels: [
    { lv: 1, items: [ { n: "瀹挎晫", e: "Favored Enemy" }, { n: "鑷劧鎺㈢储鑰?, e: "Natural Explorer" } ] },
    { lv: 2, items: [ { n: "鎴樻枟椋庢牸", e: "Fighting Style" }, { n: "鏂芥硶", e: "Spellcasting" } ] },
    { lv: 3, items: [ { n: "鍘熷鎰熺煡", e: "Primeval Awareness" }, { n: "娓镐緺鑼冨瀷", e: "Ranger Archetype" } ] },
    { lv: 4, items: [ { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" } ] },
    { lv: 5, items: [ { n: "棰濆鏀诲嚮", e: "Extra Attack" } ] },
    { lv: 6, items: [ { n: "瀹挎晫涓庤嚜鐒舵帰绱㈣€呭己鍖?, e: "Favored Enemy and Natural Explorer improvements" } ] },
    { lv: 7, items: [ { n: "娓镐緺鑼冨瀷鐗规€?, e: "Ranger Archetype feature" } ] },
    { lv: 8, items: [ { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" }, { n: "澶у湴鐤捐", e: "Land's Stride" } ] },
    { lv: 9, items: [] },
    { lv: 10, items: [ { n: "鑷劧鎺㈢储鑰呭己鍖?, e: "Natural Explorer improvement" }, { n: "鏋楅棿闅愬尶", e: "Hide in Plain Sight" } ] },
    { lv: 11, items: [ { n: "娓镐緺鑼冨瀷鐗规€?, e: "Ranger Archetype feature" } ] },
    { lv: 12, items: [ { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" } ] },
    { lv: 13, items: [] },
    { lv: 14, items: [ { n: "瀹挎晫寮哄寲", e: "Favored Enemy improvement" }, { n: "娑堝け", e: "Vanish" } ] },
    { lv: 15, items: [ { n: "娓镐緺鑼冨瀷鐗规€?, e: "Ranger Archetype feature" } ] },
    { lv: 16, items: [ { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" } ] },
    { lv: 17, items: [] },
    { lv: 18, items: [ { n: "閲庢€ф劅鐭?, e: "Feral Senses" } ] },
    { lv: 19, items: [ { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" } ] },
    { lv: 20, items: [ { n: "灞犳埉鑰?, e: "Foe Slayer" } ] }
  ],
  subclasses: [
    {
      name: "鐚庝汉", en: "Hunter",
      features: [
        { lv: 3, items: [ { n: "鐚庝汉鐚庣墿", e: "Hunter's Prey" } ] },
        { lv: 7, items: [ { n: "闃插尽鎴樻湳", e: "Defensive Tactics" } ] },
        { lv: 11, items: [ { n: "澶氶噸鏀诲嚮", e: "Multiattack" } ] },
        { lv: 15, items: [ { n: "鍗撹秺鐚庢墜闃插尽", e: "Superior Hunter's Defense" } ] }
      ]
    },
    {
      name: "椹吔甯?, en: "Beast Master",
      features: [
        { lv: 3, items: [ { n: "娓镐緺浼欎即", e: "Ranger's Companion" } ] },
        { lv: 7, items: [ { n: "鍗撹秺璁粌", e: "Exceptional Training" } ] },
        { lv: 11, items: [ { n: "閲庡吔涔嬫€?, e: "Bestial Fury" } ] },
        { lv: 15, items: [ { n: "鍏变韩娉曟湳", e: "Share Spells" } ] }
      ]
    }
  ]
};

CLASS_FEATURES["rogue"] = {
  levels: [
    { lv: 1, items: [ { n: "涓撶簿", e: "Expertise" }, { n: "鍋疯", e: "Sneak Attack" }, { n: "鐩楄醇榛戣瘽", e: "Thieves' Cant" } ] },
    { lv: 2, items: [ { n: "鐏靛阀鍔ㄤ綔", e: "Cunning Action" } ] },
    { lv: 3, items: [ { n: "娓歌崱鑰呰寖鍨?, e: "Roguish Archetype" } ] },
    { lv: 4, items: [ { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" } ] },
    { lv: 5, items: [ { n: "闂伩", e: "Uncanny Dodge" } ] },
    { lv: 6, items: [ { n: "涓撶簿", e: "Expertise" } ] },
    { lv: 7, items: [ { n: "闂伩", e: "Evasion" } ] },
    { lv: 8, items: [ { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" } ] },
    { lv: 9, items: [ { n: "娓歌崱鑰呰寖鍨嬬壒鎬?, e: "Roguish Archetype feature" } ] },
    { lv: 10, items: [ { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" } ] },
    { lv: 11, items: [ { n: "鍙潬鎵嶈兘", e: "Reliable Talent" } ] },
    { lv: 12, items: [ { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" } ] },
    { lv: 13, items: [ { n: "娓歌崱鑰呰寖鍨嬬壒鎬?, e: "Roguish Archetype feature" } ] },
    { lv: 14, items: [ { n: "鐩叉劅", e: "Blindsense" } ] },
    { lv: 15, items: [ { n: "鏈烘晱蹇冩櫤", e: "Slippery Mind" } ] },
    { lv: 16, items: [ { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" } ] },
    { lv: 17, items: [ { n: "娓歌崱鑰呰寖鍨嬬壒鎬?, e: "Roguish Archetype feature" } ] },
    { lv: 18, items: [ { n: "椋樺拷涓嶅畾", e: "Elusive" } ] },
    { lv: 19, items: [ { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" } ] },
    { lv: 20, items: [ { n: "骞歌繍涓€鍑?, e: "Stroke of Luck" } ] }
  ],
  subclasses: [
    {
      name: "鐩楄醇", en: "Thief",
      features: [
        { lv: 3, items: [ { n: "蹇墜", e: "Fast Hands" }, { n: "椋炴獝璧板", e: "Second-Story Work" } ] },
        { lv: 9, items: [ { n: "瓒呭嚒娼滆", e: "Supreme Sneak" } ] },
        { lv: 13, items: [ { n: "浣跨敤榄旀硶瑁呯疆", e: "Use Magic Device" } ] },
        { lv: 17, items: [ { n: "鐩楄醇鍙嶅皠", e: "Thief's Reflexes" } ] }
      ]
    },
    {
      name: "鍒哄", en: "Assassin",
      features: [
        { lv: 3, items: [ { n: "棰濆鐔熺粌", e: "Bonus Proficiencies" }, { n: "鏆楁潃", e: "Assassinate" } ] },
        { lv: 9, items: [ { n: "娓楅€忎笓瀹?, e: "Infiltration Expertise" } ] },
        { lv: 13, items: [ { n: "浼澶у笀", e: "Impostor" } ] },
        { lv: 17, items: [ { n: "姝讳骸鎵撳嚮", e: "Death Strike" } ] }
      ]
    },
    {
      name: "濂ユ硶璇℃湳甯?, en: "Arcane Trickster",
      features: [
        { lv: 3, items: [ { n: "鏂芥硶", e: "Spellcasting" }, { n: "娉曞笀涔嬫墜宸ф妧", e: "Mage Hand Legerdemain" } ] },
        { lv: 9, items: [ { n: "榄旀硶浼忓嚮", e: "Magical Ambush" } ] },
        { lv: 13, items: [ { n: "澶氶潰璇℃湳甯?, e: "Versatile Trickster" } ] },
        { lv: 17, items: [ { n: "娉曟湳绐冨彇", e: "Spell Thief" } ] }
      ]
    }
  ]
};

CLASS_FEATURES["sorcerer"] = {
  levels: [
    { lv: 1, items: [ { n: "鏂芥硶", e: "Spellcasting" }, { n: "鏈硶璧锋簮", e: "Sorcerous Origin" } ] },
    { lv: 2, items: [ { n: "榄旀硶婧愭硥", e: "Font of Magic" } ] },
    { lv: 3, items: [ { n: "瓒呴瓟娉?, e: "Metamagic" } ] },
    { lv: 4, items: [ { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" } ] },
    { lv: 5, items: [] },
    { lv: 6, items: [ { n: "鏈硶璧锋簮鐗规€?, e: "Sorcerous Origin feature" } ] },
    { lv: 7, items: [] },
    { lv: 8, items: [ { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" } ] },
    { lv: 9, items: [] },
    { lv: 10, items: [ { n: "瓒呴瓟娉?, e: "Metamagic" } ] },
    { lv: 11, items: [] },
    { lv: 12, items: [ { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" } ] },
    { lv: 13, items: [] },
    { lv: 14, items: [ { n: "鏈硶璧锋簮鐗规€?, e: "Sorcerous Origin feature" } ] },
    { lv: 15, items: [] },
    { lv: 16, items: [ { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" } ] },
    { lv: 17, items: [ { n: "瓒呴瓟娉?, e: "Metamagic" } ] },
    { lv: 18, items: [ { n: "鏈硶璧锋簮鐗规€?, e: "Sorcerous Origin feature" } ] },
    { lv: 19, items: [ { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" } ] },
    { lv: 20, items: [ { n: "鏈硶鎭㈠", e: "Sorcerous Restoration" } ] }
  ],
  subclasses: [
    {
      name: "榫欐棌琛€鑴?, en: "Draconic Bloodline",
      features: [
        { lv: 1, items: [ { n: "榫欐棌鍏堢", e: "Dragon Ancestor" }, { n: "榫欐棌闊ф€?, e: "Draconic Resilience" } ] },
        { lv: 6, items: [ { n: "鍏冪礌浜插拰", e: "Elemental Affinity" } ] },
        { lv: 14, items: [ { n: "榫欑考", e: "Dragon Wings" } ] },
        { lv: 18, items: [ { n: "榫欎箣濞佷华", e: "Draconic Presence" } ] }
      ]
    },
    {
      name: "鐙傞噹榄旀硶", en: "Wild Magic",
      features: [
        { lv: 1, items: [ { n: "鐙傞噹榄旀硶娑屽姩", e: "Wild Magic Surge" }, { n: "娣锋矊涔嬫疆", e: "Tides of Chaos" } ] },
        { lv: 6, items: [ { n: "鎵洸骞歌繍", e: "Bend Luck" } ] },
        { lv: 14, items: [ { n: "鍙楁帶娣锋矊", e: "Controlled Chaos" } ] },
        { lv: 18, items: [ { n: "娉曟湳杞板嚮", e: "Spell Bombardment" } ] }
      ]
    }
  ]
};

CLASS_FEATURES["warlock"] = {
  levels: [
    { lv: 1, items: [ { n: "寮傜晫瀹椾富", e: "Otherworldly Patron" }, { n: "濂戠害榄旀硶", e: "Pact Magic" } ] },
    { lv: 2, items: [ { n: "榄旇兘绁堝敜", e: "Eldritch Invocations" } ] },
    { lv: 3, items: [ { n: "濂戠害鎭╄祼", e: "Pact Boon" } ] },
    { lv: 4, items: [ { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" } ] },
    { lv: 5, items: [] },
    { lv: 6, items: [ { n: "寮傜晫瀹椾富鐗规€?, e: "Otherworldly Patron feature" } ] },
    { lv: 7, items: [] },
    { lv: 8, items: [ { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" } ] },
    { lv: 9, items: [] },
    { lv: 10, items: [ { n: "寮傜晫瀹椾富鐗规€?, e: "Otherworldly Patron feature" } ] },
    { lv: 11, items: [ { n: "绉樻硶濂ョ锛?鐜級", e: "Mystic Arcanum (6th level)" } ] },
    { lv: 12, items: [ { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" } ] },
    { lv: 13, items: [ { n: "绉樻硶濂ョ锛?鐜級", e: "Mystic Arcanum (7th level)" } ] },
    { lv: 14, items: [ { n: "寮傜晫瀹椾富鐗规€?, e: "Otherworldly Patron feature" } ] },
    { lv: 15, items: [ { n: "绉樻硶濂ョ锛?鐜級", e: "Mystic Arcanum (8th level)" } ] },
    { lv: 16, items: [ { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" } ] },
    { lv: 17, items: [ { n: "绉樻硶濂ョ锛?鐜級", e: "Mystic Arcanum (9th level)" } ] },
    { lv: 18, items: [] },
    { lv: 19, items: [ { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" } ] },
    { lv: 20, items: [ { n: "榄旇兘澶у笀", e: "Eldritch Master" } ] }
  ],
  subclasses: [
    {
      name: "濡栫簿瀹椾富", en: "The Archfey",
      features: [
        { lv: 1, items: [ { n: "濡栫簿鏄剧伒", e: "Fey Presence" } ] },
        { lv: 6, items: [ { n: "杩烽浘鑴遍€?, e: "Misty Escape" } ] },
        { lv: 10, items: [ { n: "杩锋儜闃插尽", e: "Beguiling Defenses" } ] },
        { lv: 14, items: [ { n: "榛戞殫璋靛", e: "Dark Delirium" } ] }
      ]
    },
    {
      name: "閭瓟瀹椾富", en: "The Fiend",
      features: [
        { lv: 1, items: [ { n: "榛戞殫绁濈", e: "Dark One's Blessing" } ] },
        { lv: 6, items: [ { n: "榛戞殫骞歌繍", e: "Dark One's Own Luck" } ] },
        { lv: 10, items: [ { n: "閭瓟闊ф€?, e: "Fiendish Resilience" } ] },
        { lv: 14, items: [ { n: "鎺峰叆鍦扮嫳", e: "Hurl Through Hell" } ] }
      ]
    },
    {
      name: "鏃ф棩鏀厤鑰?, en: "The Great Old One",
      features: [
        { lv: 1, items: [ { n: "瑙夐啋蹇冪伒", e: "Awakened Mind" } ] },
        { lv: 6, items: [ { n: "鐔典箣瀹堟姢", e: "Entropic Ward" } ] },
        { lv: 10, items: [ { n: "鎬濇兂鎶ょ浘", e: "Thought Shield" } ] },
        { lv: 14, items: [ { n: "鍒涢€犲ゴ浠?, e: "Create Thrall" } ] }
      ]
    }
  ]
};

CLASS_FEATURES["wizard"] = {
  levels: [
    { lv: 1, items: [ { n: "鏂芥硶", e: "Spellcasting" }, { n: "濂ユ湳鎭㈠", e: "Arcane Recovery" } ] },
    { lv: 2, items: [ { n: "濂ユ湳浼犵粺", e: "Arcane Tradition" } ] },
    { lv: 3, items: [] },
    { lv: 4, items: [ { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" } ] },
    { lv: 5, items: [] },
    { lv: 6, items: [ { n: "濂ユ湳浼犵粺鐗规€?, e: "Arcane Tradition feature" } ] },
    { lv: 7, items: [] },
    { lv: 8, items: [ { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" } ] },
    { lv: 9, items: [] },
    { lv: 10, items: [ { n: "濂ユ湳浼犵粺鐗规€?, e: "Arcane Tradition feature" } ] },
    { lv: 11, items: [] },
    { lv: 12, items: [ { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" } ] },
    { lv: 13, items: [] },
    { lv: 14, items: [ { n: "濂ユ湳浼犵粺鐗规€?, e: "Arcane Tradition feature" } ] },
    { lv: 15, items: [] },
    { lv: 16, items: [ { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" } ] },
    { lv: 17, items: [] },
    { lv: 18, items: [ { n: "娉曟湳绮鹃€?, e: "Spell Mastery" } ] },
    { lv: 19, items: [ { n: "灞炴€у€兼彁鍗?, e: "Ability Score Improvement" } ] },
    { lv: 20, items: [ { n: "鎷涚墝娉曟湳", e: "Signature Spells" } ] }
  ],
  subclasses: [
    {
      name: "闃叉姢瀛︽淳", en: "School of Abjuration",
      features: [
        { lv: 2, items: [ { n: "闃叉姢瀛﹁€?, e: "Abjuration Savant" }, { n: "濂ユ湳瀹堝尽", e: "Arcane Ward" } ] },
        { lv: 6, items: [ { n: "鎶曞皠瀹堝尽", e: "Projected Ward" } ] },
        { lv: 10, items: [ { n: "寮哄寲闃叉姢", e: "Improved Abjuration" } ] },
        { lv: 14, items: [ { n: "娉曟湳鎶楁€?, e: "Spell Resistance" } ] }
      ]
    },
    {
      name: "鍜掓硶瀛︽淳", en: "School of Conjuration",
      features: [
        { lv: 2, items: [ { n: "鍜掓硶瀛﹁€?, e: "Conjuration Savant" }, { n: "灏忓瀷鍜掓硶", e: "Minor Conjuration" } ] },
        { lv: 6, items: [ { n: "鑹€х灛绉?, e: "Benign Transposition" } ] },
        { lv: 10, items: [ { n: "涓撴敞鍜掓硶", e: "Focused Conjuration" } ] },
        { lv: 14, items: [ { n: "鎸佷箙鍙敜", e: "Durable Summons" } ] }
      ]
    },
    {
      name: "棰勮█瀛︽淳", en: "School of Divination",
      features: [
        { lv: 2, items: [ { n: "棰勮█瀛﹁€?, e: "Divination Savant" }, { n: "棰勫厗", e: "Portent" } ] },
        { lv: 6, items: [ { n: "涓撳棰勮█", e: "Expert Divination" } ] },
        { lv: 10, items: [ { n: "绗笁鍙溂", e: "The Third Eye" } ] },
        { lv: 14, items: [ { n: "楂樼瓑棰勫厗", e: "Greater Portent" } ] }
      ]
    },
    {
      name: "闄勯瓟瀛︽淳", en: "School of Enchantment",
      features: [
        { lv: 2, items: [ { n: "闄勯瓟瀛﹁€?, e: "Enchantment Savant" }, { n: "鍌湢鍑濊", e: "Hypnotic Gaze" } ] },
        { lv: 6, items: [ { n: "鏈兘榄呮儜", e: "Instinctive Charm" } ] },
        { lv: 10, items: [ { n: "鍒嗚闄勯瓟", e: "Split Enchantment" } ] },
        { lv: 14, items: [ { n: "绡℃敼璁板繂", e: "Alter Memories" } ] }
      ]
    },
    {
      name: "濉戣兘瀛︽淳", en: "School of Evocation",
      features: [
        { lv: 2, items: [ { n: "濉戣兘瀛﹁€?, e: "Evocation Savant" }, { n: "濉戝舰娉曟湳", e: "Sculpt Spells" } ] },
        { lv: 6, items: [ { n: "寮哄姏鎴忔硶", e: "Potent Cantrip" } ] },
        { lv: 10, items: [ { n: "寮哄寲濉戣兘", e: "Empowered Evocation" } ] },
        { lv: 14, items: [ { n: "瓒呰浇", e: "Overchannel" } ] }
      ]
    },
    {
      name: "骞绘湳瀛︽淳", en: "School of Illusion",
      features: [
        { lv: 2, items: [ { n: "骞绘湳瀛﹁€?, e: "Illusion Savant" }, { n: "寮哄寲娆＄骇骞诲奖", e: "Improved Minor Illusion" } ] },
        { lv: 6, items: [ { n: "鍙骞绘湳", e: "Malleable Illusions" } ] },
        { lv: 10, items: [ { n: "骞诲奖鑷垜", e: "Illusory Self" } ] },
        { lv: 14, items: [ { n: "铏氬够鐪熷疄", e: "Illusory Reality" } ] }
      ]
    },
    {
      name: "姝荤伒瀛︽淳", en: "School of Necromancy",
      features: [
        { lv: 2, items: [ { n: "姝荤伒瀛﹁€?, e: "Necromancy Savant" }, { n: "鎭愭€栨敹鍓?, e: "Grim Harvest" } ] },
        { lv: 6, items: [ { n: "浜＄伒浠嗕粠", e: "Undead Thralls" } ] },
        { lv: 10, items: [ { n: "涓嶆閫傚簲", e: "Inured to Undeath" } ] },
        { lv: 14, items: [ { n: "鍛戒护浜＄伒", e: "Command Undead" } ] }
      ]
    },
    {
      name: "鍙樺寲瀛︽淳", en: "School of Transmutation",
      features: [
        { lv: 2, items: [ { n: "鍙樺寲瀛﹁€?, e: "Transmutation Savant" }, { n: "灏忓瀷鐐奸噾", e: "Minor Alchemy" } ] },
        { lv: 6, items: [ { n: "鍙樺寲甯堜箣鐭?, e: "Transmuter's Stone" } ] },
        { lv: 10, items: [ { n: "鍙樺舰鑰?, e: "Shapechanger" } ] },
        { lv: 14, items: [ { n: "鍙樺寲澶у笀", e: "Master Transmuter" } ] }
      ]
    }
  ]
};
