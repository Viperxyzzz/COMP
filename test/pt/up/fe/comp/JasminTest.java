package pt.up.fe.comp;

import org.junit.Test;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.specs.util.SpecsIo;

public class JasminTest {
    @Test
    public void test(){
        var jasminResult = TestUtils.backend(SpecsIo.getResource("fixtures/public/HelloWorld.jmm"));
        TestUtils.noErrors(jasminResult);

        String result = jasminResult.run();
    }

    @Test
    public void test2(){
        new JasminResult(SpecsIo.getResource("fixtures/public/jasmin/HelloWorld.j")).run();
    }
}
