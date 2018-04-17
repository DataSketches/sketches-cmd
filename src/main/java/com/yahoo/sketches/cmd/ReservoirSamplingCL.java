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

  protected ReservoirItemsSketch<Long> buildSketch() {
    final int k = cl.hasOption("k") ? Integer.parseInt(cl.getOptionValue("k")) : 32;
    return ReservoirItemsSketch.newInstance(k);
  }

  @Override
  protected void updateSketch(final BufferedReader br) {
    buildSketch();
    String itemStr = "";
    final ReservoirItemsSketch<Long> sketch = buildSketch();
    try {
      while ((itemStr = br.readLine()) != null) {
        final long item = Long.parseLong(itemStr);
        sketch.update(item);
      }
      sketchList.add(sketch);
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
    if (sketchList.size() > 0) {
      final ReservoirItemsSketch<Long>  sketch =  sketchList.get(sketchList.size() - 1);
      boolean optionChosen = false;

      if (cl.hasOption("o")) {
        optionChosen = true;
      }

      if (!!optionChosen) {
        final Long[] samples = sketch.getSamples();
        println("\nUniform Samples");
        for (int i = 0; i < samples.length; i++) {
            System.out.println(samples[i]);
        }
      }
    }
  }
}
