package pt.up.fe.comp;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.Type;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class OllirToJasmin {

    private final ClassUnit classUnit;
    private final FunctionClassMap<Instruction, String> instructionMap;
    private Method currentMethod;

    private String className;
    private int conditionals;

    private int comparisons;


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
        instructionMap.put(SingleOpCondInstruction.class, this::getCode);
        instructionMap.put(CondBranchInstruction.class, this::getCode);
        instructionMap.put(GotoInstruction.class, this::getCode);
        instructionMap.put(UnaryOpInstruction.class, this::getCode);
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

        //System.out.println("STRING BUGGY: " + className);

        return "java/lang/Object";
    }

    private String getConstructor(String superName){

        return ".method public <init>()V\n" +
                "  aload_0\n" +
                "  invokespecial "+ superName + "/<init>()V\n" +
                "  return\n" +
                ".end method";
    }

    public String getCode(){
        var code = new StringBuilder();

        code.append(".class public ").append(classUnit.getClassName()).append("\n");

        var superQualifiedName = getFullyQualifiedName(classUnit.getSuperClass());
        code.append(".super ").append(superQualifiedName).append("\n");

        className =classUnit.getClassName();


        for (var field : classUnit.getFields()) {
            code.append(getFieldCode(field));
        }

        code.append(getConstructor(superQualifiedName)).append("\n\n");

        for (Method method : classUnit.getMethods()){
            code.append(getCode(method) + "\n");
            //System.out.println("\nMethod is:" + method.getMethodName() + "\n");
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

        conditionals = 0;
        comparisons = 0;


        if (method.isConstructMethod()){
            return "";
        }

        var code = new StringBuilder();
        this.currentMethod = method;

        code.append(".method public "); //hard coded

        if (method.isStaticMethod()){
            code.append("static ");
        }

        code.append(method.getMethodName()).append("(");

        var methodParamTypes = method.getParams().stream()
                .map(element -> getJasminType(element.getType()))
                .collect(Collectors.joining());

        code.append(methodParamTypes).append(")");
        code.append(getJasminType(method.getReturnType()));
        code.append("\n");


        ArrayList<Integer> locals = new ArrayList<>();
        for (Descriptor d : method.getVarTable().values()) {
            if (!locals.contains(d.getVirtualReg()))
                locals.add(d.getVirtualReg());
        }
        if (!locals.contains(0) && !method.isConstructMethod())
            locals.add(0);

        code.append("\t.limit stack 99\n");
        code.append("\t.limit locals ").append(locals.size()).append("\n\n");


        for (Instruction inst : method.getInstructions()){

            for (var label : method.getLabels().keySet()) {
                if (method.getLabels().get(label) == inst) {
                    code.append(label + ":\n");
                }
            }

            //code.append("entering inst: " + inst.getInstType() + " ");

            //testing
            //code.append("Inst of type: \n" + inst.getInstType().toString() + "\n");

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


        //code.append("singleOpInst load: ");
        code.append(generateLoadInstruction(inst.getSingleOperand()));
        //code.append("\n");

        return code.toString();
    }

    public String getCode(BinaryOpInstruction inst){
        var code = new StringBuilder();



        //code.append("BinaryOpInst op type is: " + inst.getOperation().getOpType().toString() + "\n");

        switch (inst.getOperation().getOpType()) {
            /*case ANDB:
                code.append(generateLoadInstruction(inst.getOperands().get(0)));
                code.append(generateLoadInstruction(inst.getOperands().get(1)));
                code.append("imul\n");

             */
            case ANDB:
                conditionals++;
                //code.append("binaryOpInst andb load left: ");
                code.append(generateLoadInstruction(inst.getLeftOperand()));
                code.append("ifeq FALSE_" + conditionals + "\n");
                //code.append("binaryOpInst andb load right: ");
                code.append(generateLoadInstruction(inst.getRightOperand()));
                code.append("ifeq FALSE_" + conditionals + "\n");
                code.append("iconst_1" + "\n");
                code.append("goto STORE_" + conditionals + "\n");
                code.append("FALSE_" + conditionals + ":\n");
                code.append("iconst_0" + "\n");
                code.append("STORE_" + conditionals + ":\n");
                break;

            case LTH:
                conditionals++;
                //code.append("binaryOpInst lth load left: ");
                code.append(generateLoadInstruction(inst.getLeftOperand()));
                //code.append("binaryOpInst lth load right: ");
                code.append(generateLoadInstruction(inst.getRightOperand()));
                code.append("if_icmplt TRUE_" + conditionals + "\n");
                code.append("iconst_0\n");
                code.append("goto STORE_" + conditionals + "\n");
                code.append("TRUE_" + conditionals + ":\n");
                code.append("iconst_1\n");
                code.append("STORE_" + conditionals + ":\n");
                break;
            case NOTB:
                conditionals++;

                code.append(generateLoadInstruction(inst.getLeftOperand()));
                if (((Operand) inst.getRightOperand()).getName()
                        .equals(((Operand) inst.getLeftOperand()).getName())
                ) {
                    code.append("ifeq");
                }
                else {
                    code.append(generateLoadInstruction(inst.getRightOperand()));
                    code.append(getComparison(inst.getOperation()));
                }
            default:
                //code.append("binaryOpInst default load left: ");
                code.append(generateLoadInstruction(inst.getLeftOperand()));
                code.append(getCodeUnaryOpInstruction(inst));
                return code.toString();
        }

        return code.toString();
    }

    public String getCode(AssignInstruction inst){
        var code = new StringBuilder();

        int lhsCurrent = currentMethod.getVarTable().get(((Operand)inst.getDest()).getName()).getVirtualReg();


        //code.append("before map: "+ inst.getRhs().getInstType().toString() + "\n");
        code.append(instructionMap.apply(inst.getRhs()));


        //code.append("after map\n");

        switch (inst.getTypeOfAssign().getTypeOfElement()) {
            case INT32:
            case BOOLEAN:
                code.append("istore");
                break;
            case OBJECTREF:
                code.append("astore");
                break;
            case VOID:
                break;
            case ARRAYREF:
                code.append("iastore\n");


                break;

            default:
                throw new NotImplementedException("Assign Type not implemented" + inst.getTypeOfAssign().getTypeOfElement());
        }
        if(inst.getTypeOfAssign().getTypeOfElement() != ElementType.ARRAYREF)
            code.append((lhsCurrent <= 3) ? "_" : " ").append(lhsCurrent).append("\n");


        return code.toString();
    }

    public String generateLoadInstruction(Element element) {
        var code = new StringBuilder();
        if (element.isLiteral()) {
            String value = ((LiteralElement) element).getLiteral();
            code.append("ldc " + value + "\n");
        }

        else {

            //code.append("var " + element.getType() + " isn't literal\n");
            if(currentMethod.getVarTable().isEmpty()){
                code.append("");
                return code.toString();
            }
            if(currentMethod.getVarTable().get(((Operand) element).getName()) == null){
                code.append("");
                return code.toString();
            }
            int currentLocation = currentMethod.getVarTable().get(((Operand) element).getName()).getVirtualReg();
            switch (element.getType().getTypeOfElement()) {
                case INT32:
                    code.append("iload");
                    break;
                case BOOLEAN:
                    if (((Operand) element).getName().equals("true") || ((Operand) element).getName().equals("false")) {
                        code.append("iconst_" +
                                (((Operand) element).getName().equals("false")  ? 0 : 1 ) + "\n")  ;
                        return code.toString();
                    }
                    else {
                        code.append("iload");
                        break;
                    }

                case THIS:
                    code.append("aload_0" + "\n");
                    return code.toString();
                case OBJECTREF:
                case ARRAYREF:
                    code.append("aload");
                    break;
                case VOID:
                    return code.toString();
                default:
                    throw new NotImplementedException("Load Type not implemented" + element.getType().getTypeOfElement());
            }
            code.append((currentLocation <= 3) ? "_" : " ").append(currentLocation).append("\n");
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
                case OBJECTREF:
                case ARRAYREF:
                    code.append("areturn \n");
                    break;
                default:
                    System.out.println(element.getType().getTypeOfElement());
                    throw new NotImplementedException("Return Type not implemented: ");

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


    private String getCode(CondBranchInstruction inst){
        var code = new StringBuilder();

        comparisons++;


        //code.append("condition is:\n" + inst.getCondition().getInstType().toString() + "\n");
        code.append(instructionMap.apply(inst.getCondition()));
        code.append("ifne THEN_" + comparisons + "\n");


        return code.toString();
    }


    private String getCode(GotoInstruction inst){
        return ("goto " + inst.getLabel() + "\n");
    }

    private String getCode(UnaryOpInstruction inst) {
        var code = new StringBuilder();

        code.append(generateLoadInstruction(inst.getOperand()) + "\n");

        //code.append("inst operation is: " + inst.getOperation().getOpType() + " ");

        if (inst.getOperation().getOpType() == OperationType.NOTB) {
            code.append("ifeq TRUELABEL_" + conditionals + "\n");
            code.append("iconst_0\n");
            code.append("goto STOPLABEL_" + conditionals + "\n");
            code.append("TRUELABEL_" + conditionals + ":\n");
            code.append("iconst_1\n");
            code.append("STOPLABEL_" + conditionals + ":\n");
        }
        else {
            code.append("i");
            code.append(inst.getOperation().getOpType().toString().toLowerCase());
            code.append("\n");
        }



        return code.toString();
    }

    private String getCodeUnaryOpInstruction (BinaryOpInstruction inst) {
        var code = new StringBuilder();

        code.append(generateLoadInstruction(inst.getRightOperand()));
        code.append("\n");
        code.append("i");
        //code.append("insta operation is: " + inst.getOperation().getOpType() + " ");
        code.append(inst.getOperation().getOpType().toString().toLowerCase());
        code.append("\n");

        return code.toString();
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

        if (inst.getReturnType().getTypeOfElement() == ElementType.OBJECTREF) {
            for (Element e : inst.getListOfOperands()) {
                code.append(generateLoadInstruction(e));
            }

            code.append("new ");
            code.append(((Operand) inst.getFirstArg()).getName());
            code.append("\n");
            code.append("dup\n");
        }

        else if (inst.getReturnType().getTypeOfElement() == ElementType.ARRAYREF) {
            for (Element e : inst.getListOfOperands()) {
                code.append(generateLoadInstruction(e));
            }

            code.append("newarray ");
            if (inst.getListOfOperands().get(0).getType().getTypeOfElement() == ElementType.INT32) {
                code.append("int\n");
            }


        }

        /*var className = getJasminType(inst.getReturnType());

        code.append("new " + className + "\n");
        code.append("dup\n");
        //code.append(getCodeInvokeSpecial(inst));

         */


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

        for (Element e : inst.getListOfOperands()) {
            code.append(getJasminType(e.getType()));
        }

        code.append(")");
        code.append(getJasminType(inst.getReturnType()));
        code.append("\n");

        return code.toString();
    }

    private String getCodeInvokeSpecial(CallInstruction inst) {
        var code = new StringBuilder();

        code.append(generateLoadInstruction(inst.getFirstArg()));


        code.append("invokespecial ")
                .append(inst.getFirstArg().getType().getTypeOfElement() == ElementType.THIS
                        ? classUnit.getSuperClass() : className )
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
        //if (true)  return "type of element is: " + type.getTypeOfElement();
        if (type instanceof ArrayType){
            return "[" + getJasminType(((ArrayType) type).getTypeOfElements());
        }
        switch (type.getTypeOfElement()){
            case THIS:
                return classUnit.getClassName();
            case OBJECTREF:
                return "L" + ((ClassType) type).getName() + ";";
            default:
                getJasminType(type.getTypeOfElement());
                break;
        }

        return getJasminType(type.getTypeOfElement());
    }

    private String getObjectName(String name) {
        if (name.equals("this"))
            return className;
        return name;
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

    private String getComparison(Operation operation) {
        switch (operation.getOpType()) {
            case GTE:
                return "if_icmpge";
            case LTH:
                return "if_icmplt";
            case EQ:
                return "if_icmpeq";
            case NOTB:
            case NEQ:
                return "if_icmpne";
            default:
                System.out.println(operation.getOpType());
                return "ERROR comparison not implemented yet";
        }
    }

}


