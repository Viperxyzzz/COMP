package pt.up.fe.comp;

import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;

import java.util.Collections;

public class JasminEmitter implements JasminBackend {

    @Override
    public JasminResult toJasmin(OllirResult ollirResult) {
        //ollirResult.getOllirClass();

        String jasminCode = "";

        return new JasminResult(ollirResult, jasminCode, Collections.EMPTY_LIST);
    }
}