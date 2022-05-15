package pt.up.fe.comp.ollir;

import org.specs.comp.ollir.Ollir;
import pt.up.fe.comp.AstUtils;
import pt.up.fe.comp.MySymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.List;
import java.util.stream.Collectors;

/*
    TODO
    Obter tipos para as variaveis e isso
    a[3];
    fields verify if public or not


    REFACTOR
    fieldVisit, mudar o nó para fieldDecl em vez de VarDecl

    Basic Class Structure? Done
    Class Fields? Verify if public or private
    Method Structure? Not sure what this means ask professor
    Assignments? Working maaaas aquela duvida de a = temp1 + temp2 :(
    Arithmetic Operation? Deve estar, tenho que reverificar
    Method Invocation?  Not sure what this means UWU
 */


public class OllirGenerator extends AJmmVisitor<String, Code> {
    private final StringBuilder code;
    private final SymbolTable mySymbolTable;
    private String currentMethodname;
    public OllirGenerator(SymbolTable mySymbolTable){
        this.code = new StringBuilder();
        this.mySymbolTable = mySymbolTable;
        this.currentMethodname = "";

        addVisit("Start",this::programVisit);
        addVisit("ClassDeclaration", this::classDeclVisit);
        //addVisit("VarDecl", this::fieldVisit);
        addVisit("MethodDecl", this::methodDeclVisit);
        addVisit("InitStatement",this::initStatementVisit);
        addVisit("DotExp",this::dotExpressionVisit);
        addVisit("ParamToPass",this::argumentsVisit);
        addVisit("Id",this::idVisit);
        addVisit("IdInt",this::idIntVisit); //fix this
        addVisit("BinOp",this::visitBinOp);
        addVisit("Assignment",this::assignmentVisit);
        addVisit("ReturnExp", this::returnExpVisit);
        addVisit("ThisId",this::thisIdVisit);
        addVisit("NewExp", this::newExpVisit);
    }

    public String getCode(){
        return this.code.toString();
    }


    private Code programVisit(JmmNode program, String dummy){
        for (var importString : mySymbolTable.getImports()){
            code.append("import ").append(importString).append(";\n");
        }
        for (var child : program.getChildren()){
            visit(child);
        }
        return null;
    }

    private Code classDeclVisit(JmmNode classDecl, String dummy){
        //public HelloWorld extends BoardBase
        code.append("public ").append(mySymbolTable.getClassName());
        var superClass = mySymbolTable.getSuper();
        if(superClass != null){
            code.append(" extends ").append(superClass);
        }
        code.append("{\n");
        code.append("\n");
        for (var field : mySymbolTable.getFields()){
            code.append(".field public ");
            code.append(field.getName() + ".");
            code.append(OllirUtils.getOllirType(field.getType().getName()));
            code.append(";\n");
        }
        code.append("\n");
        code.append(OllirUtils.getConstructor(mySymbolTable.getClassName()));
        code.append("\n");
        for (var child : classDecl.getChildren()){
            visit(child);
        }

        code.append("}\n");
        return null;
    }

    private Code methodDeclVisit(JmmNode methodDecl, String dummy){

        var methodSignature = methodDecl.getJmmChild(1).get("value");
        var isStatic = false;
        for(var staticCheck : methodDecl.getJmmChild(0).getAttributes()){
            if(staticCheck.equals("isStatic")){
                isStatic = true;
            }
        }

        code.append(".method public ");
        if(isStatic){
            code.append("static ");
        }
        this.currentMethodname = methodDecl.getJmmChild(1).get("value");
        code.append(methodDecl.getJmmChild(1).get("value") + "(");
        var params = mySymbolTable.getParameters(methodSignature);
        var paramCode = params.stream()
                .map(symbol -> OllirUtils.getCode(symbol))
                .collect(Collectors.joining(", "));
        code.append(paramCode);
        code.append(").");
        code.append(OllirUtils.getCode(mySymbolTable.getReturnType(methodSignature)));
        code.append(" {\n");
        int lastParamIndex = -1;
        for(int i = 0; i < methodDecl.getNumChildren();i++){
            if(methodDecl.getJmmChild(i).getKind().equals("Param")){
                lastParamIndex = i;
            }
        }
        var stmts = methodDecl.getChildren().subList(lastParamIndex+2, methodDecl.getNumChildren());
        System.out.println(stmts);
        for (var stmt: stmts){
            visit(stmt);
        }
        code.append("}\n");

        return null;
    }

    private Code initStatementVisit(JmmNode jmmNode, String integer) {
        //2:33:00
        for(var node : jmmNode.getChildren()){
            var nodeCode = visit(node);
            System.out.println(nodeCode.prefix);
            code.append(nodeCode.prefix);

        }


        return null;
    }

    private Code argumentsVisit(JmmNode arguments, String dummy){
        for (var child : arguments.getChildren()){
            code.append(", ");
            visit(child);
        }
        return null;
    }

