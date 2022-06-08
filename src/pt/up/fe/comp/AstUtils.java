package pt.up.fe.comp;

import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.specs.util.SpecsCheck;

public class AstUtils {

    public static Type buildType(JmmNode type1){
        SpecsCheck.checkArgument(type1.getKind().equals("Type"),
                () -> "Expected node Type but got '" + type1.getKind() + "'");
        var typeName = type1.get("value");
        var isArray1= type1.getOptional("isArray").map(isArray -> Boolean.valueOf(isArray)).orElse(false);

        return new Type(typeName, isArray1);
    }

    public static Type getVarType(String varName, String methodName, MySymbolTable symbolTable){
        var localVars = symbolTable.getLocalVariables(methodName);
        for (var localVar: localVars){
            if (localVar.getName().equals(varName)){
                //System.out.println(varName + " está nas localVars\n");
                return localVar.getType();
            }
        }
        var methodParams = symbolTable.getParameters(methodName);
        for (var param: methodParams){
            if (param.getName().equals(varName)){
                //System.out.println(varName + " está nos params\n");
                return param.getType();
            }
        }
        var fields = symbolTable.getFields();
        for (var field : fields){
            if (field.getName().equals(varName)){
                //System.out.println(varName + " está nos fields\n");
                return field.getType();
            }
        }

        return null;
    }

    public static Boolean varIsField(String varName, String methodName, MySymbolTable symbolTable){
        var localVars = symbolTable.getLocalVariables(methodName);
        for (var localVar: localVars){
            if (localVar.getName().equals(varName)){
                //System.out.println(varName + " está nas localVars\n");
                return false;
            }
        }
        var methodParams = symbolTable.getParameters(methodName);
        for (var param: methodParams){
            if (param.getName().equals(varName)){
                //System.out.println(varName + " está nos params\n");
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
}
