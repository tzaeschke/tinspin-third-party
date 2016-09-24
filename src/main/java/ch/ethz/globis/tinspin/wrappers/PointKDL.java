/*
 * Copyright 2011-2016 ETH Zurich. All Rights Reserved.
 *
 * This software is the proprietary information of ETH Zurich.
 * Use is subject to license terms.
 */
package ch.ethz.globis.tinspin.wrappers;

import ch.ethz.globis.phtree.demo.PR_Entry;
import ch.ethz.globis.tinspin.TestStats;
import edu.wlu.cs.levy.CG.KDTree;
import edu.wlu.cs.levy.CG.KeyDuplicateException;
import edu.wlu.cs.levy.CG.KeyMissingException;
import edu.wlu.cs.levy.CG.KeySizeException;

/**
 * Wrapper for KD-Tree by:
 * http://home.wlu.edu/~levys/software/kd/
 */
public class PointKDL extends Candidate {

	private KDTree<PR_Entry> kdl;
	
	private final int dims;
	private final int N;
	
	private double[] data;
	
	public PointKDL(TestStats ts) {
		this.dims = ts.cfgNDims;
		this.N = ts.cfgNEntries;
	}
	
	@Override
	public void load(double[] data, int idxDim) {
		this.data = data;
		kdl = new KDTree<>(idxDim);
		//dummy, otherwise queries return no results
		PR_Entry dummy = new PR_Entry(new double[dims]);
		for (int i = 0; i < N; i++) {
			double[] buf = new double[idxDim];
			for (int d = 0; d < idxDim; d++) {
				buf[d] = data[idxDim*i+d]; 
			}
			//TODO the following allows correct kNN results
			//PR_Entry dummy = new PR_Entry(buf);
			try {
				kdl.insert(buf, dummy);
			} catch (KeySizeException | KeyDuplicateException e1) {
				throw new RuntimeException(e1);
			}
		}
	}
	
	@Override
	public double[][] preparePointQuery(double[][] qA) {
		return qA;
	}

	@Override
	public int pointQuery(Object qA) {
		double[][] a = (double[][]) qA;
		int n = 0;
		for (double[] q: a) {
			try {
				if (kdl.search(q) != null) {
					n++;
				}
			} catch (KeySizeException e) {
				throw new RuntimeException(e);
			}
		}
		return n;
	}

	@Override
	public int query(double[] min, double[] max) {
		int n = 0;
		try {
			for (PR_Entry e: kdl.range(min, max)) {
				if (e.data() != null) {
					n++;
				}
			}
		} catch (KeySizeException e) {
			throw new RuntimeException(e);
		}
		return n;
	}

	@Override
	public double knnQuery(int k, double[] center) {
		double ret = 0;
		try {
			int n = 0;
			for (PR_Entry e: kdl.nearest(center, k)) {
				double[] v = e.data();
				ret += dist(center, v);
				if (++n == k) {
					break;
				}
			}
		} catch (KeySizeException | IllegalArgumentException e) {
			throw new RuntimeException(e);
		}
		return ret;
	}
	
	@Override
	public boolean supportsKNN() {
		return true;
	}
	
	@Override
	public int unload() {
		int n = 0;
		double[] e = new double[dims];
		for (int i = 0; i < N>>1; i++) {
			try {
				kdl.delete(getEntry(e, i));
				kdl.delete(getEntry(e, N-i-1));
				n+=2;
			} catch (KeySizeException | KeyMissingException e1) {
				throw new RuntimeException(e1);
			}
		}
		return n;
	}

	private double[] getEntry(double[] e, int pos) {
		for (int d = 0; d < dims; d++) {
			e[d] = data[pos*dims+d];
		}
		return e;
	}

	@Override
	public void release() {
		// nothing to be done
	}

	@Override
	public int update(double[][] updateTable) {
		PR_Entry dummy = new PR_Entry(new double[dims]);
		int n = 0;
		for (int i = 0; i < updateTable.length; ) {
			double[] p1 = updateTable[i++];
			double[] p2 = updateTable[i++];
			//if (kdl.delete(p1, true)) {
				try {
					kdl.delete(p1, true);
					kdl.insert(p2, dummy);
				} catch (KeySizeException | KeyMissingException e) {
					throw new RuntimeException(e);
				} catch (KeyDuplicateException e) {
					throw new RuntimeException(e);
				}
				n++;
			//}
		}
		return n;
	}
	
	@Override
	public boolean supportsUpdate() {
		return true;
	}
}
