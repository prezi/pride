package com.prezi.gradle.pride.vcs

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

	public void checkoutThroughCache(VcsSupport vcsSupport, String repositoryUrl, File targetDirectory) {
		def normalizedUrl = vcsSupport.normalizeRepositoryUrl(repositoryUrl)

		def moduleInCacheName = cacheMapping.getProperty(normalizedUrl)
		def newName = moduleInCacheName == null

		if (newName) {
			moduleInCacheName = sanitize(normalizedUrl)
		}

		def moduleInCache = new File(cacheDirectory, moduleInCacheName)
		if (!moduleInCache.exists()) {
			log.info "Caching repository ${repositoryUrl} as ${moduleInCacheName}"
			vcsSupport.checkout(repositoryUrl, moduleInCache, true)
		} else {
			log.info "Updating cached repository in ${moduleInCacheName}"
			vcsSupport.update(repositoryUrl, moduleInCache, true)
		}

		vcsSupport.checkout(moduleInCache.absolutePath, targetDirectory, false)
		vcsSupport.activate(repositoryUrl, targetDirectory)

		if (newName) {
			cacheMapping.put(normalizedUrl, moduleInCacheName)
			saveCacheMapping()
		}
	}

	private static String sanitize(String repositoryUrl) {
		def randomBytes = new byte[32]
		new Random().nextBytes(randomBytes)
		byte[] bytes = [repositoryUrl.bytes, randomBytes].flatten()
		def hash = MessageDigest.getInstance("SHA1").digest(bytes).encodeHex().toString().substring(0, 7)
		return repositoryUrl
				.replaceAll(/[^a-zA-Z0-9]+/, "-") + "-" + hash
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
