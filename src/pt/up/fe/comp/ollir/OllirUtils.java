package pt.up.fe.comp.ollir;

import pt.up.fe.comp.AstUtils;
import pt.up.fe.comp.MySymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

public class OllirUtils {
    private static int tempId;
    public static String getCode(Symbol symbol){
        return symbol.getName() + "." + getCode(symbol.getType());
    }
    public static String getCode(Type type){
        StringBuilder code = new StringBuilder();
        if(type.isArray()){
            code.append("array.");
        }
        code.append(getOllirType(type.getName()));
        return code.toString();
    }

    public static String getType(JmmNode jmmNode, String currentMethodname, SymbolTable mySymbolTable){
        return OllirUtils.getOllirType(AstUtils.getVarType(jmmNode.getJmmChild(0).get("value"),currentMethodname,(MySymbolTable) mySymbolTable).getName());
    }

    public static String getOllirType(String jmmType){
        switch(jmmType){
            case "void":
                return "V";
            case "int":
                return "i32";
            case "boolean":
                return "bool";
            default:
                return jmmType;
        }
    }
    public static String assignOp(String op){
        switch(op){
            case "add":
                return "+";
            case "mult":
                return "*";
            case "div":
                return "/";
            case "sub":
                return "-";
            default:
                return "XD?";
        }
    }
    public static String createTemp(){
        String temp = "temp" + Integer.toString(tempId);
        tempId += 1;
        return temp;
    }
    public static String getConstructor(String methodName){
        String constructor = "\t.construct "+methodName+"().V {\n" +
                "\t\tinvokespecial(this, \"<init>\").V;\n" +
                "\t}";
        return constructor;
    }
}
