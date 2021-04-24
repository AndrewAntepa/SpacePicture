    package com.example.spacepicture;

import java.util.ArrayList;

public class ResponseTranslate {

    class TextTranslation {
        String text;
        String to;
    }

    ArrayList<TextTranslation> translations;

    @Override
    public String toString() {
        String s = "";
        for (TextTranslation tt:translations) {
            s += tt.text + "\n\n";
        }
        return s;
    }
}
