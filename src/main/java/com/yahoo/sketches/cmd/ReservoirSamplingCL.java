package com.yahoo.sketches.cmd;

import java.io.BufferedReader;
import java.io.IOException;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;

import com.yahoo.memory.Memory;
import com.yahoo.sketches.ArrayOfLongsSerDe;
import com.yahoo.sketches.sampling.ReservoirItemsSketch;
import com.yahoo.sketches.sampling.ReservoirItemsUnion;


public class ReservoirSamplingCL extends SketchCommandLineParser<ReservoirItemsSketch<Long>> {
  ReservoirSamplingCL() {
    super();
    // input options
    options.addOption(Option.builder("k")
        .desc("parameter k")
        .hasArg()
        .build());

  }

  @Override
  protected void showHelp() {
        final HelpFormatter helpf = new HelpFormatter();
        helpf.setOptionComparator(null);
        helpf.printHelp( "ds rsamp", options);
  }


  @Override
  protected void buildSketch() {
    final ReservoirItemsSketch<Long> sketch;
    if (cl.hasOption("k")) {
      sketch = //user defined k
          ReservoirItemsSketch.newInstance(Integer.parseInt(cl.getOptionValue("k")));
    } else {
      sketch = ReservoirItemsSketch.newInstance(32); // default k is 32
    }
    sketchList.add(sketch);
  }

  @Override
  protected void updateSketch(final BufferedReader br) {
    String itemStr = "";
    final ReservoirItemsSketch<Long> sketch = sketchList.get(sketchList.size() - 1);
    try {
      while ((itemStr = br.readLine()) != null) {
        final long item = Long.parseLong(itemStr);
        sketch.update(item);
      }
    } catch (final IOException | NumberFormatException e ) {
      printlnErr("Read Error: Item: " + itemStr + ", " + br.toString());
      throw new RuntimeException(e);
    }
  }

  @Override
  protected ReservoirItemsSketch<Long>  deserializeSketch(final byte[] bytes) {
    return ReservoirItemsSketch.heapify(Memory.wrap(bytes), new ArrayOfLongsSerDe());
  }

  @Override
  protected byte[] serializeSketch(final ReservoirItemsSketch<Long> sketch) {
    return sketch.toByteArray(new ArrayOfLongsSerDe());
  }

  @Override
  protected void mergeSketches() {
    final int k = sketchList.get(sketchList.size() - 1).getK();
    final ReservoirItemsUnion<Long> union = ReservoirItemsUnion.newInstance(k);
    for (ReservoirItemsSketch<Long>  sketch: sketchList) {
      union.update(sketch);
    }
    sketchList.add(union.getResult());
  }

  @Override
  protected void queryCurrentSketch() {
    final ReservoirItemsSketch<Long>  sketch =  sketchList.get(sketchList.size() - 1);
    final Long[] samples = sketch.getSamples();
    println("\nUniform Samples");
    for (int i = 0; i < samples.length; i++) {
        System.out.println(samples[i]);
    }
    return;
  }
}
