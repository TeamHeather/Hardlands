package team.heather.hardlands.common.item;

import java.util.Optional;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

public enum HardlandsItem {

    ENDER_BAG(new ItemBuilder(Material.CARROT_ON_A_STICK)
            .name("<white>Ender Bag")
            .formattedLore("Abre tu [cofre de Ender] al hacer {clic derecho}.")
    ),

    VOID_BAG(new ItemBuilder(Material.CARROT_ON_A_STICK)
            .name("<white>Void Bag")
            .formattedLore("Abre el [inventario compartido] de tu equipo al hacer {clic derecho}.")
    ),

    GOLDEN_HEAD(new ItemBuilder(Material.PLAYER_HEAD)
            .skullOwner("MHF_Apple")
            .name("<white>Golden Head")
            .formattedLore("Se consume rápidamente y te otorga los efectos de una [manzana dorada], amplificados en {un nivel}.")
    ),

    GLEAM_DUALITY_SPEAR(new ItemBuilder(Material.GOLDEN_SPEAR)
            .name("Gleam Duality Spear")
            .formattedLore("Al atacar, entra en enfriamiento y despierta")
            .formattedLore("a su opuesto.")
            .formattedLore("Puede blandirse al instante.")
            .instantAttack()
            .enchant(Enchantment.LUNGE, 2)
    ),

    BLEAK_DUALITY_SPEAR(new ItemBuilder(Material.IRON_SPEAR)
            .name("Bleak Duality Spear")
            .formattedLore("Al atacar, entra en enfriamiento y despierta")
            .formattedLore("a su opuesto.")
            .formattedLore("Puede blandirse al instante.")
            .instantAttack()
            .enchant(Enchantment.LUNGE, 2)
    ),

    ;

    private final ItemBuilder builder;

    HardlandsItem(ItemBuilder builder) {
        this.builder = builder;
    }

    public static Optional<HardlandsItem> find(String identifier) {
        return Optional.of(valueOf(identifier.toUpperCase()));
    }

    public ItemStack build() {
        return this.builder.build().clone();
    }

    public String getIdentifier() {
        return this.name().toLowerCase();
    }
}