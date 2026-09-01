package team.heather.hardlands.common.item;

import java.util.Locale;

import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

public enum HardlandsItem {

    ENDER_BAG(new ItemBuilder(Material.CARROT_ON_A_STICK)
            .name("<#9B5DE5>Bolsa del End")
            .formattedLore(
                    TextColor.color(0xCDB4FF),
                    "Abre tu {cofre de Ender} al usarlo."
            )
            .footerLore("hardlands:ender_bag")
    ),

    GOLDEN_HEAD(new ItemBuilder(Material.PLAYER_HEAD)
            .skullOwner("MHF_Apple")
            .name("<#F4B942>Cabeza Dorada")
            .formattedLore(
                    TextColor.color(0xFFE08A),
                    "Se consume rápidamente y te otorga los efectos de una",
                    "{manzana dorada}, amplificados en un {nivel}."
            )
            .footerLore("hardlands:golden_head")
    ),

    VOID_BAG(new ItemBuilder(Material.CARROT_ON_A_STICK)
            .name("<#3A86A8>Bolsa del Vacío")
            .formattedLore(
                    TextColor.color(0x90E0EF),
                    "Abre el {inventario compartido} de tu equipo al usarlo."
            )
            .footerLore("hardlands:void_bag")
    ),

    BLEAK_DUALITY_SPEAR(new ItemBuilder(Material.IRON_SPEAR)
            .name("<#59656F>Dualilanza Sombría")
            .formattedLore(
                    TextColor.color(0xA7B6C2),
                    "Al atacar, entra en {enfriamiento} y despierta a su {opuesto}.",
                    "Puede blandirse {al instante}."
            )
            .footerLore("epiphany:duality_spear_bleak")
            .instantAttack()
            .enchant(Enchantment.LUNGE, 2)
    ),

    GLEAM_DUALITY_SPEAR(new ItemBuilder(Material.GOLDEN_SPEAR)
            .name("<#E6B94A>Dualilanza Radiante")
            .formattedLore(
                    TextColor.color(0xFFE6A3),
                    "Al atacar, entra en {enfriamiento} y despierta a su {opuesto}.",
                    "Puede blandirse {al instante}."
            )
            .footerLore("epiphany:duality_spear_gleam")
            .instantAttack()
            .enchant(Enchantment.LUNGE, 2)
    )

    ;

    private final ItemBuilder builder;

    HardlandsItem(ItemBuilder builder) {
        this.builder = builder;
    }

    public ItemStack build() {
        return this.builder.build();
    }

    public String getIdentifier() {
        return this.name().toLowerCase(Locale.ROOT);
    }
}