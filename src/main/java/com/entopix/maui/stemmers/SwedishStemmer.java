package com.entopix.maui.stemmers;

import org.tartarus.snowball.ext.swedishStemmer;

public class SwedishStemmer extends Stemmer {
	private static final long serialVersionUID = 1L;
	
	private transient swedishStemmer snowball;
	
	@Override
	public String stem(String str) {
		if (snowball == null) {
			snowball = new swedishStemmer();
		}
		snowball.setCurrent(str);
		snowball.stem();
		return snowball.getCurrent();
	}

}
