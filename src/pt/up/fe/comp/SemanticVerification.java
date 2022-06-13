package pt.up.fe.comp;

import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.List;

public class SemanticVerification extends PreorderJmmVisitor<MySymbolTable,String> {

    private final List<Report> reports;

    public SemanticVerification(){
        this.reports = new ArrayList<>();

        addVisit("InitStatement", this::visitStatement);
        addVisit("ReturnExp", this::visitReturn);
        addVisit("IfStatement", this::visitIf);
        addVisit("WhileStatement", this::visitWhile);
        addVisit("Assignment", this::visitAssignment);
        addVisit("AndExp", this::visitAnd);
        addVisit("SmallerThan", this::visitSmaller);
        addVisit("BinOp", this::visitOperation);
        addVisit("ArrayExp", this::visitArray);
        addVisit("Not", this::visitNot);
        addVisit("DotExp", this::visitDot);
        addVisit("IdInt", this::visitInt);
        addVisit("TrueId", this::visitBoolean);
        addVisit("FalseId", this::visitBoolean);
        addVisit("NewArray", this::visitNewArray);
        addVisit("NewExp", this::visitNew);
        addVisit("ThisId", this::visitThis);
    }

    private String visitThis(JmmNode jmmNode, MySymbolTable symbolTable) {
        if (jmmNode.getAncestor("MethodDecl").get().getJmmChild(1).get("value").equals("main")){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.valueOf(jmmNode.get("line")),
                    Integer.valueOf(jmmNode.get("col")), "Cannot use this in static methods"));
        }
        return "";
    }


    public boolean isImported(JmmNode jmmNode, MySymbolTable symbolTable) {

        if (jmmNode.getKind().equals("Id")) {

            Type varType = AstUtils.getVarType(jmmNode.get("value"), jmmNode.getAncestor("MethodDecl").get().getJmmChild(1).get("value"), symbolTable);

            if(!(varType == null)){
                if (symbolTable.getImports().contains(varType.getName())) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean checkDotClass(JmmNode jmmNode, MySymbolTable symbolTable) {
        if (jmmNode.getKind().equals("ThisId")) {
            return true;
        }

        if (jmmNode.getAttributes().contains("value")){

            var varName = jmmNode.get("value");

            if (varName.equals(symbolTable.getClassName())){
                return true;
            }

            var imports = symbolTable.getImports();
            if(imports.contains(varName)){
                return true;
            }


            Type varType = AstUtils.getVarType(varName, jmmNode.getAncestor("MethodDecl").get().getJmmChild(1).get("value"), symbolTable);
            /*if(varType != null && (extends || imports || className)){
                return true;
            }*/
            if(!(varType == null)){
                return true;
            }


        }
        return false;
    }

    private String checkDotParams(JmmNode call, String varType, MySymbolTable symbolTable){
        var methodName = call.getJmmChild(0).get("value");

        // check for calls to undeclared methods
        if (!symbolTable.getMethods().contains(methodName)){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.valueOf(call.get("line")),
                    Integer.valueOf(call.get("col")), "Method '" + methodName + "' does not exist."));

            return "";
        }

        // verificar parâmetros
        if (call.getNumChildren() > 1 &&
                (symbolTable.hasMethod(methodName) && varType.equals(symbolTable.getClassName()))) { // existem parâmetros e o método está declarado na classe
            var params = call.getJmmChild(1);

            // verificar se numero de params está correto
            if (symbolTable.getParameters(methodName).size() == params.getNumChildren()) {
                // verificar se tipo dos params está correto
                var paramsInMethod = symbolTable.getParameters(methodName);
                for (int i = 0; i < params.getNumChildren(); i++) {
                    var param = params.getJmmChild(i);

                    if (!param.getKind().equals("Id")) {
                        if (param.getKind().equals("ThisId")){ // This as arg
                            if (!paramsInMethod.get(i).getType().getName().equals(symbolTable.getClassName())) {
                                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.valueOf(call.get("line")),
                                        Integer.valueOf(call.get("col")), "Got incompatible arguments for call on method " + methodName));
                            }
                        }
                        else{
                            var paramType = visit(param, symbolTable);

                            if (!paramsInMethod.get(i).getType().getName().equals(paramType)) {
                                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.valueOf(call.get("line")),
                                        Integer.valueOf(call.get("col")), "Got incompatible arguments for call on method " + methodName));
                            }
                        }
                    } else {
                        Type idType = AstUtils.getVarType(param.get("value"), param.getAncestor("MethodDecl").get().getJmmChild(1).get("value"), symbolTable);
                        if (!paramsInMethod.get(i).getType().getName().equals(idType.getName())) {
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.valueOf(call.get("line")),
                                    Integer.valueOf(call.get("col")), "Got incompatible arguments for call on method " + methodName));
                        }
                    }
                }
            } else {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.valueOf(call.get("line")),
                        Integer.valueOf(call.get("col")), "Got wrong number of parameters for call on method " + methodName));
            }
        }
        else if (symbolTable.getParameters(methodName).size() != 0) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.valueOf(call.get("line")),
                    Integer.valueOf(call.get("col")), "Got wrong number of parameters for call on method " + methodName));
        }

        if (varType != null && varType.equals(symbolTable.getClassName())) { // é do tipo da classe principal
            if (!symbolTable.hasMethod(methodName) && symbolTable.getSuper() == null) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.valueOf(call.get("line")),
                        Integer.valueOf(call.get("col")), "Call to undeclared method: " + methodName));
            }
            if (symbolTable.hasMethod(methodName)) {
                Type returnType = symbolTable.getReturnType(methodName);
                if (returnType.isArray())
                    return returnType.getName() + "[]";
                else
                    return returnType.getName();
            }
        }

        return "dot";
    }

    private String visitDot(JmmNode jmmNode, MySymbolTable symbolTable) {
        JmmNode id = jmmNode.getJmmChild(0);
        JmmNode call = jmmNode.getJmmChild(1);

        if (!id.getKind().equals("DotExp") && !checkDotClass(id, symbolTable)) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.valueOf(jmmNode.get("line")),
                    Integer.valueOf(jmmNode.get("col")), "Class " + id.get("value") + " not imported."));
        }

        if (call.getKind().equals("LengthExp")) {
            if (id.getKind().equals("ThisId")) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.valueOf(jmmNode.get("line")),
                        Integer.valueOf(jmmNode.get("col")), "this.length is not valid."));
            }
            else {
                if (id.getKind().equals("Id")){
                    Type varType = AstUtils.getVarType(id.get("value"), id.getAncestor("MethodDecl").get().getJmmChild(1).get("value"), symbolTable);

                    if (varType != null) {
                        if (!varType.isArray() && (varType.getName().equals("int") || varType.getName().equals("boolean"))) {
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.valueOf(jmmNode.get("line")),
                                    Integer.valueOf(jmmNode.get("col")), varType.getName() + " cannot be used with length."));
                        }
                        else if (symbolTable.getImports().contains(varType.getName()) || symbolTable.getClassName().equals(varType.getName()) || symbolTable.getImports().contains(id.get("value"))){
                            System.out.println(id.get("value") + " pode ser importado.");
                        }
                        else if (!varType.isArray()){
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.valueOf(jmmNode.get("line")),
                                    Integer.valueOf(jmmNode.get("col")), varType.getName() + " cannot be used with length."));
                        }
                    }

                }
                else {
                    var type = visit(id, symbolTable);
                    System.out.println("Este tipo fica À esquerda de length " + type);
                }


                return "int";
            }

        }

        var methodName = call.getJmmChild(0).get("value");

        if (!id.getKind().equals("ThisId")) {

            if (id.get("value").equals(symbolTable.getClassName())){ // Classe.etc()
                return checkDotParams(call, symbolTable.getClassName(), symbolTable);
            }

            // check for calls to undeclared methods
            Type varType = AstUtils.getVarType(id.get("value"), id.getAncestor("MethodDecl").get().getJmmChild(1).get("value"), symbolTable);

            // verificar parâmetros
            if (call.getNumChildren() > 1 &&
                    (symbolTable.hasMethod(methodName) && varType.getName().equals(symbolTable.getClassName()))) { // existem parâmetros e o método está declarado na classe
                var params = call.getJmmChild(1);

                // verificar se numero de params está correto
                if (symbolTable.getParameters(methodName).size() == params.getNumChildren()) {
                    // verificar se tipo dos params está correto
                    var paramsInMethod = symbolTable.getParameters(methodName);
                    for (int i = 0; i < params.getNumChildren(); i++) {
                        var param = params.getJmmChild(i);

                        if (!param.getKind().equals("Id")) {
                            if (param.getKind().equals("ThisId")){ // This as arg
                                if (!paramsInMethod.get(i).getType().getName().equals(symbolTable.getClassName())) {
                                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.valueOf(call.get("line")),
                                            Integer.valueOf(call.get("col")), "Got incompatible arguments for call on method " + methodName));
                                }
                            }
                            else{
                                var paramType = visit(param, symbolTable);

                                if (!paramsInMethod.get(i).getType().getName().equals(paramType)) {
                                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.valueOf(call.get("line")),
                                            Integer.valueOf(call.get("col")), "Got incompatible arguments for call on method " + methodName));
                                }
                            }
                        } else {
                            Type idType = AstUtils.getVarType(param.get("value"), param.getAncestor("MethodDecl").get().getJmmChild(1).get("value"), symbolTable);
                            if (!paramsInMethod.get(i).getType().getName().equals(idType.getName())) {
                                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.valueOf(jmmNode.get("line")),
                                        Integer.valueOf(jmmNode.get("col")), "Got incompatible arguments for call on method " + methodName));
                            }
                        }
                    }
                } else {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.valueOf(jmmNode.get("line")),
                            Integer.valueOf(jmmNode.get("col")), "Got wrong number of parameters for call on method " + methodName));
                }
            }

            if (varType != null && varType.getName().equals(symbolTable.getClassName())) { // é do tipo da classe principal
                if (!symbolTable.hasMethod(methodName) && symbolTable.getSuper() == null) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.valueOf(jmmNode.get("line")),
                            Integer.valueOf(jmmNode.get("col")), "Call to undeclared method: " + methodName));
                }
                if (symbolTable.hasMethod(methodName)) {
                    Type returnType = symbolTable.getReturnType(methodName);
                    if (returnType.isArray())
                        return returnType.getName() + "[]";
                    else
                        return returnType.getName();
                }
            }
            else if (varType != null && (symbolTable.getImports().contains(varType.getName()))) { // é de um tipo importado
                System.out.println("Method " + methodName + " could be imported in: " + id.get("value") + " of type " + varType.getName());
            }
            else if(symbolTable.getImports().contains(id.get("value"))) { // é igual ao import
                System.out.println("Method " + methodName + " could be imported, extended or is the class");
            }
            else {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.valueOf(jmmNode.get("line")),
                        Integer.valueOf(jmmNode.get("col")), id.get("value") + " does not have methods."));
            }

        }
        else {
            return checkDotParams(call, symbolTable.getClassName(), symbolTable);
        }

        return "dot";
    }

    private String visitIf(JmmNode jmmNode, MySymbolTable symbolTable) {
        JmmNode conditionNode = jmmNode.getJmmChild(0);
        String conditionType = visit(conditionNode, symbolTable);
        if (!(conditionType.equals("boolean") || conditionType.equals("dot"))){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,Integer.valueOf(jmmNode.get("line")),
                    Integer.valueOf(jmmNode.get("col")),"Expressions in conditions must return a boolean, got " + conditionType));
        }

        return "";
    }

    private String visitWhile(JmmNode jmmNode, MySymbolTable symbolTable) {
        JmmNode conditionNode = jmmNode.getJmmChild(0);
        String conditionType = visit(conditionNode, symbolTable);
        if (!(conditionType.equals("boolean") || conditionType.equals("dot"))){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,Integer.valueOf(jmmNode.get("line")),
                    Integer.valueOf(jmmNode.get("col")),"Expressions in conditions must return a boolean, got " + conditionType));
        }

        return "";
    }

    private String visitBoolean(JmmNode jmmNode, MySymbolTable symbolTable) {
        return "boolean";
    }

    private String visitInt(JmmNode jmmNode, MySymbolTable symbolTable) {
        return "int";
    }

    private String visitNot(JmmNode jmmNode, MySymbolTable symbolTable) {
        JmmNode node = jmmNode.getJmmChild(0);
        String nodeType = visit(node, symbolTable);

        if (nodeType.equals("boolean") || nodeType.equals("dot")){
            return "boolean";
        }
        else{
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,Integer.valueOf(jmmNode.get("line")),
                    Integer.valueOf(jmmNode.get("col")),"Negation only works for boolean expressions, got " + nodeType));

        }

        return "";
    }

    private String visitNewArray(JmmNode jmmNode, MySymbolTable symbolTable) {
        return "int[]";
    }

    private String visitNew(JmmNode jmmNode, MySymbolTable symbolTable) {
        JmmNode node = jmmNode.getJmmChild(0);

        return node.get("value");
    }

    private String visitSmaller(JmmNode jmmNode, MySymbolTable symbolTable) {
        JmmNode leftNode = jmmNode.getChildren().get(0);
        JmmNode rightNode = jmmNode.getChildren().get(1);

        String leftType = visit(leftNode, symbolTable);
        String rightType = visit(rightNode, symbolTable);

        if ((leftType.equals("int") || leftType.equals("dot")) && (rightType.equals("int") || rightType.equals("dot"))){
            return "boolean";
        }
        else {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,Integer.valueOf(jmmNode.get("line")),
                    Integer.valueOf(jmmNode.get("col")),"Operands of a comparable operation must be int, got " + leftType + " and " + rightType));
        }

        return "";
    }

    private String visitAnd(JmmNode jmmNode, MySymbolTable symbolTable) {
        JmmNode leftNode = jmmNode.getChildren().get(0);
        JmmNode rightNode = jmmNode.getChildren().get(1);

        String leftType = visit(leftNode, symbolTable);
        String rightType = visit(rightNode, symbolTable);

        if ((leftType.equals("boolean") || leftType.equals("dot")) && (rightType.equals("boolean") || rightType.equals("dot"))){
            return "boolean";
        }
        else {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,Integer.valueOf(jmmNode.get("line")),
                    Integer.valueOf(jmmNode.get("col")),"Operands of a logical operation must be boolean, got " + leftType + " and " + rightType));
        }

        return "";
    }

    private String visitAssignment(JmmNode jmmNode, MySymbolTable symbolTable) {
        JmmNode leftNode = jmmNode.getChildren().get(0);
        JmmNode rightNode = jmmNode.getChildren().get(1);

        if (!leftNode.getKind().equals("Id") && !leftNode.getKind().equals("ArrayExp")){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,Integer.valueOf(jmmNode.get("line")),
                    Integer.valueOf(jmmNode.get("col")),"Can only assign to a variable"));
        }

        String leftType = visit(leftNode, symbolTable);
        String rightType = visit(rightNode, symbolTable);

        if (symbolTable.getSuper() != null && ((symbolTable.getSuper().equals(rightType) && symbolTable.getClassName().equals(leftType)) || (symbolTable.getSuper().equals(leftType) && symbolTable.getClassName().equals(rightType)))){
            System.out.println(leftType + " and " + rightType + " can be assigned because one extends the other.");
        }

        if (isImported(leftNode, symbolTable) && isImported(rightNode,symbolTable)){
            System.out.println("Both imported with types " + leftType + " and " + rightType);
        }

        if ((!(leftType.equals(rightType) || leftType.equals("dot") || rightType.equals("dot") || (isImported(leftNode, symbolTable) && isImported(rightNode,symbolTable)))) &&
                !(symbolTable.getSuper() != null && ((symbolTable.getSuper().equals(rightType) && symbolTable.getClassName().equals(leftType)) ||
                        (symbolTable.getSuper().equals(leftType) && symbolTable.getClassName().equals(rightType))))) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,Integer.valueOf(jmmNode.get("line")),
                    Integer.valueOf(jmmNode.get("col")),"Type of the assignee must be compatible with the assigned, got " + leftType + " and " + rightType));
        }

        return "";
    }

    public void checkOpExpression(JmmNode jmmNode, MySymbolTable symbolTable /*, String parentMethodName*/) {
        String visited = visit(jmmNode, symbolTable);
        if (jmmNode.getKind().equals("BinOp")) {
            return;
            // Não precisamos lidar com expressões matemáticas, porque o visitor já irá visitá-las
        }
        /*else if (jmmNode.getKind().equals("Id")){
            Type varType = AstUtils.getVarType(jmmNode.get("value"), jmmNode.getAncestor("MethodDecl").get().getJmmChild(1).get("value"), symbolTable);
            System.out.println("var do tipo: " + varType.getName());
            if (!varType.getName().equals("int")){
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,Integer.valueOf(jmmNode.get("line")),
                        Integer.valueOf(jmmNode.get("col")),"Operand of operation must be of type Int, got: " + varType.getName()));
                return;
            }
            else if (varType.isArray()){
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,Integer.valueOf(jmmNode.get("line")),
                        Integer.valueOf(jmmNode.get("col")),"Array cannot be used in arithmetic operations."));
                return;
            }
        }
        else if (!visited.equals("dot") && !visited.equals("int")){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,Integer.valueOf(jmmNode.get("line")),
                    Integer.valueOf(jmmNode.get("col")),"Operand of operation must be of type Int, got: " + visited));
            return;
        }*/

        else if (visited.equals("int[]")){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,Integer.valueOf(jmmNode.get("line")),
                    Integer.valueOf(jmmNode.get("col")),"Array cannot be used in arithmetic operations."));
            return;
        }
        else if (!visited.equals("dot") && !visited.equals("int")){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,Integer.valueOf(jmmNode.get("line")),
                    Integer.valueOf(jmmNode.get("col")),"Operand of operation must be of type Int, got: " + visited));
            return;
        }
    }

    private String visitOperation(JmmNode jmmNode, MySymbolTable symbolTable) {
        // Operands of an operation must types compatible with the operation (e.g. int + boolean is an error because + expects two integers.)
        JmmNode leftNode = jmmNode.getChildren().get(0);
        JmmNode rightNode = jmmNode.getChildren().get(1);

        checkOpExpression(leftNode, symbolTable);
        checkOpExpression(rightNode, symbolTable);

        return "int";
    }

    private String visitId(JmmNode jmmNode, MySymbolTable symbolTable) {
        // Verify if variable names used in the code have a corresponding declaration, either as a local variable, a method parameter or a field of the class (if applicable)
        var varName = jmmNode.get("value");
        if (jmmNode.getAncestor("MethodDecl").isPresent()){
            var methodType = jmmNode.getAncestor("MethodDecl").get().getJmmChild(0);
            if (methodType.getAttributes().contains("isStatic")){
                if (AstUtils.varIsField(varName, jmmNode.getAncestor("MethodDecl").get().getJmmChild(1).get("value"), symbolTable)){
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.valueOf(jmmNode.get("line")),
                            Integer.valueOf(jmmNode.get("col")), "Fields cannot be accessed from static methods"));
                }
            }
        }

        // Check the declared type => no Type means no declaration
        if (!jmmNode.getAncestor("DotExp").isPresent() && !jmmNode.getAncestor("NewExp").isPresent()){

            Type varType = AstUtils.getVarType(varName, jmmNode.getAncestor("MethodDecl").get().getJmmChild(1).get("value"), symbolTable);
            //System.out.println("var: " + jmmNode.get("value"));
            if (varType == null){
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,Integer.valueOf(jmmNode.get("line")),
                        Integer.valueOf(jmmNode.get("col")),"Variable " + varName + " not declared."));
            }
            else {
                if (varType.isArray())
                    return varType.getName() + "[]";
                else
                    return varType.getName();
            }
        }
        return "";
    }

    private String visitStatement(JmmNode jmmNode, MySymbolTable symbolTable) {
        addVisit("Id", this::visitId);
        return "";
    }

    private String visitReturn(JmmNode jmmNode, MySymbolTable symbolTable) {
        //addVisit("Id", this::visitId);
        String methodName = jmmNode.getAncestor("MethodDecl").get().getJmmChild(1).get("value");
        Type methodReturnType = symbolTable.getReturnType(methodName);
        String actualReturnType = visit(jmmNode.getJmmChild(0),symbolTable);

        //System.out.println("Expected return type: " + methodReturnType + "; Actual return type: " + actualReturnType);

        if (actualReturnType.equals("dot")){
            System.out.println("Return pode ser válido");
        }
        else if (!((methodReturnType.isArray() && actualReturnType.equals("int[]")) ||
                (!methodReturnType.isArray() && methodReturnType.getName().equals(actualReturnType)))){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,Integer.valueOf(jmmNode.get("line")),
                    Integer.valueOf(jmmNode.get("col")),"Incompatible return type, expected " + methodReturnType + " but got " + actualReturnType));
        }

        return "";
    }

    private String visitArray(JmmNode arrayExp, MySymbolTable symbolTable) {
        // Array access is done over an array
        var arrayName = arrayExp.getJmmChild(0).get("value");
        var varType = AstUtils.getVarType(arrayName, arrayExp.getAncestor("MethodDecl").get().getJmmChild(1).get("value"), symbolTable);
        if (varType == null){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,Integer.valueOf(arrayExp.get("line")),
                    Integer.valueOf(arrayExp.get("col")),"Array access cannot be done over a non declared variable."));
        } else if (!varType.isArray()){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,Integer.valueOf(arrayExp.get("line")),
                    Integer.valueOf(arrayExp.get("col")),"Array access cannot be done over a " +
                    varType.getName() + " type."));
        }

        // Array access index is an expression of type integer
        var arrayIndex = arrayExp.getJmmChild(1);

        String indexType = visit(arrayIndex, symbolTable);

        if (indexType == null){
            System.out.println("null");
        }
        else
            System.out.println(indexType);

        if (!(indexType.equals("int") || indexType.equals("dot"))){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,Integer.valueOf(arrayExp.get("line")),
                    Integer.valueOf(arrayExp.get("col")),"Array access index should be of type int, got " +
                    indexType + " instead."));
        }

        return varType.getName();
    }

    public List<Report> getReports(){
        return reports;
    }
}
