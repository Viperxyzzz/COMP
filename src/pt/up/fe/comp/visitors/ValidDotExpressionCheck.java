package pt.up.fe.comp.visitors;

import pt.up.fe.comp.MySymbolTable;
import pt.up.fe.comp.SemanticAnalyser;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ValidDotExpressionCheck extends PreorderJmmVisitor<Integer,Boolean> implements SemanticAnalyser{
    private final MySymbolTable mySymbolTable;
    private final List<Report> reports;
    public ValidDotExpressionCheck(MySymbolTable mySymbolTable){
        this.mySymbolTable = mySymbolTable;
        this.reports = new ArrayList<>();

        addVisit("InitStatement",this::visitDotExpr);
    }

    //doesnt check if there was a declaration BEFORE the call, fix that
    private Boolean isValid(String varName,String methodSignature){
        return mySymbolTable.getImports().contains(varName) || mySymbolTable.getFields().contains(varName) || mySymbolTable.getLocalVariables(methodSignature).contains(varName) || mySymbolTable.getParameters(methodSignature).contains(varName);
    }

    private Boolean visitDotExpr(JmmNode initStatement, Integer dummy){
        var children = initStatement.getChildren();
        for (var node : children){
            if(node.getKind().equals("Id")){
                var varName = node.get("value");
                //giga spaghetti, basically we have to get the methodName and to do that we go to class declaration -> getChildren -> find methodDecl -> find methodName
                var list1 = node.getJmmParent().getJmmParent().getChildren();
                String methodName = "";
                for (JmmNode node1 : list1){
                    if(node1.getKind().equals("MethodName")){
                        methodName = node1.get("value");
                    }
                }
                if(!isValid(varName,methodName)){
                    return reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,Integer.valueOf(node.get("line")),Integer.valueOf(node.get("col")),"Invalid function call " + varName + " wasn't instantiated yet"));
                }
            }
        }
        return true;
    }
    @Override
    public List<Report> getReports() {
        return reports;
    }
}
