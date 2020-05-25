package com.entopix.maui.stemmers;

import org.tartarus.snowball.ext.dutchStemmer;

public class DutchStemmer extends Stemmer {
	private static final long serialVersionUID = 1L;

	private transient dutchStemmer snowball;

	@Override
	public String stem(String str) {
		if (snowball == null) {
			snowball = new dutchStemmer();
		}
		snowball.setCurrent(str);
		snowball.stem();
		return snowball.getCurrent();
	}

}
