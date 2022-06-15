package pt.up.fe.comp;

import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;

import java.util.Collections;

public class JasminEmitter implements JasminBackend {

    @Override
    public JasminResult toJasmin(OllirResult ollirResult) {

        String jasminCode = new OllirToJasmin(ollirResult.getOllirClass()).getCode();

        if (ollirResult.getConfig().get("debug") == "true")
            System.out.println("JASMIN CODE:\n" + jasminCode);
        else
            System.out.println("Generating jasmin");

        return new JasminResult(ollirResult, jasminCode, Collections.EMPTY_LIST);
    }
}
