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
package matrixSparse;

import java.util.Arrays;
import java.util.Comparator;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.math3.distribution.TDistribution;

import util.EvaRunnable;
import util.GenRunnable;
import util.Utils;
import circuits.BitonicSortLib;
import circuits.arithmetic.FloatLib;
import circuits.arithmetic.IntegerLib;
import flexsc.CompEnv;

public class AlphaDiversity {
	static int width = 54;
	static int offset = 11;
	static public<T> T[][] compute(CompEnv<T> gen, T[][] inputCounters, T[][][] inputAliceCase, 
			T[][][] inputBobCase, T[][][] inputAliceControl, T[][][] inputBobControl,
			T[] aliceCaseNum, T[] bobCaseNum, T[] aliceControlNum, T[] bobControlNum){

		BitonicSortLib<T> lib = new BitonicSortLib<T>(gen);
		IntegerLib<T> ilib = new IntegerLib<T>(gen);
		FloatLib<T> flib = new FloatLib<T>(gen, width, offset);
		T [] zero = flib.publicValue(0.0);
		T [] one = flib.publicValue(1.0);
		T [] pointOne = flib.publicValue(0.000001);
		int [] rows = {1,2};
		
		T[] holder =  flib.publicValue(0.0);
		T[] holder2 =  flib.publicValue(0.0);
		T[] holder3 =  flib.publicValue(0.0);

		for(int i = (inputAliceCase[0].length-2); i >= 0; i--){
			holder = inputAliceCase[0][i+1];
			T rowIdsEqual = ilib.not(flib.eq(inputAliceCase[0][i], holder));
			T[] addAndIncrement = flib.add(inputAliceCase[1][i], inputAliceCase[1][i+1]);
			T[] i2multiplied = flib.multiply(flib.add(pointOne,inputAliceCase[2][i]), 
					flib.add(pointOne,flib.sub(inputAliceCase[2][i], one)));
			i2multiplied = flib.sub(i2multiplied, flib.multiply(inputAliceCase[2][i], pointOne));
			T[] addAndMultiply = flib.add(i2multiplied, inputAliceCase[2][i+1]);
			inputAliceCase[1][i] = ilib.mux(addAndIncrement, inputAliceCase[1][i], rowIdsEqual);
			inputAliceCase[2][i] = ilib.mux(addAndMultiply, i2multiplied, rowIdsEqual);
			inputAliceCase[0][i+1] = ilib.mux(zero, holder, rowIdsEqual);
			inputAliceCase[1][i+1] = ilib.mux(zero, holder2, rowIdsEqual);
			inputAliceCase[2][i+1] = ilib.mux(zero, holder3, rowIdsEqual);
			holder2 = inputAliceCase[1][i];
			holder3 = inputAliceCase[2][i];
		}
		
		lib.sortWithPayloadM(inputAliceCase[0], inputAliceCase, rows, lib.SIGNAL_ZERO);

		holder =  flib.publicValue(0.0);
		holder2 =  flib.publicValue(0.0);
		holder3 =  flib.publicValue(0.0);

		for(int i = (inputBobCase[0].length-2); i >= 0; i--){
			holder = inputBobCase[0][i+1];
			T rowIdsEqual = ilib.not(flib.eq(inputBobCase[0][i], holder));
			T[] addAndIncrement = flib.add(inputBobCase[1][i], inputBobCase[1][i+1]);
			T[] i2multiplied = flib.multiply(flib.add(pointOne,inputBobCase[2][i]), 
					flib.add(pointOne,flib.sub(inputBobCase[2][i], one)));
			i2multiplied = flib.sub(i2multiplied, flib.multiply(inputBobCase[2][i], pointOne));

			T[] addAndMultiply = flib.add(i2multiplied, inputBobCase[2][i+1]);
			inputBobCase[1][i] = ilib.mux(addAndIncrement, inputBobCase[1][i], rowIdsEqual);
			inputBobCase[2][i] = ilib.mux(addAndMultiply, i2multiplied, rowIdsEqual);
			inputBobCase[0][i+1] = ilib.mux(zero, holder, rowIdsEqual);
			inputBobCase[1][i+1] = ilib.mux(zero, holder2, rowIdsEqual);
			inputBobCase[2][i+1] = ilib.mux(zero, holder3, rowIdsEqual);
			holder2 = inputBobCase[1][i];
			holder3 = inputBobCase[2][i];
		}
		
		lib.sortWithPayloadM(inputBobCase[0], inputBobCase, rows, lib.SIGNAL_ZERO);
		
		holder =  flib.publicValue(0.0);
		holder2 =  flib.publicValue(0.0);
		holder3 =  flib.publicValue(0.0);
		
		for(int i = (inputAliceControl[0].length-2); i >= 0; i--){
			holder = inputAliceControl[0][i+1];
			T rowIdsEqual = ilib.not(flib.eq(inputAliceControl[0][i], holder));
			T[] addAndIncrement = flib.add(inputAliceControl[1][i], inputAliceControl[1][i+1]);
			T[] i2multiplied = flib.multiply(flib.add(pointOne,inputAliceControl[2][i]), 
					flib.add(pointOne,flib.sub(inputAliceControl[2][i], one)));
			i2multiplied = flib.sub(i2multiplied, flib.multiply(inputAliceControl[2][i], pointOne));

			T[] addAndMultiply = flib.add(i2multiplied, inputAliceControl[2][i+1]);
			inputAliceControl[1][i] = ilib.mux(addAndIncrement, inputAliceControl[1][i], rowIdsEqual);
			inputAliceControl[2][i] = ilib.mux(addAndMultiply, i2multiplied, rowIdsEqual);
			inputAliceControl[0][i+1] = ilib.mux(zero, holder, rowIdsEqual);
			inputAliceControl[1][i+1] = ilib.mux(zero, holder2, rowIdsEqual);
			inputAliceControl[2][i+1] = ilib.mux(zero, holder3, rowIdsEqual);
			holder2 = inputAliceControl[1][i];
			holder3 = inputAliceControl[2][i];
		}
		
		lib.sortWithPayloadM(inputAliceControl[0], inputAliceControl, rows, lib.SIGNAL_ZERO);

		holder =  flib.publicValue(0.0);
		holder2 =  flib.publicValue(0.0);
		holder3 =  flib.publicValue(0.0);

		for(int i = (inputBobControl[0].length-2); i >= 0; i--){
			holder = inputBobControl[0][i+1];
			T rowIdsEqual = ilib.not(flib.eq(inputBobControl[0][i], holder));
			T[] addAndIncrement = flib.add(inputBobControl[1][i], inputBobControl[1][i+1]);
			T[] i2multiplied = flib.multiply(flib.add(pointOne,inputBobControl[2][i]), 
					flib.add(pointOne,flib.sub(inputBobControl[2][i], one)));
			T[] addAndMultiply = flib.add(i2multiplied, inputBobControl[2][i+1]);
			i2multiplied = flib.sub(i2multiplied, flib.multiply(inputBobControl[2][i], pointOne));

			inputBobControl[1][i] = ilib.mux(addAndIncrement, inputBobControl[1][i], rowIdsEqual);
			inputBobControl[2][i] = ilib.mux(addAndMultiply, i2multiplied, rowIdsEqual);
			inputBobControl[0][i+1] = ilib.mux(zero, holder, rowIdsEqual);
			inputBobControl[1][i+1] = ilib.mux(zero, holder2, rowIdsEqual);
			inputBobControl[2][i+1] = ilib.mux(zero, holder3, rowIdsEqual);
			holder2 = inputBobControl[1][i];
			holder3 = inputBobControl[2][i];
		}
		
		lib.sortWithPayloadM(inputBobControl[0], inputBobControl, rows, lib.SIGNAL_ZERO);
		
		T[][] res = gen.newTArray(2, 0);
		
		for(int i = 0; i < inputCounters.length; i++){
			inputAliceCase[0][i] = flib.sub(one, flib.div(inputAliceCase[2][i], flib.multiply(inputAliceCase[1][i], flib.sub(inputAliceCase[1][i], one))));
		}

		for(int i = 0; i < inputCounters.length; i++){
			inputBobCase[0][i] = flib.sub(one, flib.div(inputBobCase[2][i], flib.multiply(inputBobCase[1][i], flib.sub(inputBobCase[1][i], one))));
		}
		
		for(int i = 0; i < inputCounters.length; i++){
			inputAliceControl[0][i] = flib.sub(one, flib.div(inputAliceControl[2][i], flib.multiply(inputAliceControl[1][i], flib.sub(inputAliceControl[1][i], one))));
		}

		for(int i = 0; i < inputCounters.length; i++){
			inputBobControl[0][i] = flib.sub(one, flib.div(inputBobControl[2][i], flib.multiply(inputBobControl[1][i], flib.sub(inputBobControl[1][i], one))));
		}
		
		T[] caseTotalSum = flib.publicValue(0.0);
		T[] caseSumOfSquares = flib.publicValue(0.0);
		T[] controlTotalSum = flib.publicValue(0.0);
		T[] controlSumOfSquares = flib.publicValue(0.0);
		T[] caseNum = flib.add(ilib.toSecureFloat(aliceCaseNum, flib), ilib.toSecureFloat(bobCaseNum, flib));
		T[] controlNum = flib.add(ilib.toSecureFloat(aliceControlNum, flib), ilib.toSecureFloat(bobControlNum, flib));

		T[] tStat;
		for(int i = 0; i < inputCounters.length; i++){	
			caseTotalSum = flib.add(caseTotalSum, inputAliceCase[0][i]);
			caseTotalSum = flib.add(caseTotalSum, inputBobCase[0][i]);

			caseSumOfSquares = flib.add(caseSumOfSquares, flib.multiply(flib.add(zero, inputAliceCase[0][i]),flib.add(zero, inputAliceCase[0][i])));
			caseSumOfSquares = flib.add(caseSumOfSquares, flib.multiply(flib.add(zero, inputBobCase[0][i]),flib.add(zero, inputBobCase[0][i])));
		}
		
		for(int i = 0; i < inputCounters.length; i++){	
			controlTotalSum = flib.add(controlTotalSum, inputAliceControl[0][i]);
			controlTotalSum = flib.add(controlTotalSum, inputBobControl[0][i]);
			controlSumOfSquares = flib.add(controlSumOfSquares, flib.multiply(flib.add(zero,inputAliceControl[0][i]), flib.add(zero,inputAliceControl[0][i])));
			controlSumOfSquares = flib.add(controlSumOfSquares, flib.multiply(flib.add(zero,inputBobControl[0][i]), flib.add(zero,inputBobControl[0][i])));
		}


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

		caseMeanAbundance = flib.div(caseTotalSum, caseNum);
		caseVarianceSecondTerm = flib.div(flib.multiply(flib.add(zero, caseTotalSum), flib.add(zero,caseTotalSum)), caseNum);
		caseVariance = flib.div(flib.sub(caseSumOfSquares, caseVarianceSecondTerm), caseNum);
		controlMeanAbundance = flib.div(controlTotalSum, controlNum);		    
		controlVarianceSecondTerm = flib.div(flib.multiply(flib.add(zero, controlTotalSum), flib.add(zero, controlTotalSum)), controlNum);
		controlVariance = flib.div(flib.sub(controlSumOfSquares, controlVarianceSecondTerm), controlNum);

		tUpper = flib.sub(controlMeanAbundance, caseMeanAbundance);
		tLowerFirst = flib.div(caseVariance, caseNum);
		tLowerSecond = flib.div(controlVariance, controlNum);
		tLowerSqrt = flib.sqrt(flib.add(tLowerFirst, tLowerSecond));
		tStat = flib.div(tUpper, tLowerSqrt);

		T[] degreesOfFreedomTop = flib.add(tLowerFirst, tLowerSecond);
		degreesOfFreedomTop = flib.multiply(flib.add(zero, degreesOfFreedomTop), flib.add(zero, degreesOfFreedomTop));

		T[] degreesOfFreedomBottomFirst = flib.div(caseVariance, flib.sub(caseNum, flib.publicValue(1.0)));
		degreesOfFreedomBottomFirst = flib.multiply(flib.add(zero, degreesOfFreedomBottomFirst), flib.add(zero, degreesOfFreedomBottomFirst));
		degreesOfFreedomBottomFirst = flib.div(degreesOfFreedomBottomFirst, flib.sub(caseNum, flib.publicValue(1.0)));

		T[] degreesOfFreedomBottomSecond = flib.div(controlVariance, flib.sub(caseNum, flib.publicValue(1.0)));
		degreesOfFreedomBottomSecond = flib.multiply(flib.add(zero, degreesOfFreedomBottomSecond), flib.add(zero, degreesOfFreedomBottomSecond));
		degreesOfFreedomBottomSecond = flib.div(degreesOfFreedomBottomSecond, flib.sub(controlNum, flib.publicValue(1.0)));

		T[] degreesOfFreedom = flib.div(degreesOfFreedomTop, flib.add(degreesOfFreedomBottomFirst, degreesOfFreedomBottomSecond));
		res[0] = tStat;
		res[1] = degreesOfFreedom;
		return res;
	}
	
