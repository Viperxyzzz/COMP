package pt.up.fe.comp;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.ollir.OllirGenerator;

import java.util.Collections;

public class JmmOptimizer implements JmmOptimization {

    @Override
    public OllirResult toOllir(JmmSemanticsResult semanticsResult) {
        var ollirGenerator = new OllirGenerator(semanticsResult.getSymbolTable());
        ollirGenerator.visit(semanticsResult.getRootNode());
        var ollirCode = ollirGenerator.getCode();
        /*var ollirCode = "import io;\n" +
                "import Quicksort;\n" +
                "SymbolTable extends Quicksort {\n" +
                "    .field private a.i32;\n" +
                "    .construct SymbolTable().V {\n" +
                "        invokespecial(this, \"<init>\").V;\n" +
                "    }\n" +
                "    .method public static main(args.array.String).V{\n" +
                "        temp0.i32 :=.i32 2.i32 +.i32 3.i32;\n" +
                "        temp1.i32 :=.i32 temp1.i32 +.i32 5.i32;\n" +
                "        temp2.i32 :=.i32 invokestatic(io, \"getWidth\").i32;\n" +
                "        temp3.i32 :=.i32 temp1.i32 +.i32 temp2.i32;\n" +
                "        a.i32 :=.i32 temp3.i32;\n" +
                "    }" +
                ".method public foo().i32 {\n" +
                "a.i32 :=.i32 1.i32;\n" +
                "temp1.V :=.V invokestatic(io,\"println\",a.V).V;\n" +
                "}\n" +
                "}";*/

        if (semanticsResult.getConfig().get("debug") == "true")
            System.out.print("OLLIR CODE :\n" + ollirCode);
        else
            System.out.println("Generating Ollir");
        
        return new OllirResult(semanticsResult,ollirCode, Collections.emptyList());
    }
}
