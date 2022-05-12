package pt.up.fe.comp.ollir;

import pt.up.fe.comp.MySymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

public class OllirGenerator extends AJmmVisitor<Integer, Integer> {
    private final StringBuilder code;
    private final MySymbolTable mySymbolTable;
    public OllirGenerator(MySymbolTable mySymbolTable){
        this.code = new StringBuilder();
        this.mySymbolTable = mySymbolTable;
    }
    private Integer programVisit(JmmNode program, Integer dummy){
        //xd
        return 0;
    }

}
