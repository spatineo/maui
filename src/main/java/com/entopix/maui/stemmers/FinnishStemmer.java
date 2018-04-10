package com.entopix.maui.stemmers;

import java.util.List;

import org.puimula.libvoikko.Analysis;
import org.puimula.libvoikko.Voikko;

public class FinnishStemmer extends Stemmer {
	private static final long serialVersionUID = 1L;

	private transient Voikko voikko = null;
	
	public FinnishStemmer() {
	
	}
	
	@Override
	public String stem(String str) {
		if (voikko == null) {
			voikko = new Voikko("fi");
		}
	
		List<Analysis> analysis = voikko.analyze(str);
		
		String stemmed = null;
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
		
		return stemmed;
	}

}
