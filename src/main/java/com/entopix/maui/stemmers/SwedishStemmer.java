package com.entopix.maui.stemmers;

import org.tartarus.snowball.ext.swedishStemmer;

public class SwedishStemmer extends Stemmer {
	private static final long serialVersionUID = 1L;
	
	private swedishStemmer snowball = new swedishStemmer();
	
	@Override
	public String stem(String str) {
		snowball.setCurrent(str);
		snowball.stem();
		return snowball.getCurrent();
	}

}
