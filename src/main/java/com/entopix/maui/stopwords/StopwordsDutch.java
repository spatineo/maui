package com.entopix.maui.stopwords;

/**
 * Class that can test whether a given string is a stop word. Lowercases all
 * words before the test.
 *
 * @author MJ Suhonos (mj@suhonos.ca)
 * @version 1.0
 */
public class StopwordsDutch extends Stopwords {

    private static final long serialVersionUID = 1L;

    /**
     * Default constructor uses a static stopword list
     */
    public StopwordsDutch() {
        super(StopwordsStatic.DUTCH);
    }

    public StopwordsDutch(String filePath) {
        super(filePath);
    }

    @Override
    public boolean isStopword(String word) {
        // make sure word is in lowercase
        return super.isStopword(word.toLowerCase());
    }
}
