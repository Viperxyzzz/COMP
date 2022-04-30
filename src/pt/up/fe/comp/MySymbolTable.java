package pt.up.fe.comp;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MySymbolTable implements SymbolTable {
    private final List<String> imports;
    private String className;
    private String superClass;

    private final List<String> methods;
    private final Map<String, Type> methodReturnTypes;
    private final Map<String, List<Symbol>> methodParams;

    public MySymbolTable() {
        this.imports = new ArrayList<>();
        this.className = null;
        this.superClass = null;
        this.methods = new ArrayList<>();
        this.methodReturnTypes = new HashMap<>();
        this.methodParams = new HashMap<>();

    }

    @Override
    public List<String> getImports() {
        return imports;
    }

    public void addImport(String importString){
        imports.add(importString);
    }

    @Override
    public String getClassName() {
        return className;
    }

    public void setClassName(String className){
        this.className = className;
    }

    @Override
    public String getSuper() {
        return superClass;
    }

    public void setSuperClass(String className){
        this.className = className;
    }

    @Override
    public List<Symbol> getFields() {
        return null;
    }

    @Override
    public List<String> getMethods() {
        return methods;
    }

    @Override
    public Type getReturnType(String methodSignature) {
        return methodReturnTypes.get(methodSignature);
    }

    @Override
    public List<Symbol> getParameters(String methodSignature) {
        return methodParams.get(methodSignature);
    }

    @Override
    public List<Symbol> getLocalVariables(String methodSignature) {
        return null;
    }

    public boolean hasMethod(String methodSignature){
        return methods.contains(methodSignature);
    }

    public void addMethod(String methodSignature, Type returnType, List<Symbol> params){
        methods.add(methodSignature);
        methodReturnTypes.put(methodSignature, returnType);
        methodParams.put(methodSignature, params);

        //completar
    }
}
