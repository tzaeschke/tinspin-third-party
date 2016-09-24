/*
 * Copyright 2011-2016 ETH Zurich. All Rights Reserved.
 *
 * This software is the proprietary information of ETH Zurich.
 * Use is subject to license terms.
 */
package ch.ethz.globis.tinspin.wrappers;

import java.util.ArrayList;
import java.util.List;

import org.khelekore.prtree.AcceptAll;
import org.khelekore.prtree.DistanceCalculator;
import org.khelekore.prtree.DistanceResult;
import org.khelekore.prtree.NodeFilter;
import org.khelekore.prtree.PRTree;
import org.khelekore.prtree.PointND;
import org.khelekore.prtree.SimpleMBR;
import org.khelekore.prtree.SimplePointND;

import ch.ethz.globis.phtree.demo.PR_Entry;
import ch.ethz.globis.phtree.demo.PR_PR_PointConverter;
import ch.ethz.globis.tinspin.TestStats;

/**
 * Wrapper for Priority R-Tree by:
 * http://www.khelekore.org/prtree/
 */
public class PointPRTree extends Candidate {

	private final int BRANCH_FACTOR = 30;
	private PRTree<PR_Entry> prt;
	private DistanceCalculator<PR_Entry> dc;
    private NodeFilter<PR_Entry> acceptAll = new AcceptAll<> ();

	
	private final int dims;
	private final int N;
	
	public PointPRTree(TestStats ts) {
		this.dims = ts.cfgNDims;
		this.N = ts.cfgNEntries;
		dc = (PR_Entry t, PointND p) -> {
			double dist = 0;
			double[] data = t.data();
			for (int i = 0; i < data.length; i++) {
				double d = p.getOrd(i)-data[i];
				dist += d*d;
			}
			return Math.sqrt(dist);
		};
	}

	@Override
	public void load(double[] data, int idxDim) {
		ArrayList<PR_Entry> data2 = new ArrayList<>(N);
		
		prt = new PRTree<>(new PR_PR_PointConverter(dims), BRANCH_FACTOR); 
		double[] buf = new double[dims];
		for (int i = 0; i < N; i++) {
			System.arraycopy(data, i*dims, buf, 0, dims);
			data2.add(new PR_Entry(buf));
		}
		prt.load(data2);
	}

	@Override
	public Object preparePointQuery(double[][] qA) {
		SimpleMBR[] r = new SimpleMBR[qA.length];
		for (int i = 0; i < qA.length; i++) {
			double[] qa = qA[i];
			SimpleMBR q = new SimpleMBR(qa, qa);
			r[i] = q;
		}
		return r;
	}

	@Override
	public int pointQuery(Object qA) {
		SimpleMBR[] a = (SimpleMBR[]) qA;
		int n = 0;
		for (SimpleMBR q: a) {
			for (PR_Entry e: prt.find(q)) {
				for (int d = 0; d < dims; d++) {
					if (q.getMin(d) == e.data()[d] &&
							q.getMax(d) == e.data()[d]) {
						n++;
						break;
					}
				}
			}
		}
		return n;
	}

	@Override
	public int unload() {
		System.err.println("deletion not supported!");
		return N;
	}

	@SuppressWarnings("unused")
	@Override
	public int query(double[] min, double[] max) {
		SimpleMBR mbr = new SimpleMBR(min, max);
		int n = 0;
		for (PR_Entry e: prt.find(mbr)) {
			n++;
		}
		return n;
	}

	@Override
	public double knnQuery(int k, double[] center) {
		double ret = 0;
		
		SimplePointND p = new SimplePointND(center);
		List<DistanceResult<PR_Entry>> result = prt.nearestNeighbour(dc, acceptAll, k, p);
		
		for (DistanceResult<PR_Entry> dr: result) {
			ret += dr.getDistance();
		}
		
		return ret;
	}
	
	@Override
	public boolean supportsKNN() {
		return true;
	}
	
	@Override
	public void release() {
		// nothing to be done
	}

	@Override
	public int update(double[][] updateTable) {
//		Object dummy = new Object();
//		int n = 0;
//		for (int i = 0; i < updateTable.length; ) {
//			int[] p1 = toKey(updateTable[i++]);
//			int[] p2 = toKey(updateTable[i++]);
//			if (cbf.remove(p1) != null) {
//				cbf.put(p2, dummy);
//				n++;
//			}
//		}
//		return n;
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean supportsUpdate() {
		return false;
	}
	
	@Override
	public void getStats(TestStats S) {
		S.statNnodes = prt.getNumberOfLeaves();
		S.statNpostlen = prt.getHeight();
	}
}
