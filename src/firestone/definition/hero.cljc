(ns firestone.definition.hero
  (:require [firestone.definitions :as definitions]
            [firestone.events.hero-powers :as hero-powers]))

(def hero-definitions
  {
   "Anduin Wrynn"
   {:name       "Anduin Wrynn"
    :type       :hero
    :class      :priest
    :health     30
    :hero-power "Lesser Heal"}

   "Jaina Proudmoore"
   {:name       "Jaina Proudmoore"
    :type       :hero
    :class      :mage
    :health     30
    :hero-power "Fireblast"}

   "Rexxar"
   {:name       "Rexxar"
    :type       :hero
    :class      :hunter
    :health     30
    :hero-power "Steady Shot"}

   "Uther Lightbringer"
   {:name       "Uther Lightbringer"
    :type       :hero
    :class      :paladin
    :health     30
    :hero-power "Reinforce"}

   "Fireblast"
   {:name        "Fireblast"
    :mana-cost   2
    :type        :hero-power
    :description "Deal 1 damage."
    :power       hero-powers/fireblast
    :valid-targets hero-powers/valid-fireblast-targets}

   "Lesser Heal"
   {:name        "Lesser Heal"
    :mana-cost   2
    :type        :hero-power
    :description "Restore 2 health."
    :power hero-powers/lesser-heal
    :valid-targets hero-powers/valid-lesser-heal-targets}

   "Reinforce"
   {:name        "Reinforce"
    :mana-cost   2
    :type        :hero-power
    :description "Summon a 1/1 Silver Hand Recruit."
    :power hero-powers/reinforce}

   "Steady Shot"
   {:name        "Steady Shot"
    :mana-cost   2
    :type        :hero-power
    :description "Deal 2 damage to the enemy hero."
    :power hero-powers/steady-shot}

   })

(definitions/add-definitions! hero-definitions)