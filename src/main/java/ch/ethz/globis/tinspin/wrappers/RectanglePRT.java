/*
 * Copyright 2011-2016 ETH Zurich. All Rights Reserved.
 *
 * This software is the proprietary information of ETH Zurich.
 * Use is subject to license terms.
 */
package ch.ethz.globis.tinspin.wrappers;

import java.util.ArrayList;
import java.util.List;

import org.khelekore.prtree.PRTree;
import org.khelekore.prtree.SimpleMBR;

import ch.ethz.globis.phtree.demo.PR_Entry;
import ch.ethz.globis.phtree.demo.PR_PR_RangeConverter;
import ch.ethz.globis.phtree.nv.PhTreeNV;
import ch.ethz.globis.tinspin.TestStats;
import ch.ethz.globis.tinspin.wrappers.Candidate;

/**
 * Wrapper for Priority R-Tree by:
 * http://www.khelekore.org/prtree/
 */
public class RectanglePRT extends Candidate {
	
	private static int BRANCH_FACTOR = 30;
	
	private final PRTree<PR_Entry> prt;
	private final int dims;
	private final int N;
	
	/**
	 * Setup of a priority R-Tree.
	 * 
	 * @param ts test configuration
	 */
	public RectanglePRT(TestStats ts) {
		this.dims = ts.cfgNDims;
		this.N = ts.cfgNEntries;
		prt = new PRTree<PR_Entry>(new PR_PR_RangeConverter(dims), BRANCH_FACTOR);
	}
	
	@Override
	public void load(double[] data, int dims) {
		ArrayList<PR_Entry> data2 = new ArrayList<PR_Entry>(N);
		double[] buf1 = new double[dims];
		double[] buf2 = new double[dims];
		for (int i = 0; i < N; i++) {
			System.arraycopy(data, i*dims*2, buf1, 0, dims);
			System.arraycopy(data, i*dims*2+dims, buf2, 0, dims);
			data2.add(new PR_Entry(buf1, buf2));
		}
		prt.load(data2);
	}

	@Override
	public SimpleMBR[] preparePointQuery(double[][] q) {
		int repeat = q.length/2;
		SimpleMBR[] qA = new SimpleMBR[repeat];
		for (int i = 0; i < repeat; i++) {
			//query index
			qA[i] = new SimpleMBR(q[i*2], q[i*2+1]);
		}
		return qA;
	}

	@Override
	public int pointQuery(Object qA) {
		int n = 0;
		for (SimpleMBR q: (SimpleMBR[])qA) {
			for (PR_Entry e: prt.find(q)) {
				for (int d = 0; d < q.getDimensions(); d++) {
					if (q.getMin(d) == e.data()[d*2] &&
							q.getMax(d) == e.data()[d*2+1]) {
						n++;
						break;
					}
				}
			}
			//log("q=" + Arrays.toString(q));
		}
		return n;
	}

	@Override
	public int unload() {
		System.err.println("UNLOAD not supported for PRTree");
		return N;
	}
	
	@Override
	public int query(double[] min, double[] max) {
		SimpleMBR mbr = new SimpleMBR(min, max);
		int n = 0;
		for (PR_Entry e: prt.find(mbr)) {
			if (e != null) {
				n++;
			}
		}
		return n;
	}
	
	@Override
	public List<PR_Entry> queryToList(double[] min, double[] max) {
		ArrayList<PR_Entry> ret = new ArrayList<>();
		SimpleMBR mbr = new SimpleMBR(min, max);
		for (PR_Entry e: prt.find(mbr)) {
			ret.add(e);
		}
		return ret;
	}
	
	@Override
	public void release() {
		// nothing
	}


	public PhTreeNV getNative() {
		return null;
	}

	@Override
	public void getStats(TestStats S) {
		S.statNnodes = prt.getNumberOfLeaves();
		S.statNpostlen = prt.getHeight();
	}
	


	@Override
	public int update(double[][] updateTable) {
		System.err.println("UPDATE not supported for PRTree");
		return N;
//		int n = 0;
//		for (int i = 0; i < updateTable.length; ) {
//			double[] lo1 = updateTable[i++];
//			double[] up1 = updateTable[i++];
//			double[] lo2 = updateTable[i++];
//			double[] up2 = updateTable[i++];
//			if (phc.update(lo1, up1, lo2, up2)) {
//				n++;
//			}
//		}
//		return n;
	}
}