    private Code visitBinOp(JmmNode node, String dummy){
        var lhs = visit(node.getJmmChild(0),dummy);
        var rhs = visit(node.getJmmChild(1),dummy);
        String op = node.get("op");

        Code thisCode = new Code();
        thisCode.prefix = lhs.prefix;
        thisCode.prefix += rhs.prefix;
        String temp = OllirUtils.createTemp();
        var type = "";
        if(dummy == null){
            type = "V";
        }
        else{
            type = dummy;
        }
        System.out.println("TYPE " + type);
        thisCode.prefix += temp+"." + type + " :=."+ type + " " + lhs.code +"."+ type + " " + OllirUtils.assignOp(op) + "." + type + " " + rhs.code +"."+ type + ";\n";
        thisCode.code = temp;
        return thisCode;
    }
    /*
    private Code visitInvocation(JmmNode node, Integer dummy){
        String prefixCode = "";
        Code target = visit(node.getJmmChild(0));
        prefixCode += target.prefix;

        String methodSignature = node.getAttributes()
        return thiscode;
    }*/

    private Code idVisit(JmmNode id, String dummy){
        Code thisCode = new Code();
        thisCode.code = id.get("value");
        return thisCode;
    }

    private Code idIntVisit(JmmNode id, String dummy){
        Code thisCode = new Code();
        thisCode.code = id.get("value");
        return thisCode;
    }

    private Code assignmentVisit(JmmNode node, String dummy){
        var lhs = visit(node.getJmmChild(0));
        var type = OllirUtils.getOllirType(AstUtils.getVarType(node.getJmmChild(0).get("value"),node.getAncestor("MethodDecl").get().getJmmChild(1).get("value"),(MySymbolTable) mySymbolTable).getName());
        var rhs = visit(node.getJmmChild(1),type);
        Code thisCode = new Code();
        thisCode.prefix = lhs.prefix;
        thisCode.prefix += rhs.prefix;
        String temp = OllirUtils.createTemp();
        thisCode.prefix += lhs.code + "." + type  + " :=." + type +  " " + rhs.code +"." + type + ";\n"; //FIXME -> type should be get by some way?
        thisCode.code = temp;

        return thisCode;
    }
    /*
    private Code dotExpressionVisit(JmmNode node, Integer dummy){
        var lhs = visit(node.getJmmChild(0));
        var rhs = visit(node.getJmmChild(1));

        Code thisCode = new Code();
        thisCode.prefix = lhs.prefix;
        thisCode.prefix += rhs.prefix;
        String temp = OllirUtils.createTemp();
        //this aint working w length
        thisCode.prefix += "invokestatic(" + lhs.code + ", \"" + rhs.code + "\").V";
        thisCode.code = temp;
        return thisCode;
    }*/
    private Code thisIdVisit(JmmNode id, String dummy){
        Code thisCode = new Code();
        thisCode.code = "this";
        return thisCode;
    }

    private Code dotExpressionVisit(JmmNode node, String dummy){
        String prefixCode = "";
        Code target = visit(node.getJmmChild(0));
        var lhs = node.getJmmChild(0);
        prefixCode += target.prefix;
        String methodName = node.getJmmChild(1).getJmmChild(0).get("value"); //DotExp.CallMethod.id.value
        Type type;
        type = AstUtils.getVarType(lhs.get("value"),this.currentMethodname,(MySymbolTable) mySymbolTable);
        String finalCode = "";
        if(type != null){
            finalCode = "invokevirtual(" + target.code + "." + OllirUtils.getOllirType(type.getName()) +",\""+methodName + "\"";
        }
        else{
            finalCode = "invokestatic(" +target.code+",\""+methodName + "\"";
        }

        boolean areThereParams = node.getJmmChild(1).getNumChildren() != 1;
        if(areThereParams){
            for(var arg : node.getJmmChild(1).getJmmChild(1).getChildren()){
                Code argCode = visit(arg);

                prefixCode += argCode.prefix;
                var returnType = AstUtils.getVarType(argCode.code,this.currentMethodname,(MySymbolTable) mySymbolTable);
                var returnTypeString = "";
                if(dummy != null){
                    returnTypeString = dummy;
                }
                else {
                    if (returnType != null)
                        returnTypeString = "." + OllirUtils.getCode(returnType);
                    else
                        returnTypeString = ".V";
                }
                finalCode += "," + argCode.code + returnTypeString;
            }
        }
        var returnType = mySymbolTable.getReturnType(methodName);
        var returnTypeString = "";
        System.out.println(returnType);
        System.out.println(methodName);
        if(dummy != null){
            returnTypeString = dummy;
        }
        else {
            if (returnType != null)
                returnTypeString = OllirUtils.getCode(returnType);
            else
                returnTypeString = "V"; //FIXME .V -> OllirUtils.getCode(); (EDIT1 : Think I fixed it above? XD)
        }
        System.out.println("RETURNTYPESTRING " + returnTypeString);
        finalCode += ")." + returnTypeString + ";\n";
        String temp = OllirUtils.createTemp();
        prefixCode += temp + "." + returnTypeString + " :=." + returnTypeString + " " + finalCode;
        Code thisCode = new Code();
        thisCode.code = temp;
        thisCode.prefix = prefixCode;
        return thisCode;
    }

    private Code returnExpVisit(JmmNode jmmNode, String dummy){
        var returnType = OllirUtils.getCode(mySymbolTable.getReturnType(jmmNode.getAncestor("MethodDecl").get().getJmmChild(1).get("value")));
        var type = visit(jmmNode.getJmmChild(0),returnType);
        code.append(type.prefix);
        code.append("ret." + returnType + " " + type.code + "." + returnType + ";\n");
        return null;
    }

    private Code newExpVisit(JmmNode jmmNode, String dummy){
        var id = jmmNode.getJmmChild(0);
        Code thisCode = new Code();
        thisCode.code = "new(" + id.get("value") +")";
        return thisCode;
    }

}
