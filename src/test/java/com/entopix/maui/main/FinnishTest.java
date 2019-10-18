package com.entopix.maui.main;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.entopix.maui.stemmers.FinnishStemmer;
import com.entopix.maui.stemmers.Stemmer;

public class FinnishTest {

	private Stemmer stemmer;
	
	@Before
	public void setUp() throws Exception {
		stemmer = new FinnishStemmer();
	}

	@Test
	public void testKissa() {
		assertEquals("kissa", stemmer.stem("kissa"));
		assertEquals("kissa", stemmer.stem("kissoja"));
		assertEquals("kissa", stemmer.stem("kissoillakin"));
	}

}
