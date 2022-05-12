package pt.up.fe.comp.visitors;

import pt.up.fe.comp.MySymbolTable;
import pt.up.fe.comp.SemanticAnalyser;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.List;

public class ArrayAccessCheck implements SemanticAnalyser {
    private final MySymbolTable mySymbolTable;
    private final List<Report> reports;
    public ArrayAccessCheck(MySymbolTable mySymbolTable){
        this.mySymbolTable = mySymbolTable;
        this.reports = new ArrayList<>();

    }

    public boolean isInArithmeticExpr(JmmNode jmmNode){ // Array cannot be used in arithmetic operations

        return reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,Integer.valueOf(jmmNode.get("line")),Integer.valueOf(jmmNode.get("col")),"An array cannot be used in arithmetic operations."));
    }

    public boolean isArray(JmmNode jmmNode){ // Array access is done over an array
        if (jmmNode.getJmmChild(0).getKind().equals("INT_ARR")){ // tipo array
            return true;
        }
        else {
            return reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,Integer.valueOf(jmmNode.get("line")),Integer.valueOf(jmmNode.get("col")),"Array access cannot be done over a " + "boolean" + " type."));
        }
    }

    public boolean isIntArrayIndex(JmmNode jmmNode){ // Array access index is an expression of type integer
        var index = jmmNode.getJmmChild(1);
        if (index.getKind().equals("IdInt")){ // ou expressão do tipo int
            return true;
        }
        else {
            return reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,Integer.valueOf(jmmNode.get("line")),Integer.valueOf(jmmNode.get("col")),"Array access index should be of type Int, got " + index.getKind() + " instead."));
        }
    }

    @Override
    public List<Report> getReports() {
        return reports;
    }
}
