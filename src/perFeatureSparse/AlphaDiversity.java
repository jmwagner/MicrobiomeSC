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

package perFeatureSparse;

import naiveF.PrepareDataDANaive;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.math3.distribution.TDistribution;

import util.EvaRunnable;
import util.GenRunnable;
import util.Utils;
import circuits.arithmetic.FloatLib;
import circuits.arithmetic.IntegerLib;
import flexsc.CompEnv;

public class AlphaDiversity {
	static int PLength = 54;
	static int VLength = 11;
	
	static public<T> T[] computeSimpsons(CompEnv<T> gen, T[][] input){
		FloatLib<T> flib = new FloatLib<T>(gen, PLength, VLength);
		T[] pointOne = flib.publicValue(0.000001);
		T[] one = flib.publicValue(1.0);
		T[] zero = flib.publicValue(0.0);
		T[] simpsons = flib.publicValue(0.0);
		T[] simpsonsUpper = flib.publicValue(0.0);
		T[] simpsonsLower = flib.publicValue(0.0);

		for(int i = 0; i < input.length ; i++){
				simpsonsUpper = flib.add(simpsonsUpper, 
						flib.multiply(flib.sub(input[i], pointOne),flib.sub(flib.sub(input[i], pointOne), one)));
				simpsonsLower = flib.add(simpsonsLower, flib.sub(input[i], pointOne));
		}
		simpsonsLower = flib.add(pointOne, simpsonsLower);

		simpsons = flib.sub(one, flib.div(flib.add(simpsonsUpper, pointOne), flib.multiply(simpsonsLower, flib.sub(simpsonsLower,one))));

		return simpsons;
	}
	
