/*
 * Copyright 2011-2016 ETH Zurich. All Rights Reserved.
 *
 * This software is the proprietary information of ETH Zurich.
 * Use is subject to license terms.
 */
package ch.ethz.globis.tinspin.wrappers;

import io.prelink.critbit.MCritBitTree;

import java.util.Map.Entry;

import org.ardverk.collection.Cursor;
import org.ardverk.collection.IntArrayKeyAnalyzer;

import ch.ethz.globis.phtree.util.BitTools;
import ch.ethz.globis.tinspin.TestStats;
import ch.ethz.globis.tinspin.wrappers.Candidate;

/**
 * Wrapper for CritBit index by:
 * https://github.com/jfager/functional-critbit
 */
public class PointCBF extends Candidate {

	private MCritBitTree<int[], Object> cbf;
	
	private final int dims;
	private final int DEPTH = 64;
	private final int N;
	
	private double[] data;
	
	public PointCBF(TestStats ts) {
		this.dims = ts.cfgNDims;
		this.N = ts.cfgNEntries;
	}
	
	@Override
	public void load(double[] data, int idxDim) {
		this.data = data;
		cbf = new MCritBitTree<int[], Object>(new IntArrayKeyAnalyzer(idxDim, DEPTH));
		long[] buf = new long[idxDim];
		Object dummy = new Object();
		for (int i = 0; i < N; i++) {
			for (int d = 0; d < idxDim; d++) {
				buf[d] = BitTools.toSortableLong(data[idxDim*i+d]); 
			}
			
			int[] key = BitTools.merge(DEPTH, buf);
			if (cbf.put(key, dummy) != null) {
				System.out.println("Dupl!");
			}
		}
	}
	
	@Override
	public long[][] preparePointQuery(double[][] qA) {
		long[][] r = new long[qA.length][];
		long[] val = new long[dims];
		for (int i = 0; i < qA.length; i++) {
			double[] q = qA[i];
			for (int d = 0; d < dims; d++) {
				val[d] = BitTools.toSortableLong(q[d]);
			}
			r[i] = val; 
		}
		return r;
	}

	@Override
	public int pointQuery(Object qA) {
		int n = 0;
		long[][] a = (long[][]) qA;
		for (long[] q: a) {
			//log("q=" + q);
			int[] key = BitTools.merge(DEPTH, q);
			if (cbf.containsKey(key)) {
				n++;
			}
		}
		return n;
	}

	@Override
	public boolean supportsWindowQuery() {
		return N <= 10000;
	}
	
	@Override
	public int query(double[] min, double[] max) {
		final long[] lower = new long[dims];
		final long[] upper = new long[dims];
		for (int i = 0; i < dims; i++) {
			lower[i] = BitTools.toSortableLong(min[i]);
			upper[i] = BitTools.toSortableLong(max[i]);
		}

		//query index
		final int[] fromKey;
		final int[] toKey;
		fromKey = BitTools.merge(DEPTH, lower);
		toKey = BitTools.merge(DEPTH, upper);
		
		//TODO
		//TODO
		//This does not work. traverse() starts always from the beginning, which is bad.
		//traverseWithPrefix() is also bad:
		//a) How to set prefix length???? lentgh of int[]??   Count only '1'-bits ??? --> BAD
		//b) Prefix traversal is anyway bad: traversing from 01111 to 10000 has not comon prefix
		//   --> Full scan --> BAD
		//TODO
		//TODO
		//TODO
		
		
		final int[] nA = {0};
		final int[] nnA = {0};
		Cursor<int[], Object> cursor = new Cursor<int[], Object>() {
			@Override
			public org.ardverk.collection.Cursor.Decision select(
					Entry<? extends int[], ? extends Object> entry) {
				nnA[0]++;
				int[] key = entry.getKey();
				for (int ik = 0; ik < key.length; ik++) {
					if (key[ik] < toKey[ik]) {
						//key can not be larger than toKey
						break;
					}
					if (key[ik] > toKey[ik]) {
						//key is to large
//						System.out.println("frKey=" + Bits.toBinary(fromKey));
//						System.out.println("key  =" + Bits.toBinary(key));
//						System.out.println("toKey=" + Bits.toBinary(toKey));
						return Decision.EXIT;
					}
					//keys are equal up to here, check further...
				}

				long[] val = BitTools.split(dims, DEPTH, key);
				boolean match = true;
				for (int d = 0; d < dims; d++) {
					if (val[d] < lower[d] || val[d] > upper[d]) {
						match = false;
						break;
					}
				}
				if (match) {
					nA[0]++;
				}
				return Decision.CONTINUE;
			} 
		};
		cbf.traverse(cursor);
//		System.out.println("MIN  =" + Bits.toBinary(cbf.min().getKey()));
//		System.out.println("frKey=" + Bits.toBinary(fromKey));
//		System.out.println("toKey=" + Bits.toBinary(toKey));
//		System.out.println("MAX  =" + Bits.toBinary(cbf.max().getKey()));
		cbf.traverseWithPrefix(fromKey, cursor);
//		//log("q=" + Arrays.toString(q));
//		System.out.println("nn=" + nnA[0] + "  n=" + nA[0]); //TODO
		return nA[0];
	}

	@Override
	public int unload() {
		int n = 0;
		long[] e = new long[dims];
		for (int i = 0; i < N>>1; i++) {
			n += cbf.remove(getEntryInt(e, i)) != null ? 1 : 0;
			n += cbf.remove(getEntryInt(e, N-i-1)) != null ? 1 : 0;
		}
		return n;
	}

	private int[] getEntryInt(long[] buf, int pos) {
		for (int d = 0; d < dims; d++) {
			buf[d] = BitTools.toSortableLong(data[pos*dims+d]); 
		}
		
		int[] key = BitTools.merge(DEPTH, buf);
		return key;
	}
	
	@Override
	public void release() {
		// nothing to be done
	}
	
	@Override
	public int update(double[][] updateTable) {
		Object dummy = new Object();
		int n = 0;
		for (int i = 0; i < updateTable.length; ) {
			int[] p1 = toKey(updateTable[i++]);
			int[] p2 = toKey(updateTable[i++]);
			if (cbf.remove(p1) != null) {
				cbf.put(p2, dummy);
				n++;
			}
		}
		return n;
	}
	
	private int[] toKey(double[] src) {
		long[] buf = new long[src.length];
		for (int d = 0; d < dims; d++) {
			buf[d] = BitTools.toSortableLong(src[d]); 
		}
		return BitTools.merge(DEPTH, buf);
	}
	

}