	public static class Generator<T> extends GenRunnable<T> {
		T[][][] inputBobCase;
		T[][][] inputAliceCase;
		T[][][] inputAliceControl;
		T[][][] inputBobControl;
		T[][] inputCounters;
		T[][] in;
		
		T[] aliceCaseNum;
		T[] bobCaseNum;
		T[] aliceControlNum;
		T[] bobControlNum;

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
			FloatLib<T> flib = new FloatLib<T>(gen, width, offset);
			T[] l = flib.publicValue(0.0);
			double[][] caseInput = PrepareDataDA.readFile(cmd.getOptionValue("s"));
			double[][] controlInput = PrepareDataDA.readFile(cmd.getOptionValue("t"));
			int numCounters = (int)caseInput[0][1];
			System.out.println("num counters: " + numCounters);
			int aliceCaseNumInt = (int)caseInput[0][0];			
			System.out.println("alice case num: " + aliceCaseNumInt);
			int aliceControlNumInt = (int)controlInput[0][0];
			System.out.println("alice control num: " + aliceControlNumInt);

			aliceCaseNum = gen.inputOfAlice(Utils.fromInt((int)Math.round(caseInput[0][1]), 32));
			bobCaseNum = gen.inputOfBob(new boolean[32]);
			aliceControlNum = gen.inputOfAlice(Utils.fromInt((int)Math.round(controlInput[0][1]), 32));
			bobControlNum = gen.inputOfBob(new boolean[32]);

