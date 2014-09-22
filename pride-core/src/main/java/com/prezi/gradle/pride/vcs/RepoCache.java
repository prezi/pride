package com.prezi.gradle.pride.vcs;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Properties;

public class RepoCache {

	private static final Logger log = LoggerFactory.getLogger(RepoCache.class);
	private static final String CACHE_MAPPING_FILE = "mapping";
	private final File cacheDirectory;
	private final File mappingFile;
	private final Properties cacheMapping;

	public RepoCache(File cacheDirectory) throws IOException {
		this.cacheDirectory = cacheDirectory;
		this.mappingFile = new File(cacheDirectory, CACHE_MAPPING_FILE);
		this.cacheMapping = loadCacheMapping(mappingFile);
	}

	public void checkoutThroughCache(VcsSupport vcsSupport, final String repositoryUrl, File targetDirectory, String branch, boolean recursive) throws IOException {
		String normalizedUrl = vcsSupport.normalizeRepositoryUrl(repositoryUrl);

		String moduleInCacheName = cacheMapping.getProperty(normalizedUrl);
		boolean newName = moduleInCacheName == null;

		if (newName) {
			moduleInCacheName = sanitize(normalizedUrl);
		}

		File moduleInCache = new File(cacheDirectory, moduleInCacheName);
		if (!moduleInCache.exists()) {
			log.info("Caching repository " + repositoryUrl + " as " + moduleInCacheName);
			vcsSupport.checkout(repositoryUrl, moduleInCache, null, false, true);
		} else {
			log.info("Updating cached repository in " + moduleInCacheName);
			vcsSupport.update(moduleInCache, null, false, true);
		}

		vcsSupport.checkout(moduleInCache.getAbsolutePath(), targetDirectory, branch, recursive, false);
		vcsSupport.activate(repositoryUrl, targetDirectory);

		if (newName) {
			cacheMapping.put(normalizedUrl, moduleInCacheName);
			saveCacheMapping();
		}
	}

	private static String sanitize(String repositoryUrl) {
		byte[] bytes = repositoryUrl.getBytes();
		String hash = DigestUtils.sha1Hex(bytes).substring(0, 7);
		return repositoryUrl.replaceAll("[^a-zA-Z0-9]+", "-") + "-" + hash;
	}

	private static Properties loadCacheMapping(File mappingFile) throws IOException {
		final Properties mapping = new Properties();
		if (mappingFile.exists()) {
			Reader reader = new FileReader(mappingFile);
			try {
				mapping.load(reader);
			} finally {
				reader.close();
			}
		}

		return mapping;
	}

	private void saveCacheMapping() throws IOException {
		Writer writer = new FileWriter(mappingFile);
		try {
			cacheMapping.store(writer, null);
		} finally {
			writer.close();
		}
	}
}
