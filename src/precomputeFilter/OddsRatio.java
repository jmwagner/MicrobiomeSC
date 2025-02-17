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

package precomputeFilter;

import java.util.ArrayList;
import java.util.HashSet;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.math3.distribution.ChiSquaredDistribution;

import precomputeFilter.PrepareDataOddsRatio;
import precomputeFilter.PrepareDataOddsRatio.StatisticsData;
import util.EvaRunnable;
import util.GenRunnable;
import util.Utils;
import circuits.CircuitLib;
import circuits.arithmetic.FloatLib;
import circuits.arithmetic.IntegerLib;
import flexsc.CompEnv;

public class OddsRatio {
	static public int Width = 32;
	static public int FWidth = 54;
	static public int FOffset = 11;
	static int filterThreshold = 5;

	static public<T> T[] filter(CompEnv<T> gen, T[][][] aliceCasePresent,
			T[][][] bobCasePresent,
			T[][][] aliceControlPresent,
			T[][][] bobControlPresent, int numOfTests){

		IntegerLib<T> ilib = new IntegerLib<T>(gen, 32);
		T[] threshold = ilib.publicValue(filterThreshold);
		T[] filterResults = gen.newTArray(numOfTests);
		T[] caseNum;
		T[] controlNum;
		T[] totalPresent;
		T aboveThreshold;
		for(int i = 0; i < numOfTests; i++){			
			caseNum = ilib.add(aliceCasePresent[i][0], bobCasePresent[i][0]);
			controlNum = ilib.add(aliceControlPresent[i][0], bobControlPresent[i][0]);
			totalPresent = ilib.add(caseNum, controlNum);
			aboveThreshold = ilib.geq(totalPresent, threshold);
			filterResults[i] = aboveThreshold;
		}
		
		return filterResults;
	}
	
	public static<T> T[][] compute(CompEnv<T> gen, T[][][] aliceCase,
			T[][][] bobCase,
			T[][][] aliceControl,
			T[][][] bobControl, int numOfTests) {

		T[][] res = gen.newTArray(numOfTests, 0);

		IntegerLib<T> lib = new IntegerLib<T>(gen);
		FloatLib<T> flib = new FloatLib<T>(gen, FWidth, FOffset);
		T[] zero = flib.publicValue(0.0);
		for(int i = 0; i < numOfTests; i++){			
			T[] a = lib.add(aliceCase[i][0], bobCase[i][0]);
			T[] b = lib.add(aliceCase[i][1], bobCase[i][1]);
			T[] c = lib.add(aliceControl[i][0], bobControl[i][0]);
			T[] d = lib.add(aliceControl[i][1], bobControl[i][1]);

			T[] fa = lib.toSecureFloat(a, flib);
			T[] fb = lib.toSecureFloat(b, flib);
			T[] fc = lib.toSecureFloat(c, flib);
			T[] fd = lib.toSecureFloat(d, flib);

			res[i] = flib.div(flib.multiply(fa, fd), flib.multiply(fb, fc));
		}
		
		return res;
	}
	
	public static class Generator<T> extends GenRunnable<T> {
		T[][][] aliceCase;
		T[][][] bobCase;
		T[][][] aliceControl;
		T[][][] bobControl;
		T[][][] aliceCaseFiltered;
		T[][][] bobCaseFiltered;
		T[][][] aliceControlFiltered;
		T[][][] bobControlFiltered;
		T[][][] aliceCaseHolder;
		T[][][] aliceControlHolder;

