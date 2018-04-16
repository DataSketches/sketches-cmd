package com.yahoo.sketches.cmd;

import java.io.BufferedReader;
import java.io.IOException;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;

import com.yahoo.memory.Memory;
import com.yahoo.sketches.hll.HllSketch;
import com.yahoo.sketches.hll.TgtHllType;
import com.yahoo.sketches.hll.Union;

public class HllCL extends SketchCommandLineParser<HllSketch> {

  private static final int DEFAULT_LG_K = 12;

  HllCL() {
    super();
    // input options
    options.addOption(Option.builder("lgk")
        .desc("parameter lgK = log2(k)")
        .hasArg()
        .build());
  }

  @Override
  protected void showHelp() {
        final HelpFormatter helpf = new HelpFormatter();
        helpf.setOptionComparator(null);
        helpf.printHelp("ds hll", options);
  }

  protected HllSketch buildSketch() {
    final int lgk = cl.hasOption("lgk")
        ? Integer.parseInt(cl.getOptionValue("lgk")) : DEFAULT_LG_K;
    return new HllSketch(lgk);
  }

  @Override
  protected void updateSketch(final BufferedReader br) {
    final HllSketch sketch = buildSketch();
    String itemStr = "";
    try {
      while ((itemStr = br.readLine()) != null) {
        sketch.update(itemStr);
      }
      sketchList.add(sketch);
    } catch (final IOException e) {
      printlnErr("Read Error: Item: " + itemStr + ", " + br.toString());
      throw new RuntimeException(e);
    }
  }

  @Override
  protected HllSketch deserializeSketch(final byte[] bytes) {
    return HllSketch.heapify(Memory.wrap(bytes));
  }

  @Override
  protected byte[] serializeSketch(final HllSketch sketch) {
    return sketch.toCompactByteArray();
  }

  @Override
  protected void mergeSketches() {
    final int lgk = cl.hasOption("k") ? Integer.parseInt(cl.getOptionValue("lgk")) : DEFAULT_LG_K;
    final Union union = new Union(lgk);
    for (HllSketch sketch: sketchList) {
      union.update(sketch);
    }
    sketchList.add(union.getResult(TgtHllType.HLL_4));
  }

  @Override
  protected void queryCurrentSketch() {
    if (sketchList.size() > 0) {
      final HllSketch sketch = sketchList.get(sketchList.size() - 1);
      final double est = sketch.getEstimate();
      final double lb = sketch.getLowerBound(2);
      final double ub = sketch.getUpperBound(2);
      System.out.format("%f %f %f\n",lb, est, ub);
    }
  }

}
