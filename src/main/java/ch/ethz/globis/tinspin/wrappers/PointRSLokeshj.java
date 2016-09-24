/*
 * Copyright 2011-2016 ETH Zurich. All Rights Reserved.
 *
 * This software is the proprietary information of ETH Zurich.
 * Use is subject to license terms.
 */
package ch.ethz.globis.tinspin.wrappers;

import java.util.List;

import ch.ethz.globis.tinspin.TestStats;
import lokeshj.rstar3.RStarTree;
import lokeshj.rstar3.spatial.SpatialPoint;

/**
 * Wrapper class for R*Tree by LokeshJ:
 * https://github.com/lokeshj/RStar-Tree
 */
public class PointRSLokeshj extends Candidate {


	private RStarTree xtr;
	
	private final int dims;
	private final int N;
	
	public PointRSLokeshj(TestStats ts) {
		this.dims = ts.cfgNDims;
		this.N = ts.cfgNEntries;
	}
	
	@Override
	public void load(double[] data, int idxDim) {
		xtr = new RStarTree(dims, false);
		int j = 0;
		for (int i = 0; i < N; i++) {
			double[] buf = new double[idxDim];
			for (int d = 0; d < idxDim; d++) {
				buf[d] = data[idxDim*i+d]; 
			}
			SpatialPoint sp = new SpatialPoint(buf, Integer.valueOf(i));
			
			if (xtr.insert(sp) != 1) {
				throw new IllegalStateException();
			}
			
			if (++j%N==0)
				System.out.print(j/N+"%, ");
		}
	}
	
	@Override
	public SpatialPoint[] preparePointQuery(double[][] qA) {
		SpatialPoint[] r = new SpatialPoint[qA.length];
		for (int i = 0; i < qA.length; i++) {
			double[] q = qA[i];
			r[i] = new SpatialPoint(q);
		}
		return r;
	}

	@Override
	public int pointQuery(Object qA) {
		SpatialPoint[] a = (SpatialPoint[]) qA;
		int n = 0;
		for (SpatialPoint point: a) {
			if (xtr.pointSearch(point) != null) {
				n++;
			}
		}
		return n;
	}

	@Override
	public int query(double[] min, double[] max) {
		List<SpatialPoint> list = xtr.rangeSearch(min, max);
		return list.size();
	}

	@Override
	public double knnQuery(int k, double[] center) {
		SpatialPoint sp = new SpatialPoint(center, null);
		List<SpatialPoint> list = xtr.knnSearch(sp, k);
		double totalDist = 0;
		int n = 0;
        for (SpatialPoint p: list) {
        	totalDist += p.distance(sp);
        	if (++n == k) {
        		break;
        	}
        }
        if (n < k) {
        	throw new IllegalStateException("n/k=" + n + "/" + k);
        }
		return totalDist;
	}
	
	@Override
	public boolean supportsKNN() {
		//TODO
		//KNN works, but the default initial range is '1', which means it processes ALL
		//entries. We should give a better estimate and/or shrink the query rectangle during 
		//the query!
		return true;
	}

	@Override
	public int unload() {
		System.err.println("UNLOAD not feasible");
		//xtr.clear();
		return N;
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
	public String toString() {
		return xtr.toString();
	}
}
