package com.yahoo.sketches.cmd;

import java.io.BufferedReader;
import java.io.IOException;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;

import com.yahoo.sketches.sampling.VarOptItemsSamples;
import com.yahoo.sketches.sampling.VarOptItemsSketch;

public class VarOptSamplingCL extends CommandLine<VarOptItemsSketch<String>> {
  VarOptSamplingCL() {
    super();
    // input options
    options.addOption(Option.builder("k")
        .desc("parameter k")
        .hasArg()
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
    if (cmd.hasOption("k")) { // user defined k
      sketch = VarOptItemsSketch.newInstance(Integer.parseInt(cmd.getOptionValue("k")));
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
      if (cmd.hasOption("w")) {
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
    // BufferReader br =
    //    new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes,UTF_8)));
    // String itemStr = "";
    // VarOptItemsSketch<String> sketch =  // user defined k
    //    VarOptItemsSketch.newInstance(Integer.parseInt(cmd.getOptionValue("k")));

    // try {
    //   while ((itemStr = br.readLine()) != null) {
    //     String[] tokens = itemStr.split("\\s+");
    //     if (tokens.length == 2) {
    //       sketch.update(tokens[1], Double.parseDouble(tokens[0]));
    //     }
    //   }
    // } catch (final IOException | NumberFormatException e ) {
    //   printlnErr("Read Error: Item: " + itemStr + ", " + br.toString());
    //   throw new RuntimeException(e);
    // }
    return null; //VarOptItemsSketch.heapify(Memory.wrap(bytes), new ArrayOfItemsSerDe<String>());
  }

  @Override
  protected byte[] serializeSketch(final VarOptItemsSketch<String> sketch) {
    // String itemStr = "";
    // VarOptItemsSamples<String> samples = union.getResult().getSketchSamples();
    // for (VarOptItemsSamples<String>.WeightedSample ws : samples) {
    //   itemStr += ws.getItem() + "\t" + ws.getWeight() + "\n";
    // }
    // return itemStr.getBytes();
    return null; //
    // return sketch.toByteArray(new ArrayOfItemsSerDe<String>());
  }

  @Override
  protected void mergeSketches() {
    //   int k = sketches.get(sketches.size()-1).getK();
    //   VarOptItemsUnion<String> union = VarOptItemsUnion.newInstance(k);
    //   for (VarOptItemsSketch  sketch: sketches) {
    //     union.update(sketch);
    //   }
    //   sketches.add(union.getResult());
  }

  @Override
  protected void queryCurrentSketch() {
    final VarOptItemsSketch<String>  sketch =  sketches.get(sketches.size() - 1);
    final VarOptItemsSamples<String> samples = sketch.getSketchSamples();

    for (VarOptItemsSamples<String>.WeightedSample ws : samples) {
      System.out.println(ws.getItem() + "\t" + ws.getWeight());
    }
  }
}
