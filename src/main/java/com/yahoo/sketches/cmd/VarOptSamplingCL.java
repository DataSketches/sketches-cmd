package com.yahoo.sketches.cmd;

import static com.yahoo.sketches.Util.TAB;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;

import com.yahoo.memory.Memory;
import com.yahoo.sketches.ArrayOfStringsSerDe;
import com.yahoo.sketches.sampling.SampleSubsetSummary;
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
    options.addOption(Option.builder("r")
        .longOpt("num-samples")
        .desc("query number of samples retained")
        .build());
    options.addOption(Option.builder("x")
        .longOpt("reg-ex")
        .hasArg()
        .desc("query retained samples by given regEx and compute subset sum")
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

  protected VarOptItemsSketch<String> buildSketch() {
    final int k = cl.hasOption("k") ? Integer.parseInt(cl.getOptionValue("k")) : 32;
    return VarOptItemsSketch.newInstance(k);
  }

  @Override
  protected void updateSketch(final BufferedReader br) {
    final VarOptItemsSketch<String> sketch = buildSketch();
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
      sketchList.add(sketch);
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
    final int k = sketchList.get(sketchList.size() - 1).getK();
    final VarOptItemsUnion<String> union = VarOptItemsUnion.newInstance(k);
    for (VarOptItemsSketch<String>  sketch: sketchList) {
      union.update(sketch);
    }
    sketchList.add(union.getResult());
  }

  @Override
  protected void queryCurrentSketch() {
    if (sketchList.size() > 0) {
      final VarOptItemsSketch<String>  sketch =  sketchList.get(sketchList.size() - 1);
      boolean optionChosen = false;

      if (cl.hasOption("n")) {
        final String n = Long.toString(sketch.getN());
        println("Stream Length   : " + n);
      }

      if (cl.hasOption("r")) {
        final String ret = Long.toString(sketch.getNumSamples());
        println("Samples Retained: " + ret);
      }

      if (cl.hasOption("o")) {
        optionChosen = true;
      }

      if (cl.hasOption("x")) {
        optionChosen = true;
        final String regex = cl.getOptionValue("x");
        final Predicate<String> predicate = Pattern.compile(regex).asPredicate();
        final SampleSubsetSummary ssSum = sketch.estimateSubsetSum(predicate);
        println("Lower Bound Sum: " + ssSum.getLowerBound());
        println("Estimate Sum   : " + ssSum.getEstimate());
        println("Upper Bound Sum: " + ssSum.getUpperBound());
        println("Total Sketch Wt: " + ssSum.getTotalSketchWeight());
      }

      if (!!optionChosen) {
        final VarOptItemsSamples<String> samples = sketch.getSketchSamples();
        println("\nItems" + TAB + "Weights");
        for (VarOptItemsSamples<String>.WeightedSample ws : samples) {
          System.out.println(ws.getItem() + "\t" + ws.getWeight());
        }
      }
    }
  }
}
