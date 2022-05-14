package pt.up.fe.comp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.visitors.ExtendsValidImportCheck;
import pt.up.fe.comp.visitors.ValidDotExpressionCheck;

public class JmmAnalyser implements JmmAnalysis {

    @Override
    public JmmSemanticsResult semanticAnalysis(JmmParserResult parserResult) {
        List<Report> reports = new ArrayList<>();

        MySymbolTable symbolTable = new MySymbolTable();

        var symbolTableFiller = new SymbolTableFiller();
        symbolTableFiller.visit(parserResult.getRootNode(), symbolTable);
        /*
        reports.addAll(symbolTableFiller.getReports());
        List<SemanticAnalyser> analysers = Arrays.asList(new ExtendsValidImportCheck(symbolTable));
        /*ValidDotExpressionCheck validDotExpressionCheck = new ValidDotExpressionCheck(symbolTable);
        validDotExpressionCheck.visit(parserResult.getRootNode());
        reports.addAll(validDotExpressionCheck.getReports());

        for(var analyzer : analysers){
            reports.addAll(analyzer.getReports());
        }*/

        return new JmmSemanticsResult(parserResult, symbolTable, reports);

    }

}
