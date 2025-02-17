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

/*Original implementation of chi-square performed by xiao wang,
 *  modified by justin wagner to handle microbiome count data
 */

package precompute;

import java.nio.ByteBuffer;

import network.Server;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.math3.distribution.ChiSquaredDistribution;

import precompute.PrepareDataChiSquare;
import precompute.PrepareDataChiSquare.StatisticsData;
import util.EvaRunnable;
import util.GenRunnable;
import util.Utils;
import circuits.arithmetic.FloatLib;
import circuits.arithmetic.IntegerLib;
import flexsc.CompEnv;

public class ChiSquare {
	static public int Width = 32;
	static public int FWidth = 54;
	static public int FOffset = 11;

	public static<T> T[][] compute(CompEnv<T> gen, T[][][] aliceCase,
			T[][][] bobCase,
			T[][][] aliceControl,
			T[][][] bobControl, int numOfTests) {

		T[][] res = gen.newTArray(numOfTests, 0);

		IntegerLib<T> lib = new IntegerLib<T>(gen);
		FloatLib<T> flib = new FloatLib<T>(gen, FWidth, FOffset);

		for(int i = 0; i < numOfTests; i++){			
			T[] a = lib.add(aliceCase[i][0], bobCase[i][0]);
			T[] b = lib.add(aliceCase[i][1], bobCase[i][1]);
			T[] c = lib.add(aliceControl[i][0], bobControl[i][0]);
			T[] d = lib.add(aliceControl[i][1], bobControl[i][1]);

			T[] fa = lib.toSecureFloat(a, flib);
			T[] fb = lib.toSecureFloat(b, flib);
			T[] fc = lib.toSecureFloat(c, flib);
			T[] fd = lib.toSecureFloat(d, flib);

			T[] upperFirst = flib.add(fa, flib.add(fb, flib.add(fc, fd)));
			T[] upperSecond = flib.sub(flib.multiply(fb, fc), flib.multiply(fa, fd));
			upperSecond = flib.multiply(upperSecond, upperSecond);
			T[] upper = flib.multiply(upperFirst, upperSecond);
			T[] lower = flib.multiply(flib.multiply(flib.add(fa, fb), flib.add(fa, fc)), flib.multiply(flib.add(fb, fd), flib.add(fc, fd)));
			res[i] = flib.div(upper, lower);
		}
		return res;
	}
	
	public static class Generator<T> extends GenRunnable<T> {
		T[][][] aliceCase;
		T[][][] bobCase;
		T[][][] aliceControl;
		T[][][] bobControl;

		int numOfTests;

		double extraFactor;
		boolean precise;
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
			StatisticsData caseInput = PrepareDataChiSquare.readFile(cmd.getOptionValue("s"));
			StatisticsData controlInput = PrepareDataChiSquare.readFile(cmd.getOptionValue("t"));
			StatisticsChiSquare[] caseSta = caseInput.data;
			StatisticsChiSquare[] controlSta = controlInput.data;
			boolean[][][] caseData = new boolean[caseSta.length][2][Width];

			int caseLength = gen.channel.readInt();
			int controlLength = gen.channel.readInt();

			for(int i = 0; i < caseSta.length; ++i) {
				caseData[i][0] = Utils.fromInt(caseSta[i].numOfPresent, Width);
				caseData[i][1] = Utils.fromInt(caseSta[i].totalNum - caseSta[i].numOfPresent, Width);
			}

			boolean[][][] controlData = new boolean[controlSta.length][2][Width];
			for(int i = 0; i < controlSta.length; ++i) {
				controlData[i][0] = Utils.fromInt(controlSta[i].numOfPresent, Width);
				controlData[i][1] = Utils.fromInt(controlSta[i].totalNum - controlSta[i].numOfPresent, Width);
			}
			
			aliceCase = gen.inputOfAlice(caseData);
			aliceControl = gen.inputOfAlice(controlData);
			bobCase = gen.inputOfBob(new boolean[caseSta.length][2][Width]);
			bobControl = gen.inputOfBob(new boolean[controlSta.length][2][Width]);
			numOfTests = caseSta.length;
		}

		T[][] res;
		@Override
		public void secureCompute(CompEnv<T> gen) {
			res = compute(gen, aliceCase, bobCase, aliceControl, bobControl, numOfTests);
		}

		@Override
		public void prepareOutput(CompEnv<T> gen) {
			FloatLib<T> flib = new FloatLib<T>(gen, FWidth, FOffset);
			ChiSquaredDistribution chiDistribution = new ChiSquaredDistribution(1.0);
			System.out.println("chi,p-value");
			for(int i = 0; i < numOfTests; ++i){
				double chi = flib.outputToAlice(res[i]);// * extraFactor;
				if(chi == 0.0){
					System.out.println("NA,NA");
					continue;
				}
				System.out.println(chi + "," + (1-chiDistribution.cumulativeProbability(chi)));
			}
		}
	}

	public static class Evaluator<T> extends EvaRunnable<T> {
		T[][][] aliceCase;
		T[][][] bobCase;
		T[][][] aliceControl;
		T[][][] bobControl;
		int numOfTests;
		boolean precise;

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

			StatisticsData caseInput = PrepareDataChiSquare.readFile(cmd.getOptionValue("s"));
			StatisticsData controlInput = PrepareDataChiSquare.readFile(cmd.getOptionValue("t"));
			StatisticsChiSquare[] caseSta = caseInput.data;
			StatisticsChiSquare[] controlSta = controlInput.data;
			boolean[][][] caseData = new boolean[caseSta.length][2][Width];

			
			gen.channel.writeInt(caseInput.numberOftuples);
			gen.channel.writeInt(controlInput.numberOftuples);
			gen.channel.flush();
			
			for(int i = 0; i < caseSta.length; ++i) {
				caseData[i][0] = Utils.fromInt(caseSta[i].numOfPresent, Width);
				caseData[i][1] = Utils.fromInt(caseSta[i].totalNum - caseSta[i].numOfPresent, Width);
			}

			boolean[][][] controlData = new boolean[controlSta.length][2][Width];
			for(int i = 0; i < controlSta.length; ++i) {
				controlData[i][0] = Utils.fromInt(controlSta[i].numOfPresent, Width);
				controlData[i][1] = Utils.fromInt(controlSta[i].totalNum - controlSta[i].numOfPresent, Width);
			}
			aliceCase = gen.inputOfAlice(new boolean[caseSta.length][2][Width]);
			aliceControl = gen.inputOfAlice(new boolean[controlSta.length][2][Width]);
			bobCase = gen.inputOfBob(caseData);
			bobControl = gen.inputOfBob(controlData);
			numOfTests = caseSta.length;
		}
		T[][] res;

		@Override
		public void secureCompute(CompEnv<T> gen) {
			res = compute(gen, aliceCase, bobCase, aliceControl, bobControl, numOfTests);
		}

		@Override
		public void prepareOutput(CompEnv<T> gen) {
			FloatLib<T> flib = new FloatLib<T>(gen, FWidth, FOffset);
			for(int i = 0; i < numOfTests; ++i)
				flib.outputToAlice(res[i]);
		}
	}
}
