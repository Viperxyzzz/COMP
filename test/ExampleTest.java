import org.junit.Test;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.specs.util.SpecsIo;

public class ExampleTest {
    @Test
    public void testAccept() {
        var parserResult = TestUtils.parse("2+3");
        TestUtils.noErrors(parserResult.getReports());
    }
    @Test
    public void testFail() {
        var parserResult = TestUtils.parse("while(1+1)");
        TestUtils.mustFail(parserResult.getReports());
    }
    @Test
    public void testFile(){
        var result = TestUtils.parse(SpecsIo.getResource("fixtures/public/input.jmm"));
        TestUtils.noErrors(result.getReports());
    }
}