		T[][][] bobCaseHolder;
		T[][][] bobControlHolder;

		
		int numOfTests;
		boolean precise;
		T[]filterRes;
		double extraFactor;
		@Override
		public void prepareInput(CompEnv<T> gen) throws Exception {			
			Options options = new Options();
			options.addOption("h", false, "high precision");
			options.addOption("s", "case", true, "case");
			options.addOption("t", "control", true, "control");

			CommandLineParser parser = new BasicParser();
			CommandLine cmd = parser.parse(options, args);

			precise = cmd.hasOption("t");
			if(!cmd.hasOption("s") || !cmd.hasOption("t")) {
				throw new Exception("wrong input");
			}
			StatisticsData caseInput = PrepareDataOddsRatio.readFile(cmd.getOptionValue("s"));
			StatisticsData controlInput = PrepareDataOddsRatio.readFile(cmd.getOptionValue("t"));
			StatisticsOddsRatio[] caseSta = caseInput.data;
			StatisticsOddsRatio[] controlSta = controlInput.data;
			boolean[][][] caseData = new boolean[caseSta.length][2][Width];
			boolean[][][] caseData2 = new boolean[caseSta.length][2][Width];
			boolean[][][] caseData3 = new boolean[caseSta.length][2][Width];

			for(int i = 0; i < caseSta.length; ++i) {
				caseData[i][0] = Utils.fromInt(caseSta[i].numOfPresent, Width);
				caseData[i][1] = Utils.fromInt(caseSta[i].totalNum - caseSta[i].numOfPresent, Width);

				caseData2[i][0] = Utils.fromInt(caseSta[i].numOfPresent, Width);
				caseData2[i][1] = Utils.fromInt(caseSta[i].totalNum - caseSta[i].numOfPresent, Width);

				caseData3[i][0] = Utils.fromInt(caseSta[i].numOfPresent, Width);
				caseData3[i][1] = Utils.fromInt(caseSta[i].totalNum - caseSta[i].numOfPresent, Width);
			}

			boolean[][][] controlData = new boolean[controlSta.length][2][Width];
			boolean[][][] controlData2 = new boolean[controlSta.length][2][Width];
			boolean[][][] controlData3 = new boolean[controlSta.length][2][Width];

			for(int i = 0; i < controlSta.length; ++i) {
				controlData[i][0] = Utils.fromInt(controlSta[i].numOfPresent, Width);
				controlData[i][1] = Utils.fromInt(controlSta[i].totalNum - controlSta[i].numOfPresent, Width);

				controlData2[i][0] = Utils.fromInt(controlSta[i].numOfPresent, Width);
				controlData2[i][1] = Utils.fromInt(controlSta[i].totalNum - controlSta[i].numOfPresent, Width);

				controlData3[i][0] = Utils.fromInt(controlSta[i].numOfPresent, Width);
				controlData3[i][1] = Utils.fromInt(controlSta[i].totalNum - controlSta[i].numOfPresent, Width);
			}
			System.out.println(caseSta.length);
			System.out.println(controlSta.length);

			aliceCase = gen.inputOfAlice(caseData);
			aliceControl = gen.inputOfAlice(controlData);
			bobCase = gen.inputOfBob(new boolean[caseSta.length][2][Width]);
			bobControl = gen.inputOfBob(new boolean[controlSta.length][2][Width]);

			
			aliceCaseFiltered = gen.inputOfAlice(caseData2);
			aliceControlFiltered = gen.inputOfAlice(controlData2);
			bobCaseFiltered = gen.inputOfBob(new boolean[caseSta.length][2][Width]);
			bobControlFiltered = gen.inputOfBob(new boolean[controlSta.length][2][Width]);


			aliceCaseHolder = gen.inputOfAlice(caseData3);
			aliceControlHolder = gen.inputOfAlice(controlData3);
			bobCaseHolder = gen.inputOfBob(new boolean[caseSta.length][2][Width]);
			bobControlHolder = gen.inputOfBob(new boolean[controlSta.length][2][Width]);
			numOfTests = caseSta.length;
		}

		T[][] res;
		int[] indices;
		int numFiltered;
		boolean[] filResOut;
		@Override
		public void secureCompute(CompEnv<T> gen) {
			numFiltered = 0;
			CircuitLib<T> cl = new CircuitLib<T>(gen);
			filterRes = filter(gen, aliceCase, bobCase, aliceControl, bobControl, numOfTests);
			filResOut = gen.outputToAlice(filterRes);
			for(int i =0; i < numOfTests; i++){
				gen.channel.writeBoolean(filResOut[i]);
				gen.channel.flush();
			}

			indices = new int[numOfTests];
			for(int i = 0; i < numOfTests; i++){
				if(filResOut[i]){
					indices[numFiltered] = i;
					numFiltered++;
				}
			}
			gen.flush();

			System.out.println(numFiltered);
			System.out.println(" ");
			for(int i = 0; i < numFiltered; i++){				
				System.arraycopy(aliceCaseFiltered[i][0], 0, aliceCaseHolder[indices[i]][0], 0, Width);
				System.arraycopy(aliceCaseFiltered[i][1], 0, aliceCaseHolder[indices[i]][1], 0, Width);

				System.arraycopy(aliceControlFiltered[i][0], 0, aliceControlHolder[indices[i]][0], 0, Width);
				System.arraycopy(aliceControlFiltered[i][1],0, aliceControlHolder[indices[i]][1], 0, Width);
			}

			res = compute(gen, aliceCaseFiltered, bobCaseFiltered, aliceControlFiltered, bobControlFiltered, numFiltered);
			gen.flush();

		}

