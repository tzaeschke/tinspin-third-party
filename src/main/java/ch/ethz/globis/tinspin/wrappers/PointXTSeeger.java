/*
 * Copyright 2011-2016 ETH Zurich. All Rights Reserved.
 *
 * This software is the proprietary information of ETH Zurich.
 * Use is subject to license terms.
 */
package ch.ethz.globis.tinspin.wrappers;

import java.util.ArrayList;

import org.seegerx.Data;
import org.seegerx.XTree;

import ch.ethz.globis.tinspin.TestStats;


/**
 * Wrapper for X-Tree by Seeger:
 * http://chorochronos.datastories.org/
 */
public class PointXTSeeger extends Candidate {


	private XTree<Object> rt;
	
	private final int dims;
	private final int N;
	
	public PointXTSeeger(TestStats ts) {
		this.dims = ts.cfgNDims;
		this.N = ts.cfgNEntries;
	}
	
	@Override
	public void load(double[] data, int idxDim) {
		rt = new XTree<>(dims);
		
		int j = 0;
		int pos = 0;
		for (int n = 0; n < N; n++) {
			double[] p = new double[dims];
			for (int d = 0; d < dims; d++) {
				p[d] = data[pos+d]; 
			}
			pos += dims;
			
			Data rect = new Data(p);
			rt.insert(rect);
			
			if (++j%N==0) {
				System.out.print(j/N+"%, ");
			}
		}
	}
	
	@Override
	public double[][] preparePointQuery(double[][] qA) {
//		DoublePointRectangle[] r = new DoublePointRectangle[qA.length];
//		for (int i = 0; i < qA.length; i++) {
//			double[] q = qA[i];
//			double[] q2 = new double[DIM];
//			for (int d = 0; d < DIM; d++) {
//				q2[d] = q[d] + E;
//			}
//			r[i] = new DoublePointRectangle(q, q2);
//		}
//		return r;
		return qA;
	}

	@Override
	public int pointQuery(Object qA) {
//		double[][] a = (double[][]) qA;
//		int n = 0;
//		
//		SortedLinList res = new SortedLinList();
//		double[] mbr = new double[dims*2];
//		
//		for (int i = 0; i < a.length; i+=2) {
//			double[] min = a[i];
//			double[] max = a[i+1];
//			for (int d = 0; d < dims; d++) {
//				mbr[2*d] = min[d];
//				mbr[2*d+1] = max[d];
//			}
//			xtr.rangeQuery(mbr, res);
//			for (Object obj = res.get_first(); obj != null; obj = res.get_next()) {
//				Data d = (Data) obj;
//				if (Arrays.equals(d.data, mbr)) {
//					n++;
//					break;
//				}
//			}
//			//log("q=" + Arrays.toString(q));
//		}
//		return n;
		return -1;
	}

	@Override
	public int query(double[] min, double[] max) {
		int n = 0;
		ArrayList<Data> ret = rt.rangeQuery(min, max);
        for (int i = 0; i < ret.size(); i++) {
			n++;
        }
		return n;
	}

	@Override
	public double knnQuery(int k, double[] center) {
		//This causes NPEs...
//		if (k==1) {
//			PPoint result = new PPoint(dims);
//			rt.NearestNeighborQuery(p, result);
//			return result.dist();
//		}
		
		ArrayList<Data> ret = rt.kNearestNeighbourSearch(center, k);
		double totalDist = 0;
		int n = 0;
        for (int i = 0; i < ret.size(); i++) {
        	Data d = ret.get(i);
        	totalDist += dist(center, d.getCords());
        	//The 'distanz' does not match at all with the actual distance...
        	//totalDist += Math.sqrt(d.distanz);
        	n++;
        	if (n==k) {
        		break;
        	}
        }
        if (n < k) {
        	//throw new IllegalStateException("n/k=" + n + "/" + k);
        	System.out.println("ERROR: n/k=" + n + "/" + k);
        }
		return totalDist;
	}
	
	@Override
	public boolean supportsKNN() {
		//disabled because of wrong results
		return true;
	}
	
	@Override
	public int unload() {
		System.err.println("UNLOAD not feasible");
		return N;
	}

	@Override
	public void release() {
		// nothing to be done
	}

	@Override
	public int update(double[][] updateTable) {
//		int n = 0;
//		for (int i = 0; i < updateTable.length; ) {
//			double[] lo1 = updateTable[i++];
//			double[] up1 = updateTable[i++];
//			double[] lo2 = updateTable[i++];
//			double[] up2 = updateTable[i++];
//			DoublePointRectangle p1 = new DoublePointRectangle(lo1, up1);
//			DoublePointRectangle p2 = new DoublePointRectangle(lo2, up2);
//			//TODO use update(data,data) instead?
//			//xtr.update(p1, new KPE(p2));
//			xtr.update(p1, p2);
//			//TODO no check yet...
//			n++;
//		}
//		return n;
		System.err.println("UPDATE not feasible");
		return -1;
	}
	
	@Override
	public boolean supportsUpdate() {
		return false;
	}
	
	@Override
	public String toString() {
		return rt.toString();
	}
	
}