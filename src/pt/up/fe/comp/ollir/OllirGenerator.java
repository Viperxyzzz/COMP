package pt.up.fe.comp.ollir;

import org.specs.comp.ollir.Ollir;
import pt.up.fe.comp.MySymbolTable;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.stream.Collectors;

/*
    TODO
    Obter tipos para as variaveis e isso
    a[3];
    fields
 */


public class OllirGenerator extends AJmmVisitor<Integer, Code> {
    private final StringBuilder code;
    private final SymbolTable mySymbolTable;
    public OllirGenerator(SymbolTable mySymbolTable){
        this.code = new StringBuilder();
        this.mySymbolTable = mySymbolTable;

        addVisit("Start",this::programVisit);
        addVisit("ClassDeclaration", this::classDeclVisit);
        addVisit("MethodDecl", this::methodDeclVisit);
        addVisit("InitStatement",this::initStatementVisit);
        addVisit("DotExp",this::dotExpressionVisit);
        addVisit("ParamToPass",this::argumentsVisit);
        addVisit("Id",this::idVisit);
        addVisit("IdInt",this::idIntVisit); //fix this
        addVisit("BinOp",this::visitBinOp);
        addVisit("Assignment",this::assignmentVisit);
    }

    public String getCode(){
        return this.code.toString();
    }


    private Code programVisit(JmmNode program, Integer dummy){
        for (var importString : mySymbolTable.getImports()){
            code.append("import ").append(importString).append(";\n");
        }
        for (var child : program.getChildren()){
            visit(child);
        }
        return null;
    }

    private Code classDeclVisit(JmmNode classDecl, Integer dummy){
        //public HelloWorld extends BoardBase
        code.append("public ").append(mySymbolTable.getClassName());
        var superClass = mySymbolTable.getSuper();
        if(superClass != null){
            code.append(" extends ").append(superClass);
        }
        code.append("{\n");
        code.append(OllirUtils.getConstructor(mySymbolTable.getClassName()));
        code.append("\n");
        for (var child : classDecl.getChildren()){
            visit(child);
        }

        code.append("}\n");
        return null;
    }

    private Code methodDeclVisit(JmmNode methodDecl, Integer dummy){

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

        code.append("main(");
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

    private Code initStatementVisit(JmmNode jmmNode, Integer integer) {
        //2:33:00
        for(var node : jmmNode.getChildren()){
            var nodeCode = visit(node);
            System.out.println("CODE PREFIX" + nodeCode.prefix);

            System.out.println("CODE " + nodeCode.code);
            code.append(nodeCode.prefix);

        }


        return null;
    }

    private Code dotExpVisit(JmmNode dotExp, Integer integer){
        code.append("invokestatic(");
        // TODO : ExprToOLlir -> codeBefore, value
        visit(dotExp.getJmmChild(0)); // se tipo for expressão -> object reference se for classe -> estático, metodo para passar expressao generica e retornar tipo
        code.append(", \"");
        visit(dotExp.getJmmChild(1).getJmmChild(0));
        code.append("\"");
        //visit(dotExp.getJmmChild(1).getJmmChild(1)); //TODO : verificar se ParamToPass realmente existe antes de fazer isto
        code.append(")").append(".V"); //TODO : getExprType(memberCall)
        return null;
    }

    private Code argumentsVisit(JmmNode arguments, Integer dummy){
        for (var child : arguments.getChildren()){
            code.append(", ");
            visit(child);
        }
        return null;
    }

    private Code visitBinOp(JmmNode node, Integer dummy){
        var lhs = visit(node.getJmmChild(0));
        var rhs = visit(node.getJmmChild(1));
        String op = node.get("op");

        Code thisCode = new Code();
        thisCode.prefix = lhs.prefix;
        thisCode.prefix += rhs.prefix;
        String temp = OllirUtils.createTemp();
        thisCode.prefix += temp+".i32" + ":=.i32" + lhs.code +".i32" + OllirUtils.assignOp(op) + rhs.code +".i32" + ";\n";
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

    private Code idVisit(JmmNode id, Integer dummy){
        Code thisCode = new Code();
        thisCode.code = id.get("value");
        return thisCode;
    }

    private Code idIntVisit(JmmNode id, Integer dummy){
        Code thisCode = new Code();
        thisCode.code = id.get("value");
        return thisCode;
    }

    private Code assignmentVisit(JmmNode node, Integer dummy){
        var lhs = visit(node.getJmmChild(0));
        var rhs = visit(node.getJmmChild(1));

        Code thisCode = new Code();
        thisCode.prefix = lhs.prefix;
        thisCode.prefix += rhs.prefix;
        String temp = OllirUtils.createTemp();
        thisCode.prefix += lhs.code + ".i32" + ":=.i32 " + rhs.code +".i32" + ";\n"; //FIXME -> type should be get by some way?
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

    private Code dotExpressionVisit(JmmNode node, Integer dummy){
        String prefixCode = "";
        Code target = visit(node.getJmmChild(0));
        prefixCode += target.prefix;
        String methodName = node.getJmmChild(1).getJmmChild(0).get("value"); //DotExp.CallMethod.id.value
        String finalCode = "invokestatic("+target.code+",\""+methodName + "\"";
        boolean areThereParams = node.getJmmChild(1).getNumChildren() != 1;
        if(areThereParams){
            for(var arg : node.getJmmChild(1).getJmmChild(1).getChildren()){
                Code argCode = visit(arg);
                prefixCode += argCode.prefix;
                finalCode += "," + argCode.code;
            }
        }

        //finalCode += ").V" + OllirUtils.getCode(mySymbolTable.getReturnType(methodName));
        finalCode += ").V;\n"; //FIXME .V -> OllirUtils.getCode();
        String temp = OllirUtils.createTemp();
        prefixCode += temp + ":=" + finalCode;
        Code thisCode = new Code();
        thisCode.code = temp;
        thisCode.prefix = prefixCode;
        return thisCode;
    }

}
