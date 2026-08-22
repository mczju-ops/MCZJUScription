#!/usr/bin/env python3
"""按 mob-cards-catalog.md 为 cards.yml 填写 sigils / evolvesTo。"""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
CARDS = ROOT / "src/main/resources/cards.yml"

# id -> (sigils, evolvesTo or None)
CARD_SIGILS = {
    "mob_allay": (["HIGH_JUMP", "GUARD_DOG"], None),
    "mob_armadillo": (["HARD_SHELL"], None),
    "mob_axolotl": (["WATER_STRIKE", "SPLIT_STRIKE", "FISH_BAIT"], None),
    "mob_bat": (["AIR_STRIKE"], None),
    "mob_bee": (["AIR_STRIKE"], None),
    "mob_blaze": (["AIR_STRIKE", "SCORCH"], None),
    "mob_bogged": (["BEAM", "VENOM_KILL"], None),
    "mob_breeze": (["GUST_SHIFT"], None),
    "mob_camel": (["WATER_STORE"], None),
    "mob_cat": (["ETERNAL_LIFE"], None),
    "mob_cave_spider": (["VENOM_KILL"], None),
    "mob_chicken": (["BREEDING"], None),
    "mob_cod": (["FISH_BAIT"], None),
    "mob_cow": (["ABUNDANCE"], None),
    "mob_creeper": (["SELF_DESTRUCT"], None),
    "mob_dolphin": (["WATER_STRIKE", "FISH_BAIT"], None),
    "mob_donkey": (["RIDING", "WANDER"], None),
    "mob_drowned": (["WATER_STRIKE"], None),
    "mob_elder_guardian": (["INTIMIDATE"], None),
    "mob_enderman": (["ENDER_SHIFT"], None),
    "mob_endermite": (["SURPRISE_ENTRY"], None),
    "mob_evoker": (["EVOKE_VEX"], None),
    "mob_fox": (["STEAL_BONE"], None),
    "mob_frog": (["HIGH_JUMP"], None),
    "mob_ghast": (["AIR_STRIKE"], None),
    "mob_glow_squid": (["INK", "FISH_BAIT"], None),
    "mob_goat": (["DEMON_OFFER"], None),
    "mob_guardian": (["WATER_STRIKE", "BEAM"], None),
    "mob_hoglin": (["DOUBLE_STRIKE", "AFFLICTION"], "mob_zoglin"),
    "mob_horse": (["RIDING"], None),
    "mob_husk": (["SCORCH"], None),
    "mob_iron_golem": (["GUARD_DOG"], None),
    "mob_llama": (["BEAM"], None),
    "mob_magma_cube": (["SPLIT_SPAWN"], None),
    "mob_magma_cube_small": ([], None),
    "mob_mooshroom": (["FERMENT"], None),
    "mob_mule": (["WANDER", "RUSH_PUSH"], None),
    "mob_ocelot": (["HISS"], None),
    "mob_panda": (["STINKY"], None),
    "mob_parrot": (["AIR_STRIKE", "ENDER_SHIFT"], None),
    "mob_phantom": (["AIR_STRIKE"], None),
    "mob_pig": (["STINKY_FAR"], None),
    "mob_piglin": (["AFFLICTION"], "mob_zombified_piglin"),
    "mob_piglin_brute": ([], None),
    "mob_pillager": (["BEAM"], None),
    "mob_polar_bear": (["ALL_STRIKE"], None),
    "mob_pufferfish": (["SPIKY_ARMOR"], None),
    "mob_rabbit": ([], None),
    "mob_ravager": (["TRI_STRIKE"], None),
    "mob_salmon": (["WATER_STRIKE", "FISH_BAIT"], None),
    "mob_sheep": (["QUALITY_SACRIFICE"], None),
    "mob_shulker": (["FIRST_SHIELD"], None),
    "mob_silverfish": (["COPY_ON_DEATH"], None),
    "mob_skeleton": (["BEAM"], None),
    "mob_skeleton_horse": (["BONE_ROYALTY"], None),
    "mob_slime": (["SPLIT_SPAWN"], None),
    "mob_slime_small": ([], None),
    "mob_sniffer": (["SNIFF_STEAL"], None),
    "mob_snow_golem": (["TAUNT_AURA"], None),
    "mob_spider": (["WEB_WEAK"], None),
    "mob_squid": (["INK", "FISH_BAIT"], None),
    "mob_stray": (["BEAM", "SLOW"], None),
    "mob_strider": (["WANDER", "SCORCH"], None),
    "mob_tadpole": (["FLEDGLING"], "mob_frog"),
    "mob_trader_llama": (["BEAM", "WANDER"], None),
    "mob_tropical_fish": (["FISH_BAIT", "SCORCH"], None),
    "mob_turtle": (["HARD_SHELL"], None),
    "mob_vex": ([], None),
    "mob_villager": (["TRADE"], None),
    "mob_vindicator": ([], None),
    "mob_warden": (["SONAR"], None),
    "mob_witch": (["VENOM_KILL"], None),
    "mob_wither": (["ALL_STRIKE"], None),
    "mob_wither_skeleton": (["BONE_ROYALTY"], None),
    "mob_wolf": ([], None),
    "mob_wolf_cub": (["FLEDGLING"], "mob_wolf"),
    "mob_zoglin": (["DOUBLE_STRIKE"], None),
    "mob_zombie": ([], None),
    "mob_zombie_horse": (["RIDING"], None),
    "mob_zombie_villager": (["TRADE"], None),
    "mob_zombified_piglin": ([], None),
    "mob_fish_dried": ([], None),
}

SPLIT_TOKEN = {
    "mob_slime": "mob_slime_small",
    "mob_magma_cube": "mob_magma_cube_small",
}


def patch_block(block: str, card_id: str) -> str:
    if card_id not in CARD_SIGILS:
        return block
    sigils, evolves = CARD_SIGILS[card_id]
    if card_id in SPLIT_TOKEN and "SPLIT_SPAWN" in sigils:
        evolves = SPLIT_TOKEN[card_id]

    sig_line = "    sigils: []"
    if sigils:
        sig_line = "    sigils: [" + ", ".join(sigils) + "]"
    if re.search(r"^\s+sigils:", block, re.M):
        block = re.sub(r"^\s+sigils:.*$", sig_line, block, count=1, flags=re.M)
    else:
        block = block.replace("    builtin: false", sig_line + "\n    builtin: false", 1)

    if evolves:
        if re.search(r"^\s+evolvesTo:", block, re.M):
            block = re.sub(r"^\s+evolvesTo:.*$", f"    evolvesTo: {evolves}", block, count=1, flags=re.M)
        else:
            block = block.replace(sig_line, sig_line + f"\n    evolvesTo: {evolves}", 1)
    else:
        block = re.sub(r"^\s+evolvesTo:.*\n", "", block, flags=re.M)
    return block


def main():
    text = CARDS.read_text(encoding="utf-8")
    parts = re.split(r"(^  mob_[a-z0-9_]+:\n)", text, flags=re.M)
    out = [parts[0]]
    i = 1
    while i < len(parts):
        header = parts[i]
        body = parts[i + 1] if i + 1 < len(parts) else ""
        card_id = header.strip().rstrip(":")
        out.append(header)
        out.append(patch_block(body, card_id))
        i += 2
    CARDS.write_text("".join(out), encoding="utf-8")
    print("patched", len(CARD_SIGILS), "cards in", CARDS)


if __name__ == "__main__":
    main()
