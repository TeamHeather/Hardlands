package team.heather.hardlands.internal.repository;

import team.heather.hardlands.Hardlands;

public final class Repositories {

		private final PlayerRepository playerRepository;
		private final PresetRepository presetRepository;

		public Repositories(Hardlands hardlands) {
				this.playerRepository = new PlayerRepository(hardlands);
				this.presetRepository = new PresetRepository(hardlands);
		}

		public PlayerRepository player() {
				return this.playerRepository;
		}

		public PresetRepository preset() {
				return this.presetRepository;
		}
}