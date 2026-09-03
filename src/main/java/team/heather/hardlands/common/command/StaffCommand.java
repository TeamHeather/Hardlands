package team.heather.hardlands.common.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
import com.destroystokyo.paper.profile.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import team.heather.hardlands.Hardlands;
import team.heather.hardlands.common.player.PlayerManager;
import team.heather.hardlands.common.ui.chat.ChatMessenger;

@CommandAlias("staff")
@CommandPermission("hardlands.admin")
public final class StaffCommand extends BaseCommand {

    @Subcommand("profile create")
    @CommandCompletion("@players")
    private void onProfileCreate(Player sender, String username) {
        PlayerManager playerManager = Hardlands.getInstance().getPlayerManager();
        Player onlinePlayer = Bukkit.getPlayerExact(username);

        if (onlinePlayer != null) {
            if (playerManager.get(onlinePlayer) != null) {
                ChatMessenger.send(
                        sender,
                        "<white>El perfil de <red>%s<white> ya está registrado.".formatted(onlinePlayer.getName())
                );
                return;
            }

            playerManager.register(onlinePlayer);

            ChatMessenger.send(
                    sender,
                    "<white>Se ha creado el perfil de <green>%s<white>.".formatted(onlinePlayer.getName())
            );
            return;
        }

        PlayerProfile profile;

        try {
            profile = Bukkit.createProfile(username);
        } catch (IllegalArgumentException exception) {
            ChatMessenger.send(sender, "<red>El username proporcionado no es válido.");
            return;
        }

        ChatMessenger.send(sender, "<gray>Buscando el perfil de <white>%s<gray>...".formatted(username));

        profile.update().whenCompleteAsync(
                (resolvedProfile, exception) -> {
                    if (exception != null) {
                        ChatMessenger.send(sender, "<red>No se pudo obtener el perfil de <white>%s<red>.".formatted(username));
                        return;
                    }

                    String resolvedName = resolvedProfile.getName();

                    if (resolvedProfile.getId() == null || resolvedName == null || resolvedName.isBlank()) {
                        ChatMessenger.send(
                                sender,
                                "<white>No existe una cuenta de Minecraft para <red>%s<white>.".formatted(username)
                        );
                        return;
                    }

                    if (playerManager.get(resolvedProfile.getId()) != null) {
                        ChatMessenger.send(
                                sender,
                                "<white>El perfil de <red>%s<white> ya está registrado.".formatted(resolvedName)
                        );
                        return;
                    }

                    playerManager.register(resolvedName, resolvedProfile.getId());

                    ChatMessenger.send(
                            sender,
                            "<white>Se ha creado el perfil de <green>%s<white>.".formatted(resolvedName)
                    );
                },
                runnable -> Bukkit.getScheduler().runTask(Hardlands.getInstance(), runnable)
        );
    }
}