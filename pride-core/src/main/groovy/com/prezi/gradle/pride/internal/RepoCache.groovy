package com.prezi.gradle.pride.internal

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.security.MessageDigest

/**
 * Created by lptr on 18/04/14.
 */
class RepoCache {
	private static final Logger log = LoggerFactory.getLogger(RepoCache)
	private static final String CACHE_MAPPING_FILE = "mapping"

	private final File cacheDirectory
	private final File mappingFile
	private final Properties cacheMapping

	public RepoCache(File cacheDirectory) {
		this.cacheDirectory = cacheDirectory
		this.mappingFile = new File(cacheDirectory, CACHE_MAPPING_FILE)
		this.cacheMapping = loadCacheMapping(mappingFile)
	}

	public void cloneRepository(String repositoryUrl, File targetDirectory) {
		def moduleInCacheName = cacheMapping.getProperty(repositoryUrl)
		def newName = moduleInCacheName == null

		if (newName) {
			moduleInCacheName = sanitize(repositoryUrl)
		}

		def moduleInCache = new File(cacheDirectory, moduleInCacheName)
		if (!moduleInCache.exists()) {
			log.info "Caching repository ${repositoryUrl} as ${moduleInCacheName}"
			GitUtils.cloneRepository(repositoryUrl, moduleInCache, true)
		} else {
			log.info "Updating cached repository in ${moduleInCacheName}"
			ProcessUtils.executeIn(moduleInCache, ["git", "fetch", "--all"])
		}

		GitUtils.cloneRepository(moduleInCache.absolutePath, targetDirectory, false)
		GitUtils.setOrigin(repositoryUrl, targetDirectory)

		if (newName) {
			cacheMapping.put(repositoryUrl, moduleInCacheName)
			saveCacheMapping()
		}
	}

	private static String sanitize(String repositoryUrl) {
		def randomBytes = new byte[32]
		new Random().nextBytes(randomBytes)
		byte[] bytes = [repositoryUrl.bytes, randomBytes].flatten()
		def hash = MessageDigest.getInstance("SHA1").digest(bytes).encodeHex().toString().substring(0, 7)
		return repositoryUrl.replaceAll(/[^a-zA-Z0-9]+/, "-") + "-" + hash
	}

	private static Properties loadCacheMapping(File mappingFile) {
		def mapping = new Properties()
		if (mappingFile.exists()) {
			mappingFile.withReader { mapping.load(it) }
		}
		return mapping
	}

	private void saveCacheMapping() {
		mappingFile.withWriter { cacheMapping.store(it, null) }
	}
}
