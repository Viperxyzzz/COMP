package pt.up.fe.comp;

import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;

import java.util.List;
import java.util.stream.Collectors;

public class AstVisitor extends PreorderJmmVisitor<List<String>,Boolean> {
    MySymbolTable symbolTable;
    public AstVisitor(MySymbolTable symbolTable){

        this.symbolTable = symbolTable;

        addVisit("ImportDeclaration", this::visitImport);
        addVisit("ClassDeclaration", this::visitClass);
        addVisit("MainMethod", this::visitMain);
        addVisit("MethodDecl", this::visitMethod);
        
        /*addVisit("ExtendsExp", this::visitExtends);
        */
    }

    private Boolean visitImport(JmmNode importDecl, List<String> imports){
        var importString = importDecl.getChildren().stream()
                .map(Id -> Id.get("value"))
                .collect(Collectors.joining("."));

        imports.add(importString);

        return true;
    }

    private Boolean visitClass(JmmNode classDeclaration, List<String> imports){
        var className = classDeclaration.getChildren().get(0).get("value");
        imports.add(className);

        var index1 = classDeclaration.getChildren().get(1);
        if (index1.getKind().equals("ExtendsExp")){
            var extendsExp = index1.get("value");
            imports.add(extendsExp);
        }

        return true;
    }

    private Boolean visitExtends(JmmNode extendsExp, List<String> extendsValue){
        extendsValue.add(extendsExp.get("value"));

        return true;
    }

    private Boolean visitMain(JmmNode mainMethod, List<String> args){
        var argString = mainMethod.getChildren().get(0).get("value");
        args.add(argString);

        return true;
    }

    private Boolean visitMethod(JmmNode methodDecl, List<String> args){
        var methodType = methodDecl.getChildren().get(0).getKind();
        args.add(methodType);

        var methodName = methodDecl.getChildren().get(1).get("value");
        args.add(methodName);

        var parameters = methodDecl.getChildren().get(2);
        //for (parameter : parameters){var paramType = parameter.getChildren().get(0); var paramValue = parameter.getChildren().get(1);}
        //args.add(parameters);

        return true;
    }
}