			Comparator<Double[]> comparator = new Comparator<Double[]>(){
				@Override
				public int compare(Double[] a, Double[] b){
					return Double.compare(a[0], b[0]);
				}
			};
			int EvaCaseNum = gen.channel.readInt();
			gen.channel.flush();
			int GenCaseNum = caseInput[0].length-2;
			gen.channel.writeInt(GenCaseNum);
			gen.channel.flush();
			int EvaControlNum = gen.channel.readInt();
			gen.channel.flush();
			int GenControlNum = controlInput[0].length-2;
			gen.channel.writeInt(GenControlNum);
			gen.channel.flush();

			System.out.println(numCounters);

			inputCounters = gen.newTArray(numCounters, 0);
			for(int i = 0; i < numCounters; i++){
				inputCounters[i] = gen.inputOfBob(Utils.fromFloat(i+1, width, offset));
			}
			gen.flush();
			System.out.println("Done with inputCounters gen");
			
			Double[][] caseIn = new Double[GenCaseNum+numCounters][3];
			for(int i = 0; i < numCounters; i++){
				caseIn[i][0] = new Double(i+1);
				caseIn[i][1] = new Double(0);
				caseIn[i][2] = new Double(0);
			}			
			for(int i = 0; i < GenCaseNum; i++){
				caseIn[i+numCounters][0] = caseInput[0][i+2];
				caseIn[i+numCounters][1] = caseInput[1][i+2];
				caseIn[i+numCounters][2] = caseInput[1][i+2];
			}

