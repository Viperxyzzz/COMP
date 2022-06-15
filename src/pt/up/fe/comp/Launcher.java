package pt.up.fe.comp;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsSystem;

public class Launcher {

    public static void main(String[] args) {
        SpecsSystem.programStandardInit();

        SpecsLogs.info("Executing with args: " + Arrays.toString(args));

        // read the input code
        /*if (args.length != 1) {
            throw new RuntimeException("Expected a single argument, a path to an existing input file.");
        }*/
        if (args.length > 4){
            throw new RuntimeException("Expected four arguments maximum, usage: comp2022-5a [-r=<num>] [-o] [-d] -i=<input_file.jmm>");
        }
        String filename = args[args.length - 1];
        File inputFile = new File(filename);
        if (!inputFile.isFile()) {
            throw new RuntimeException("Expected a path to an existing input file, got '" + filename + "'.");
        }
        String input = SpecsIo.read(inputFile);

        // Create config
        Map<String, String> config = new HashMap<>();
        /*config.put("inputFile", args[0]);
        config.put("optimize", "false");
        config.put("registerAllocation", "-1");
        config.put("debug", "true");*/

        System.out.println("FILE: " + filename);

        config.put("inputFile", filename);
        if (args[0].contains("-r")) {
            config.put("registerAllocation", args[0].substring(3));
            // Not implemented
        }
        else {
            config.put("registerAllocation", "-1");
        }
        if (Arrays.asList(args).contains("-o")) {
            config.put("optimize", "true");
            // Not implemented
        }
        else {
            config.put("optimize", "false");
        }
        if (Arrays.asList(args).contains("-d")) {
            config.put("debug", "true");
        }
        else {
            config.put("debug", "false");
        }

        // Instantiate JmmParser
        SimpleParser parser = new SimpleParser();

        // Parse stage
        JmmParserResult parserResult = parser.parse(input, config);

        // Check if there are parsing errors
        TestUtils.noErrors(parserResult.getReports());

        // Instantiate JmmAnalysis
        JmmAnalyser analyser = new JmmAnalyser();

        // Analysis stage
        JmmSemanticsResult analysisResult = analyser.semanticAnalysis(parserResult);

        // Check if there are parsing errors
        TestUtils.noErrors(analysisResult.getReports());

        // Instantiate JmmOptimization
        JmmOptimizer optimizer = new JmmOptimizer();

        // Optimization stage
        OllirResult optimizationResult = optimizer.toOllir(analysisResult);

        // Check if there are parsing errors
        TestUtils.noErrors(optimizationResult.getReports());

        // Instantiate JasminBackend
        JasminEmitter jasminEmitter = new JasminEmitter();

        // Backend Stage
        JasminResult backendResult = jasminEmitter.toJasmin(optimizationResult);

        // Check if there are parsing errors
        TestUtils.noErrors(backendResult.getReports());

    }

}
