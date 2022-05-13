package pt.up.fe.comp;

import jdk.swing.interop.SwingInterOpUtils;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
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

        addVisit("InitStatement", this::visitStatement);
        addVisit("ReturnExp", this::visitReturn);
        addVisit("ArrayExp", this::visitArray);
        addVisit("BinOp", this::visitOperation);
        //addVisit("Id", this::visitId);

    }

    private Boolean visitOperation(JmmNode jmmNode, MySymbolTable symbolTable) {
        // Operands of an operation must types compatible with the operation (e.g. int + boolean is an error because + expects two integers.)
        var leftOperand = jmmNode.getJmmChild(0);
        var rightOperand = jmmNode.getJmmChild(1);
        var operation = jmmNode.get("op");

        switch (operation){
            case "assign":
                break;
            case "and":
                break;
            case "smaller":
                break;
            case "add":
            case "sub":
            case "mult":
            case "div":
                break;

        }

        return true;
    }

    private Boolean visitId(JmmNode jmmNode, MySymbolTable symbolTable) {
        // Verify if variable names used in the code have a corresponding declaration, either as a local variable, a method parameter or a field of the class (if applicable)

        // Check the declared type => no Type means no declaration
        if (!jmmNode.getAncestor("DotExp").isPresent()){
            Type varType = AstUtils.getVarType(jmmNode.get("value"), jmmNode.getAncestor("MethodDecl").get().getJmmChild(1).get("value"), symbolTable);
            System.out.println("var: " + jmmNode.get("value"));
            if (varType == null){
                return reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,Integer.valueOf(jmmNode.get("line")),
                        Integer.valueOf(jmmNode.get("col")),"Variable " + jmmNode.get("value") + " not declared."));
            }
        }

        return true;
    }

    private Boolean visitStatement(JmmNode jmmNode, MySymbolTable symbolTable) {
        addVisit("Id", this::visitId);
        return true;
    }

    private Boolean visitReturn(JmmNode jmmNode, MySymbolTable symbolTable) {
        addVisit("Id", this::visitId);
        return true;
    }

    private Boolean visitArray(JmmNode arrayExp, MySymbolTable symbolTable) {
        // Array access is done over an array
        var arrayName = arrayExp.getJmmChild(0).get("value");
        var varType = AstUtils.getVarType(arrayName, arrayExp.getAncestor("MethodDecl").get().getJmmChild(1).get("value"), symbolTable);
        if (varType == null){
            return reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,Integer.valueOf(arrayExp.get("line")),
                    Integer.valueOf(arrayExp.get("col")),"Array access cannot be done over a non declared variable."));
        } else if (!varType.isArray()){
            return reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,Integer.valueOf(arrayExp.get("line")),
                    Integer.valueOf(arrayExp.get("col")),"Array access cannot be done over a " +
                    varType.getName() + " type."));
        }

        // Array access index is an expression of type integer
        /*if (!arrayExp.getJmmChild(1).getKind().equals("IdInt")){ // ou expressão do tipo int
            return reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,Integer.valueOf(arrayExp.get("line")),
                    Integer.valueOf(arrayExp.get("col")),"Array access index should be of type Int, got " +
                    arrayExp.getJmmChild(1).getKind() + " instead."));
        }*/

        return true;
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
        return true;

    }
}
