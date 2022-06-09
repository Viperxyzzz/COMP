package pt.up.fe.comp.ollir;

import org.eclipse.jgit.patch.HunkHeader;
import org.specs.comp.ollir.Ollir;
import pt.up.fe.comp.AstUtils;
import pt.up.fe.comp.MySymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.HashMap;
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
    private HashMap<String, String> temporaryTypeHashMap;
    public OllirGenerator(SymbolTable mySymbolTable){
        this.code = new StringBuilder();
        this.mySymbolTable = mySymbolTable;
        this.currentMethodname = "";
        this.temporaryTypeHashMap = new HashMap<String, String>();

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
        addVisit("ArrayExp", this::indexingArrayVisit);
        addVisit("FalseId",this::falseIdVisit);
        addVisit("TrueId",this::trueIdVisit);
        addVisit("SmallerThan",this::smallerThanVisit);
        addVisit("AndExp",this::andExpVisit);
        addVisit("IfStatement",this::ifStatementVisit);
        addVisit("ElseStatement",this::elseStatementVisit);
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
            code.append(OllirUtils.getCode(field));
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
            type = "i32";
        }
        else{
            type = dummy;
        }

        thisCode.prefix += temp+"." + type + " :=."+ type + " " + lhs.code +"."+ type + " " + OllirUtils.assignOp(op) + "." + type + " " + rhs.code +"."+ type + ";\n";
        thisCode.code = temp;

        this.temporaryTypeHashMap.put(temp,type);

        return thisCode;
    }

    private Code idVisit(JmmNode id, String dummy){
        Code thisCode = new Code();
        String temp = OllirUtils.createTemp();
        thisCode.code = varToParam(id.get("value"));
        if(isField(id.get("value"),this.currentMethodname, (MySymbolTable) mySymbolTable)){
            String type = OllirUtils.getOllirType(AstUtils.getVarType(id.get("value"),this.currentMethodname,(MySymbolTable) mySymbolTable).getName());
            thisCode.prefix += temp + "." + type + " :=." + type + " getfield(this, " + id.get("value") + "." + type + ")." + type + ";\n"; //FIXME -> must be class name
            thisCode.code = temp;
            this.temporaryTypeHashMap.put(temp,type);
        }
        else
            thisCode.code = id.get("value");

        return thisCode;
    }

    private Code idIntVisit(JmmNode id, String dummy){
        Code thisCode = new Code();
        String temp = OllirUtils.createTemp();

        if(isField(id.get("value"),this.currentMethodname, (MySymbolTable) mySymbolTable)){
            String type = OllirUtils.getOllirType(AstUtils.getVarType(id.get("value"),this.currentMethodname,(MySymbolTable) mySymbolTable).getName());
            thisCode.prefix += temp + "." + type + " :=." + type + " getfield(this, " + id.get("value") + "." + type + ")." + type + ";\n"; //FIXME -> must be class name
            thisCode.code = temp;
        }
        else
            thisCode.code = id.get("value");
        return thisCode;
    }

    private Type getFieldType(String varName){
        var fields = mySymbolTable.getFields();
        for (var field : fields){
            if (field.getName().equals(varName)){
                return field.getType();
            }
        }
        return null;
    }

    private Code getPutField(JmmNode node,String type, Code lhs){
        var rhs = visit(node.getJmmChild(1),type);
        Code thisCode = new Code();
        thisCode.prefix = lhs.prefix;
        thisCode.prefix += rhs.prefix;
        System.out.println("RHS PREFIX " + rhs.prefix);
        System.out.println("RHS CODE " + rhs.code);
        String temp = OllirUtils.createTemp();
        thisCode.prefix += "putfield(this," + lhs.code + "." + type + "," + rhs.code + "." + type +").V;\n";
        thisCode.code = temp;

        if(node.getJmmChild(1).getKind().equals("NewExp")){
            thisCode.prefix += "invokespecial(" + lhs.code + "." + type + ",\"<init>\").V;\n";
        }
        this.temporaryTypeHashMap.put(temp,type);
        return thisCode;
    }

    private Code assignmentVisit(JmmNode node, String dummy){
        var lhs = visit(node.getJmmChild(0),dummy);
        String type;

        //only works if localVar name is different from fieldVar name


        if(node.getJmmChild(0).getKind().equals("ArrayExp")){
            type = OllirUtils.getOllirType(AstUtils.getVarType(node.getJmmChild(0).getJmmChild(0).get("value"),this.currentMethodname,(MySymbolTable) mySymbolTable).getName());
        }
        else {
            String varName = node.getJmmChild(0).get("value");
            Type type1 = getFieldType(varName);
            type = OllirUtils.getOllirType(AstUtils.getVarType(varName, node.getAncestor("MethodDecl").get().getJmmChild(1).get("value"), (MySymbolTable) mySymbolTable).getName());
            if(type1 != null){
                return getPutField(node,type,lhs);
            }
        }
        var rhs = visit(node.getJmmChild(1),type);
        Code thisCode = new Code();
        thisCode.prefix = lhs.prefix;
        thisCode.prefix += rhs.prefix;
        String temp = OllirUtils.createTemp();
        thisCode.prefix += lhs.code + "." + type  + " :=." + type +  " " + rhs.code +"." + type + ";\n"; //FIXME -> type should be get by some way?
        thisCode.code = temp;

        if(node.getJmmChild(1).getKind().equals("NewExp")){
            thisCode.prefix += "invokespecial(" + lhs.code + "." + type + ",\"<init>\").V;\n";
        }

        this.temporaryTypeHashMap.put(temp,type);

        return thisCode;
    }

    private Code thisIdVisit(JmmNode id, String dummy){
        Code thisCode = new Code();
        thisCode.code = "this";
        return thisCode;
    }

    private Code getLengthOllir(String varName){
        String temp = OllirUtils.createTemp();

        Code thisCode = new Code();
        var returnType= OllirUtils.getOllirType(AstUtils.getVarType(varName,this.currentMethodname,(MySymbolTable) mySymbolTable).getName());

        thisCode.prefix = temp + "." + returnType + " :=." + returnType + " " + "arraylength(" + varName +".array." + returnType + ").i32;\n";
        thisCode.code = temp;

        return thisCode;
    }

    private Code dotExpressionVisit(JmmNode node, String dummy){
        String prefixCode = "";
        Code target = visit(node.getJmmChild(0),dummy);
        var lhs = node.getJmmChild(0);
        prefixCode += target.prefix;

        if(node.getJmmChild(1).getKind().equals("LengthExp")){
            return getLengthOllir(target.code);
        }
        String methodName = node.getJmmChild(1).getJmmChild(0).get("value"); //DotExp.CallMethod.id.value
        String finalCode = "";

        if(target.code.equals("this")){
            finalCode = "invokevirtual(" + target.code + ",\"" + methodName +"\"";
        }
        else {
            Type type;
            type = AstUtils.getVarType(lhs.get("value"), this.currentMethodname, (MySymbolTable) mySymbolTable);
            if (type != null) {
                finalCode = "invokevirtual(" + target.code + "." + OllirUtils.getOllirType(type.getName()) + ",\"" + methodName + "\"";

            } else {
                finalCode = "invokestatic(" + target.code + ",\"" + methodName + "\"";
            }
        }
        boolean areThereParams = node.getJmmChild(1).getNumChildren() != 1;
        if(areThereParams){
            for(var arg : node.getJmmChild(1).getJmmChild(1).getChildren()){
                Code argCode = visit(arg,dummy);

                prefixCode += argCode.prefix;
                var returnType = AstUtils.getVarType(argCode.code,this.currentMethodname,(MySymbolTable) mySymbolTable);
                var returnTypeString = "";
               if (returnType != null)
                        returnTypeString = "." + OllirUtils.getCode(returnType);
               else {
                    var paramType = this.temporaryTypeHashMap.get(argCode.code);

                   if(paramType == null)
                       returnTypeString = ".V";
                   else
                       returnTypeString = "." + paramType;
               }
                finalCode += "," + argCode.code + returnTypeString;
            }
        }
        var returnType = mySymbolTable.getReturnType(methodName);
        var returnTypeString = "";
        if(dummy != null){
            returnTypeString = dummy;
        }
        else {
            if (returnType != null)
                returnTypeString = OllirUtils.getCode(returnType);
            else
                returnTypeString = "V";
        }
        finalCode += ")." + returnTypeString + ";\n";
        String temp = OllirUtils.createTemp();
        prefixCode += temp + "." + returnTypeString + " :=." + returnTypeString + " " + finalCode;
        Code thisCode = new Code();
        thisCode.code = temp;
        thisCode.prefix = prefixCode;
        this.temporaryTypeHashMap.put(temp,returnTypeString);
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
        String className = id.get("value");
        String temp = OllirUtils.createTemp();
        Code thisCode = new Code();
        thisCode.prefix = temp + "." + className + " :=." + className + " new(" + className + ")." + className + ";\n";
        thisCode.code = temp;
        this.temporaryTypeHashMap.put(temp,className);
        return thisCode;
    }

    private Code indexingArrayVisit(JmmNode jmmNode, String dummy){
        //b[t.i32].i32
        var type = OllirUtils.getOllirType(AstUtils.getVarType(jmmNode.getJmmChild(0).get("value"),this.currentMethodname,(MySymbolTable) mySymbolTable).getName());
        Code thisCode = new Code();
        var lhs = visit(jmmNode.getJmmChild(0),type);
        var rhs = visit(jmmNode.getJmmChild(1),type);
        thisCode.prefix = lhs.prefix;
        if(jmmNode.getJmmChild(1).getKind().equals("IdInt")){
            String temp2 = OllirUtils.createTemp();
            thisCode.prefix += temp2 + "." + type + " :=." + type + " " + rhs.code + "." + type + ";\n";
            rhs.code = temp2;
            this.temporaryTypeHashMap.put(temp2,type);
        }

        thisCode.prefix += rhs.prefix;
        String temp = OllirUtils.createTemp();
        thisCode.prefix += temp +"."+ type + " :=." + type + " " + lhs.code + "[" + rhs.code + "." + type + "]." + type + ";\n";
        thisCode.code = temp;
        this.temporaryTypeHashMap.put(temp,type);
        return thisCode;
    }
    private Code falseIdVisit(JmmNode node, String dummy){
        Code thisCode = new Code();
        thisCode.code = "0";
        return thisCode;

    }

    private Code trueIdVisit(JmmNode node, String dummy){
        Code thisCode = new Code();
        thisCode.code = "1";
        return thisCode;
    }

    private Code smallerThanVisit(JmmNode node, String dummy){
        var lhs = visit(node.getJmmChild(0),dummy);
        var rhs = visit(node.getJmmChild(1),dummy);

        Code thisCode = new Code();
        thisCode.prefix = lhs.prefix;
        thisCode.prefix += rhs.prefix;
        String temp = OllirUtils.createTemp();
        var type = "";

        if(dummy == null){
            type = "bool";
        }
        else{
            type = dummy;
        }

        thisCode.prefix += temp+"." + type + " :=."+ type + " " + lhs.code +"."+ type + " " + "<" + "." + type + " " + rhs.code +"."+ type + ";\n";
        thisCode.code = temp;

        this.temporaryTypeHashMap.put(temp,type);

        return thisCode;
    }

    private Code andExpVisit(JmmNode node, String dummy){
        var lhs = visit(node.getJmmChild(0),dummy);
        var rhs = visit(node.getJmmChild(1),dummy);

        Code thisCode = new Code();
        thisCode.prefix = lhs.prefix;
        thisCode.prefix += rhs.prefix;
        String temp = OllirUtils.createTemp();
        var type = "";

        if(dummy == null){
            type = "bool";
        }
        else{
            type = dummy;
        }

        thisCode.prefix += temp+"." + type + " :=."+ type + " " + lhs.code +"."+ type + " " + "&" + "." + type + " " + rhs.code +"."+ type + ";\n";
        thisCode.code = temp;

        this.temporaryTypeHashMap.put(temp,type);

        return thisCode;
    }

    private boolean isField(String varName, String methodName, MySymbolTable symbolTable){
        var localVars = symbolTable.getLocalVariables(methodName);
        for(var localVar : localVars){
            if(localVar.getName().equals(varName)){
                return false;
            }
        }
        var methodParams = symbolTable.getParameters(methodName);
        for(var param: methodParams){
            if(param.getName().equals(varName)){
                return false;
            }
        }
        var fields = symbolTable.getFields();
        for (var field : fields){
            if (field.getName().equals(varName)){
                //System.out.println(varName + " está nos fields\n");
                return true;
            }
        }
        return false;
    }

    private boolean isParameter(String varName, String methodName, MySymbolTable symbolTable){
        var localVars = symbolTable.getLocalVariables(methodName);
        for(var localVar : localVars){
            if(localVar.getName().equals(varName)){
                return false;
            }
        }
        var methodParams = symbolTable.getParameters(methodName);
        for(var param: methodParams){
            if(param.getName().equals(varName)){
                return true;
            }
        }
        return false;
    }

    //assumes we pass a valid parameter
    private int getParamPosition(String var,String methodName){
        var methodParams = mySymbolTable.getParameters(methodName);
        for(int i = 0; i < methodParams.size(); i++){
            if(methodParams.get(i).getName().equals(var)){
                return i;
            }
        }
        //this won't happen, inshallah
        return -1;
    }

    private String varToParam(String var){
        if(!isParameter(var,this.currentMethodname,(MySymbolTable) mySymbolTable)){
            return var;
        }
        int index = getParamPosition(var,this.currentMethodname) + 1;
        return "$" + index + "." + var;

    }

    private Code ifStatementVisit(JmmNode node, String dummy){
        var lhs = visit(node.getJmmChild(0),dummy);
        code.append(lhs.prefix);
        code.append("if (" + lhs.code + "." + this.temporaryTypeHashMap.get(lhs.code) + ") goto else;\n");
        var nodeList = node.getChildren();
        for(int i = 1; i < node.getNumChildren(); i++){
            var nodeCode = visit(nodeList.get(i));
            code.append(nodeCode.prefix);
        }
        Code thisCode = new Code();
        thisCode.prefix = "";


        return thisCode;
    }
    private Code elseStatementVisit(JmmNode node, String dummy){
        code.append("goto endif;\n"); //this is really dumb but makes some sense
        code.append("else: \n");
        for(var jmmNode : node.getChildren()){
            var nodeCode = visit(jmmNode);
            code.append(nodeCode.prefix);
        }
        code.append("endif: \n");

        Code thisCode = new Code();
        thisCode.prefix = "";

        return thisCode;
    }

}
