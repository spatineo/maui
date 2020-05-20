/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.entopix.maui.vocab;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.entopix.maui.stemmers.Stemmer;

/**
 *
 * @author nathanholmberg, zelandiya
 */
public class VocabularyStoreFactory {

    private static final Logger log = LoggerFactory.getLogger(VocabularyStoreFactory.class);

    @SuppressWarnings("rawtypes")
    public static final Class DEFAULT_VOCABULARY_CLASS = VocabularyStore_Original.class;

    private static String filenameForVocabulary(String vocabularyName, Stemmer stemmer, @SuppressWarnings("rawtypes") Class vocabularyClass) {
        return vocabularyName + "_" + vocabularyClass.getName() + "_" + stemmer.getClass().getSimpleName() + ".serialized";
    }

    public static VocabularyStore createVocabStore(String vocabularyName, Stemmer stemmer, boolean serialize) {
    	return createVocabStore(vocabularyName, stemmer, serialize, DEFAULT_VOCABULARY_CLASS);
    }

    public static VocabularyStore createVocabStore(String vocabularyName, Stemmer stemmer, boolean serialize, @SuppressWarnings("rawtypes")Class vocabularyClass) {
        VocabularyStore vocab_store = null;
        if (serialize) {
	        String filename = filenameForVocabulary(vocabularyName, stemmer, vocabularyClass);
        	log.info("Deserializing vocabulary from " + filename);
	        try {
	            FileInputStream fis = new FileInputStream(filename);
	            ObjectInputStream in = new ObjectInputStream(fis);
	            vocab_store = (VocabularyStore) in.readObject();
	            in.close();
	        } catch (IOException ex) {
	            log.info("Serialized version of the vocabulary doesn't exist. Checked " + filename);
	        } catch (ClassNotFoundException ex) {
	            log.error("Class not found while reading " + filename, ex);
	        }
        }
        if (vocab_store != null) {
            return vocab_store;
        }

        try {
        	vocab_store = (VocabularyStore) vocabularyClass.newInstance();
        } catch (InstantiationException | IllegalAccessException ex) {
        	throw new IllegalArgumentException("Could not create instance of class type ("+vocabularyClass+")", ex);
        }
        vocab_store.setVocabularyName(vocabularyName);

        return vocab_store;
    }

    public static void serializeNewVocabStore(String vocabularyName, VocabularyStore vocabStore, Stemmer stemmer) {
        if (!vocabStore.getWantsSerialization()) {
            return;
        }
        log.info("Serializing loaded vocabulary");
        String filename = filenameForVocabulary(vocabularyName, stemmer, vocabStore.getClass());
        try {
            FileOutputStream fos = new FileOutputStream(filename);
            ObjectOutputStream out = new ObjectOutputStream(fos);
            out.writeObject(vocabStore);
            out.close();
        } catch (IOException ex) {
            log.error("Error serializing vocabstore", ex);
        }
    }
}
