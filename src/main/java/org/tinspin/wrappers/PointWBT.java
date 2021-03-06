/*
 * Copyright 2011-2016 ETH Zurich. All Rights Reserved.
 *
 * This software is the proprietary information of ETH Zurich.
 * Use is subject to license terms.
 */
package org.tinspin.wrappers;

import ch.ethz.globis.tinspin.TestStats;
import ch.ethz.globis.tinspin.wrappers.Candidate;

import org.tinspin.index.Index;
import org.tinspin.index.PointEntry;
import org.tinspin.index.PointEntryDist;
import org.tinspin.index.QueryIterator;
import org.tinspin.index.QueryIteratorKNN;

import weka.tinspin.WekaTreeWrapper;

/**
 * R-Tree with sort tile recursive bulkloading.
 *
 */
public class PointWBT extends Candidate {
	
	private WekaTreeWrapper<double[]> phc;
	private final int dims;
	private final int N;
	private double[] data;
	private QueryIterator<PointEntry<double[]>> it;
	private QueryIteratorKNN<PointEntryDist<double[]>> itKnn;

	
	/**
	 * Setup of a native PH tree
	 * 
	 * @param ts test stats
	 */
	public PointWBT(TestStats ts) {
		this.N = ts.cfgNEntries;
		this.dims = ts.cfgNDims;
	}
	
	@Override
	public void load(double[] data, int dims) {
		int pos = 0;
		double[][] allData = new double[N][dims];
		for (int n = 0; n < N; n++) {
			double[] p = allData[n];
			System.arraycopy(data, pos, p, 0, dims);
			pos += dims;
		}
//		phc.load(list);
		phc = WekaTreeWrapper.create(WekaTreeWrapper.Type.BALL, dims, allData, allData);
		this.data = data;
	}

	@Override
	public Object preparePointQuery(double[][] q) {
		return q;
	}

	@Override
	public int pointQuery(Object qA) {
		int n = 0;
		for (double[] q: (double[][])qA) {
			if (phc.query(q, q) != null) {
				n++;
			}
			//log("q=" + Arrays.toString(q));
		}
		return n;
	}

	@Override
	public int unload() {
		int n = 0;
		double[] l = new double[dims];
		for (int i = 0; i < N>>1; i++) {
			n += phc.remove(getEntry(l, i)) != null ? 1 : 0;
			n += phc.remove(getEntry(l, N-i-1)) != null ? 1 : 0;
		}
		if ((N%2) != 0) {
			int i = (N>>1);
			n += phc.remove(getEntry(l, i)) != null ? 1 : 0;
		}
		return n;
	}

	private double[] getEntry(double[] val, int pos) {
		for (int d = 0; d < dims; d++) {
			val[d] = data[pos*dims+d];
		}
		return val;
	}
	
	@Override
	public int query(double[] min, double[] max) {
		if (it == null) {
			it = phc.query(min, max);
		} else {
			it.reset(min, max);
		}
		int n = 0;
		while (it.hasNext()) {
			it.next();
			n++;
		}
//		int n = ((PhTree7)phc).queryAll(min2, max2).size();
		//log("q=" + Arrays.toString(q));
		return n;
	}
	
	@Override
	public double knnQuery(int k, double[] center) {
		if (k == 1) {
			return phc.query1NN(center).dist();
		}
		if (itKnn == null) {
			itKnn = phc.queryKNN(center, k);
		} else {
			itKnn.reset(center, k);
		}
		double ret = 0;
		int n = 0;
		while (itKnn.hasNext() && n++ < k) {
			ret += itKnn.next().dist();
		}
		if (n < k) {
			throw new IllegalStateException("n/k=" + n + "/" + k);
		}
		return ret;
	}

	@Override
	public boolean supportsKNN() {
		return true;
	}
	
	@Override
	public void release() {
		data = null;
	}

	
	/**
	 * Used to test the native code during development process
	 */
	public Index<double[]> getNative() {
		return phc;
	}

	@Override
	public void getStats(TestStats s) {
		s.statNnodes = phc.getNodeCount();
		s.statNpostlen = phc.getDepth();
		//phc.printStats(N);
		//phc.printQuality();
		//PhTreeStats q = phc.getStats();
		//S.setStats(q);
		//System.out.println(phc.getQuality());
	}
	
	@Override
	public int update(double[][] updateTable) {
//		int n = 0;
//		for (int i = 0; i < updateTable.length; ) {
//			double[] p1 = updateTable[i++];
//			double[] p2 = Arrays.copyOf(updateTable[i++], dims);
//			if (phc.update(p1, p1, p2, p2) != null) {
//				n++;
//			}
//		}
//		return n;
		return -1;
	}
	
	@Override
	public boolean supportsWindowQuery() {
		return false;
	}

	@Override
	public boolean supportsPointQuery() {
		return false;
	}
	
	@Override
	public boolean supportsUpdate() {
		return false;
	}

	@Override
	public boolean supportsUnload() {
		return false;
	}
	
	@Override
	public String toString() {
		return phc.toString(); 
	}
}
