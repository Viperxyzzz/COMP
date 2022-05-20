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
    private Method currentMethod;

    public OllirToJasmin (ClassUnit classUnit){
        this.classUnit = classUnit;
        this.currentMethod = null;

        instructionMap = new FunctionClassMap<>();
        instructionMap.put(CallInstruction.class, this::getCode);
        instructionMap.put(AssignInstruction.class, this::getCode);
        instructionMap.put(ReturnInstruction.class, this::getCode);
        instructionMap.put(SingleOpInstruction.class, this::getCode);
        instructionMap.put(BinaryOpInstruction.class, this::getCode);
        instructionMap.put(PutFieldInstruction.class, this::getCode);
        instructionMap.put(GetFieldInstruction.class, this::getCode);
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

        System.out.println("STRING BUGGY: " + className);

        return "java/lang/Object";
    }

    private String getConstructor(String superName){

        return ".method public <init>()V\n" +
                "  aload_0\n" +
                "  invokenonvirtual "+ superName + "/<init>()V\n" +
                "  return\n" +
                ".end method";
    }

    public String getCode(){
        var code = new StringBuilder();

        code.append(".class public ").append(classUnit.getClassName()).append("\n");

        var superQualifiedName = getFullyQualifiedName(classUnit.getSuperClass());
        code.append(".super ").append(superQualifiedName).append("\n");



        for (var field : classUnit.getFields()) {
            code.append(getFieldCode(field));
        }

        code.append(getConstructor(superQualifiedName)).append("\n");

        for (var method : classUnit.getMethods()){
            code.append(getCode(method));
        }

        return code.toString();
    }

    public String getFieldCode(Field field){
        var code = new StringBuilder();

        code.append(".field ");

        if (field.getFieldAccessModifier() == AccessModifiers.PUBLIC) {
            code.append("public ");
        }
        else if (field.getFieldAccessModifier() == AccessModifiers.PRIVATE) {
            code.append("private ");
        }
        else if (field.getFieldAccessModifier() == AccessModifiers.PROTECTED) {
            code.append("protected ");
        }

        if (field.isStaticField())
            code.append("static ");
        if (field.isFinalField())
            code.append("final ");

        code.append(field.getFieldName() + " ");

        code.append(getJasminType(field.getFieldType()) + "\n");

        return code.toString();
    }

    public String getCode(Method method){

        if (method.isConstructMethod()){
            return "";
        }

        var code = new StringBuilder();
        this.currentMethod = method;

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
        code.append(".limit locals 99\n");

        //registodenomedavar = method.getVarTable().get("nome da var").getVirtualReg();

        for (var inst : method.getInstructions()){
            code.append(getCode(inst));
        }

        code.append(".end method\n\n");

        return code.toString();
    }

    public String getCode(Instruction inst){
        return instructionMap.apply(inst);
    }

    public String getCode(GetFieldInstruction inst){
        var code = new StringBuilder();

        var firstElement = inst.getFirstOperand();
        var secondElement = inst.getSecondOperand();

        code.append(generateLoadInstruction(firstElement));

        var className = getJasminType(firstElement.getType());

        String fieldName = ((Operand) secondElement).getName();

        code.append("getfield ").append(className).append("/").append(fieldName).append(" ")
                .append(getJasminType(secondElement.getType())).append("\n");

        return code.toString();
    }

    public String getCode(PutFieldInstruction inst){
        var code = new StringBuilder();

        var firstElement = inst.getFirstOperand();
        var secondElement = inst.getSecondOperand();
        var thirdElement = inst.getThirdOperand();

        code.append(generateLoadInstruction(firstElement));
        code.append(generateLoadInstruction(thirdElement));

        var className = getJasminType(firstElement.getType());

        String fieldName = ((Operand) secondElement).getName();

        code.append("putfield ").append(className).append("/").append(fieldName).append(" ")
                .append(getJasminType(secondElement.getType())).append("\n");

        return code.toString();
    }

    public String getCode(SingleOpInstruction inst) {
        var code = new StringBuilder();

        code.append(generateLoadInstruction(inst.getSingleOperand()));

        inst.getSingleOperand().getType();

        return code.toString();
    }

    public String getCode(BinaryOpInstruction inst){
        var code = new StringBuilder();

        code.append(generateLoadInstruction(inst.getLeftOperand()));
        code.append(generateLoadInstruction(inst.getRightOperand()));

        switch (inst.getLeftOperand().getType().getTypeOfElement()){
            case INT32:
                code.append("i");
                break;
            default:
                throw new RuntimeException("BinaryOpInstruction Type not implemented");
        }

        code.append(inst.getOperation().getOpType().toString().toLowerCase()).append("\n");

        return code.toString();
    }

    public String getCode(AssignInstruction inst){
        var code = new StringBuilder();

        int lhsCurrent = currentMethod.getVarTable().get(((Operand)inst.getDest()).getName()).getVirtualReg();

        code.append(instructionMap.apply(inst.getRhs()));

        switch (inst.getTypeOfAssign().getTypeOfElement()) {
            case INT32:
            case BOOLEAN:
                code.append("istore " + lhsCurrent + "\n");
                break;
            case OBJECTREF:
                code.append("astore " + lhsCurrent + "\n");
                break;
            case VOID:
                return code.toString();
            default:
                throw new NotImplementedException("Assign Type not implemented" + inst.getTypeOfAssign().getTypeOfElement());
        }


        return code.toString();
    }

    public String generateLoadInstruction(Element element){
        var code = new StringBuilder();

        if (element.isLiteral()) {
            String value = ((LiteralElement) element).getLiteral();
            code.append("ldc " + value + "\n");
        }

        else {
            int currentLocation = currentMethod.getVarTable().get(((Operand) element).getName()).getVirtualReg();

            switch (element.getType().getTypeOfElement()) {
                case INT32:
                case BOOLEAN:
                    code.append("iload " + currentLocation + "\n");
                    break;
                case THIS:
                    code.append("aload_0" + "\n");
                    break;
                case OBJECTREF:
                    code.append("aload " + currentLocation + "\n");
                    break;
                case VOID:
                    return code.toString();
                case ARRAYREF:
                    System.out.println("CURRENTLOCATION + " + currentLocation);
                    code.append("aload " + currentLocation +"\n");
                    break;
                default:
                    throw new NotImplementedException("Load Type not implemented" + element.getType().getTypeOfElement());
            }
        }

        return code.toString();
    }

    public String getCode(ReturnInstruction inst){
        var code = new StringBuilder();
        var element = inst.getOperand();

        if (inst.hasReturnValue()) {
            code.append(generateLoadInstruction(element));
            switch (element.getType().getTypeOfElement()) {
                case INT32:
                case BOOLEAN:
                    code.append("ireturn \n");
                    break;
                default:
                    throw new NotImplementedException("Return Type not implemented");
            }
        }

        else { code.append("return\n"); }

        return code.toString();
    }


    public String getCode(CallInstruction inst){
        switch (inst.getInvocationType()){
            case invokestatic:
                return getCodeInvokeStatic(inst);
            case invokespecial:
                return getCodeInvokeSpecial(inst);
            case invokevirtual:
                return getCodeInvokeVirtual(inst);
            case NEW:
                return getCodeNew(inst);
            case arraylength:
                return getArrayLength(inst);
            default:
                throw new NotImplementedException(inst.getInvocationType());
        }
    }

    private String getCodeInvokeVirtual(CallInstruction inst){
        var code = new StringBuilder();

        code.append(generateLoadInstruction(inst.getFirstArg()));

        for (Element e : inst.getListOfOperands())
            code.append(generateLoadInstruction(e));

        var className = ((ClassType)inst.getFirstArg().getType()).getName();
        var methodCall = ((LiteralElement)inst.getSecondArg()).getLiteral().replace("\"", "");

        code.append("invokevirtual ")
                .append(className)
                .append("/")
                .append(methodCall)
                .append("(");

        for (Element e : inst.getListOfOperands())
            code.append(getJasminType(e.getType()));


        code.append(")")
                .append(getJasminType(inst.getReturnType())).append("\n");

        return code.toString();
    }

    private String getCodeNew(CallInstruction inst){
        var code = new StringBuilder();

        var className = getJasminType(inst.getReturnType());

        code.append("new " + className + "\n");

        return code.toString();
    }

    private String getArrayLength(CallInstruction inst){
        var code = new StringBuilder();
        Element element = inst.getFirstArg();
        int location = currentMethod.getVarTable().get(((Operand) element).getName()).getVirtualReg();
        code.append("aload " + location + "\n");
        return code.toString();

    }

    private String getCodeInvokeStatic(CallInstruction inst){
        var code = new StringBuilder();

        for (Element e : inst.getListOfOperands())
            code.append(generateLoadInstruction(e));

        code.append("invokestatic ");

        var methodClass = ((Operand) inst.getFirstArg()).getName();
        code.append(getFullyQualifiedName(methodClass));
        code.append("/");

        var calledMethod = ((LiteralElement) inst.getSecondArg()).getLiteral();
        code.append(calledMethod.substring(1, calledMethod.length() - 1));

        code.append("(");

        for (Element e : inst.getListOfOperands())
            code.append(getJasminType(e.getType()));

        code.append(")");
        code.append(getJasminType(inst.getReturnType()));
        code.append("\n");

        return code.toString();
    }

    private String getCodeInvokeSpecial(CallInstruction inst) {
        var code = new StringBuilder();

        code.append(generateLoadInstruction(inst.getFirstArg()));


        code.append("invokespecial ").append(inst.getFirstArg().getType()
                .getTypeOfElement() == ElementType.THIS
                        ? classUnit.getSuperClass() : ((ClassType) inst.getFirstArg().getType()).getName())
                .append("/<init>(");

        for(var operand : inst.getListOfOperands()){
            getArgumentCode(operand);
        }

        code.append(")");
        code.append(getJasminType(inst.getReturnType()));
        code.append("\n");

        return code.toString();
    }

    private String getArgumentCode(Element operand){
        var code = new StringBuilder();

        code.append(getJasminType(operand.getType()));

        return code.toString();
    }

    public String getJasminType(Type type){
        if (type instanceof ArrayType){
            return "[" + getJasminType(((ArrayType) type).getTypeOfElements());
        }
        switch (type.getTypeOfElement()){
            case THIS:
                return classUnit.getClassName();
            case OBJECTREF:
                return ((ClassType) type).getName();
            default:
                getJasminType(type.getTypeOfElement());
                break;
        }

        return getJasminType(type.getTypeOfElement());
    }

    public String getJasminType(ElementType type){
        switch (type){
            case STRING:
                return "Ljava/lang/String;";
            case INT32:
                return "I";
            case BOOLEAN:
                return "Z";
            case VOID:
                return "V";
            default:
                throw new NotImplementedException(type);
        }
    }
}
