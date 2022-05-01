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
}
