package ar.emily.wets.bukkit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import com.sk89q.worldedit.bukkit.BukkitAdapter;

public final class WESpreadPlugin extends JavaPlugin implements Listener {

	private static final List<String> COMPLETIONS = List.of("sorted", "not-sorted");

	private final AbstractScheduler scheduler =
			new AbstractScheduler(
					getServer()::getCurrentTick,
					task -> getServer().getScheduler().runTaskTimer(this, task, 1L, 1L)
					);
	private final WESpread wetsSpread = new WESpread(this.scheduler);

	@EventHandler
	public void on(final PlayerQuitEvent event) {
		this.wetsSpread.playerLogout(event.getPlayer().getUniqueId());
	}

	@Override
	public void onLoad() {
		this.wetsSpread.load();
	}

	@Override
	public void onEnable() {
		saveDefaultConfig();
		this.scheduler.setup();
		getServer().getPluginManager().registerEvents(this, this);
	}

	@Override
	public boolean onCommand(
			final @NotNull CommandSender sender,
			final @NotNull Command command,
			final @NotNull String label,
			final String @NotNull [] args
			) {
		this.wetsSpread.command(BukkitAdapter.adapt(sender), Arrays.asList(args));
		return true;
	}

	@Override
	public @NotNull List<String> onTabComplete(
			final @NotNull CommandSender sender,
			final @NotNull Command command,
			final @NotNull String alias,
			final String @NotNull [] args
			) {
		return args.length == 1 ? StringUtil.copyPartialMatches(args[0], COMPLETIONS, new ArrayList<>(1)) : List.of();
	}

}