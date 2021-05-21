package com.ibm.wh.extractionservice.support;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class JsoupUtils {

    public static Document parseDocumentPreservingWhitespaces(String html) {
        Document document = Jsoup.parse(html);

        // We need to disable the pretty print of the document to avoid the automatic
        // indentation spaces insertion inside the textual elements. Spacing inside
        // the fragment elements is necessary to preserve the correct textual representation
        // of the document
        document.outputSettings().prettyPrint(false);
        return document;
    }

}
