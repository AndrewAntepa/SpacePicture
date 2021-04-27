package com.example.spacepicture;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Root;

@Root(name="speak", strict = false)
public class VoiceChoice {
    @Attribute
    String version = "1.0";

    @Attribute
    @Namespace(prefix = "xml")
    String lang = "1.0";

    @Element
    Voice voice;
}

class Voice{
    @Attribute
    @Namespace(prefix = "xml")
    String lang;

    @Attribute
    @Namespace(prefix = "xml")
    String gender;

    @Attribute
    String name;

    String text;
}