			Arrays.sort(caseIn, comparator);
			System.out.println(GenCaseNum+numCounters);
			inputAliceCase = gen.newTArray(3, GenCaseNum+numCounters, 0);			
			for(int i = 0; i < GenCaseNum+numCounters; i++){
				inputAliceCase[0][i] = gen.inputOfAlice(Utils.fromFloat(caseIn[i][0].doubleValue(), width, offset));
			}
			for (int i = 0; i < GenCaseNum+numCounters; i++){
				inputAliceCase[1][i] = gen.inputOfAlice(Utils.fromFloat(caseIn[i][1].doubleValue(), width, offset));
			}
			for (int i = 0; i < GenCaseNum+numCounters; i++){
				inputAliceCase[2][i] = gen.inputOfAlice(Utils.fromFloat(caseIn[i][2].doubleValue(), width, offset));
			}
			
			gen.flush();
			System.out.println("Done with inputAliceCase gen");

			inputBobCase = gen.newTArray(3, EvaCaseNum+numCounters, 0);
			for (int j = 0; j < EvaCaseNum+numCounters; j++) {
				inputBobCase[0][j] = gen.inputOfBob(new boolean[l.length]);
			}
			for (int j = 0; j < EvaCaseNum+numCounters; j++) {
				inputBobCase[1][j] = gen.inputOfBob(new boolean[l.length]);
			}
			for (int j = 0; j < EvaCaseNum+numCounters; j++) {
				inputBobCase[2][j] = gen.inputOfBob(new boolean[l.length]);
			}
			gen.flush();
			System.out.println("Done with inputBobCase gen");

