package com.entopix.maui.main;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.entopix.maui.stemmers.DutchStemmer;
import com.entopix.maui.stemmers.Stemmer;

public class DutchStemmerTest {

	private Stemmer stemmer;
	
	@Before
	public void setUp() throws Exception {
		stemmer = new DutchStemmer();
	}

	@Test
	public void testHuis() {
		assertEquals("huis", stemmer.stem("huis"));
		assertEquals("huiz", stemmer.stem("huizen")); // the result is not ideal but that's what Snowball does
	}

}
