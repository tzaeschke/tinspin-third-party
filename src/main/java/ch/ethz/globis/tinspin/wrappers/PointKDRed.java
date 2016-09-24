/*
 * Copyright 2011-2016 ETH Zurich. All Rights Reserved.
 *
 * This software is the proprietary information of ETH Zurich.
 * Use is subject to license terms.
 */
/*
 * Copyright 2011-2016 ETH Zurich. All Rights Reserved.
 *
 * This software is the proprietary information of ETH Zurich.
 * Use is subject to license terms.
 */
package ch.ethz.globis.tinspin.wrappers;

import ags.utils.dataStructures.trees.thirdGenKD.KdTree;
import ags.utils.dataStructures.trees.thirdGenKD.NearestNeighborIterator;
import ags.utils.dataStructures.trees.thirdGenKD.SquareEuclideanDistanceFunction;
import ch.ethz.globis.phtree.demo.PR_KDS_DoublePoint;
import ch.ethz.globis.tinspin.TestStats;


/**
 * Wrapper class for the Rednaxela kd-tree.
 * 
 * http://robowiki.net/wiki/User:Rednaxela/kD-Tree
 * https://bitbucket.org/rednaxela/knn-benchmark/src/tip/ags/utils/dataStructures/trees/thirdGenKD/
 *
 */
public class PointKDRed extends Candidate {
	
	private KdTree<double[]> kdr;
	
	private final int dims;
	private final int N;
	private SquareEuclideanDistanceFunction edf;
	
	public PointKDRed(TestStats ts) {
		this.dims = ts.cfgNDims;
		this.N = ts.cfgNEntries;
		this.edf = new SquareEuclideanDistanceFunction();
	}
	
	@Override
	public void load(double[] data, int idxDim) {
		kdr = new KdTree<>(idxDim);
		for (int i = 0; i < N; i++) {
			double[] point = new double[idxDim];
			for (int d = 0; d < idxDim; d++) {
				point[d] = data[idxDim*i + d]; 
			}
			kdr.addPoint(point, point);
		}
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
//		PR_KDS_DoublePoint[] a = (PR_KDS_DoublePoint[]) qA;
//		int n = 0;
//		for (PR_KDS_DoublePoint q: a) {
//			if (kdr.containsKey(q)) {
//				n++;
//			}
//		}
//		return n;
		return -1;
	}

	@Override
	public int query(double[] min, double[] max) {
//		PR_KDS_DoublePoint lower = new PR_KDS_DoublePoint(dims);
//		PR_KDS_DoublePoint upper = new PR_KDS_DoublePoint(dims);
//		for (int i = 0; i < dims; i++) {
//			lower.setCoord(i, min[i]);
//			upper.setCoord(i, max[i]);
//		}
//		int n = 0;
//		Iterator<?> it = kds.iterator(lower, upper);
//		while (it.hasNext()) {
//			it.next();
//			n++;
//		}
//		return n;
		return -1;
	}

	@Override
	public double knnQuery(int k, double[] center) {
		double ret = 0;
		
		NearestNeighborIterator<double[]> iter = kdr.getNearestNeighborIterator(center, k, edf);
		
		while (iter.hasNext()) {
			double[] point = iter.next();
			ret += dist(center, point);
		}
		
		return ret;
	}
	
	@Override
	public int unload() {
//		int n = 0;
//		PR_KDS_DoublePoint e = new PR_KDS_DoublePoint(dims);
//		for (int i = 0; i < N>>1; i++) {
//			n += kds.remove(getEntry(e, i)) != null ? 1 : 0;
//			n += kds.remove(getEntry(e, N-i-1)) != null ? 1 : 0;
//		}
//		return n;
		System.err.println("deletion not supported!");
		return N;
	}

//	private PR_KDS_DoublePoint getEntry(PR_KDS_DoublePoint e, int pos) {
//		for (int d = 0; d < dims; d++) {
//			e.setCoord(d, data[pos*dims+d]);
//		}
//		return e;
//	}

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
}
