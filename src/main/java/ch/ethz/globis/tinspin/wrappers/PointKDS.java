/*
 * Copyright 2011-2016 ETH Zurich. All Rights Reserved.
 *
 * This software is the proprietary information of ETH Zurich.
 * Use is subject to license terms.
 */
package ch.ethz.globis.tinspin.wrappers;

import java.util.Iterator;

import com.savarese.spatial.KDTree;
import com.savarese.spatial.NearestNeighbors;

import ch.ethz.globis.phtree.demo.PR_Entry;
import ch.ethz.globis.phtree.demo.PR_KDS_DoublePoint;
import ch.ethz.globis.tinspin.TestStats;
import ch.ethz.globis.tinspin.TestStats.TST;
import ch.ethz.globis.tinspin.wrappers.Candidate;


/**
 * Wrapper class for the Savarese kd-tree.
 * 
 * https://www.savarese.com/software/libssrckdtree-j/
 * 
 */
public class PointKDS extends Candidate {

	private KDTree<Double, PR_KDS_DoublePoint, PR_Entry> kds;
	
	private final int dims;
	private final int N;
	private boolean isCluster;
	
	private double[] data;
	
	public PointKDS(TestStats ts) {
		this.dims = ts.cfgNDims;
		this.N = ts.cfgNEntries;
		this.isCluster = TST.CLUSTER.equals(ts.TEST) && ts.param1 < 5.0;
	}
	
	@Override
	public void load(double[] data, int idxDim) {
		this.data = data;
		kds = new KDTree<>(idxDim);
		PR_Entry dummy = new PR_Entry(new double[dims]);
		for (int i = 0; i < N; i++) {
			PR_KDS_DoublePoint buf = new PR_KDS_DoublePoint(idxDim);
			for (int d = 0; d < idxDim; d++) {
				buf.setCoord(d, data[idxDim*i + d]); 
			}
			kds.put(buf, dummy);
		}
		//TODO?
//		log("KDs-Tree: optimise");
//		long tt1 = System.currentTimeMillis();
//		kds.optimize();
//		long tt2 = System.currentTimeMillis();
//		log("optimise took: " + (tt2-tt1));
	}
	
	@Override
	public PR_KDS_DoublePoint[] preparePointQuery(double[][] qA) {
		PR_KDS_DoublePoint[] a = new PR_KDS_DoublePoint[qA.length];
		for (int i = 0; i < a.length; i++) {
			double[] dd = qA[i];
			PR_KDS_DoublePoint buf = new PR_KDS_DoublePoint(dims);
			for (int d = 0; d < dd.length; d++) {
				buf.setCoord(d, dd[d]); 
			}
			a[i] = buf;
		}
		return a;
	}

	@Override
	public int pointQuery(Object qA) {
		PR_KDS_DoublePoint[] a = (PR_KDS_DoublePoint[]) qA;
		int n = 0;
		for (PR_KDS_DoublePoint q: a) {
			if (kds.containsKey(q)) {
				n++;
			}
		}
		return n;
	}

	@Override
	public int query(double[] min, double[] max) {
		PR_KDS_DoublePoint lower = new PR_KDS_DoublePoint(dims);
		PR_KDS_DoublePoint upper = new PR_KDS_DoublePoint(dims);
		for (int i = 0; i < dims; i++) {
			lower.setCoord(i, min[i]);
			upper.setCoord(i, max[i]);
		}
		int n = 0;
		Iterator<?> it = kds.iterator(lower, upper);
		while (it.hasNext()) {
			it.next();
			n++;
		}
		return n;
	}
	
	@Override
	public boolean supportsWindowQuery() {
		//Cluster 3.4 / 3.5 requires >250000 ns/query object == 1hour
		return !isCluster || N <= 1000000;
	}

	@Override
	public double knnQuery(int k, double[] center) {
		double ret = 0;
		PR_KDS_DoublePoint query = new PR_KDS_DoublePoint(dims);
		for (int d = 0; d < dims; d++) {
			query.setCoord(d, center[d]); 
		}

		NearestNeighbors<Double, PR_KDS_DoublePoint, PR_Entry> nn = new NearestNeighbors<>();
		NearestNeighbors.Entry<Double, PR_KDS_DoublePoint, PR_Entry>[] ne;
		ne = nn.get(kds, query, k, false);

		int n = 0;
		for (NearestNeighbors.Entry<Double, PR_KDS_DoublePoint, PR_Entry> e: ne) {
			ret += Math.sqrt(e.getDistance2());
			if (++n == k) {
				break;
			}
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
		PR_KDS_DoublePoint e = new PR_KDS_DoublePoint(dims);
		for (int i = 0; i < N>>1; i++) {
			n += kds.remove(getEntry(e, i)) != null ? 1 : 0;
			n += kds.remove(getEntry(e, N-i-1)) != null ? 1 : 0;
		}
		if ((N%2) != 0) {
			int i = (N>>1);
			n += kds.remove(getEntry(e, N-i-1)) != null ? 1 : 0;
		}
		return n;
	}

	private PR_KDS_DoublePoint getEntry(PR_KDS_DoublePoint e, int pos) {
		for (int d = 0; d < dims; d++) {
			e.setCoord(d, data[pos*dims+d]);
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
			PR_KDS_DoublePoint buf = new PR_KDS_DoublePoint(dims);
			for (int d = 0; d < dims; d++) {
				buf.setCoord(d, p1[d]); 
			}
			if (kds.remove(buf) != null) {
				for (int d = 0; d < dims; d++) {
					buf.setCoord(d, p2[d]); 
				}
				kds.put(buf, dummy);
				n++;
			}
		}
		return n;
	}
	
	@Override
	public boolean supportsUpdate() {
		return true;
	}
}