	static public<T> T[][] compute(CompEnv<T> gen, T[][] aliceCaseSimpsons, 
			T[][] bobCaseSimpsons, T[][] aliceControlSimpsons, T[][] bobControlSimpsons){
		FloatLib<T> flib = new FloatLib<T>(gen, PLength, VLength);
		T[] pointOne = flib.publicValue(0.1);
		T[] zero = flib.publicValue(0.0);
		T[] caseSum = flib.publicValue(0.0);
		T[] caseSumOfSquares = flib.publicValue(0.0);
		T[] caseShift = pointOne;
		for(int i = 0; i < aliceCaseSimpsons.length; i++){
			caseSum = flib.add(caseSum, flib.sub(aliceCaseSimpsons[i], caseShift));
			caseSumOfSquares = flib.add(caseSumOfSquares, 
					flib.multiply(flib.sub(aliceCaseSimpsons[i], caseShift), flib.sub(aliceCaseSimpsons[i], caseShift)));
		}
		
		for(int i = 0; i < bobCaseSimpsons.length; i++){
			caseSum = flib.add(caseSum, flib.sub(bobCaseSimpsons[i], caseShift));
			caseSumOfSquares = flib.add(caseSumOfSquares, 
					flib.multiply(flib.sub(bobCaseSimpsons[i], caseShift), flib.sub(bobCaseSimpsons[i], caseShift)));
		}
		
		T[] controlSum = flib.publicValue(0.0);
		T[] controlSumOfSquares = flib.publicValue(0.0);
		T[] controlShift = pointOne;

		for(int i = 0; i < aliceControlSimpsons.length; i++){
			controlSum = flib.add(controlSum, flib.sub(aliceControlSimpsons[i], controlShift));
			T[] addSquare = flib.multiply(flib.sub(aliceControlSimpsons[i], controlShift), flib.sub(aliceControlSimpsons[i], controlShift));
			controlSumOfSquares = flib.add(controlSumOfSquares, addSquare);
		}
		
		for(int i = 0; i < bobControlSimpsons.length; i++){
			controlSum = flib.add(controlSum, flib.sub(bobControlSimpsons[i], controlShift));
			controlSumOfSquares = flib.add(controlSumOfSquares, 
					flib.multiply(flib.sub(bobControlSimpsons[i], controlShift), flib.sub(bobControlSimpsons[i], controlShift)));
		}

		T[] caseNum = flib.publicValue(aliceCaseSimpsons.length + bobCaseSimpsons.length);
		T[] controlNum = flib.publicValue(aliceControlSimpsons.length + bobControlSimpsons.length);
		T[] tStat;
		T[][] res = gen.newTArray(2, 0);

		T[] caseTotalSum;
		T[] controlTotalSum;

		T[] caseVariance;
		T[] controlVariance;
		T[] caseVarianceSecondTerm;
		T[] controlVarianceSecondTerm;
		T[] caseMeanAbundance;
		T[] controlMeanAbundance;
		T[] tUpper;
		T[] tLowerFirst;
		T[] tLowerSecond;
		T[] tLowerSqrt;

		caseTotalSum = caseSum;
		controlTotalSum = controlSum;

		caseMeanAbundance = flib.div(flib.add(caseTotalSum, caseShift), caseNum);
		caseVarianceSecondTerm = flib.div(flib.multiply(caseTotalSum, caseTotalSum), caseNum);
		caseVariance = flib.div(flib.sub(caseSumOfSquares, caseVarianceSecondTerm), caseNum);
		controlMeanAbundance = flib.div(flib.add(controlSum, controlShift), controlNum);		    
		controlVarianceSecondTerm = flib.div(flib.multiply(controlSum, controlSum), controlNum);
		controlVariance = flib.div(flib.sub(controlSumOfSquares, controlVarianceSecondTerm), controlNum);

		tUpper = flib.sub(controlMeanAbundance, caseMeanAbundance);
		tLowerFirst = flib.div(caseVariance, caseNum);
		tLowerSecond = flib.div(controlVariance, controlNum);
		tLowerSqrt = flib.sqrt(flib.add(tLowerFirst, tLowerSecond));
		tStat = flib.div(tUpper, tLowerSqrt);

		T[] degreesOfFreedomTop = flib.add(tLowerFirst, tLowerSecond);
		degreesOfFreedomTop = flib.multiply(flib.add(zero,degreesOfFreedomTop), flib.add(zero,degreesOfFreedomTop));

		T[] degreesOfFreedomBottomFirst = flib.div(caseVariance, flib.sub(caseNum, flib.publicValue(1.0)));
		degreesOfFreedomBottomFirst = flib.multiply(flib.add(zero,degreesOfFreedomBottomFirst), flib.add(zero,degreesOfFreedomBottomFirst));
		degreesOfFreedomBottomFirst = flib.div(degreesOfFreedomBottomFirst, flib.sub(caseNum, flib.publicValue(1.0)));

		T[] degreesOfFreedomBottomSecond = flib.div(controlVariance, flib.sub(caseNum, flib.publicValue(1.0)));
		degreesOfFreedomBottomSecond = flib.multiply(flib.add(zero,degreesOfFreedomBottomSecond), flib.add(zero,degreesOfFreedomBottomSecond));
		degreesOfFreedomBottomSecond = flib.div(degreesOfFreedomBottomSecond, flib.sub(controlNum, flib.publicValue(1.0)));

		T[] degreesOfFreedom = flib.div(degreesOfFreedomTop, flib.add(degreesOfFreedomBottomFirst, degreesOfFreedomBottomSecond));
		res[0] = tStat;
		res[1] = degreesOfFreedom;

		return res;		
	}
	
	public static class Generator<T> extends GenRunnable<T> {

		T[][] simpsonsResAliceCase;
		T[][] simpsonsResBobCase;
		T[][] simpsonsResAliceControl;
		T[][] simpsonsResBobControl;

		T[][] alphaDiversityRes;
		
		T[] aliceCaseNum;
		T[] bobCaseNum;
		T[] aliceControlNum;
		T[] bobControlNum;
		
		boolean[][] inputBobCase;
		boolean[][] inputAliceCase;
		boolean[][] inputCounters;
		boolean[][] inputAliceControl;
		boolean[][] inputBobControl;
		T[] scResult;
		T[][][] in;

		T[] numAliceCase;
		T[] numBobCase;
		T[] numAliceControl;
		T[] numBobControl;
		double[][] caseInput;
		double[][] controlInput;

