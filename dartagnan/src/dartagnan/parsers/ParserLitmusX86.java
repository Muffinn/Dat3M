package dartagnan.parsers;

import dartagnan.parsers.utils.ParserErrorListener;
import dartagnan.parsers.utils.ProgramBuilder;
import dartagnan.parsers.visitors.VisitorLitmusX86;
import dartagnan.program.Program;
import org.antlr.v4.runtime.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class ParserLitmusX86 implements ParserInterface {

    @Override
    public Program parse(String inputFilePath) throws IOException {
        File file = new File(inputFilePath);
        FileInputStream stream = new FileInputStream(file);
        CharStream charStream = CharStreams.fromStream(stream);

        LitmusX86Lexer lexer = new LitmusX86Lexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);

        LitmusX86Parser parser = new LitmusX86Parser(tokenStream);
        parser.addErrorListener(new DiagnosticErrorListener(true));
        parser.addErrorListener(new ParserErrorListener());
        ProgramBuilder pb = new ProgramBuilder();
        ParserRuleContext parserEntryPoint = parser.main();
        VisitorLitmusX86 visitor = new VisitorLitmusX86(pb);

        Program program = (Program) parserEntryPoint.accept(visitor);
        program.setName(inputFilePath);
        return program;
    }
}