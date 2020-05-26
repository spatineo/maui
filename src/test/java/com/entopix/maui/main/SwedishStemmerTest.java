package com.entopix.maui.main;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.entopix.maui.stemmers.SwedishStemmer;
import com.entopix.maui.stemmers.Stemmer;

public class SwedishStemmerTest {

	private Stemmer stemmer;
	
	@Before
	public void setUp() throws Exception {
		stemmer = new SwedishStemmer();
	}

	@Test
	public void testKatt() {
		assertEquals("katt", stemmer.stem("katt"));
		assertEquals("katt", stemmer.stem("katten"));
		assertEquals("katt", stemmer.stem("katter"));
		assertEquals("katt", stemmer.stem("katterna"));
	}

}
