package com.entopix.maui.main;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.entopix.maui.stopwords.NoStopwords;
import com.entopix.maui.stopwords.Stopwords;

public class NoStopwordsTest {

	private Stopwords stopwords;

	@Before
	public void setUp() throws Exception {
		stopwords = new NoStopwords();
	}

	@Test
	public void testIsStopword() {
		assertEquals(false, stopwords.isStopword("the"));
		assertEquals(false, stopwords.isStopword("cat"));
	}

}
