package pt.up.fe.comp;

import org.junit.Test;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.specs.util.SpecsIo;

import java.util.Collections;

public class JasminTest {
    @Test
    public void test(){
        var jasminResult = TestUtils.backend(SpecsIo.getResource("fixtures/public/Simple.jmm"));
        TestUtils.noErrors(jasminResult);

        jasminResult.compile();
        String result = jasminResult.run();
        System.out.println("JASMIN CODE: " + result);
    }

    @Test
    public void test2(){
        new JasminResult(SpecsIo.getResource("fixtures/public/jasmin/HelloWorld.j")).run();
    }

    @Test
    public void test3(){
        var ollirResult = new OllirResult(SpecsIo.getResource("fixtures/public/ollir/HelloWord.ollir"), Collections.emptyMap());
        var jasminResult = TestUtils.backend(ollirResult);

        jasminResult.run();
    }

    @Test
    public void test4(){
        var ollirResult = new OllirResult(SpecsIo.getResource("fixtures/public/cp2/OllirToJasminBasic.ollir"), Collections.emptyMap());
        var jasminResult = TestUtils.backend(ollirResult);

    }

    @Test
    public void test5(){
        var ollirResult = new OllirResult(SpecsIo.getResource("fixtures/public/cp2/OllirToJasminArithmetics.ollir"), Collections.emptyMap());
        var jasminResult = TestUtils.backend(ollirResult);

    }

    @Test
    public void test6(){
        var ollirResult = new OllirResult(SpecsIo.getResource("fixtures/public/cp2/OllirToJasminFields.ollir"), Collections.emptyMap());
        var jasminResult = TestUtils.backend(ollirResult);
    }

    @Test
    public void test7(){
        var ollirResult = new OllirResult(SpecsIo.getResource("fixtures/public/cp2/OllirToJasminInvoke.ollir"), Collections.emptyMap());
        var jasminResult = TestUtils.backend(ollirResult);
    }
}
