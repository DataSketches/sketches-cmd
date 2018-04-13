package com.yahoo.sketches.cmd;

import static com.yahoo.sketches.Util.TAB;

import java.io.BufferedReader;
import java.io.IOException;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;

import com.yahoo.memory.Memory;
import com.yahoo.sketches.ArrayOfStringsSerDe;
import com.yahoo.sketches.sampling.VarOptItemsSamples;
import com.yahoo.sketches.sampling.VarOptItemsSketch;
import com.yahoo.sketches.sampling.VarOptItemsUnion;

public class VarOptSamplingCL extends SketchCommandLineParser<VarOptItemsSketch<String>> {
  VarOptSamplingCL() {
    super();
    // input options
    options.addOption(Option.builder("k")
        .desc("parameter k")
        .hasArg()
        .build());
    options.addOption(Option.builder("n")
        .longOpt("stream-length")
        .desc("query stream length")
        .build());
    options.addOption(Option.builder("m")
        .longOpt("num-samples")
        .desc("query number of samples retained")
        .build());
    options.addOption(Option.builder("w")
        .desc("Each line is two tokens separated by a tab, comma, or spaces. "
            + "Token 0 is a floating point weight, the second token is the item. "
            + "If there is only one token it is assumed to be the item with weight = 1.0.")
        .longOpt("weights")
        .build());
  }

  @Override
  protected void showHelp() {
    final HelpFormatter helpf = new HelpFormatter();
      helpf.setOptionComparator(null);
      helpf.printHelp( "ds vsamp", options);
  }

  @Override
  protected void buildSketch() {
    final VarOptItemsSketch<String> sketch;
    if (cl.hasOption("k")) { // user defined k
      sketch = VarOptItemsSketch.newInstance(Integer.parseInt(cl.getOptionValue("k")));
    } else {
      sketch = VarOptItemsSketch.newInstance(32); // default k is 32
    }
    sketches.add(sketch);
  }

  @Override
  protected void updateSketch(final BufferedReader br) {
    final VarOptItemsSketch<String> sketch = sketches.get(sketches.size() - 1);
    String itemStr = "";
    try {
      if (cl.hasOption("w")) {
        while ((itemStr = br.readLine()) != null) {
          if (itemStr.isEmpty()) { continue; }
          final String[] tokens = itemStr.split("[\\t, ]+", 2);
          if (tokens.length < 2) {
            sketch.update(tokens[1], 1.0);
          } else {
            sketch.update(tokens[1], Double.parseDouble(tokens[0]));
          }
        }
      } else { //assume entire line is item
        while ((itemStr = br.readLine()) != null) {
          if (itemStr.isEmpty()) { continue; }
          sketch.update(itemStr, 1.0);
        }
      }
    } catch (final IOException | NumberFormatException e) {
      printlnErr("Read Error: Item: " + itemStr + ", " + br.toString());
      throw new RuntimeException(e);
    }
  }

  @Override
  protected VarOptItemsSketch<String>  deserializeSketch(final byte[] bytes) {
    final Memory mem = Memory.wrap(bytes);
    final VarOptItemsSketch<String> sketch =
        VarOptItemsSketch.heapify(mem, new ArrayOfStringsSerDe());
    return sketch;
  }

  @Override
  protected byte[] serializeSketch(final VarOptItemsSketch<String> sketch) {
    return sketch.toByteArray(new ArrayOfStringsSerDe());
  }

  @Override
  protected void mergeSketches() {
    final int k = sketches.get(sketches.size() - 1).getK();
    final VarOptItemsUnion<String> union = VarOptItemsUnion.newInstance(k);
    for (VarOptItemsSketch<String>  sketch: sketches) {
      union.update(sketch);
    }
    sketches.add(union.getResult());
  }

  @Override
  protected void queryCurrentSketch() {
    if (sketches.size() > 0) {
      final VarOptItemsSketch<String>  sketch =  sketches.get(sketches.size() - 1);

      if (cl.hasOption("n")) {
        final String n = Long.toString(sketch.getN());
        println("Stream Length: " + n);
      }

      if (cl.hasOption("m")) {
        final String m = Long.toString(sketch.getNumSamples());
        println("Num Samples  : " + m);
      }

      final VarOptItemsSamples<String> samples = sketch.getSketchSamples();
      println("\nItems" + TAB + "Weights");
      for (VarOptItemsSamples<String>.WeightedSample ws : samples) {
        System.out.println(ws.getItem() + "\t" + ws.getWeight());
      }
    }
  }
}
