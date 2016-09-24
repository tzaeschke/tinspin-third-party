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
import org.xxl.xtree2.IteratorCursor;
import org.xxl.xtree2.KPE;
import org.xxl.xtree2.MapContainer;
import org.xxl.xtree2.SortBasedBulkLoading;
import org.xxl.xtree2.XTree;

import ch.ethz.globis.tinspin.TestStats;



/**
 * Wrapper for X-Tree from XXL library:
 * https://github.com/umr-dbs/xxl
 *
 */
public class PointXtree extends Candidate {

	private XTree xtr;
	
	private final int dims;
	private final int N;
	
	public PointXtree(TestStats ts) {
		this.dims = ts.cfgNDims;
		this.N = ts.cfgNEntries;
	}
	
	@Override
	public void load(double[] data, int idxDim) {
		xtr = new XTree();
		/* Factor which the minimum capacity of nodes is smaller than the maximum capacity. */
		double minMaxFactor = 1.0/3.0;
		int blockSize = 1536;
		int dataSize = dims*2*8+4; // DoublePointRectangle (2 doubles per dimension) + int
		int descriptorSize = dims*2*8; // DoublePointRectangle (2 doubles per dimension)
		//boolean useMultiBlockContainer = true;//tree.equalsIgnoreCase("x");
		int entrySize = Math.max(dataSize, descriptorSize+8);
		int xTreeMaxCap = (blockSize - 6) / entrySize;
		int xTreeMinCap = (int) (xTreeMaxCap * minMaxFactor);
//		Function<KPE, DoublePointRectangle> GET_DESCRIPTOR = new AbstractFunction<KPE, DoublePointRectangle>() {
//			public DoublePointRectangle invoke (KPE o) {
//				return ((KPE)o).getKey(); 
//				//return new DoublePointRectangle((DoublePoint)o, (DoublePoint)o);
//				//DoublePoint dp = (DoublePoint) ((KPE)o).getData();
//				//return new DoublePointRectangle(dp, dp);
//			}
//		};
		//TODO do we need to used a clone-container?
		Container container = new MapContainer();
		xtr.initialize(container, xTreeMinCap, xTreeMaxCap, dims);
		int j = 0;
		int p = N/100;
		ArrayList<KPE> bulk = new ArrayList<KPE>(N);
		for (int i = 0; i < N; i++) {
			double[] buf = new double[idxDim];
			for (int d = 0; d < idxDim; d++) {
				buf[d] = data[idxDim*i+d]; 
			}
			// insert new point
			//DoublePoint dp = new DoublePoint(buf);
			//TODO ???
			//KPE kpe = new KPE(new DoublePointRectangle(buf, buf2), i, IntegerConverter.DEFAULT_INSTANCE);
			//KPE kpe = new KPE(new DoublePointRectangle(buf, buf2), i);
			KPE kpe = new KPE(new DoublePointRectangle(buf, buf), i);
			bulk.add(kpe);
			//xtr.insert(kpe);
			
			if (++j%N==0)
				System.out.print(j/N+"%, ");
		}
		Cursor<KPE> cursor = new IteratorCursor<KPE>(bulk);
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
//			return o1.getKey().compareTo(o2.getKey()); 
//		}
//	};

	@Override
	public DoublePointRectangle[] preparePointQuery(double[][] qA) {
		DoublePointRectangle[] r = new DoublePointRectangle[qA.length];
		for (int i = 0; i < qA.length; i++) {
			double[] q = qA[i];
			r[i] = new DoublePointRectangle(q, q);
		}
		return r;
	}

	@Override
	public int pointQuery(Object qA) {
		DoublePointRectangle[] a = (DoublePointRectangle[]) qA;
		int n = 0;
		for (DoublePointRectangle q: a) {
			if (xtr.get(q) != null) {
				n++;
			}
			//log("q=" + Arrays.toString(q));
			//This is just too slow
			if (n == 1000) {
				break;
			}
		}
		return n;
	}

	@Override
	public int query(double[] min, double[] max) {
		double[] lower = new double[dims];
		double[] upper = new double[dims];
		for (int i = 0; i < dims; i++) {
			lower[i] = min[i];
			upper[i] = max[i];
		}
		int n = 0;
		Descriptor q = new DoublePointRectangle(lower, upper);
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

	@Override
	public int unload() {
		System.err.println("UNLOAD not feasible");
		xtr.clear();
		return N;
	}

	@Override
	public void release() {
		// nothing to be done
	}

	@Override
	public int update(double[][] updateTable) {
		int n = 0;
		for (int i = 0; i < updateTable.length; ) {
			double[] p1 = updateTable[i++];
			double[] p2 = updateTable[i++];
			double[] buf = new double[p1.length];
			for (int d = 0; d < buf.length; d++) {
				buf[d] = p1[d]; 
			}
			// insert new point
			double[] buf2 = new double[p1.length];
			for (int d = 0; d < buf2.length; d++) {
				buf2[d] = p2[d]; 
			}
//			if (xtr.remove(kpe) != null) {
//				xtr.insert(kpe2);
//				n++;
//			}
			xtr.update(new DoublePointRectangle(buf, buf), new DoublePointRectangle(buf2, buf2));
			n++;
		}
		return n;
	}
	
	@Override
	public boolean supportsUpdate() {
		//This is just too slow
		return true;
	}
}
