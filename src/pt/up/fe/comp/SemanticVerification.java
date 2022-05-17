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

        addVisit("ReturnExp", this::visitReturn);
        addVisit("InitStatement", this::visitStatement);
        addVisit("ArrayExp", this::visitArray);
        addVisit("BinOp", this::visitOperation);
        addVisit("Assignment", this::visitAssignment);
        addVisit("IdInt", this::visitInt);
        addVisit("TrueId", this::visitBoolean);
        addVisit("FalseId", this::visitBoolean);
        addVisit("AndExp", this::visitAnd);
        addVisit("SmallerThan", this::visitSmaller);
        addVisit("NewExp", this::visitNew);
        addVisit("NewArray", this::visitNewArray);
        addVisit("Not", this::visitNot);
        addVisit("WhileStatement", this::visitWhile);
        addVisit("IfStatement", this::visitIf);
        addVisit("DotExp", this::visitDot);
        //addVisit("Id", this::visitId);
    }

    public boolean checkDot(JmmNode jmmNode, MySymbolTable symbolTable) {
        if (jmmNode.get("value").equals(symbolTable.getClassName())){
            return true;
        }

        var imports = symbolTable.getImports();
        if(imports.contains(jmmNode.get("value"))){
            return true;
        }

        Type varType = AstUtils.getVarType(jmmNode.get("value"), jmmNode.getAncestor("MethodDecl").get().getJmmChild(1).get("value"), symbolTable);
        if(!(varType == null)){
            return true;
        }

        return false;
    }

    private String visitDot(JmmNode jmmNode, MySymbolTable symbolTable) {
        JmmNode id = jmmNode.getJmmChild(0);
        JmmNode call = jmmNode.getJmmChild(1);

        if (!checkDot(id, symbolTable)){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.valueOf(jmmNode.get("line")),
                    Integer.valueOf(jmmNode.get("col")), "Class " + id.get("value") + " not imported."));
        }

        if (call.getKind().equals("LengthExp")){
            return "int";
        }
        else
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

        if (!(leftType.equals(rightType) || leftType.equals("dot") || rightType.equals("dot"))) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,Integer.valueOf(jmmNode.get("line")),
                    Integer.valueOf(jmmNode.get("col")),"Type of the assignee must be compatible with the assigned, got " + leftType + " and " + rightType));
        }

        return "";
    }

    public void checkOpExpression(JmmNode jmmNode, MySymbolTable symbolTable /*, String parentMethodName*/) {

        if (jmmNode.getKind().equals("BinOp")) {
            return;
            // Não precisamos lidar com expressões matemáticas, porque o visitor já irá visitá-las
        }
        else if (jmmNode.getKind().equals("Id")){
            Type varType = AstUtils.getVarType(jmmNode.get("value"), jmmNode.getAncestor("MethodDecl").get().getJmmChild(1).get("value"), symbolTable);
            //System.out.println("var do tipo: " + varType.getName());
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
        else if (jmmNode.getKind().equals("dot")){
            return;
        }


        /* else if (node é dot expression){
            // verificar valor de retorno da função chamada
        } else if (node é um array) {
            // Verificar tipo do array.
        }*/
        // AND, SMALLER, NOTEXP, DOTEXP
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

        // Check the declared type => no Type means no declaration
        if (!jmmNode.getAncestor("DotExp").isPresent() && !jmmNode.getAncestor("NewExp").isPresent()){

            Type varType = AstUtils.getVarType(jmmNode.get("value"), jmmNode.getAncestor("MethodDecl").get().getJmmChild(1).get("value"), symbolTable);
            System.out.println("var: " + jmmNode.get("value"));
            if (varType == null){
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,Integer.valueOf(jmmNode.get("line")),
                        Integer.valueOf(jmmNode.get("col")),"Variable " + jmmNode.get("value") + " not declared."));
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
        if (!(indexType.equals("int") || indexType.equals("dot"))){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,Integer.valueOf(arrayExp.get("line")),
                    Integer.valueOf(arrayExp.get("col")),"Array access index should be of type int, got " +
                    arrayExp.getJmmChild(1).getKind() + " instead."));
        }

        return varType.getName();
    }

    public List<Report> getReports(){
        return reports;
    }
}