		@Override
		public void prepareOutput(CompEnv<T> gen) {
			FloatLib<T> flib = new FloatLib<T>(gen, FWidth, FOffset);
			System.out.println("odds ratio");
			boolean[] out;
			int counter = 0;
			HashSet<Integer> indicesArL = new HashSet<Integer>();
			for(int i =0; i < indices.length; i++){
				indicesArL.add(indices[i]);
			}
			CircuitLib<T> cl = new CircuitLib<T>(gen);
			for(int i = 0; i < numOfTests; i++){
				double chi;
				if(!filResOut[i]){
					chi = 0.0;
				}
				else{
					out = gen.outputToAlice(res[counter]);
					chi = Utils.toFloat(out, FWidth, FOffset);
					counter++;
				}
				System.out.println(chi);
			}
		}
	}

	public static class Evaluator<T> extends EvaRunnable<T> {
		T[][][] aliceCase;
		T[][][] bobCase;
		T[][][] aliceControl;
		T[][][] bobControl;
		T[][][] aliceCaseFiltered;
		T[][][] bobCaseFiltered;
		T[][][] aliceControlFiltered;
		T[][][] bobControlFiltered;

		T[][][] aliceCaseHolder;
		T[][][] aliceControlHolder;
		T[][][] bobCaseHolder;
		T[][][] bobControlHolder;
		
		int numOfTests;
		boolean precise;
		T[]filterRes;

		@Override
		public void prepareInput(CompEnv<T> gen) throws Exception {
			Options options = new Options();
			options.addOption("h", "high_precision", false, "high precision");
			options.addOption("s", "case", true, "case");
			options.addOption("t", "control", true, "control");

			CommandLineParser parser = new BasicParser();
			CommandLine cmd = parser.parse(options, args);

			precise = cmd.hasOption("h");
			if(!cmd.hasOption("s") || !cmd.hasOption("t")) {
				throw new Exception("wrong input");
			}

			StatisticsData caseInput = PrepareDataOddsRatio.readFile(cmd.getOptionValue("s"));
			StatisticsData controlInput = PrepareDataOddsRatio.readFile(cmd.getOptionValue("t"));
			StatisticsOddsRatio[] caseSta = caseInput.data;
			StatisticsOddsRatio[] controlSta = controlInput.data;
			boolean[][][] caseData = new boolean[caseSta.length][2][Width];
			boolean[][][] caseData2 = new boolean[caseSta.length][2][Width];
			boolean[][][] caseData3 = new boolean[caseSta.length][2][Width];

			for(int i = 0; i < caseSta.length; ++i) {
				caseData[i][0] = Utils.fromInt(caseSta[i].numOfPresent, Width);
				caseData[i][1] = Utils.fromInt(caseSta[i].totalNum - caseSta[i].numOfPresent, Width);
				
				caseData2[i][0] = Utils.fromInt(caseSta[i].numOfPresent, Width);
				caseData2[i][1] = Utils.fromInt(caseSta[i].totalNum - caseSta[i].numOfPresent, Width);

				caseData3[i][0] = Utils.fromInt(caseSta[i].numOfPresent, Width);
				caseData3[i][1] = Utils.fromInt(caseSta[i].totalNum - caseSta[i].numOfPresent, Width);
			}

			boolean[][][] controlData = new boolean[controlSta.length][2][Width];
			boolean[][][] controlData2 = new boolean[controlSta.length][2][Width];
			boolean[][][] controlData3 = new boolean[controlSta.length][2][Width];

			for(int i = 0; i < controlSta.length; ++i) {
				controlData[i][0] = Utils.fromInt(controlSta[i].numOfPresent, Width);
				controlData[i][1] = Utils.fromInt(controlSta[i].totalNum - controlSta[i].numOfPresent, Width);
				
				controlData2[i][0] = Utils.fromInt(controlSta[i].numOfPresent, Width);
				controlData2[i][1] = Utils.fromInt(controlSta[i].totalNum - controlSta[i].numOfPresent, Width);
				
				controlData3[i][0] = Utils.fromInt(controlSta[i].numOfPresent, Width);
				controlData3[i][1] = Utils.fromInt(controlSta[i].totalNum - controlSta[i].numOfPresent, Width);
			}
			aliceCase = gen.inputOfAlice(new boolean[caseSta.length][2][Width]);
			aliceControl = gen.inputOfAlice(new boolean[controlSta.length][2][Width]);
			bobCase = gen.inputOfBob(caseData);
			bobControl = gen.inputOfBob(controlData);

			aliceCaseFiltered = gen.inputOfAlice(new boolean[caseSta.length][2][Width]);
			aliceControlFiltered = gen.inputOfAlice(new boolean[controlSta.length][2][Width]);
			bobCaseFiltered = gen.inputOfBob(caseData2);
			bobControlFiltered = gen.inputOfBob(controlData2);

			aliceCaseHolder = gen.inputOfAlice(new boolean[caseSta.length][2][Width]);
			aliceControlHolder = gen.inputOfAlice(new boolean[controlSta.length][2][Width]);
			bobCaseHolder = gen.inputOfBob(caseData3);
			bobControlHolder = gen.inputOfBob(controlData3);
			
			numOfTests = caseSta.length;
		}
		T[][] res;
		int[] indices;

