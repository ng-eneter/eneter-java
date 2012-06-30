package service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.diagnostic.EneterTrace.EDetailLevel;

public class Program
{
    public static void main(String[] args) throws Exception
    {
    	//EneterTrace.setDetailLevel(EDetailLevel.Debug);
    	//EneterTrace.setNameSpaceFilter(Pattern.compile("^(?!\beneter.messaging.dataprocessing\b)"));
    	//EneterTrace.setTraceLog(new PrintStream("D:\\Trace.txt"));
    	
        Calculator aCalculator = new Calculator();
        aCalculator.startCalculatorService();
        
        System.out.println("Calculator service is running. Press ENTER to stop.");
        new BufferedReader(new InputStreamReader(System.in)).readLine();

        aCalculator.stopCalculatorService();
    }

}

