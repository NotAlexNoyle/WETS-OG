package ar.emily.wets.bukkit;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.LongSupplier;
import java.util.stream.Stream;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.event.platform.PlatformUnreadyEvent;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.AbstractBufferingExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.RunContext;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.Identifiable;
import com.sk89q.worldedit.util.collection.BlockMap;
import com.sk89q.worldedit.util.eventbus.EventHandler;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;

public final class WESpread {

	public static final UUID NON_PLAYER_ACTOR_ID =
			V5UUID.create(WESpread.class.descriptorString().getBytes(StandardCharsets.UTF_8));

	private AbstractScheduler scheduler;

	File configFile = new File("plugins/WETS-OG/config.yml");

    public static FileConfiguration convertFileToConfig(File file) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(file); 

        try {
            config.load(file);
        } catch (IOException | InvalidConfigurationException error) {
            error.printStackTrace();
        }

        return config;
    }

	FileConfiguration configFileYAML = convertFileToConfig(configFile);

	public WESpread(AbstractScheduler scheduler) {
		this.scheduler = scheduler;
	}

	private final Component INVALID_ARGUMENT = TextComponent.of(Utils.legacySerializerAnyCase(Utils.legacySerializerAnyCase(configFileYAML.getString("ChatPrefix") + "&cERROR: Blocks per tick must be a valid positive number!")));
	private final Component COMMAND_USAGE = TextComponent.of(Utils.legacySerializerAnyCase(configFileYAML.getString("ChatPrefix") + "&6Usage: &e/wets (<blocks per tick> | sorted | not-sorted)"));
	private final Component COMMAND_SORTED = TextComponent.of(Utils.legacySerializerAnyCase(configFileYAML.getString("ChatPrefix") + "&6Block placement of new operations will now be &esorted&6."));
	private final Component COMMAND_NOT_SORTED = TextComponent.of(Utils.legacySerializerAnyCase(configFileYAML.getString("ChatPrefix") + "&6Block placement of new operations will now be &eunsorted&6."));
	private final Component COMMAND_BPT_UPDATED = TextComponent.of(Utils.legacySerializerAnyCase(configFileYAML.getString("ChatPrefix") + "&aBlocks per tick placement set to: "));

	private final Object2LongMap<UUID> blocksPerTickMap = new Object2LongOpenHashMap<>();
	private final Set<UUID> actorsWhosePlacementIsNotSorted = new HashSet<>();
	private long defaultBlocksPerTick = configFileYAML.getInt("DefaultBlocksPerTick");

	public void playerLogout(final UUID id) {
		this.blocksPerTickMap.removeLong(id);
		this.actorsWhosePlacementIsNotSorted.remove(id);
	}

	public void load() {
		WorldEdit.getInstance().getEventBus().register(this);
	}

	// We really really really want to flush the queue before WorldEdit itself unloads.
	@Subscribe(priority = EventHandler.Priority.VERY_EARLY)
	public void on(final PlatformUnreadyEvent event) {
		WorldEdit.getInstance().getEventBus().unregister(this);
		this.defaultBlocksPerTick = Long.MAX_VALUE;
		this.blocksPerTickMap.clear();
		this.actorsWhosePlacementIsNotSorted.clear();
		this.scheduler.flush();
	}

	@Subscribe
	public void on(final EditSessionEvent event) {
		if (event.getStage() == EditSession.Stage.BEFORE_CHANGE) {
			final UUID actorId =
					Optional.ofNullable(event.getActor())
					// use a set ID for non-player actors, such as console or potentially API users
					.filter(Actor::isPlayer)
					.map(Identifiable::getUniqueId)
					.orElse(NON_PLAYER_ACTOR_ID);
			event.setExtent(new SchedulingExtent(event.getExtent(), actorId));
		}
	}

	public void command(final Actor source, final List<String> args) {
		if (args.size() != 1) {
			source.print(COMMAND_USAGE);
			return;
		}

		final UUID id = source.isPlayer() ? source.getUniqueId() : NON_PLAYER_ACTOR_ID;
		final String arg = args.iterator().next();
		if ("sorted".equals(arg)) {
			this.actorsWhosePlacementIsNotSorted.remove(id);
			source.print(COMMAND_SORTED);
			return;
		} else if ("not-sorted".equals(arg)) {
			this.actorsWhosePlacementIsNotSorted.add(id);
			source.print(COMMAND_NOT_SORTED);
			return;
		}

		try {
			long blocksPerTick = Long.parseLong(arg);
			if (blocksPerTick < 0) { blocksPerTick = Long.MAX_VALUE; }
			this.blocksPerTickMap.put(id, blocksPerTick);
			configFileYAML.set("DefaultBlocksPerTick", blocksPerTick);
			try {
			    configFileYAML.save(configFile); 
			}
			catch (IOException error) {
			    error.printStackTrace();
			}
			source.print(COMMAND_BPT_UPDATED.append(TextComponent.of(Utils.legacySerializerAnyCase("&e" + String.valueOf(blocksPerTick)))));
		}
		catch (final NumberFormatException exception) {
			source.printError(INVALID_ARGUMENT);
			source.print(COMMAND_USAGE);
		}
	}

	private final class SchedulingExtent extends AbstractBufferingExtent {

		private final BlockMap<BaseBlock> buffer = BlockMap.createForBaseBlock();
		private final LongSupplier blocksPerTick;
		private final boolean sorted;

		private SchedulingExtent(final Extent delegate, final UUID actor) {
			super(delegate);
			this.sorted = ! WESpread.this.actorsWhosePlacementIsNotSorted.contains(actor);
			this.blocksPerTick = () -> WESpread.this.blocksPerTickMap.getOrDefault(actor, WESpread.this.defaultBlocksPerTick);
		}

		@Override
		protected @Nullable BaseBlock getBufferedFullBlock(final BlockVector3 position) {
			return this.buffer.computeIfAbsent(position, getExtent()::getFullBlock);
		}

		@Override
		public <T extends BlockStateHolder<T>> boolean setBlock(final BlockVector3 position, final T block) {
			this.buffer.remove(position); // workaround silly WorldEdit bug, see https://github.com/emilyy-dev/wets/issues/1
			this.buffer.put(position, block.toBaseBlock());
			return true;
		}

		@Override
		protected Operation commitBefore() {
			final Iterator<Map.Entry<BlockVector3, BaseBlock>> it;
			{
				Stream<Map.Entry<BlockVector3, BaseBlock>> stream = this.buffer.entrySet().stream();
				if (this.sorted) { stream = stream.sorted(Map.Entry.comparingByKey(BlockVector3.sortByCoordsYzx())); }
				it = stream.iterator();
			}

			return new Operation() {
				boolean started = false;

				@Override
				public Operation resume(final RunContext run) {
					if (! this.started && it.hasNext()) {
						this.started = true;
						WESpread.this.scheduler.runPeriodically(this::putBlock, 1L, 1L);
					}

					return null;
				}

				private void putBlock(final Scheduler.Task task) {
					try {
						for (long i = 0; it.hasNext() && i < SchedulingExtent.this.blocksPerTick.getAsLong(); ++i) {
							final var entry = it.next();
							setDelegateBlock(entry.getKey(), entry.getValue());
						}
					} catch (final WorldEditException exception) {
						throw new RuntimeException(exception);
					} finally {
						if (! it.hasNext()) { task.cancel(); }
					}
				}

				@Override
				public void cancel() {
				}
			};
		}
	}
}