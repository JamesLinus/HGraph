/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.trend.hgraph.mapreduce.pagerank;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

import org.apache.commons.lang.time.StopWatch;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import org.trend.hgraph.HBaseGraphConstants;

/**
 * A <code>Reducer</code> for calculating new pageranks by its upstream <code>Mapper</code>s.
 * @author scott_miao
 * @see CalculateInitPageRankMapper
 * @see CalculateIntermediatePageRankMapper
 */
public class CalculatePageRankReducer extends
 Reducer<Text, DoubleWritable, Text, DoubleWritable> {
  
  public static enum Counters {
    CHANGED_PAGE_RANK_COUNT, CAL_NEW_PR_TIME_CONSUMED, CMP_OLD_NEW_PR_TIME_CONSUMED
  }
  
  private double verticesTotalCnt = 1.0D;
  private double dampingFactor = Constants.PAGE_RANK_DAMPING_FACTOR_DEFAULT_VALUE;
  private HTable vertexTable = null;
  private int pageRankCompareScale = 3;

  /*
   * (non-Javadoc)
   * @see org.apache.hadoop.mapreduce.Reducer#reduce(java.lang.Object, java.lang.Iterable, Context)
   */
  @Override
  protected void reduce(Text key, Iterable<DoubleWritable> incomingPageRanks,
      Context context) throws IOException, InterruptedException {

    String rowkey = Bytes.toString(key.getBytes()).trim();
    double incomingPageRankSum = 0.0D;
    StopWatch sw = new StopWatch();
    sw.start();
    for (DoubleWritable incomingPageRank : incomingPageRanks) {
      incomingPageRankSum = incomingPageRankSum + incomingPageRank.get();
    }
    // calculate new pageRank here
    double newPageRank =
        (dampingFactor * incomingPageRankSum) + ((1.0D - dampingFactor) / verticesTotalCnt);
    sw.stop();
    context.getCounter(Counters.CAL_NEW_PR_TIME_CONSUMED).increment(sw.getTime());
    
    sw.reset(); sw.start();
    double oldPageRank = Utils.getPageRank(vertexTable, rowkey, Constants.PAGE_RANK_CQ_TMP_NAME);
    if (!pageRankEquals(oldPageRank, newPageRank, pageRankCompareScale)) {
      // collect pageRank changing count with counter
      context.getCounter(Counters.CHANGED_PAGE_RANK_COUNT).increment(1);
    }
    sw.stop();
    context.getCounter(Counters.CMP_OLD_NEW_PR_TIME_CONSUMED).increment(sw.getTime());

    context.write(key, new DoubleWritable(newPageRank));
  }

  static boolean pageRankEquals(double src, double dest, int scale) {
    BigDecimal a = new BigDecimal(src);
    BigDecimal b = new BigDecimal(dest);
    a = a.setScale(scale, RoundingMode.DOWN);
    b = b.setScale(scale, RoundingMode.DOWN);
    return a.compareTo(b) == 0 ? true : false;
  }

  /*
   * (non-Javadoc)
   * @see org.apache.hadoop.mapreduce.Reducer#setup(org.apache.hadoop.mapreduce.Reducer.Context)
   */
  @Override
  protected void setup(Context context) throws IOException, InterruptedException {
    Configuration conf = context.getConfiguration();

    vertexTable =
        Utils.initTable(conf, HBaseGraphConstants.HBASE_GRAPH_TABLE_VERTEX_NAME_KEY,
          this.getClass());

    String value = conf.get(Constants.PAGE_RANK_VERTICES_TOTAL_COUNT_KEY);
    if (null != value) verticesTotalCnt = Double.parseDouble(value);

    value = conf.get(Constants.PAGE_RANK_DAMPING_FACTOR_KEY);
    if (null != value) dampingFactor = Double.parseDouble(value);
  }

  /*
   * (non-Javadoc)
   * @see org.apache.hadoop.mapreduce.Reducer#cleanup(org.apache.hadoop.mapreduce.Reducer.Context)
   */
  @Override
  protected void cleanup(Context context) throws IOException, InterruptedException {
    vertexTable.close();
  }

}
