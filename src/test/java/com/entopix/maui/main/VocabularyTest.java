package com.entopix.maui.main;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.entopix.maui.vocab.Vocabulary;

public class VocabularyTest {

	private Vocabulary vocabulary  = new Vocabulary();

	@Before
	public void loadVocabulary() throws Exception {
		// Vocabulary
		// agrovoc root URI is "http://www.fao.org/aos/agrovoc#"
		String vocabularyFormat = "skos";
		String vocabularyPath = "src/test/resources/data/vocabularies/agrovoc_sample.rdf";
		vocabulary.initializeVocabulary(vocabularyPath, vocabularyFormat);

		//System.out.println("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-");
		//System.out.println(vocabulary.getVocabularyStore().getClass().getName());
	}

	/*
	 * 1. Test term with loaded URI
	 */
	@Test
	public void testURI() {
		String uri = "http://www.fao.org/aos/agrovoc#c_165";
		String term = "Africa";

		assertEquals(term, vocabulary.getTerm(uri));
		assertEquals(uri, vocabulary.getSenses(term).get(0));
	}

	/*
	 * 2. Test term with different root URI
	 */
	@Test
	public void testRootURI() {
		String uri = "https://test.ing/12345";
		String term = "antidisestablishmentarianism";

		vocabulary.getVocabularyStore().addDescriptor(uri, term);
		vocabulary.getVocabularyStore().addSense(term, uri);

		assertEquals(term, vocabulary.getTerm(uri));
		assertEquals(uri, vocabulary.getSenses(term).get(0));
	}

	/*
	 * 3. Test term with URN
	 */
	@Test
	public void testURN() {
		String uri = "urn:isbn:1234567890";
		String term = "supercalifragilisticexpialidocious";

		vocabulary.getVocabularyStore().addDescriptor(uri, term);
		vocabulary.getVocabularyStore().addSense(term, uri);

		assertEquals(term, vocabulary.getTerm(uri));
		assertEquals(uri, vocabulary.getSenses(term).get(0));
	}

}
