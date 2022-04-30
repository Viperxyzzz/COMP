package pt.up.fe.comp;

import java.util.ArrayList;
import java.util.List;

import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;

public class JmmAnalyser implements JmmAnalysis {

    @Override
    public JmmSemanticsResult semanticAnalysis(JmmParserResult parserResult) {
        List<Report> reports = new ArrayList<>();

        MySymbolTable symbolTable = new MySymbolTable();

        var symbolTableFiller = new SymbolTableFiller(symbolTable);
        symbolTableFiller.visit(parserResult.getRootNode(), symbolTable);
        reports.addAll(symbolTableFiller.getReports());

        /*
        var imports = new ArrayList<String>();
        importCollector.visit(rootNode, imports);
        System.out.println(imports);*/

        return new JmmSemanticsResult(parserResult, symbolTable, reports);

    }

}