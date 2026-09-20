package de.aerialmace.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Targeted tests for the combat configuration: default safety, range normalization and
 * the cloud-upload sanitizing that must never leak sharing settings.
 */
class ModConfigTest {

	@Test
	void freshConfigIsDisabledByDefault() {
		// An automation module must never be armed on a fresh install.
		assertFalse(new ModConfig().enabled);
	}

	@Test
	void freshConfigMatchesSpecDelays() {
		ModConfig config = new ModConfig();
		assertEquals(100, config.initialDelayMin);
		assertEquals(120, config.initialDelayMax);
		assertEquals(70, config.equipToMaceDelayMin);
		assertEquals(80, config.equipToMaceDelayMax);
		assertEquals(67, config.maceToAttackDelayMin);
		assertEquals(90, config.maceToAttackDelayMax);
	}

	@Test
	void normalizeFixesSwappedDelayRanges() {
		ModConfig config = new ModConfig();
		config.initialDelayMin = 150;
		config.initialDelayMax = 110;
		config.normalize();
		assertTrue(config.initialDelayMin <= config.initialDelayMax);
	}

	@Test
	void normalizeClampsOutOfRangeValues() {
		ModConfig config = new ModConfig();
		config.targetHeightMin = -5.0;
		config.targetHeightMax = 999.0;
		config.maxTargetDistance = 0.1;
		config.normalize();
		assertTrue(config.targetHeightMin >= 0.0 && config.targetHeightMin <= 16.0);
		assertTrue(config.targetHeightMax >= config.targetHeightMin && config.targetHeightMax <= 32.0);
		assertTrue(config.maxTargetDistance >= 1.0 && config.maxTargetDistance <= 64.0);
	}

	@Test
	void normalizeKeepsHeightWindowAroundThreeBlocks() {
		ModConfig config = new ModConfig();
		config.normalize();
		assertEquals(2.85, config.targetHeightMin, 1e-9);
		assertEquals(3.25, config.targetHeightMax, 1e-9);
	}

	@Test
	void sanitizedForSharingNeverLeaksCloudSettings() {
		ModConfig config = new ModConfig();
		config.cloudShareUrl = "https://example.supabase.co";
		config.cloudShareKey = "anon-key";
		config.cloudAuthor = "tester";
		config.enabled = true;
		config.initialDelayMin = 105;

		ModConfig shared = config.sanitizedForSharing();

		assertEquals("", shared.cloudShareUrl, "share URL must never be uploaded");
		assertEquals("", shared.cloudShareKey, "anon key must never be uploaded");
		assertEquals("", shared.cloudAuthor, "author must never be uploaded");
		// Gameplay values are shared as configured.
		assertTrue(shared.enabled);
		assertEquals(105, shared.initialDelayMin);
		// The local config keeps its cloud settings.
		assertEquals("https://example.supabase.co", config.cloudShareUrl);
	}

	@Test
	void copyFromCarriesGameplayValues() {
		ModConfig source = new ModConfig();
		source.enabled = true;
		source.requireSneaking = true;
		source.initialDelayMin = 99;
		source.postAttackDelayMax = 250;

		ModConfig target = new ModConfig();
		target.copyFrom(source);

		assertTrue(target.enabled);
		assertTrue(target.requireSneaking);
		assertEquals(99, target.initialDelayMin);
		assertEquals(250, target.postAttackDelayMax);
	}
}