		int NumFeatures;
		int[] EvaCaseSamples;
		int[] GenCaseSamples;
		int[] EvaControlSamples;
		int[] GenControlSamples;
		int EvaCaseFeaturesNum;
		int GenCaseFeaturesNum;
		int EvaControlFeaturesNum;
		int GenControlFeaturesNum;

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
			IntegerLib<T> flib = new IntegerLib<T>(gen, 32);
			T[] l = flib.publicValue(0.0);
			caseInput = PrepareDataDANaive.readFile(cmd.getOptionValue("s"));
			controlInput = PrepareDataDANaive.readFile(cmd.getOptionValue("t"));

			EvaCaseFeaturesNum = gen.channel.readInt();
			gen.channel.flush();
			GenCaseFeaturesNum = caseInput.length;
			gen.channel.writeInt(GenCaseFeaturesNum);
			gen.channel.flush();
			EvaControlFeaturesNum = gen.channel.readInt();
			gen.channel.flush();
			GenControlFeaturesNum = controlInput.length;
			gen.channel.writeInt(GenControlFeaturesNum);
			gen.channel.flush();
			
			EvaCaseSamples = new int[EvaCaseFeaturesNum];
			GenCaseSamples = new int[GenCaseFeaturesNum];
			EvaControlSamples = new int[EvaControlFeaturesNum];
			GenControlSamples = new int[GenControlFeaturesNum];
			
			for(int i =0; i < GenCaseFeaturesNum; i++){
				int numCase = 0;
				for(int j = 0; j < caseInput[0].length; j++){
					if(caseInput[i][j] > 0.0){
						numCase++;
					}
				}
				GenCaseSamples[i] = numCase;
			}
			for(int i =0; i < GenControlFeaturesNum; i++){
				int numControl = 0;
				for(int j = 0; j < controlInput[0].length; j++){
					if(controlInput[i][j] > 0.0){
						numControl++;
					}
				}
				GenControlSamples[i] = numControl;
			}
			
			for(int i = 0; i < EvaCaseFeaturesNum; i++){
				EvaCaseSamples[i] = gen.channel.readInt();
				gen.channel.flush();
			}
			for(int i = 0; i < GenCaseFeaturesNum; i++){
				gen.channel.writeInt(GenCaseSamples[i]);
				gen.channel.flush();
			}
			for(int i = 0; i < EvaControlFeaturesNum; i++){
				EvaControlSamples[i] = gen.channel.readInt();
				gen.channel.flush();
			}
			for(int i = 0; i < GenControlFeaturesNum; i++){
				gen.channel.writeInt(GenControlSamples[i]);
				gen.channel.flush();
			}

			simpsonsResAliceCase  = gen.newTArray(GenCaseFeaturesNum, 0);
			simpsonsResBobCase  = gen.newTArray(EvaCaseFeaturesNum, 0);
			simpsonsResAliceControl = gen.newTArray(GenControlFeaturesNum, 0);
			simpsonsResBobControl = gen.newTArray(EvaControlFeaturesNum, 0);
			System.out.println(GenCaseFeaturesNum);
			System.out.println(EvaCaseFeaturesNum);
			System.out.println(GenControlFeaturesNum);
			System.out.println(EvaControlFeaturesNum);

		}
		
		@Override
		public void secureCompute(CompEnv<T> gen) {
			FloatLib<T> flib = new FloatLib<T>(gen, PLength, VLength);
			T[] l = flib.publicValue(0.0);
			int caseCounter = 0;
			int controlCounter = 0;
				for(int i = 0; i < GenCaseFeaturesNum; i++){
					inputAliceCase = new boolean[GenCaseSamples[i]][l.length];
					caseCounter = 0;
					for(int j =0; j < caseInput[i].length; j++){
						if(caseInput[i][j] > 0.0){
							inputAliceCase[caseCounter++] = Utils.fromFloat(caseInput[i][j], PLength, VLength);
						}
					}
					gen.flush();
					simpsonsResAliceCase[i] = computeSimpsons(gen, gen.inputOfAlice(inputAliceCase));
				}
				for(int i = 0; i < EvaCaseFeaturesNum; i++){
					inputBobCase = new boolean[EvaCaseSamples[i]][l.length];
					gen.flush();
					simpsonsResBobCase[i] = computeSimpsons(gen, gen.inputOfBob(inputBobCase));
				}
				for(int i = 0; i < GenControlFeaturesNum; i++){
					inputAliceControl = new boolean[GenControlSamples[i]][l.length];
					controlCounter = 0;
					for(int j =0; j < controlInput[i].length; j++){
						if(controlInput[i][j] > 0.0){
							inputAliceControl[controlCounter++] = Utils.fromFloat(controlInput[i][j], PLength, VLength);

						}
					}
					gen.flush();
					simpsonsResAliceControl[i] = computeSimpsons(gen, gen.inputOfAlice(inputAliceControl));
				}
				for(int i = 0; i < EvaControlFeaturesNum; i++){
					inputBobControl = new boolean[EvaControlSamples[i]][l.length];
					gen.flush();
					simpsonsResBobControl[i] = computeSimpsons(gen, gen.inputOfBob(inputBobControl));
				}
			
			alphaDiversityRes = compute(gen, simpsonsResAliceCase, simpsonsResBobCase, 
					simpsonsResAliceControl, simpsonsResBobControl);
		}
		
