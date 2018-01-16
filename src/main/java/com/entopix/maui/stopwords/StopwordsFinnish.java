package com.entopix.maui.stopwords;

/**
 * Class that can test whether a given string is a stop word. Lowercases all
 * words before the test.
 *
 * @author Sampo Savolainen (sampo.savolainen@spatineo.com)
 * @version 1.0
 */
public class StopwordsFinnish extends Stopwords {

    private static final long serialVersionUID = 1L;
    
    /**
     * Default constructor uses a static stopword list
     */
    public StopwordsFinnish() {
        super(StopwordsStatic.FINNISH);
    }

    public StopwordsFinnish(String filePath) {
        super(filePath);
    }
    
    @Override
    public boolean isStopword(String word) {
        // make sure word is in lowercase
        return super.isStopword(word.toLowerCase());
    }
}
