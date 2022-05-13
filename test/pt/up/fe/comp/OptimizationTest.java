package pt.up.fe.comp;

import org.junit.Test;
import pt.up.fe.specs.util.SpecsIo;

public class OptimizationTest {

    @Test
    public void test(){
        var ollirResources = TestUtils.optimize(SpecsIo.getResource("fixtures/public/HelloWorld.jmm"));

        TestUtils.noErrors(ollirResources);
    }
}
