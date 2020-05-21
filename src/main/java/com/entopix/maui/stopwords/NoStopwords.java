package com.entopix.maui.stopwords;

import java.util.ArrayList;

/**
 * Dummy class that works like the other Stopwords classes but never
 * identifies any word as a stopword. Can be used for languages for which
 * no better Stopwords class exists.
 *
 * @author Osma Suominen (osma.suominen@helsinki.fi)
 * @version 1.0
 */
public class NoStopwords extends Stopwords {

    private static final long serialVersionUID = 1L;

    public NoStopwords() {
        // don't add any words to the list of stopwords
        super(new ArrayList<String>());
    }

    @Override
    public boolean isStopword(String word) {
        return false;
    }
}
