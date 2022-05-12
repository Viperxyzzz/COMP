package pt.up.fe.comp;

import org.junit.Test;
import pt.up.fe.specs.util.SpecsIo;

public class JasminTest {
    @Test
    public void test(){
        var jasminResult = TestUtils.backend(SpecsIo.getResource("fixtures/public/HelloWorld.jmm"));
        TestUtils.noErrors(jasminResult);

        System.out.println("aqui\n");
        String result = jasminResult.run();
        System.out.println("JASMIN: " + result);
    }
}