		@Override
		public void prepareOutput(CompEnv<T> gen) {
			FloatLib<T> flib = new FloatLib<T>(gen, PLength, VLength);
			double tStat = flib.outputToAlice(alphaDiversityRes[0]);
			double df = flib.outputToAlice(alphaDiversityRes[1]);
			if (tStat == 0.0){
				System.out.println("NA,NA,NA");
				return;
			}
			if (df <= 0.0){
				System.out.println(tStat +",NA,NA");
				return;
			}
			TDistribution tDistribution = new TDistribution(df);
			if(tStat > 0.0)
				System.out.println(tStat + "," + df + "," + (1-tDistribution.cumulativeProbability(tStat))*2.0);
			else
				System.out.println(tStat + "," + df + "," +  tDistribution.cumulativeProbability(tStat)*2.0);
			
			
		}	
	}
	
	public static class Evaluator<T> extends EvaRunnable<T> {
		T[][] simpsonsResAliceCase;
		T[][] simpsonsResBobCase;
		T[][] simpsonsResAliceControl;
		T[][] simpsonsResBobControl;

		T[][] alphaDiversityRes;
		
		T[] aliceCaseNum;
		T[] bobCaseNum;
		T[] aliceControlNum;
		T[] bobControlNum;
		
		boolean[][] inputBobCase;
		boolean[][] inputAliceCase;
		T[][] inputCounters;
		boolean[][] inputAliceControl;
		boolean[][] inputBobControl;
		T[] scResult;
		T[][][] in;

		T[] numAliceCase;
		T[] numBobCase;
		T[] numAliceControl;
		T[] numBobControl;
		double[][] caseInput;
		double[][] controlInput;

		int NumFeatures;
		int[] EvaCaseSamples;
		int[] GenCaseSamples;
		int[] EvaControlSamples;
		int[] GenControlSamples;
		int EvaCaseFeaturesNum;
		int GenCaseFeaturesNum;
		int EvaControlFeaturesNum;
		int GenControlFeaturesNum;
		
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

			IntegerLib<T> flib = new IntegerLib<T>(gen, 32);
			T[] l = flib.publicValue(0.0);
			caseInput = PrepareDataDANaive.readFile(cmd.getOptionValue("s"));
			controlInput = PrepareDataDANaive.readFile(cmd.getOptionValue("t"));

			EvaCaseFeaturesNum = caseInput.length;
			gen.channel.writeInt(EvaCaseFeaturesNum);
			gen.channel.flush();	
			GenCaseFeaturesNum = gen.channel.readInt();
			gen.channel.flush();
			EvaControlFeaturesNum = controlInput.length;
			gen.channel.writeInt(EvaControlFeaturesNum);
			gen.channel.flush();
			GenControlFeaturesNum = gen.channel.readInt();
			gen.channel.flush();
			
			EvaCaseSamples = new int[EvaCaseFeaturesNum];
			GenCaseSamples = new int[GenCaseFeaturesNum];
			EvaControlSamples = new int[EvaControlFeaturesNum];
			GenControlSamples = new int[GenControlFeaturesNum];
			
			for(int i =0; i < EvaCaseFeaturesNum; i++){
				int numCase = 0;
				for(int j = 0; j < caseInput[0].length; j++){
					if(caseInput[i][j] > 0.0){
						numCase++;
					}
				}
				EvaCaseSamples[i] = numCase;
			}
			
			for(int i =0; i < EvaControlFeaturesNum; i++){
				int numControl = 0;
				for(int j = 0; j < controlInput[0].length; j++){
					if(controlInput[i][j] > 0.0){
						numControl++;
					}
				}
				EvaControlSamples[i] = numControl;
			}

			for(int i = 0; i < EvaCaseFeaturesNum; i++){		
				gen.channel.writeInt(EvaCaseSamples[i]);
				gen.channel.flush();
			}
			for(int i = 0; i < GenCaseFeaturesNum; i++){		
				GenCaseSamples[i] = gen.channel.readInt();
				gen.channel.flush();
			}
			for(int i = 0; i < EvaControlFeaturesNum; i++){		
				gen.channel.writeInt(EvaControlSamples[i]);
				gen.channel.flush();
			}
			for(int i = 0; i < GenControlFeaturesNum; i++){		
				GenControlSamples[i] = gen.channel.readInt();
				gen.channel.flush();
			}

			simpsonsResAliceCase  = gen.newTArray(GenCaseFeaturesNum, 0);
			simpsonsResBobCase  = gen.newTArray(EvaCaseFeaturesNum, 0);
			simpsonsResAliceControl = gen.newTArray(GenControlFeaturesNum, 0);
			simpsonsResBobControl = gen.newTArray(EvaControlFeaturesNum, 0);

			System.out.println(GenCaseFeaturesNum);
			System.out.println(EvaCaseFeaturesNum);
			System.out.println(GenControlFeaturesNum);
			System.out.println(EvaControlFeaturesNum);

		}
		
		@Override
		public void secureCompute(CompEnv<T> gen) {
			FloatLib<T> flib = new FloatLib<T>(gen, PLength, VLength);
			T[] l = flib.publicValue(0.0);
			int caseCounter = 0;
			int controlCounter = 0;
				for(int i =0; i < GenCaseFeaturesNum; i++){
					inputAliceCase = new boolean[GenCaseSamples[i]][l.length];
					gen.flush();
					simpsonsResAliceCase[i] = computeSimpsons(gen, gen.inputOfAlice(inputAliceCase));
				}
				for(int i =0; i < EvaCaseFeaturesNum; i++){
					inputBobCase = new boolean[EvaCaseSamples[i]][l.length];
					caseCounter = 0;
					for(int j =0; j < caseInput[i].length; j++){
						if(caseInput[i][j] > 0.0){
							inputBobCase[caseCounter++] = Utils.fromFloat(caseInput[i][j], PLength, VLength);
						}
					}
					gen.flush();
					simpsonsResBobCase[i] = computeSimpsons(gen, gen.inputOfBob(inputBobCase));
				}
				for(int i =0; i < GenControlFeaturesNum; i++){
					inputAliceControl = new boolean[GenControlSamples[i]][l.length];
					gen.flush();
					simpsonsResAliceControl[i] = computeSimpsons(gen, gen.inputOfAlice(inputAliceControl));
				}
				for(int i =0; i < EvaControlFeaturesNum; i++){
					inputBobControl = new boolean[EvaControlSamples[i]][l.length];
					controlCounter = 0;
					for(int j =0; j < controlInput[i].length; j++){
						if(controlInput[i][j] > 0.0){
							inputBobControl[controlCounter++] = Utils.fromFloat(controlInput[i][j], PLength, VLength);
						}
					}
					gen.flush();
					simpsonsResBobControl[i] = computeSimpsons(gen, gen.inputOfBob(inputBobControl));
				}
			
			alphaDiversityRes = compute(gen, simpsonsResAliceCase, simpsonsResBobCase, 
					simpsonsResAliceControl, simpsonsResBobControl);
		}
		
		@Override
		public void prepareOutput(CompEnv<T> gen) {
			FloatLib<T> flib = new FloatLib<T>(gen, PLength, VLength);
			flib.outputToAlice(alphaDiversityRes[0]);
			flib.outputToAlice(alphaDiversityRes[1]);
		}
				
	}
}
