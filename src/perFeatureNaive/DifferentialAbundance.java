/*
The MIT License (MIT)

Copyright (c) <2015> <Justin Wagner>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/

package perFeatureNaive;

import naiveF.PrepareDataDANaive;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.math3.distribution.TDistribution;

import util.EvaRunnable;
import util.GenRunnable;
import util.Utils;
import circuits.CircuitLib;
import circuits.arithmetic.FloatLib;
import circuits.arithmetic.IntegerLib;
import flexsc.CompEnv;

public class DifferentialAbundance {
	static int PLength = 54;
	static int VLength = 11;
	static int filterThreshold = 1;
	
	static public<T> T[][] compute(CompEnv<T> gen, T[][] inputAliceCase, 
			T[][] inputBobCase, T[][] inputAliceControl, T[][] inputBobControl, T[] numAliceCase,
			T[] numBobCase, T[] numAliceControl, T[] numBobControl){
		FloatLib<T> flib = new FloatLib<T>(gen, PLength, VLength);
		IntegerLib<T> ilib = new IntegerLib<T>(gen, 32);
		CircuitLib<T> cl = new CircuitLib<T>(gen);

		T[] pointOne = flib.publicValue(.1);
		T[] zero = flib.publicValue(0.0);
		T[] one = flib.publicValue(1.0);
		T[] negOne = flib.publicValue(-1.0);
		
		T[] caseSum = flib.publicValue(0.0);
		T[] caseSumOfSquares = flib.publicValue(0.0);
		T[] caseShift = pointOne;
		for(int i = 0; i < inputAliceCase.length; i++){
			caseSum = flib.add(caseSum, flib.sub(inputAliceCase[i], caseShift));
			T[] addSquare = flib.multiply(flib.sub(inputAliceCase[i], caseShift), flib.sub(inputAliceCase[i], caseShift));
			caseSumOfSquares = flib.add(caseSumOfSquares, addSquare);
		}
		
		for(int i = 0; i < inputBobCase.length; i++){
			caseSum = flib.add(caseSum, flib.sub(inputBobCase[i], caseShift));
			T[] addSquare = flib.multiply(flib.sub(inputBobCase[i], caseShift), flib.sub(inputBobCase[i], caseShift));
			caseSumOfSquares = flib.add(caseSumOfSquares, addSquare);
		}
		
		T[] controlSum = flib.publicValue(0.0);
		T[] controlSumOfSquares = flib.publicValue(0.0);
		T[] controlShift = pointOne;

		for(int i = 0; i < inputAliceControl.length; i++){
			controlSum = flib.add(controlSum, flib.sub(inputAliceControl[i], controlShift));
			T[] addSquare = flib.multiply(flib.sub(inputAliceControl[i], controlShift), flib.sub(inputAliceControl[i], controlShift));
			controlSumOfSquares = flib.add(controlSumOfSquares, addSquare);
		}
		
		for(int i = 0; i < inputBobControl.length; i++){
			controlSum = flib.add(controlSum, flib.sub(inputBobControl[i], controlShift));
			T[] addSquare = flib.multiply(flib.sub(inputBobControl[i], controlShift), flib.sub(inputBobControl[i], controlShift));
			controlSumOfSquares = flib.add(controlSumOfSquares, addSquare);
		}

		T[] caseNumPresent = ilib.add(numAliceCase, numBobCase);
		T[] controlNumPresent= ilib.add(numAliceControl,numBobControl);
		T[] caseNum = flib.add(flib.publicValue(inputAliceCase.length), flib.publicValue(inputBobCase.length));
		T[] controlNum  = flib.add(flib.publicValue(inputAliceControl.length), flib.publicValue(inputBobControl.length));

		
		T[][] res = gen.newTArray(2, 0);

		T[] zeroInt = ilib.publicValue(0.0);
		T caseIsZero = ilib.eq(caseNumPresent, zeroInt);
		T controlIsZero = ilib.eq(controlNumPresent, zeroInt);
		T oneIsZero = ilib.or(caseIsZero, controlIsZero);
		T sendOneOrNegOne = ilib.geq(caseNumPresent, controlNumPresent);
		T[] tStatZero = cl.mux(one, negOne, sendOneOrNegOne);
		
		T[] caseMeanAbundance = flib.div(flib.add(caseSum, caseShift), caseNum);
		T[] caseVarianceSecondTerm = flib.div(flib.multiply(caseSum, caseSum), caseNum);
		T[] caseVariance = flib.div(flib.sub(caseSumOfSquares, caseVarianceSecondTerm), caseNum);
		T[] controlMeanAbundance = flib.div(flib.add(controlSum, controlShift), controlNum);		    
		T[] controlVarianceSecondTerm = flib.div(flib.multiply(controlSum, controlSum), controlNum);
		T[] controlVariance = flib.div(flib.sub(controlSumOfSquares, controlVarianceSecondTerm), controlNum);

		T[] tUpper = flib.sub(controlMeanAbundance, caseMeanAbundance);
		T tLowerFirstIsZero = flib.eq(caseNumPresent, zero);
		T[] tLowerFirst = flib.div(caseVariance, caseNum);
		tLowerFirst = cl.mux(tLowerFirst, zero, tLowerFirstIsZero);
		
		T tLowerSecondIsZero = flib.eq(controlNumPresent, zero);
		
		T[] tLowerSecond = flib.div(controlVariance, controlNum);
		tLowerSecond = cl.mux(tLowerSecond, zero, tLowerSecondIsZero);
		
		T[] tLowerSqrt = flib.sqrt(flib.add(flib.add(zero,tLowerFirst), flib.add(zero,tLowerSecond)));
		T[] tStatNonZero = flib.div(tUpper, tLowerSqrt);
		
		T[] degreesOfFreedomTop = flib.add(tLowerFirst, tLowerSecond);
		degreesOfFreedomTop = flib.multiply(flib.add(zero,degreesOfFreedomTop), flib.add(zero,degreesOfFreedomTop));

		T[] degreesOfFreedomBottomFirst = flib.div(caseVariance, flib.sub(caseNum, one));
		degreesOfFreedomBottomFirst = flib.multiply(flib.add(zero,degreesOfFreedomBottomFirst), flib.add(zero,degreesOfFreedomBottomFirst));
		degreesOfFreedomBottomFirst = flib.div(degreesOfFreedomBottomFirst, flib.sub(caseNum, one));

		T[] degreesOfFreedomBottomSecond = flib.div(controlVariance, flib.sub(controlNum, one));
		degreesOfFreedomBottomSecond = flib.multiply(flib.add(zero,degreesOfFreedomBottomSecond), flib.add(zero,degreesOfFreedomBottomSecond));
		degreesOfFreedomBottomSecond = flib.div(degreesOfFreedomBottomSecond, flib.sub(controlNum, one));

		T[] degreesOfFreedom = flib.div(degreesOfFreedomTop, flib.add(degreesOfFreedomBottomFirst, degreesOfFreedomBottomSecond));
		
		T[] oneInt = ilib.publicValue(1.0);
		T caseIsOne = ilib.eq(caseNumPresent, oneInt);
		T controlIsOne = ilib.eq(controlNumPresent, oneInt);
		T greaterThanOne = ilib.and(ilib.xor(caseIsOne, controlIsOne), oneIsZero);
		
		T returnCase = ilib.not(ilib.leq(caseNumPresent, controlNumPresent));
			
		T[] tStat = cl.mux(tStatNonZero, tStatZero,  greaterThanOne);
		T[] dfZero = cl.mux(flib.sub(controlNum, one), flib.sub(caseNum, one), returnCase);
		T[] df = cl.mux(degreesOfFreedom, dfZero, oneIsZero);

		res[0] = tStat;
		res[1] = df;
		return res;
	}
	
	public static class Generator<T> extends GenRunnable<T> {
		T[][] inputBobCase;
		T[][] inputAliceCase;
		T[][] inputAliceControl;
		T[][] inputBobControl;
		T[][] inputCounters;
		T[][][] in;
		double[][] caseInput;
		double[][] controlInput;
		T[] aliceCaseNum;
		T[] bobCaseNum;
		T[] aliceControlNum;
		T[] bobControlNum;
		
		int EvaCaseFeatures;
		int EvaCaseSamples;
		int GenCaseFeatures;
		int GenCaseSamples;
		int EvaControlFeatures;
		int EvaControlSamples;
		int GenControlFeatures;
		int GenControlSamples;		

		T[] numAliceCase;
		T[] numBobCase;
		T[] numAliceControl;
		T[] numBobControl;

		@Override
		public void prepareInput(CompEnv<T> gen) throws Exception {
			Options options = new Options();
			options.addOption("s", "case", true, "case");
			options.addOption("t", "control", true, "control");

			CommandLineParser parser = new BasicParser();
			CommandLine cmd = parser.parse(options, args);

			if(!cmd.hasOption("s") || !cmd.hasOption("t")) {
			  throw new Exception("wrong input");
			}
			caseInput = PrepareDataDANaive.readFile(cmd.getOptionValue("s"));
			controlInput = PrepareDataDANaive.readFile(cmd.getOptionValue("t"));

			EvaCaseFeatures = gen.channel.readInt();
			gen.channel.flush();
			EvaCaseSamples = gen.channel.readInt();
			gen.channel.flush();

			GenCaseFeatures = caseInput.length;
			gen.channel.writeInt(GenCaseFeatures);
			gen.channel.flush();
			GenCaseSamples = caseInput[0].length;
			gen.channel.writeInt(GenCaseSamples);
			gen.channel.flush();
			EvaControlFeatures = gen.channel.readInt();
			gen.channel.flush();
			EvaControlSamples = gen.channel.readInt();
			gen.channel.flush();
			GenControlFeatures = controlInput.length;
			gen.channel.writeInt(GenControlFeatures);
			gen.channel.flush();
			GenControlSamples = controlInput[0].length;
			gen.channel.writeInt(GenControlSamples);
			gen.channel.flush();

			in = gen.newTArray(EvaCaseFeatures, 2, 0);

			System.out.println("Eva Case Dim 1 " + EvaCaseFeatures);
			System.out.println("Eva Case Dim 2 " + EvaCaseSamples);
			System.out.println("Eva Control Dim 1 " + EvaControlFeatures);
			System.out.println("Eva Control Dim 2 " + EvaControlSamples);
			System.out.println("Gen Case Dim 1 " + GenCaseFeatures);
			System.out.println("Gen Case Dim 2 " + GenCaseSamples);
			System.out.println("Gen Control Dim 1 " + GenControlFeatures);
			System.out.println("Gen Control Dim 2 " + GenControlSamples);
		}
		
		@Override
		public void secureCompute(CompEnv<T> gen) {
			FloatLib<T> flib = new FloatLib<T>(gen, PLength, VLength);
			T[] l = flib.publicValue(0.0);

			for(int i =0; i < EvaCaseFeatures; i++){
				int numCase = 0;
				int numControl = 0;
				for(int j = 0; j < caseInput[0].length; j++){
					if(caseInput[i][j] > 0.0){
						numCase++;
					}
				}
				for(int j = 0; j < controlInput[0].length; j++){
					if(controlInput[i][j] > 0.0){
						numControl++;
					}
				}

				numAliceCase = gen.inputOfAlice(Utils.fromInt(numCase, 32));
				numBobCase = gen.inputOfBob(new boolean[32]);
				numAliceControl = gen.inputOfAlice(Utils.fromInt(numControl, 32));
				numBobControl = gen.inputOfBob(new boolean[32]);
				
				inputAliceCase = gen.newTArray(GenCaseSamples, 0);
				inputBobCase = gen.newTArray(EvaCaseSamples, 0);
				inputAliceControl = gen.newTArray(GenControlSamples, 0);
				inputBobControl = gen.newTArray(EvaControlSamples, 0);

				for(int j =0; j < GenCaseSamples; j++){
					inputAliceCase[j] = gen.inputOfAlice(Utils.fromFloat(caseInput[i][j], PLength, VLength));
				}
				gen.flush();
				for(int j =0; j < EvaCaseSamples; j++){
					inputBobCase[j] = gen.inputOfBob(new boolean[l.length]);
				}
				gen.flush();
				for(int j =0; j < GenControlSamples; j++){
					inputAliceControl[j] = gen.inputOfAlice(Utils.fromFloat(controlInput[i][j], PLength, VLength));
				}
				gen.flush();
				for(int j =0; j < EvaControlSamples; j++){
					inputBobControl[j] = gen.inputOfBob(new boolean[l.length]);
				}
				gen.flush();

				in[i] = compute(gen, inputAliceCase, inputBobCase, inputAliceControl, inputBobControl,
						numAliceCase, numBobCase, numAliceControl, numBobControl);
			}
		}
		@Override
		public void prepareOutput(CompEnv<T> gen) {
			FloatLib<T> flib = new FloatLib<T>(gen, PLength, VLength);
			for(int j = 0; j < in.length; j++){
				double tStat = flib.outputToAlice(in[j][0]);
				double df = flib.outputToAlice(in[j][1]);
				if (tStat == 0.0){
					System.out.println("NA,NA,NA");
					continue;
				}
				if (df <= 0.0){
					System.out.println(tStat +",NA,NA");
					continue;
				}
				TDistribution tDistribution = new TDistribution(df);
				if(tStat > 0.0)
					System.out.println(tStat + "," + df + "," + (1-tDistribution.cumulativeProbability(tStat))*2.0);
				else
					System.out.println(tStat + "," + df + "," +  tDistribution.cumulativeProbability(tStat)*2.0);

			}			
		}
	}
	
	public static class Evaluator<T> extends EvaRunnable<T> {
		T[][] inputBobCase;
		T[][] inputAliceCase;
		T[][] inputCounters;
		T[][] inputAliceControl;
		T[][] inputBobControl;
		T[] scResult;
		T[][][] in;
		
		double[][] caseInput;
		double[][] controlInput;
		
		int EvaCaseFeatures;
		int EvaCaseSamples;
		int GenCaseFeatures;
		int GenCaseSamples;
		int EvaControlFeatures;
		int EvaControlSamples;
		int GenControlFeatures;
		int GenControlSamples;
		T[] numAliceCase;
		T[] numBobCase;
		T[] numAliceControl;
		T[] numBobControl;
		
		@Override
		public void prepareInput(CompEnv<T> gen) throws Exception {
			Options options = new Options();
			options.addOption("s", "case", true, "case");
			options.addOption("t", "control", true, "control");

			CommandLineParser parser = new BasicParser();
			CommandLine cmd = parser.parse(options, args);

			if(!cmd.hasOption("s") || !cmd.hasOption("t")) {
			  throw new Exception("wrong input");
			}

			caseInput = PrepareDataDANaive.readFile(cmd.getOptionValue("s"));
			controlInput = PrepareDataDANaive.readFile(cmd.getOptionValue("t"));

			EvaCaseFeatures = caseInput.length;
			gen.channel.writeInt(EvaCaseFeatures);
			gen.channel.flush();
			EvaCaseSamples = caseInput[0].length;
			gen.channel.writeInt(EvaCaseSamples);
			gen.channel.flush();			
			GenCaseFeatures = gen.channel.readInt();
			gen.channel.flush();
			GenCaseSamples = gen.channel.readInt();
			gen.channel.flush();
			EvaControlFeatures = controlInput.length;
			gen.channel.writeInt(EvaControlFeatures);
			gen.channel.flush();
			EvaControlSamples = controlInput[0].length;
			gen.channel.writeInt(EvaControlSamples);
			gen.channel.flush();
			GenControlFeatures = gen.channel.readInt();
			gen.channel.flush();
			GenControlSamples = gen.channel.readInt();
			gen.channel.flush();

			in = gen.newTArray(EvaCaseFeatures, 2, 0);

			System.out.println("Eva Case Dim 1 " + EvaCaseFeatures);
			System.out.println("Eva Case Dim 2 " + EvaCaseSamples);
			System.out.println("Eva Control Dim 1 " + EvaControlFeatures);
			System.out.println("Eva Control Dim 2 " + EvaControlSamples);
			System.out.println("Gen Case Dim 1 " + GenCaseFeatures);
			System.out.println("Gen Case Dim 2 " + GenCaseSamples);
			System.out.println("Gen Control Dim 1 " + GenControlFeatures);
			System.out.println("Gen Control Dim 2 " + GenControlSamples);
			
			System.out.println("Done with inputBobControl eva");
		}
		
		@Override
		public void secureCompute(CompEnv<T> gen) {
			FloatLib<T> flib = new FloatLib<T>(gen, PLength, VLength);
			T[] l = flib.publicValue(0.0);
			
			for(int i =0; i < EvaCaseFeatures; i++){
				int numCase = 0;
				int numControl = 0;
				for(int j = 0; j < caseInput[0].length; j++){
					if(caseInput[i][j] > 0.0){
						numCase++;
					}
				}
				for(int j = 0; j < controlInput[0].length; j++){
					if(controlInput[i][j] > 0.0){
						numControl++;
					}
				}
				
				numAliceCase = gen.inputOfAlice(new boolean[32]);
				numBobCase = gen.inputOfBob(Utils.fromInt(numCase, 32));
				numAliceControl = gen.inputOfAlice(new boolean[32]);
				numBobControl = gen.inputOfBob(Utils.fromInt(numControl, 32));
				
				inputAliceCase = gen.newTArray(GenCaseSamples, 0);
				inputBobCase = gen.newTArray(EvaCaseSamples, 0);
				inputAliceControl = gen.newTArray(GenControlSamples, 0);
				inputBobControl = gen.newTArray(EvaControlSamples, 0);

				for(int j =0; j < GenCaseSamples; j++){
					inputAliceCase[j] = gen.inputOfAlice(new boolean[l.length]);
				}
				gen.flush();
				for(int j =0; j < EvaCaseSamples; j++){
					inputBobCase[j] = gen.inputOfBob(Utils.fromFloat(caseInput[i][j], PLength, VLength));
				}
				gen.flush();
				for(int j =0; j < GenControlSamples; j++){
					inputAliceControl[j] = gen.inputOfAlice(new boolean[l.length]);
				}
				gen.flush();
				for(int j =0; j < EvaControlSamples; j++){
					inputBobControl[j] = gen.inputOfBob(Utils.fromFloat(controlInput[i][j], PLength, VLength));
				}
				gen.flush();

				in[i] = compute(gen, inputAliceCase, inputBobCase, inputAliceControl, inputBobControl,
						numAliceCase, numBobCase, numAliceControl, numBobControl);
			}
		}
		
		@Override
		public void prepareOutput(CompEnv<T> gen) {
			FloatLib<T> flib = new FloatLib<T>(gen, PLength, VLength);
			for(int i = 0; i < in.length; i++){
				flib.outputToAlice(in[i][0]);
				flib.outputToAlice(in[i][1]);
			}
		}
				
	}
}
