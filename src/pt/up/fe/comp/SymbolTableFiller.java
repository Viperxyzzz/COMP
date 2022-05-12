package pt.up.fe.comp;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
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

    public SymbolTableFiller(){
        this.reports = new ArrayList<>();

        addVisit("ImportDeclaration", this::visitImport);
        addVisit("ClassDeclaration", this::visitClass);
        addVisit("MethodDecl", this::visitMethod);
        addVisit("VarDecl",this::visitVar);

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
        //classDeclaration.getOptional("extends").ifPresent(superClass -> symbolTable.setSuperClass(superClass));

        var index1 = classDeclaration.getChildren().get(1);
        if (index1.getKind().equals("ExtendsExp")){
            var superClass = index1.get("value");
            symbolTable.setSuperClass(superClass);
        }

        return true;
    }

    private Boolean visitMethod(JmmNode methodDecl, MySymbolTable symbolTable){
        var methodName = methodDecl.getJmmChild(1).get("value");

        if (symbolTable.hasMethod(methodName)){
            reports.add(Report.newError(Stage.SEMANTIC, Integer.valueOf(methodDecl.get("line")), Integer.valueOf(methodDecl.get("col")), "Duplicated method: " + methodName, null));
            return false;
        }

        var returnTypeNode = methodDecl.getJmmChild(0);
        var returnType = AstUtils.buildType(returnTypeNode);

        var params = methodDecl.getChildren().subList(2, methodDecl.getNumChildren()).stream()
                .filter(node -> node.getKind().equals("Param"))
                .collect(Collectors.toList());

        var paramSymbols = params.stream()
                .map(param -> new Symbol(AstUtils.buildType(param.getJmmChild(0)), param.getJmmChild(1).get("value")))
                .collect(Collectors.toList());

        List<Symbol> localVarsSymbols = new ArrayList<>();
        for(int i = 0; i < methodDecl.getNumChildren();i++){
            var node = methodDecl.getJmmChild(i);
            if(node.getKind().equals("InitVarDecl") && (node.getNumChildren() != 0)){
                var localVars = node.getChildren();
                localVarsSymbols = localVars.stream()
                        .map(localVar -> new Symbol(AstUtils.buildType(localVar.getJmmChild(0)), localVar.getJmmChild(1).get("value")))
                        .collect(Collectors.toList());
            }
            System.out.println(localVarsSymbols);
        }
        symbolTable.addMethod(methodName, returnType, paramSymbols,localVarsSymbols);

        return true;
    }

    private Boolean visitVar(JmmNode varDecl, MySymbolTable symbolTable){
        if(!varDecl.getJmmParent().getKind().equals("ClassDeclaration")){ // are we declaring a variable inside a class?
            return false;
        }
        var returnType = AstUtils.buildType(varDecl.getJmmChild(0));
        var fieldSymbol = new Symbol(returnType,varDecl.getJmmChild(1).get("value"));
        symbolTable.addField(fieldSymbol);

        System.out.println(fieldSymbol);
        return true;

    }
}
