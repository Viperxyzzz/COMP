package pt.up.fe.comp.ollir;

import pt.up.fe.comp.MySymbolTable;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.stream.Collectors;

public class OllirGenerator extends AJmmVisitor<Integer, Integer> {
    private final StringBuilder code;
    private final SymbolTable mySymbolTable;
    public OllirGenerator(SymbolTable mySymbolTable){
        this.code = new StringBuilder();
        this.mySymbolTable = mySymbolTable;

        addVisit("Start",this::programVisit);
        addVisit("ClassDeclaration", this::classDeclVisit);
        addVisit("MethodDecl", this::methodDeclVisit);
        addVisit("InitStatement",this::initStatementVisit);
        addVisit("DotExp",this::dotExpVisit);
    }

    public String getCode(){
        return this.code.toString();
    }


    private Integer programVisit(JmmNode program, Integer dummy){
        for (var importString : mySymbolTable.getImports()){
            code.append("import ").append(importString).append(";\n");
        }
        for (var child : program.getChildren()){
            visit(child);
        }
        return 0;
    }

    private Integer classDeclVisit(JmmNode classDecl, Integer dummy){
        //public HelloWorld extends BoardBase
        code.append("public ").append(mySymbolTable.getClassName());
        var superClass = mySymbolTable.getSuper();
        if(superClass != null){
            code.append(" extends ").append(superClass);
        }
        code.append("{\n");

        for (var child : classDecl.getChildren()){
            visit(child);
        }

        code.append("}\n");
        return 0;
    }

    private Integer methodDeclVisit(JmmNode methodDecl, Integer dummy){

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
        System.out.print(stmts);
        for (var stmt: stmts){
            visit(stmt);
        }
        code.append("}\n");

        return 0;
    }

    private Integer initStatementVisit(JmmNode jmmNode, Integer integer) {
        //2:35:00
        /*
        visit(jmmNode.getJmmChild(0));
        code.append(";\n");
        */
        return 0;
    }


    private Integer dotExpVisit(JmmNode dotExp, Integer integer){
        //
        return 0;
    }


}
