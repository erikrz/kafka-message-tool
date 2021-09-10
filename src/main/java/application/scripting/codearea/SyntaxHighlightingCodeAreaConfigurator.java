package application.scripting.codearea;

import java.util.concurrent.Executor;

import org.fxmisc.richtext.CodeArea;

public class SyntaxHighlightingCodeAreaConfigurator {

    private final Executor executor;

    public SyntaxHighlightingCodeAreaConfigurator(Executor executor) {
        this.executor = executor;
    }

    public void configureGroovySyntaxHighlighting(CodeArea area) {
        GroovyCodeAreaConfigurator.configure(area, executor);
    }

    public void configureJsonSyntaxHighlighting(CodeArea area) {
        JsonCodeAreaConfigurator.configure(area, executor);
    }
}