			Double[][] controlIn = new Double[GenControlNum+numCounters][3];
			for(int i = 0; i < numCounters; i++){
				controlIn[i][0] = new Double(i+1);
				controlIn[i][1] = new Double(0);
				controlIn[i][2] = new Double(0);
			}			
			for(int i = 0; i < GenControlNum; i++){
				controlIn[i+numCounters][0] = controlInput[0][i+2];
				controlIn[i+numCounters][1] = controlInput[1][i+2];
				controlIn[i+numCounters][2] = controlInput[1][i+2];
			}

			Arrays.sort(controlIn, comparator);
			inputAliceControl = gen.newTArray(3, GenControlNum+numCounters, 0);			
			for(int i = 0; i < GenControlNum+numCounters; i++){
				inputAliceControl[0][i] = gen.inputOfAlice(Utils.fromFloat(controlIn[i][0].doubleValue(), width, offset));
			}
			for(int i = 0; i < GenControlNum+numCounters; i++){
				inputAliceControl[1][i] = gen.inputOfAlice(Utils.fromFloat(controlIn[i][1].doubleValue(), width, offset));
			}
			for(int i = 0; i < GenControlNum+numCounters; i++){
				inputAliceControl[2][i] = gen.inputOfAlice(Utils.fromFloat(controlIn[i][2].doubleValue(), width, offset));
			}			
			gen.flush();
			System.out.println("Done with inputAliceControl gen");

			inputBobControl = gen.newTArray(3, EvaControlNum+numCounters, 0);
			for (int j = 0; j < EvaControlNum+numCounters; j++) {
				inputBobControl[0][j] = gen.inputOfBob(new boolean[l.length]);
			}
			for (int j = 0; j < EvaControlNum+numCounters; j++) {
				inputBobControl[1][j] = gen.inputOfBob(new boolean[l.length]);
			}
			for (int j = 0; j < EvaControlNum+numCounters; j++) {
				inputBobControl[2][j] = gen.inputOfBob(new boolean[l.length]);
			}
			gen.flush();
			System.out.println("Done with inputBobControl gen");
		}
		
