package application.constants;

import groovy.lang.Reference;

public class GroovyStringEscaper {
    public static String escape(final String varName, String content) {
        final Reference<String> s = new Reference<String>(content);

        s.set(s.get().replace("\\", "\\\\"));
        s.set(s.get().replace("\"", "\\\""));
        return varName + "=\"\"\"" + s.get() + "\"\"\"";
    }

}