		int numFiltered;
		boolean[] filResOut;
		@Override
		public void secureCompute(CompEnv<T> gen) {
			numFiltered = 0;
			CircuitLib<T> cl = new CircuitLib<T>(gen);
			filterRes = filter(gen, aliceCase, bobCase, aliceControl, bobControl, numOfTests);
			gen.outputToAlice(filterRes);
			filResOut = new boolean[numOfTests];
			for(int i = 0; i < numOfTests; i++){
				filResOut[i] = gen.channel.readBoolean();
				gen.channel.flush();
			}
			indices = new int[numOfTests];
			for(int i = 0; i < numOfTests; i++){
				if(filResOut[i]){
					indices[numFiltered] = i;
					numFiltered++;
				}
			}
			gen.flush();
			System.out.println(numFiltered);
			System.out.println(" ");

			System.out.println("d");


			for(int i = 0; i < numFiltered; i++){				
				System.arraycopy(bobCaseFiltered[i][0], 0, bobCaseHolder[indices[i]][0], 0, Width);
				System.arraycopy(bobCaseFiltered[i][1], 0, bobCaseHolder[indices[i]][1], 0, Width);

				System.arraycopy(bobControlFiltered[i][0], 0, bobControlHolder[indices[i]][0], 0, Width);
				System.arraycopy(bobControlFiltered[i][1], 0, bobControlHolder[indices[i]][1], 0, Width);
			}
			System.out.println("d2 ");

			res = compute(gen, aliceCaseFiltered, bobCaseFiltered, aliceControlFiltered, bobControlFiltered, numFiltered);
			gen.flush();

		}

		@Override
		public void prepareOutput(CompEnv<T> gen) {
			CircuitLib<T> cl = new CircuitLib<T>(gen);
			FloatLib<T> flib = new FloatLib<T>(gen, FWidth, FOffset);
			int counter = 0;
			HashSet<Integer> indicesArL = new HashSet<Integer>();
			for(int i =0; i < indices.length; i++){
				indicesArL.add(indices[i]);
			}
			for(int i = 0; i < numOfTests; i++){
				if(!filResOut[i]){
					continue;
				}
				else{
					Utils.toFloat(gen.outputToAlice(res[counter]), FWidth, FOffset);
					counter++;
				}
			}
		}
	}
}
