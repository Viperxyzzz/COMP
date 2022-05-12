package pt.up.fe.comp;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.Type;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import java.util.Locale;
import java.util.stream.Collectors;

import static org.specs.comp.ollir.AccessModifiers.DEFAULT;

public class OllirToJasmin {

    private final ClassUnit classUnit;
    private final FunctionClassMap<Instruction, String> instructionMap;

    public OllirToJasmin (ClassUnit classUnit){
        this.classUnit = classUnit;

        instructionMap = new FunctionClassMap<>();
        instructionMap.put(CallInstruction.class, this::getCode);
    }

    public String getFullyQualifiedName(String className){
        for (var importString : classUnit.getImports()){
            var splittedImports = importString.split("\\.");

            String lastName;

            if (splittedImports.length == 0){
                lastName = importString;
            }
            else{
                lastName = splittedImports[splittedImports.length-1];
            }

            if (lastName.equals(className)){
                return importString.replace('.', '/');
            }
        }
        throw new RuntimeException("Could not find import for class " + className);
    }

    public String getCode(){
        var code = new StringBuilder();

        code.append(".class public ").append(classUnit.getClassName()).append("\n");

        var superQualifiedName = getFullyQualifiedName(classUnit.getSuperClass());
        code.append(".super ").append(superQualifiedName).append("\n");

        code.append(SpecsIo.getResource("pt/up/fe/comp/jasminContructor.template").replace("${SUPER_NAME}",
                superQualifiedName)).append("\n");

        for (var method : classUnit.getMethods()){
            code.append(getCode(method));
        }

        return code.toString();
    }

    public String getCode(Method method){
        var code = new StringBuilder();

        code.append(".method public "); //hard coded

        /* //se AccessModifier for DEFAULT não mostrar nada - 3:43:21
        if (method.getMethodAccessModifier() == DEFAULT){
            code.append(".method ");
        }
        else{
            code.append(".method ").append(method.getMethodAccessModifier().name().toLowerCase()).append(" ");
        }*/

        if (method.isStaticMethod()){
            code.append("static ");
        }

        code.append(method.getMethodName()).append("(");

        var methodParamTypes = method.getParams().stream()
                .map(element -> getJasminType(element.getType()))
                .collect(Collectors.joining());

        code.append(methodParamTypes).append(")").append(getJasminType(method.getReturnType())).append("\n");
        code.append(".limit stack 99\n");
        code.append(".limit locals 99\n ");

        for (var inst : method.getInstructions()){
            code.append(getCode(inst));
        }

        code.append("return\n.end method\n\n");

        return code.toString();
    }

    public String getCode(Instruction inst){
        return instructionMap.apply(inst);
    }

    public String getCode(CallInstruction inst){
        switch (inst.getInvocationType()){
            case invokestatic:
                return getCodeInvokeStatic(inst);
            default:
                throw new NotImplementedException(inst.getInvocationType());
        }
    }

    private String getCodeInvokeStatic(CallInstruction inst){
        var code = new StringBuilder();

        code.append("invokestatic ");

        var methodClass = ((Operand) inst.getFirstArg()).getName();
        code.append(getFullyQualifiedName(methodClass));
        code.append("/");

        // rever esta parte
        var calledMethod = ((LiteralElement) inst.getSecondArg()).getLiteral();
        //code.append(calledMethod.substring(1, calledMethod.length()-1));
        code.append(calledMethod);

        code.append("(");

        for(var operand : inst.getListOfOperands()){
            getArgumentCode(operand);
        }

        code.append(")");
        code.append(getJasminType(inst.getReturnType()));
        code.append("\n");

        return code.toString();
    }

    private void getArgumentCode(Element operand){
        throw new NotImplementedException(this);
    }

    public String getJasminType(Type type){
        if (type instanceof ArrayType){
            return "[" + getJasminType(((ArrayType) type).getTypeOfElements());
        }

        return getJasminType(type.getTypeOfElement());
    }

    public String getJasminType(ElementType type){
        switch (type){
            case STRING:
                return "Ljava/lang/String;";
            case VOID:
                return "V";
            default:
                throw new NotImplementedException(type);
        }
    }
}
