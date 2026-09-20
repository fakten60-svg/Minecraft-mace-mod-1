package de.aerialmace.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Targeted tests for the cloud share endpoint validation: only HTTPS URLs may be used,
 * so the client can never be pointed at an unencrypted endpoint.
 */
class CloudConfigsUrlTest {

	@Test
	void acceptsHttpsUrls() {
		assertTrue(CloudConfigs.isHttpsUrl("https://example.supabase.co"));
		assertTrue(CloudConfigs.isHttpsUrl(" https://example.supabase.co/rest/v1 "));
		assertTrue(CloudConfigs.isHttpsUrl("HTTPS://EXAMPLE.SUPABASE.CO"));
	}

	@Test
	void rejectsPlainHttp() {
		assertFalse(CloudConfigs.isHttpsUrl("http://example.supabase.co"));
	}

	@Test
	void rejectsOtherSchemesAndGarbage() {
		assertFalse(CloudConfigs.isHttpsUrl("ftp://example.com"));
		assertFalse(CloudConfigs.isHttpsUrl("file:///etc/passwd"));
		assertFalse(CloudConfigs.isHttpsUrl("not a url"));
		assertFalse(CloudConfigs.isHttpsUrl(""));
		assertFalse(CloudConfigs.isHttpsUrl("   "));
	}

	@Test
	void rejectsNull() {
		assertFalse(CloudConfigs.isHttpsUrl(null));
	}
}
