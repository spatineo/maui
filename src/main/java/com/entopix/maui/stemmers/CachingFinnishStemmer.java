package com.entopix.maui.stemmers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.puimula.libvoikko.Analysis;
import org.puimula.libvoikko.Voikko;

public class CachingFinnishStemmer extends Stemmer {
	private static final long serialVersionUID = 1L;

	public static final int DEFAULT_LRU_CACHE_SIZE = 100000;
	
	private transient Voikko voikko = null;
	
	private LRUCache<String, String> cache;
	
	public CachingFinnishStemmer() {
		cache = new LRUCache<>(DEFAULT_LRU_CACHE_SIZE);
	}
	
	@Override
	public String stem(String str) {
		if (voikko == null) {
			voikko = new Voikko("fi");
		}
		
		String stemmed;
		synchronized(cache) {
			stemmed = cache.get(str);
		}
		
		if (stemmed == null) {
			List<Analysis> analysis = voikko.analyze(str);
			
			for (Analysis a : analysis) {
				String baseform = a.get("BASEFORM");
				if (baseform != null) {
					stemmed = baseform;
					break;
				}
			}
			
			// If no baseform is found, return the original word
			if (stemmed == null) {
				stemmed = str;
			}
			
			synchronized(cache) {
				cache.put(str, stemmed);
			}
		}
	
		return stemmed;
	}
	
	private class LRUCache<K, V> extends LinkedHashMap<K, V> {
		private static final long serialVersionUID = 1L;
		
		private int cacheSize;

		public LRUCache(int cacheSize) {
			super(16, 0.75f, true);
			this.cacheSize = cacheSize;
		}

		protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
			return size() >= cacheSize;
		}
	}

}
