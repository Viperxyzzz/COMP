package pt.up.fe.comp;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SymbolTableFiller extends PreorderJmmVisitor<MySymbolTable,Boolean> {

    private final List<Report> reports;

    public SymbolTableFiller(MySymbolTable symbolTable){
        this.reports = new ArrayList<>();

        addVisit("ImportDeclaration", this::visitImport);
        addVisit("ClassDeclaration", this::visitClass);
        addVisit("MethodDecl", this::visitMethod);

    }

    public List<Report> getReports(){
        return reports;
    }

    private Boolean visitImport(JmmNode importDecl, MySymbolTable symbolTable){
        var importString = importDecl.getChildren().stream()
                .map(Id -> Id.get("value"))
                .collect(Collectors.joining("."));

        symbolTable.addImport(importString);

        return true;
    }

    private Boolean visitClass(JmmNode classDeclaration, MySymbolTable symbolTable){
        var className = classDeclaration.getChildren().get(0).get("value");
        symbolTable.setClassName(className);

        var index1 = classDeclaration.getChildren().get(1);
        if (index1.getKind().equals("ExtendsExp")){
            var superClass = index1.get("value");
            symbolTable.setSuperClass(superClass);
        }

        return true;
    }

    private Boolean visitMethod(JmmNode methodDecl, MySymbolTable symbolTable){
        var methodType = methodDecl.getChildren().get(0).getKind();
        //symbolTable.add(methodType);

        var methodName = methodDecl.getJmmChild(1).get("value");
        //symbolTable.add(methodName);

        if (symbolTable.hasMethod(methodName)){
            reports.add(Report.newError(Stage.SEMANTIC, -1, -1, "Duplicated method: " + methodName, null)); //ver como ir buscar linha e col (na aula)
            return false;
        }

        var parameters = methodDecl.getChildren().get(2);
        //for (parameter : parameters){var paramType = parameter.getChildren().get(0); var paramValue = parameter.getChildren().get(1);}
        //args.add(parameters);

        return true;


/*
        var methodName = methodDecl.getJmmChild(1).get("value");

        if (symbolTable.hasMethod(methodName)){
            reports.add(Report.newError(Stage.SEMANTIC, -1, -1, "Duplicated method " + methodName, null)); //ver como ir buscar linha e col (na aula)
            return false;
        }

        var returnType = methodDecl.getJmmChild(0);
        var typeName = returnType.get("value");

        var params = methodDecl.getChildren().subList(2, methodDecl.getNumChildren()-2); //verificar para nossa ast

        symbolTable.addMethod(methodName, new Type(typeName, isArray), params);*/
    }
}
