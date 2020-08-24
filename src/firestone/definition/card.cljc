(ns firestone.definition.card
  (:require [firestone.definitions :as definitions]
            [firestone.events.triggers :as triggers]
            [firestone.events.deathrattles :as deathrattles]
            [firestone.events.battlecries :as battlecries]
            [firestone.events.spells :as spells]))

(def card-definitions
  {
   "Lightwarden"
   {:name         "Lightwarden"
    :base-attack  1
    :health       2
    :mana-cost    1
    :type         :minion
    :set          :classic
    :rarity       :rare
    :description  "Whenever a character is healed, gain +2 Attack."
    :trigger      triggers/lightwarden
    }
   "The Coin"
   {:name        "The Coin"
    :mana-cost   0
    :type        :spell
    :set         :basic
    :description "Gain 1 Mana Crystal this turn only."
    :spells spells/coin}

   "Gallywix's Coin"
   {:name        "Gallywix's Coin"
    :mana-cost   0
    :type        :spell
    :set         :goblins-vs-gnomes
    :description "Gain 1 Mana Crystal this turn only. (Won't trigger Gallywix.)"
    :spells spells/coin}

   "Trade Prince Gallywix"
   {:name         "Trade Prince Gallywix"
    :mana-cost    6
    :health       5
    :base-attack  8
    :type         :minion
    :set          :goblins-vs-gnomes
    :rarity       :legendary
    :description  "Whenever your opponent casts a spell, gain a copy of it and give them a Coin."
    :trigger      triggers/gallywix}

   "Dalaran Mage"
   {:name         "Dalaran Mage"
    :mana-cost    3
    :health       4
    :base-attack  1
    :type         :minion
    :set          :basic
    :rarity       :none
    :description  "Spell Damage +1"
    :spell-damage 1}

   "Malygos"
   {:name        "Malygos"
    :base-attack      4
    :health      12
    :type        :minion
    :mana-cost   9
    :race        :dragon
    :set         :classic
    :rarity      :legendary
    :description "Spell Damage +5"
    :spell-damage 5}

   "Defender"
   {:name        "Defender"
    :base-attack 2
    :health      1
    :mana-cost   1
    :set         :classic
    :class       :paladin
    :type        :minion
    :rarity      :common}

   "Imp"
   {:name        "Imp"
    :base-attack 1
    :health      1
    :mana-cost   1
    :rarity      :common
    :set         :classic
    :type        :minion
    :race        :demon}

   "Ogre Magi"
   {:name         "Ogre Magi"
    :base-attack  4
    :health       4
    :mana-cost    4
    :spell-damage 1
    :type         :minion
    :set          :basic
    :description  "Spell Damage +1"}

   "War Golem"
   {:name        "War Golem"
    :base-attack 7
    :health      7
    :mana-cost   7
    :type        :minion
    :set         :basic
    :rarity      :none}

   "Big Game Hunter"
   {:name        "Big Game Hunter"
    :base-attack 4
    :health      2
    :mana-cost   5
    :type        :minion
    :set         :classic
    :rarity      :epic
    :description "Battlecry: Destroy a minion with an Attack of 7 or more."
    :battlecry battlecries/big-game-hunter
    :valid-targets battlecries/big-game-hunter-targets}

   "Eater of Secrets"
   {:name        "Eater of Secrets"
    :base-attack 2
    :health      4
    :mana-cost   4
    :type        :minion
    :set         :whispers-of-the-old-gods
    :rarity      :rare
    :description "Battlecry: Destroy all enemy Secrets. Gain +1/+1 for each."
    :battlecry   battlecries/eater-of-secrets}

   "Arcane Golem"
   {:name        "Arcane Golem"
    :base-attack 4
    :health      4
    :mana-cost   3
    :type        :minion
    :set         :classic
    :rarity      :rare
    :description "Battlecry: Give your opponent a Mana Crystal."
    :battlecry battlecries/arcane-golem
    }

   "Acolyte of Pain"
   {:name        "Acolyte of Pain"
    :base-attack 1
    :health      3
    :mana-cost   3
    :type        :minion
    :set         :classic
    :rarity      :common
    :description "Whenever this minion takes damage, draw a card."
    :trigger     triggers/acolyte-of-pain}

   "Lorewalker Cho"
   {:name        "Lorewalker Cho"
    :base-attack 0
    :health      4
    :type        :minion
    :mana-cost   2
    :set         :classic
    :rarity      :legendary
    :description "Whenever a player casts a spell, put a copy into the other player's hand."
    :trigger   triggers/lorewalker-cho}

   "Archmage Antonidas"
   {:name        "Archmage Antonidas"
    :base-attack 5
    :health      7
    :type        :minion
    :mana-cost   7
    :set         :classic
    :rarity      :legendary
    :description "Whenever you cast a spell, put a 'Fireball' spell into your hand."
    :trigger   triggers/archmage-antonidas}

   "Snake"
   {:name        "Snake"
    :base-attack 1
    :health      1
    :mana-cost   1
    :type        :minion
    :rarity      :rare
    :set         :classic}

   "Ancient Watcher"
   {:name        "Ancient Watcher"
    :base-attack 4
    :health      5
    :mana-cost   2
    :type        :minion
    :set         :classic
    :rarity      :rare
    :cant-attack  true
    :description "Cant attack."}

   "Sneed's Old Shredder"
   {:name        "Sneed's Old Shredder"
    :base-attack 5
    :health      7
    :mana-cost   8
    :type        :minion
    :set         :goblins-vs-gnomes
    :rarity      :legendary
    :deathrattle true
    :description "Deathrattle: Summon a random Legendary minion."
    :trigger deathrattles/sneeds-old-shredder}

   "King Mukla"
   {:name        "King Mukla"
    :base-attack 5
    :health      5
    :mana-cost   3
    :type        :minion
    :set         :classic
    :rarity      :legendary
    :description "Battlecry: Give your opponent 2 Bananas."}

   "Frostbolt"
   {:name        "Frostbolt"
    :mana-cost   2
    :type        :spell
    :set         :basic
    :rarity      :none
    :description "Deal 3 damage to a character and Freeze it."
    :spells spells/frostbolt
    :valid-targets spells/frostbolt-valid-targets}

   "Cabal Shadow Priest"
   {:name        "Cabal Shadow Priest"
    :base-attack 4
    :health      5
    :mana-cost   6
    :type        :minion
    :set         :classic
    :rarity      :epic
    :description "Battlecry: Take control of an enemy minion that has 2 or less Attack."
    :valid-targets battlecries/cabal-shadow-priest-targets}

   "Mind Control"
   {:name        "Mind Control"
    :mana-cost   10
    :type        :spell
    :set         :basic
    :rarity      :none
    :description "Take control of an enemy minion."
    :spells spells/mind-control
    :valid-targets spells/valid-mind-control-targets}

   "Deranged Doctor"
   {:name        "Deranged Doctor"
    :base-attack 8
    :health      8
    :mana-cost   8
    :type        :minion
    :set         :the-witchwood
    :rarity      :common
    :deathrattle true
    :description "Deathrattle: Restore 8 Health to your hero."
    :trigger deathrattles/deranged-doctor}

   "Sylvanas Windrunner"
   {:name        "Sylvanas Windrunner"
    :base-attack 5
    :health      5
    :mana-cost   6
    :type        :minion
    :set         :hall-of-fame
    :rarity      :legendary
    :deathrattle true
    :description "Deathrattle: Take control of a random enemy minion."
    :trigger deathrattles/sylvanas-windrunner}

   "Frothing Berserker"
   {:name        "Frothing Berserker"
    :base-attack 2
    :health      4
    :mana-cost   3
    :type        :minion
    :set         :classic
    :rarity      :rare
    :description "Whenever a minion takes damage, gain +1 Attack."
    :trigger     triggers/frothing-berserker}

   "Bananas"
   {:name        "Bananas"
    :mana-cost   1
    :type        :spell
    :set         :classic
    :description "Give a minion +1/+1."
    :spells spells/bananas
    :valid-targets spells/bananas-valid-targets}

   "Loot Hoarder"
   {:name        "Loot Hoarder"
    :base-attack 2
    :health      1
    :mana-cost   2
    :type        :minion
    :set         :classic
    :rarity      :common
    :deathrattle true
    :description "Deathrattle: Draw a card."
    :trigger     deathrattles/loot-hoarder}

   "Hadronox"
   {:name         "Hadronox"
    :base-attack  3
    :health       7
    :mana-cost    9
    :type         :minion
    :set          :knights-of-the-frozen-throne
    :rarity       :legendary
    :deathrattle  true
    :description  "Deathrattle: Summon your Taunt minions that died this game."
    :trigger      deathrattles/hadronox}

   "N'Zoth, the Corruptor"
   {:name         "N'Zoth, the Corruptor"
    :base-attack  5
    :health       7
    :mana-cost    10
    :type         :minion
    :class        :paladin
    :set          :whispers-of-the-old-gods
    :rarity       :legendary
    :deathrattle  true
    :description  "Deathrattle: Summon your Deathrattle minions that died this game."
    :trigger      deathrattles/nzoth-the-corruptor}

   "The Black Knight"
   {:name        "The Black Knight"
    :base-attack 4
    :health      5
    :mana-cost   6
    :type        :minion
    :set         :classic
    :rarity      :legendary
    :description "Battlecry: Destroy an enemy minion with Taunt."
    :battlecry battlecries/the-black-knight
    :valid-targets battlecries/valid-the-black-knight-targets}

   "Snake Trap"
   {:name        "Snake Trap"
    :mana-cost   2
    :type        :spell
    :set         :classic
    :class       :hunter
    :rarity      :epic
    :description "Secret: When one of your minions is attacked summon three 1/1 Snakes."
    :secret      true
    :trigger     triggers/snake-trap-secret}

   "Competitive Spirit"
   {:name        "Competitive Spirit"
    :type        :spell
    :mana-cost   1
    :class       :paladin
    :set         :the-grand-tournament
    :rarity      :rare
    :description "Secret: When your turn starts give your minions +1/+1."
    :secret      true
    :trigger     triggers/competitive-spirit-secret}

   "Silver Hand Recruit"
   {:name        "Silver Hand Recruit"
    :base-attack 1
    :health      1
    :mana-cost   1
    :class       :paladin
    :type        :minion
    :set         :basic
    :rarity      :none}

   "Abusive Sergeant"
   {:name        "Abusive Sergeant"
    :base-attack 1
    :health      1
    :type        :minion
    :mana-cost   1
    :set         :classic
    :rarity      :common
    :description "Battlecry: Give a minion +2 Attack this turn."
    :battlecry battlecries/abusive-sergeant
    :valid-targets battlecries/valid-abusive-sergeant-targets}

   "Rampage"
   {:name        "Rampage"
    :type        :spell
    :mana-cost   2
    :class       :warrior
    :set         :classic
    :rarity      :common
    :description "Give a damaged minion +3/+3."
    :spells spells/rampage
    :valid-targets spells/valid-rampage-targets}


   "Shrinkmeister"
   {:name        "Shrinkmeister"
    :base-attack 3
    :health      2
    :type        :minion
    :mana-cost   2
    :class       :priest
    :set         :goblins-vs-gnomes
    :rarity      :common
    :description "Battlecry: Give a minion -2 Attack this turn."
    :battlecry battlecries/shrinkmeister
    :valid-targets battlecries/valid-shrinkmeister-targets}

   "Doomsayer"
   {:name        "Doomsayer"
    :base-attack 0
    :health      7
    :type        :minion
    :mana-cost   2
    :set         :classic
    :rarity      :epic
    :description "At the start of your turn destroy ALL minions."
    :trigger triggers/doomsayer }

   "Alarm-o-Bot"
   {:name        "Alarm-o-Bot"
   :base-attack 0
   :health      3
   :type        :minion
   :mana-cost   3
   :race        :mech
   :set         :classic
   :rarity      :rare
   :description "At the start of your turn swap this minion with a random one in your hand."
   :trigger triggers/alarm-o-bot}

   "Shieldbearer"
   {:name        "Shieldbearer"
    :base-attack 0
    :health      4
    :type        :minion
    :mana-cost   0
    :set         :classic
    :rarity      :common
    :taunt       true
    :char-states  ["TAUNT"]
    :description "Taunt"}

   "Fireball"
   {:name        "Fireball"
    :type        :spell
    :mana-cost   4
    :class       :mage
    :set         :basic
    :rarity      :none
    :description "Deal 6 damage."
    :spells spells/fireball
    :valid-targets spells/fireball-valid-targets}

   "Flare"
   {:name        "Flare"
    :type        :spell
    :mana-cost   2
    :class       :hunter
    :set         :classic
    :rarity      :rare
    :description "All minions lose Stealth. Destroy all enemy Secrets. Draw a card."
    :spells spells/flare}

   "Moroes"
   {:name         "Moroes"
    :base-attack  1
    :health       1
    :mana-cost    3
    :type         :minion
    :set          :one-night-in-karazhan
    :rarity       :legendary
    :char-states  ["STEALTH"]
    :description  "Stealth: At the end of your turn, summon a 1/1 Steward."
    :trigger      triggers/moroes
    }

   "Steward"
   {:name         "Steward"
    :base-attack  1
    :health       1
    :mana-cost    1
    :type         :minion
    :set          :one-night-in-karazhan}

   "Blood Imp"
   {:name         "Blood Imp"
    :base-attack  0
    :health       1
    :mana-cost    1
    :type         :minion
    :set          :classic
    :char-states  ["STEALTH"]
    :description  "Stealth. At the end of your turn, give another random friendly minion +1 Health."
    :trigger      triggers/blood-imp}

   "Unpowered Mauler"
   {:name       "Unpowered Mauler"
    :base-attack  2
    :health       4
    :mana-cost    2
    :type         :minion
    :set          :the-boomday-project
    :rarity       :rare
    :description  "Can only attack if you cast a spell this turn."}

  "Booty Bay Bodyguard"
  {:name        "Booty Bay Bodyguard"
   :base-attack 5
   :health      4
   :type        :minion
   :mana-cost   5
   :set         :basic
   :rarity      :common
   :taunt       true
   :char-states  ["TAUNT"]
   :description "Taunt"}})

(definitions/add-definitions! card-definitions)