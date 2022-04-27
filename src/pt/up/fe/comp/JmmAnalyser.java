package pt.up.fe.comp;

import java.util.ArrayList;
import java.util.Collections;

import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.parser.JmmParserResult;

public class JmmAnalyser implements JmmAnalysis {

    @Override
    public JmmSemanticsResult semanticAnalysis(JmmParserResult parserResult) {
        MySymbolTable symbolTable = new MySymbolTable();

        JmmNode rootNode = parserResult.getRootNode();

        var importCollector = new AstVisitor(symbolTable);
        var imports = new ArrayList<String>();
        System.out.print(importCollector.visit(rootNode, imports));

        return new JmmSemanticsResult(parserResult, symbolTable, Collections.emptyList());

    }

}
