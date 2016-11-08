/*
 * Copyright 2011-2016 ETH Zurich. All Rights Reserved.
 *
 * This software is the proprietary information of ETH Zurich.
 * Use is subject to license terms.
 */
package ch.ethz.globis.tinspin.wrappers;

import java.util.ArrayList;

import org.xxl.xtree2.Constant;
import org.xxl.xtree2.Container;
import org.xxl.xtree2.Cursor;
import org.xxl.xtree2.Descriptor;
import org.xxl.xtree2.DoublePointRectangle;
import org.xxl.xtree2.DoublePointRectangleEntry;
import org.xxl.xtree2.IteratorCursor;
import org.xxl.xtree2.MapContainer;
import org.xxl.xtree2.SortBasedBulkLoading;
import org.xxl.xtree2.XTree;

import ch.ethz.globis.tinspin.TestStats;



/**
 * Wrapper for X-Tree from XXL library:
 * https://github.com/umr-dbs/xxl
 *
 */
public class RectangleXtree extends Candidate {


	private XTree xtr;
	
	private final int DIM;
	private final int N;
	
	public RectangleXtree(TestStats ts) {
		this.DIM = ts.cfgNDims;
		this.N = ts.cfgNEntries;
	}
	
	@Override
	public void load(double[] data, int idxDim) {
		xtr = new XTree();
		/* Factor which the minimum capacity of nodes is smaller than the maximum capacity. */
		double minMaxFactor = 1.0/3.0;
		int blockSize = 1536;
		int dataSize = DIM*2*8+4; // DoublePointRectangle (2 doubles per dimension) + int
		int descriptorSize = DIM*2*8; // DoublePointRectangle (2 doubles per dimension)
		//boolean useMultiBlockContainer = true;//tree.equalsIgnoreCase("x");
		int entrySize = Math.max(dataSize, descriptorSize+8);
		int xTreeMaxCap = (blockSize - 6) / entrySize;
		int xTreeMinCap = (int) (xTreeMaxCap * minMaxFactor);
		//TODO do we need to used a clone-container?
		Container container = new MapContainer();
		xtr.initialize(container, xTreeMinCap, xTreeMaxCap, DIM);
		int j = 0;
		int p = N/100;
		ArrayList<DoublePointRectangleEntry<?>> bulk = new ArrayList<DoublePointRectangleEntry<?>>(N);
		int pos = 0;
		for (int n = 0; n < N; n++) {
			double[] lo = new double[DIM];
			double[] hi = new double[DIM];
			System.arraycopy(data, pos, lo, 0, DIM);
			pos += DIM;
			System.arraycopy(data, pos, hi, 0, DIM);
			pos += DIM;
			// insert new point
			//KPE kpe = new KPE(new DoublePointRectangle(buf, buf2), i, IntegerConverter.DEFAULT_INSTANCE);
			//KPE kpe = new KPE(new DoublePointRectangle(lo, hi), n);
			//bulk.add(kpe);
			bulk.add(new DoublePointRectangleEntry<>(lo, hi));
			//xtr.insert(kpe);
			
			if (++j%N==0)
				System.out.print(j/N+"%, ");
		}
		//Collections.sort(bulk, COMPARE); //TODO this has very bad impact on performance....?!?!?!?
		//Cursor<KPE> cursor = new IteratorCursor<KPE>(bulk);
		Cursor<DoublePointRectangleEntry<?>> cursor = new IteratorCursor<DoublePointRectangleEntry<?>>(bulk);
		//cursor = new MergeSorter(cursor, COMPARE, dataSize, 4*4096, 4*4096); 
		new SortBasedBulkLoading(xtr, cursor, new Constant(container));
		cursor.close();
		
		container.flush();
	}
	
	/** 
	 * A function that sorts the rectangles according to their lower left border.
	 * This is needed for sort-based bulk insertion.
	 */
//	private static Comparator<KPE> COMPARE = new Comparator<KPE>() {
//		public int compare(KPE o1, KPE o2) {
//			return ((Rectangle)(((KPE)o1).getData())).compareTo(((KPE)o2).getData()); 
//		}
//	};

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
//		for (int i = 0; i < a.length; i+=2) {
//			DoublePointRectangle r = new DoublePointRectangle(a[i], a[i+1]);
//			if (xtr.get(r) != null) {
//				n++;
//			}
////			Cursor<?> it = xtr.query(r);
////			while (it.hasNext()) {
////				KPE kpe = (KPE) it.next();
////				if (kpe.getData().equals(r)) {
////					n++;
////					break;
////				}
////			}
//			//log("q=" + Arrays.toString(q));
//		}
//		return n;
		return -1;
	}

	@Override
	public int query(double[] min, double[] max) {
		int n = 0;
		Descriptor q = new DoublePointRectangle(min, max);
		Cursor<?> it = xtr.query(q);
		while (it.hasNext()) {
			it.next();
			n++;
		}
//		log("N=" + N);
//		log("n=" + n);
//		log("q=" + lower + " / " + upper);
		return n;
	}

//	@Override
//	public double knnQuery(int k, double[] center) {
//		SortedLinList res = new SortedLinList();
//		DataPoint p = new DataPoint(center);
//		xtr.rt.k_NearestNeighborQuery(p, k, res);
//		double totalDist = 0;
//		int n = 0;
//        for (Object obj = res.get_first(); obj != null; obj = res.get_next()) {
//        	Data d = (Data) obj;
//        	totalDist += Math.sqrt(d.distanz);
//        	n++;
//        }
//        if (n < k) {
//        	throw new IllegalStateException("n/k=" + n + "/" + k);
//        }
//		return totalDist;
//	}
	
	@Override
	public int unload() {
		System.err.println("UNLOAD not feasible");
		xtr.clear();
		return N;
	}
	
	@Override
	public boolean supportsUnload() {
		return false;
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
