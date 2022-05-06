package pt.up.fe.comp.visitors;

import pt.up.fe.comp.MySymbolTable;
import pt.up.fe.comp.SemanticAnalyser;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ExtendsValidImportCheck implements SemanticAnalyser {

    private final MySymbolTable mySymbolTable;
    public ExtendsValidImportCheck(MySymbolTable mySymbolTable){
        this.mySymbolTable = mySymbolTable;
    }
    @Override
    public List<Report> getReports() {
        if(!mySymbolTable.getImports().contains(mySymbolTable.getSuper())){
            return Arrays.asList(new Report(ReportType.ERROR, Stage.SEMANTIC,-1,-1,"Invalid super class " + mySymbolTable.getSuper()));
        }
        return Collections.emptyList();
    }

}