		@Override
		public void secureCompute(CompEnv<T> gen) {
			in = compute(gen, inputCounters, inputAliceCase, inputBobCase, inputAliceControl, inputBobControl,
					aliceCaseNum, bobCaseNum, aliceControlNum, bobControlNum);
		}
		@Override
		public void prepareOutput(CompEnv<T> gen) {
			FloatLib<T> flib = new FloatLib<T>(gen, width, offset);

			double tStat = flib.outputToAlice(in[0]);
			double df = flib.outputToAlice(in[1]);
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
		T[][][] inputBobCase;
		T[][][] inputAliceCase;
		T[][] inputCounters;
		T[][][] inputAliceControl;
		T[][][] inputBobControl;
		T[] scResult;
		T[][] in;
		T[] aliceCaseNum;
		T[] bobCaseNum;
		T[] aliceControlNum;
		T[] bobControlNum;
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

			FloatLib<T> flib = new FloatLib<T>(gen, width, offset);
			T[] l = flib.publicValue(0.0);
			double[][] caseInput = PrepareDataDA.readFile(cmd.getOptionValue("s"));
			double[][] controlInput = PrepareDataDA.readFile(cmd.getOptionValue("t"));
			int numCounters = (int)caseInput[0][1];
			int bobCaseNumInt = (int)caseInput[0][0];
			int bobControlNumInt = (int)controlInput[0][0];

			aliceCaseNum = gen.inputOfAlice(new boolean[32]);
			bobCaseNum = gen.inputOfBob(Utils.fromInt((int)Math.round(caseInput[0][1]), 32));
			aliceControlNum = gen.inputOfAlice(new boolean[32]);
			bobControlNum = gen.inputOfBob(Utils.fromInt((int)Math.round(controlInput[0][1]), 32));
			Comparator<Double[]> comparator = new Comparator<Double[]>(){
				@Override
				public int compare(Double[] a, Double[] b){
					return Double.compare(a[0], b[0]);
				}
			};
			int EvaCaseNum = caseInput[0].length-2;
			gen.channel.writeInt(EvaCaseNum);
			gen.channel.flush();
			int GenCaseNum = gen.channel.readInt();
			gen.channel.flush();
			int EvaControlNum = controlInput[0].length-2;
			gen.channel.writeInt(EvaControlNum);
			gen.channel.flush();
			int GenControlNum = gen.channel.readInt();
			gen.channel.flush();

			System.out.println(numCounters);
			System.out.println(EvaControlNum);
			gen.flush();
			inputCounters = gen.newTArray(numCounters, 0);
			for (int j = 0; j < numCounters; j++) {
				inputCounters[j] = gen.inputOfBob(new boolean[l.length]);
			}
			gen.flush();
			System.out.println("Done with inputCounters eva");
			System.out.println(GenCaseNum+numCounters);

			inputAliceCase = gen.newTArray(3, GenCaseNum+numCounters, 0);
			for (int j = 0; j < GenCaseNum+numCounters; j++) {
				inputAliceCase[0][j] = gen.inputOfAlice(new boolean[l.length]);
			}
			for (int j = 0; j < GenCaseNum+numCounters; j++) {
				inputAliceCase[1][j] = gen.inputOfAlice(new boolean[l.length]);
			}
			for (int j = 0; j < GenCaseNum+numCounters; j++) {
				inputAliceCase[2][j] = gen.inputOfAlice(new boolean[l.length]);
			}
			gen.flush();
			System.out.println("Done with inputAliceCase eva");
			
			Double[][] caseIn = new Double[EvaCaseNum+numCounters][3];
			for(int i = 0; i < numCounters; i++){
				caseIn[i][0] = new Double(i+1);
				caseIn[i][1] = new Double(0);
				caseIn[i][2] = new Double(0);
			}			
			for(int i = 0; i < EvaCaseNum; i++){
				caseIn[i+numCounters][0] = caseInput[0][i+2];
				caseIn[i+numCounters][1] = caseInput[1][i+2];
				caseIn[i+numCounters][2] = caseInput[1][i+2];
			}

			Arrays.sort(caseIn, comparator);
			System.out.println("Done with sorting case eva");

			inputBobCase = gen.newTArray(3, EvaCaseNum+numCounters, 0);			
			for(int i = 0; i < EvaCaseNum+numCounters; i++){
				inputBobCase[0][i] = gen.inputOfBob(Utils.fromFloat(caseIn[i][0].doubleValue(), width, offset));
			}
			for(int i = 0; i < EvaCaseNum+numCounters; i++){
				inputBobCase[1][i] = gen.inputOfBob(Utils.fromFloat(caseIn[i][1].doubleValue(), width, offset));
			}
			for(int i = 0; i < EvaCaseNum+numCounters; i++){
				inputBobCase[2][i] = gen.inputOfBob(Utils.fromFloat(caseIn[i][2].doubleValue(), width, offset));
			}
			gen.flush();
			System.out.println("Done with inputBobCase eva");

			inputAliceControl = gen.newTArray(3, GenControlNum+numCounters, 0);
			for (int j = 0; j < GenControlNum+numCounters; j++) {
				inputAliceControl[0][j] = gen.inputOfAlice(new boolean[l.length]);
			}
			for (int j = 0; j < GenControlNum+numCounters; j++) {
				inputAliceControl[1][j] = gen.inputOfAlice(new boolean[l.length]);
			}
			for (int j = 0; j < GenControlNum+numCounters; j++) {
				inputAliceControl[2][j] = gen.inputOfAlice(new boolean[l.length]);
			}
			gen.flush();
			System.out.println("Done with inputAliceControl eva");

			Double[][] controlIn = new Double[EvaControlNum+numCounters][3];
			for(int i = 0; i < numCounters; i++){
				controlIn[i][0] = new Double(i+1);
				controlIn[i][1] = new Double(0);
				controlIn[i][2] = new Double(0);
			}			
			for(int i = 0; i < EvaControlNum; i++){
				controlIn[i+numCounters][0] = caseInput[0][i+2];
				controlIn[i+numCounters][1] = caseInput[1][i+2];
				controlIn[i+numCounters][2] = caseInput[1][i+2];
			}

			Arrays.sort(controlIn, comparator);
			inputBobControl = gen.newTArray(3, EvaControlNum+numCounters, 0);			
			for(int i = 0; i < EvaControlNum+numCounters; i++){
				inputBobControl[0][i] = gen.inputOfBob(Utils.fromFloat(controlIn[i][0].doubleValue(), width, offset));
			}
			for(int i = 0; i < EvaControlNum+numCounters; i++){
				inputBobControl[1][i] = gen.inputOfBob(Utils.fromFloat(controlIn[i][1].doubleValue(), width, offset));
			}
			for(int i = 0; i < EvaControlNum+numCounters; i++){
				inputBobControl[2][i] = gen.inputOfBob(Utils.fromFloat(controlIn[i][2].doubleValue(), width, offset));
			}
			gen.flush();
			System.out.println("Done with inputBobControl eva");
		}
		
		@Override
		public void secureCompute(CompEnv<T> gen) {
			in = compute(gen, inputCounters, inputAliceCase, inputBobCase, inputAliceControl, inputBobControl,
					aliceCaseNum, bobCaseNum, aliceControlNum, bobControlNum);
		}
		
		@Override
		public void prepareOutput(CompEnv<T> gen) {
			FloatLib<T> flib = new FloatLib<T>(gen, width, offset);
			flib.outputToAlice(in[0]);
			flib.outputToAlice(in[1]);			
		}
				
	}
	
}
